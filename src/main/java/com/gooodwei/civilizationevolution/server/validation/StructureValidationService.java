package com.gooodwei.civilizationevolution.server.validation;

import com.gooodwei.civilizationevolution.api.IMultiBlockMachine;
import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 多方块结构验证后台线程服务。
 *
 * <p>将定时验证从主线程移至独立后台线程，避免巨型结构造成 TPS 暴跌。
 *
 * <h3>线程安全设计</h3>
 * <table>
 *   <tr><th>数据</th><th>线程</th><th>安全保证</th></tr>
 *   <tr><td>BlockEntity (IMultiBlockPart)</td><td>主线程</td>
 *       <td>HashMap 访问不安全，通过 {@link PartSnapshot} 快照传递</td></tr>
 *   <tr><td>LevelChunk 引用</td><td>主线程预取</td>
 *       <td>{@link ServerLevel#getChunkSource()#getChunkNow} 在主线程调用，
 *           后台线程仅读取已获取的引用</td></tr>
 *   <tr><td>BlockState</td><td>后台线程</td>
 *       <td>{@link LevelChunk#getBlockState} 仅做数组访问 + 读取不可变 flyweight，
 *           JVM 保证引用数组元素读取的原子性</td></tr>
 *   <tr><td>ParsedPattern</td><td>后台线程</td>
 *       <td>不可变 record，构造后不修改</td></tr>
 *   <tr><td>MultiBlockState 结果写入</td><td>主线程回调</td>
 *       <td>通过 {@code MinecraftServer.execute} 回到主线程</td></tr>
 *   <tr><td>版本号</td><td>原子操作</td>
 *       <td>{@link AtomicInteger}，丢弃过期验证结果</td></tr>
 * </table>
 *
 * <h3>受限场景</h3>
 * <p>以下情况验证<b>不会</b>提交到后台，而是由调用方处理：
 * <ul>
 *   <li>结构区域跨多个区块且任一区块未加载 → 主线程定时重试</li>
 *   <li>解析模式失败（配置错误）→ 直接返回</li>
 * </ul>
 */
public final class StructureValidationService {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "CivEvo-StructureValidator");
        t.setDaemon(true);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        return t;
    });

    /** 全局验证版本号，用于丢弃过期结果 */
    private static final AtomicInteger GLOBAL_VERSION = new AtomicInteger(0);

    /** 正在验证中的机器集合（防止同一台机器重复提交） */
    private static final Set<IMultiBlockMachine> PENDING = Collections.synchronizedSet(new HashSet<>());

    private StructureValidationService() {}

    // ==================== 提交验证 ====================

    /**
     * 提交定时验证任务到后台线程。
     *
     * <p>在主线程完成所有需要线程安全保证的操作（BlockEntity 快照、Chunk 预取），
     * 将纯数据传递给后台线程进行匹配计算。
     *
     * @param machine 待验证的多方块机器
     * @param level   服务端世界
     * @return true 表示已提交，false 表示跳过
     */
    public static boolean submitPeriodicValidation(IMultiBlockMachine machine, ServerLevel level) {
        if (!PENDING.add(machine)) {
            return false; // 已有验证进行中
        }

        IMultiBlockMachine.ParsedPattern pattern = machine.parsePattern();
        if (pattern == null) {
            PENDING.remove(machine);
            return false;
        }

        Direction facing = machine.getFacingDirection();
        BlockPos controllerPos = machine.getBlockPos();
        int version = GLOBAL_VERSION.incrementAndGet();

        // ===== 主线程：计算结构包围盒并预取所有 LevelChunk =====
        // 控制器为中心的偏移量
        int offsetX = pattern.controllerX();
        int offsetZ = pattern.controllerZ();
        BlockPos minCorner = IMultiBlockMachine.worldPosFromLocal(-offsetX, -pattern.controllerY(), -offsetZ,
                facing, controllerPos);
        BlockPos maxCorner = IMultiBlockMachine.worldPosFromLocal(pattern.width() - 1 - offsetX,
                pattern.height() - 1 - pattern.controllerY(),
                pattern.depth() - 1 - offsetZ,
                facing, controllerPos);

        // 归一化包围盒
        int minChunkX = Math.min(minCorner.getX(), maxCorner.getX()) >> 4;
        int minChunkZ = Math.min(minCorner.getZ(), maxCorner.getZ()) >> 4;
        int maxChunkX = Math.max(minCorner.getX(), maxCorner.getX()) >> 4;
        int maxChunkZ = Math.max(minCorner.getZ(), maxCorner.getZ()) >> 4;

        Map<ChunkPos, LevelChunk> chunkMap = new HashMap<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) {
                    // 区块未加载 → 不提交后台验证，等待下次主线程重试
                    PENDING.remove(machine);
                    return false;
                }
                chunkMap.put(new ChunkPos(cx, cz), chunk);
            }
        }

        // ===== 主线程：收集 IMultiBlockPart 零件快照 =====
        Map<BlockPos, PartSnapshot> partSnapshots = new HashMap<>();
        collectPartSnapshots(level, pattern, facing, controllerPos,
                machine.getTier(), chunkMap, partSnapshots);

        // ===== 提交到后台线程（只传递线程安全的数据） =====
        final int fVersion = version;
        EXECUTOR.submit(() -> {
            try {
                ValidationResult result = validateInBackground(
                        pattern, facing, controllerPos, chunkMap,
                        partSnapshots, machine.getTier());

                // 回传主线程应用结果
                level.getServer().execute(() -> {
                    try {
                        if (GLOBAL_VERSION.get() == fVersion) {
                            applyResult(machine, result);
                        }
                    } finally {
                        PENDING.remove(machine);
                    }
                });
            } catch (Exception e) {
                // CivilizationEvolution.LOGGER.error("后台结构验证异常", e);
                level.getServer().execute(() -> PENDING.remove(machine));
            }
        });

        return true;
    }

    // ==================== 零件快照收集（仅主线程） ====================

    /**
     * 在<b>主线程</b>收集 IMultiBlockPart 零件信息。
     *
     * <p>BlockEntity 访问（HashMap lookup）不安全，必须在主线程完成。
     * 方块类型位置由后台线程通过 {@link LevelChunk#getBlockState} 直接读取。
     */
    private static void collectPartSnapshots(
            ServerLevel level,
            IMultiBlockMachine.ParsedPattern pattern,
            Direction facing,
            BlockPos controllerPos,
            com.gooodwei.civilizationevolution.api.tier.Tier controllerTier,
            Map<ChunkPos, LevelChunk> chunkMap,
            Map<BlockPos, PartSnapshot> outSnapshots) {

        for (int i = 0; i < pattern.positionCount(); i++) {
            int lx = pattern.localOffsetsX()[i];
            int ly = pattern.localOffsetsY()[i];
            int lz = pattern.localOffsetsZ()[i];
            char c = pattern.positionChars()[i];

            // 使用预编译 MatchTarget 检查是否需要收集零件快照
            // 方块/Tag/self 类型直接读 BlockState，不需要快照；仅 PART_TYPE 需要
            boolean needsPartSnapshot = false;
            IMultiBlockMachine.MatchTarget[] targets = pattern.allowedTargetsCache().get(c);
            if (targets != null) {
                for (IMultiBlockMachine.MatchTarget t : targets) {
                    if (t.kind() == IMultiBlockMachine.MatchKind.PART_TYPE) {
                        needsPartSnapshot = true;
                        break;
                    }
                }
            }
            if (!needsPartSnapshot) continue;

            BlockPos worldPos = IMultiBlockMachine.worldPosFromPrecomputed(
                    lx, ly, lz, facing, controllerPos);

                    // 用预取的 chunk 获取 BE（避免通过 ServerLevel 间接查找）
                    int chunkX = worldPos.getX() >> 4;
                    int chunkZ = worldPos.getZ() >> 4;
                    LevelChunk chunk = chunkMap.get(new ChunkPos(chunkX, chunkZ));
                    if (chunk == null) continue;

                    IMultiBlockPart part = resolvePartFromChunk(chunk, worldPos);
                    if (part != null) {
                        BlockPos owner = part.getOwningController(level, worldPos);
                        outSnapshots.put(worldPos, new PartSnapshot(
                                part.getPartType(), part.getPartTier().getLevel(), owner));
                    }
        }
    }

    // ==================== 后台验证（纯数据，无线程安全问题） ====================

    /**
     * 在<b>后台线程</b>执行结构验证。
     *
     * <p>不接收 {@link ServerLevel} 参数，仅操作预取的 {@link LevelChunk} 引用
     * 和零件快照。{@link LevelChunk#getBlockState} 仅访问内部的
     * {@code LevelChunkSection[]} 数组，JVM 保证引用读取原子性。
     */
    private static ValidationResult validateInBackground(
            IMultiBlockMachine.ParsedPattern pattern,
            Direction facing,
            BlockPos controllerPos,
            Map<ChunkPos, LevelChunk> chunkMap,
            Map<BlockPos, PartSnapshot> partSnapshots,
            com.gooodwei.civilizationevolution.api.tier.Tier controllerTier) {

        // 使用 positionCount 估算合理容量，减少 ArrayList 扩容开销
        int estimatedParts = pattern.positionCount();
        int hatchEstimate = Math.max(4, estimatedParts / 10);

        List<BlockPos> populationInputHatches = new ArrayList<>(hatchEstimate);
        List<BlockPos> populationOutputHatches = new ArrayList<>(hatchEstimate);
        List<BlockPos> itemInputHatches = new ArrayList<>(hatchEstimate);
        List<BlockPos> itemOutputHatches = new ArrayList<>(hatchEstimate);
        List<BlockPos> foodHatches = new ArrayList<>(hatchEstimate);
        List<BlockPos> fluidInputHatches = new ArrayList<>(hatchEstimate);
        List<BlockPos> fluidOutputHatches = new ArrayList<>(hatchEstimate);
        List<BlockPos> casingPositions = new ArrayList<>(Math.max(32, estimatedParts));
        Set<BlockPos> allParts = new LinkedHashSet<>(Math.max(64, estimatedParts));
        Map<Character, Integer> keyCounts = new HashMap<>(pattern.keyDefs().size());

        for (int i = 0; i < pattern.positionCount(); i++) {
            int lx = pattern.localOffsetsX()[i];
            int ly = pattern.localOffsetsY()[i];
            int lz = pattern.localOffsetsZ()[i];
            char c = pattern.positionChars()[i];

            BlockPos worldPos = IMultiBlockMachine.worldPosFromPrecomputed(
                    lx, ly, lz, facing, controllerPos);

                    // 从预取 chunk 获取 BlockState（数组访问，线程安全）
                    int chunkX = worldPos.getX() >> 4;
                    int chunkZ = worldPos.getZ() >> 4;
                    LevelChunk chunk = chunkMap.get(new ChunkPos(chunkX, chunkZ));
                    if (chunk == null) continue; // 不应发生（主线程已验证全部加载）

                    // 简单位置快速路径：纯方块 ID + 无 alternatives + 无 min/max 约束
                    // 直接 Block 引用 == 比较，跳过 MatchTarget 管道（约占 80%+ 位置）
                    if (pattern.positionKinds()[i] == IMultiBlockMachine.PositionKind.SIMPLE_BLOCK) {
                        Block expected = (Block) pattern.positionTargets()[i];
                        if (chunk.getBlockState(worldPos).getBlock() != expected) {
                            return ValidationResult.notFormed();
                        }
                        allParts.add(worldPos);
                        casingPositions.add(worldPos);
                        // SIMPLE_BLOCK 无 min/max 约束，跳过 keyCounts
                        continue;
                    }

                    // 复杂位置：走预编译 MatchTarget 完整管道
                    IMultiBlockMachine.MatchTarget[] targets = pattern.allowedTargetsCache().get(c);
                    boolean matched = false;
                    String matchedTypeStr = null;

                    for (IMultiBlockMachine.MatchTarget target : targets) {
                        switch (target.kind()) {
                            case BLOCK_ID -> {
                                Block expected = (Block) target.value();
                                if (chunk.getBlockState(worldPos).getBlock() == expected) {
                                    matched = true;
                                    matchedTypeStr = BuiltInRegistries.BLOCK.getKey(expected).toString();
                                }
                            }
                            case BLOCK_TAG -> {
                                @SuppressWarnings("unchecked")
                                TagKey<Block> tagKey = (TagKey<Block>) target.value();
                                if (chunk.getBlockState(worldPos).is(tagKey)) {
                                    matched = true;
                                    matchedTypeStr = "tag:" + tagKey.location();
                                }
                            }
                            case PART_TYPE -> {
                                String partType = (String) target.value();
                                PartSnapshot snapshot = partSnapshots.get(worldPos);
                                if (snapshot != null
                                        && snapshot.tierLevel <= controllerTier.getLevel()
                                        && snapshot.partType.equals(partType)) {
                                    // 检查零件认领（非共享模式下，已被其他控制器认领时跳过此类型）
                                    if (snapshot.owningController != null
                                            && !snapshot.owningController.equals(controllerPos)
                                            && !pattern.shareable()) {
                                        continue;
                                    }
                                    matched = true;
                                    matchedTypeStr = partType;
                                }
                            }
                            case SELF -> { /* 不应出现（预计算已过滤 self 位置） */ }
                        }
                        if (matched) break;
                    }

                    if (!matched) {
                        return ValidationResult.notFormed();
                    }

                    allParts.add(worldPos);
                    if (matchedTypeStr != null) {
                        if (IMultiBlockMachine.isBlockOrTagType(matchedTypeStr)
                                || IMultiBlockMachine.isTagType(matchedTypeStr)) {
                            casingPositions.add(worldPos);
                        } else {
                            switch (matchedTypeStr) {
                                case IMultiBlockPart.TYPE_POPULATION_INPUT_HATCH -> populationInputHatches.add(worldPos);
                                case IMultiBlockPart.TYPE_POPULATION_OUTPUT_HATCH -> populationOutputHatches.add(worldPos);
                                case IMultiBlockPart.TYPE_FOOD_HATCH -> foodHatches.add(worldPos);
                                case IMultiBlockPart.TYPE_FLUID_INPUT_HATCH -> fluidInputHatches.add(worldPos);
                                case IMultiBlockPart.TYPE_FLUID_OUTPUT_HATCH -> fluidOutputHatches.add(worldPos);
                                case IMultiBlockPart.TYPE_ITEM_INPUT_HATCH -> itemInputHatches.add(worldPos);
                                case IMultiBlockPart.TYPE_ITEM_OUTPUT_HATCH -> itemOutputHatches.add(worldPos);
                                default -> casingPositions.add(worldPos);
                            }
                        }
                    }

                    Character matchedKey = IMultiBlockMachine.findMatchingKey(c, matchedTypeStr, pattern);
                    keyCounts.merge(matchedKey, 1, Integer::sum);
        }

        // 验证 min_count / max_count
        for (Map.Entry<Character, IMultiBlockMachine.KeyDefinition> entry : pattern.keyDefs().entrySet()) {
            char c = entry.getKey();
            IMultiBlockMachine.KeyDefinition kd = entry.getValue();
            int count = keyCounts.getOrDefault(c, 0);
            if (kd.minCount() > 0 && count < kd.minCount()) return ValidationResult.notFormed();
            if (kd.maxCount() < Integer.MAX_VALUE && count > kd.maxCount()) return ValidationResult.notFormed();
        }

        return ValidationResult.formed(
                populationInputHatches, populationOutputHatches, foodHatches,
                fluidInputHatches, fluidOutputHatches,itemInputHatches, itemOutputHatches,
                casingPositions, allParts);
    }

    // ==================== 结果应用（主线程回调） ====================

    /** 在<b>主线程</b>应用后台验证结果 */
    private static void applyResult(IMultiBlockMachine machine, ValidationResult result) {
        var mbs = machine.mbs();
        Level level = machine.getLevel();

        if (result.structureFormed) {
            mbs.populationInputHatches.clear();
            mbs.populationInputHatches.addAll(result.populationInputHatches);
            mbs.populationOutputHatches.clear();
            mbs.populationOutputHatches.addAll(result.populationOutputHatches);
            mbs.foodHatches.clear();
            mbs.foodHatches.addAll(result.foodHatches);
            mbs.fluidInputHatches.clear();
            mbs.fluidInputHatches.addAll(result.fluidInputHatches);
            mbs.fluidOutputHatches.clear();
            mbs.fluidOutputHatches.addAll(result.fluidOutputHatches);
            mbs.itemInputHatches.clear();
            mbs.itemInputHatches.addAll(result.itemInputHatches);
            mbs.itemOutputHatches.clear();
            mbs.itemOutputHatches.addAll(result.itemOutputHatches);
            mbs.casingPositions.clear();
            mbs.casingPositions.addAll(result.casingPositions);
            mbs.allPartPositions.clear();
            mbs.allPartPositions.addAll(result.allPartPositions);

            // 首次成型或状态变更时认领所有零件（与同步验证路径的 claimPart 保持一致）
            if (!mbs.structureFormed && level != null) {
                BlockPos controllerPos = machine.getBlockPos();
                for (BlockPos partPos : result.allPartPositions) {
                    IMultiBlockPart part = machine.resolveMultiBlockPart(partPos);
                    if (part != null) {
                        part.claimPart(level, partPos, controllerPos);
                    }
                }
            }
            mbs.structureFormed = true;
            machine.markChanged();
        } else {
            if (mbs.structureFormed) {
                mbs.structureFormed = false;
                // 释放已认领的零件（与同步验证路径的 notifyPartsUnformed 保持一致）
                machine.notifyPartsUnformed();
                mbs.clearPartCaches();
                machine.markChanged();
            }
        }
    }

    // ==================== 关闭 ====================

    public static void shutdown() {
        EXECUTOR.shutdownNow();
    }

    // ==================== 辅助方法 ====================

    /**
     * 从 LevelChunk 解析 {@link IMultiBlockPart}。
     * 仅在主线程调用（BlockEntity 访问安全）。
     *
     * @param chunk 目标区块
     * @param pos   世界坐标
     * @return 该位置的 IMultiBlockPart 实例，若均不匹配则返回 null
     */
    @Nullable
    private static IMultiBlockPart resolvePartFromChunk(LevelChunk chunk, BlockPos pos) {
        if (chunk.getBlockEntity(pos) instanceof IMultiBlockPart part) return part;
        if (chunk.getBlockState(pos).getBlock() instanceof IMultiBlockPart part) return part;
        return null;
    }

    // ==================== 内部类型 ====================

    /** IMultiBlockPart 位置的主线程快照（不可变） */
    private record PartSnapshot(String partType, int tierLevel,
                                 @org.jetbrains.annotations.Nullable BlockPos owningController) {}

    /** 后台验证结果 */
    private static class ValidationResult {
        final boolean structureFormed;
        final List<BlockPos> populationInputHatches;
        final List<BlockPos> populationOutputHatches;
        final List<BlockPos> foodHatches;
        final List<BlockPos> fluidInputHatches;
        final List<BlockPos> fluidOutputHatches;
        final List<BlockPos> casingPositions;
        final Set<BlockPos> allPartPositions;
        final List<BlockPos> itemInputHatches;
        final List<BlockPos> itemOutputHatches;

        private ValidationResult(boolean structureFormed,
                                 List<BlockPos> populationInputHatches, List<BlockPos> populationOutputHatches,
                                 List<BlockPos> foodHatches, List<BlockPos> fluidInputHatches,
                                 List<BlockPos> fluidOutputHatches, List<BlockPos> casingPositions,
                                 List<BlockPos> itemInputHatches, List<BlockPos> itemOutputHatches,
                                 Set<BlockPos> allPartPositions) {
            this.structureFormed = structureFormed;
            this.populationInputHatches = populationInputHatches;
            this.populationOutputHatches = populationOutputHatches;
            this.foodHatches = foodHatches;
            this.fluidInputHatches = fluidInputHatches;
            this.fluidOutputHatches = fluidOutputHatches;
            this.itemInputHatches = itemInputHatches;
            this.itemOutputHatches = itemOutputHatches;
            this.casingPositions = casingPositions;
            this.allPartPositions = allPartPositions;
        }

        static ValidationResult formed(List<BlockPos> input, List<BlockPos> output,
                                       List<BlockPos> food, List<BlockPos> fluidIn,
                                       List<BlockPos> fluidOut, List<BlockPos> casings,
                                       List<BlockPos> itemIn, List<BlockPos> itemOut,
                                       Set<BlockPos> all) {
            return new ValidationResult(true, input, output, food,
                    fluidIn, fluidOut, casings, itemIn, itemOut, all);
        }

        static ValidationResult notFormed() {
            return new ValidationResult(false, List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), Set.of());
        }
    }
}

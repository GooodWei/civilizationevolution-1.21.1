package com.gooodwei.civilizationevolution.server.blockentity.abstractmachine;

import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.block.AbstractMachineBlock;
import com.google.gson.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 多方块机器通用父类。
 *
 * <p>位于 {@link AbstractMachineBlockEntity} 和具体多方块机器
 * （如 {@code AbstractHospitalBlockEntity}）之间，
 * 提供 JSON pattern 解析、结构验证、方向旋转、延迟验证、
 * Tier 合规检查和零件分类缓存等所有多方块机器共享的逻辑。
 *
 * <h3>JSON 结构定义格式</h3>
 * <p>子类覆写 {@link #getStructurePattern()} 返回 JSON 字符串，
 * 格式类原版 {@code crafting_shaped} 配方的 pattern+key，
 * 但扩展为 3D（按 Y 分层）：
 * <pre>{@code
 * {
 *   "controller": [0, 0, 0],
 *   "pattern": {
 *     "y0": "CX,XX",
 *     "y1": "XX,XX"
 *   },
 *   "key": {
 *     "C": {"block": "self"},
 *     "X": {"type": "multi_block_part"}
 *   }
 * }
 * }</pre>
 *
 * <p>坐标系统（局部）：x = right（facing 顺时针），y = up，z = forward（facing 方向）。
 * pattern 字符串中空格字符表示该位置可为任意方块（不参与结构判定）。
 *
 * @see IMultiBlockPart
 */
public abstract class AbstractMultiBlockMachineBlockEntity extends AbstractMachineBlockEntity {

    /** 结构是否完整成型（不持久化，每次进世界重新验证） */
    protected boolean structureFormed = false;

    /** 查询结构是否已成型 */
    public boolean isStructureFormed() { return structureFormed; }

    /** 延迟验证倒计时（tick），大于 0 时每 tick 递减，归零时执行验证 */
    private int revalidationDelay = 0;

    /** 解析后的结构模式缓存，避免重复解析 JSON */
    @Nullable
    private ParsedPattern cachedPattern = null;

    /** 结构零件位置缓存（世界坐标），按角色分类。每次 validateStructure() 成功后重建。 */
    protected final List<BlockPos> inputHatches = new ArrayList<>();
    protected final List<BlockPos> outputHatches = new ArrayList<>();
    protected final List<BlockPos> foodHatches = new ArrayList<>();
    protected final List<BlockPos> casingPositions = new ArrayList<>();

    /** 所有已成型零件位置（不分类型），用于破坏时通知所有零件 */
    protected final Set<BlockPos> allPartPositions = new LinkedHashSet<>();

    // ==================== 构造器 ====================

    protected AbstractMultiBlockMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
    }

    // ==================== 抽象方法（子类必须覆写） ====================

    /**
     * 返回多方块结构的 JSON 定义字符串。
     * 详见类级 Javadoc。
     *
     * @return JSON 格式的结构定义
     */
    protected abstract String getStructurePattern();

    // ==================== JSON 解析：内部 Record ====================

    /**
     * 解析后的结构模式。
     *
     * @param width       X 方向宽度（每行字符数）
     * @param height      Y 方向高度（yN 键数量）
     * @param depth       Z 方向深度（逗号分隔的行数）
     * @param controllerY 控制器 Y 层索引
     * @param controllerX 控制器 X 列索引
     * @param controllerZ 控制器 Z 行索引
     * @param layerChars  [y][z][x] 三维字符数组，空格=' '表示任意方块
     * @param key         字符 → 原始 JSON，标记每个字符对应的方块要求
     */
    protected record ParsedPattern(
            int width, int height, int depth,
            int controllerY, int controllerX, int controllerZ,
            char[][][] layerChars,
            Map<Character, JsonObject> key
    ) {}

    // ==================== JSON 解析方法 ====================

    private static final Gson GSON = new Gson();

    /**
     * 解析 {@link #getStructurePattern()} 返回的 JSON 字符串。
     *
     * @return 解析后的结构模式
     * @throws JsonParseException JSON 格式错误时抛出
     */
    protected ParsedPattern parsePattern() {
        if (cachedPattern != null) return cachedPattern;

        String json = getStructurePattern();
        JsonObject root = GSON.fromJson(json, JsonObject.class);

        // 1. 解析 controller 坐标 [y, x, z]
        JsonArray controllerArr = root.getAsJsonArray("controller");
        if (controllerArr.size() != 3)
            throw new JsonParseException("controller 必须是 [y, x, z] 三个整数的数组");
        int controllerY = controllerArr.get(0).getAsInt();
        int controllerX = controllerArr.get(1).getAsInt();
        int controllerZ = controllerArr.get(2).getAsInt();

        // 2. 解析 pattern 对象（按 y0, y1, ... 排序 key）
        JsonObject patternObj = root.getAsJsonObject("pattern");
        List<Map.Entry<String, JsonElement>> sortedLayers = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : patternObj.entrySet()) {
            sortedLayers.add(entry);
        }
        // 按 yN 的数字排序
        sortedLayers.sort(Comparator.comparingInt(e -> {
            String key = e.getKey();
            if (!key.startsWith("y"))
                throw new JsonParseException("pattern 的 key 必须以 'y' 开头，当前: " + key);
            return Integer.parseInt(key.substring(1));
        }));

        int height = sortedLayers.size();
        if (height == 0)
            throw new JsonParseException("pattern 至少需要一个 yN 层");

        // 验证 yN 从 0 开始且无跳跃
        for (int i = 0; i < height; i++) {
            String key = sortedLayers.get(i).getKey();
            int n = Integer.parseInt(key.substring(1));
            if (n != i)
                throw new JsonParseException("y 层必须连续从 0 开始，期望 y" + i + "，实际 " + key);
        }

        // 3. 解析每层的行
        List<String[]> allRows = new ArrayList<>(); // 按 [layer][row] 存储
        int width = -1;
        int depth = -1;

        for (Map.Entry<String, JsonElement> entry : sortedLayers) {
            String layerStr = entry.getValue().getAsString();
            String[] rows = layerStr.split(",", -1); // -1 保留尾部空行
            allRows.add(rows);

            if (depth == -1) {
                depth = rows.length;
            } else if (rows.length != depth) {
                throw new JsonParseException(
                        "y 层 " + entry.getKey() + " 有 " + rows.length + " 行，期望 " + depth);
            }
            if (depth == 0)
                throw new JsonParseException("每层至少需要 1 行");

            for (String row : rows) {
                if (width == -1) {
                    width = row.length();
                } else if (row.length() != width) {
                    throw new JsonParseException(
                            "行 \"" + row + "\" 长度为 " + row.length() + "，期望 " + width);
                }
            }
        }

        // 4. 构建三维字符数组 [y][z][x]
        char[][][] layerChars = new char[height][depth][width];
        for (int y = 0; y < height; y++) {
            String[] rows = allRows.get(y);
            for (int z = 0; z < depth; z++) {
                String row = rows[z];
                for (int x = 0; x < width; x++) {
                    layerChars[y][z][x] = row.charAt(x);
                }
            }
        }

        // 5. 解析 key 映射
        JsonObject keyObj = root.getAsJsonObject("key");
        Map<Character, JsonObject> key = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : keyObj.entrySet()) {
            if (entry.getKey().length() != 1)
                throw new JsonParseException("key 的键必须是单字符，当前: " + entry.getKey());
            char c = entry.getKey().charAt(0);
            if (c == ' ')
                throw new JsonParseException("空格字符不能用作 key 定义");
            key.put(c, entry.getValue().getAsJsonObject());
        }

        // 6. 验证 controller 坐标有效
        if (controllerY < 0 || controllerY >= height ||
                controllerX < 0 || controllerX >= width ||
                controllerZ < 0 || controllerZ >= depth) {
            throw new JsonParseException("controller 坐标 [" + controllerY + "," + controllerX +
                    "," + controllerZ + "] 超出范围 width=" + width + " height=" + height + " depth=" + depth);
        }
        char controllerChar = layerChars[controllerY][controllerZ][controllerX];
        if (controllerChar == ' ') {
            throw new JsonParseException("controller 坐标 [" + controllerY + "," + controllerX +
                    "," + controllerZ + "] 为空格（任意方块），不允许");
        }
        if (!key.containsKey(controllerChar)) {
            throw new JsonParseException("controller 字符 '" + controllerChar + "' 未在 key 中定义");
        }

        // 7. 验证所有非空格字符在 key 中有定义
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    char c = layerChars[y][z][x];
                    if (c != ' ' && !key.containsKey(c)) {
                        throw new JsonParseException(
                                "位置 [" + y + "," + x + "," + z + "] 的字符 '" + c + "' 未在 key 中定义");
                    }
                }
            }
        }

        cachedPattern = new ParsedPattern(width, height, depth,
                controllerY, controllerX, controllerZ, layerChars, key);
        return cachedPattern;
    }

    /** 清除缓存的解析结果（子类在配置变更时调用） */
    protected void invalidatePatternCache() {
        this.cachedPattern = null;
    }

    // ==================== 方向旋转 ====================

    /**
     * 将局部坐标 (x, y, z) 根据 facing 旋转为世界坐标偏移。
     *
     * <p>局部坐标系：x = right（facing 顺时针方向），y = up，z = forward（facing 方向）。
     * 原点 = 控制器方块位置。
     *
     * @param x      局部 X（right 方向，pattern 行内字符列）
     * @param y      局部 Y（up 方向，yN 层索引）
     * @param z      局部 Z（forward 方向，逗号分隔的行索引）
     * @param facing 控制器朝向
     * @return 相对于控制器位置的世界坐标偏移
     */
    protected BlockPos rotateOffset(int x, int y, int z, Direction facing) {
        Direction right = facing.getClockWise();
        return new BlockPos(
                x * right.getStepX() + z * facing.getStepX(),
                y,
                x * right.getStepZ() + z * facing.getStepZ()
        );
    }

    /**
     * 获取指定局部坐标的世界绝对位置。
     *
     * @param x      局部 X
     * @param y      局部 Y
     * @param z      局部 Z
     * @param facing 控制器朝向
     * @return 世界绝对坐标
     */
    protected BlockPos getWorldPos(int x, int y, int z, Direction facing) {
        return getBlockPos().offset(rotateOffset(x, y, z, facing));
    }

    // ==================== 结构验证 ====================

    /**
     * 验证多方块结构是否完整。
     *
     * <p>遍历 pattern 中所有非空格、非 self 位置，
     * 检查对应世界坐标的方块是否实现了 {@link IMultiBlockPart} 且 Tier 合规。
     * 任意位置的区块未加载时，自动调度延迟验证并返回 false。
     *
     * @return true 表示结构完整成型
     */
    public boolean validateStructure() {
        ParsedPattern pattern;
        try {
            pattern = parsePattern();
        } catch (JsonParseException e) {
            structureFormed = false;
            return false;
        }

        if (level == null) return false;

        Direction facing = getBlockState().getValue(AbstractMachineBlock.FACING);

        // 临时列表，验证全部通过后替换正式缓存
        List<BlockPos> newInputHatches = new ArrayList<>();
        List<BlockPos> newOutputHatches = new ArrayList<>();
        List<BlockPos> newFoodHatches = new ArrayList<>();
        List<BlockPos> newCasingPositions = new ArrayList<>();
        Set<BlockPos> newAllParts = new LinkedHashSet<>();

        for (int y = 0; y < pattern.height; y++) {
            for (int z = 0; z < pattern.depth; z++) {
                for (int x = 0; x < pattern.width; x++) {
                    // 跳过控制器自身
                    if (y == pattern.controllerY && x == pattern.controllerX && z == pattern.controllerZ) {
                        continue;
                    }

                    char c = pattern.layerChars[y][z][x];
                    // 跳过空格（任意方块）
                    if (c == ' ') continue;

                    JsonObject keyDef = pattern.key.get(c);
                    String blockType = keyDef.has("block") ? keyDef.get("block").getAsString() : null;

                    // 跳过 self 标记
                    if ("self".equals(blockType)) continue;

                    BlockPos worldPos = getWorldPos(x, y, z, facing);

                    // 检查区块是否已加载
                    if (!level.isLoaded(worldPos)) {
                        scheduleRevalidation(40);
                        return false;
                    }

                    // 检查方块或方块实体是否实现了 IMultiBlockPart
                    IMultiBlockPart part = resolveMultiBlockPart(worldPos);
                    if (part == null) {
                        structureFormed = false;
                        notifyPartsUnformed();
                        return false;
                    }

                    // 检查 Tier 合规
                    if (part.getPartTier().getLevel() > this.getTier().getLevel()) {
                        structureFormed = false;
                        notifyPartsUnformed();
                        return false;
                    }

                    // 按角色分类缓存
                    newAllParts.add(worldPos);
                    String type = keyDef.has("type") ? keyDef.get("type").getAsString() : "multi_block_part";
                    switch (type) {
                        case "input_hatch" -> newInputHatches.add(worldPos);
                        case "output_hatch" -> newOutputHatches.add(worldPos);
                        case "food_hatch" -> newFoodHatches.add(worldPos);
                        default -> newCasingPositions.add(worldPos);
                    }
                }
            }
        }

        // 全部通过 → 更新缓存
        inputHatches.clear();
        inputHatches.addAll(newInputHatches);
        outputHatches.clear();
        outputHatches.addAll(newOutputHatches);
        foodHatches.clear();
        foodHatches.addAll(newFoodHatches);
        casingPositions.clear();
        casingPositions.addAll(newCasingPositions);
        allPartPositions.clear();
        allPartPositions.addAll(newAllParts);

        structureFormed = true;
        setChanged();
        return true;
    }

    // ==================== 延迟验证机制 ====================

    /**
     * 每 tick 处理延迟验证倒计时。
     * 子类的 {@code serverTick} 应在开头调用此方法。
     */
    protected void tickRevalidation() {
        if (revalidationDelay > 0) {
            revalidationDelay--;
            if (revalidationDelay == 0) {
                validateStructure();
                setChanged();
            }
        }
    }

    /**
     * 调度延迟重新验证。
     *
     * @param delayTicks 延迟 tick 数（通常 40 tick = 2 秒）
     */
    protected void scheduleRevalidation(int delayTicks) {
        this.revalidationDelay = delayTicks;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // 延迟 40 tick（2 秒）后重新验证结构，给周围 chunk 加载留时间
        scheduleRevalidation(40);
    }

    // ==================== 结构破坏处理 ====================

    /**
     * 当任意结构零件被破坏时，由零件方块的 {@code onRemove} 调用。
     *
     * @param partPos 被破坏的零件世界坐标
     */
    public void onStructurePartBroken(BlockPos partPos) {
        if (!structureFormed) return;
        structureFormed = false;
        // 弹出所有内部物品到控制器位置
        dropAllItems();
        // 通知所有已缓存零件解除成型状态
        notifyPartsUnformed();
        // 清除缓存
        clearPartCaches();
    }

    /**
     * 掉落全部容器内物品到控制器所在世界位置。
     */
    protected void dropAllItems() {
        if (level instanceof ServerLevel sl) {
            for (int i = 0; i < getContainerSize(); i++) {
                ItemStack stack = getItem(i);
                if (!stack.isEmpty()) {
                    Block.popResource(sl, worldPosition, stack);
                    setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    /**
     * 通知所有已缓存零件解除成型状态。
     * 零件 BE 如果实现了 {@link IMultiBlockPart} 接口，不做额外状态变更
     * （因为零件不存储成型状态，由控制器全权追踪）。
     */
    protected void notifyPartsUnformed() {
        // 零件不存储成型状态，控制器本身清空即可
        // 子类可覆写以添加额外通知逻辑（如粒子效果等）
    }

    /** 清除所有零件位置缓存 */
    protected void clearPartCaches() {
        inputHatches.clear();
        outputHatches.clear();
        foodHatches.clear();
        casingPositions.clear();
        allPartPositions.clear();
    }

    // ==================== 零件位置访问器 ====================

    /** 获取所有输入接口的世界坐标列表 */
    public List<BlockPos> getInputHatches() { return Collections.unmodifiableList(inputHatches); }

    /** 获取所有输出接口的世界坐标列表 */
    public List<BlockPos> getOutputHatches() { return Collections.unmodifiableList(outputHatches); }

    /** 获取所有食物接口的世界坐标列表 */
    public List<BlockPos> getFoodHatches() { return Collections.unmodifiableList(foodHatches); }

    /** 获取所有外壳方块的世界坐标列表 */
    public List<BlockPos> getCasingPositions() { return Collections.unmodifiableList(casingPositions); }

    /**
     * 从指定位置解析 {@link IMultiBlockPart}。
     * 先检查 BlockEntity，再检查 Block 自身（支持无 BE 的外壳方块）。
     *
     * @param pos 世界坐标
     * @return 该位置的 IMultiBlockPart 实例，若均不匹配则返回 null
     */
    @Nullable
    private IMultiBlockPart resolveMultiBlockPart(BlockPos pos) {
        if (level == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IMultiBlockPart part) return part;
        if (level.getBlockState(pos).getBlock() instanceof IMultiBlockPart part) return part;
        return null;
    }

    // ==================== NBT 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("StructureFormed", structureFormed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // structureFormed 从 NBT 加载作为初始参考值，onLoad() 的延迟验证会修正
        structureFormed = tag.getBoolean("StructureFormed");
    }
}

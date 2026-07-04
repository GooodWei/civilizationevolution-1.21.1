package com.gooodwei.civilizationevolution.server.blockentity.multiblock;

import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import com.gooodwei.civilizationevolution.api.career.Career;
import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import com.gooodwei.civilizationevolution.server.block.part.MiningShaftPipe;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractFluidHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractFoodInputHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.config.CivilizationMachineConfig;
import com.gooodwei.civilizationevolution.server.item.PopulationItem;
import com.gooodwei.civilizationevolution.server.registry.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 采石场/矿井抽象父类。
 *
 * <p>提供管道系统、流体消耗、食物消耗、区块采掘等通用逻辑。
 * 子类（如 {@link VillageQuarryBlockEntity}）只需实现少量抽象方法。
 *
 * <h3>工作流程</h3>
 * <ol>
 *   <li>绑定控制器 + 结构成型 → 从人口输入仓路由人口到内部槽位</li>
 *   <li>进度条满 → 老化人口 + 健康波动</li>
 *   <li>学徒系统：失业人口累积目标职业经验</li>
 *   <li>计算食物因子 + 工作效率 → 每周期破坏 {@code floor(效率×2)} 个方块</li>
 *   <li>首次运行：建立首根管道（控制器正下第 2 格）</li>
 *   <li>检查管道连贯性 + 流体充足</li>
 *   <li>到达新 Y 层时建立可挖掘方块列表，每周期从列表中铲除若干方块</li>
 *   <li>本层清空后才下降，防止外部方块生成导致永不下层</li>
 *   <li>每周期消耗镐子耐久 = 可用工人数量（受耐久附魔减免）</li>
 * </ol>
 *
 * @see VillageQuarryBlockEntity
 */
public abstract class AbstractQuarryBlockEntity extends AbstractMultiBlockMachineBlockEntity {

    // ==================== 字段 ====================

    /** 当前已挖掘到的 Y 层，-1 表示管道系统尚未建立 */
    protected int minedY = -1;
    /** 每种矿石最低保留数量（预留，工作逻辑未实现） */
    protected int minKeepNumber;
    /** 当前 Y 层待挖掘方块列表（到达新 Y 层时建立，挖完才准许下降） */
    protected final List<BlockPos> pendingBlocks = new ArrayList<>();
    /** pendingBlocks 对应的 Y 层 */
    protected int pendingY = Integer.MIN_VALUE;
    /** 管道无法延伸时标记为 true，需玩家手动破坏障碍物并重新放置机器核心方块才能重置 */
    protected boolean workCompleted = false;
    /** 客户端同步数据（3 字段：workProgress, workTotalTime, minKeepNumber） */
    protected final ContainerData data;

    // ==================== 构造器 ====================

    protected AbstractQuarryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
        this.data = createData();
    }

    // ==================== 抽象方法 ====================

    /** 镐子槽位索引 */
    protected abstract int getPickaxeSlot();
    /** 人口槽位列表 */
    protected abstract List<Integer> getPopulationSlots();

    // ==================== 可覆写方法 ====================

    /** 采石场工作要求的职业名称（每个具体采石场类必须覆写） */
    @Override
    public abstract String getWorkerCareer();

    /** 每次工作周期给学徒的经验量，优先从配置读取 */
    protected int getApprenticeExpPerCycle() {
        return CivilizationMachineConfig.getApprenticeExpPerCycle(getConfigKey(), 1);
    }

    /** 每次工作消耗的岩浆量（mB），优先从配置读取 */
    protected int getFluidLavaPerCycle() {
        return CivilizationMachineConfig.getFluidLavaPerCycle(getConfigKey(), 100);
    }

    /** 每次工作消耗的水量（mB），优先从配置读取 */
    protected int getFluidWaterPerCycle() {
        return CivilizationMachineConfig.getFluidWaterPerCycle(getConfigKey(), 100);
    }

    /** 每个人口每次工作消耗的食物份数，优先从配置读取 */
    protected int getFoodPerPopulation() {
        return CivilizationMachineConfig.getFoodPerPopulation(getConfigKey(), 1);
    }

    /**
     * 效率→每周期破坏方块数的乘数（每周期方块数 = floor(效率 × 此值)）。
     * 优先从配置读取。
     */
    protected int getBlocksPerCycleMultiplier() {
        return CivilizationMachineConfig.getBlocksPerCycleMultiplier(getConfigKey(), 2);
    }

    /**
     * 水平挖掘范围边长（如 16 表示 16×16 即 1 个区块）。
     * 优先从配置读取，子类可覆写。
     */
    protected int getMiningHorizontalSize() {
        return CivilizationMachineConfig.getMiningHorizontalSize(getConfigKey(), 16);
    }

    /** 健康度波动下限，优先从配置读取 */
    protected int getHealthFluctuateMin() {
        return CivilizationMachineConfig.getHealthFluctuateMin(getConfigKey(), -1);
    }

    /** 健康度波动上限，优先从配置读取 */
    protected int getHealthFluctuateMax() {
        return CivilizationMachineConfig.getHealthFluctuateMax(getConfigKey(), 0);
    }

    // ==================== 管道位置 ====================

    /**
     * 获取首根管道应放置的世界坐标。
     * 控制器正下第 2 格 = 第一个非 JSON 定义的结构方块位置。
     */
    protected BlockPos getFirstPipePos() {
        return getBlockPos().below(2);
    }

    // ==================== 人口管理 ====================

    /**
     * 从人口槽位中筛选适合工作的人口物品。
     * 条件：PopulationItem、年龄 18-65、未死亡、矿工职业。
     */
    protected List<ItemStack> getAvailableWorkers() {
        List<ItemStack> all = new ArrayList<>();
        for (int slot : getPopulationSlots()) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                all.add(stack);
            }
        }
        List<ItemStack> eligible = filterAvailable(all, stack ->
                stack.getItem() instanceof PopulationItem
                        && !PopulationNBT.isDead(stack)
                        && PopulationNBT.getAge(stack) >= 18
                        && PopulationNBT.getAge(stack) <= 65);

        return filterAvailable(eligible, stack ->
                Career.isKindOf(PopulationNBT.getCareer(stack), getWorkerCareer()));
    }

    /**
     * 从人口输入仓室将 PopulationItem 路由到内部空人口槽位。
     */
    protected void routePopulationFromInputHatches() {
        if (level == null) return;
        for (BlockPos hatchPos : getInputHatches()) {
            BlockEntity be = level.getBlockEntity(hatchPos);
            if (!(be instanceof com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractPopulationInputHatchBlockEntity hatch))
                continue;
            ItemStack stack = hatch.getItem(0);
            if (stack.isEmpty() || !(stack.getItem() instanceof PopulationItem)) continue;

            for (int slot : getPopulationSlots()) {
                if (getItem(slot).isEmpty()) {
                    setItem(slot, stack.copy());
                    hatch.setItem(0, ItemStack.EMPTY);
                    hatch.setChanged();
                    setChanged();
                    return;
                }
            }
        }
    }

    // ==================== 流体操作 ====================

    /**
     * 检查流体输入仓是否有足够的岩浆和水。
     */
    protected boolean checkFluidInputs(ServerLevel level) {
        int lavaNeeded = getFluidLavaPerCycle();
        int waterNeeded = getFluidWaterPerCycle();
        long lavaFound = 0, waterFound = 0;
        for (BlockPos hatchPos : getFluidInputHatches()) {
            if (level.getBlockEntity(hatchPos) instanceof AbstractFluidHatchBlockEntity hatch) {
                var stored = hatch.getStoredFluid();
                if (stored == Fluids.LAVA && lavaFound < lavaNeeded) {
                    lavaFound += Math.min(hatch.getFluidAmount(), lavaNeeded - lavaFound);
                } else if (stored == Fluids.WATER && waterFound < waterNeeded) {
                    waterFound += Math.min(hatch.getFluidAmount(), waterNeeded - waterFound);
                }
            }
        }
        return lavaFound >= lavaNeeded && waterFound >= waterNeeded;
    }

    /**
     * 从流体输入仓消耗岩浆和水。
     */
    protected void consumeFluids(ServerLevel level) {
        long lavaToConsume = getFluidLavaPerCycle();
        long waterToConsume = getFluidWaterPerCycle();
        for (BlockPos hatchPos : getFluidInputHatches()) {
            if (lavaToConsume <= 0 && waterToConsume <= 0) break;
            if (level.getBlockEntity(hatchPos) instanceof AbstractFluidHatchBlockEntity hatch) {
                var stored = hatch.getStoredFluid();
                if (stored == Fluids.LAVA && lavaToConsume > 0) {
                    FluidStack drained = hatch.drainInternal(
                            (int) Math.min(lavaToConsume, Integer.MAX_VALUE));
                    lavaToConsume -= drained.getAmount();
                } else if (stored == Fluids.WATER && waterToConsume > 0) {
                    FluidStack drained = hatch.drainInternal(
                            (int) Math.min(waterToConsume, Integer.MAX_VALUE));
                    waterToConsume -= drained.getAmount();
                }
            }
        }
    }

    // ==================== 食物消耗 ====================

    /**
     * 从食物输入仓室消耗食物，计算食物因子。
     *
     * @param level 服务端世界
     * @param foodPerPopulation 每个人口消耗的食物份数
     * @param workerCount 工作人口数量
     * @return 食物因子（0.5 ~ N）
     */
    @SuppressWarnings("unused")
    protected float consumeFoodFromHatches(ServerLevel level, int foodPerPopulation, int workerCount) {
        int totalNeeded = workerCount * foodPerPopulation;
        if (totalNeeded <= 0) return 1.0f;

        float totalNutrition = 0;
        float totalSaturation = 0;
        int collected = 0;

        // 记录消耗信息用于第二遍实际消耗
        record FoodEntry(BlockPos pos, int count, float nutrition, float saturation) {}
        List<FoodEntry> consumed = new ArrayList<>();

        for (BlockPos hatchPos : getFoodHatches()) {
            if (collected >= totalNeeded) break;
            if (!(level.getBlockEntity(hatchPos) instanceof AbstractFoodInputHatchBlockEntity hatch)) continue;
            ItemStack stack = hatch.getItem(0);
            if (stack.isEmpty() || !stack.has(DataComponents.FOOD)) continue;

            FoodProperties food = stack.get(DataComponents.FOOD);
            float nut = food != null ? food.nutrition() : 0;
            float sat = food != null ? nut * food.saturation() * 2 : 0;

            int take = Math.min(stack.getCount(), totalNeeded - collected);
            consumed.add(new FoodEntry(hatchPos, take, nut, sat));
            totalNutrition += nut * take;
            totalSaturation += sat * take;
            collected += take;
        }

        if (collected == 0) return 0.5f; // 无食物 → 50% 效率

        // 第二遍：实际消耗（需要重新锁定）
        for (FoodEntry entry : consumed) {
            if (level.getBlockEntity(entry.pos()) instanceof AbstractFoodInputHatchBlockEntity hatch) {
                hatch.getItem(0).shrink(entry.count());
                hatch.setChanged();
            }
        }

        // 食物因子 = sqrt(totalNutrition + totalSaturation)
        double factor = Math.sqrt(totalNutrition + totalSaturation);
        return (float) (Math.round(factor * 1000.0) / 1000.0);
    }

    /**
     * 额外每人口消耗 1 个食物，将食物营养值直接加到人口的饱食度上。
     *
     * <p>与 {@link #consumeFoodFromHatches} 独立——后者用于计算效率因子，
     * 本方法直接提升人口 NBT 中的 food 值。
     *
     * @param level   服务端世界
     * @param workers 可用工作人口列表
     */
    protected void feedWorkersDirectly(ServerLevel level, List<ItemStack> workers) {
        if (workers.isEmpty()) return;

        int needed = workers.size(); // 每人 1 份食物
        List<BlockPos> foodHatches = getFoodHatches();
        if (foodHatches.isEmpty()) return;

        // 收集食物
        record FoodItem(BlockPos pos, FoodProperties food) {}
        List<FoodItem> available = new ArrayList<>();
        for (BlockPos hatchPos : foodHatches) {
            if (available.size() >= needed) break;
            if (!(level.getBlockEntity(hatchPos) instanceof AbstractFoodInputHatchBlockEntity hatch)) continue;
            ItemStack stack = hatch.getItem(0);
            if (stack.isEmpty() || !stack.has(DataComponents.FOOD)) continue;
            int take = Math.min(stack.getCount(), needed - available.size());
            for (int i = 0; i < take; i++) {
                available.add(new FoodItem(hatchPos, stack.get(DataComponents.FOOD)));
            }
        }

        if (available.isEmpty()) return;

        // 每人消耗 1 份食物，累加饱食度（需要重新锁定）
        for (int i = 0; i < workers.size() && i < available.size(); i++) {
            ItemStack workerStack = workers.get(i);
            FoodItem foodItem = available.get(i);
            FoodProperties food = foodItem.food();

            if (food != null) {
                int currentFood = PopulationNBT.getFood(workerStack);
                int newFood = Math.min(100, currentFood + food.nutrition());
                PopulationNBT.setFood(workerStack, newFood);
            }

            // 从食物输入仓扣除
            if (level.getBlockEntity(foodItem.pos()) instanceof AbstractFoodInputHatchBlockEntity hatch) {
                hatch.getItem(0).shrink(1);
                hatch.setChanged();
            }
        }
    }

    // ==================== 管道连贯性 ====================

    /**
     * 检查指定 Y 范围内管道是否连贯。
     */
    protected boolean checkPipeContinuity(ServerLevel level, int fromY, int toY) {
        BlockPos first = getFirstPipePos();
        for (int y = fromY; y <= toY; y++) {
            BlockPos checkPos = new BlockPos(first.getX(), y, first.getZ());
            BlockState state = level.getBlockState(checkPos);
            if (state.getBlock() instanceof MiningShaftPipe) continue;
            if (isStructureDefinedBlock(level, checkPos)) continue;
            return false;
        }
        return true;
    }

    /**
     * 找到第一个缺失的管道位置并放置 MiningShaftPipe，本周期只修复一根。
     *
     * @return true 表示已全部连贯
     */
    protected boolean repairMissingPipe(ServerLevel level, int fromY, int toY) {
        BlockPos first = getFirstPipePos();
        for (int y = fromY; y <= toY; y++) {
            BlockPos checkPos = new BlockPos(first.getX(), y, first.getZ());
            BlockState state = level.getBlockState(checkPos);
            if (state.getBlock() instanceof MiningShaftPipe) continue;
            if (isStructureDefinedBlock(level, checkPos)) continue;

            level.setBlock(checkPos, BlockRegistry.MINING_SHAFT_PIPE.get().defaultBlockState(), 3);
            return false; // 本周期只修复一根
        }
        return true; // 全部连贯
    }

    /**
     * 检查某位置是否属于多方块结构定义的有效方块（外壳等）。
     */
    private boolean isStructureDefinedBlock(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.getBlock() instanceof IMultiBlockPart) return true;
        if (level.getBlockEntity(pos) instanceof IMultiBlockPart) return true;
        return false;
    }

    // ==================== 挖掘逻辑 ====================

    /**
     * 在指定 Y 层建立待挖掘方块列表。
     * <p>遍历管道所在区块 16×16，收集所有可被当前镐子破坏的方块位置。
     * 列表一旦建立就不再扫描新方块，防止外部方块生成导致永不下层。
     */
    protected void buildPendingBlocks(ServerLevel level, int y) {
        pendingBlocks.clear();
        pendingY = y;
        BlockPos pipePos = getFirstPipePos();
        int size = getMiningHorizontalSize();
        int minX = (pipePos.getX() >> 4) << 4;
        int minZ = (pipePos.getZ() >> 4) << 4;
        ItemStack pickaxe = getItem(getPickaxeSlot());

        for (int dx = 0; dx < size; dx++) {
            for (int dz = 0; dz < size; dz++) {
                BlockPos targetPos = new BlockPos(minX + dx, y, minZ + dz);
                BlockState targetState = level.getBlockState(targetPos);

                if (targetState.isAir()) continue;
                if (!targetState.getFluidState().isEmpty()) continue;
                if (targetState.getBlock() instanceof MiningShaftPipe) continue;
                // 白名单过滤：仅挖掘采石场可挖掘标签内的方块（石头、泥土、沙砾等）
                // 矿石、机器外壳等不在标签内的方块自动跳过
                if (!targetState.is(com.gooodwei.civilizationevolution.tags.ModTags.QUARRY_MINEABLE))
                    continue;

                pendingBlocks.add(targetPos);
            }
        }
    }

    /**
     * 从待挖掘列表中取出若干方块并破坏。
     *
     * <p>每次最多挖掘 {@code blocksPerCycle} 个有效方块（跳过已被外部改变的无效位置），
     * 产物按效率倍率缩放后路由到输出接口。每成功挖掘一个方块即从列表中移除。
     *
     * @param level         服务端世界
     * @param blocksPerCycle 本周期最多破坏的方块数量
     * @param pickaxe       当前镐子
     * @param efficiency    工作效率（产物倍率）
     * @return 实际破坏的方块数量
     */
    protected int minePendingBlocks(ServerLevel level, int blocksPerCycle,
                                     ItemStack pickaxe, float efficiency) {
        List<ItemStack> allDrops = new ArrayList<>();
        int blocksBroken = 0;

        while (blocksBroken < blocksPerCycle && !pendingBlocks.isEmpty()) {
            BlockPos targetPos = pendingBlocks.remove(0);
            BlockState targetState = level.getBlockState(targetPos);

            // 重新校验 —— 方块可能已被外部改变（玩家挖掘、流体流入等）
            if (targetState.isAir()) continue;
            if (!targetState.getFluidState().isEmpty()) continue;
            if (targetState.getBlock() instanceof MiningShaftPipe) continue;
            if (!targetState.is(com.gooodwei.civilizationevolution.tags.ModTags.QUARRY_MINEABLE))
                continue;

            List<ItemStack> drops = Block.getDrops(targetState, level, targetPos,
                    level.getBlockEntity(targetPos), null,
                    pickaxe.isEmpty() ? ItemStack.EMPTY : pickaxe);

            for (ItemStack drop : drops) {
                int newCount = Math.round(drop.getCount() * efficiency);
                if (newCount > 0) {
                    allDrops.add(drop.copyWithCount(newCount));
                }
            }

            level.destroyBlock(targetPos, false);
            blocksBroken++;
        }

        // 产物输出
        if (!allDrops.isEmpty()) {
            outputToHatches(level, allDrops);
        }

        return blocksBroken;
    }

    // ==================== 产物输出 ====================

    /**
     * 将掉落物路由到物品输出接口，遍历所有槽位（兼容 1 槽 Primitive 和 27 槽 Village）。
     * 接口满则掉落在机器上方。
     */
    protected void outputToHatches(ServerLevel level, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            ItemStack remaining = drop.copy();

            // 第一遍：合并到已有同类物品的槽位（遍历所有槽位）
            for (BlockPos hatchPos : getOutputHatches()) {
                if (remaining.isEmpty()) break;
                if (!(level.getBlockEntity(hatchPos) instanceof Container container)) continue;
                for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                    ItemStack slotStack = container.getItem(slot);
                    if (ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                        int space = slotStack.getMaxStackSize() - slotStack.getCount();
                        int toMove = Math.min(space, remaining.getCount());
                        if (toMove > 0) {
                            slotStack.grow(toMove);
                            remaining.shrink(toMove);
                            container.setChanged();
                        }
                    }
                }
            }

            // 第二遍：放入空槽位
            for (BlockPos hatchPos : getOutputHatches()) {
                if (remaining.isEmpty()) break;
                if (!(level.getBlockEntity(hatchPos) instanceof Container container)) continue;
                for (int slot = 0; slot < container.getContainerSize() && !remaining.isEmpty(); slot++) {
                    if (container.getItem(slot).isEmpty()) {
                        container.setItem(slot, remaining.copy());
                        container.setChanged();
                        remaining.setCount(0);
                        break;
                    }
                }
            }

            // 输出接口已满 → 掉落在机器上方
            if (!remaining.isEmpty()) {
                Block.popResource(level, getBlockPos().above(), remaining);
            }
        }
    }

    // ==================== 工作周期 ====================

    @Override
    public void executeWorkCycle(Level level) {
        this.ageAllPopulations(this.getAgeIncrement());
        this.fluctuateHealth(getHealthFluctuateMin(), getHealthFluctuateMax());

        BlockPos pos = this.getBlockPos();

        if (!this.canWork()) {
            this.workProgress = 0;
            this.setChanged();
            return;
        }

        if (!(level instanceof ServerLevel serverLevel)) return;

        // 1. 学徒系统
        addApprenticeExpToPopulationSlots(getWorkerCareer(), getApprenticeExpPerCycle());

        // 2. 计算工作效率
        List<ItemStack> workers = getAvailableWorkers();
        double totalWorkEfficiency = calculateTotalWorkEfficiency(workers);
        float foodFactor = consumeFoodFromHatches(serverLevel,
                getFoodPerPopulation(), workers.size());
        float efficiency = foodFactor * (float) totalWorkEfficiency;

        // 2.5. 额外每人口消耗 1 食物直接补充人口饱食度
        feedWorkersDirectly(serverLevel, workers);

        // 每周期最多破坏方块数 = floor(效率 × 乘数)，至少 1 块
        int blocksPerCycle = Math.max(1, (int) (efficiency * getBlocksPerCycleMultiplier()));
        ItemStack pickaxe = this.getItem(getPickaxeSlot());

        // 3. 首次运行：建立首根管道
        if (minedY == -1) {
            BlockPos firstPipe = getFirstPipePos();
            BlockState firstState = level.getBlockState(firstPipe);

            if (!firstState.isAir() && firstState.getFluidState().isEmpty()
                    && firstState.getDestroySpeed(level, firstPipe) >= 0
                    && !(firstState.getBlock() instanceof MiningShaftPipe)) {
                if (pickaxe.isEmpty() || pickaxe.isCorrectToolForDrops(firstState)) {
                    List<ItemStack> drops = Block.getDrops(firstState, serverLevel,
                            firstPipe, level.getBlockEntity(firstPipe), null,
                            pickaxe.isEmpty() ? ItemStack.EMPTY : pickaxe);
                    if (!drops.isEmpty()) {
                        outputToHatches(serverLevel, drops);
                    }
                    level.destroyBlock(firstPipe, false);
                }
            }

            if (!checkFluidInputs(serverLevel)) {
                this.workProgress = 0;
                this.setChanged();
                return;
            }

            consumeFluids(serverLevel);
            level.setBlock(firstPipe, BlockRegistry.MINING_SHAFT_PIPE.get().defaultBlockState(), 3);
            minedY = firstPipe.getY();

            // ★ 向下扫描已有连续管道（重新放置核心时可能已有遗留管道）
            // 逐格检查，遇到第一个非管道位置即停止——间断留给 checkPipeContinuity 修补
            {
                BlockPos first = getFirstPipePos();
                while (true) {
                    BlockPos below = new BlockPos(first.getX(), minedY - 1, first.getZ());
                    BlockState belowState = level.getBlockState(below);
                    if (belowState.getBlock() instanceof MiningShaftPipe) {
                        minedY--;
                    } else {
                        break;
                    }
                }
            }

            // ★ 管道放置后立即在管道所在 Y 层建立待破坏方块列表
            buildPendingBlocks(serverLevel, minedY);
            pendingY = minedY;
            this.workProgress = 0;
            this.setChanged();
            return;
        }

        // 4. 检查管道连贯性
        int firstPipeY = getFirstPipePos().getY();
        if (!checkPipeContinuity(serverLevel, firstPipeY, minedY)) {
            repairMissingPipe(serverLevel, firstPipeY, minedY);
            this.workProgress = 0;
            this.setChanged();
            return;
        }

        // 5. ★ 待破坏列表为空 → 延伸管道到下一层并建立新列表
        if (pendingBlocks.isEmpty()) {
            int extendToY;
            if (pendingY == Integer.MIN_VALUE) {
                // 首轮：列表建立在当前管道所在层（minedY），无需延伸管道
                extendToY = minedY;
            } else {
                // 上层已清空，从最深管道处向下延伸一层
                extendToY = minedY - 1;
            }

            if (extendToY < level.getMinBuildHeight()) {
                // 到达世界底部，标记工作完成
                workCompleted = true;
                this.workProgress = 0;
                this.setChanged();
                return;
            }

            // ★ 检查管道延伸路径上的方块
            BlockPos pipePos = new BlockPos(getFirstPipePos().getX(), extendToY,
                    getFirstPipePos().getZ());
            BlockState pipeState = level.getBlockState(pipePos);

            boolean isAir = pipeState.isAir() || pipeState.canBeReplaced();
            boolean isMineable = !isAir
                    && pipeState.getDestroySpeed(level, pipePos) >= 0
                    && pipeState.is(com.gooodwei.civilizationevolution.tags.ModTags.QUARRY_MINEABLE)
                    && !(pipeState.getBlock() instanceof MiningShaftPipe);

            // ★ 延伸位置已有管道（旧运行遗留或中途修补）→ 直接使用，不消耗流体
            if (pipeState.getBlock() instanceof MiningShaftPipe) {
                minedY = extendToY;
                buildPendingBlocks(serverLevel, minedY);
                pendingY = minedY;
                this.workProgress = 0;
                this.setChanged();
                return;
            }

            if (!isAir && !isMineable) {
                // 管道延伸路径被非白名单方块（黑曜石、机器外壳等）堵住，标记工作完成
                // 玩家需手动破坏障碍物，拆除后采石场自动恢复工作
                workCompleted = true;
                this.workProgress = 0;
                this.setChanged();
                return;
            }

            // 管道延伸路径畅通，清除 workCompleted 标记（障碍物已被移除）
            workCompleted = false;

            // 消耗流体并延伸管道
            if (!checkFluidInputs(serverLevel)) {
                this.workProgress = 0;
                this.setChanged();
                return;
            }
            consumeFluids(serverLevel);

            // 如果延伸位置是白名单方块，先破坏它
            if (isMineable) {
                List<ItemStack> drops = Block.getDrops(pipeState, serverLevel, pipePos,
                        level.getBlockEntity(pipePos), null,
                        pickaxe.isEmpty() ? ItemStack.EMPTY : pickaxe);
                if (!drops.isEmpty()) {
                    outputToHatches(serverLevel, drops);
                }
                level.destroyBlock(pipePos, false);
            }

            // 放置管道
            level.setBlock(pipePos,
                    BlockRegistry.MINING_SHAFT_PIPE.get().defaultBlockState(), 3);
            minedY = extendToY;

            // 在新管道所在 Y 层建立待破坏方块列表
            buildPendingBlocks(serverLevel, minedY);
            pendingY = minedY;

            // 延伸周期不采矿，下个周期开始破坏新列表中的方块
            this.workProgress = 0;
            this.setChanged();
            return;
        }

        // 6. 从待破坏列表中破坏方块（所有方块均在管道的 minedY 层）
        minePendingBlocks(serverLevel, blocksPerCycle,
                pickaxe.isEmpty() ? ItemStack.EMPTY : pickaxe, efficiency);

        // 7. 每工作周期消耗镐子耐久 = 可用工人数量（受耐久附魔影响）
        int workerCount = getAvailableWorkers().size();
        if (workerCount > 0 && !pickaxe.isEmpty() && pickaxe.isDamageableItem()) {
            int unbreakingLevel = pickaxe.getEnchantments()
                    .getLevel(level.registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.UNBREAKING));
            int actualDamage = 0;
            if (unbreakingLevel > 0) {
                RandomSource random = level.getRandom();
                for (int i = 0; i < workerCount; i++) {
                    if (random.nextFloat() >= 1.0f / (unbreakingLevel + 1)) {
                        actualDamage++;
                    }
                }
            } else {
                actualDamage = workerCount;
            }
            if (actualDamage > 0) {
                pickaxe.hurtAndBreak(actualDamage, serverLevel, null,
                        item -> pickaxe.shrink(1));
            }
        }

        // 8. 更新进度（本轮不会延长管道，延长动作推迟到下周期第一步）
        this.workProgress = 0;
        this.setChanged();
    }

    // ==================== canWork ====================

    @Override
    public boolean isSelfScheduled() {
        return true; // 采石场由自身 serverTick 驱动工作周期，不由控制器调度
    }

    @Override
    public boolean canWork() {
        // 不检查 workCompleted：workCompleted 为 true 时仍需执行工作周期（年龄增长+学徒训练），
        // 否则管道被障碍物堵住后再也无法恢复（workCompleted 永不被重置）。
        // 管道延伸逻辑内部会检查 workCompleted 并自动重置。
        return isStructureFormed() && isBound() && !getAvailableWorkers().isEmpty();
    }

    // ==================== serverTick ====================

    /**
     * 服务端 tick 入口，子类或注册时绑定为 ticker。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   AbstractQuarryBlockEntity be) {
        be.tickRevalidation();

        if (be.isBound() && be.workProgress < be.getWorkTotalTime()) {
            be.workProgress++;
            be.setChanged();
        }

        if (be.isStructureFormed()) {
            be.routePopulationFromInputHatches();
        }

        // 进度条满时始终执行工作周期：即使无可工作人口（全部退休），
        // 仍需老化人口 + 健康波动 + 消耗食物，否则退休人口永不死亡 → 永不过期
        if (be.workProgress >= be.getWorkTotalTime() && be.isStructureFormed() && be.isBound()) {
            be.executeWorkCycle(level);
        }
    }

    // ==================== IPopulationMachine 实现 ====================

    @Override
    public boolean isPopulationSlot(int slot) {
        return getPopulationSlots().contains(slot);
    }

    @Override
    public boolean isOutputSlot(int slot) {
        return false;
    }

    @Override
    public boolean isFoodSlot(int slot) {
        return false;
    }

    @Override
    public List<Integer> populationSlots() {
        return getPopulationSlots();
    }

    // ==================== ContainerData ====================

    @Override
    protected ContainerData createData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> workProgress;
                    case 1 -> getWorkTotalTime();
                    case 2 -> minKeepNumber;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> workProgress = value;
                    case 2 -> minKeepNumber = value;
                    default -> { }
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    // ==================== NBT 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("MinKeepNumber", minKeepNumber);
        tag.putInt("MinedY", minedY);
        tag.putBoolean("WorkCompleted", workCompleted);
        // 待挖掘列表持久化（防止中途卸载/重启丢失进度）
        if (!pendingBlocks.isEmpty()) {
            tag.putInt("PendingY", pendingY);
            long[] arr = new long[pendingBlocks.size()];
            for (int i = 0; i < pendingBlocks.size(); i++) {
                arr[i] = pendingBlocks.get(i).asLong();
            }
            tag.putLongArray("PendingBlocks", arr);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        minKeepNumber = tag.getInt("MinKeepNumber");
        minedY = tag.contains("MinedY") ? tag.getInt("MinedY") : -1;
        workCompleted = tag.getBoolean("WorkCompleted");
        // 恢复待挖掘列表
        pendingBlocks.clear();
        if (tag.contains("PendingY")) {
            pendingY = tag.getInt("PendingY");
            long[] arr = tag.getLongArray("PendingBlocks");
            for (long l : arr) {
                pendingBlocks.add(BlockPos.of(l));
            }
        } else {
            pendingY = Integer.MIN_VALUE;
        }
    }
}

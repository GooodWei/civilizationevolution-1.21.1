package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

/**
 * 农场机器的抽象父类 —— 提供作物催熟和水管理的通用实现。
 *
 * <p>继承层次：
 * <pre>
 * BaseContainerBlockEntity
 *  └── AbstractMachineBlockEntity
 *       └── AbstractRangeMachineBlockEntity
 *            └── AbstractFarmBlockEntity (本类)
 *                 └── PrimitiveFarmBlockEntity (Tier 0 农场)
 * </pre>
 *
 * <h3>核心功能</h3>
 * <ul>
 *   <li><b>作物催熟</b>：扫描范围内 {@link BonemealableBlock} 作物，
 *       通过 {@link BonemealableBlock#isValidBonemealTarget} 检测，
 *       调用 {@link BonemealableBlock#performBonemeal} 催熟</li>
 *   <li><b>储水系统</b>：内部储罐通过 {@link IFluidHandler} 接口暴露，
 *       支持所有物流模组（Pipez/Mekanism/AE2 等）通过
 *       {@code Capabilities.FluidHandler.BLOCK} 自动化输入水</li>
 *   <li><b>流体判断</b>：使用 {@link FluidTags#WATER} 标签判断流体类型，
 *       兼容其他模组修改的原版水变体</li>
 *   <li><b>按需耗水</b>：每催熟一个作物消耗 {@code water_per_crop} mB 水，
 *       由配置文件控制</li>
 * </ul>
 *
 * <h3>槽位布局（与原始牧场相同，共 9 个）</h3>
 * <ul>
 *   <li>槽位 0-5：食物输入槽（2×3）</li>
 *   <li>槽位 6-8：人口输入槽（农民）</li>
 * </ul>
 *
 * <h3>子类需实现</h3>
 * <ul>
 *   <li>{@link #getTankCapacity()} —— 储罐容量（由 Tier 决定）</li>
 *   <li>{@link #getMachineConfigKey()} —— 配置文件中此机器的 section key</li>
 *   <li>{@link #getFoodPerPopulation()} —— 每个人口槽位每次工作的食物消耗量</li>
 *   <li>{@link #createMenu(int, Inventory)} —— 创建 GUI Menu</li>
 * </ul>
 *
 * @see AbstractRangeMachineBlockEntity
 * @see IFluidHandler
 */
public abstract class AbstractFarmBlockEntity extends AbstractRangeMachineBlockEntity {

    // ==================== 储水字段 ====================

    /** 当前储水量（mB），NBT 持久化 */
    private long waterAmount = 0;

    /**
     * 自定义流体处理器 —— 实现 {@link IFluidHandler}，
     * 仅接受水（通过 {@link FluidTags#WATER} 标签判断），
     * 容量由 {@link #getTankCapacity()} 动态查询。
     *
     * <p>所有物流模组的管道均通过 {@code Capabilities.FluidHandler.BLOCK} 与此接口交互。
     */
    protected final IFluidHandler fluidHandler = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1; // 单储罐
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (waterAmount <= 0) {
                return FluidStack.EMPTY;
            }
            // 返回当前储罐中的水量和流体类型
            return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, (int) Math.min(waterAmount, Integer.MAX_VALUE));
        }

        @Override
        public int getTankCapacity(int tank) {
            long cap = AbstractFarmBlockEntity.this.getTankCapacity();
            return cap > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cap;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            // 仅接受水（通过 FluidTags.WATER 标签判断，兼容其他模组的修改版水）
            return stack.is(FluidTags.WATER);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !resource.is(FluidTags.WATER)) {
                return 0;
            }
            long capacity = AbstractFarmBlockEntity.this.getTankCapacity();
            if (waterAmount >= capacity) {
                return 0; // 储罐已满
            }
            long canFill = capacity - waterAmount;
            int toFill = (int) Math.min(canFill, (long) resource.getAmount());
            if (action.execute()) {
                waterAmount += toFill;
                setChanged();
            }
            return toFill;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty() || !resource.is(FluidTags.WATER) || waterAmount <= 0) {
                return FluidStack.EMPTY;
            }
            int toDrain = Math.min(resource.getAmount(), (int) Math.min(waterAmount, Integer.MAX_VALUE));
            if (action.execute()) {
                waterAmount -= toDrain;
                setChanged();
            }
            return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, toDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (waterAmount <= 0 || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            int toDrain = Math.min(maxDrain, (int) Math.min(waterAmount, Integer.MAX_VALUE));
            if (action.execute()) {
                waterAmount -= toDrain;
                setChanged();
            }
            return new FluidStack(net.minecraft.world.level.material.Fluids.WATER, toDrain);
        }
    };

    // ==================== 构造器 ====================

    /**
     * @param type  BlockEntity 类型
     * @param pos   方块坐标
     * @param state 方块状态
     * @param size  容器槽位总数（通常为 9）
     */
    protected AbstractFarmBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
    }

    // ==================== 抽象方法（子类实现） ====================

    /**
     * 储罐总容量（mB），由 Tier 决定。
     * <p>例如：Tier 0 = 8000 mB（8 桶），Tier 1 = 16000 mB（16 桶）。
     *
     * @return 储罐容量（mB）
     */
    public abstract long getTankCapacity();

    /** 配置文件中此机器的 section key（如 "primitive_farm"） */
    protected abstract String getMachineConfigKey();

    /** 每个人口每次工作消耗的食物份数，优先从配置读取 */
    protected int getFoodPerPopulation() {
        return PopulationMachineConfig.getFoodPerPopulation(getMachineConfigKey(), 8);
    }

    // ==================== 可覆写方法（有默认值） ====================

    /** 农场工作要求的职业名称（每个具体农场类必须覆写） */
    @Override
    public abstract String getWorkerCareer();

    /** 每次工作周期给学徒的经验量，优先从配置读取 */
    @Override
    protected int getApprenticeExpPerCycle() {
        return PopulationMachineConfig.getApprenticeExpPerCycle(getMachineConfigKey(), 1);
    }

    /** 健康度波动下限，优先从配置读取 */
    @Override
    protected int getHealthFluctuateMin() {
        return PopulationMachineConfig.getHealthFluctuateMin(getMachineConfigKey(), -5);
    }

    /** 健康度波动上限，优先从配置读取 */
    @Override
    protected int getHealthFluctuateMax() {
        return PopulationMachineConfig.getHealthFluctuateMax(getMachineConfigKey(), -1);
    }

    // ==================== 公开存取器 ====================

    /**
     * 获取流体处理器，供 {@code RegisterCapabilitiesEvent} 中注册
     * {@code Capabilities.FluidHandler.BLOCK} 能力时使用。
     *
     * @return 此农场方块实体的 {@link IFluidHandler} 实例
     */
    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    /**
     * 获取当前储水量（mB），供 GUI 渲染水位条。
     *
     * @return 当前储水量（mB）
     */
    public long getWaterAmount() {
        return waterAmount;
    }

    // ==================== ContainerData ====================

    /**
     * 创建同步数据容器，包含 3 个字段：
     * <ul>
     *   <li>index 0 —— 当前工作进度（{@link #workProgress}）</li>
     *   <li>index 1 —— 工作总时长（{@link #getWorkTotalTime()}）</li>
     *   <li>index 2 —— 当前储水量（低位 32 位，供 GUI 水位条）</li>
     * </ul>
     */
    @Override
    protected ContainerData createData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> workProgress;
                    case 1 -> getWorkTotalTime();
                    case 2 -> (int) Math.min(waterAmount, Integer.MAX_VALUE);
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> workProgress = value;
                    // index 1 和 2 由服务端维护，客户端只读
                    default -> { }
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    // ==================== IPopulationMachine 实现 ====================

    @Override
    public int getWorkTotalTime() {
        return PopulationMachineConfig.getWorkTotalTime(getMachineConfigKey());
    }

    @Override
    public int getAgeIncrement() {
        return PopulationMachineConfig.getAgeIncrement(getMachineConfigKey());
    }

    @Override
    public abstract boolean isPopulationSlot(int slot);

    @Override
    public abstract List<Integer> populationSlots();

    @Override
    public boolean isOutputSlot(int slot) {
        return false; // 农场无输出槽位
    }

    @Override
    public abstract boolean isFoodSlot(int slot);

    // canWork 继承 AbstractRangeMachineBlockEntity（bind + 有可用农民）
    // 食物检查已移除 —— consumeFoodWithFallback 自动处理食物不足回退

    // ==================== IClientUpdateReceiver ====================

    /**
     * 接收客户端发来的字段更新（由 {@code UpdateMachineFieldPayload} 携带）。
     * 运行在服务端。预留：后续可在此处理客户端设置（如最小保留数量等）。
     *
     * @param fieldId 字段编号
     * @param data    客户端提交的数据，通过 CompoundTag 携带任意类型
     */
    @Override
    public void onClientUpdate(int fieldId, net.minecraft.nbt.CompoundTag data) {
        // 预留：后续可在此处理客户端设置
    }

    // ==================== 工作周期（作物催熟 + 耗水） ====================

    /**
     * 执行一次农场工作周期。
     *
     * <p>流程：
     * <ol>
     *   <li>调用 {@link #ageAllPopulations} 和 {@link #fluctuateHealth} —— 人口老化</li>
     *   <li>扫描冲突（同类型机器范围重叠检测）</li>
     *   <li>检查食物是否充足</li>
     *   <li>扫描范围内所有 {@link BonemealableBlock} 作物</li>
     *   <li>根据农民工作效率计算可催熟作物数</li>
     *   <li>逐个催熟，每催熟一个消耗 {@code water_per_crop} mB 水</li>
     *   <li>水量不足时停止催熟</li>
     * </ol>
     *
     * @param level 当前世界（服务端）
     */
    @Override
    public void executeWorkCycle(Level level) {
        // 人口老化
        this.ageAllPopulations(this.getAgeIncrement());
        this.fluctuateHealth(getHealthFluctuateMin(), getHealthFluctuateMax());

        // 冲突检测
        if (level instanceof ServerLevel serverLevel) {
            this.scanAndMarkConflicts(serverLevel);
        }

        BlockPos pos = this.getBlockPos();

        if (level instanceof ServerLevel serverLevel && this.canWork()) {
            // 学徒系统：调用 IPopulationItem.addApprenticeExp 处理晋级
            addApprenticeExpToPopulationSlots(getWorkerCareer(), getApprenticeExpPerCycle());

            // 计算农民总工作效率
            double totalWorkEfficiency = calculateTotalWorkEfficiency(this.getAvailableWorkers());

            // 消耗食物并获取食物因子
            float foodFactor = consumeFoodWithFallback(
                    getFoodPerPopulation(),
                    Math::sqrt,
                    getAvailableWorkers().size());
            float efficiency = foodFactor * (float) totalWorkEfficiency;

            // 根据效率计算可催熟作物数（至少 1）
            int cropCount = Math.max(1, Math.round(efficiency));

            // 获取每次催熟的水消耗量
            int waterPerCrop = PopulationMachineConfig.getWaterPerCrop(getMachineConfigKey());

            // 扫描范围内可催熟作物
            var range = getSelectionRange();
            int cropsFertilized = 0;

            for (int x = (int) range.minX; x < (int) range.maxX && cropsFertilized < cropCount; x++) {
                for (int z = (int) range.minZ; z < (int) range.maxZ && cropsFertilized < cropCount; z++) {
                    for (int y = (int) range.minY; y < (int) range.maxY && cropsFertilized < cropCount; y++) {
                        BlockPos cropPos = new BlockPos(x, y, z);
                        BlockState cropState = serverLevel.getBlockState(cropPos);

                        // 仅催熟农作物类方块（通过 minecraft:crops 标签过滤，排除草方块等非作物）
                        if (cropState.is(BlockTags.CROPS)
                                && cropState.getBlock() instanceof BonemealableBlock bonemealable) {

                            // 跳过已成熟作物（CropBlock 类通过 isMaxAge 判断，其他块信任 isValidBonemealTarget）
                            if (cropState.getBlock() instanceof CropBlock cropBlock
                                    && cropBlock.isMaxAge(cropState)) {
                                continue;
                            }

                            // 检查是否有足够的水
                            if (waterAmount < waterPerCrop) {
                                // 水量不足，结束催熟
                                cropsFertilized = cropCount; // 跳出所有循环
                                break;
                            }

                            if (bonemealable.isValidBonemealTarget(serverLevel, cropPos, cropState)) {
                                // 消耗水
                                waterAmount -= waterPerCrop;
                                // 催熟作物（消耗骨粉效果）
                                bonemealable.performBonemeal(serverLevel,
                                        serverLevel.getRandom(), cropPos, cropState);
                                cropsFertilized++;
                            }
                        }
                    }
                }
            }
        }

        // 重置工作进度，标记变更
        this.workProgress = 0;
        setChanged(level, pos, level.getBlockState(pos));
    }

    // ==================== NBT 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("WaterAmount", waterAmount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        waterAmount = tag.getLong("WaterAmount");
    }

    // ==================== 冲突检测 ====================

    /**
     * 农场使用冲突检测，防止两个农场并发操作同一作物导致并发漏洞。
     * <p>范围冲突时所有相关农场触发红色粒子边框、暂停工作进度。
     *
     * @return 农场冲突检测标签 {@link ModTags#FARM_CONFLICTS}
     */
    @Override
    protected net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getConflictTag() {
        return ModTags.FARM_CONFLICTS;
    }
}

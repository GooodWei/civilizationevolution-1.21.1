package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.block.machine.AbstractMachineBlock;
import com.gooodwei.civilizationevolution.server.config.CivilizationMachineConfig;
import com.gooodwei.civilizationevolution.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.List;

public abstract class AbstractHarvesterBlockEntity extends AbstractRangeMachineBlockEntity {

    /**
     * 机器中储罐容积
     */
    private long waterAmount = 0;

    protected final IFluidHandler fluidHandler = new IFluidHandler() {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (waterAmount <= 0) {
                return FluidStack.EMPTY;
            }
            return new FluidStack(Fluids.WATER, (int) Math.min(waterAmount, Integer.MAX_VALUE));
        }

        @Override
        public int getTankCapacity(int tank) {
            long cap = AbstractHarvesterBlockEntity.this.getTankCapacity();
            return cap > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) cap;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack fluidStack) {
            return fluidStack.is(FluidTags.WATER);
        }

        @Override
        public int fill(FluidStack resource, FluidAction fluidAction) {
            if (resource.isEmpty() || !resource.is(FluidTags.WATER)) {
                return 0;
            }
            long capacity = AbstractHarvesterBlockEntity.this.getTankCapacity();
            if (waterAmount >= capacity) {
                return 0; // 储罐已满
            }
            long canFill = capacity - waterAmount;
            int toFill = (int) Math.min(canFill, (long) resource.getAmount());
            if (fluidAction.execute()) {
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
            return new FluidStack(Fluids.WATER, toDrain);
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
            return new FluidStack(Fluids.WATER, toDrain);
        }
    };

    protected AbstractHarvesterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
    }

    public long getWaterAmount() {
        return waterAmount;
    }

    public IFluidHandler getFluidHandler() {
        return fluidHandler;
    }

    // ==================== ContainerData ====================

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
            int cropCount = Math.max(1, Math.round(efficiency));
            int waterPerCrop = CivilizationMachineConfig.getWaterPerCrop(getMachineConfigKey());
            AABB range = getSelectionRange();
            int cropNum = 0;
            for (int x = (int) range.minX; x < (int) range.maxX && cropNum < cropCount; x++) {
                for (int z = (int) range.minZ; z < (int) range.maxZ && cropNum < cropCount; z++) {
                    for (int y = (int) range.minY; y < (int) range.maxY && cropNum < cropCount; y++) {
                        BlockPos cropPos = new BlockPos(x, y, z);
                        BlockState cropState = serverLevel.getBlockState(cropPos);
                        if (cropState.is(BlockTags.CROPS)) {
                            // 成熟作物（CropBlock 类通过 isMaxAge 判断）
                            if (cropState.getBlock() instanceof CropBlock cropBlock
                                    && cropBlock.isMaxAge(cropState)) {
                                // 检查是否有足够的水
                                if (waterAmount < waterPerCrop) {
                                    // 水量不足，结束
                                    cropNum = cropCount; // 跳出所有循环
                                    break;
                                }
                                // 获取掉落物
                                List<ItemStack> drops = Block.getDrops(cropState, serverLevel, cropPos,
                                        serverLevel.getBlockEntity(cropPos), null, ItemStack.EMPTY);
                                serverLevel.setBlock(cropPos, cropState.setValue(CropBlock.AGE, 0), CropBlock.UPDATE_CLIENTS);
                                waterAmount -= waterPerCrop;

                                for (ItemStack drop : drops) {
                                    // ★ 效率乘数量
                                    int newCount = Math.round(drop.getCount() * efficiency);
                                    if (newCount <= 0) continue;

                                    ItemStack remaining = drop.copyWithCount(newCount);
                                    // 先合并到已有同类物品的输出槽
                                    for (int i = 0; i < getContainerSize() && !remaining.isEmpty(); i++) {
                                        if (!isOutputSlot(i)) continue;
                                        ItemStack slotStack = getItem(i);
                                        if (ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                                            int space = slotStack.getMaxStackSize() - slotStack.getCount();
                                            int toMove = Math.min(space, remaining.getCount());
                                            if (toMove > 0) {
                                                slotStack.grow(toMove);
                                                remaining.shrink(toMove);
                                            }
                                        }
                                    }

                                    // 再放入空输出槽
                                    for (int i = 0; i < getContainerSize() && !remaining.isEmpty(); i++) {
                                        if (!isOutputSlot(i)) continue;
                                        if (getItem(i).isEmpty()) {
                                            int toMove = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                                            setItem(i, remaining.copyWithCount(toMove));
                                            remaining.shrink(toMove);
                                        }
                                    }

                                    // 还有剩余则弹出到世界
                                    if (!remaining.isEmpty()) {
                                        Block.popResource(serverLevel, pos.above(), remaining);
                                    }

                                }
                                cropNum++;
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
                    default -> {
                    }
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    @Override
    public abstract boolean isOutputSlot(int slot);

    /**
     * 配置文件中此机器的 section key（如 "primitive_ranch"）
     */
    protected abstract String getMachineConfigKey();

    /** 每个人口每次工作消耗的食物份数，优先从配置读取 */
    protected int getFoodPerPopulation() {
        return CivilizationMachineConfig.getFoodPerPopulation(getMachineConfigKey(), 8);
    }

    /**
     * 冲突检测用的 Block Tag
     */
    @Override
    protected TagKey<Block> getConflictTag() {
        return ModTags.HARVESTER_CONFLICTS;
    }

    /**
     * 储罐总容量（mB），由 Tier 决定。
     * <p>例如：Tier 0 = 8000 mB（8 桶），Tier 1 = 16000 mB（16 桶）。
     *
     * @return 储罐容量（mB）
     */
    public abstract long getTankCapacity();

    /**
     * 此机器的 Tier 等级。
     *
     * <p>每个具体机器子类<b>必须</b>覆写此方法，显式声明所属时代。
     * 与对应 Block 的 {@link AbstractMachineBlock#getTier()} 保持相同值，
     * 确保绑定逻辑与物品 tooltip 一致。
     *
     * @return 此机器的 Tier 等级
     */
    @Override
    public abstract Tier getTier();

    @Override
    public int getWorkTotalTime() {
        return CivilizationMachineConfig.getWorkTotalTime(getMachineConfigKey());
    }

    @Override
    public int getAgeIncrement() {
        return CivilizationMachineConfig.getAgeIncrement(getMachineConfigKey());
    }

    @Override
    protected abstract Component getDefaultName();

    @Override
    protected abstract AbstractContainerMenu createMenu(int i, Inventory inventory);

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
}

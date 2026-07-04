package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.api.util.SimpleWaterTank;
import com.gooodwei.civilizationevolution.server.block.machine.AbstractMachineBlock;
import com.gooodwei.civilizationevolution.server.config.CivilizationMachineConfig;
import com.gooodwei.civilizationevolution.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
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
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractHarvesterBlockEntity extends AbstractRangeMachineBlockEntity {

    /**
     * 水罐 —— 实现 {@link IFluidHandler}，仅接受水。
     *
     * <p>所有物流模组的管道均通过 {@code Capabilities.FluidHandler.BLOCK} 与此接口交互。
     * 容量由 {@link #getTankCapacity()} 动态查询。
     */
    protected final SimpleWaterTank waterTank = new SimpleWaterTank(
            this::getTankCapacity,
            this::setChanged
    );

    protected AbstractHarvesterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
    }

    public long getWaterAmount() {
        return waterTank.getWaterAmount();
    }

    public IFluidHandler getFluidHandler() {
        return waterTank;
    }

    // ==================== ContainerData ====================

    @Override
    public void executeWorkCycle(Level level) {
        this.executeWorkCyclePrelude(level);

        BlockPos pos = this.getBlockPos();
        if (level instanceof ServerLevel serverLevel && this.canWork()) {
            addApprenticeExpToPopulationSlots(getWorkerCareer(), getApprenticeExpPerCycle());

            float efficiency = calculateWorkEfficiency(getFoodPerPopulation());
            int cropCount = Math.max(1, Math.round(efficiency));
            int waterPerCrop = CivilizationMachineConfig.getWaterPerCrop(getMachineConfigKey());
            AABB range = getSelectionRange();
            int cropNum = 0;
            List<ItemStack> allDrops = new ArrayList<>();
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
                                if (waterTank.getWaterAmount() < waterPerCrop) {
                                    cropNum = cropCount; // 跳出所有循环
                                    break;
                                }
                                // 获取掉落物
                                List<ItemStack> drops = Block.getDrops(cropState, serverLevel, cropPos,
                                        serverLevel.getBlockEntity(cropPos), null, ItemStack.EMPTY);
                                serverLevel.setBlock(cropPos, cropState.setValue(CropBlock.AGE, 0), CropBlock.UPDATE_CLIENTS);
                                waterTank.setWaterAmount(waterTank.getWaterAmount() - waterPerCrop);

                                for (ItemStack drop : drops) {
                                    int newCount = Math.round(drop.getCount() * efficiency);
                                    if (newCount > 0) {
                                        allDrops.add(drop.copyWithCount(newCount));
                                    }
                                }
                                cropNum++;
                            }
                        }
                    }
                }
            }
            if (!allDrops.isEmpty()) {
                outputOrDrop(serverLevel, pos, allDrops);
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
                    case 2 -> (int) Math.min(waterTank.getWaterAmount(), Integer.MAX_VALUE);
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
        tag.putLong("WaterAmount", waterTank.getWaterAmount());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        waterTank.setWaterAmount(tag.getLong("WaterAmount"));
    }
}

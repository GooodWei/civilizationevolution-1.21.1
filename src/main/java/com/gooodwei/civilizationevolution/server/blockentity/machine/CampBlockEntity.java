package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractMachineBlockEntity;

import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import com.gooodwei.civilizationevolution.server.config.PopulationConfig;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.CampMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 营地方块的 BlockEntity。
 */
public class CampBlockEntity extends AbstractMachineBlockEntity {
    public static final int SIZE = 10;

    public CampBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.CAMP_BLOCK_ENTITY.get(), pos, blockState, SIZE);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   CampBlockEntity campBlockEntity) {
        // 营地的工作由控制器调度，无独立 tick 逻辑
    }

    @Override
    public void executeWorkCycle(Level level) {
        this.ageAllPopulations(this.getAgeIncrement());
        // 健康度波动（-10 ~ +5）
        this.fluctuateHealth(-10, 5);

        // 满足条件时执行工作
        // 营地公式：sqrt(total / 176)，176 = 16个面包总和
        if (this.canWork()) {
            double foodFactor = this.consumeAndGetFoodFactor(8,
                    total -> Math.sqrt(total / 176.0));
            this.doReproduction(level, foodFactor);
        }
        BlockPos pos = this.getBlockPos();
        BlockState state = this.getBlockState();
        setChanged(level, pos, state);
    }

    private void doReproduction(Level level, double foodFactor) {
        ItemStack parentA = getItem(4);
        ItemStack parentB = getItem(5);

        // 子嗣数量 = 双方 workEfficiency 乘积 × 食物因子，向下取整
        int count = (int) Math.floor(
                PopulationNBT.getWorkEfficiency(parentA)
                        * PopulationNBT.getWorkEfficiency(parentB)
                        * foodFactor);

        // count = 0 时不生育
        for (int i = 0; i < count; i++) {
            int slot = findEmptyOutputSlot();
            if (slot == -1) {
                break; // 槽位不足，忽略剩余子嗣
            }
            setItem(slot, reproduction(level, parentA, parentB));
        }
    }

    /**
     * 基于父母生成一个子嗣 ItemStack。
     * 性别和寿命随机，其余属性取父母平均，心理状态固定 100。
     */
    private ItemStack reproduction(Level level, ItemStack parentA, ItemStack parentB) {
        ItemStack baby = new ItemStack(parentA.getItem());
        RandomSource rand = level.getRandom();

        PopulationNBT.setAge(baby, 0);
        PopulationNBT.setLifespan(baby, rand.nextIntBetweenInclusive(
                PopulationConfig.LIFESPAN_MIN, PopulationConfig.LIFESPAN_MAX));
        PopulationNBT.setGender(baby, rand.nextBoolean());
        PopulationNBT.setCareer(baby, "unemployed");
        PopulationNBT.setHealth(baby, (
                PopulationNBT.getHealth(parentA) + PopulationNBT.getHealth(parentB)) / 2);
        PopulationNBT.setFood(baby, (
                PopulationNBT.getFood(parentA) + PopulationNBT.getFood(parentB)) / 2);
        PopulationNBT.setProficiency(baby, (
                PopulationNBT.getProficiency(parentA) + PopulationNBT.getProficiency(parentB)) / 2);
        PopulationNBT.setWorkEfficiency(baby, (
                PopulationNBT.getWorkEfficiency(parentA) + PopulationNBT.getWorkEfficiency(parentB)) / 2.0);
        PopulationNBT.setMentalState(baby, 100.0);

        return baby;
    }

    // ==================== IPopulationMachine 实现 ====================

    private int findEmptyOutputSlot() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (isOutputSlot(i) && getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getWorkTotalTime() {
        return PopulationMachineConfig.CAMP_WORK_TOTAL_TIME;
    }

    @Override
    public int getAgeIncrement() {
        return PopulationMachineConfig.CAMP_AGE_INCREMENT;
    }

    @Override
    public boolean isPopulationSlot(int slot) {
        return slot == 4 || slot == 5;
    }

    @Override
    public java.util.List<Integer> populationSlots() {
        return java.util.List.of(4, 5);
    }

    @Override
    public boolean isOutputSlot(int slot) {
        return slot >= 6;
    }

    @Override
    public boolean isFoodSlot(int slot) {
        return slot >= 0 && slot <= 3;
    }

    @Override
    public boolean canWork() {
        if (!super.canWork()) {
            return false;
        }
        // 必须双方存活
        if (isPopulationDead(4) || isPopulationDead(5)) {
            return false;
        }
        // 必须有足够食物（营地每人口消耗 8）
        if (!hasEnoughFood(8)) {
            return false;
        }
        // 营地特有：异性 + 年龄 18-50
        int ageA = getPopulationAge(4);
        int ageB = getPopulationAge(5);
        return getPopulationGender(4) != getPopulationGender(5)
                && ageA >= 18 && ageA <= 50
                && ageB >= 18 && ageB <= 50;
    }

    /**
     * 获取当前工作进度比例（0.0 ~ 1.0），供 GUI 进度条使用
     */
    public float getWorkProgressRatio() {
        return (float) workProgress / getWorkTotalTime();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.civilizationevolution.camp");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.camp");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CampMenu(containerId, inventory, this);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new CampMenu(containerId, inventory, this);
    }
}

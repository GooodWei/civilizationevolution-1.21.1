package com.gooodwei.civilizationevolution.api.util;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.function.LongSupplier;

/**
 * 简单水罐 —— 实现 {@link IFluidHandler}，仅接受水（通过 {@link FluidTags#WATER} 标签判断）。
 *
 * <p>用于 Farm、Harvester 等需要储水但不需要完整流体仓室系统的机器。
 * 消除了 {@code AbstractFarmBlockEntity} 和 {@code AbstractHarvesterBlockEntity}
 * 中 ~70 行的 IFluidHandler 匿名实现重复。
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 * public class MyMachineBlockEntity extends ... {
 *     private final SimpleWaterTank waterTank = new SimpleWaterTank(
 *         this::getTankCapacity,   // 容量（mB），由 Tier 决定
 *         this::setChanged          // 变更通知
 *     );
 *
 *     // 流体能力注册
 *     public IFluidHandler getFluidHandler() { return waterTank; }
 *
 *     // NBT 持久化
 *     tag.putLong("WaterAmount", waterTank.getWaterAmount());
 *     waterTank.setWaterAmount(tag.getLong("WaterAmount"));
 *
 *     // 业务逻辑
 *     if (waterTank.getWaterAmount() >= waterPerCrop) {
 *         waterTank.setWaterAmount(waterTank.getWaterAmount() - waterPerCrop);
 *     }
 * }
 * }</pre>
 *
 * @see IFluidHandler
 * @see AbstractFarmBlockEntity
 * @see AbstractHarvesterBlockEntity
 */
public class SimpleWaterTank implements IFluidHandler {

    /** 当前储水量（mB） */
    private long waterAmount = 0;

    /** 容量提供者（mB），通常由 Tier 决定 */
    private final LongSupplier capacitySupplier;

    /** 变更通知回调（通常调用 BE 的 setChanged()） */
    private final Runnable changeNotifier;

    /**
     * @param capacitySupplier 储罐容量提供者（mB），每次查询时动态调用
     * @param changeNotifier   数据变更回调（标记方块实体需要保存）
     */
    public SimpleWaterTank(LongSupplier capacitySupplier, Runnable changeNotifier) {
        this.capacitySupplier = capacitySupplier;
        this.changeNotifier = changeNotifier;
    }

    // ==================== 存取器 ====================

    /** 获取当前储水量（mB），供 NBT 持久化和业务逻辑使用 */
    public long getWaterAmount() {
        return waterAmount;
    }

    /** 设置当前储水量（mB），供 NBT 加载使用 */
    public void setWaterAmount(long amount) {
        this.waterAmount = Math.max(0, amount);
    }

    // ==================== IFluidHandler 实现 ====================

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        if (waterAmount <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(Fluids.WATER, toIntAmount(waterAmount));
    }

    @Override
    public int getTankCapacity(int tank) {
        long cap = capacitySupplier.getAsLong();
        return toIntAmount(cap);
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
        long capacity = capacitySupplier.getAsLong();
        if (waterAmount >= capacity) {
            return 0; // 储罐已满
        }
        long canFill = capacity - waterAmount;
        int toFill = (int) Math.min(canFill, (long) resource.getAmount());
        if (action.execute()) {
            waterAmount += toFill;
            changeNotifier.run();
        }
        return toFill;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !resource.is(FluidTags.WATER) || waterAmount <= 0) {
            return FluidStack.EMPTY;
        }
        int toDrain = Math.min(resource.getAmount(), toIntAmount(waterAmount));
        if (action.execute()) {
            waterAmount -= toDrain;
            changeNotifier.run();
        }
        return new FluidStack(Fluids.WATER, toDrain);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (waterAmount <= 0 || maxDrain <= 0) {
            return FluidStack.EMPTY;
        }
        int toDrain = Math.min(maxDrain, toIntAmount(waterAmount));
        if (action.execute()) {
            waterAmount -= toDrain;
            changeNotifier.run();
        }
        return new FluidStack(Fluids.WATER, toDrain);
    }

    // ==================== 内部工具 ====================

    /** 安全地将 long 型液量截断为 int（上限 Integer.MAX_VALUE） */
    private static int toIntAmount(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }
}

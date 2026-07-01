package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 原始流体输出接口 —— Tier 0。
 *
 * <p>内部储罐 8000 mB，仅允许外部管道提取（drain 允许），不接受外部输入（fill 返回 0）。
 */
public class PrimitiveFluidOutputHatchBlockEntity extends AbstractFluidHatchBlockEntity {

    public PrimitiveFluidOutputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    @Override
    public String getPartType() {
        return TYPE_FLUID_OUTPUT_HATCH;
    }

    @Override
    public IFluidHandler getFluidHandler() {
        return new IFluidHandler() {
            @Override
            public int getTanks() { return 1; }

            @Override
            public FluidStack getFluidInTank(int tank) {
                if (fluidAmount <= 0) return FluidStack.EMPTY;
                return new FluidStack(storedFluid, (int) Math.min(fluidAmount, Integer.MAX_VALUE));
            }

            @Override
            public int getTankCapacity(int tank) {
                return (int) Math.min(DEFAULT_CAPACITY, Integer.MAX_VALUE);
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return storedFluid == Fluids.EMPTY || stack.is(storedFluid);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource.isEmpty() || fluidAmount <= 0) return FluidStack.EMPTY;
                if (storedFluid != Fluids.EMPTY && !resource.is(storedFluid)) return FluidStack.EMPTY;
                int toDrain = Math.min(resource.getAmount(), (int) Math.min(fluidAmount, Integer.MAX_VALUE));
                if (action.execute()) {
                    fluidAmount -= toDrain;
                    if (fluidAmount <= 0) storedFluid = Fluids.EMPTY;
                    setChanged();
                }
                return new FluidStack(storedFluid != Fluids.EMPTY ? storedFluid : Fluids.WATER, toDrain);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                if (fluidAmount <= 0 || maxDrain <= 0) return FluidStack.EMPTY;
                int toDrain = Math.min(maxDrain, (int) Math.min(fluidAmount, Integer.MAX_VALUE));
                if (action.execute()) {
                    fluidAmount -= toDrain;
                    if (fluidAmount <= 0) storedFluid = Fluids.EMPTY;
                    setChanged();
                }
                return new FluidStack(storedFluid != Fluids.EMPTY ? storedFluid : Fluids.WATER, toDrain);
            }
        };
    }

    /** 供多方块控制器内部调用的填充方法 */
    public int fillInternal(FluidStack stack) {
        if (!canFill(stack)) return 0;
        long canFill = DEFAULT_CAPACITY - fluidAmount;
        if (canFill <= 0) return 0;
        int toFill = (int) Math.min(canFill, (long) stack.getAmount());
        if (storedFluid == Fluids.EMPTY) {
            storedFluid = stack.getFluid();
        }
        fluidAmount += toFill;
        setChanged();
        return toFill;
    }
}

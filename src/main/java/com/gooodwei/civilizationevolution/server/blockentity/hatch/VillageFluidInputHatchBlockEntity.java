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
 * 村庄流体输入接口 —— Tier 1。
 *
 * <p>内部储罐 8000 mB，仅接受外部管道输入，不允许外部提取。
 */
public class VillageFluidInputHatchBlockEntity extends AbstractFluidHatchBlockEntity {

    public VillageFluidInputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    public String getPartType() {
        return TYPE_FLUID_INPUT_HATCH;
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
                if (!canFill(resource)) return 0;
                long canFill = DEFAULT_CAPACITY - fluidAmount;
                if (canFill <= 0) return 0;
                int toFill = (int) Math.min(canFill, (long) resource.getAmount());
                if (action.execute()) {
                    if (storedFluid == Fluids.EMPTY) {
                        storedFluid = resource.getFluid();
                    }
                    fluidAmount += toFill;
                    setChanged();
                }
                return toFill;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                return FluidStack.EMPTY;
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                return FluidStack.EMPTY;
            }
        };
    }

    public FluidStack drainInternal(int maxDrain) {
        if (fluidAmount <= 0 || maxDrain <= 0) return FluidStack.EMPTY;
        int toDrain = Math.min(maxDrain, (int) Math.min(fluidAmount, Integer.MAX_VALUE));
        fluidAmount -= toDrain;
        if (fluidAmount <= 0) {
            storedFluid = Fluids.EMPTY;
        }
        setChanged();
        return new FluidStack(storedFluid != Fluids.EMPTY ? storedFluid : Fluids.WATER, toDrain);
    }
}

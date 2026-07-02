package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.block.machine.AbstractMachineBlock;
import com.gooodwei.civilizationevolution.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public abstract class AbstractHarvesterBlockEntity extends AbstractRanchBlockEntity {

    /**  机器中储罐容积   */
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

    /**
     * 配置文件中此机器的 section key（如 "primitive_ranch"）
     */
    @Override
    protected abstract String getMachineConfigKey();

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
    public Tier getTier() {
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return null;
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return null;
    }
}

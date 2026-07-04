package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 村庄流体输出接口 —— Tier 1。
 *
 * <p>内部储罐 8000 mB，仅允许外部管道提取，不接受外部输入。
 * IFluidHandler 实现由父类 {@link AbstractFluidHatchBlockEntity#createFluidHandler(boolean)} 提供。
 */
public class VillageFluidOutputHatchBlockEntity extends AbstractFluidHatchBlockEntity {

    public VillageFluidOutputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    public String getPartType() {
        return TYPE_FLUID_OUTPUT_HATCH;
    }

    @Override
    public IFluidHandler getFluidHandler() {
        return createFluidHandler(false);
    }
}

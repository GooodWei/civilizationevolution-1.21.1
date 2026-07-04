package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 村庄流体输入接口 —— Tier 1。
 *
 * <p>内部储罐 8000 mB，仅接受外部管道输入，不允许外部提取。
 * IFluidHandler 实现由父类 {@link AbstractFluidHatchBlockEntity#createFluidHandler(boolean)} 提供。
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
        return createFluidHandler(true);
    }
}

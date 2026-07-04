package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 原始流体输入接口 —— Tier 0。
 *
 * <p>内部储罐 8000 mB，仅接受外部管道输入（fill 允许），不允许外部提取（drain 返回空）。
 * IFluidHandler 实现由父类 {@link AbstractFluidHatchBlockEntity#createFluidHandler(boolean)} 提供。
 * 内部调用（多方块控制器、桶交互）使用父类的 {@code drainInternal} / {@code fillInternal} 绕过方向限制。
 */
public class PrimitiveFluidInputHatchBlockEntity extends AbstractFluidHatchBlockEntity {

    public PrimitiveFluidInputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
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

package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * 原始流体输出接口 —— Tier 0。
 *
 * <p>内部储罐 8000 mB，仅允许外部管道提取（drain 允许），不接受外部输入（fill 返回 0）。
 * IFluidHandler 实现由父类 {@link AbstractFluidHatchBlockEntity#createFluidHandler(boolean)} 提供。
 * 内部调用（多方块控制器、桶交互）使用父类的 {@code drainInternal} / {@code fillInternal} 绕过方向限制。
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
        return createFluidHandler(false);
    }
}

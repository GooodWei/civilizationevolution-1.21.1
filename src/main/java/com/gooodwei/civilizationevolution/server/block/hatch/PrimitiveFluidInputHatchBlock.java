package com.gooodwei.civilizationevolution.server.block.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.PrimitiveFluidInputHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 原始流体输入接口 —— Tier 0。
 *
 * <p>含内部储罐，通过 {@code IFluidHandler} 接受外部管道输入。
 */
public class PrimitiveFluidInputHatchBlock extends AbstractFluidInputHatchBlock {

    public static final MapCodec<PrimitiveFluidInputHatchBlock> CODEC =
            simpleCodec(PrimitiveFluidInputHatchBlock::new);

    public PrimitiveFluidInputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrimitiveFluidInputHatchBlockEntity(
                BlockEntityRegistry.PRIMITIVE_FLUID_INPUT_HATCH.get(), pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }
}

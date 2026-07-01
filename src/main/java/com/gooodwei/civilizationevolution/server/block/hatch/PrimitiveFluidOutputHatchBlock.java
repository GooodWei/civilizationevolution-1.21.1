package com.gooodwei.civilizationevolution.server.block.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.PrimitiveFluidOutputHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 原始流体输出接口 —— Tier 0。
 *
 * <p>含内部储罐，通过 {@code IFluidHandler} 向外部管道输出。
 */
public class PrimitiveFluidOutputHatchBlock extends AbstractFluidOutputHatchBlock {

    public static final MapCodec<PrimitiveFluidOutputHatchBlock> CODEC =
            simpleCodec(PrimitiveFluidOutputHatchBlock::new);

    public PrimitiveFluidOutputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrimitiveFluidOutputHatchBlockEntity(
                BlockEntityRegistry.PRIMITIVE_FLUID_OUTPUT_HATCH.get(), pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }
}

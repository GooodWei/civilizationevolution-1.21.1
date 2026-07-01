package com.gooodwei.civilizationevolution.server.block.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.VillageFluidOutputHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 村庄流体输出接口 —— Tier 1。
 *
 * <p>含内部储罐，通过 {@code IFluidHandler} 向外部管道输出。
 */
public class VillageFluidOutputHatchBlock extends AbstractFluidOutputHatchBlock {

    public static final MapCodec<VillageFluidOutputHatchBlock> CODEC =
            simpleCodec(VillageFluidOutputHatchBlock::new);

    public VillageFluidOutputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VillageFluidOutputHatchBlockEntity(
                BlockEntityRegistry.VILLAGE_FLUID_OUTPUT_HATCH.get(), pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.VILLAGE;
    }
}

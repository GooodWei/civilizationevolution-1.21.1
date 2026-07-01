package com.gooodwei.civilizationevolution.server.block.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.PrimitiveItemOutputHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 原始物品输出接口 —— Tier 0。
 *
 * <p>含 1 个槽位，不接受外部放入（仅代码产出）。
 */
public class PrimitiveItemOutputHatchBlock extends AbstractItemOutputHatchBlock {

    public static final MapCodec<PrimitiveItemOutputHatchBlock> CODEC =
            simpleCodec(PrimitiveItemOutputHatchBlock::new);

    public PrimitiveItemOutputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrimitiveItemOutputHatchBlockEntity(
                BlockEntityRegistry.PRIMITIVE_ITEM_OUTPUT_HATCH.get(), pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }
}

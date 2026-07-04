package com.gooodwei.civilizationevolution.server.block.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.PrimitiveItemInputHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 原始物品输入接口 —— Tier 0。
 *
 * <p>含 1 个槽位，接受任意物品。
 */
public class PrimitiveItemInputHatchBlock extends AbstractItemInputHatchBlock {

    public static final MapCodec<PrimitiveItemInputHatchBlock> CODEC =
            simpleCodec(PrimitiveItemInputHatchBlock::new);

    public PrimitiveItemInputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrimitiveItemInputHatchBlockEntity(
                BlockEntityRegistry.PRIMITIVE_ITEM_INPUT_HATCH.get(), pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }
}

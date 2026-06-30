package com.gooodwei.civilizationevolution.server.block.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.PrimitiveFoodInputHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 原始食物输入接口 —— Tier 0。
 *
 * <p>含 1 个槽位，仅接受食物物品。
 * （Tier 0 诊所暂不消耗食物，预留供后续高级医院使用）
 */
public class PrimitiveFoodInputHatchBlock extends AbstractFoodInputHatchBlock {

    public static final MapCodec<PrimitiveFoodInputHatchBlock> CODEC =
            simpleCodec(PrimitiveFoodInputHatchBlock::new);

    public PrimitiveFoodInputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrimitiveFoodInputHatchBlockEntity(
                BlockEntityRegistry.PRIMITIVE_FOOD_INPUT_HATCH.get(), pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }
}

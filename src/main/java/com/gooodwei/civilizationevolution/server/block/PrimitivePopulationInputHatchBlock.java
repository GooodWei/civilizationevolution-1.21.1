package com.gooodwei.civilizationevolution.server.block;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.PrimitivePopulationInputHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 原始人口输入接口 —— Tier 0。
 *
 * <p>含 1 个槽位，仅接受人口物品。
 * 多方块控制器通过此接口拉取待治疗的人口。
 */
public class PrimitivePopulationInputHatchBlock extends AbstractPopulationInputHatchBlock {

    public static final MapCodec<PrimitivePopulationInputHatchBlock> CODEC =
            simpleCodec(PrimitivePopulationInputHatchBlock::new);

    public PrimitivePopulationInputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrimitivePopulationInputHatchBlockEntity(
                BlockEntityRegistry.PRIMITIVE_POPULATION_INPUT_HATCH.get(), pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }
}

package com.gooodwei.civilizationevolution.server.block.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.PrimitivePopulationOutputHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 原始人口输出接口 —— Tier 0。
 *
 * <p>含 1 个槽位，不接受手动放入（仅代码产出）。
 * 多方块控制器将治疗后健康度超阈值的人口推送到此接口。
 */
public class PrimitivePopulationOutputHatchBlock extends AbstractPopulationOutputHatchBlock {

    public static final MapCodec<PrimitivePopulationOutputHatchBlock> CODEC =
            simpleCodec(PrimitivePopulationOutputHatchBlock::new);

    public PrimitivePopulationOutputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrimitivePopulationOutputHatchBlockEntity(
                BlockEntityRegistry.PRIMITIVE_POPULATION_OUTPUT_HATCH.get(), pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }
}

package com.gooodwei.civilizationevolution.server.block.hatch;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 人口输入接口的抽象方块类。
 *
 * <p>每个输入接口含 1 个槽位，仅接受 {@code PopulationItem}。
 * 多方块控制器通过接口拉取人口物品填充治疗槽位。
 * 子类只需覆写 {@link #getPartTier()} 和 {@link #newBlockEntity}。
 */
public abstract class AbstractPopulationInputHatchBlock extends AbstractHatchBlock {

    protected AbstractPopulationInputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Override
    public String getPartType() {
        return TYPE_POPULATION_INPUT_HATCH;
    }
}

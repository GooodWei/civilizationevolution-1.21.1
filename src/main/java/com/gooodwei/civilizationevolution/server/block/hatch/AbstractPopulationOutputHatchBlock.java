package com.gooodwei.civilizationevolution.server.block.hatch;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 人口输出接口的抽象方块类。
 *
 * <p>每个输出接口含 1 个槽位，拒绝手动放入物品（仅代码产出）。
 * 多方块控制器将治疗后健康度超阈值的人口物品推送到此接口。
 * 子类只需覆写 {@link #getPartTier()} 和 {@link #newBlockEntity}。
 */
public abstract class AbstractPopulationOutputHatchBlock extends AbstractHatchBlock {

    protected AbstractPopulationOutputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Override
    public String getPartType() {
        return TYPE_OUTPUT_HATCH;
    }
}

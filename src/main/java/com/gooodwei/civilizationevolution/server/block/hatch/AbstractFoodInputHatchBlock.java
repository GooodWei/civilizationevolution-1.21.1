package com.gooodwei.civilizationevolution.server.block.hatch;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 食物输入接口的抽象方块类。
 *
 * <p>每个食物输入接口含 1 个槽位，仅接受食物物品。
 * 多方块控制器通过此接口消耗食物（预留，Tier 0 诊所暂不消耗食物）。
 * 子类只需覆写 {@link #getPartTier()} 和 {@link #newBlockEntity}。
 */
public abstract class AbstractFoodInputHatchBlock extends AbstractHatchBlock {

    protected AbstractFoodInputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Override
    public String getPartType() {
        return TYPE_FOOD_HATCH;
    }
}

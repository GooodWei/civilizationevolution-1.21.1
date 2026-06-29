package com.gooodwei.civilizationevolution.server.block;

import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 人口输出接口的抽象基类。
 *
 * <p>每个输出接口含 1 个槽位，不接受玩家/漏斗放入（仅代码产出）。
 * 多方块控制器将治疗后健康度超阈值的人口推送到此接口。
 * 子类只需覆写 {@link #getPartTier()} 和 {@link #newBlockEntity}。
 */
public abstract class AbstractPopulationOutputHatchBlock extends BaseEntityBlock implements IMultiBlockPart {

    protected AbstractPopulationOutputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            notifyNearbyControllers(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void notifyNearbyControllers(Level level, BlockPos brokenPos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    mutable.set(brokenPos.getX() + dx, brokenPos.getY() + dy, brokenPos.getZ() + dz);
                    if (level.getBlockEntity(mutable) instanceof com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractMultiBlockMachineBlockEntity controller) {
                        controller.onStructurePartBroken(brokenPos);
                    }
                }
            }
        }
    }
}

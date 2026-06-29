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
 * 人口输入接口的抽象基类。
 *
 * <p>每个输入接口含 1 个槽位，仅接受 {@code PopulationItem}。
 * 多方块控制器通过接口拉取人口物品填充治疗槽位。
 * 子类只需覆写 {@link #getPartTier()} 和 {@link #newBlockEntity}。
 */
public abstract class AbstractPopulationInputHatchBlock extends BaseEntityBlock implements IMultiBlockPart {

    protected AbstractPopulationInputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    /**
     * 被破坏时通知附近的多方块控制器。
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            notifyNearbyControllers(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * 扫描附近 ±3 格范围内的多方块控制器并通知结构破坏。
     */
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

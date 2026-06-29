package com.gooodwei.civilizationevolution.server.block;

import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractMultiBlockMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 所有多方块结构零件的抽象基类。
 *
 * <p>不携带 BlockEntity（纯结构方块），{@link IMultiBlockPart#getPartTier()}
 * 由子类覆写返回所在时代的 Tier 常量。
 * 被破坏时自动扫描周围寻找控制器并通知结构破坏。
 *
 * <p>对于需要方块实体的零件（输入/输出接口），应在对应的
 * 抽象中层类（如 {@code AbstractPopulationInputHatchBlock}）中通过 BlockEntity 实现。
 *
 * @see IMultiBlockPart
 * @see AbstractMultiBlockMachineBlockEntity#onStructurePartBroken(BlockPos)
 */
public abstract class AbstractMultiBlockPart extends Block implements IMultiBlockPart {

    protected AbstractMultiBlockPart(Properties properties) {
        super(properties);
    }

    /**
     * 方块被移除时，通知附近的多方块控制器结构已破坏。
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide() && !state.is(newState.getBlock())) {
            // 扫描周围 3 格寻找多方块控制器
            notifyNearbyControllers(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /**
     * 扫描附近 ±3 格范围内的多方块控制器并通知结构破坏。
     *
     * @param level    世界
     * @param brokenPos 被破坏的零件位置
     */
    private void notifyNearbyControllers(Level level, BlockPos brokenPos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -3; dy <= 3; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    mutable.set(brokenPos.getX() + dx, brokenPos.getY() + dy, brokenPos.getZ() + dz);
                    BlockEntity be = level.getBlockEntity(mutable);
                    if (be instanceof AbstractMultiBlockMachineBlockEntity controller) {
                        controller.onStructurePartBroken(brokenPos);
                    }
                }
            }
        }
    }
}

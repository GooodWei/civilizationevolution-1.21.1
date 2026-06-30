package com.gooodwei.civilizationevolution.server.block.part;

import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 所有多方块结构零件的抽象基类。
 *
 * <p>不携带 BlockEntity（纯结构方块），{@link IMultiBlockPart#getPartTier()}
 * 由子类覆写返回所在时代的 Tier 常量。
 * 结构验证仅由控制器在放置、破坏及定时检测时触发。
 *
 * <p>对于需要方块实体的零件（输入/输出接口），应在对应的
 * 抽象中层类（如 {@code AbstractPopulationInputHatchBlock}）中通过 BlockEntity 实现。
 *
 * @see IMultiBlockPart
 */
public abstract class AbstractMultiBlockPart extends Block implements IMultiBlockPart {

    protected AbstractMultiBlockPart(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}

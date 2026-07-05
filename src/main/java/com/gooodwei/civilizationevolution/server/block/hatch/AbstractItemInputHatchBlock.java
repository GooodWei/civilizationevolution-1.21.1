package com.gooodwei.civilizationevolution.server.block.hatch;

/**
 * 物品输入接口的抽象基类。
 *
 * <p>每个物品输入接口含 1 个槽位，接受任意物品。
 * 多方块控制器可通过此接口拉取物品（工具、燃料等）。
 * 子类只需覆写 {@link #codec()}、{@link #newBlockEntity} 和 {@link #getPartTier()}。
 */
public abstract class AbstractItemInputHatchBlock extends AbstractHatchBlock {

    protected AbstractItemInputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public String getPartType() {
        return TYPE_ITEM_INPUT_HATCH;
    }
}

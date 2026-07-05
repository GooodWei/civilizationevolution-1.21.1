package com.gooodwei.civilizationevolution.server.block.hatch;

/**
 * 物品输出接口的抽象基类。
 *
 * <p>每个物品输出接口含 1 个槽位，不接受外部放入（仅代码产出）。
 * 多方块控制器将产物推送到此接口供玩家/漏斗提取。
 * 子类只需覆写 {@link #codec()}、{@link #newBlockEntity} 和 {@link #getPartTier()}。
 */
public abstract class AbstractItemOutputHatchBlock extends AbstractHatchBlock {

    protected AbstractItemOutputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public String getPartType() {
        return TYPE_ITEM_OUTPUT_HATCH;
    }
}

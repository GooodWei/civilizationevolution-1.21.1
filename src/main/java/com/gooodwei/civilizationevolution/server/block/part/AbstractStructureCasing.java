package com.gooodwei.civilizationevolution.server.block.part;

/**
 * 多方块结构填充外壳的抽象基类。
 *
 * <p>纯结构方块（无 BlockEntity），仅作为多方块结构的填充材料。
 * 子类只需覆写 {@link #getPartTier()} 返回对应时代的 Tier。
 */
public abstract class AbstractStructureCasing extends AbstractMultiBlockPart {

    protected AbstractStructureCasing(Properties properties) {
        super(properties);
    }
}

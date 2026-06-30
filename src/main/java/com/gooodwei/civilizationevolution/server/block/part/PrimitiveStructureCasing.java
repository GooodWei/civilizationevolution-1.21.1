package com.gooodwei.civilizationevolution.server.block.part;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;

/**
 * 原始结构方块 —— Tier 0 多方块结构填充外壳。
 *
 * <p>纯粹的装饰/填充方块（无 BlockEntity），
 * 用于构建原始时代的医院等多方块机器结构。
 */
public class PrimitiveStructureCasing extends AbstractStructureCasing {

    public PrimitiveStructureCasing(Properties properties) {
        super(properties);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }
}

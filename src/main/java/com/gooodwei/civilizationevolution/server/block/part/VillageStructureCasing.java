package com.gooodwei.civilizationevolution.server.block.part;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;

/**
 * 村庄结构方块 —— Tier 1 多方块结构填充外壳。
 *
 * <p>纯粹的装饰/填充方块（无 BlockEntity），
 * 用于构建村庄时代的多方块机器结构。
 */
public class VillageStructureCasing extends AbstractStructureCasing {

    public VillageStructureCasing(Properties properties) {
        super(properties);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.VILLAGE;
    }
}

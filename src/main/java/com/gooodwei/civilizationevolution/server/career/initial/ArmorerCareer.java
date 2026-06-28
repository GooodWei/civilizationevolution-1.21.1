package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 盔甲匠职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#ARMORER}，负责制作和交易盔甲。
 */
public class ArmorerCareer extends Career {
    public ArmorerCareer() {
        super("armorer", 1, VillagerProfession.ARMORER);
    }
}

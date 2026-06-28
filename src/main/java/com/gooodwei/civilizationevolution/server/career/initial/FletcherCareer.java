package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 制箭师职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#FLETCHER}，负责弓箭和弩箭交易。
 */
public class FletcherCareer extends Career {
    public FletcherCareer() {
        super("fletcher", 1, VillagerProfession.FLETCHER);
    }
}

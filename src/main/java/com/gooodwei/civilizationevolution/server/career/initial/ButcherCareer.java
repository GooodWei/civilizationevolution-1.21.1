package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 屠夫职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#BUTCHER}，负责肉类加工和交易。
 */
public class ButcherCareer extends Career {
    public ButcherCareer() {
        super("butcher", 1, VillagerProfession.BUTCHER);
    }
}

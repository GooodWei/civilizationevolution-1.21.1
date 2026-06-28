package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 牧羊人职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#SHEPHERD}，负责羊毛和染色类交易。
 */
public class ShepherdCareer extends Career {
    public ShepherdCareer() {
        super("shepherd", 1, VillagerProfession.SHEPHERD);
    }
}

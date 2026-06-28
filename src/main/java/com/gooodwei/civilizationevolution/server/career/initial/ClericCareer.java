package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 牧师职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#CLERIC}，负责魔法物品和药水交易。
 */
public class ClericCareer extends Career {
    public ClericCareer() {
        super("cleric", 1, VillagerProfession.CLERIC);
    }
}

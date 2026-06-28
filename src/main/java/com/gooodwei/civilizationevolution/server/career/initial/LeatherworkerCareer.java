package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 皮匠职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#LEATHERWORKER}，负责皮革制品交易。
 */
public class LeatherworkerCareer extends Career {
    public LeatherworkerCareer() {
        super("leatherworker", 1, VillagerProfession.LEATHERWORKER);
    }
}

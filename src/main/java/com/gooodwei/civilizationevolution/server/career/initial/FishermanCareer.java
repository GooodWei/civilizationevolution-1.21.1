package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 渔夫职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#FISHERMAN}，负责捕鱼和鱼类交易。
 */
public class FishermanCareer extends Career {
    public FishermanCareer() {
        super("fisherman", 1, VillagerProfession.FISHERMAN);
    }
}

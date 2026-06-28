package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 农民职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#FARMER}，负责农作物种植和食物交易。
 */
public class FarmerCareer extends Career {
    public FarmerCareer() {
        super("farmer", 1, VillagerProfession.FARMER);
    }
}

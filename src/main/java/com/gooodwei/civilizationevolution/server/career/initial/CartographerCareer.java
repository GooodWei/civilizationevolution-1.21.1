package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 制图师职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#CARTOGRAPHER}，负责地图和探险物品交易。
 */
public class CartographerCareer extends Career {
    public CartographerCareer() {
        super("cartographer", 1, VillagerProfession.CARTOGRAPHER);
    }
}

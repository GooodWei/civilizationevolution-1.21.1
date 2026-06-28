package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 石匠职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#MASON}，负责石质建材和装饰方块交易。
 */
public class MasonCareer extends Career {
    public MasonCareer() {
        super("mason", 1, VillagerProfession.MASON);
    }
}

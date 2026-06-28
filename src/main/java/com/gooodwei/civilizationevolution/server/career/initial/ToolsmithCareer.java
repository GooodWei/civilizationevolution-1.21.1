package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 工具匠职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#TOOLSMITH}，负责工具类（镐、斧、锹、锄）交易。
 */
public class ToolsmithCareer extends Career {
    public ToolsmithCareer() {
        super("toolsmith", 1, VillagerProfession.TOOLSMITH);
    }
}

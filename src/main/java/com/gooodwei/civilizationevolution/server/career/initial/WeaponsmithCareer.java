package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 武器匠职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#WEAPONSMITH}，负责武器类（剑、斧）交易。
 */
public class WeaponsmithCareer extends Career {
    public WeaponsmithCareer() {
        super("weaponsmith", 1, VillagerProfession.WEAPONSMITH);
    }
}

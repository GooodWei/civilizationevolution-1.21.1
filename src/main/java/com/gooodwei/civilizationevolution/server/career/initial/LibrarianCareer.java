package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

/**
 * 图书管理员职业（1 级初始职业）。
 * 对应原版 {@link VillagerProfession#LIBRARIAN}，负责附魔书和书架类交易。
 */
public class LibrarianCareer extends Career {
    public LibrarianCareer() {
        super("librarian", 1, VillagerProfession.LIBRARIAN);
    }
}

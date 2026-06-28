package com.gooodwei.civilizationevolution.api.event;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

/**
 * 人口死亡事件，在 {@code Population.addAge()} 判定人口死亡时发出。
 * 附属模组可监听此事件处理人口死亡后的逻辑（如记录统计、掉落物品等）。
 */
public class PopulationDeathEvent extends Event {

    private final ItemStack populationStack;

    /**
     * 构造人口死亡事件。
     * @param populationStack 已死亡的人口 ItemStack（此时年龄 >= 寿命）
     */
    public PopulationDeathEvent(ItemStack populationStack) {
        this.populationStack = populationStack;
    }

    /** @return 死亡的人口物品（年龄已达或超过寿命，物品即将被销毁） */
    public ItemStack getPopulationStack() {
        return populationStack;
    }
}

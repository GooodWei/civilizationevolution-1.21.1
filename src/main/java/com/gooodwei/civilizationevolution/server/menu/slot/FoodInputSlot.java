package com.gooodwei.civilizationevolution.server.menu.slot;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 食物输入槽位 —— 仅接受含有 {@link net.minecraft.core.component.DataComponents#FOOD} 组件的物品。
 *
 * <p>用于营地、狩猎场等机器的食物输入区域，确保只能放入可食用的物品。</p>
 */
public class FoodInputSlot extends Slot {

    /**
     * @param container 底层容器
     * @param slot      槽位索引
     * @param x         GUI 中的 X 坐标
     * @param y         GUI 中的 Y 坐标
     */
    public FoodInputSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    /**
     * 判断物品是否可放入此槽位。
     *
     * @param stack 待放入的物品堆
     * @return 仅当物品含有 {@code FOOD} 组件时返回 {@code true}
     */
    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }
}

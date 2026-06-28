package com.gooodwei.civilizationevolution.server.menu.slot;

import com.gooodwei.civilizationevolution.server.item.PopulationItem;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 人口物品槽位 —— 仅接受 {@link com.gooodwei.civilizationevolution.server.item.PopulationItem}。
 *
 * <p>用于营地、狩猎场等机器的人口安置区域，确保只能放入人口物品。
 * 不允许 Shift+放入非人口物品。</p>
 */
public class PopulationItemSlot extends Slot {

    /**
     * @param container 底层容器
     * @param slot      槽位索引
     * @param x         GUI 中的 X 坐标
     * @param y         GUI 中的 Y 坐标
     */
    public PopulationItemSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    /**
     * 判断物品是否可放入此槽位。
     *
     * @param stack 待放入的物品堆
     * @return 仅当物品为 {@link com.gooodwei.civilizationevolution.server.item.PopulationItem} 实例时返回 {@code true}
     */
    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getItem() instanceof PopulationItem;
    }
}

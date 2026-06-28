package com.gooodwei.civilizationevolution.server.menu.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 机器产物输出槽位 —— 拒绝任何手动放入操作。
 *
 * <p>仅允许通过代码（如机器的产出逻辑）向此槽位放置物品。
 * 在 Shift+点击快速移动时也会被自动跳过（因为 {@code mayPlace} 始终返回 {@code false}）。</p>
 */
public class PopulationMachineResultSlot extends Slot {

    /**
     * @param container 底层容器
     * @param slot      槽位索引
     * @param x         GUI 中的 X 坐标
     * @param y         GUI 中的 Y 坐标
     */
    public PopulationMachineResultSlot(Container container, int slot, int x, int y) {
        super(container, slot, x, y);
    }

    /**
     * 始终拒绝手动放入。
     *
     * @param stack 待放入的物品堆
     * @return 始终返回 {@code false}
     */
    @Override
    public boolean mayPlace(ItemStack stack) {
        return false;
    }
}

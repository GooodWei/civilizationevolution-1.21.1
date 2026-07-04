package com.gooodwei.civilizationevolution.server.menu.slot;

import com.gooodwei.civilizationevolution.api.inventory.OversizedStackHandler;
import net.minecraft.world.item.ItemStack;

/**
 * 储物坑类型存储槽位，在 {@link OversizedSlot} 基础上追加类型锁定逻辑。
 *
 * <p>核心规则：每个槽位只能存放一种物品类型。空槽位可放入任意物品，
 * 已有物品的槽位仅接受同种物品（通过 {@link ItemStack#isSameItemSameComponents} 比较）。
 *
 * <p>所有容量计算（getMaxStackSize、safeInsert、tryRemove、getRealCount）
 * 均由父类 {@link OversizedSlot} 提供。
 */
public class StoragePitSlot extends OversizedSlot {

    /**
     * @param handler   超大堆叠物品栈管理器
     * @param slotIndex 槽位索引（handler 中的 index）
     * @param x         GUI 中 X 坐标
     * @param y         GUI 中 Y 坐标
     */
    public StoragePitSlot(OversizedStackHandler handler, int slotIndex, int x, int y) {
        super(handler, slotIndex, x, y);
    }

    /**
     * 类型锁定：空槽位可放入任意物品；已有物品的槽位仅接受同种物品。
     */
    @Override
    public boolean mayPlace(ItemStack stack) {
        ItemStack existing = getItem(); // 来自 SlotItemHandler，返回真实 ItemStack
        return existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack);
    }
}

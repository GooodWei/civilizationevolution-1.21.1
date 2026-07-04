package com.gooodwei.civilizationevolution.api.util;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 超大堆叠物品栈在 Menu 中的通用操作工具。
 *
 * <p>照搬 Sophisticated Storage {@code mergeItemStack} 的核心逻辑：
 * 手动遍历目标槽位范围转移物品，上限由调用方决定，
 * 最后用 {@code sourceSlot.set(remaining)} 把剩余物品设回源槽位。
 *
 * <p>典型用法（在 {@code quickMoveStack} 中）：
 * <pre>{@code
 *   int maxExtract = Math.min(slot.getItem().getCount(), slot.getItem().getMaxStackSize());
 *   ItemStack remaining = OversizedMenuHelper.mergeToPlayerInventory(
 *       this, PLAYER_INV_START, PLAYER_HOTBAR_END, slot.getItem().copy(), maxExtract);
 *   slot.set(remaining);
 * }</pre>
 */
public final class OversizedMenuHelper {

    private OversizedMenuHelper() {}

    /**
     * 在指定槽位范围内转移物品，上限为 {@code maxTransfer}。
     *
     * <p>两遍扫描：
     * <ol>
     *   <li>合并到已有同种物品的槽位（{@code targetStack.grow(toMove)}）</li>
     *   <li>放入空槽位（{@code targetSlot.set(placed)}）</li>
     * </ol>
     *
     * @param menu         容器菜单（用于 {@code getSlot()}）
     * @param rangeStart   目标槽位范围起始索引（含）
     * @param rangeEnd     目标槽位范围结束索引（含）
     * @param sourceStack  源物品栈（会被 {@code shrink()} 修改）
     * @param maxTransfer  最多转移数量
     * @return 剩余未转移的物品栈（即传入的 sourceStack 对象）
     */
    public static ItemStack mergeToPlayerInventory(AbstractContainerMenu menu,
                                                    int rangeStart, int rangeEnd,
                                                    ItemStack sourceStack, int maxTransfer) {
        if (sourceStack.isEmpty() || maxTransfer <= 0) return sourceStack;

        int toTransfer = Math.min(sourceStack.getCount(), maxTransfer);

        // 第一遍：合并到已有同种物品的槽位
        for (int i = rangeStart; i <= rangeEnd && toTransfer > 0; i++) {
            Slot targetSlot = menu.getSlot(i);
            ItemStack targetStack = targetSlot.getItem();
            if (!targetStack.isEmpty()
                    && ItemStack.isSameItemSameComponents(targetStack, sourceStack)
                    && targetSlot.mayPlace(sourceStack)) {
                int canFit = targetSlot.getMaxStackSize(targetStack) - targetStack.getCount();
                int toMove = Math.min(toTransfer, canFit);
                if (toMove > 0) {
                    targetStack.grow(toMove);
                    toTransfer -= toMove;
                    targetSlot.setChanged();
                }
            }
        }

        // 第二遍：放入空槽位
        for (int i = rangeStart; i <= rangeEnd && toTransfer > 0; i++) {
            Slot targetSlot = menu.getSlot(i);
            if (!targetSlot.hasItem() && targetSlot.mayPlace(sourceStack)) {
                int canFit = targetSlot.getMaxStackSize(sourceStack);
                int toMove = Math.min(toTransfer, canFit);
                if (toMove > 0) {
                    targetSlot.set(sourceStack.copyWithCount(toMove));
                    toTransfer -= toMove;
                }
            }
        }

        int moved = Math.min(sourceStack.getCount(), maxTransfer) - toTransfer;
        if (moved > 0) {
            sourceStack.shrink(moved);
        }
        return sourceStack;
    }
}

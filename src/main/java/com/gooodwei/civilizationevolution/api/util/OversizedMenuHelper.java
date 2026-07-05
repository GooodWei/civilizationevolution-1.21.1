package com.gooodwei.civilizationevolution.api.util;

import com.gooodwei.civilizationevolution.server.menu.slot.OversizedSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
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
 *
 * <p>超大槽位点击处理（在 {@code clicked()} 中）：
 * <pre>{@code
 *   if (OversizedMenuHelper.handleOversizedSlotClick(this, slotId, dragType, clickType, player)) {
 *       return;
 *   }
 *   super.clicked(slotId, dragType, clickType, player);
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

    // ==================== 超大槽位点击处理 ====================

    /**
     * 处理 {@link AbstractContainerMenu#clicked} 中对 {@link OversizedSlot} 的交互。
     *
     * <p>覆盖三种点击类型：
     * <ul>
     *   <li><b>SWAP（数字键）</b>——快捷栏与超大槽位交换物品</li>
     *   <li><b>QUICK_MOVE（Shift+点击）</b>——委托给 {@code menu.quickMoveStack()}</li>
     *   <li><b>PICKUP（左/右键）</b>——从超大槽位提取/向超大槽位插入，单次最多取一个 itemMaxStackSize</li>
     * </ul>
     *
     * <p>典型用法：
     * <pre>{@code
     * @Override
     * public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
     *     if (OversizedMenuHelper.handleOversizedSlotClick(this, slotId, dragType, clickType, player)) {
     *         return;
     *     }
     *     super.clicked(slotId, dragType, clickType, player);
     * }
     * }</pre>
     *
     * @param menu      容器菜单
     * @param slotId    被点击的槽位索引
     * @param dragType  拖拽类型（PICKUP: 0=左键, 1=右键；SWAP: 快捷栏槽位索引）
     * @param clickType 点击类型
     * @param player    操作玩家
     * @return true 表示已处理（调用方应直接 return），false 表示应回退到 super.clicked()
     */
    public static boolean handleOversizedSlotClick(AbstractContainerMenu menu, int slotId,
                                                    int dragType, ClickType clickType, Player player) {
        if (slotId < 0) return false;

        Slot slot = menu.getSlot(slotId);
        if (!(slot instanceof OversizedSlot)) return false;

        // ===== SWAP（数字键）：快捷栏 ↔ 超大槽位 =====
        if (clickType == ClickType.SWAP) {
            Inventory inv = player.getInventory();
            ItemStack hotbarStack = inv.getItem(dragType);
            ItemStack slotStack = slot.getItem();

            if (hotbarStack.isEmpty() && !slotStack.isEmpty()) {
                // 快捷栏空、槽位有物品 → 移入快捷栏
                if (slot.mayPickup(player)) {
                    if (slotStack.getCount() <= slotStack.getMaxStackSize()) {
                        inv.setItem(dragType, slotStack.copy());
                        slot.set(ItemStack.EMPTY);
                        slot.onTake(player, slotStack);
                    } else {
                        inv.setItem(dragType, slotStack.copyWithCount(slotStack.getMaxStackSize()));
                        slot.set(slotStack.copyWithCount(slotStack.getCount() - slotStack.getMaxStackSize()));
                    }
                }
            } else if (!hotbarStack.isEmpty() && slotStack.isEmpty()) {
                // 快捷栏有物品、槽位空 → 移入槽位
                if (slot.mayPlace(hotbarStack)) {
                    int limit = slot.getMaxStackSize(hotbarStack);
                    if (hotbarStack.getCount() > limit) {
                        slot.set(hotbarStack.split(limit));
                    } else {
                        slot.set(hotbarStack.copy());
                        inv.setItem(dragType, ItemStack.EMPTY);
                    }
                }
            } else if (!hotbarStack.isEmpty() && !slotStack.isEmpty()) {
                // 两者都有物品 → 尝试交换
                if (slotStack.getCount() <= slotStack.getMaxStackSize()
                        && slot.mayPickup(player) && slot.mayPlace(hotbarStack)) {
                    int limit = slot.getMaxStackSize(hotbarStack);
                    if (hotbarStack.getCount() > limit) {
                        slot.set(hotbarStack.split(limit));
                        slot.onTake(player, slotStack);
                        if (!inv.add(slotStack)) {
                            player.drop(slotStack, true);
                        }
                    } else {
                        ItemStack slotCopy = slotStack.copy();
                        slot.set(hotbarStack.copy());
                        inv.setItem(dragType, slotCopy);
                        slot.onTake(player, slotCopy);
                    }
                }
            }
            slot.setChanged();
            return true;
        }

        // ===== QUICK_MOVE（Shift+点击）：委托给菜单的 quickMoveStack =====
        if (clickType == ClickType.QUICK_MOVE) {
            if (slot.mayPickup(player)) {
                menu.quickMoveStack(player, slotId);
            }
            return true;
        }

        // ===== PICKUP（左/右键）：插入/提取 =====
        if (clickType == ClickType.PICKUP && (dragType == 0 || dragType == 1)) {
            boolean isPrimary = dragType == 0;
            ItemStack carriedStack = menu.getCarried();
            ItemStack slotStack = slot.getItem();

            if (slotStack.isEmpty()) {
                // 槽位空 → 放入
                if (!carriedStack.isEmpty()) {
                    int count = isPrimary ? carriedStack.getCount() : 1;
                    menu.setCarried(slot.safeInsert(carriedStack, count));
                }
            } else if (slot.mayPickup(player)) {
                if (carriedStack.isEmpty()) {
                    // 手上空 → 取出
                    int toRemove = Math.min(slotStack.getCount(), slotStack.getMaxStackSize());
                    if (!isPrimary) {
                        toRemove = toRemove / 2 + toRemove % 2;
                    }
                    var extracted = slot.tryRemove(toRemove, Integer.MAX_VALUE, player);
                    extracted.ifPresent(s -> {
                        menu.setCarried(s);
                        slot.onTake(player, s);
                    });
                } else if (slot.mayPlace(carriedStack)) {
                    // 手上有同种物品 → 尝试放入
                    if (ItemStack.isSameItemSameComponents(slotStack, carriedStack)) {
                        int count = isPrimary ? carriedStack.getCount() : 1;
                        menu.setCarried(slot.safeInsert(carriedStack, count));
                    } else if (carriedStack.getCount() <= slot.getMaxStackSize(carriedStack)
                            && slotStack.getCount() <= slotStack.getMaxStackSize()) {
                        // 不同种物品且双方都在单组范围内 → 交换
                        slot.set(carriedStack.copy());
                        menu.setCarried(slotStack.copy());
                        slot.setChanged();
                        return true;
                    }
                } else if (!ItemStack.isSameItemSameComponents(slotStack, carriedStack)
                        && carriedStack.getCount() <= slot.getMaxStackSize(carriedStack)
                        && slotStack.getCount() <= slotStack.getMaxStackSize()) {
                    // 手上物品不可放入但双方都在单组范围内 → 交换
                    slot.set(carriedStack.copy());
                    menu.setCarried(slotStack.copy());
                    slot.setChanged();
                    return true;
                } else if (ItemStack.isSameItemSameComponents(slotStack, carriedStack)) {
                    // 同种物品但 mayPlace=false（槽位满或类型限制） → 尝试取出到手上
                    var extracted = slot.tryRemove(slotStack.getCount(),
                            carriedStack.getMaxStackSize() - carriedStack.getCount(), player);
                    extracted.ifPresent(s -> {
                        carriedStack.grow(s.getCount());
                        slot.onTake(player, s);
                    });
                }
            }
            slot.setChanged();
            return true;
        }

        return false;
    }

    // ==================== 超大槽位 Shift+点击转移 ====================

    /**
     * 在 {@code quickMoveStack} 中处理 OversizedSlot → 玩家背包的转移。
     *
     * <p>一次最多移动一个 itemMaxStackSize（通常 64），防止从超大槽位
     * 一次取出过多物品。转移逻辑委托给 {@link #mergeToPlayerInventory}。
     *
     * <p>典型用法：
     * <pre>{@code
     * } else if (slot instanceof OversizedSlot) {
     *     if (!OversizedMenuHelper.quickMoveFromOversizedSlot(
     *             this, slot, PLAYER_INV_START, PLAYER_HOTBAR_END)) {
     *         return ItemStack.EMPTY;
     *     }
     *     return ItemStack.EMPTY;
     * }
     * }</pre>
     *
     * @param menu        容器菜单
     * @param slot        超大槽位
     * @param playerStart 玩家背包范围起始索引（含）
     * @param playerEnd   玩家背包范围结束索引（含）
     * @return true 表示至少移动了 1 个物品，false 表示无物品被移动
     */
    public static boolean quickMoveFromOversizedSlot(AbstractContainerMenu menu, Slot slot,
                                                      int playerStart, int playerEnd) {
        ItemStack stackInSlot = slot.getItem();
        if (stackInSlot.isEmpty()) return false;

        int maxExtract = Math.min(stackInSlot.getCount(), stackInSlot.getMaxStackSize());
        ItemStack copy = stackInSlot.copy();
        ItemStack remaining = mergeToPlayerInventory(menu, playerStart, playerEnd, copy, maxExtract);
        slot.set(remaining);
        return remaining.getCount() != stackInSlot.getCount();
    }
}

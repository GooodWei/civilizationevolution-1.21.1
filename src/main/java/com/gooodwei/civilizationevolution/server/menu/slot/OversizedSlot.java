package com.gooodwei.civilizationevolution.server.menu.slot;

import com.gooodwei.civilizationevolution.api.inventory.OversizedStackHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.Optional;

/**
 * 支持超大堆叠的通用槽位，参照 SophisticatedCore {@code StorageInventorySlot} 设计。
 *
 * <p>继承 {@link SlotItemHandler}，通过 {@link OversizedStackHandler} 获取容量，
 * 使 count > 99 的 ItemStack 能在 GUI 中正确交互。
 *
 * <p>继承层次：
 * <pre>
 * SS:  Slot → SlotItemHandler → SlotSuppliedHandler → StorageInventorySlot
 * 我们: Slot → SlotItemHandler → OversizedSlot → StoragePitSlot（追加 mayPlace 类型锁定）
 * </pre>
 *
 * <p>子类只需覆写 {@link #mayPlace(ItemStack)} 即可实现自定义类型锁定逻辑。
 */
public class OversizedSlot extends SlotItemHandler {
    private final OversizedStackHandler handler;
    private final int slotIndex;

    /**
     * @param handler   超大堆叠物品栈管理器
     * @param slotIndex 槽位索引
     * @param x         GUI 中 X 坐标
     * @param y         GUI 中 Y 坐标
     */
    public OversizedSlot(OversizedStackHandler handler, int slotIndex, int x, int y) {
        super(handler, slotIndex, x, y);
        this.handler = handler;
        this.slotIndex = slotIndex;
    }

    // ==================== 容量委托给 OversizedStackHandler ====================

    /** 槽位绝对容量上限（例如 1728 = 27 组） */
    @Override
    public int getMaxStackSize() {
        return handler.getSlotLimit(slotIndex);
    }

    /** 指定物品的堆叠上限（照搬 SS {@code InventoryHandler.getStackLimit}） */
    @Override
    public int getMaxStackSize(ItemStack stack) {
        return handler.getStackLimit(slotIndex, stack);
    }

    // ==================== 插入/提取（照搬 SS StorageInventorySlot） ====================

    /**
     * 安全插入物品，使用 {@link #getMaxStackSize(ItemStack)} 计算容量。
     * 与原版 {@link SlotItemHandler#safeInsert(ItemStack, int)} 的差异：
     * 容量由 handler 动态决定（而非固定的 {@code getMaxStackSize()}）。
     */
    @Override
    public ItemStack safeInsert(ItemStack stack, int maxCount) {
        if (!stack.isEmpty() && mayPlace(stack)) {
            ItemStack existing = getItem();
            int limit = getMaxStackSize(stack) - existing.getCount();
            if (limit <= 0) {
                return stack; // 槽位已满
            }
            int toInsert = Math.min(Math.min(maxCount, stack.getCount()), limit);
            if (existing.isEmpty()) {
                ItemStack toSet = stack.copyWithCount(toInsert);
                stack.shrink(toInsert);
                set(toSet);
            } else if (ItemStack.isSameItemSameComponents(existing, stack)) {
                stack.shrink(toInsert);
                existing.grow(toInsert);
                set(existing);
            }
            return stack;
        }
        return stack;
    }

    /**
     * 限制单次提取量不超过物品原始 maxStackSize（通常 64）。
     * 防止左键从 1728 的槽位一次提取全部。
     */
    @Override
    public Optional<ItemStack> tryRemove(int amount, int limit, Player player) {
        int cap = getItem().getMaxStackSize(); // 物品原始上限（通常 64）
        return super.tryRemove(Math.min(amount, cap), limit, player);
    }

    /**
     * 获取真实存储数量。Screen 用此值渲染格式化数量（如 "1.7K"）。
     * {@link #getItem()} 返回真实 ItemStack（包含完整 count），所以直接取其 count。
     */
    public int getRealCount() {
        ItemStack real = getItem();
        return real.isEmpty() ? 0 : real.getCount();
    }
}

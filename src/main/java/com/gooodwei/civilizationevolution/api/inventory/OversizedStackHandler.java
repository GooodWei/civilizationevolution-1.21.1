package com.gooodwei.civilizationevolution.api.inventory;

import com.gooodwei.civilizationevolution.api.util.OversizedStackCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * 支持超大堆叠的通用 ItemStackHandler。
 * 参照 SophisticatedCore {@code InventoryHandler} 的核心模式。
 *
 * <p>与 SS InventoryHandler 的差异（精简版）：
 * <ul>
 *   <li>不依赖 IStorageWrapper（独立可用，任何 BE 都能直接使用）</li>
 *   <li>不集成 InventoryPartitioner / SlotTracker / 升级钩子</li>
 *   <li>子类可覆写 {@link #onLoad()}、插入/提取钩子等</li>
 * </ul>
 *
 * <p>容量计算（照搬 SS {@code InventoryHandler.getStackLimit}）：
 * <pre>
 *     getStackLimit(slot, stack) = stack.getMaxStackSize() * baseSlotLimit / 64
 * </pre>
 * 例如 baseSlotLimit = 1728，物品 maxStackSize = 64 → 1728 * 64 / 64 = 1728
 */
public class OversizedStackHandler extends ItemStackHandler {
    /** 每个槽位的基础容量（默认 64，即原版一组） */
    private int baseSlotLimit;

    /**
     * @param size 槽位数量
     */
    public OversizedStackHandler(int size) {
        super(size);
        this.baseSlotLimit = 64;
    }

    // ==================== 容量控制 ====================

    /** 设置基础槽位容量（每组倍数，例如 1728 = 27 组） */
    public void setBaseSlotLimit(int limit) {
        this.baseSlotLimit = limit;
    }

    public int getBaseSlotLimit() {
        return baseSlotLimit;
    }

    @Override
    public int getSlotLimit(int slot) {
        return baseSlotLimit;
    }

    /**
     * 获取指定槽位对指定物品的堆叠上限。
     * 照搬 SS {@code InventoryHandler.getStackLimit(int, ItemStack)}。
     *
     * @param slot  槽位索引
     * @param stack 物品（空栈返回 getSlotLimit）
     * @return 最大堆叠数量
     */
    public int getStackLimit(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return getSlotLimit(slot);
        }
        // 公式：物品原版堆叠上限 × (baseSlotLimit / 64)
        // 例如：64 × (1728 / 64) = 1728，16 × (1728 / 64) = 432
        return Math.max(1, stack.getMaxStackSize() * baseSlotLimit / 64);
    }

    // ==================== 内部栈访问 ====================

    /**
     * 获取底层物品列表（供 BE 的 {@code getItems()/setItems()} 桥接使用）。
     * {@link ItemStackHandler#stacks} 是 protected 字段，
     * 通过此方法暴露给外部。
     */
    public NonNullList<ItemStack> getStacks() {
        return stacks;
    }

    /**
     * 用新的列表替换底层物品列表（供 BE 的 {@code setItems()} 桥接）。
     */
    public void setStacks(NonNullList<ItemStack> newStacks) {
        // 确保新列表大小匹配当前槽位数
        if (newStacks.size() != stacks.size()) {
            stacks = NonNullList.withSize(newStacks.size(), ItemStack.EMPTY);
        }
        for (int i = 0; i < Math.min(stacks.size(), newStacks.size()); i++) {
            stacks.set(i, newStacks.get(i).copy());
        }
    }

    // ==================== 序列化（用 OversizedStackCodec 替代 ContainerHelper） ====================

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Size", stacks.size());
        tag.put("Items", OversizedStackCodec.saveAllItems(stacks, registries));
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag nbt) {
        int size = nbt.getInt("Size");
        if (size != stacks.size()) {
            setSize(size);
        }
        // 清空当前栈
        for (int i = 0; i < stacks.size(); i++) {
            stacks.set(i, ItemStack.EMPTY);
        }
        OversizedStackCodec.loadAllItems(nbt.getList("Items", 10), stacks, registries);
        onLoad();
    }

    /**
     * 反序列化完成后的回调。子类可覆写以执行额外逻辑（例如同步到客户端）。
     */
    protected void onLoad() {
    }
}

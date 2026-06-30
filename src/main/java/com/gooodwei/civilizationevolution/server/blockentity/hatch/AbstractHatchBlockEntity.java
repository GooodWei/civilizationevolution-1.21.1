package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 所有 hatch（多方块接口）方块实体的抽象基类。
 *
 * <p>提取了 Container 实现、NBT 持久化、listener 管理、MenuProvider 等所有 hatch 的公共逻辑。
 * 子类只需实现 {@link #getPartTier()}、{@link #getMaxStackSize()}、
 * {@link #canPlaceItem(int, ItemStack)}、{@link #getContainerName()} 和
 * {@link #createMenu(int, Inventory, Player)}。
 *
 * <p>hatch 拥有独立的存储空间和 GUI，无论是否在多方块结构中都可以右键打开。
 *
 * @see IMultiBlockPart
 */
public abstract class AbstractHatchBlockEntity extends BlockEntity
        implements net.minecraft.world.Container, IMultiBlockPart, MenuProvider {

    /** 物品槽位列表，大小由子类构造时指定 */
    protected final NonNullList<ItemStack> items;
    /** GUI 容器监听器（菜单打开时设置） */
    @Nullable
    private ContainerListener listener;
    /** 槽位数量（缓存，等于 items.size()） */
    private final int slotCount;

    // ==================== 构造器 ====================

    /**
     * @param type      BlockEntity 类型
     * @param pos       方块世界坐标
     * @param state     方块状态
     * @param slotCount 槽位数量
     */
    protected AbstractHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slotCount) {
        super(type, pos, state);
        this.slotCount = slotCount;
        this.items = NonNullList.withSize(slotCount, ItemStack.EMPTY);
    }

    // ==================== Container 实现 ====================

    @Override
    public int getContainerSize() {
        return slotCount;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    // ==================== 状态变更通知 ====================

    @Override
    public void setChanged() {
        super.setChanged();
        if (listener != null) {
            listener.containerChanged(this);
        }
    }

    // ==================== NBT 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    // ==================== MenuProvider ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable(getContainerName());
    }

    @Override
    public abstract AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player);

    // ==================== 抽象方法 ====================

    /** 获取此 hatch 的容器名称翻译 key */
    protected abstract String getContainerName();

    // 以下三个抽象方法由 IMultiBlockPart 和 Container 接口要求：

    @Override
    public abstract int getMaxStackSize();

    @Override
    public abstract boolean canPlaceItem(int slot, ItemStack stack);
}

package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.ContainerListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 原始人口输出接口的方块实体。
 *
 * <p>含 1 个槽位，不接受玩家/漏斗手动放入物品（仅代码产出）。
 * 多方块控制器将治疗后健康度超阈值的人口物品推送到此接口。
 */
public class PrimitivePopulationOutputHatchBlockEntity extends BlockEntity
        implements net.minecraft.world.Container, IMultiBlockPart {

    private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
    @Nullable
    private ContainerListener listener;

    public PrimitivePopulationOutputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================== IMultiBlockPart ====================

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    // ==================== Container ====================

    @Override
    public int getContainerSize() { return 1; }

    @Override
    public boolean isEmpty() { return items.getFirst().isEmpty(); }

    @Override
    public ItemStack getItem(int slot) { return items.get(slot); }

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
        stack = stack.copyWithCount(Math.min(stack.getCount(), getMaxStackSize()));
        setChanged();
    }

    @Override
    public int getMaxStackSize() { return 1; }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5,
                worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        // 输出接口不允许外部放入
        return false;
    }

    @Override
    public void clearContent() { items.clear(); }

    @Override
    public void setChanged() {
        super.setChanged();
        if (listener != null) listener.containerChanged(this);
    }

    // ==================== NBT ====================

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
}

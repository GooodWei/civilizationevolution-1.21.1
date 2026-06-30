package com.gooodwei.civilizationevolution.server.menu.machine;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 人口机器的菜单基类，提供统一的 shift+点击快速移动逻辑。
 *
 * <p>子类只需覆写 {@link #machineSlotCount()} 返回机器专用槽数即可。
 * 移动规则：
 * <ul>
 *   <li>机器槽 → 玩家背包（优先快捷栏）</li>
 *   <li>玩家背包 → 机器输入槽（自动跳过输出槽，因为 {@code PopulationMachineResultSlot.mayPlace} 返回 false）</li>
 * </ul>
 */
public abstract class MachineMenu extends AbstractContainerMenu {

    protected MachineMenu(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    /**
     * 机器专用槽位数量。
     * 槽位 0 ~ count-1 视为机器槽，槽位 count ~ slots.size()-1 视为玩家背包。
     */
    protected abstract int machineSlotCount();

    /**
     * 添加玩家背包（27 格）和快捷栏（9 格）到菜单中。
     * 各子类根据 GUI 高度传入对应的 yOffset。
     *
     * @param playerInventory 玩家物品栏
     * @param yOffset         玩家背包区域的起始 Y 坐标（快捷栏自动 +58）
     */
    protected void addPlayerSlots(Inventory playerInventory, int yOffset) {
        // 玩家背包（3 行 × 9 列）
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, yOffset + row * 18));
            }
        }
        // 玩家快捷栏（1 行 × 9 列）
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col,
                    8 + col * 18, yOffset + 58));
        }
    }

    /**
     * Shift+点击快速移动物品的统一逻辑。
     *
     * <p>规则：
     * <ul>
     *   <li>机器槽 → 玩家背包（优先快捷栏，{@code reverse=true}）</li>
     *   <li>玩家背包 → 机器输入槽（输出槽因 {@code mayPlace=false} 自动跳过）</li>
     * </ul>
     *
     * @param player 操作玩家
     * @param index  被 Shift+点击的槽位索引
     * @return 移动后的剩余物品堆（无法完全移动时返回剩余部分）
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            result = stackInSlot.copy();

            int machineSlots = machineSlotCount();
            int playerStart = machineSlots;
            int playerEnd = this.slots.size();

            if (index < machineSlots) {
                // 机器槽 → 玩家背包（reverse=true 优先快捷栏）
                if (!this.moveItemStackTo(stackInSlot, playerStart, playerEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 玩家背包 → 机器输入槽（输出槽 mayPlace=false 自动跳过）
                if (!this.moveItemStackTo(stackInSlot, 0, machineSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (stackInSlot.getCount() == result.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, stackInSlot);
        }
        return result;
    }
}

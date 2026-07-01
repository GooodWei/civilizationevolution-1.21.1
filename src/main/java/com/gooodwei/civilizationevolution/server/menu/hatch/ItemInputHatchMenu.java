package com.gooodwei.civilizationevolution.server.menu.hatch;

import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 物品输入接口的菜单（Primitive + Village 共用）。
 *
 * <p>1 个普通槽位（接受任意物品）+ 玩家背包 + 快捷栏。
 */
public class ItemInputHatchMenu extends AbstractHatchMenu {

    private static final int SLOT_X = 80;
    private static final int SLOT_Y = 35;

    public ItemInputHatchMenu(int containerId, Inventory playerInventory, Container container) {
        super(MenuRegistry.ITEM_INPUT_HATCH_MENU.get(), containerId, container);
        this.addSlot(new Slot(container, 0, SLOT_X, SLOT_Y));
        addPlayerSlots(playerInventory, 84);
    }

    public static ItemInputHatchMenu fromNetwork(int containerId, Inventory playerInventory,
                                                  RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof Container container) {
            return new ItemInputHatchMenu(containerId, playerInventory, container);
        }
        return null;
    }

    @Override
    protected int machineSlotCount() {
        return 1;
    }
}

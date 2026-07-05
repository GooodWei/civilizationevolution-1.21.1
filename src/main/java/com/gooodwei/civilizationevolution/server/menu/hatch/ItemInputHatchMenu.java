package com.gooodwei.civilizationevolution.server.menu.hatch;

import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 物品输入接口的菜单（Primitive 用）。
 *
 * <p>4 个槽位（2×2 网格）+ 玩家背包 + 快捷栏。
 */
public class ItemInputHatchMenu extends AbstractHatchMenu {

    private static final int COLS = 2;
    private static final int ROWS = 2;
    static final int CONTAINER_SIZE = 4;
    private static final int START_X = 62;
    private static final int SLOT_Y = 26;

    public ItemInputHatchMenu(int containerId, Inventory playerInventory, Container container) {
        super(MenuRegistry.ITEM_INPUT_HATCH_MENU.get(), containerId, container);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                this.addSlot(new Slot(container, col + row * COLS, START_X + col * 18, SLOT_Y + row * 18));
            }
        }

        addPlayerSlots(playerInventory, 84);
    }

    public static ItemInputHatchMenu fromNetwork(int containerId, Inventory playerInventory,
                                                  RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof Container container) {
            return new ItemInputHatchMenu(containerId, playerInventory, container);
        }
        // BE 不在场时返回带空容器的占位菜单（防止客户端 NPE）
        return new ItemInputHatchMenu(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE));
    }

    @Override
    protected int machineSlotCount() {
        return CONTAINER_SIZE;
    }
}

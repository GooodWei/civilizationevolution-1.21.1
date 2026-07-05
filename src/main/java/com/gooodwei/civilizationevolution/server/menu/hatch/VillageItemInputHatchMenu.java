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
 * 村庄物品输入接口菜单（9 槽 3×3 布局）。
 *
 * <p>3 行 × 3 列物品槽位 + 玩家背包 + 快捷栏。
 */
public class VillageItemInputHatchMenu extends AbstractHatchMenu {

    /** 容器槽位列数 */
    private static final int COLS = 3;
    /** 容器槽位行数 */
    private static final int ROWS = 3;
    /** 容器槽位总数 */
    static final int CONTAINER_SIZE = 9;
    /** 容器槽位起始 X */
    private static final int START_X = 62;
    /** 容器槽位起始 Y */
    private static final int SLOT_Y = 18;
    /** 玩家背包 Y */
    private static final int INVENTORY_Y = 85;

    public VillageItemInputHatchMenu(int containerId, Inventory playerInventory, Container container) {
        super(MenuRegistry.VILLAGE_ITEM_INPUT_HATCH_MENU.get(), containerId, container);

        // 3×3 容器槽位
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                this.addSlot(new Slot(container, col + row * COLS, START_X + col * 18, SLOT_Y + row * 18));
            }
        }

        addPlayerSlots(playerInventory, INVENTORY_Y);
    }

    public static VillageItemInputHatchMenu fromNetwork(int containerId, Inventory playerInventory,
                                                        RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof Container container) {
            return new VillageItemInputHatchMenu(containerId, playerInventory, container);
        }
        // BE 不在场时返回带空容器的占位菜单（防止客户端 NPE）
        return new VillageItemInputHatchMenu(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE));
    }

    @Override
    protected int machineSlotCount() {
        return CONTAINER_SIZE;
    }
}

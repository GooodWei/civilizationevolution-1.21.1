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
 * 村庄物品输出接口菜单（27 槽潜影盒布局）。
 *
 * <p>3 行 × 9 列物品槽位 + 玩家背包 + 快捷栏。
 * 槽位拒绝外部放入（由 {@code canPlaceItem} 限制），仅代码产出可写入。
 * 布局与 {@link VillageItemInputHatchMenu} 完全一致。
 */
public class VillageItemOutputHatchMenu extends AbstractHatchMenu {

    private static final int COLS = 9;
    private static final int ROWS = 3;
    static final int CONTAINER_SIZE = 27;
    private static final int SLOT_Y = 18;
    private static final int INVENTORY_Y = 85;

    public VillageItemOutputHatchMenu(int containerId, Inventory playerInventory, Container container) {
        super(MenuRegistry.VILLAGE_ITEM_OUTPUT_HATCH_MENU.get(), containerId, container);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                this.addSlot(new Slot(container, col + row * COLS, 8 + col * 18, SLOT_Y + row * 18) {
                    @Override
                    public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        addPlayerSlots(playerInventory, INVENTORY_Y);
    }

    public static VillageItemOutputHatchMenu fromNetwork(int containerId, Inventory playerInventory,
                                                          RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof Container container) {
            return new VillageItemOutputHatchMenu(containerId, playerInventory, container);
        }
        return new VillageItemOutputHatchMenu(containerId, playerInventory, new SimpleContainer(CONTAINER_SIZE));
    }

    @Override
    protected int machineSlotCount() {
        return CONTAINER_SIZE;
    }
}

package com.gooodwei.civilizationevolution.server.menu.hatch;

import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 物品输出接口的菜单（Primitive + Village 共用）。
 *
 * <p>1 个只读产物槽位（拒绝放入）+ 玩家背包 + 快捷栏。
 */
public class ItemOutputHatchMenu extends AbstractHatchMenu {

    private static final int SLOT_X = 80;
    private static final int SLOT_Y = 35;

    public ItemOutputHatchMenu(int containerId, Inventory playerInventory, Container container) {
        super(MenuRegistry.ITEM_OUTPUT_HATCH_MENU.get(), containerId, container);
        // 只读产物槽位：拒绝玩家/漏斗放入，仅代码产出
        this.addSlot(new Slot(container, 0, SLOT_X, SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addPlayerSlots(playerInventory, 84);
    }

    public static ItemOutputHatchMenu fromNetwork(int containerId, Inventory playerInventory,
                                                   RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof Container container) {
            return new ItemOutputHatchMenu(containerId, playerInventory, container);
        }
        // BE 不在场时返回带空容器的占位菜单（防止客户端 NPE）
        return new ItemOutputHatchMenu(containerId, playerInventory, new SimpleContainer(1));
    }

    @Override
    protected int machineSlotCount() {
        return 1;
    }
}

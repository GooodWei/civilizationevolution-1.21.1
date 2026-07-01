package com.gooodwei.civilizationevolution.server.menu.hatch;

import com.gooodwei.civilizationevolution.server.menu.slot.FoodInputSlot;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 村庄食物输入接口的菜单（Tier 1）。
 *
 * <p>1 个 {@link FoodInputSlot} + 玩家背包 + 快捷栏。
 */
public class VillageFoodInputHatchMenu extends AbstractHatchMenu {

    /** 输入槽位的 GUI 坐标 */
    private static final int SLOT_X = 80;
    private static final int SLOT_Y = 35;

    // ==================== 服务端构造器 ====================

    public VillageFoodInputHatchMenu(int containerId, Inventory playerInventory,
                                     Container container) {
        super(MenuRegistry.VILLAGE_FOOD_INPUT_HATCH_MENU.get(), containerId, container);

        // 单个食物输入槽位
        this.addSlot(new FoodInputSlot(container, 0, SLOT_X, SLOT_Y));

        // 玩家背包 + 快捷栏
        addPlayerSlots(playerInventory, 84);
    }

    // ==================== 客户端构造器（fromNetwork） ====================

    public static VillageFoodInputHatchMenu fromNetwork(int containerId, Inventory playerInventory,
                                                        RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof Container container) {
            return new VillageFoodInputHatchMenu(containerId, playerInventory, container);
        }
        return null;
    }

    // ==================== 快速移动 ====================

    @Override
    protected int machineSlotCount() {
        return 1;
    }
}

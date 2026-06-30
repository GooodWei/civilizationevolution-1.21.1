package com.gooodwei.civilizationevolution.server.menu.hatch;

import com.gooodwei.civilizationevolution.server.menu.slot.FoodInputSlot;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 原始食物输入接口的菜单。
 *
 * <p>1 个 {@link FoodInputSlot} + 玩家背包 + 快捷栏。
 *
 * <p>TODO：当前槽位坐标为营地占位 GUI 的临时值，hatch 专用 GUI 设计完成后需调整。</p>
 */
public class PrimitiveFoodInputHatchMenu extends AbstractHatchMenu {

    /** 输入槽位的 GUI 坐标（TODO：当前为营地占位 GUI 的临时值，hatch 专用 GUI 设计完成后需调整） */
    private static final int SLOT_X = 80;
    private static final int SLOT_Y = 35;

    // ==================== 服务端构造器 ====================

    public PrimitiveFoodInputHatchMenu(int containerId, Inventory playerInventory,
                                        Container container) {
        super(MenuRegistry.PRIMITIVE_FOOD_INPUT_HATCH_MENU.get(), containerId, container);

        // 单个食物输入槽位
        this.addSlot(new FoodInputSlot(container, 0, SLOT_X, SLOT_Y));

        // 玩家背包 + 快捷栏
        addPlayerSlots(playerInventory, 84);
    }

    // ==================== 客户端构造器（fromNetwork） ====================

    public static PrimitiveFoodInputHatchMenu fromNetwork(int containerId, Inventory playerInventory,
                                                            RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof Container container) {
            return new PrimitiveFoodInputHatchMenu(containerId, playerInventory, container);
        }
        return null;
    }

    // ==================== 快速移动 ====================

    @Override
    protected int machineSlotCount() {
        return 1;
    }
}

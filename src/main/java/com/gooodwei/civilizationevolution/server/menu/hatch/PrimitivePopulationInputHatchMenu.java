package com.gooodwei.civilizationevolution.server.menu.hatch;

import com.gooodwei.civilizationevolution.server.menu.slot.PopulationItemSlot;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 原始人口输入接口的菜单。
 *
 * <p>1 个 {@link PopulationItemSlot} + 玩家背包 + 快捷栏。
 *
 * <p>TODO：当前槽位坐标为营地占位 GUI 的临时值，hatch 专用 GUI 设计完成后需调整。</p>
 */
public class PrimitivePopulationInputHatchMenu extends AbstractHatchMenu {

    /** 输入槽位的 GUI 坐标（TODO：当前为营地占位 GUI 的临时值，hatch 专用 GUI 设计完成后需调整） */
    private static final int SLOT_X = 80;
    private static final int SLOT_Y = 35;

    // ==================== 服务端构造器 ====================

    public PrimitivePopulationInputHatchMenu(int containerId, Inventory playerInventory,
                                              Container container) {
        super(MenuRegistry.PRIMITIVE_POPULATION_INPUT_HATCH_MENU.get(), containerId, container);

        // 单个人口输入槽位
        this.addSlot(new PopulationItemSlot(container, 0, SLOT_X, SLOT_Y));

        // 玩家背包 + 快捷栏
        addPlayerSlots(playerInventory, 84);
    }

    // ==================== 客户端构造器（fromNetwork） ====================

    public static PrimitivePopulationInputHatchMenu fromNetwork(int containerId, Inventory playerInventory,
                                                                  RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof Container container) {
            return new PrimitivePopulationInputHatchMenu(containerId, playerInventory, container);
        }
        return null;
    }

    // ==================== 快速移动 ====================

    @Override
    protected int machineSlotCount() {
        return 1;
    }
}

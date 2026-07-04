package com.gooodwei.civilizationevolution.server.menu.hatch;

import com.gooodwei.civilizationevolution.server.menu.slot.PopulationMachineResultSlot;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 原始人口输出接口的菜单。
 *
 * <p>1 个 {@link PopulationMachineResultSlot}（只读，拒绝放入）+ 玩家背包 + 快捷栏。
 *
 * <p>TODO：当前槽位坐标为营地占位 GUI 的临时值，hatch 专用 GUI 设计完成后需调整。</p>
 */
public class PrimitivePopulationOutputHatchMenu extends AbstractHatchMenu {

    /** 输出槽位的 GUI 坐标（TODO：当前为营地占位 GUI 的临时值，hatch 专用 GUI 设计完成后需调整） */
    private static final int SLOT_X = 80;
    private static final int SLOT_Y = 35;

    // ==================== 服务端构造器 ====================

    public PrimitivePopulationOutputHatchMenu(int containerId, Inventory playerInventory,
                                               Container container) {
        super(MenuRegistry.PRIMITIVE_POPULATION_OUTPUT_HATCH_MENU.get(), containerId, container);

        // 单个输出槽位（只读）
        this.addSlot(new PopulationMachineResultSlot(container, 0, SLOT_X, SLOT_Y));

        // 玩家背包 + 快捷栏
        addPlayerSlots(playerInventory, 84);
    }

    // ==================== 客户端构造器（fromNetwork） ====================

    public static PrimitivePopulationOutputHatchMenu fromNetwork(int containerId, Inventory playerInventory,
                                                                   RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof Container container) {
            return new PrimitivePopulationOutputHatchMenu(containerId, playerInventory, container);
        }
        // BE 不在场时返回带空容器的占位菜单（防止客户端 NPE）
        return new PrimitivePopulationOutputHatchMenu(containerId, playerInventory, new SimpleContainer(1));
    }

    // ==================== 快速移动 ====================

    @Override
    protected int machineSlotCount() {
        return 1;
    }
}

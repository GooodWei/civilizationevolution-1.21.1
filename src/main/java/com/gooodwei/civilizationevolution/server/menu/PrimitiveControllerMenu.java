package com.gooodwei.civilizationevolution.server.menu;

import com.gooodwei.civilizationevolution.server.blockentity.controllermachine.PrimitiveControllerBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 原始控制器 GUI 的容器菜单。
 *
 * <p>槽位布局（共 1 个机器槽位）：
 * <ul>
 *   <li>槽位 0：文明核心槽位（仅接受 {@link com.gooodwei.civilizationevolution.server.item.CivilizationCoreItem}，
 *       由 {@code BlockEntity.canPlaceItem} 限制）</li>
 * </ul>
 *
 * <p>通过 {@code ContainerData}（3 个字段）同步日晷进度和绑定状态：
 * <ul>
 *   <li>{@code containerData[0]} — 当前日晷进度（currentDayProgress）</li>
 *   <li>{@code containerData[1]} — 已绑定机器数量（boundCount）</li>
 *   <li>{@code containerData[2]} — 日晷总时长（DAY_TICKS，通常为 24000）</li>
 * </ul>
 */
public class PrimitiveControllerMenu extends MachineMenu {
    /** 底层的方块实体容器引用 */
    public final Container container;
    /** 同步到客户端的数据（日晷进度、绑定数量等） */
    public ContainerData containerData;

    /**
     * 服务端构造器。
     *
     * @param containerId     容器窗口 ID
     * @param playerInventory 玩家物品栏
     * @param blockEntity     原始控制器方块实体
     * @param containerData   同步数据
     */
    public PrimitiveControllerMenu(int containerId, Inventory playerInventory, PrimitiveControllerBlockEntity blockEntity, ContainerData containerData) {
        super(MenuRegistry.PRIMITIVE_CONTROLLER_MENU.get(), containerId);
        this.container = blockEntity;
        this.containerData = containerData;
        this.addDataSlots(containerData);
        this.addSlot(new Slot(container, 0, 9, 81){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return container.canPlaceItem(0, stack);
            }
        });
        // 玩家背包 + 快捷栏
        addPlayerSlots(playerInventory, 114);
    }

    /**
     * 机器专用槽位数量。
     *
     * <p>槽位 0 ~ count-1 视为机器槽，槽位 count ~ slots.size()-1 视为玩家背包。</p>
     *
     * @return 机器槽位数量（固定为 1）
     */
    @Override
    protected int machineSlotCount() {
        return 1;
    }

    /**
     * 原始控制器菜单始终有效（不需要玩家在方块附近）。
     *
     * @param player 当前玩家
     * @return 始终返回 {@code true}
     */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /**
     * 客户端构造器 —— 从网络数据包重建。
     *
     * @param containerId     容器窗口 ID
     * @param playerInventory 客户端玩家物品栏
     * @param buf             网络数据包（包含 BlockPos）
     * @return 重建的 PrimitiveControllerMenu 实例（使用占位 {@link SimpleContainerData}）
     */
    public static PrimitiveControllerMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        BlockPos blockPos = buf.readBlockPos();
        PrimitiveControllerBlockEntity blockEntity = (PrimitiveControllerBlockEntity) playerInventory.player.level().getBlockEntity(blockPos);
        return new PrimitiveControllerMenu(containerId, playerInventory, blockEntity, new SimpleContainerData(3));
    }

    /**
     * 获取日晷进度比例（0.0 ~ 1.0），供 GUI 渲染进度条。
     *
     * @return 当前进度 / 日晷总时长（DAY_TICKS），总时长为 0 时返回 0
     */
    public float getWorkProgressRatio() {
        int total = this.containerData.get(2); // DAY_TICKS
        return total == 0 ? 0 : (float) this.containerData.get(0) / total;
    }

    /**
     * 获取当前已绑定的机器数量。
     *
     * @return 已绑定机器数量
     */
    public int getBoundCount() {
        return this.containerData.get(1);
    }

    /**
     * 获取日晷总 tick 数。
     *
     * @return 日晷周期总 tick 数（通常为 24000）
     */
    public int getDayTicks() {
        return this.containerData.get(2);
    }

    /**
     * 获取对应的原始控制器方块坐标。
     *
     * @return 方块坐标
     */
    public BlockPos getBlockPos() {
        return ((PrimitiveControllerBlockEntity) this.container).getBlockPos();
    }


}

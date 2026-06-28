package com.gooodwei.civilizationevolution.server.menu;

import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractCampBlockEntity;
import com.gooodwei.civilizationevolution.server.menu.slot.FoodInputSlot;
import com.gooodwei.civilizationevolution.server.menu.slot.PopulationItemSlot;
import com.gooodwei.civilizationevolution.server.menu.slot.PopulationMachineResultSlot;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;

/**
 * 原始营地 GUI 的容器菜单（Tier 0）。
 *
 * <p>槽位布局（共 10 个机器槽位）：
 * <ul>
 *   <li>槽位 0-3：食物输入槽（{@link FoodInputSlot}，仅接受含 {@code FOOD} 组件的物品）</li>
 *   <li>槽位 4-5：人口输入槽（{@link PopulationItemSlot}，仅接受 {@code PopulationItem}）</li>
 *   <li>槽位 6-9：产物输出槽（{@link PopulationMachineResultSlot}，拒绝放入，仅由代码产出）</li>
 * </ul>
 */
public class PrimitiveCampMenu extends MachineMenu {

    /** 底层的方块实体容器引用 */
    public final Container container;

    /**
     * 服务端构造器。
     *
     * @param containerId     容器窗口 ID
     * @param playerInventory 玩家物品栏
     * @param blockEntity     营地方块实体
     */
    public PrimitiveCampMenu(int containerId, Inventory playerInventory, AbstractCampBlockEntity blockEntity) {
        super(MenuRegistry.PRIMITIVE_CAMP_MENU.get(), containerId);
        this.container = blockEntity;
        // 槽位 0-3：通用输入（接口不限制）
        for (int num = 0; num <= 3; num++) {
            this.addSlot(new FoodInputSlot(container, num, 7 + num * 18, 11));
        }
        // 槽位 4-5：人口输入
        for (int num = 0; num < 2; num++) {
            this.addSlot(new PopulationItemSlot(container, num + 4, 133 + num * 18, 11));
        }
        // 槽位 6-9：仅输出
        for (int num = 0; num < 4; num++) {
            this.addSlot(new PopulationMachineResultSlot(container, num + 6, 96 + num * 18, 106));
        }
        // 玩家背包 + 快捷栏
        addPlayerSlots(playerInventory, 139);
    }

    /**
     * 客户端构造器 —— 从网络数据包重建。
     *
     * @param containerId     容器窗口 ID
     * @param playerInventory 客户端玩家物品栏
     * @param buf             网络数据包（包含 BlockPos）
     * @return 重建的 PrimitiveCampMenu 实例
     */
    public static PrimitiveCampMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        AbstractCampBlockEntity be = (AbstractCampBlockEntity) playerInventory.player.level().getBlockEntity(buf.readBlockPos());
        return new PrimitiveCampMenu(containerId, playerInventory, be);
    }

    @Override
    protected int machineSlotCount() {
        return 10;
    }

    /**
     * 原始营地菜单始终有效（不需要玩家在方块附近）。
     *
     * @param player 当前玩家
     * @return 始终返回 {@code true}
     */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

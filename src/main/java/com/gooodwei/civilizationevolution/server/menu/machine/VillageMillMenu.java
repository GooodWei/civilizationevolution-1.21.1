package com.gooodwei.civilizationevolution.server.menu.machine;

import com.gooodwei.civilizationevolution.server.blockentity.multiblock.VillageMillBlockEntity;
import com.gooodwei.civilizationevolution.server.menu.slot.PopulationItemSlot;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 村庄磨坊 GUI 的容器菜单。
 *
 * <p>槽位布局（共 4 个机器槽位）：
 * <ul>
 *   <li>槽位 0-3：人口输入槽（{@link PopulationItemSlot}，2×2 网格）</li>
 * </ul>
 *
 * <p>原料从物品输入仓直接取，产物路由到物品输出仓，不占用 Menu 槽位。
 *
 * <p>通过 {@code ContainerData}（2 个字段）同步工作进度：
 * <ul>
 *   <li>{@code data[0]} — 当前工作进度</li>
 *   <li>{@code data[1]} — 配方工作总时长</li>
 * </ul>
 */
public class VillageMillMenu extends MachineMenu {

    public final Container container;
    public ContainerData data;

    /** 服务端构造器 */
    public VillageMillMenu(int containerId, Inventory playerInventory,
                            VillageMillBlockEntity blockEntity, ContainerData data) {
        super(MenuRegistry.VILLAGE_MILL_MENU.get(), containerId);
        this.container = blockEntity;
        this.data = data;
        this.addDataSlots(data);

        // 人口槽位（2×2 网格）
        this.addSlot(new PopulationItemSlot(container, 0, 62, 26));
        this.addSlot(new PopulationItemSlot(container, 1, 80, 26));
        this.addSlot(new PopulationItemSlot(container, 2, 62, 44));
        this.addSlot(new PopulationItemSlot(container, 3, 80, 44));

        addPlayerSlots(playerInventory, PLAYER_INVENTORY_Y);
    }

    /** 客户端构造器 —— 从网络数据包重建 */
    public static VillageMillMenu fromNetwork(int containerId, Inventory playerInventory,
                                                RegistryFriendlyByteBuf buf) {
        VillageMillBlockEntity be = (VillageMillBlockEntity)
                playerInventory.player.level().getBlockEntity(buf.readBlockPos());
        return new VillageMillMenu(containerId, playerInventory, be,
                new SimpleContainerData(2));
    }

    /** 获取工作进度比例（0.0 ~ 1.0），供 Screen 渲染进度条 */
    public float getWorkProgressRatio() {
        return getWorkProgressRatio(data);
    }

    /** 获取当前方块坐标，供客户端发包使用 */
    public BlockPos getBlockPos() {
        return ((VillageMillBlockEntity) this.container).getBlockPos();
    }

    @Override
    protected int machineSlotCount() {
        return 4;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

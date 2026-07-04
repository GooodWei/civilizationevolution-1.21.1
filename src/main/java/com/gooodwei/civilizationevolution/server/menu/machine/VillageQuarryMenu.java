package com.gooodwei.civilizationevolution.server.menu.machine;

import com.gooodwei.civilizationevolution.server.blockentity.multiblock.VillageQuarryBlockEntity;
import com.gooodwei.civilizationevolution.server.menu.slot.PopulationItemSlot;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 村庄采石场 GUI 的容器菜单。
 *
 * <p>极简槽位布局（共 4 个机器槽位）：
 * <ul>
 *   <li>槽位 0-2：人口输入槽（{@link PopulationItemSlot}，x=62/80/98, y=18）</li>
 *   <li>槽位 3：镐子输入槽（仅接受 {@code #minecraft:pickaxes}，x=80, y=52）</li>
 * </ul>
 *
 * <p>食物从多方块结构的食物输入仓直接消耗，产物直接路由到物品输出接口，
 * 不再占用 Menu 槽位。
 *
 * <p>通过 {@code ContainerData}（3 个字段）同步工作进度：
 * <ul>
 *   <li>{@code data[0]} — 当前工作进度</li>
 *   <li>{@code data[1]} — 工作总时长</li>
 *   <li>{@code data[2]} — 最低保留数量</li>
 * </ul>
 */
public class VillageQuarryMenu extends MachineMenu {

    public final Container container;
    public ContainerData data;

    /** 服务端构造器 */
    public VillageQuarryMenu(int containerId, Inventory playerInventory,
                              VillageQuarryBlockEntity blockEntity, ContainerData data) {
        super(MenuRegistry.VILLAGE_QUARRY_MENU.get(), containerId);
        this.container = blockEntity;
        this.data = data;
        this.addDataSlots(data);

        // 人口槽位（3 格横排）
        for (int i = 0; i < 3; i++) {
            this.addSlot(new PopulationItemSlot(container, i, 62 + 18 * i, 18));
        }

        // 镐槽
        this.addSlot(new Slot(container, 3, 80, 52) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ItemTags.PICKAXES);
            }
        });

        addPlayerSlots(playerInventory, 84);
    }

    /** 客户端构造器 —— 从网络数据包重建 */
    public static VillageQuarryMenu fromNetwork(int containerId, Inventory playerInventory,
                                                  RegistryFriendlyByteBuf buf) {
        VillageQuarryBlockEntity be = (VillageQuarryBlockEntity)
                playerInventory.player.level().getBlockEntity(buf.readBlockPos());
        return new VillageQuarryMenu(containerId, playerInventory, be,
                new SimpleContainerData(3));
    }

    public float getWorkProgressRatio() {
        int total = this.data.get(1);
        return total == 0 ? 0 : (float) this.data.get(0) / total;
    }

    public int getMinKeepNumber() {
        return this.data.get(2);
    }

    public BlockPos getBlockPos() {
        return ((VillageQuarryBlockEntity) this.container).getBlockPos();
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

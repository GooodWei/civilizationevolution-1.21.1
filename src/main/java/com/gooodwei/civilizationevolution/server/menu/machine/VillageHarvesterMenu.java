package com.gooodwei.civilizationevolution.server.menu.machine;

import com.gooodwei.civilizationevolution.server.blockentity.machine.VillageHarvesterBlockEntity;
import com.gooodwei.civilizationevolution.server.menu.slot.FoodInputSlot;
import com.gooodwei.civilizationevolution.server.menu.slot.PopulationItemSlot;
import com.gooodwei.civilizationevolution.server.menu.slot.PopulationMachineResultSlot;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import com.gooodwei.civilizationevolution.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class VillageHarvesterMenu extends MachineMenu {
    public final Container container;
    private final ContainerData data;

    public VillageHarvesterMenu(int containerId, Inventory inventory, VillageHarvesterBlockEntity blockEntity, ContainerData data) {
        super(MenuRegistry.VILLAGE_HARVESTER_MENU.get(), containerId);
        this.container = blockEntity;
        this.data = data;
        addDataSlots(data);
        int num = 0;
        for(int i = 0; i < 2; i++) {
            for(int j = 0; j < 3; j++) {
                this.addSlot(new FoodInputSlot(container, num, 8 + 18 * j, 13 + 18 * i));
                num++;
            }
        }
        for(int i = 0; i < 3; i++) {
            this.addSlot(new PopulationItemSlot(container, num, 8 + 18 * i, 57));
            num++;
        }
        this.addSlot(new Slot(container, num, 81, 13){
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.HOES);
            }
        });
        num++;
        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                this.addSlot(new PopulationMachineResultSlot(container, num, 112 + 18 * j, 17 + 18 * i));
                num++;
            }
        }
        // 玩家背包 + 快捷栏
        addPlayerSlots(inventory, 84);
    }

    public static VillageHarvesterMenu fromNetwork(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(pos);
        if (blockEntity instanceof VillageHarvesterBlockEntity harvesterBlockEntity) {
            return new VillageHarvesterMenu(containerId, inventory,
                    harvesterBlockEntity, new SimpleContainerData(3));
        }
        return new VillageHarvesterMenu(containerId, inventory,
                null, new SimpleContainerData(3));
    }

    /** 进度比例（0.0~1.0），供进度条渲染 */
    public float getWorkProgressRatio() {
        int total = data.get(1);
        if (total == 0) return 0f;
        return (float) data.get(0) / total;
    }


    /**
     * 机器专用槽位数量。
     * 槽位 0 ~ count-1 视为机器槽，槽位 count ~ slots.size()-1 视为玩家背包。
     */
    @Override
    protected int machineSlotCount() {
        return 19;
    }

    /**
     * 菜单关闭时触发一次冲突扫描（仅服务端）。
     *
     * @param player 关闭菜单的玩家
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && container instanceof VillageHarvesterBlockEntity be) {
            be.onPlacedOrOpened((ServerLevel) player.level());
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

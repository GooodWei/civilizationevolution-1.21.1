package com.gooodwei.civilizationevolution.server.menu;

import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractHuntingGroundBlockEntity;
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

/**
 * 狩猎场 GUI 的容器菜单。
 *
 * <p>槽位布局（共 19 个机器槽位）：
 * <ul>
 *   <li>槽位 0-5：食物输入槽（2×3，{@link FoodInputSlot}）</li>
 *   <li>槽位 6-8：人口输入槽（{@link PopulationItemSlot}）</li>
 *   <li>槽位 9：武器输入槽（仅接受 {@code ModTags.WEAPONS}）</li>
 *   <li>槽位 10-18：产物输出槽（3×3，{@link PopulationMachineResultSlot}）</li>
 * </ul>
 *
 * <p>通过 {@code ContainerData}（3 个字段）同步工作进度和最小保留数量：
 * <ul>
 *   <li>{@code data[0]} — 当前工作进度</li>
 *   <li>{@code data[1]} — 工作总时长</li>
 *   <li>{@code data[2]} — 最小保留数量</li>
 * </ul>
 *
 * <p>菜单关闭时触发一次冲突扫描（{@code onPlacedOrOpened}）。</p>
 */
public class PrimitiveHuntingGroundMenu extends MachineMenu {
    /** 底层的方块实体容器引用 */
    public final Container container;
    /** 同步到客户端的数据（工作进度、最小保留数量等） */
    public ContainerData data;

    /**
     * 服务端构造器。
     *
     * @param containerId     容器窗口 ID
     * @param playerInventory 玩家物品栏
     * @param blockEntity     狩猎场方块实体
     * @param data            同步数据
     */
    public PrimitiveHuntingGroundMenu(int containerId, Inventory playerInventory, AbstractHuntingGroundBlockEntity blockEntity, ContainerData data) {
        super(MenuRegistry.PRIMITIVE_HUNTING_GROUND_MENU.get(), containerId);
        this.container = blockEntity;
        this.data = data;
        this.addDataSlots(data);
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
                return stack.is(ModTags.WEAPONS);
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
        addPlayerSlots(playerInventory, 84);
    }

    /**
     * 客户端构造器 —— 从网络数据包重建。
     *
     * @param containerId     容器窗口 ID
     * @param playerInventory 客户端玩家物品栏
     * @param buf             网络数据包（包含 BlockPos）
     * @return 重建的 PrimitiveHuntingGroundMenu 实例（使用占位 {@link SimpleContainerData}）
     */
    public static PrimitiveHuntingGroundMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buf) {
        AbstractHuntingGroundBlockEntity be = (AbstractHuntingGroundBlockEntity)
                playerInventory.player.level().getBlockEntity(buf.readBlockPos());
        return new PrimitiveHuntingGroundMenu(containerId, playerInventory, be,
                new SimpleContainerData(3));
    }

    /**
     * 获取工作进度比例（0.0 ~ 1.0），供 GUI 渲染进度条。
     *
     * @return 当前进度 / 总时长，总时长为 0 时返回 0
     */
    public float getWorkProgressRatio() {
        int total = this.data.get(1);
        return total == 0 ? 0 : (float) this.data.get(0) / total;
    }

    /**
     * 获取最小保留数量（产出物在达到此数量前不会被自动消耗）。
     *
     * @return 最小保留数量
     */
    public int getMinKeepNumber() {
        return this.data.get(2);
    }

    /**
     * 获取对应的狩猎场方块坐标。
     *
     * @return 方块坐标
     */
    public BlockPos getBlockPos() {
        return ((AbstractHuntingGroundBlockEntity) this.container).getBlockPos();
    }

    /**
     * 菜单关闭时触发一次冲突扫描（仅服务端）。
     *
     * @param player 关闭菜单的玩家
     */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide && container instanceof AbstractHuntingGroundBlockEntity be) {
            be.onPlacedOrOpened((ServerLevel) player.level());
        }
    }

    @Override
    protected int machineSlotCount() {
        return 19;
    }

    /**
     * 狩猎场菜单始终有效（不需要玩家在方块附近）。
     *
     * @param player 当前玩家
     * @return 始终返回 {@code true}
     */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

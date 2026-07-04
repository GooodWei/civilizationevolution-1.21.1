package com.gooodwei.civilizationevolution.server.menu.hatch;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.menu.slot.FoodInputSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 食物输入接口的菜单（Primitive + Village 共用）。
 *
 * <p>1 个 {@link FoodInputSlot} + 玩家背包 + 快捷栏。
 * 通过 {@link MenuType} 参数区分 Primitive/Village 的 GUI 纹理。
 *
 * <p>TODO：当前槽位坐标为营地占位 GUI 的临时值，hatch 专用 GUI 设计完成后需调整。</p>
 */
public class FoodInputHatchMenu extends AbstractHatchMenu {

    /** 输入槽位的 GUI 坐标（TODO：当前为营地占位 GUI 的临时值，hatch 专用 GUI 设计完成后需调整） */
    private static final int SLOT_X = 80;
    private static final int SLOT_Y = 35;

    // ==================== 服务端构造器 ====================

    public FoodInputHatchMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                              Container container) {
        super(type, containerId, container);

        // 单个食物输入槽位
        this.addSlot(new FoodInputSlot(container, 0, SLOT_X, SLOT_Y));

        // 玩家背包 + 快捷栏
        addPlayerSlots(playerInventory, 84);
    }

    // ==================== 客户端构造器（fromNetwork） ====================

    /** 原始食物输入接口的 fromNetwork 入口，通过运行时注册表查找 MenuType 避免自引用 */
    public static FoodInputHatchMenu fromNetworkPrimitive(int containerId,
                                                           Inventory playerInventory,
                                                           RegistryFriendlyByteBuf buf) {
        return fromNetwork(getMenuType("primitive_food_input_hatch"), containerId, playerInventory, buf);
    }

    /** 村庄食物输入接口的 fromNetwork 入口，通过运行时注册表查找 MenuType 避免自引用 */
    public static FoodInputHatchMenu fromNetworkVillage(int containerId,
                                                         Inventory playerInventory,
                                                         RegistryFriendlyByteBuf buf) {
        return fromNetwork(getMenuType("village_food_input_hatch"), containerId, playerInventory, buf);
    }

    private static MenuType<?> getMenuType(String name) {
        return BuiltInRegistries.MENU.get(
                ResourceLocation.fromNamespaceAndPath(CivilizationEvolution.MODID, name));
    }

    private static FoodInputHatchMenu fromNetwork(MenuType<?> type, int containerId,
                                                   Inventory playerInventory,
                                                   RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof Container container) {
            return new FoodInputHatchMenu(type, containerId, playerInventory, container);
        }
        // BE 不在场时返回带空容器的占位菜单（防止客户端 NPE）
        return new FoodInputHatchMenu(type, containerId, playerInventory, new SimpleContainer(1));
    }

    // ==================== 快速移动 ====================

    @Override
    protected int machineSlotCount() {
        return 1;
    }
}

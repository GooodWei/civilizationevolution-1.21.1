package com.gooodwei.civilizationevolution.server.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.menu.CampMenu;
import com.gooodwei.civilizationevolution.server.menu.HuntingGroundMenu;
import com.gooodwei.civilizationevolution.server.menu.PrimitiveRanchMenu;
import com.gooodwei.civilizationevolution.server.menu.PrimitiveSettlementMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Menu（容器菜单）类型注册表。
 * 使用 {@link IMenuTypeExtension#create} 以支持从网络包构造菜单（客户端侧）。
 */
public class MenuRegistry {

    /** Menu 类型注册器 */
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, CivilizationEvolution.MODID);

    /** 营地菜单类型 */
    public static final Supplier<MenuType<CampMenu>> CAMP_MENU =
            MENUS.register("camp", () -> IMenuTypeExtension.create(CampMenu::fromNetwork));

    /** 狩猎场菜单类型 */
    public static final Supplier<MenuType<HuntingGroundMenu>> HUNTING_GROUND_MENU =
            MENUS.register("hunting_ground", () -> IMenuTypeExtension.create(HuntingGroundMenu::fromNetwork));

    /** 原始牧场菜单类型 */
    public static final Supplier<MenuType<PrimitiveRanchMenu>> PRIMITIVE_RANCH_MENU =
            MENUS.register("primitive_ranch", () -> IMenuTypeExtension.create(PrimitiveRanchMenu::fromNetwork));

    /** 原始聚落菜单类型 */
    public static final Supplier<MenuType<PrimitiveSettlementMenu>> PRIMITIVE_SETTLEMENT_MENU =
            MENUS.register("primitive_settlement", () -> IMenuTypeExtension.create(PrimitiveSettlementMenu::fromNetwork));

    /**
     * 向事件总线注册所有 Menu 类型。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}

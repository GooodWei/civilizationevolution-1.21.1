package com.gooodwei.civilizationevolution.server.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveDoctorCabinMenu;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveFarmMenu;
import com.gooodwei.civilizationevolution.server.menu.machine.VillageQuarryMenu;
import com.gooodwei.civilizationevolution.server.menu.hatch.ItemInputHatchMenu;
import com.gooodwei.civilizationevolution.server.menu.hatch.ItemOutputHatchMenu;
import com.gooodwei.civilizationevolution.server.menu.hatch.PrimitiveFoodInputHatchMenu;
import com.gooodwei.civilizationevolution.server.menu.hatch.VillageFoodInputHatchMenu;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveHuntingGroundMenu;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveCampMenu;
import com.gooodwei.civilizationevolution.server.menu.hatch.PrimitivePopulationInputHatchMenu;
import com.gooodwei.civilizationevolution.server.menu.hatch.PrimitivePopulationOutputHatchMenu;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveRanchMenu;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveControllerMenu;
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

    /** 原始营地菜单类型（Tier 0） */
    public static final Supplier<MenuType<PrimitiveCampMenu>> PRIMITIVE_CAMP_MENU =
            MENUS.register("primitive_camp", () -> IMenuTypeExtension.create(PrimitiveCampMenu::fromNetwork));

    /** 原始狩猎场菜单类型（Tier 0） */
    public static final Supplier<MenuType<PrimitiveHuntingGroundMenu>> PRIMITIVE_HUNTING_GROUND_MENU =
            MENUS.register("primitive_hunting_ground", () -> IMenuTypeExtension.create(PrimitiveHuntingGroundMenu::fromNetwork));

    /** 原始牧场菜单类型（Tier 0） */
    public static final Supplier<MenuType<PrimitiveRanchMenu>> PRIMITIVE_RANCH_MENU =
            MENUS.register("primitive_ranch", () -> IMenuTypeExtension.create(PrimitiveRanchMenu::fromNetwork));

    /** 原始控制器菜单类型（Tier 0） */
    public static final Supplier<MenuType<PrimitiveControllerMenu>> PRIMITIVE_CONTROLLER_MENU =
            MENUS.register("primitive_controller", () -> IMenuTypeExtension.create(PrimitiveControllerMenu::fromNetwork));

    /** 原始农场菜单类型（Tier 0） */
    public static final Supplier<MenuType<PrimitiveFarmMenu>> PRIMITIVE_FARM_MENU =
            MENUS.register("primitive_farm", () -> IMenuTypeExtension.create(PrimitiveFarmMenu::fromNetwork));

    /** 原始诊所菜单类型（Tier 0） */
    public static final Supplier<MenuType<PrimitiveDoctorCabinMenu>> PRIMITIVE_DOCTOR_CABIN_MENU =
            MENUS.register("primitive_doctor_cabin", () -> IMenuTypeExtension.create(PrimitiveDoctorCabinMenu::fromNetwork));

    /** 原始人口输入接口菜单类型（Tier 0） */
    public static final Supplier<MenuType<PrimitivePopulationInputHatchMenu>> PRIMITIVE_POPULATION_INPUT_HATCH_MENU =
            MENUS.register("primitive_population_input_hatch",
                    () -> IMenuTypeExtension.create(PrimitivePopulationInputHatchMenu::fromNetwork));

    /** 原始食物输入接口菜单类型（Tier 0） */
    public static final Supplier<MenuType<PrimitiveFoodInputHatchMenu>> PRIMITIVE_FOOD_INPUT_HATCH_MENU =
            MENUS.register("primitive_food_input_hatch",
                    () -> IMenuTypeExtension.create(PrimitiveFoodInputHatchMenu::fromNetwork));

    /** 村庄食物输入接口菜单类型（Tier 1） */
    public static final Supplier<MenuType<VillageFoodInputHatchMenu>> VILLAGE_FOOD_INPUT_HATCH_MENU =
            MENUS.register("village_food_input_hatch",
                    () -> IMenuTypeExtension.create(VillageFoodInputHatchMenu::fromNetwork));

    /** 原始人口输出接口菜单类型（Tier 0） */
    public static final Supplier<MenuType<PrimitivePopulationOutputHatchMenu>> PRIMITIVE_POPULATION_OUTPUT_HATCH_MENU =
            MENUS.register("primitive_population_output_hatch",
                    () -> IMenuTypeExtension.create(PrimitivePopulationOutputHatchMenu::fromNetwork));

    /** 物品输入接口菜单类型（Primitive + Village 共用） */
    public static final Supplier<MenuType<ItemInputHatchMenu>> ITEM_INPUT_HATCH_MENU =
            MENUS.register("item_input_hatch",
                    () -> IMenuTypeExtension.create(ItemInputHatchMenu::fromNetwork));

    /** 物品输出接口菜单类型（Primitive + Village 共用） */
    public static final Supplier<MenuType<ItemOutputHatchMenu>> ITEM_OUTPUT_HATCH_MENU =
            MENUS.register("item_output_hatch",
                    () -> IMenuTypeExtension.create(ItemOutputHatchMenu::fromNetwork));

    /** 村庄采石场菜单类型（Tier 1） */
    public static final Supplier<MenuType<VillageQuarryMenu>> VILLAGE_QUARRY_MENU =
            MENUS.register("village_quarry", () -> IMenuTypeExtension.create(VillageQuarryMenu::fromNetwork));

    /**
     * 向事件总线注册所有 Menu 类型。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        MENUS.register(bus);
    }
}

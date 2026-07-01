package com.gooodwei.civilizationevolution.client.registry;

import com.gooodwei.civilizationevolution.client.screen.machine.PrimitiveDoctorCabinScreen;
import com.gooodwei.civilizationevolution.client.screen.machine.PrimitiveFarmScreen;
import com.gooodwei.civilizationevolution.client.screen.machine.VillageQuarryScreen;
import com.gooodwei.civilizationevolution.client.screen.hatch.ItemInputHatchScreen;
import com.gooodwei.civilizationevolution.client.screen.hatch.ItemOutputHatchScreen;
import com.gooodwei.civilizationevolution.client.screen.hatch.PrimitiveFoodInputHatchScreen;
import com.gooodwei.civilizationevolution.client.screen.hatch.VillageFoodInputHatchScreen;
import com.gooodwei.civilizationevolution.client.screen.hatch.VillageItemInputHatchScreen;
import com.gooodwei.civilizationevolution.client.screen.hatch.VillageItemOutputHatchScreen;
import com.gooodwei.civilizationevolution.client.screen.machine.PrimitiveHuntingGroundScreen;
import com.gooodwei.civilizationevolution.client.screen.machine.PrimitiveCampScreen;
import com.gooodwei.civilizationevolution.client.screen.hatch.PrimitivePopulationInputHatchScreen;
import com.gooodwei.civilizationevolution.client.screen.hatch.PrimitivePopulationOutputHatchScreen;
import com.gooodwei.civilizationevolution.client.screen.machine.PrimitiveRanchScreen;
import com.gooodwei.civilizationevolution.client.screen.machine.PrimitiveControllerScreen;
import com.gooodwei.civilizationevolution.client.screen.machine.VillageControllerScreen;
import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Screen 注册表（仅客户端）。
 * 将 Menu 类型与对应的 Screen 构造器绑定。
 */
@EventBusSubscriber(modid = CivilizationEvolution.MODID, value = Dist.CLIENT)
public class ScreenRegistry {

    /**
     * 在 NeoForge 客户端事件中注册 Menu → Screen 映射。
     * @param event 屏幕注册事件
     */
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MenuRegistry.PRIMITIVE_CAMP_MENU.get(), PrimitiveCampScreen::new);
        event.register(MenuRegistry.PRIMITIVE_HUNTING_GROUND_MENU.get(), PrimitiveHuntingGroundScreen::new);
        event.register(MenuRegistry.PRIMITIVE_RANCH_MENU.get(), PrimitiveRanchScreen::new);
        event.register(MenuRegistry.PRIMITIVE_FARM_MENU.get(), PrimitiveFarmScreen::new);
        event.register(MenuRegistry.PRIMITIVE_CONTROLLER_MENU.get(), PrimitiveControllerScreen::new);
        event.register(MenuRegistry.VILLAGE_CONTROLLER_MENU.get(), VillageControllerScreen::new);
        event.register(MenuRegistry.PRIMITIVE_DOCTOR_CABIN_MENU.get(), PrimitiveDoctorCabinScreen::new);
        event.register(MenuRegistry.PRIMITIVE_POPULATION_INPUT_HATCH_MENU.get(),
                PrimitivePopulationInputHatchScreen::new);
        event.register(MenuRegistry.PRIMITIVE_FOOD_INPUT_HATCH_MENU.get(),
                PrimitiveFoodInputHatchScreen::new);
        event.register(MenuRegistry.VILLAGE_FOOD_INPUT_HATCH_MENU.get(),
                VillageFoodInputHatchScreen::new);
        event.register(MenuRegistry.PRIMITIVE_POPULATION_OUTPUT_HATCH_MENU.get(),
                PrimitivePopulationOutputHatchScreen::new);
        event.register(MenuRegistry.ITEM_INPUT_HATCH_MENU.get(), ItemInputHatchScreen::new);
        event.register(MenuRegistry.ITEM_OUTPUT_HATCH_MENU.get(), ItemOutputHatchScreen::new);
        event.register(MenuRegistry.VILLAGE_ITEM_INPUT_HATCH_MENU.get(), VillageItemInputHatchScreen::new);
        event.register(MenuRegistry.VILLAGE_ITEM_OUTPUT_HATCH_MENU.get(), VillageItemOutputHatchScreen::new);
        event.register(MenuRegistry.VILLAGE_QUARRY_MENU.get(), VillageQuarryScreen::new);
    }
}

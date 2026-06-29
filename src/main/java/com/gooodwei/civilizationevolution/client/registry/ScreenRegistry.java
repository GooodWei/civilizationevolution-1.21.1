package com.gooodwei.civilizationevolution.client.registry;

import com.gooodwei.civilizationevolution.client.screen.PrimitiveFarmScreen;
import com.gooodwei.civilizationevolution.client.screen.PrimitiveHuntingGroundScreen;
import com.gooodwei.civilizationevolution.client.screen.PrimitiveCampScreen;
import com.gooodwei.civilizationevolution.client.screen.PrimitiveRanchScreen;
import com.gooodwei.civilizationevolution.client.screen.PrimitiveControllerScreen;
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
    }
}

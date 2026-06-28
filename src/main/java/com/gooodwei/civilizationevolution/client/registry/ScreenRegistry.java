package com.gooodwei.civilizationevolution.client.registry;

import com.gooodwei.civilizationevolution.client.screen.CampScreen;
import com.gooodwei.civilizationevolution.client.screen.HuntingGroundScreen;
import com.gooodwei.civilizationevolution.client.screen.PrimitiveRanchScreen;
import com.gooodwei.civilizationevolution.client.screen.PrimitiveSettlementScreen;
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
        event.register(MenuRegistry.CAMP_MENU.get(), CampScreen::new);
        event.register(MenuRegistry.HUNTING_GROUND_MENU.get(), HuntingGroundScreen::new);
        event.register(MenuRegistry.PRIMITIVE_RANCH_MENU.get(), PrimitiveRanchScreen::new);
        event.register(MenuRegistry.PRIMITIVE_SETTLEMENT_MENU.get(), PrimitiveSettlementScreen::new);
    }
}

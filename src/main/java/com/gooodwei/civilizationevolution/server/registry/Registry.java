package com.gooodwei.civilizationevolution.server.registry;

import com.gooodwei.civilizationevolution.api.component.ModDataComponents;
import com.gooodwei.civilizationevolution.client.registry.CreativeTabRegistry;
import net.neoforged.bus.api.IEventBus;

/**
 * 模组的中央注册入口。
 * 所有子注册表均通过此类进行注册。
 */
public class Registry {

    /**
     * 将所有模组内容（方块、物品等）注册到模组事件总线。
     * 这是 CivilizationEvolution 注册时唯一需要调用的方法。
     */
    public static void registerAll(IEventBus bus) {
        ModDataComponents.register(bus);
        BlockRegistry.register(bus);
        ItemRegistry.register(bus);
        BlockEntityRegistry.register(bus);
        MenuRegistry.register(bus);
        CreativeTabRegistry.register(bus);
    }
}

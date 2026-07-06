package com.gooodwei.civilizationevolution.client;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.client.config.ClientConfig;
import com.gooodwei.civilizationevolution.network.NetworkHandler;
import com.gooodwei.civilizationevolution.server.registry.BlockRegistry;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * 文明演进模组客户端入口。
 *
 * <p>仅在物理客户端加载（{@code dist = Dist.CLIENT}），负责：
 * <ul>
 *   <li>委托 {@link NetworkHandler} 注册服务端→客户端 Payload</li>
 *   <li>注册 NeoForge 配置界面扩展点</li>
 *   <li>执行客户端初始化逻辑</li>
 * </ul>
 */
@Mod(value = CivilizationEvolution.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = CivilizationEvolution.MODID, value = Dist.CLIENT)
public class CivilizationEvolutionClient {

    /**
     * 客户端构造器。注册 Payload 处理器和配置界面扩展点。
     *
     * @param modEventBus 模组事件总线
     * @param container   模组容器
     */
    public CivilizationEvolutionClient(IEventBus modEventBus, ModContainer container) {
        modEventBus.addListener(NetworkHandler::registerClientPayloads);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /** 客户端初始化完成事件：输出玩家名和日志，加载客户端配置，注册方块渲染类型 */
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CivilizationEvolution.LOGGER.info("HELLO FROM CLIENT SETUP");

        // 加载客户端配置文件（首次启动自动生成默认配置）
        event.enqueueWork(ClientConfig::init);

        // 控制器方块注册 cutout 渲染类型，使玻璃外壳正确处理透明像素
        event.enqueueWork(() -> {
            ItemBlockRenderTypes.setRenderLayer(BlockRegistry.PRIMITIVE_CONTROLLER.get(), RenderType.cutout());
            ItemBlockRenderTypes.setRenderLayer(BlockRegistry.VILLAGE_CONTROLLER.get(), RenderType.cutout());
        });
    }
}

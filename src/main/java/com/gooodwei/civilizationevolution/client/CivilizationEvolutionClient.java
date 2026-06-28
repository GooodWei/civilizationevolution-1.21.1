package com.gooodwei.civilizationevolution.client;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.network.SyncMachineListPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * 文明演进模组客户端入口。
 *
 * <p>仅在物理客户端加载（{@code dist = Dist.CLIENT}），负责：
 * <ul>
 *   <li>注册服务端→客户端 Payload（{@link SyncMachineListPayload}）</li>
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
        modEventBus.addListener(CivilizationEvolutionClient::registerClientPayloads);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    /**
     * 注册服务端→客户端 Payload 的编解码器和处理器。
     * 当前注册 {@link SyncMachineListPayload}。
     */
    private static void registerClientPayloads(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");
        registrar.playToClient(
                SyncMachineListPayload.TYPE,
                SyncMachineListPayload.STREAM_CODEC,
                ClientPayloadHandler::handleSyncMachineList
        );
    }

    /** 客户端初始化完成事件：输出玩家名和日志 */
    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        CivilizationEvolution.LOGGER.info("HELLO FROM CLIENT SETUP");
        CivilizationEvolution.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}

package com.gooodwei.civilizationevolution.client.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.client.renderer.ControllerBeamRenderer;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * BlockEntityRenderer 注册表（仅客户端）。
 *
 * <p>将所有 BER 与对应的 BlockEntity 类型绑定。
 * 沿用 {@link ScreenRegistry} 的 {@code @EventBusSubscriber} +
 * {@code @SubscribeEvent} 静态方法模式。
 *
 * <p>当前注册：
 * <ul>
 *   <li>{@link ControllerBeamRenderer} → {@code PRIMITIVE_CONTROLLER} / {@code VILLAGE_CONTROLLER}</li>
 * </ul>
 */
@EventBusSubscriber(modid = CivilizationEvolution.MODID, value = Dist.CLIENT)
public class RendererRegistry {

    /**
     * 注册 BlockEntityRenderer 到对应的 BlockEntity 类型。
     *
     * @param event 渲染器注册事件（模组总线）
     */
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                BlockEntityRegistry.PRIMITIVE_CONTROLLER.get(),
                ControllerBeamRenderer::new
        );
        event.registerBlockEntityRenderer(
                BlockEntityRegistry.VILLAGE_CONTROLLER.get(),
                ControllerBeamRenderer::new
        );
    }
}

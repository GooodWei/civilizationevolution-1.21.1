package com.gooodwei.civilizationevolution.client;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.api.CivilizationAPI;
import com.gooodwei.civilizationevolution.api.career.Career;
import com.gooodwei.civilizationevolution.server.registry.ItemRegistry;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端专用的物品属性（ItemProperty）注册器。
 *
 * <p>为人口物品（PopulationItem）注册名为 {@code career} 的 property override，
 * 根据物品 NBT 中的 {@code career} 字段动态切换模型贴图：
 * <ul>
 *   <li>读取 {@code CUSTOM_DATA} 组件中的 {@code career} 字段</li>
 *   <li>在职业注册表中查找对应的 {@link Career} 实例</li>
 *   <li>将 {@code modelIndex / 100.0f} 作为 predicate 值返回，
 *       模型 JSON 中对应定义多个 predicate 值，每个对应一种职业贴图</li>
 * </ul>
 *
 * <p>仅在物理客户端加载（{@link Dist#CLIENT}）。</p>
 */
@EventBusSubscriber(modid = CivilizationEvolution.MODID, value = Dist.CLIENT)
public final class ClientItemProperties {

    private ClientItemProperties() {}

    /**
     * 客户端初始化时注册人口物品的职业贴图 predicate。
     *
     * <p>使用 {@link FMLClientSetupEvent#enqueueWork} 确保在安全线程上下文中执行。</p>
     *
     * @param event 客户端初始化事件
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ItemProperties.register(
                    ItemRegistry.POPULATION.get(),
                    ResourceLocation.fromNamespaceAndPath(CivilizationEvolution.MODID, "career"),
                    (stack, level, entity, seed) -> {
                        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                        if (tag.isEmpty() || !tag.contains("career")) return 0.0f;
                        Career c = CivilizationAPI.getCareerRegistry().byName(tag.getString("career"));
                        return c != null ? c.getModelIndex() / 100.0f : 0.0f;
                    }
            );
        });
    }
}

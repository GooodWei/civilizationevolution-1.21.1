package com.gooodwei.civilizationevolution.api.component;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 模组自定义 DataComponent 注册表。
 *
 * <p>参考 AE2 的 {@code AEComponents} 模式，使用 {@link DataComponentType}
 * 替代旧版 NBT 实现物品数据的持久化存储与网络同步。
 */
public class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, CivilizationEvolution.MODID);

    /**
     * 文明核心 UUID，以字符串形式存储。
     * 合成时自动生成，此后随物品永久绑定。
     */
    public static final Supplier<DataComponentType<String>> CIVILIZATION_CORE_ID =
            DATA_COMPONENTS.register("civilization_core_id",
                    () -> DataComponentType.<String>builder()
                            .persistent(Codec.STRING)
                            .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                            .build());

    /**
     * 连接器物品记录的目标核心 UUID。
     * 通过 {@link ConnectorTarget#CODEC} 持久化、{@link ConnectorTarget#STREAM_CODEC} 网络同步。
     */
    public static final Supplier<DataComponentType<ConnectorTarget>> CONNECTOR_TARGET =
            DATA_COMPONENTS.register("connector_target",
                    () -> DataComponentType.<ConnectorTarget>builder()
                            .persistent(ConnectorTarget.CODEC)
                            .networkSynchronized(ConnectorTarget.STREAM_CODEC)
                            .build());

    /**
     * 向 NeoForge 事件总线注册所有 DataComponent。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}

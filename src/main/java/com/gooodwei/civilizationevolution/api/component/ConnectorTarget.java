package com.gooodwei.civilizationevolution.api.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 连接器记录的目标——文明核心的 UUID。
 * <p>机器绑定到核心而非控制器方块，核心移动到新控制器时绑定数据自动跟随。
 */
public record ConnectorTarget(String coreUuid) {
    public static final Codec<ConnectorTarget> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Codec.STRING.fieldOf("coreUuid").forGetter(ConnectorTarget::coreUuid)
        ).apply(instance, ConnectorTarget::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ConnectorTarget> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ConnectorTarget::coreUuid,
            ConnectorTarget::new
        );
}

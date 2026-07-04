package com.gooodwei.civilizationevolution.network;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 服务端→客户端：同步超大堆叠容器的完整内容。
 * 照搬 SophisticatedCore {@code SyncContainerStacksPayload}。
 *
 * <p>与原版 {@code ClientboundContainerSetContentPacket} 的差异：
 * 使用 {@link ItemStack#OPTIONAL_STREAM_CODEC} 序列化（不验证 count 范围），
 * 可安全传输 count > 99 的 ItemStack。
 *
 * @param containerId 容器窗口 ID
 * @param stateId     状态 ID（递增，防止乱序更新）
 * @param stacks      所有物品栈列表
 * @param carriedStack 玩家手持物品
 */
public record SyncOversizedStacksPayload(int containerId, int stateId,
                                          List<ItemStack> stacks,
                                          ItemStack carriedStack) implements CustomPacketPayload {

    public static final Type<SyncOversizedStacksPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CivilizationEvolution.MODID, "sync_oversized_stacks"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncOversizedStacksPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SyncOversizedStacksPayload::containerId,
                    ByteBufCodecs.INT,
                    SyncOversizedStacksPayload::stateId,
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SyncOversizedStacksPayload::stacks,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    SyncOversizedStacksPayload::carriedStack,
                    SyncOversizedStacksPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

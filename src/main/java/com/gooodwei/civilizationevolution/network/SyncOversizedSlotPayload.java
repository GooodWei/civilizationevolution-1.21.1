package com.gooodwei.civilizationevolution.network;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 服务端→客户端：同步超大堆叠容器的单个槽位变化。
 * 照搬 SophisticatedCore {@code SyncSlotStackPayload}。
 *
 * <p>使用 {@link ItemStack#OPTIONAL_STREAM_CODEC} 序列化（不验证 count 范围）。
 *
 * @param containerId 容器窗口 ID
 * @param stateId     状态 ID（递增，防止乱序更新）
 * @param slotIndex   槽位索引
 * @param stack       槽位中的物品栈（count 可能 > 99）
 */
public record SyncOversizedSlotPayload(int containerId, int stateId,
                                        int slotIndex,
                                        ItemStack stack) implements CustomPacketPayload {

    public static final Type<SyncOversizedSlotPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CivilizationEvolution.MODID, "sync_oversized_slot"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncOversizedSlotPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT,
                    SyncOversizedSlotPayload::containerId,
                    ByteBufCodecs.INT,
                    SyncOversizedSlotPayload::stateId,
                    ByteBufCodecs.INT,
                    SyncOversizedSlotPayload::slotIndex,
                    ItemStack.OPTIONAL_STREAM_CODEC,
                    SyncOversizedSlotPayload::stack,
                    SyncOversizedSlotPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

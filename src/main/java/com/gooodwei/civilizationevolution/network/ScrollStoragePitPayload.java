package com.gooodwei.civilizationevolution.network;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveStoragePitMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：同步储物坑 GUI 的滚动偏移。
 *
 * <p>当玩家在储物坑 GUI 中滚动鼠标滚轮或拖拽滚动条时，
 * 客户端通过此包将新的滚动偏移发送到服务端。
 * 服务端收到后调用 {@link PrimitiveStoragePitMenu#rebuildStorageSlots(int)}
 * 重建可见存储槽位，保证两端槽位索引一致。
 */
public record ScrollStoragePitPayload(int scrollOffset) implements CustomPacketPayload {

    public static final Type<ScrollStoragePitPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CivilizationEvolution.MODID, "scroll_storage_pit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScrollStoragePitPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ScrollStoragePitPayload::scrollOffset,
                    ScrollStoragePitPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

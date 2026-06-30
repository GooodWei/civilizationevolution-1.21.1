package com.gooodwei.civilizationevolution.network;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.api.PreviewBlockInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.List;

/**
 * 服务端→客户端：同步多方块结构预览数据。
 *
 * <p>当 blocks 列表非空时表示开启预览，空列表表示停止预览。
 * 客户端收到后存入 {@code PreviewState}，由 {@code StructurePreviewRenderer} 渲染。
 */
public record StructurePreviewPayload(BlockPos controllerPos, List<PreviewBlockInfo> blocks)
        implements CustomPacketPayload {

    public static final Type<StructurePreviewPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CivilizationEvolution.MODID, "structure_preview"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StructurePreviewPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    StructurePreviewPayload::controllerPos,
                    PreviewBlockInfo.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    StructurePreviewPayload::blocks,
                    StructurePreviewPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 创建一个停止预览的 Payload（blocks 为空列表）。
     */
    public static StructurePreviewPayload stop(BlockPos controllerPos) {
        return new StructurePreviewPayload(controllerPos, Collections.emptyList());
    }

    /**
     * 是否请求开启预览（blocks 非空）。
     */
    public boolean isStart() {
        return !blocks.isEmpty();
    }
}

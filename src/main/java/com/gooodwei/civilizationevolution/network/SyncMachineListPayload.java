package com.gooodwei.civilizationevolution.network;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 服务端→客户端：同步绑定机器列表到原始聚落 GUI。
 * 每条目包含机器坐标、启用状态和机器名称。
 */
public record SyncMachineListPayload(List<MachineEntry> machines) implements CustomPacketPayload {

    public static final Type<SyncMachineListPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CivilizationEvolution.MODID, "sync_machine_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMachineListPayload> STREAM_CODEC =
            StreamCodec.composite(
                    MachineEntry.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    SyncMachineListPayload::machines,
                    SyncMachineListPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** 单条机器条目，包含坐标、启用状态、名称和所在维度 */
    public record MachineEntry(BlockPos pos, boolean enabled, String machineName, String dimension) {
        public static final StreamCodec<RegistryFriendlyByteBuf, MachineEntry> STREAM_CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC,     MachineEntry::pos,
                        ByteBufCodecs.BOOL,        MachineEntry::enabled,
                        ByteBufCodecs.STRING_UTF8, MachineEntry::machineName,
                        ByteBufCodecs.STRING_UTF8, MachineEntry::dimension,
                        MachineEntry::new
                );
    }
}

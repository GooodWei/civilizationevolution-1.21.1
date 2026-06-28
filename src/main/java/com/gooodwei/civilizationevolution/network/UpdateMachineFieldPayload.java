package com.gooodwei.civilizationevolution.network;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端→服务端：更新机器字段的通用 Payload。
 *
 * <p>不针对每种操作单独定义 Payload，而是通过 {@code fieldId} 区分操作类型
 * （解绑、启停等），数据通过 {@code CompoundTag} 携带。各 BE 在
 * {@link IClientUpdateReceiver#onClientUpdate} 中按 fieldId 分派。
 *
 * @param pos     目标 BlockEntity 坐标
 * @param fieldId 字段编号（各 BE 自行定义，如 {@code FIELD_UNBIND_MACHINE}）
 * @param data    操作数据（如机器坐标、启用标志）
 */
public record UpdateMachineFieldPayload(BlockPos pos, int fieldId, CompoundTag data) implements CustomPacketPayload {

    /** Payload 类型标识 */
    public static final Type<UpdateMachineFieldPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    CivilizationEvolution.MODID, "update_machine_field"));

    /** 编解码器：依次写入/读取 BlockPos、fieldId、CompoundTag */
    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateMachineFieldPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,    UpdateMachineFieldPayload::pos,
                    ByteBufCodecs.VAR_INT,    UpdateMachineFieldPayload::fieldId,
                    ByteBufCodecs.COMPOUND_TAG,    UpdateMachineFieldPayload::data,
                    UpdateMachineFieldPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

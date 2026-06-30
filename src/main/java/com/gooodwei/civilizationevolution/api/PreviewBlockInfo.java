package com.gooodwei.civilizationevolution.api;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * 多方块结构预览的单个位置数据。
 *
 * <p>由服务端 {@link IMultiBlockMachine#collectPreviewPositions()} 收集，
 * 通过 {@code StructurePreviewPayload} 发送到客户端用于全息渲染。
 *
 * @param pos           该位置的世界坐标
 * @param expectedTypes 该位置接受的 type 字符串集合（如 {@code ["minecraft:stone_bricks"]}）
 * @param patternChar   对应的模式字符（预留，可用于按 key 分组着色）
 */
public record PreviewBlockInfo(BlockPos pos, List<String> expectedTypes, char patternChar) {

    /** 将 char 编码为单字节（模式字符均为 ASCII） */
    private static final StreamCodec<RegistryFriendlyByteBuf, Character> CHAR_CODEC =
            StreamCodec.of(
                    (buf, c) -> buf.writeByte((byte) c.charValue()),
                    buf -> (char) buf.readByte()
            );

    public static final StreamCodec<RegistryFriendlyByteBuf, PreviewBlockInfo> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC,
                    PreviewBlockInfo::pos,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    PreviewBlockInfo::expectedTypes,
                    CHAR_CODEC,
                    PreviewBlockInfo::patternChar,
                    PreviewBlockInfo::new
            );
}

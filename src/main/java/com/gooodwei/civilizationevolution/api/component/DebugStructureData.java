package com.gooodwei.civilizationevolution.api.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * 结构调试获取器存储的坐标数据，通过 DataComponent 持久化到物品上。
 *
 * @param x1      pos1 X 坐标
 * @param y1      pos1 Y 坐标
 * @param z1      pos1 Z 坐标
 * @param dim1    pos1 所在维度 ID（{@code level.dimension().location().toString()}）
 * @param x2      pos2 X 坐标
 * @param y2      pos2 Y 坐标
 * @param z2      pos2 Z 坐标
 * @param dim2    pos2 所在维度 ID
 * @param hasPos1 是否已设定 pos1
 * @param hasPos2 是否已设定 pos2
 */
public record DebugStructureData(
        int x1, int y1, int z1, String dim1,
        int x2, int y2, int z2, String dim2,
        boolean hasPos1, boolean hasPos2) {

    /** 空数据常量，两个坐标均未设定 */
    public static final DebugStructureData EMPTY =
            new DebugStructureData(0, 0, 0, "", 0, 0, 0, "", false, false);

    /** 持久化 Codec */
    public static final Codec<DebugStructureData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.INT.fieldOf("x1").forGetter(DebugStructureData::x1),
                    Codec.INT.fieldOf("y1").forGetter(DebugStructureData::y1),
                    Codec.INT.fieldOf("z1").forGetter(DebugStructureData::z1),
                    Codec.STRING.fieldOf("dim1").forGetter(DebugStructureData::dim1),
                    Codec.INT.fieldOf("x2").forGetter(DebugStructureData::x2),
                    Codec.INT.fieldOf("y2").forGetter(DebugStructureData::y2),
                    Codec.INT.fieldOf("z2").forGetter(DebugStructureData::z2),
                    Codec.STRING.fieldOf("dim2").forGetter(DebugStructureData::dim2),
                    Codec.BOOL.fieldOf("hasPos1").forGetter(DebugStructureData::hasPos1),
                    Codec.BOOL.fieldOf("hasPos2").forGetter(DebugStructureData::hasPos2)
            ).apply(instance, DebugStructureData::new));

    /** 网络同步 StreamCodec（字段数 > 6，手动编解码） */
    public static final StreamCodec<ByteBuf, DebugStructureData> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public DebugStructureData decode(ByteBuf buf) {
                    int x1 = ByteBufCodecs.VAR_INT.decode(buf);
                    int y1 = ByteBufCodecs.VAR_INT.decode(buf);
                    int z1 = ByteBufCodecs.VAR_INT.decode(buf);
                    String dim1 = ByteBufCodecs.STRING_UTF8.decode(buf);
                    int x2 = ByteBufCodecs.VAR_INT.decode(buf);
                    int y2 = ByteBufCodecs.VAR_INT.decode(buf);
                    int z2 = ByteBufCodecs.VAR_INT.decode(buf);
                    String dim2 = ByteBufCodecs.STRING_UTF8.decode(buf);
                    boolean hasPos1 = ByteBufCodecs.BOOL.decode(buf);
                    boolean hasPos2 = ByteBufCodecs.BOOL.decode(buf);
                    return new DebugStructureData(x1, y1, z1, dim1, x2, y2, z2, dim2, hasPos1, hasPos2);
                }

                @Override
                public void encode(ByteBuf buf, DebugStructureData d) {
                    ByteBufCodecs.VAR_INT.encode(buf, d.x1);
                    ByteBufCodecs.VAR_INT.encode(buf, d.y1);
                    ByteBufCodecs.VAR_INT.encode(buf, d.z1);
                    ByteBufCodecs.STRING_UTF8.encode(buf, d.dim1);
                    ByteBufCodecs.VAR_INT.encode(buf, d.x2);
                    ByteBufCodecs.VAR_INT.encode(buf, d.y2);
                    ByteBufCodecs.VAR_INT.encode(buf, d.z2);
                    ByteBufCodecs.STRING_UTF8.encode(buf, d.dim2);
                    ByteBufCodecs.BOOL.encode(buf, d.hasPos1);
                    ByteBufCodecs.BOOL.encode(buf, d.hasPos2);
                }
            };

    /**
     * 返回设定了 pos1 的新实例。
     *
     * @param x   X 坐标
     * @param y   Y 坐标
     * @param z   Z 坐标
     * @param dim 维度 ID 字符串
     * @return 更新了 pos1 的新 DebugStructureData
     */
    public DebugStructureData withPos1(int x, int y, int z, String dim) {
        return new DebugStructureData(x, y, z, dim, x2, y2, z2, dim2, true, hasPos2);
    }

    /**
     * 返回设定了 pos2 的新实例。
     *
     * @param x   X 坐标
     * @param y   Y 坐标
     * @param z   Z 坐标
     * @param dim 维度 ID 字符串
     * @return 更新了 pos2 的新 DebugStructureData
     */
    public DebugStructureData withPos2(int x, int y, int z, String dim) {
        return new DebugStructureData(x1, y1, z1, dim1, x, y, z, dim, hasPos1, true);
    }
}

package com.gooodwei.civilizationevolution.server.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;


/**
 * 研磨配方的一条输出产物。
 *
 * @param item                  产出物品
 * @param allowExtraOutputTier  机器效率影响产出的最低 Tier。
 *                              <ul>
 *                                <li>-1：不受机器效率影响，始终产出固定数量</li>
 *                                <li>≥0：机器 Tier 达到该值时，效率开始乘算产出数量</li>
 *                              </ul>
 */
public record GrindingOutput(ItemStack item, int allowExtraOutputTier) {

    /** JSON 序列化器 */
    public static final MapCodec<GrindingOutput> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    ItemStack.CODEC.fieldOf("item")
                            .forGetter(GrindingOutput::item),
                    Codec.INT.optionalFieldOf("allow_extra_output_tier", -1)
                            .forGetter(GrindingOutput::allowExtraOutputTier)
            ).apply(instance, GrindingOutput::new)
    );

    /** 网络同步序列化器 */
    public static final StreamCodec<RegistryFriendlyByteBuf, GrindingOutput> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC,
                    GrindingOutput::item,
                    ByteBufCodecs.INT,
                    GrindingOutput::allowExtraOutputTier,
                    GrindingOutput::new
            );

}

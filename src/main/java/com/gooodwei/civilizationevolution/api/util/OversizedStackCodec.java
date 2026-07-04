package com.gooodwei.civilizationevolution.api.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * 超大堆叠物品栈的 Codec 工具类。
 * 参照 SophisticatedCore {@code CodecHelper.OVERSIZED_ITEM_STACK_CODEC} 设计。
 *
 * <p>与 vanilla {@link ItemStack#CODEC} 的唯一区别：count 字段使用 {@link Codec#INT}
 * 而非 {@code Codec.intRange(1, 99)}，允许序列化任意正整数的 count 值。
 *
 * <p>用途：
 * <ul>
 *   <li>NBT 序列化/反序列化（替换 {@code ContainerHelper.saveAllItems/loadAllItems}）</li>
 *   <li>未来 DataComponent 中的 oversize 物品栈存储</li>
 *   <li>网络包 payload 中的显式序列化（当不使用 STREAM_CODEC 时）</li>
 * </ul>
 */
public final class OversizedStackCodec {
    /** 不限制 count 范围的 ItemStack Codec（照搬 SS CodecHelper 的设计） */
    public static final Codec<ItemStack> CODEC = Codec.lazyInitialized(() ->
            RecordCodecBuilder.create(instance -> instance.group(
                    ItemStack.ITEM_NON_AIR_CODEC.fieldOf("id").forGetter(ItemStack::getItemHolder),
                    Codec.INT.fieldOf("count").orElse(1).forGetter(ItemStack::getCount),
                    DataComponentPatch.CODEC.optionalFieldOf("components", DataComponentPatch.EMPTY)
                            .forGetter(ItemStack::getComponentsPatch)
            ).apply(instance, ItemStack::new))
    );

    /** 可选空 map → ItemStack.EMPTY 的 codec */
    public static final Codec<ItemStack> OPTIONAL_CODEC = ExtraCodecs.optionalEmptyMap(CODEC)
            .xmap(opt -> opt.orElse(ItemStack.EMPTY),
                    stack -> stack.isEmpty() ? Optional.empty() : Optional.of(stack));

    /**
     * 保存物品列表到 ListTag（不验证 count 范围）。
     * 替代原版 {@code ContainerHelper.saveAllItems}。
     *
     * @param items      物品列表
     * @param registries HolderLookup 提供者
     * @return 序列化后的 ListTag
     */
    public static ListTag saveAllItems(NonNullList<ItemStack> items, HolderLookup.Provider registries) {
        ListTag listTag = new ListTag();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                CompoundTag itemTag = new CompoundTag();
                itemTag.putByte("Slot", (byte) i);
                // 使用自定义 codec 序列化，count 不限于 99
                DataResult<Tag> result = CODEC.encodeStart(
                        registries.createSerializationContext(NbtOps.INSTANCE), stack);
                result.result().ifPresent(element -> {
                    itemTag.put("item", element);
                    listTag.add(itemTag);
                });
            }
        }
        return listTag;
    }

    /**
     * 从 ListTag 加载物品列表（不验证 count 范围）。
     * 替代原版 {@code ContainerHelper.loadAllItems}。
     *
     * @param listTag    序列化的 ListTag
     * @param items      目标物品列表（会被修改）
     * @param registries HolderLookup 提供者
     */
    public static void loadAllItems(ListTag listTag, NonNullList<ItemStack> items,
                                     HolderLookup.Provider registries) {
        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag itemTag = listTag.getCompound(i);
            int slot = itemTag.getByte("Slot") & 255;
            if (slot >= 0 && slot < items.size()) {
                Tag itemElement = itemTag.get("item");
                if (itemElement != null) {
                    DataResult<ItemStack> result = CODEC.parse(
                            registries.createSerializationContext(NbtOps.INSTANCE), itemElement);
                    result.result().ifPresent(stack -> items.set(slot, stack));
                }
            }
        }
    }

    private OversizedStackCodec() {}
}

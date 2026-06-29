package com.gooodwei.civilizationevolution.tags;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * 模组自定义标签，便于在 {@code mayPlace} 等方法中做槽位限制。
 */
public class ModTags {

    /** 武器标签：包含原版 #swords、#axes、三叉戟、重锤、弓、弩 */
    public static final TagKey<Item> WEAPONS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "weapons")
    );

    /** 狩猎场冲突检测标签：扫描范围内命中此标签的方块视为冲突 */
    public static final TagKey<Block> HUNTING_GROUND_CONFLICTS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "hunting_ground_conflicts")
    );

    /** 原始牧场冲突检测标签：扫描范围内命中此标签的方块视为冲突 */
    public static final TagKey<Block> PRIMITIVE_RANCH_CONFLICTS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "primitive_ranch_conflicts")
    );

    /** 农场冲突检测标签：扫描范围内命中此标签的方块视为冲突，防止两个农场并发操作同一作物 */
    public static final TagKey<Block> FARM_CONFLICTS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "farm_conflicts")
    );

    private ModTags() {
        // 工具类，禁止实例化
    }
}

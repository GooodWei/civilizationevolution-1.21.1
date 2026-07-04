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

    /** 锄头标签：包含原版 #minecraft:hoes，用于收割机锄槽过滤 */
    public static final TagKey<Item> HOES = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "hoes")
    );

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

    /** 镐子标签：包含 #minecraft:pickaxes，用于采石场镐槽过滤 */
    public static final TagKey<Item> PICKAXES = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "pickaxes")
    );

    /** 农场冲突检测标签：扫描范围内命中此标签的方块视为冲突，防止两个农场并发操作同一作物 */
    public static final TagKey<Block> FARM_CONFLICTS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "farm_conflicts")
    );

    /** 采石场可挖掘方块白名单：只有此标签内的方块才会被列入采掘列表 */
    public static final TagKey<Block> QUARRY_MINEABLE = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "quarry_mineable")
    );

    /** 收割机冲突检测标签：扫描范围内命中此标签的方块视为冲突，防止两个收割机并发操作同一作物 */
    public static final TagKey<Block> HARVESTER_CONFLICTS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "harvester_conflicts")
    );

    private ModTags() {
        // 工具类，禁止实例化
    }
}

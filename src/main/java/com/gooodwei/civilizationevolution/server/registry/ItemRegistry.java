package com.gooodwei.civilizationevolution.server.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.api.tier.ModTiers;
import com.gooodwei.civilizationevolution.server.item.CivilizationCoreExtractorItem;
import com.gooodwei.civilizationevolution.server.item.CivilizationCoreItem;
import com.gooodwei.civilizationevolution.server.item.ConnectorItem;
import com.gooodwei.civilizationevolution.server.item.PopulationItem;
import com.gooodwei.civilizationevolution.server.item.Recruiter;
import com.gooodwei.civilizationevolution.server.item.TieredBlockItem;
import net.minecraft.world.item.BlockItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 物品注册表。通过 NeoForge {@link DeferredRegister} 注册所有自定义物品和方块物品。
 */
public class ItemRegistry {

    /** 物品注册器 */
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CivilizationEvolution.MODID);

    /** 人口物品 —— 存储人口属性（职业、年龄、生命值等），最大堆叠 1 */
    public static final DeferredItem<PopulationItem> POPULATION =
            ITEMS.registerItem("population", properties -> new PopulationItem(properties.stacksTo(1)));

    /** 招募者 —— 右键村民将其转化为人口物品，最大堆叠 1 */
    public static final DeferredItem<Recruiter> RECRUITER =
            ITEMS.registerItem("recruiter", properties -> new Recruiter(properties.stacksTo(1)));

    /** 原始营地方块物品（Tier 0：原始时代） */
    public static final DeferredItem<BlockItem> PRIMITIVE_CAMP_BLOCK_ITEM =
            ITEMS.registerItem("primitive_camp", properties ->
                    new TieredBlockItem(BlockRegistry.PRIMITIVE_CAMP_BLOCK.get(), properties,
                            ModTiers.PRIMITIVE));

    /** 狩猎场方块物品（Tier 0：原始时代） */
    public static final DeferredItem<BlockItem> HUNTING_GROUND =
            ITEMS.registerItem("hunting_ground", properties ->
                    new TieredBlockItem(BlockRegistry.HUNTING_GROUND.get(), properties,
                            ModTiers.PRIMITIVE));

    /** 原始聚落方块物品（Tier 0：原始时代） */
    public static final DeferredItem<BlockItem> PRIMITIVE_SETTLEMENT =
            ITEMS.registerItem("primitive_settlement", properties ->
                    new TieredBlockItem(BlockRegistry.PRIMITIVE_SETTLEMENT.get(), properties,
                            ModTiers.PRIMITIVE));

    /** 文明核心 —— 控制器的数据存储介质，携带 UUID，最大堆叠 1 */
    public static final DeferredItem<CivilizationCoreItem> CIVILIZATION_CORE =
            ITEMS.registerItem("civilization_core", CivilizationCoreItem::new);

    /** 文明核心提取器 —— 从已绑定核心的机器中提取核心 UUID，最大堆叠 1 */
    public static final DeferredItem<CivilizationCoreExtractorItem> CIVILIZATION_CORE_EXTRACTOR =
            ITEMS.registerItem("civilization_core_extractor", CivilizationCoreExtractorItem::new);

    /** 连接器 —— 将人口机器与文明核心绑定/解绑/切换，最大堆叠 1 */
    public static final DeferredItem<ConnectorItem> CONNECTOR =
            ITEMS.registerItem("connector", properties -> new ConnectorItem(properties.stacksTo(1)));

    /** 原始牧地方块物品（Tier 0：原始时代） */
    public static final DeferredItem<BlockItem> PRIMITIVE_RANCH =
            ITEMS.registerItem("primitive_ranch", properties ->
                    new TieredBlockItem(BlockRegistry.PRIMITIVE_RANCH_BLOCK.get(), properties,
                            ModTiers.PRIMITIVE));

    /** 村庄控制器方块物品（Tier 1：村庄时代） */
    public static final DeferredItem<BlockItem> VILLAGE_CONTROLLER =
            ITEMS.registerItem("village_controller", properties ->
                    new TieredBlockItem(BlockRegistry.VILLAGE_CONTROLLER.get(), properties,
                            ModTiers.VILLAGE));
    /**
     * 向事件总线注册所有物品。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}

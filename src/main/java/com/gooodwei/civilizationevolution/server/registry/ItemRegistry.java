package com.gooodwei.civilizationevolution.server.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.item.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
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

    /** 小麦粉 —— 研磨小麦的产物，最大堆叠 64 */
    public static final DeferredItem<Item> WHEAT_FLOUR =
            ITEMS.registerItem("wheat_flour", Item::new);

    /** 原始营地方块物品（Tier 0：原始时代） */
    public static final DeferredItem<BlockItem> PRIMITIVE_CAMP_BLOCK_ITEM =
            ITEMS.registerItem("primitive_camp", properties ->
                    new TieredBlockItem(BlockRegistry.PRIMITIVE_CAMP_BLOCK.get(), properties));

    /** 狩猎场方块物品（Tier 0：原始时代） */
    public static final DeferredItem<BlockItem> PRIMITIVE_HUNTING_GROUND =
            ITEMS.registerItem("primitive_hunting_ground", properties ->
                    new TieredBlockItem(BlockRegistry.PRIMITIVE_HUNTING_GROUND.get(), properties));

    /** 原始控制器方块物品（Tier 0：原始时代） */
    public static final DeferredItem<BlockItem> PRIMITIVE_CONTROLLER =
            ITEMS.registerItem("primitive_controller", properties ->
                    new TieredBlockItem(BlockRegistry.PRIMITIVE_CONTROLLER.get(), properties));

    /** 文明核心 —— 控制器的数据存储介质，携带 UUID，最大堆叠 1。craftRemainder 在 CivilizationCoreItem 中覆写以避免静态初始化自引用 */
    public static final DeferredItem<CivilizationCoreItem> CIVILIZATION_CORE =
            ITEMS.registerItem("civilization_core", CivilizationCoreItem::new, new Item.Properties().rarity(Rarity.EPIC));

    /** 文明核心提取器 —— 从已绑定核心的机器中提取核心 UUID，最大堆叠 1 */
    public static final DeferredItem<CivilizationCoreExtractorItem> CIVILIZATION_CORE_EXTRACTOR =
            ITEMS.registerItem("civilization_core_extractor", CivilizationCoreExtractorItem::new);

    /** 连接器 —— 将人口机器与文明核心绑定/解绑/切换，最大堆叠 1 */
    public static final DeferredItem<ConnectorItem> CONNECTOR =
            ITEMS.registerItem("connector", properties -> new ConnectorItem(properties.stacksTo(1)));

    /** 原始牧地方块物品（Tier 0：原始时代） */
    public static final DeferredItem<BlockItem> PRIMITIVE_RANCH =
            ITEMS.registerItem("primitive_ranch", properties ->
                    new TieredBlockItem(BlockRegistry.PRIMITIVE_RANCH_BLOCK.get(), properties));

    /** 村庄控制器方块物品（Tier 1：村庄时代） */
    public static final DeferredItem<BlockItem> VILLAGE_CONTROLLER =
            ITEMS.registerItem("village_controller", properties ->
                    new TieredBlockItem(BlockRegistry.VILLAGE_CONTROLLER.get(), properties));

    /** 原始农场方块物品（Tier 0：原始时代） */
    public static final DeferredItem<BlockItem> PRIMITIVE_FARM =
            ITEMS.registerItem("primitive_farm", properties ->
                    new TieredBlockItem(BlockRegistry.PRIMITIVE_FARM_BLOCK.get(), properties));

    /** 村庄结构方块物品 */
    public static final DeferredItem<BlockItem> VILLAGE_STRUCTURE_CASING =
            ITEMS.registerItem("village_structure_casing", properties ->
                    new BlockItem(BlockRegistry.VILLAGE_STRUCTURE_CASING.get(), properties));

    /** 原始结构方块物品 */
    public static final DeferredItem<BlockItem> PRIMITIVE_STRUCTURE_CASING =
            ITEMS.registerItem("primitive_structure_casing", properties ->
                    new BlockItem(BlockRegistry.PRIMITIVE_STRUCTURE_CASING.get(), properties));

    /** 原始人口输入接口方块物品 */
    public static final DeferredItem<BlockItem> PRIMITIVE_POPULATION_INPUT_HATCH =
            ITEMS.registerItem("primitive_population_input_hatch", properties ->
                    new BlockItem(BlockRegistry.PRIMITIVE_POPULATION_INPUT_HATCH.get(), properties));

    /** 原始食物输入接口方块物品 */
    public static final DeferredItem<BlockItem> PRIMITIVE_FOOD_INPUT_HATCH =
            ITEMS.registerItem("primitive_food_input_hatch", properties ->
                    new BlockItem(BlockRegistry.PRIMITIVE_FOOD_INPUT_HATCH.get(), properties));

    /** 村庄食物输入接口方块物品（Tier 1：村庄时代） */
    public static final DeferredItem<BlockItem> VILLAGE_FOOD_INPUT_HATCH =
            ITEMS.registerItem("village_food_input_hatch", properties ->
                    new BlockItem(BlockRegistry.VILLAGE_FOOD_INPUT_HATCH.get(), properties));

    /** 原始人口输出接口方块物品 */
    public static final DeferredItem<BlockItem> PRIMITIVE_POPULATION_OUTPUT_HATCH =
            ITEMS.registerItem("primitive_population_output_hatch", properties ->
                    new BlockItem(BlockRegistry.PRIMITIVE_POPULATION_OUTPUT_HATCH.get(), properties));

    /** 结构调试获取器 —— 框选多方块结构区域并导出 JSON，最大堆叠 1 */
    public static final DeferredItem<DebugStructureGetterItem> DEBUG_STRUCTURE_GETTER =
            ITEMS.registerItem("debug_structure_getter",
                    properties -> new DebugStructureGetterItem(properties.stacksTo(1)));

    /** 多方块结构投影仪 —— 右键多方块机器核心切换结构预览渲染，最大堆叠 1 */
    public static final DeferredItem<ProjectorItem> PROJECTOR =
            ITEMS.registerItem("projector",
                    properties -> new ProjectorItem(properties.stacksTo(1)));

    /** 原始诊所方块物品（Tier 0：原始时代） */
    public static final DeferredItem<BlockItem> PRIMITIVE_DOCTOR_CABIN =
            ITEMS.registerItem("primitive_doctor_cabin", properties ->
                    new TieredBlockItem(BlockRegistry.PRIMITIVE_DOCTOR_CABIN.get(), properties));

    /** 原始物品输入接口方块物品 */
    public static final DeferredItem<BlockItem> PRIMITIVE_ITEM_INPUT_HATCH =
            ITEMS.registerItem("primitive_item_input_hatch", properties ->
                    new BlockItem(BlockRegistry.PRIMITIVE_ITEM_INPUT_HATCH.get(), properties));

    /** 村庄物品输入接口方块物品 */
    public static final DeferredItem<BlockItem> VILLAGE_ITEM_INPUT_HATCH =
            ITEMS.registerItem("village_item_input_hatch", properties ->
                    new BlockItem(BlockRegistry.VILLAGE_ITEM_INPUT_HATCH.get(), properties));

    /** 原始物品输出接口方块物品 */
    public static final DeferredItem<BlockItem> PRIMITIVE_ITEM_OUTPUT_HATCH =
            ITEMS.registerItem("primitive_item_output_hatch", properties ->
                    new BlockItem(BlockRegistry.PRIMITIVE_ITEM_OUTPUT_HATCH.get(), properties));

    /** 村庄物品输出接口方块物品 */
    public static final DeferredItem<BlockItem> VILLAGE_ITEM_OUTPUT_HATCH =
            ITEMS.registerItem("village_item_output_hatch", properties ->
                    new BlockItem(BlockRegistry.VILLAGE_ITEM_OUTPUT_HATCH.get(), properties));

    /** 原始流体输入接口方块物品 */
    public static final DeferredItem<BlockItem> PRIMITIVE_FLUID_INPUT_HATCH =
            ITEMS.registerItem("primitive_fluid_input_hatch", properties ->
                    new BlockItem(BlockRegistry.PRIMITIVE_FLUID_INPUT_HATCH.get(), properties));

    /** 村庄流体输入接口方块物品 */
    public static final DeferredItem<BlockItem> VILLAGE_FLUID_INPUT_HATCH =
            ITEMS.registerItem("village_fluid_input_hatch", properties ->
                    new BlockItem(BlockRegistry.VILLAGE_FLUID_INPUT_HATCH.get(), properties));

    /** 原始流体输出接口方块物品 */
    public static final DeferredItem<BlockItem> PRIMITIVE_FLUID_OUTPUT_HATCH =
            ITEMS.registerItem("primitive_fluid_output_hatch", properties ->
                    new BlockItem(BlockRegistry.PRIMITIVE_FLUID_OUTPUT_HATCH.get(), properties));

    /** 村庄流体输出接口方块物品 */
    public static final DeferredItem<BlockItem> VILLAGE_FLUID_OUTPUT_HATCH =
            ITEMS.registerItem("village_fluid_output_hatch", properties ->
                    new BlockItem(BlockRegistry.VILLAGE_FLUID_OUTPUT_HATCH.get(), properties));

    /** 村庄采石场方块物品（Tier 1：村庄时代） */
    public static final DeferredItem<BlockItem> VILLAGE_QUARRY =
            ITEMS.registerItem("village_quarry", properties ->
                    new TieredBlockItem(BlockRegistry.VILLAGE_QUARRY.get(), properties));

    /** 村庄营地方块物品（Tier 1：村庄时代） */
    public static final DeferredItem<BlockItem> VILLAGE_CAMP_BLOCK_ITEM =
            ITEMS.registerItem("village_camp", properties ->
                    new TieredBlockItem(BlockRegistry.VILLAGE_CAMP_BLOCK.get(), properties));

    /** 村庄狩猎场方块物品（Tier 1：村庄时代） */
    public static final DeferredItem<BlockItem> VILLAGE_HUNTING_GROUND =
            ITEMS.registerItem("village_hunting_ground", properties ->
                    new TieredBlockItem(BlockRegistry.VILLAGE_HUNTING_GROUND.get(), properties));

    /** 村庄牧地方块物品（Tier 1：村庄时代） */
    public static final DeferredItem<BlockItem> VILLAGE_RANCH =
            ITEMS.registerItem("village_ranch", properties ->
                    new TieredBlockItem(BlockRegistry.VILLAGE_RANCH_BLOCK.get(), properties));

    /** 村庄农场方块物品（Tier 1：村庄时代） */
    public static final DeferredItem<BlockItem> VILLAGE_FARM =
            ITEMS.registerItem("village_farm", properties ->
                    new TieredBlockItem(BlockRegistry.VILLAGE_FARM_BLOCK.get(), properties));

    /** 村庄诊所方块物品（Tier 1：村庄时代） */
    public static final DeferredItem<BlockItem> VILLAGE_DOCTOR_CABIN =
            ITEMS.registerItem("village_doctor_cabin", properties ->
                    new TieredBlockItem(BlockRegistry.VILLAGE_DOCTOR_CABIN.get(), properties));

    public static final DeferredItem<BlockItem> VILLAGE_HARVESTER =
            ITEMS.registerItem("village_harvester", properties ->
                    new TieredBlockItem(BlockRegistry.VILLAGE_HARVESTER.get(), properties));

    public static final DeferredItem<BlockItem> PRIMITIVE_STORAGE_PIT =
            ITEMS.registerItem("primitive_storage_pit", properties ->
                    new TieredBlockItem(BlockRegistry.PRIMITIVE_STORAGE_PIT_BLOCK.get(), properties));

    /**
     * 向事件总线注册所有物品。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}

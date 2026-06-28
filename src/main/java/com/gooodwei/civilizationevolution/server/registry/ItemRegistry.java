package com.gooodwei.civilizationevolution.server.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.block.PrimitiveRanch;
import com.gooodwei.civilizationevolution.server.item.CivilizationCoreItem;
import com.gooodwei.civilizationevolution.server.item.ConnectorItem;
import com.gooodwei.civilizationevolution.server.item.PopulationItem;
import com.gooodwei.civilizationevolution.server.item.Recruiter;
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

    /** 营地方块物品 */
    public static final DeferredItem<BlockItem> CAMP_BLOCK_ITEM =
            ITEMS.registerSimpleBlockItem("camp", BlockRegistry.CAMP_BLOCK);

    /** 狩猎场方块物品 */
    public static final DeferredItem<BlockItem> HUNTING_GROUND =
            ITEMS.registerSimpleBlockItem("hunting_ground", BlockRegistry.HUNTING_GROUND);

    /** 原始聚落方块物品 */
    public static final DeferredItem<BlockItem> PRIMITIVE_SETTLEMENT =
            ITEMS.registerSimpleBlockItem("primitive_settlement", BlockRegistry.PRIMITIVE_SETTLEMENT);

    /** 文明核心 —— 控制器的数据存储介质，携带 UUID，最大堆叠 1 */
    public static final DeferredItem<CivilizationCoreItem> CIVILIZATION_CORE =
            ITEMS.registerItem("civilization_core", CivilizationCoreItem::new);

    /** 连接器 —— 将人口机器与文明核心绑定/解绑/切换，最大堆叠 1 */
    public static final DeferredItem<ConnectorItem> CONNECTOR =
            ITEMS.registerItem("connector", properties -> new ConnectorItem(properties.stacksTo(1)));

    public static final DeferredItem<BlockItem> PRIMITIVE_RANCH =
            ITEMS.registerSimpleBlockItem("primitive_ranch", BlockRegistry.PRIMITIVE_RANCH);

    /**
     * 向事件总线注册所有物品。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}

package com.gooodwei.civilizationevolution.client.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.registry.ItemRegistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 创造模式物品栏注册。
 *
 * <p>注册一个独立的"文明演进"标签页（位于战斗标签之前），
 * 并将招募者和连接器添加到原版"工具与实用物品"标签页中。
 */
@EventBusSubscriber(modid = CivilizationEvolution.MODID)
public class CreativeTabRegistry {

    /** 创造模式标签页注册器 */
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CivilizationEvolution.MODID);

    /** 主标签页：包含模组所有物品和方块 */
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.civilizationevolution"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> ItemRegistry.PRIMITIVE_CONTROLLER.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(ItemRegistry.POPULATION.get());
                        output.accept(ItemRegistry.RECRUITER.get());
                        output.accept(ItemRegistry.CONNECTOR.get());
                        output.accept(ItemRegistry.DEBUG_STRUCTURE_GETTER.get());
                        output.accept(ItemRegistry.PROJECTOR.get());
                        output.accept(ItemRegistry.PRIMITIVE_CAMP_BLOCK_ITEM.get());
                        output.accept(ItemRegistry.PRIMITIVE_HUNTING_GROUND.get());
                        output.accept(ItemRegistry.PRIMITIVE_CONTROLLER.get());
                        output.accept(ItemRegistry.CIVILIZATION_CORE.get());
                        output.accept(ItemRegistry.CIVILIZATION_CORE_EXTRACTOR.get());
                        output.accept(ItemRegistry.PRIMITIVE_RANCH.get());
                        output.accept(ItemRegistry.VILLAGE_CONTROLLER.get());
                        output.accept(ItemRegistry.PRIMITIVE_FARM.get());
                        output.accept(ItemRegistry.PRIMITIVE_STRUCTURE_CASING.get());
                        output.accept(ItemRegistry.PRIMITIVE_POPULATION_INPUT_HATCH.get());
                        output.accept(ItemRegistry.PRIMITIVE_FOOD_INPUT_HATCH.get());
                        output.accept(ItemRegistry.VILLAGE_FOOD_INPUT_HATCH.get());
                        output.accept(ItemRegistry.PRIMITIVE_POPULATION_OUTPUT_HATCH.get());
                        output.accept(ItemRegistry.PRIMITIVE_DOCTOR_CABIN.get());
                        output.accept(ItemRegistry.PRIMITIVE_ITEM_INPUT_HATCH.get());
                        output.accept(ItemRegistry.VILLAGE_ITEM_INPUT_HATCH.get());
                        output.accept(ItemRegistry.PRIMITIVE_ITEM_OUTPUT_HATCH.get());
                        output.accept(ItemRegistry.VILLAGE_ITEM_OUTPUT_HATCH.get());
                        output.accept(ItemRegistry.PRIMITIVE_FLUID_INPUT_HATCH.get());
                        output.accept(ItemRegistry.VILLAGE_FLUID_INPUT_HATCH.get());
                        output.accept(ItemRegistry.PRIMITIVE_FLUID_OUTPUT_HATCH.get());
                        output.accept(ItemRegistry.VILLAGE_FLUID_OUTPUT_HATCH.get());
                        output.accept(ItemRegistry.VILLAGE_STRUCTURE_CASING.get());
                        output.accept(ItemRegistry.VILLAGE_QUARRY.get());
                        output.accept(ItemRegistry.VILLAGE_CAMP_BLOCK_ITEM.get());
                        output.accept(ItemRegistry.VILLAGE_HUNTING_GROUND.get());
                        output.accept(ItemRegistry.VILLAGE_RANCH.get());
                        output.accept(ItemRegistry.VILLAGE_FARM.get());
                        output.accept(ItemRegistry.VILLAGE_DOCTOR_CABIN.get());
                        output.accept(ItemRegistry.VILLAGE_HARVESTER.get());
                        output.accept(ItemRegistry.PRIMITIVE_STORAGE_PIT.get());
                        output.accept(ItemRegistry.WHEAT_FLOUR.get());

                    }).build());

    /**
     * 向事件总线注册创造标签页。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        CREATIVE_MODE_TABS.register(bus);
    }

    /** 向原版"工具与实用物品"标签页追加招募者和连接器 */
    @SubscribeEvent
    static void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ItemRegistry.RECRUITER);
            event.accept(ItemRegistry.CONNECTOR);
            event.accept(ItemRegistry.DEBUG_STRUCTURE_GETTER);
            event.accept(ItemRegistry.PROJECTOR);
        }
    }
}

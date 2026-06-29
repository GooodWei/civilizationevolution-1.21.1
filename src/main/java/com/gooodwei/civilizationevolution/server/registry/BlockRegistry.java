package com.gooodwei.civilizationevolution.server.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 方块注册表。通过 NeoForge {@link DeferredRegister} 注册所有自定义方块。
 */
public class BlockRegistry {

    /** 方块注册器 */
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CivilizationEvolution.MODID);

    /** 原始营地 —— Tier 0 生产单位，支持 2 个人口槽位进行繁殖 */
    public static final DeferredBlock<PrimitiveCampBlock> PRIMITIVE_CAMP_BLOCK = BLOCKS.register(
            "primitive_camp",
            () -> new PrimitiveCampBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 原始狩猎场 —— 在区块范围内自动猎杀动物、收集战利品 */
    public static final DeferredBlock<PrimitiveHuntingGround> PRIMITIVE_HUNTING_GROUND = BLOCKS.register(
            "primitive_hunting_ground",
            () -> new PrimitiveHuntingGround(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 原始控制器 —— 文明控制器，可绑定最多 10 台机器进行统一调度 */
    public static final DeferredBlock<PrimitiveController> PRIMITIVE_CONTROLLER = BLOCKS.register(
            "primitive_controller",
            () -> new PrimitiveController(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 原始牧场 —— Tier 0 喂养范围内动物的机器方块 */
    public static final DeferredBlock<PrimitiveRanchBlock> PRIMITIVE_RANCH_BLOCK = BLOCKS.register(
            "primitive_ranch",
            () -> new PrimitiveRanchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 村庄控制器 —— Tier 1 文明控制器 */
    public static final DeferredBlock<VillageController> VILLAGE_CONTROLLER = BLOCKS.register(
            "village_controller",
            () -> new VillageController(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 原始农场 —— Tier 0 自动催熟范围内作物的机器方块，内置储水罐 */
    public static final DeferredBlock<PrimitiveFarmBlock> PRIMITIVE_FARM_BLOCK = BLOCKS.register(
            "primitive_farm",
            () -> new PrimitiveFarmBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 原始结构方块 —— 多方块机器填充外壳（Tier 0） */
    public static final DeferredBlock<PrimitiveStructureCasing> PRIMITIVE_STRUCTURE_CASING = BLOCKS.register(
            "primitive_structure_casing",
            () -> new PrimitiveStructureCasing(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f))
    );

    /** 原始人口输入接口 —— 多方块机器人口输入口（Tier 0） */
    public static final DeferredBlock<PrimitivePopulationInputHatchBlock> PRIMITIVE_POPULATION_INPUT_HATCH = BLOCKS.register(
            "primitive_population_input_hatch",
            () -> new PrimitivePopulationInputHatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 原始食物输入接口 —— 多方块机器食物输入口（Tier 0） */
    public static final DeferredBlock<PrimitiveFoodInputHatchBlock> PRIMITIVE_FOOD_INPUT_HATCH = BLOCKS.register(
            "primitive_food_input_hatch",
            () -> new PrimitiveFoodInputHatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 原始人口输出接口 —— 多方块机器人口输出口（Tier 0） */
    public static final DeferredBlock<PrimitivePopulationOutputHatchBlock> PRIMITIVE_POPULATION_OUTPUT_HATCH = BLOCKS.register(
            "primitive_population_output_hatch",
            () -> new PrimitivePopulationOutputHatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 原始诊所 —— 多方块医院控制器（Tier 0） */
    public static final DeferredBlock<PrimitiveDoctorCabin> PRIMITIVE_DOCTOR_CABIN = BLOCKS.register(
            "primitive_doctor_cabin",
            () -> new PrimitiveDoctorCabin(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /**
     * 向事件总线注册所有方块。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}

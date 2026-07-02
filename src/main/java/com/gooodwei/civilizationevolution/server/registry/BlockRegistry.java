package com.gooodwei.civilizationevolution.server.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.block.controller.PrimitiveController;
import com.gooodwei.civilizationevolution.server.block.controller.VillageController;
import com.gooodwei.civilizationevolution.server.block.hatch.*;
import com.gooodwei.civilizationevolution.server.block.machine.*;
import com.gooodwei.civilizationevolution.server.block.part.MiningShaftPipe;
import com.gooodwei.civilizationevolution.server.block.part.PrimitiveStructureCasing;
import com.gooodwei.civilizationevolution.server.block.part.VillageStructureCasing;
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

    /** 村庄食物输入接口 —— 多方块机器食物输入口（Tier 1） */
    public static final DeferredBlock<VillageFoodInputHatchBlock> VILLAGE_FOOD_INPUT_HATCH = BLOCKS.register(
            "village_food_input_hatch",
            () -> new VillageFoodInputHatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
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

    /** 原始物品输入接口 —— 多方块机器物品输入口（Tier 0） */
    public static final DeferredBlock<PrimitiveItemInputHatchBlock> PRIMITIVE_ITEM_INPUT_HATCH = BLOCKS.register(
            "primitive_item_input_hatch",
            () -> new PrimitiveItemInputHatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 村庄物品输入接口 —— 多方块机器物品输入口（Tier 1） */
    public static final DeferredBlock<VillageItemInputHatchBlock> VILLAGE_ITEM_INPUT_HATCH = BLOCKS.register(
            "village_item_input_hatch",
            () -> new VillageItemInputHatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 原始物品输出接口 —— 多方块机器物品输出口（Tier 0） */
    public static final DeferredBlock<PrimitiveItemOutputHatchBlock> PRIMITIVE_ITEM_OUTPUT_HATCH = BLOCKS.register(
            "primitive_item_output_hatch",
            () -> new PrimitiveItemOutputHatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 村庄物品输出接口 —— 多方块机器物品输出口（Tier 1） */
    public static final DeferredBlock<VillageItemOutputHatchBlock> VILLAGE_ITEM_OUTPUT_HATCH = BLOCKS.register(
            "village_item_output_hatch",
            () -> new VillageItemOutputHatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 原始流体输入接口 —— 多方块机器流体输入口（Tier 0） */
    public static final DeferredBlock<PrimitiveFluidInputHatchBlock> PRIMITIVE_FLUID_INPUT_HATCH = BLOCKS.register(
            "primitive_fluid_input_hatch",
            () -> new PrimitiveFluidInputHatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 村庄流体输入接口 —— 多方块机器流体输入口（Tier 1） */
    public static final DeferredBlock<VillageFluidInputHatchBlock> VILLAGE_FLUID_INPUT_HATCH = BLOCKS.register(
            "village_fluid_input_hatch",
            () -> new VillageFluidInputHatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 原始流体输出接口 —— 多方块机器流体输出口（Tier 0） */
    public static final DeferredBlock<PrimitiveFluidOutputHatchBlock> PRIMITIVE_FLUID_OUTPUT_HATCH = BLOCKS.register(
            "primitive_fluid_output_hatch",
            () -> new PrimitiveFluidOutputHatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 村庄流体输出接口 —— 多方块机器流体输出口（Tier 1） */
    public static final DeferredBlock<VillageFluidOutputHatchBlock> VILLAGE_FLUID_OUTPUT_HATCH = BLOCKS.register(
            "village_fluid_output_hatch",
            () -> new VillageFluidOutputHatchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 村庄结构方块 —— 多方块机器填充外壳（Tier 1） */
    public static final DeferredBlock<VillageStructureCasing> VILLAGE_STRUCTURE_CASING = BLOCKS.register(
            "village_structure_casing",
            () -> new VillageStructureCasing(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f))
    );

    /** 矿井管道 —— 村庄采石场结构方块，黑曜石硬度，无掉落物（Tier 1） */
    public static final DeferredBlock<MiningShaftPipe> MINING_SHAFT_PIPE = BLOCKS.register(
            "mining_shaft_pipe",
            () -> new MiningShaftPipe(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .strength(50.0f, 1200.0f)
                    .requiresCorrectToolForDrops())
    );

    /** 村庄采石场 —— Tier 1 采矿机器 */
    public static final DeferredBlock<VillageQuarryBlock> VILLAGE_QUARRY = BLOCKS.register(
            "village_quarry",
            () -> new VillageQuarryBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 村庄营地 —— Tier 1 生产单位，效率为原始版本两倍 */
    public static final DeferredBlock<VillageCampBlock> VILLAGE_CAMP_BLOCK = BLOCKS.register(
            "village_camp",
            () -> new VillageCampBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 村庄狩猎场 —— Tier 1 区块内自动猎杀动物、收集战利品 */
    public static final DeferredBlock<VillageHuntingGround> VILLAGE_HUNTING_GROUND = BLOCKS.register(
            "village_hunting_ground",
            () -> new VillageHuntingGround(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 村庄牧场 —— Tier 1 喂养范围内动物的机器方块 */
    public static final DeferredBlock<VillageRanchBlock> VILLAGE_RANCH_BLOCK = BLOCKS.register(
            "village_ranch",
            () -> new VillageRanchBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 村庄农场 —— Tier 1 自动催熟范围内作物的机器方块，内置储水罐 */
    public static final DeferredBlock<VillageFarmBlock> VILLAGE_FARM_BLOCK = BLOCKS.register(
            "village_farm",
            () -> new VillageFarmBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 村庄诊所 —— Tier 1 多方块医院控制器 */
    public static final DeferredBlock<VillageDoctorCabin> VILLAGE_DOCTOR_CABIN = BLOCKS.register(
            "village_doctor_cabin",
            () -> new VillageDoctorCabin(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    public static final DeferredBlock<VillageHarvester> VILLAGE_HARVESTER = BLOCKS.register(
            "village_harvester",
            () -> new VillageHarvester(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /**
     * 向事件总线注册所有方块。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}

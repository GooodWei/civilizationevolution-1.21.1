package com.gooodwei.civilizationevolution.server.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.blockentity.controller.PrimitiveControllerBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.controller.VillageControllerBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.*;
import com.gooodwei.civilizationevolution.server.blockentity.machine.*;
import com.gooodwei.civilizationevolution.server.blockentity.multiblock.PrimitiveDoctorCabinBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.multiblock.VillageDoctorCabinBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.multiblock.VillageQuarryBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * BlockEntity 类型注册表。
 * 每个 BlockEntity 类型关联其对应的方块，Minecraft 据此创建 BE 实例。
 */
public class BlockEntityRegistry {

    /** BlockEntity 类型注册器 */
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CivilizationEvolution.MODID);

    /** 原始营地 BE 类型（Tier 0） */
    public static final Supplier<BlockEntityType<PrimitiveCampBlockEntity>> PRIMITIVE_CAMP_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("primitive_camp",
                    () -> BlockEntityType.Builder.of(PrimitiveCampBlockEntity::new,
                            BlockRegistry.PRIMITIVE_CAMP_BLOCK.get()).build(null));

    /** 原始狩猎场 BE 类型（Tier 0） */
    public static final Supplier<BlockEntityType<PrimitiveHuntingGroundBlockEntity>> PRIMITIVE_HUNTING_GROUND =
            BLOCK_ENTITIES.register("primitive_hunting_ground",
                    () -> BlockEntityType.Builder.of(PrimitiveHuntingGroundBlockEntity::new,
                            BlockRegistry.PRIMITIVE_HUNTING_GROUND.get()).build(null));

    /** 原始控制器 BE 类型 */
    public static final Supplier<BlockEntityType<PrimitiveControllerBlockEntity>> PRIMITIVE_CONTROLLER =
            BLOCK_ENTITIES.register("primitive_controller",
                    () -> BlockEntityType.Builder.of(PrimitiveControllerBlockEntity::new,
                            BlockRegistry.PRIMITIVE_CONTROLLER.get()).build(null));

    /** 原始牧场 BE 类型（Tier 0） */
    public static final Supplier<BlockEntityType<PrimitiveRanchBlockEntity>> PRIMITIVE_RANCH =
            BLOCK_ENTITIES.register("primitive_ranch",
                    () -> BlockEntityType.Builder.of(PrimitiveRanchBlockEntity::new,
                            BlockRegistry.PRIMITIVE_RANCH_BLOCK.get()).build(null));

    /** 村庄控制器 BE 类型（Tier 1） */
    public static final Supplier<BlockEntityType<VillageControllerBlockEntity>> VILLAGE_CONTROLLER =
            BLOCK_ENTITIES.register("village_controller",
                    () -> BlockEntityType.Builder.of(VillageControllerBlockEntity::new,
                            BlockRegistry.VILLAGE_CONTROLLER.get()).build(null));

    /** 原始农场 BE 类型（Tier 0） */
    public static final Supplier<BlockEntityType<PrimitiveFarmBlockEntity>> PRIMITIVE_FARM =
            BLOCK_ENTITIES.register("primitive_farm",
                    () -> BlockEntityType.Builder.of(PrimitiveFarmBlockEntity::new,
                            BlockRegistry.PRIMITIVE_FARM_BLOCK.get()).build(null));

    /** 原始人口输入接口 BE 类型（Tier 0） */
    public static final Supplier<BlockEntityType<PrimitivePopulationInputHatchBlockEntity>> PRIMITIVE_POPULATION_INPUT_HATCH =
            BLOCK_ENTITIES.register("primitive_population_input_hatch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new PrimitivePopulationInputHatchBlockEntity(
                                    BlockEntityRegistry.PRIMITIVE_POPULATION_INPUT_HATCH.get(), pos, state),
                            BlockRegistry.PRIMITIVE_POPULATION_INPUT_HATCH.get()).build(null));

    /** 原始食物输入接口 BE 类型（Tier 0） */
    public static final Supplier<BlockEntityType<PrimitiveFoodInputHatchBlockEntity>> PRIMITIVE_FOOD_INPUT_HATCH =
            BLOCK_ENTITIES.register("primitive_food_input_hatch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new PrimitiveFoodInputHatchBlockEntity(
                                    BlockEntityRegistry.PRIMITIVE_FOOD_INPUT_HATCH.get(), pos, state),
                            BlockRegistry.PRIMITIVE_FOOD_INPUT_HATCH.get()).build(null));

    /** 村庄食物输入接口 BE 类型（Tier 1） */
    public static final Supplier<BlockEntityType<VillageFoodInputHatchBlockEntity>> VILLAGE_FOOD_INPUT_HATCH =
            BLOCK_ENTITIES.register("village_food_input_hatch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new VillageFoodInputHatchBlockEntity(
                                    BlockEntityRegistry.VILLAGE_FOOD_INPUT_HATCH.get(), pos, state),
                            BlockRegistry.VILLAGE_FOOD_INPUT_HATCH.get()).build(null));

    /** 原始人口输出接口 BE 类型（Tier 0） */
    public static final Supplier<BlockEntityType<PrimitivePopulationOutputHatchBlockEntity>> PRIMITIVE_POPULATION_OUTPUT_HATCH =
            BLOCK_ENTITIES.register("primitive_population_output_hatch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new PrimitivePopulationOutputHatchBlockEntity(
                                    BlockEntityRegistry.PRIMITIVE_POPULATION_OUTPUT_HATCH.get(), pos, state),
                            BlockRegistry.PRIMITIVE_POPULATION_OUTPUT_HATCH.get()).build(null));

    /** 原始诊所 BE 类型（Tier 0） */
    public static final Supplier<BlockEntityType<PrimitiveDoctorCabinBlockEntity>> PRIMITIVE_DOCTOR_CABIN =
            BLOCK_ENTITIES.register("primitive_doctor_cabin",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new PrimitiveDoctorCabinBlockEntity(
                                    BlockEntityRegistry.PRIMITIVE_DOCTOR_CABIN.get(), pos, state),
                            BlockRegistry.PRIMITIVE_DOCTOR_CABIN.get()).build(null));

    /** 原始物品输入接口 BE 类型（Tier 0） */
    public static final Supplier<BlockEntityType<PrimitiveItemInputHatchBlockEntity>> PRIMITIVE_ITEM_INPUT_HATCH =
            BLOCK_ENTITIES.register("primitive_item_input_hatch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new PrimitiveItemInputHatchBlockEntity(
                                    BlockEntityRegistry.PRIMITIVE_ITEM_INPUT_HATCH.get(), pos, state),
                            BlockRegistry.PRIMITIVE_ITEM_INPUT_HATCH.get()).build(null));

    /** 村庄物品输入接口 BE 类型（Tier 1） */
    public static final Supplier<BlockEntityType<VillageItemInputHatchBlockEntity>> VILLAGE_ITEM_INPUT_HATCH =
            BLOCK_ENTITIES.register("village_item_input_hatch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new VillageItemInputHatchBlockEntity(
                                    BlockEntityRegistry.VILLAGE_ITEM_INPUT_HATCH.get(), pos, state),
                            BlockRegistry.VILLAGE_ITEM_INPUT_HATCH.get()).build(null));

    /** 原始物品输出接口 BE 类型（Tier 0） */
    public static final Supplier<BlockEntityType<PrimitiveItemOutputHatchBlockEntity>> PRIMITIVE_ITEM_OUTPUT_HATCH =
            BLOCK_ENTITIES.register("primitive_item_output_hatch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new PrimitiveItemOutputHatchBlockEntity(
                                    BlockEntityRegistry.PRIMITIVE_ITEM_OUTPUT_HATCH.get(), pos, state),
                            BlockRegistry.PRIMITIVE_ITEM_OUTPUT_HATCH.get()).build(null));

    /** 村庄物品输出接口 BE 类型（Tier 1） */
    public static final Supplier<BlockEntityType<VillageItemOutputHatchBlockEntity>> VILLAGE_ITEM_OUTPUT_HATCH =
            BLOCK_ENTITIES.register("village_item_output_hatch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new VillageItemOutputHatchBlockEntity(
                                    BlockEntityRegistry.VILLAGE_ITEM_OUTPUT_HATCH.get(), pos, state),
                            BlockRegistry.VILLAGE_ITEM_OUTPUT_HATCH.get()).build(null));

    /** 原始流体输入接口 BE 类型（Tier 0） */
    public static final Supplier<BlockEntityType<PrimitiveFluidInputHatchBlockEntity>> PRIMITIVE_FLUID_INPUT_HATCH =
            BLOCK_ENTITIES.register("primitive_fluid_input_hatch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new PrimitiveFluidInputHatchBlockEntity(
                                    BlockEntityRegistry.PRIMITIVE_FLUID_INPUT_HATCH.get(), pos, state),
                            BlockRegistry.PRIMITIVE_FLUID_INPUT_HATCH.get()).build(null));

    /** 村庄流体输入接口 BE 类型（Tier 1） */
    public static final Supplier<BlockEntityType<VillageFluidInputHatchBlockEntity>> VILLAGE_FLUID_INPUT_HATCH =
            BLOCK_ENTITIES.register("village_fluid_input_hatch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new VillageFluidInputHatchBlockEntity(
                                    BlockEntityRegistry.VILLAGE_FLUID_INPUT_HATCH.get(), pos, state),
                            BlockRegistry.VILLAGE_FLUID_INPUT_HATCH.get()).build(null));

    /** 原始流体输出接口 BE 类型（Tier 0） */
    public static final Supplier<BlockEntityType<PrimitiveFluidOutputHatchBlockEntity>> PRIMITIVE_FLUID_OUTPUT_HATCH =
            BLOCK_ENTITIES.register("primitive_fluid_output_hatch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new PrimitiveFluidOutputHatchBlockEntity(
                                    BlockEntityRegistry.PRIMITIVE_FLUID_OUTPUT_HATCH.get(), pos, state),
                            BlockRegistry.PRIMITIVE_FLUID_OUTPUT_HATCH.get()).build(null));

    /** 村庄流体输出接口 BE 类型（Tier 1） */
    public static final Supplier<BlockEntityType<VillageFluidOutputHatchBlockEntity>> VILLAGE_FLUID_OUTPUT_HATCH =
            BLOCK_ENTITIES.register("village_fluid_output_hatch",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new VillageFluidOutputHatchBlockEntity(
                                    BlockEntityRegistry.VILLAGE_FLUID_OUTPUT_HATCH.get(), pos, state),
                            BlockRegistry.VILLAGE_FLUID_OUTPUT_HATCH.get()).build(null));

    /** 村庄采石场 BE 类型（Tier 1） */
    public static final Supplier<BlockEntityType<VillageQuarryBlockEntity>> VILLAGE_QUARRY =
            BLOCK_ENTITIES.register("village_quarry",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new VillageQuarryBlockEntity(
                                    BlockEntityRegistry.VILLAGE_QUARRY.get(), pos, state),
                            BlockRegistry.VILLAGE_QUARRY.get()).build(null));

    /** 村庄营地 BE 类型（Tier 1） */
    public static final Supplier<BlockEntityType<VillageCampBlockEntity>> VILLAGE_CAMP =
            BLOCK_ENTITIES.register("village_camp",
                    () -> BlockEntityType.Builder.of(VillageCampBlockEntity::new,
                            BlockRegistry.VILLAGE_CAMP_BLOCK.get()).build(null));

    /** 村庄狩猎场 BE 类型（Tier 1） */
    public static final Supplier<BlockEntityType<VillageHuntingGroundBlockEntity>> VILLAGE_HUNTING_GROUND =
            BLOCK_ENTITIES.register("village_hunting_ground",
                    () -> BlockEntityType.Builder.of(VillageHuntingGroundBlockEntity::new,
                            BlockRegistry.VILLAGE_HUNTING_GROUND.get()).build(null));

    /** 村庄牧场 BE 类型（Tier 1） */
    public static final Supplier<BlockEntityType<VillageRanchBlockEntity>> VILLAGE_RANCH =
            BLOCK_ENTITIES.register("village_ranch",
                    () -> BlockEntityType.Builder.of(VillageRanchBlockEntity::new,
                            BlockRegistry.VILLAGE_RANCH_BLOCK.get()).build(null));

    /** 村庄农场 BE 类型（Tier 1） */
    public static final Supplier<BlockEntityType<VillageFarmBlockEntity>> VILLAGE_FARM =
            BLOCK_ENTITIES.register("village_farm",
                    () -> BlockEntityType.Builder.of(VillageFarmBlockEntity::new,
                            BlockRegistry.VILLAGE_FARM_BLOCK.get()).build(null));

    /** 村庄诊所 BE 类型（Tier 1） */
    public static final Supplier<BlockEntityType<VillageDoctorCabinBlockEntity>> VILLAGE_DOCTOR_CABIN =
            BLOCK_ENTITIES.register("village_doctor_cabin",
                    () -> BlockEntityType.Builder.of(
                            (pos, state) -> new VillageDoctorCabinBlockEntity(
                                    BlockEntityRegistry.VILLAGE_DOCTOR_CABIN.get(), pos, state),
                            BlockRegistry.VILLAGE_DOCTOR_CABIN.get()).build(null));

    public static final Supplier<BlockEntityType<VillageHarvesterBlockEntity>> VILLAGE_HARVESTER =
            BLOCK_ENTITIES.register("village_harvester",
                    () -> BlockEntityType.Builder.of(VillageHarvesterBlockEntity::new,
                            BlockRegistry.VILLAGE_HARVESTER.get()).build(null));
    /**
     * 向事件总线注册所有 BlockEntity 类型。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}

package com.gooodwei.civilizationevolution.server.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.blockentity.fieldmachine.PrimitiveFarmBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.fieldmachine.PrimitiveHuntingGroundBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.fieldmachine.PrimitiveRanchBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.machine.PrimitiveCampBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.controllermachine.PrimitiveControllerBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.controllermachine.VillageControllerBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
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

    /**
     * 向事件总线注册所有 BlockEntity 类型。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}

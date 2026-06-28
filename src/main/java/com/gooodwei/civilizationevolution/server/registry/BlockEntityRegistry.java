package com.gooodwei.civilizationevolution.server.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.blockentity.fieldmachine.HuntingGroundBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.fieldmachine.PrimitiveRanchBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.machine.CampBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.machine.PrimitiveSettlementBlockEntity;
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

    /** 营地 BE 类型 */
    public static final Supplier<BlockEntityType<CampBlockEntity>> CAMP_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("camp",
                    () -> BlockEntityType.Builder.of(CampBlockEntity::new,
                            BlockRegistry.CAMP_BLOCK.get()).build(null));

    /** 狩猎场 BE 类型 */
    public static final Supplier<BlockEntityType<HuntingGroundBlockEntity>> HUNT_GROUND =
            BLOCK_ENTITIES.register("hunting_ground",
                    () -> BlockEntityType.Builder.of(HuntingGroundBlockEntity::new,
                            BlockRegistry.HUNTING_GROUND.get()).build(null));

    /** 原始聚落 BE 类型 */
    public static final Supplier<BlockEntityType<PrimitiveSettlementBlockEntity>> PRIMITIVE_SETTLEMENT =
            BLOCK_ENTITIES.register("primitive_settlement",
                    () -> BlockEntityType.Builder.of(PrimitiveSettlementBlockEntity::new,
                            BlockRegistry.PRIMITIVE_SETTLEMENT.get()).build(null));

    public static final Supplier<BlockEntityType<PrimitiveRanchBlockEntity>> PRIMITIVE_RANCH =
            BLOCK_ENTITIES.register("primitive_ranch",
                    () -> BlockEntityType.Builder.of(PrimitiveRanchBlockEntity::new,
                            BlockRegistry.PRIMITIVE_RANCH.get()).build(null));

    /**
     * 向事件总线注册所有 BlockEntity 类型。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}

package com.gooodwei.civilizationevolution.server.registry;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.block.CampBlock;
import com.gooodwei.civilizationevolution.server.block.HuntingGround;
import com.gooodwei.civilizationevolution.server.block.PrimitiveRanch;
import com.gooodwei.civilizationevolution.server.block.PrimitiveSettlement;
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

    /** 营地 —— 原始社会生产单位，支持 2 个人口槽位进行繁殖 */
    public static final DeferredBlock<CampBlock> CAMP_BLOCK = BLOCKS.register(
            "camp",
            () -> new CampBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 狩猎场 —— 在区块范围内自动猎杀动物、收集战利品 */
    public static final DeferredBlock<HuntingGround> HUNTING_GROUND = BLOCKS.register(
            "hunting_ground",
            () -> new HuntingGround(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /** 原始聚落 —— 文明控制器，可绑定最多 10 台机器进行统一调度 */
    public static final DeferredBlock<PrimitiveSettlement> PRIMITIVE_SETTLEMENT = BLOCKS.register(
            "primitive_settlement",
            () -> new PrimitiveSettlement(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    public static final DeferredBlock<PrimitiveRanch> PRIMITIVE_RANCH = BLOCKS.register(
            "primitive_ranch",
            () -> new PrimitiveRanch(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0f).noOcclusion())
    );

    /**
     * 向事件总线注册所有方块。
     * @param bus 模组事件总线
     */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
    }
}

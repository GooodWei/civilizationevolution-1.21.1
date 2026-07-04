package com.gooodwei.civilizationevolution.server.block.machine;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.multiblock.VillageQuarryBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 村庄采石场方块 —— Tier 1 采矿机器。
 *
 * <p>暂为单方块形式（后续改为多方块控制器），用镐子挖掘矿石获取资源。
 * GUI 复用狩猎场布局，武器槽改为镐槽。
 */
public class VillageQuarryBlock extends AbstractMachineBlock {

    public static final MapCodec<VillageQuarryBlock> CODEC =
            simpleCodec(VillageQuarryBlock::new);

    public VillageQuarryBlock(Properties properties) {
        super(properties);
    }

    // ==================== BlockEntity ====================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VillageQuarryBlockEntity(
                BlockEntityRegistry.VILLAGE_QUARRY.get(), pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return level.isClientSide() ? null :
                createTickerHelper(type, BlockEntityRegistry.VILLAGE_QUARRY.get(),
                        VillageQuarryBlockEntity::serverTick);
    }

    // ==================== Codec & Tier ====================

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public Tier getTier() {
        return CivilizationTiers.VILLAGE;
    }
}

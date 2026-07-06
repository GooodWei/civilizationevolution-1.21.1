package com.gooodwei.civilizationevolution.server.block.machine;

import com.gooodwei.civilizationevolution.api.IPopulationMachine;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.multiblock.VillageMillBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.gooodwei.civilizationevolution.server.registry.TieredBlockItem;
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

public class VillageMillBlock extends AbstractMachineBlock{
    public static final MapCodec<VillageMillBlock> CODEC =
            simpleCodec(VillageMillBlock::new);

    public VillageMillBlock(Properties properties) {
        super(properties);
    }

    /**
     * 获取此方块的 Tier 等级。
     *
     * <p>供 {@link TieredBlockItem}
     * 在物品 tooltip 中显示时代名称，与 BE 的
     * {@link IPopulationMachine#getTier()} 保持一致。
     *
     * @return 此方块对应的 Tier 等级
     */
    @Override
    public Tier getTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VillageMillBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        return level.isClientSide() ? null :
                createTickerHelper(type, BlockEntityRegistry.VILLAGE_MILL.get(),
                        VillageMillBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * 控制器被破坏时，调用 BE 的结构破坏处理（弹出物品、释放零件认领）。
     */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                             BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VillageMillBlockEntity mill) {
                mill.onStructurePartBroken(pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

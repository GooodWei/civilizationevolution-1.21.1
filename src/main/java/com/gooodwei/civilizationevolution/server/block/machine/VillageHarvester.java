package com.gooodwei.civilizationevolution.server.block.machine;

import com.gooodwei.civilizationevolution.api.IPopulationMachine;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.machine.VillageHarvesterBlockEntity;
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

public class VillageHarvester extends AbstractMachineBlock{
    public static final MapCodec<VillageHarvester> CODEC = simpleCodec(VillageHarvester::new);

    public VillageHarvester(Properties properties) {
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

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return  RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new VillageHarvesterBlockEntity(blockPos, blockState);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,
                                                                  BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type,
                BlockEntityRegistry.VILLAGE_HARVESTER.get(),
                VillageHarvesterBlockEntity::serverTick);
    }
}

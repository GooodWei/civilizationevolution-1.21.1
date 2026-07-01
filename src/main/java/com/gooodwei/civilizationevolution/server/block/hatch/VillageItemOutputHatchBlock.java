package com.gooodwei.civilizationevolution.server.block.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.VillageItemOutputHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 村庄物品输出接口 —— Tier 1。
 *
 * <p>含 1 个槽位，不接受外部放入（仅代码产出）。
 */
public class VillageItemOutputHatchBlock extends AbstractItemOutputHatchBlock {

    public static final MapCodec<VillageItemOutputHatchBlock> CODEC =
            simpleCodec(VillageItemOutputHatchBlock::new);

    public VillageItemOutputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VillageItemOutputHatchBlockEntity(
                BlockEntityRegistry.VILLAGE_ITEM_OUTPUT_HATCH.get(), pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.VILLAGE;
    }
}

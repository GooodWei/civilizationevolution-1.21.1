package com.gooodwei.civilizationevolution.server.block.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.VillageFoodInputHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 村庄食物输入接口 —— Tier 1。
 *
 * <p>含 1 个槽位，仅接受食物物品。
 * 多方块控制器通过此接口消耗食物。
 */
public class VillageFoodInputHatchBlock extends AbstractFoodInputHatchBlock {

    public static final MapCodec<VillageFoodInputHatchBlock> CODEC =
            simpleCodec(VillageFoodInputHatchBlock::new);

    public VillageFoodInputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VillageFoodInputHatchBlockEntity(
                BlockEntityRegistry.VILLAGE_FOOD_INPUT_HATCH.get(), pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.VILLAGE;
    }
}

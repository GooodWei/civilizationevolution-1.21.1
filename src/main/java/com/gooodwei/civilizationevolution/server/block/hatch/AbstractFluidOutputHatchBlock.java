package com.gooodwei.civilizationevolution.server.block.hatch;

import com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractFluidHatchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 流体输出接口的抽象基类。
 *
 * <p>每个流体输出接口含内部流体储罐，接受代码产出流体。
 * 多方块控制器将产物流体推送到此接口供管道提取。
 * 无 GUI，纯通过 {@code IFluidHandler} 与物流管道交互。
 * 子类只需覆写 {@link #codec()}、{@link #newBlockEntity} 和 {@link #getPartTier()}。
 */
public abstract class AbstractFluidOutputHatchBlock extends AbstractHatchBlock {

    protected AbstractFluidOutputHatchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public String getPartType() {
        return TYPE_FLUID_OUTPUT_HATCH;
    }

    /** 流体接口不提供 GUI，纯通过 IFluidHandler 交互 */
    @Override
    @Nullable
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        return null;
    }

    /**
     * 右键交互：优先处理桶/流体容器操作，无物品时委托父类。
     */
    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state,
                                                        @NotNull Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof AbstractFluidHatchBlockEntity hatchBe) {
            ItemInteractionResult bucketResult = AbstractFluidHatchBlockEntity.handleBucketInteraction(
                    level, pos, player, hand, stack, hatchBe);
            if (bucketResult != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
                return bucketResult;
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}

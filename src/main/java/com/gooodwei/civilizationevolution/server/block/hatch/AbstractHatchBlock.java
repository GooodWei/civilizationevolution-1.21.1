package com.gooodwei.civilizationevolution.server.block.hatch;

import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * 所有 hatch（多方块接口）方块的抽象基类。
 *
 * <p>提取了 {@link IMultiBlockPart} 声明以及右键打开 GUI 的交互逻辑。
 * 结构验证仅由控制器在放置、破坏及定时检测时触发。
 *
 * <p>hatch 拥有独立的存储空间和 GUI，无论是否在多方块结构中都可以右键打开。
 * 三个子抽象类（输入/食物/输出接口）继承此类，只需覆写 {@link #codec()} 和
 * {@link #newBlockEntity(BlockPos, BlockState)}。
 */
public abstract class AbstractHatchBlock extends BaseEntityBlock implements IMultiBlockPart {

    protected AbstractHatchBlock(Properties properties) {
        super(properties);
    }

    // ==================== 抽象方法 ====================

    @Override
    protected abstract MapCodec<? extends BaseEntityBlock> codec();

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    // ==================== 右键交互 ====================

    /**
     * 右键打开 GUI（不依赖多方块结构成型状态）。
     * Shift+右键时跳过 GUI。
     */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                               BlockPos pos, Player player, InteractionHand hand,
                                               BlockHitResult hit) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;

        // Shift+右键 不打开 GUI
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        // 打开 GUI
        MenuProvider menuProvider = state.getMenuProvider(level, pos);
        if (menuProvider != null) {
            player.openMenu(menuProvider, pos);
        }
        return ItemInteractionResult.SUCCESS;
    }

    /**
     * 右键交互（无物品时）。
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        MenuProvider menuProvider = state.getMenuProvider(level, pos);
        if (menuProvider != null) {
            player.openMenu(menuProvider, pos);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}

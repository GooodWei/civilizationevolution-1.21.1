package com.gooodwei.civilizationevolution.server.block;

import com.gooodwei.civilizationevolution.server.item.CivilizationCoreExtractorItem;
import com.gooodwei.civilizationevolution.server.item.ConnectorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

/**
 * 机器方块的抽象基类，提供所有机器方块共用的默认实现。
 *
 * <p>子类只需覆写 {@link #newBlockEntity}、{@link #codec}、{@link #getRenderShape}、
 * {@link #getTicker}，以及可选的 {@link #preOpenMenu} 钩子。
 *
 * <p>已提供默认实现的方法：
 * <ul>
 *   <li>{@link #playerWillDestroy} — 创造模式保留 NBT 到掉落物</li>
 *   <li>{@link #getCloneItemStack} — 保留 NBT 到拾取预览</li>
 *   <li>{@link #useItemOn} — Shift+右键跳过，否则打开 Menu；子类可通过 {@link #preOpenMenu} 添加前置逻辑</li>
 * </ul>
 */
public abstract class AbstractMachineBlock extends BaseEntityBlock {

    protected AbstractMachineBlock(Properties properties) {
        super(properties);
    }

    /**
     * 在打开 GUI 之前调用（服务端）。
     * 默认空实现，子类可覆写以添加扫描、刷新粒子等前置逻辑。
     */
    protected void preOpenMenu(Level level, BlockPos pos) {
    }

    // ==================== 默认实现 ====================

    /**
     * 处理玩家右键交互。
     *
     * <p>交互规则：
     * <ol>
     *   <li>手持连接器（{@link ConnectorItem}）→ 跳过 GUI，由连接器处理绑定</li>
     *   <li>Shift+右键 → 跳过 GUI，允许放置方块等常规操作</li>
     *   <li>否则 → 调用 {@link #preOpenMenu} 钩子后打开容器 GUI</li>
     * </ol>
     *
     * @param stack  玩家手持的物品堆
     * @param state  方块状态
     * @param level  所在世界
     * @param pos    方块坐标
     * @param player 交互的玩家
     * @param hand   交互手
     * @param hit    射线追踪结果
     * @return 客户端侧返回 {@code CONSUME}，服务端侧返回 {@code SUCCESS}
     */
    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state,
                                                        @NotNull Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hit) {
        // 连接器已记录目标 → 跳过方块 GUI，交给连接器处理绑定
        if (stack.getItem() instanceof ConnectorItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 文明核心提取器 Shift+右键 → 跳过方块 GUI，交给提取器处理
        if (stack.getItem() instanceof CivilizationCoreExtractorItem && player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // Shift+右键 时不打开 GUI（允许放置方块等操作）
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            preOpenMenu(level, pos);
            if (be instanceof MenuProvider menuProvider) {
                player.openMenu(menuProvider, pos);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /**
     * 方块被玩家破坏时调用。
     *
     * <p>生存模式：将所有 BlockEntity 数据编码到掉落物上，保留内部物品和 NBT。
     * 创造模式：不产生掉落物。</p>
     *
     * @param level  所在世界
     * @param pos    方块坐标
     * @param state  方块状态
     * @param player 破坏方块的玩家
     * @return 破坏后的方块状态
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be != null && !level.isClientSide && !player.isCreative()) {
            ItemStack stack = new ItemStack(this);
            stack.applyComponents(be.collectComponents());
            ItemEntity drop = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            drop.setDefaultPickUpDelay();
            level.addFreshEntity(drop);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    /**
     * 获取拾取预览物品堆（创造模式中键）。
     *
     * <p><b>普通中键</b>：返回不含 NBT 的空物品堆（与 Ctrl+中键区分）。
     * <b>Ctrl+中键</b>：由游戏引擎在外部调用 {@code BlockEntity.saveToItem()} 追加数据。
     *
     * @param level 世界读取器
     * @param pos   方块坐标
     * @param state 方块状态
     * @return 不含 BlockEntity 数据的纯净物品堆
     */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return super.getCloneItemStack(level, pos, state);
    }
}

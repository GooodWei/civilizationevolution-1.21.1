package com.gooodwei.civilizationevolution.server.block.machine;

import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.api.IMultiBlockMachine;
import com.gooodwei.civilizationevolution.server.item.CivilizationCoreExtractorItem;
import com.gooodwei.civilizationevolution.server.item.ConnectorItem;
import com.gooodwei.civilizationevolution.server.item.DebugStructureGetterItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
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

    /** 水平朝向属性（北/南/西/东），方块正面朝向玩家 */
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /**
     * 获取此方块的 Tier 等级。
     *
     * <p>供 {@link com.gooodwei.civilizationevolution.server.registry.TieredBlockItem}
     * 在物品 tooltip 中显示时代名称，与 BE 的
     * {@link com.gooodwei.civilizationevolution.api.IPopulationMachine#getTier()} 保持一致。
     *
     * @return 此方块对应的 Tier 等级
     */
    public abstract Tier getTier();

    protected AbstractMachineBlock(Properties properties) {
        super(properties);
        // 注册默认方块状态：朝向北方
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /**
     * 在打开 GUI 之前调用（服务端）。
     * 默认空实现，子类可覆写以添加扫描、刷新粒子等前置逻辑。
     */
    protected void preOpenMenu(Level level, BlockPos pos) {
    }

    // ==================== 方向属性 ====================

    /**
     * 注册方块状态定义，添加水平朝向属性。
     *
     * @param builder 方块状态构建器
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /**
     * 方块放置时计算初始状态，正面朝向放置者。
     *
     * @param context 方块放置上下文（包含玩家朝向信息）
     * @return 包含正确朝向的方块状态
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * 方块放置后触发多方块结构验证（仅服务端）。
     *
     * <p>子类可覆写此方法添加额外逻辑（如冲突扫描），但应调用 {@code super.setPlacedBy}。
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @org.jetbrains.annotations.Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IMultiBlockMachine mbe) {
                mbe.validateStructure();
            }
        }
    }

    /**
     * 处理方块旋转（如原版扳手、 /setblock）。
     *
     * @param state    当前方块状态
     * @param rotation 旋转方式
     * @return 旋转后的方块状态
     */
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /**
     * 处理方块镜像反转。
     *
     * @param state  当前方块状态
     * @param mirror 镜像方式
     * @return 镜像后的方块状态
     */
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
        // 结构调试获取器 → 跳过方块 GUI，由物品记录坐标
        if (stack.getItem() instanceof DebugStructureGetterItem) {
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

            // 检查多方块结构解析错误 → 红字警告但不阻止打开 GUI
            if (be instanceof IMultiBlockMachine mbe && mbe.hasParseError()) {
                player.sendSystemMessage(Component.literal(mbe.getParseError()).withStyle(ChatFormatting.RED));
            }
            // 检查多方块结构是否未成型 → 红字提醒（解析错误已单独提示，此处不重复）
            if (be instanceof IMultiBlockMachine mbe && !mbe.isStructureFormed() && !mbe.hasParseError()) {
                player.sendSystemMessage(Component.translatable("msg.civilizationevolution.structure_not_formed").withStyle(ChatFormatting.RED));
            }

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

package com.gooodwei.civilizationevolution.server.block.machine;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.machine.AbstractFarmBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.machine.VillageFarmBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 村庄农场方块 —— Tier 1 自动催熟范围内作物的机器方块。
 *
 * <p>在区块范围内自动催熟 {@link net.minecraft.world.level.block.BonemealableBlock} 作物，
 * 通过 {@link VillageFarmBlockEntity} 处理工作逻辑。
 * 储水罐中的水通过 {@code Capabilities.FluidHandler.BLOCK} 暴露，
 * 支持所有物流模组的管道自动化输入水。
 *
 * <p>手持水桶/流体容器右键可直接补水（1 桶 = 1000 mB），无需打开 GUI。
 * 放置和打开 GUI 时触发冲突检测，防止两个农场并发操作同一作物。
 */
public class VillageFarmBlock extends AbstractMachineBlock {

    /** 序列化编解码器 */
    public static final MapCodec<VillageFarmBlock> CODEC = simpleCodec(VillageFarmBlock::new);

    /**
     * @param properties 方块属性（硬度、爆破阻力等）
     */
    public VillageFarmBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Tier getTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends AbstractMachineBlock> codec() {
        return CODEC;
    }

    // ==================== 方块交互 ====================

    /**
     * 处理玩家右键交互 —— 优先处理水桶/流体容器补水，其次打开 GUI。
     *
     * <p>补水逻辑：
     * <ol>
     *   <li>检查手持物品是否实现了 {@link IFluidHandlerItem} 能力</li>
     *   <li>模拟尝试从中抽取水（每次 1000 mB = 1 桶），通过 {@link FluidTags#WATER} 标签验证流体类型</li>
     *   <li>模拟尝试向储水罐充水，确认有足够空间</li>
     *   <li>两者均满足时：真正从物品抽水 → 充入储水罐 → 替换玩家手中物品（如桶变空桶）</li>
     * </ol>
     *
     * <p>兼容性：任何实现 {@code Capabilities.FluidHandler.ITEM} 的模组物品
     * （原版水桶、Mekanism 桶模式的流体储罐、AE2 流体单元等）均可补水。
     *
     * @param stack  玩家手持的物品堆
     * @param state  方块状态
     * @param level  所在世界
     * @param pos    方块坐标
     * @param player 交互的玩家
     * @param hand   交互手
     * @param hit    射线追踪结果
     * @return 补偿水时返回 {@code SUCCESS}，否则委托给父类打开 GUI
     */
    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state,
                                                        @NotNull Level level, BlockPos pos,
                                                        Player player, InteractionHand hand, BlockHitResult hit) {
        // 尝试用流体容器给农场补水
        IFluidHandlerItem itemFluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (itemFluidHandler != null) {
            // 模拟排水 —— 检查物品中是否至少含有 1 桶（1000 mB）水
            FluidStack waterRequest = new FluidStack(Fluids.WATER, 1000);
            FluidStack simulatedDrain = itemFluidHandler.drain(waterRequest, IFluidHandler.FluidAction.SIMULATE);
            if (!simulatedDrain.isEmpty() && simulatedDrain.is(FluidTags.WATER)
                    && simulatedDrain.getAmount() == 1000) {
                if (level.getBlockEntity(pos) instanceof VillageFarmBlockEntity farmBe) {
                    // 检查储水罐是否有 1000 mB 空间（满桶操作，原版桶不支持部分排水）
                    int couldFill = farmBe.getFluidHandler().fill(simulatedDrain, IFluidHandler.FluidAction.SIMULATE);
                    if (couldFill == 1000) {
                        if (!level.isClientSide) {
                            // 真正从物品抽取 1 桶水
                            itemFluidHandler.drain(waterRequest, IFluidHandler.FluidAction.EXECUTE);
                            // 充入储水罐
                            farmBe.getFluidHandler().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
                            // 创造模式不消耗手持物品（桶不变空、储罐不消耗水）
                            if (!player.isCreative()) {
                                player.setItemInHand(hand, itemFluidHandler.getContainer());
                            }
                        }
                        return ItemInteractionResult.sidedSuccess(level.isClientSide());
                    }
                }
            }
        }

        // 非流体容器 → 委托父类打开 GUI
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    /**
     * 创建村庄农场方块实体。
     *
     * @param blockPos   方块坐标
     * @param blockState 方块状态
     * @return 新的 {@link VillageFarmBlockEntity} 实例
     */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new VillageFarmBlockEntity(blockPos, blockState);
    }

    /**
     * 方块放置后触发冲突扫描（仅服务端）。
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AbstractFarmBlockEntity be) {
            be.onPlacedOrOpened((ServerLevel) level);
        }
    }

    /**
     * 获取方块实体的 Ticker（仅服务端运行）。
     *
     * <p>始终调用 serverTick 以处理粒子边框显示和工作进度推进。
     * 工作进度仅在已绑定时推进，粒子边框不受绑定状态影响。
     */
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, BlockEntityRegistry.VILLAGE_FARM.get(),
                (lvl, pos, st, be) -> VillageFarmBlockEntity.serverTick(lvl, pos, st, be));
    }

    /**
     * 打开 GUI 前的钩子：扫描冲突并刷新粒子边框。
     */
    @Override
    protected void preOpenMenu(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AbstractFarmBlockEntity farmBe) {
            farmBe.onPlacedOrOpened((ServerLevel) level);
        }
    }
}

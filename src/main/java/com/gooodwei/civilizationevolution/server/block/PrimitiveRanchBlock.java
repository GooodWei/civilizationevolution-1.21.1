package com.gooodwei.civilizationevolution.server.block;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractRanchBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.fieldmachine.PrimitiveRanchBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 原始牧场方块 —— Tier 0 喂养范围内动物的机器方块。
 *
 * <p>在区块范围内自动喂养动物（幼年加速成长，成年进入繁殖模式），
 * 通过 {@link PrimitiveRanchBlockEntity} 处理工作逻辑。
 * 放置和打开 GUI 时会触发同类型机器的冲突检测。
 */
public class PrimitiveRanchBlock extends AbstractMachineBlock {
    /** 序列化编解码器 */
    public static final MapCodec<PrimitiveRanchBlock> CODEC = simpleCodec(PrimitiveRanchBlock::new);

    /**
     * @param properties 方块属性（硬度、爆破阻力等）
     */
    public PrimitiveRanchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Tier getTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends AbstractMachineBlock> codec() {
        return CODEC;
    }

    /**
     * 创建原始牧场方块实体。
     *
     * @param blockPos   方块坐标
     * @param blockState 方块状态
     * @return 新的 {@link PrimitiveRanchBlockEntity} 实例
     */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PrimitiveRanchBlockEntity(blockPos, blockState);
    }

    /**
     * 方块放置后触发冲突扫描（仅服务端）。
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AbstractRanchBlockEntity be) {
            be.onPlacedOrOpened((ServerLevel) level);
        }
    }

    /**
     * 获取方块实体的 Ticker（仅服务端运行）。
     *
     * <p>始终调用 serverTick 以处理粒子边框显示和工作进度推进。
     * 工作进度仅在已绑定时推进，粒子边框不受绑定状态影响。</p>
     */
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(type, BlockEntityRegistry.PRIMITIVE_RANCH.get(),
                (lvl, pos, st, be) -> PrimitiveRanchBlockEntity.serverTick(lvl, pos, st, be));
    }

    /**
     * 打开 GUI 前的钩子：扫描冲突并刷新粒子边框。
     */
    @Override
    protected void preOpenMenu(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof AbstractRanchBlockEntity ranchBe) {
            ranchBe.onPlacedOrOpened((ServerLevel) level);
        }
    }
}

package com.gooodwei.civilizationevolution.server.block;

import com.gooodwei.civilizationevolution.server.blockentity.controllermachine.PrimitiveSettlementBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 原始聚落方块 —— 文明控制器的方块实现。
 *
 * <p>原始聚落是当前唯一的控制器实现，接受文明核心物品（CivilizationCoreItem），
 * 统一调度绑定的 {@link com.gooodwei.civilizationevolution.api.IPopulationMachine}（营地、狩猎场等）。
 * 自身通过日晷进度模型驱动工作周期。</p>
 *
 * <p>ticker 仅在服务端运行，直接委托给 {@link PrimitiveSettlementBlockEntity#serverTick}。</p>
 */
public class PrimitiveSettlement extends AbstractMachineBlock {
    /** 序列化编解码器 */
    public static final MapCodec<PrimitiveSettlement> CODEC = simpleCodec(PrimitiveSettlement::new);

    /**
     * @param properties 方块属性（硬度、爆破阻力等）
     */
    public PrimitiveSettlement(Properties properties) {
        super(properties);
    }

    /**
     * 获取方块实体的 Ticker（仅服务端）。
     *
     * <p>客户端返回 {@code null}，服务端委托给 {@link PrimitiveSettlementBlockEntity#serverTick}。</p>
     *
     * @param level           所在世界
     * @param state           方块状态
     * @param blockEntityType BlockEntity 类型
     * @return 服务端 ticker，客户端返回 {@code null}
     */
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null; // 仅服务端 tick
        }
        return createTickerHelper(blockEntityType, BlockEntityRegistry.PRIMITIVE_SETTLEMENT.get(),
                PrimitiveSettlementBlockEntity::serverTick);
    }

    @Override
    protected MapCodec<? extends AbstractMachineBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /**
     * 创建原始聚落方块实体。
     *
     * @param blockPos   方块坐标
     * @param blockState 方块状态
     * @return 新的 {@link PrimitiveSettlementBlockEntity} 实例
     */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PrimitiveSettlementBlockEntity(blockPos, blockState);
    }
}

package com.gooodwei.civilizationevolution.server.block;

import com.gooodwei.civilizationevolution.server.blockentity.machine.CampBlockEntity;
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
 * 营地方块 —— 具有 BlockEntity 的基础机器方块。
 *
 * <p>营地是最基础的机器类型，提供食物输入、人口安置和产物输出的 GUI。
 * 其工作逻辑由绑定的控制器（如 {@link com.gooodwei.civilizationevolution.server.blockentity.PrimitiveSettlementBlockEntity}）统一调度，
 * 自身无独立 ticker。</p>
 */
public class CampBlock extends AbstractMachineBlock {

    /** 序列化编解码器 */
    public static final MapCodec<CampBlock> CODEC = simpleCodec(CampBlock::new);

    /**
     * @param properties 方块属性（硬度、爆破阻力、无碰撞箱等）
     */
    public CampBlock(Properties properties) {
        super(properties);
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
     * 创建营地方块实体。
     *
     * @param pos   方块坐标
     * @param state 方块状态
     * @return 新的 {@link CampBlockEntity} 实例
     */
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CampBlockEntity(pos, state);
    }

    /**
     * 营地无独立 ticker，始终返回 {@code null}。
     *
     * <p>营地的工作由绑定的文明控制器统一调度，因此无需自身 tick。</p>
     *
     * @param level 所在世界
     * @param state 方块状态
     * @param type  BlockEntity 类型
     * @return 始终返回 {@code null}
     */
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return null;
    }
}

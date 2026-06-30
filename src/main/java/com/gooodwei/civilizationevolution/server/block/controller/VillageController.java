package com.gooodwei.civilizationevolution.server.block.controller;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.controller.VillageControllerBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 村庄控制器方块 —— 比原始聚落更高级的文明控制器。
 *
 * <p>ticker 仅在服务端运行，直接委托给 {@link VillageControllerBlockEntity#serverTick}。
 * 收到红石信号且核心有效时发射信标光柱。</p>
 */
public class VillageController extends AbstractControllerBlock {
    /** 序列化编解码器 */
    public static final MapCodec<VillageController> CODEC = simpleCodec(VillageController::new);

    public VillageController(Properties properties) {
        super(properties);
    }

    @Override
    public Tier getTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    /**
     * 获取方块实体的 Ticker（仅服务端）。
     *
     * <p>客户端返回 {@code null}，服务端委托给 {@link VillageControllerBlockEntity#serverTick}。</p>
     *
     * @param level           所在世界
     * @param state           方块状态
     * @param blockEntityType BlockEntity 类型
     * @return 服务端 ticker，客户端返回 {@code null}
     */
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                              BlockEntityType<T> blockEntityType) {
        if (level.isClientSide) {
            return null;
        }
        return createTickerHelper(blockEntityType, BlockEntityRegistry.VILLAGE_CONTROLLER.get(),
                VillageControllerBlockEntity::serverTick);
    }

    /**
     * 创建村庄控制器方块实体。
     *
     * @param blockPos   方块坐标
     * @param blockState 方块状态
     * @return 新的 {@link VillageControllerBlockEntity} 实例
     */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new VillageControllerBlockEntity(blockPos, blockState);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}

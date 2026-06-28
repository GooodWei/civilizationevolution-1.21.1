package com.gooodwei.civilizationevolution.server.blockentity.controllermachine;

import com.gooodwei.civilizationevolution.api.tier.ModTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractControllerBlockEntity;
import com.gooodwei.civilizationevolution.server.menu.PrimitiveSettlementMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 原始聚落方块实体 —— 文明控制器的初级阶段。
 *
 * <p>继承自 {@link AbstractControllerBlockEntity}，只需覆写类型特定的常量和方法。
 * 所有控制器通用逻辑（核心管理、绑定、调度、同步、持久化）均由父类提供。
 */
public class PrimitiveSettlementBlockEntity extends AbstractControllerBlockEntity {

    public static final int SIZE = 1;

    private static final String TYPE = "primitive_settlement";
    private static final int CHUNK_LOAD_RADIUS = 1; // 3×3 区块

    public PrimitiveSettlementBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.PRIMITIVE_SETTLEMENT.get(), pos, blockState, SIZE);
    }

    // ==================== 抽象方法实现 ====================

    @Override
    protected String getControllerType() {
        return TYPE;
    }

    @Override
    protected int getChunkLoadRadius() {
        return CHUNK_LOAD_RADIUS;
    }

    @Override
    public Tier getTier() {
        return ModTiers.PRIMITIVE;
    }

    @Override
    protected boolean isViewingController(ServerPlayer sp) {
        return sp.containerMenu instanceof PrimitiveSettlementMenu menu
                && menu.getBlockPos().equals(getBlockPos());
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PrimitiveSettlementMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.primitive_settlement");
    }

    // ==================== 供 Block ticker 引用 ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   PrimitiveSettlementBlockEntity be) {
        AbstractControllerBlockEntity.controllerServerTick(level, pos, state, be);
    }
}

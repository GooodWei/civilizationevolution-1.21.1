package com.gooodwei.civilizationevolution.server.blockentity.controller;

import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveControllerMenu;
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
public class PrimitiveControllerBlockEntity extends AbstractControllerBlockEntity {

    public static final int SIZE = 1;

    private static final String TYPE = "primitive_controller";
    private static final int CHUNK_LOAD_RADIUS = 1; // 3×3 区块

    public PrimitiveControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.PRIMITIVE_CONTROLLER.get(), pos, blockState, SIZE);
    }

    // ==================== 抽象方法实现 ====================

    @Override
    public Tier getTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    @Override
    public String getConfigKey() {
        return PopulationMachineConfig.PRIMITIVE_CONTROLLER;
    }

    @Override
    public String getWorkerCareer() {
        return CareerNames.UNEMPLOYED;
    }

    @Override
    public String getControllerType() {
        return TYPE;
    }

    @Override
    protected int getChunkLoadRadius() {
        return CHUNK_LOAD_RADIUS;
    }

    @Override
    protected boolean isViewingController(ServerPlayer sp) {
        return sp.containerMenu instanceof PrimitiveControllerMenu menu
                && menu.getBlockPos().equals(getBlockPos());
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PrimitiveControllerMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.primitive_controller");
    }

    // ==================== 供 Block ticker 引用 ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   PrimitiveControllerBlockEntity be) {
        AbstractControllerBlockEntity.controllerServerTick(level, pos, state, be);
    }
}

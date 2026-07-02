package com.gooodwei.civilizationevolution.server.blockentity.controller;

import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.machine.VillageControllerMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 村庄控制器方块实体 —— 比原始聚落更高级的文明控制器。
 *
 * <p>继承自 {@link AbstractControllerBlockEntity}，继承了全部控制器通用功能。
 * 相比原始聚落：
 * <ul>
 *   <li>区块强加载范围更大（5×5 vs 3×3）</li>
 *   <li>可绑定更多机器（通过配置的 max_bind_count 控制）</li>
 *   <li>后续可扩展更高级的调度模型和额外功能</li>
 * </ul>
 */
public class VillageControllerBlockEntity extends AbstractControllerBlockEntity {

    public static final int SIZE = 1;

    private static final String TYPE = "village_controller";
    private static final int CHUNK_LOAD_RADIUS = 2; // 5×5 区块，比原始聚落范围更大

    public VillageControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.VILLAGE_CONTROLLER.get(), pos, blockState, SIZE);
    }

    // ==================== 抽象方法实现 ====================

    @Override
    public Tier getTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    public String getConfigKey() {
        return PopulationMachineConfig.VILLAGE_CONTROLLER;
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
        return sp.containerMenu instanceof VillageControllerMenu menu
                && menu.getBlockPos().equals(getBlockPos());
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new VillageControllerMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.village_controller");
    }

    // ==================== 供 Block ticker 引用 ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   VillageControllerBlockEntity be) {
        AbstractControllerBlockEntity.controllerServerTick(level, pos, state, be);
    }
}

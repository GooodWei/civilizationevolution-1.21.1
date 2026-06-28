package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.tier.ModTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractControllerBlockEntity;
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
    protected String getControllerType() {
        return TYPE;
    }

    @Override
    protected int getChunkLoadRadius() {
        return CHUNK_LOAD_RADIUS;
    }

    @Override
    public Tier getTier() {
        return ModTiers.VILLAGE;
    }

    @Override
    protected boolean isViewingController(ServerPlayer sp) {
        // TODO: 等 VillageControllerMenu 创建后改为对应的 menu instanceof 检查
        return false;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        // TODO: 等 VillageControllerMenu 创建
        return null;
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

package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveCampMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 原始营地方块的 BlockEntity（Tier 0）。
 *
 * <p>继承自 {@link AbstractCampBlockEntity}，仅提供 Tier 0 特有的参数。
 * 所有营地通用业务逻辑（繁殖、食物消耗）由父类提供。
 */
public class PrimitiveCampBlockEntity extends AbstractCampBlockEntity {
    public static final int SIZE = 10;

    public PrimitiveCampBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.PRIMITIVE_CAMP_BLOCK_ENTITY.get(), pos, blockState, SIZE);
    }

    // ==================== 抽象方法实现 ====================

    @Override
    public Tier getTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    @Override
    protected String getMachineConfigKey() {
        return PopulationMachineConfig.CAMP;
    }

    @Override
    public String getWorkerCareer() {
        return CareerNames.UNEMPLOYED;
    }

    // ==================== serverTick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   PrimitiveCampBlockEntity campBlockEntity) {
        // 营地的工作由控制器调度，无独立 tick 逻辑
    }

    // ==================== GUI ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.civilizationevolution.primitive_camp");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.primitive_camp");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PrimitiveCampMenu(containerId, inventory, this);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PrimitiveCampMenu(containerId, inventory, this);
    }
}

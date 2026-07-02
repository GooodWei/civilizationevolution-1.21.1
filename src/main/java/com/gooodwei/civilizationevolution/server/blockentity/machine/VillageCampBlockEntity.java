package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.machine.VillageCampMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 村庄营地方块的 BlockEntity（Tier 1）。
 *
 * <p>继承自 {@link AbstractCampBlockEntity}，仅提供 Tier 1 特有的参数。
 * 所有营地通用业务逻辑（繁殖、食物消耗）由父类提供。
 */
public class VillageCampBlockEntity extends AbstractCampBlockEntity {
    public static final int SIZE = 10;

    public VillageCampBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.VILLAGE_CAMP.get(), pos, blockState, SIZE);
    }

    // ==================== 抽象方法实现 ====================

    @Override
    public Tier getTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    protected String getMachineConfigKey() {
        return PopulationMachineConfig.VILLAGE_CAMP;
    }

    @Override
    public String getWorkerCareer() {
        return CareerNames.UNEMPLOYED;
    }

    // ==================== GUI ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.civilizationevolution.village_camp");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.village_camp");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new VillageCampMenu(containerId, inventory, this);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new VillageCampMenu(containerId, inventory, this);
    }
}

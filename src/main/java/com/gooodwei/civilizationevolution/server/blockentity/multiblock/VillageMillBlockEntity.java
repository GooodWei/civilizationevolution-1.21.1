package com.gooodwei.civilizationevolution.server.blockentity.multiblock;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.config.CivilizationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.machine.VillageMillMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 村庄磨坊方块实体 —— Tier 1 多方块研磨机器。
 *
 * <p>使用自适应轮询（热/冷状态），从物品输入仓取原料研磨为产物。
 *
 * <h3>槽位布局</h3>
 * <ul>
 *   <li>槽位 0-3：人口输入槽（石匠职业）</li>
 * </ul>
 */
public class VillageMillBlockEntity extends AbstractMillBlockEntity {


    public VillageMillBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.VILLAGE_MILL.get(), pos, state, 4);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                  VillageMillBlockEntity be) {
        AbstractMillBlockEntity.serverTick(level, pos, state, be);
    }

    @Override
    public String getConfigKey() {
        return CivilizationMachineConfig.VILLAGE_MILL;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.village_mill");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        if (!isStructureFormed()) return null;
        return new VillageMillMenu(containerId, inventory, this, data);
    }

    @Override
    public Tier getTier() {
        return CivilizationTiers.VILLAGE;
    }
}

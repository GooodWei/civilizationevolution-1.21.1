package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.menu.hatch.VillageItemInputHatchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 村庄物品输入接口 —— Tier 1。
 *
 * <p>9 个槽位，接受任意物品，堆叠上限 64。
 */
public class VillageItemInputHatchBlockEntity extends AbstractItemInputHatchBlockEntity {

    public static final int SIZE = 9;

    public VillageItemInputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SIZE);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    protected String getContainerName() {
        return "container.civilizationevolution.village_item_input_hatch";
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new VillageItemInputHatchMenu(containerId, inventory, this);
    }
}

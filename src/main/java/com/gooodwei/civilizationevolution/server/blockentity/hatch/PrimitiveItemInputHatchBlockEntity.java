package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.menu.hatch.ItemInputHatchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 原始物品输入接口 —— Tier 0。
 *
 * <p>1 个槽位，接受任意物品。
 */
public class PrimitiveItemInputHatchBlockEntity extends AbstractItemInputHatchBlockEntity {

    public PrimitiveItemInputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ItemInputHatchMenu(containerId, inventory, this);
    }
}

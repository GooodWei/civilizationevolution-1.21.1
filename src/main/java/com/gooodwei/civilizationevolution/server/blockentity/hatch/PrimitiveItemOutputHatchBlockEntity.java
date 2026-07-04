package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.menu.hatch.ItemOutputHatchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 原始物品输出接口 —— Tier 0。
 *
 * <p>1 个槽位，不接受外部放入（仅代码产出）。
 */
public class PrimitiveItemOutputHatchBlockEntity extends AbstractItemOutputHatchBlockEntity {

    public PrimitiveItemOutputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ItemOutputHatchMenu(containerId, inventory, this);
    }
}

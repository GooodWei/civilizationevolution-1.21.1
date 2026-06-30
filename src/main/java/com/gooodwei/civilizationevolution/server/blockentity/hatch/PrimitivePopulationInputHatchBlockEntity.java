package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.menu.hatch.PrimitivePopulationInputHatchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 原始人口输入接口 —— Tier 0。
 *
 * <p>1 个槽位，仅接受 {@code PopulationItem}。
 * 控制器通过此接口拉取待治疗的人口物品。
 */
public class PrimitivePopulationInputHatchBlockEntity extends AbstractPopulationInputHatchBlockEntity {

    public PrimitivePopulationInputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PrimitivePopulationInputHatchMenu(containerId, inventory, this);
    }
}

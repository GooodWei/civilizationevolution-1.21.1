package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.menu.hatch.PrimitiveFoodInputHatchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 原始食物输入接口 —— Tier 0。
 *
 * <p>1 个槽位，仅接受食物物品。
 * 控制器通过此接口消耗食物（预留，Tier 0 诊所暂不消耗食物）。
 */
public class PrimitiveFoodInputHatchBlockEntity extends AbstractFoodInputHatchBlockEntity {

    public PrimitiveFoodInputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PrimitiveFoodInputHatchMenu(containerId, inventory, this);
    }
}

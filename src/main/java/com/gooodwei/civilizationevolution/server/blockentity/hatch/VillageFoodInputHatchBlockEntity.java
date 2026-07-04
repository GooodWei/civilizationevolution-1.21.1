package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.menu.hatch.FoodInputHatchMenu;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 村庄食物输入接口 —— Tier 1。
 *
 * <p>1 个槽位，仅接受食物物品。
 * 多方块控制器通过此接口消耗食物。
 */
public class VillageFoodInputHatchBlockEntity extends AbstractFoodInputHatchBlockEntity {

    public VillageFoodInputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FoodInputHatchMenu(MenuRegistry.VILLAGE_FOOD_INPUT_HATCH_MENU.get(), containerId, inventory, this);
    }
}

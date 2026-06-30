package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.menu.hatch.PrimitivePopulationOutputHatchMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 原始人口输出接口 —— Tier 0。
 *
 * <p>1 个槽位，拒绝手动放入（仅代码产出）。
 * 控制器将治疗后健康度超阈值的人口物品推送到此接口。
 */
public class PrimitivePopulationOutputHatchBlockEntity extends AbstractPopulationOutputHatchBlockEntity {

    public PrimitivePopulationOutputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public Tier getPartTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PrimitivePopulationOutputHatchMenu(containerId, inventory, this);
    }
}

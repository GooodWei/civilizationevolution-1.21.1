package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 物品输出接口的抽象 BE。
 *
 * <p>1 个槽位，拒绝外部放入（仅代码产出），堆叠上限 64。
 * 后续更高级的物品输出接口只需继承此类并覆写 {@link #getPartTier()}。
 */
public abstract class AbstractItemOutputHatchBlockEntity extends AbstractHatchBlockEntity {

    protected AbstractItemOutputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public String getPartType() {
        return TYPE_OUTPUT_HATCH;
    }

    @Override
    protected String getContainerName() {
        return "container.civilizationevolution.item_output_hatch";
    }
}

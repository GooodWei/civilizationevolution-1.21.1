package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 物品输入接口的抽象 BE。
 *
 * <p>接受任意物品，堆叠上限 64。
 * 槽位数由子类通过构造器参数决定（Primitive 为 1，Village 为 27）。
 */
public abstract class AbstractItemInputHatchBlockEntity extends AbstractHatchBlockEntity {

    protected AbstractItemInputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                                 int slotCount) {
        super(type, pos, state, slotCount);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public String getPartType() {
        return TYPE_INPUT_HATCH;
    }

    @Override
    protected String getContainerName() {
        return "container.civilizationevolution.item_input_hatch";
    }
}

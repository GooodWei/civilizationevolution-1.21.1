package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 食物输入接口的抽象 BE。
 *
 * <p>仅接受具有 {@link DataComponents#FOOD} 的食物物品，堆叠上限为 64。
 * 后续更高级的食物输入接口只需继承此类并覆写 {@link #getPartTier()}。
 */
public abstract class AbstractFoodInputHatchBlockEntity extends AbstractHatchBlockEntity {

    protected AbstractFoodInputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.has(DataComponents.FOOD);
    }

    @Override
    public String getPartType() {
        return TYPE_FOOD_HATCH;
    }

    @Override
    protected String getContainerName() {
        return "container.civilizationevolution.food_input_hatch";
    }
}

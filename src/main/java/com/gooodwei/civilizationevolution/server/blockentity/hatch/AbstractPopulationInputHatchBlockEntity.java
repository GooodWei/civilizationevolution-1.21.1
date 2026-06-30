package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import com.gooodwei.civilizationevolution.server.item.PopulationItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 人口输入接口的抽象 BE。
 *
 * <p>仅接受 {@link PopulationItem}，堆叠上限为 1。
 * 后续更高级的人口输入接口只需继承此类并覆写 {@link #getPartTier()}。
 */
public abstract class AbstractPopulationInputHatchBlockEntity extends AbstractHatchBlockEntity {

    protected AbstractPopulationInputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof PopulationItem;
    }

    @Override
    public String getPartType() {
        return TYPE_INPUT_HATCH;
    }

    @Override
    protected String getContainerName() {
        return "container.civilizationevolution.population_input_hatch";
    }
}

package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 人口输出接口的抽象 BE。
 *
 * <p>拒绝所有外部放入的物品（{@link #canPlaceItem} 返回 false），
 * 仅由多方块控制器代码产出。堆叠上限为 1。
 * 后续更高级的人口输出接口只需继承此类并覆写 {@link #getPartTier()}。
 */
public abstract class AbstractPopulationOutputHatchBlockEntity extends AbstractHatchBlockEntity {

    protected AbstractPopulationOutputHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 1);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    @Override
    public String getPartType() {
        return TYPE_POPULATION_OUTPUT_HATCH;
    }

    @Override
    protected String getContainerName() {
        return "container.civilizationevolution.population_output_hatch";
    }
}

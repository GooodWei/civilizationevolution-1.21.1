package com.gooodwei.civilizationevolution.server.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;


/**
 * 研磨配方的输入定义。
 * <p>
 * 研磨配方只有 1 个输入槽位，接受单个物品。
 */
public record GrindingRecipeInput(ItemStack input) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        if (index != 0){
            throw new IllegalArgumentException("研磨配方只有 1 个输入槽位，索引: " + index);
        }
        return input;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return input.isEmpty();
    }
}

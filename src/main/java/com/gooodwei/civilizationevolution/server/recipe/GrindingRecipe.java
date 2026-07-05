package com.gooodwei.civilizationevolution.server.recipe;

import com.gooodwei.civilizationevolution.server.registry.ModRecipeTypes;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * 研磨配方 —— 将输入物品研磨为一种或多种产物。
 *
 * <h3>JSON 格式</h3>
 * <pre>{@code
 * {
 *   "type": "civilizationevolution:grinding",
 *   "input": { "item": "minecraft:wheat" },
 *   "min_handle_tier": 1,
 *   "work_time": 300,
 *   "output": [
 *     {
 *       "item": { "id": "minecraft:sugar", "count": 2 },
 *       "allow_extra_output_tier": 1
 *     },
 *     {
 *       "item": { "id": "minecraft:bone_meal", "count": 1 },
 *       "allow_extra_output_tier": -1
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <h3>字段语义</h3>
 * <ul>
 *   <li>{@code minHandleTier}：机器 Tier 低于此值时<b>完全不能执行</b>此配方</li>
 *   <li>{@code allowExtraOutputTier}：机器 Tier 达到此值时效率开始乘算产出数量；
 *       -1 表示该产物不受效率影响，始终产出固定数量</li>
 * </ul>
 *
 * @param input         输入原料
 * @param output        研磨产物列表
 * @param minHandleTier 处理此配方所需的最低机器 Tier
 * @param workTime      完成此配方所需的工作时间（tick）
 */
public record GrindingRecipe(Ingredient input, List<GrindingOutput> output,
                             int minHandleTier, int workTime) implements Recipe<GrindingRecipeInput> {

    // ==================== Codec（JSON 序列化） ====================

    public static final MapCodec<GrindingRecipe> CODEC = RecordCodecBuilder.mapCodec(
            instance ->
                    instance.group(
                            Ingredient.CODEC.fieldOf("input").forGetter(GrindingRecipe::input),
                            GrindingOutput.CODEC.codec().listOf().fieldOf("output").forGetter(GrindingRecipe::output),
                            Codec.INT.optionalFieldOf("min_handle_tier", 0).forGetter(GrindingRecipe::minHandleTier),
                            Codec.INT.optionalFieldOf("work_time", 400).forGetter(GrindingRecipe::workTime))
                            .apply(instance, GrindingRecipe::new));

    // ==================== StreamCodec（网络同步） ====================

    public static final StreamCodec<RegistryFriendlyByteBuf, GrindingRecipe> STREAM_CODEC = StreamCodec.of(
            // 编码：配方 → 网络包
            (RegistryFriendlyByteBuf buf, GrindingRecipe recipe) -> {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, recipe.input());
                GrindingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()).encode(buf, recipe.output());
                buf.writeInt(recipe.minHandleTier());
                buf.writeInt(recipe.workTime());
            },
            // 解码：网络包 → 配方
            (RegistryFriendlyByteBuf buf) -> {
                Ingredient input = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                List<GrindingOutput> output = GrindingOutput.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buf);
                int minHandleTier = buf.readInt();
                int workTime = buf.readInt();
                return new GrindingRecipe(input, output, minHandleTier, workTime);
            });

    @Override
    public boolean matches(GrindingRecipeInput grindingRecipeInput, Level level) {
        return input.test(grindingRecipeInput.input());
    }


    /**
     * 组装产物（仅返回第一个输出物品），供 JEI / 配方书展示。
     * 机器实际使用时<b>应直接遍历 {@link #output()} 列表</b>。
     */
    @Override
    public ItemStack assemble(GrindingRecipeInput grindingRecipeInput, HolderLookup.Provider provider) {
        return getResultItem(provider);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider provider) {
        return output.isEmpty() ? ItemStack.EMPTY : output.get(0).item().copy();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, input);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.GRINDING_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.GRINDING_TYPE.get();
    }

    // ==================== 便捷查询 ====================

    /**
     * 从 {@code RecipeManager} 查找匹配当前输入的研磨配方。
     *
     * <p>此方法仅根据输入匹配配方，<b>不做 Tier 检查</b>。
     * 调用方需自行判断 {@link #minHandleTier()}。
     *
     * @param level 服务端 Level，用于获取 RecipeManager
     * @param input 输入物品
     * @return 匹配的配方，未找到则 {@code Optional.empty()}
     */
    public static Optional<GrindingRecipe> findMatch(Level level, ItemStack input) {
        try {
            return level.getRecipeManager()
                    .getRecipeFor(ModRecipeTypes.GRINDING_TYPE.get(),
                            new GrindingRecipeInput(input), level).map(RecipeHolder::value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "[civilizationevolution] GrindingRecipeInput 槽位索引越界！"
                            + " size()=1 但访问了无效索引。"
                            + " 这表示 GrindingRecipeInput.getItem() 的实现有 bug。"
                            + " 原始异常: " + e.getMessage(), e);
        }
    }
}

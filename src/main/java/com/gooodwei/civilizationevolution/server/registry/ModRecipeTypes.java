package com.gooodwei.civilizationevolution.server.registry;


import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.recipe.GrindingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * 自定义配方类型和序列化器的注册中心。
 *
 * <p>新增配方的步骤：
 * <ol>
 *   <li>在此类中添加 {@code RecipeType} 和 {@code RecipeSerializer} 的注册</li>
 *   <li>在 {@link #register(IEventBus)} 调用注册</li>
 * </ol>
 */
public class ModRecipeTypes {
    /** 配方类型注册器 */
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, CivilizationEvolution.MODID);

    /** 配方序列化器注册器 */
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CivilizationEvolution.MODID);

    // ==================== 研磨配方 ====================

    /** 研磨配方类型 */
    public static final Supplier<RecipeType<GrindingRecipe>> GRINDING_TYPE =
            RECIPE_TYPES.register("grinding",
                    () -> RecipeType.simple(
                            ResourceLocation.fromNamespaceAndPath(
                                    CivilizationEvolution.MODID, "grinding")));

    /** 研磨配方序列化器 —— 负责 JSON ↔ 配方 的转换 */
    public static final Supplier<RecipeSerializer<GrindingRecipe>> GRINDING_SERIALIZER =
            RECIPE_SERIALIZERS.register("grinding",
                    () -> new RecipeSerializer<>() {
                        @Override
                        public com.mojang.serialization.MapCodec<GrindingRecipe> codec() {
                            return GrindingRecipe.CODEC;
                        }

                        @Override
                        public StreamCodec<RegistryFriendlyByteBuf, GrindingRecipe> streamCodec() {
                            return GrindingRecipe.STREAM_CODEC;
                        }
                    });

    // ==================== 注册入口 ====================

    /**
     * 向事件总线注册所有配方类型和序列化器。
     */
    public static void register(IEventBus bus) {
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
    }
}

package com.gooodwei.civilizationevolution.server.item;

import com.gooodwei.civilizationevolution.api.CivilizationAPI;
import com.gooodwei.civilizationevolution.server.registry.ItemRegistry;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * 招募者物品 —— 右键村民将其转化为人口物品。
 *
 * <p>使用方式：主手持有招募者，右键成年村民。
 * 村民会被立即移除，其职业、年龄等属性转化为一个人口物品。
 * 人口物品优先放入玩家背包，背包满时以掉落物形式生成在村民位置。
 */
public class Recruiter extends Item {

    public Recruiter(Properties properties) {
        super(properties);
    }

    /**
     * 右键活体实体（仅处理村民）。
     *
     * <p>仅在主手且目标为存活村民时生效：
     * <ol>
     *   <li>调用 {@link CivilizationAPI#getPopulationManager()} 将村民属性转为人口物品</li>
     *   <li>优先放入玩家背包，满背包则生成掉落物</li>
     *   <li>消耗 1 个招募者物品</li>
     *   <li>移除村民实体</li>
     * </ol>
     *
     * @param stack             玩家手持的物品
     * @param player            使用物品的玩家
     * @param interactionTarget 被右键的活体实体
     * @param usedHand          使用的手（主手/副手）
     * @return 成功转化村民返回 {@link InteractionResult#CONSUME}，否则 {@link InteractionResult#PASS}
     */
    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, Player player,
                                                           @NotNull LivingEntity interactionTarget, @NotNull InteractionHand usedHand) {
        if (usedHand == InteractionHand.MAIN_HAND
                && interactionTarget instanceof Villager villager && villager.isAlive()) {
            if (!player.level().isClientSide) {
                ItemStack population = CivilizationAPI.getPopulationManager().fromVillager(villager, ItemRegistry.POPULATION.get());
                if (player.getInventory().getFreeSlot() != -1) {
                    player.addItem(population);
                    stack.consume(1, player);
                } else {
                    // 背包已满，在村民位置生成人口掉落物
                    villager.level().addFreshEntity(new ItemEntity(
                            villager.level(),
                            villager.getX(), villager.getY(), villager.getZ(),
                            population));
                    stack.consume(1, player);
                }
                villager.discard();
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

}

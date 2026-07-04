package com.gooodwei.civilizationevolution.server.item;

import com.gooodwei.civilizationevolution.api.CivilizationAPI;
import com.gooodwei.civilizationevolution.api.IPopulationItem;
import com.gooodwei.civilizationevolution.api.career.Career;
import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import com.gooodwei.civilizationevolution.server.population.Population;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;

import java.util.List;

/**
 * 人口物品 —— 模组核心物品，表示一个可工作的人口单位。
 *
 * <p>所有人口属性（职业、年龄、寿命、健康度、饱食度等）通过 NBT 存储在物品的
 * DataComponent 中，而非作为独立的物品子类。不同职业的人口共用同一个物品实例，
 * 依靠 NBT 中的 {@code Career} 字段区分。
 *
 * <p>物品显示名称动态读取职业 NBT，tooltip 展示完整的人口属性面板。
 */
public class PopulationItem extends Item implements IPopulationItem {

    public PopulationItem(Properties properties) {
        super(properties);
    }

    /**
     * 动态获取物品显示名称。
     * 从 NBT 读取职业字段，若职业存在则返回职业的翻译名称，否则回退到默认物品名。
     *
     * @param stack 人口物品
     * @return 职业翻译名或默认物品名
     */
    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String careerName = tag.getString(Population.TAG_CAREER);
        if (!careerName.isEmpty()) {
            Career career = CivilizationAPI.getCareerRegistry().byName(careerName);
            if (career != null) {
                return Component.translatable(career.getTranslationKey());
            }
        }
        return super.getName(stack);
    }

    /**
     * 渲染人口物品的完整 tooltip 信息面板。
     * 包括：死亡标记（如有）、职业、年龄/寿命、生命值、饱食度、熟练度、工作效率、心理状态、性别。
     *
     * @param stack             人口物品
     * @param context           tooltip 上下文
     * @param tooltipComponents tooltip 行列表
     * @param tooltipFlag       tooltip 标志（高级/普通）
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.isEmpty()) return;

        // 死亡标记 —— 最顶部，醒目
        if (PopulationNBT.isDead(stack)) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.civilizationevolution.population.dead")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
        }

        int lifespan = tag.getInt(Population.TAG_LIFESPAN);
        if (lifespan <= 0) return;
        int age = tag.getInt(Population.TAG_AGE);

        // 退休标记 —— 年龄超过 65 岁但未死亡的人口
        if (!PopulationNBT.isDead(stack) && age > 65) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.civilizationevolution.population.retired")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
        }

        String careerName = tag.getString(Population.TAG_CAREER);
        Career career = CivilizationAPI.getCareerRegistry().byName(careerName);
        tooltipComponents.add(line("career", career != null
                ? Component.translatable(career.getTranslationKey())
                : Component.literal(careerName)));
        tooltipComponents.add(line("age", age));
        tooltipComponents.add(line("lifespan", lifespan));
        tooltipComponents.add(line("health", tag.getInt(Population.TAG_HEALTH)));
        tooltipComponents.add(line("food", tag.getInt(Population.TAG_FOOD)));
        tooltipComponents.add(line("proficiency", tag.getInt(Population.TAG_PROFICIENCY)));
        tooltipComponents.add(line("workEfficiency",
                String.format("%.2f", tag.getDouble(Population.TAG_WORK_EFFICIENCY))));
        tooltipComponents.add(line("mentalState",
                String.format("%.2f", tag.getDouble(Population.TAG_MENTAL_STATE))));
        tooltipComponents.add(line("gender", tag.getBoolean(Population.TAG_GENDER)
                ? Component.translatable("tooltip.civilizationevolution.population.gender.male")
                : Component.translatable("tooltip.civilizationevolution.population.gender.female")));

        // 学徒经验 —— 遍历所有已累积经验的职业
        CompoundTag careerExps = tag.getCompound(Population.TAG_CAREER_EXPS);
        if (!careerExps.isEmpty()) {
            for (String expCareerName : careerExps.getAllKeys()) {
                int exp = careerExps.getInt(expCareerName);
                if (exp <= 0) continue;
                Career targetCareer = Career.byName(expCareerName);
                if (targetCareer == null) continue;
                int threshold = targetCareer.getApprenticeExpThreshold();
                if (threshold <= 0) continue; // 不可晋升的职业不显示
                tooltipComponents.add(Component.translatable(
                        "tooltip.civilizationevolution.population.apprentice_exp",
                        Component.translatable(targetCareer.getTranslationKey()),
                        exp,
                        threshold)
                        .withStyle(ChatFormatting.GREEN));
            }
        }
    }

    /**
     * 生成单行 tooltip 的便捷方法。
     *
     * @param key   tooltip key 后缀（如 {@code "age"} 拼成 {@code tooltip...population.age}）
     * @param value 值（可以是字符串、数字或 Component）
     * @return 格式化后的 tooltip 行 Component
     */
    private static Component line(String key, Object value) {
        return Component.translatable("tooltip.civilizationevolution.population." + key, value)
                .withStyle(ChatFormatting.BLUE);
    }
}

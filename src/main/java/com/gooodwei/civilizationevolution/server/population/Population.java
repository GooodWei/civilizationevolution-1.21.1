package com.gooodwei.civilizationevolution.server.population;

import com.gooodwei.civilizationevolution.api.CivilizationAPI;
import com.gooodwei.civilizationevolution.api.IPopulationManager;
import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.event.PopulationDeathEvent;
import com.gooodwei.civilizationevolution.server.config.PopulationConfig;
import com.gooodwei.civilizationevolution.server.item.PopulationItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 用于创建和操作人口物品的工具类。
 *
 * 人口物品是一个 {@code civilizationevolution:population} 类型的 ItemStack，
 * 其 NBT 标签代表一个人的各项属性。
 */
public final class Population {

    /** NBT 标签键：年龄（int，单位：年） */
    public static final String TAG_AGE = "age";
    /** NBT 标签键：寿命上限（int，单位：年） */
    public static final String TAG_LIFESPAN = "lifespan";
    /** NBT 标签键：性别（boolean，true = 男，false = 女） */
    public static final String TAG_GENDER = "gender";
    /** NBT 标签键：生命值（int，范围 0-100） */
    public static final String TAG_HEALTH = "health";
    /** NBT 标签键：饱食度（int，范围 0-100） */
    public static final String TAG_FOOD = "food";
    /** NBT 标签键：职业名称（String，如 "armorer"） */
    public static final String TAG_CAREER = "career";
    /** NBT 标签键：熟练度（int，默认 100） */
    public static final String TAG_PROFICIENCY = "proficiency";
    /** NBT 标签键：工作效率（double，综合计算值） */
    public static final String TAG_WORK_EFFICIENCY = "workEfficiency";
    /** NBT 标签键：精神状态（double，范围 0.0-1.0） */
    public static final String TAG_MENTAL_STATE = "mentalState";
    /** NBT 标签键：职业经验映射（CompoundTag，键=职业名，值=经验值 int） */
    public static final String TAG_CAREER_EXPS = "careerExps";
    /** NBT 标签键：死亡标记（boolean，true = 已死亡） */
    public static final String TAG_DEAD = "dead";

    /** IPopulationManager 接口的单例实例，供 CivilizationAPI 暴露 */
    public static final IPopulationManager INSTANCE = new IPopulationManager() {
        @Override
        public ItemStack fromVillager(Villager villager, PopulationItem item) {
            return Population.fromVillager(villager, item);
        }

        @Override
        public boolean addAge(ItemStack stack) { return Population.addAge(stack); }

        @Override
        public boolean isDead(ItemStack stack) { return Population.isDead(stack); }

        @Override
        public void recalcEfficiency(ItemStack stack) { Population.recalcEfficiency(stack); }

        @Override
        public void setHealth(ItemStack stack, int health) { Population.setHealth(stack, health); }

        @Override
        public void setFood(ItemStack stack, int food) { Population.setFood(stack, food); }

        @Override
        public void setProficiency(ItemStack stack, int proficiency) { Population.setProficiency(stack, proficiency); }

        @Override
        public void setMentalState(ItemStack stack, double value) { Population.setMentalState(stack, value); }
    };

    private Population() {}

    /**
     * 使用中心极限定理生成近似正态分布的寿命值。
     *
     * <p>均值 75，标准差约 5，范围 [30, 120]。
     * 12 个均匀分布的随机数之和近似正态分布（均值=6，标准差=1），
     * 经缩放和偏移后得到目标分布，超出边界时钳制。
     *
     * @param rand 随机数生成器
     * @return 30-120 范围内的近似正态分布寿命
     */
    public static int generateLifespan(RandomSource rand) {
        int mean = PopulationConfig.getLifespanMean();
        int min = PopulationConfig.getLifespanMin();
        int max = PopulationConfig.getLifespanMax();
        // 标准差取均值的约 1/15，保证生成值在合理范围内分散
        int stddev = Math.max(1, mean / 15);
        // 12 个 U(0,1) 之和 ≈ N(6, 1)（中心极限定理）
        double sum = 0.0;
        for (int i = 0; i < 12; i++) {
            sum += rand.nextDouble();
        }
        // (sum - 6) → N(0, 1)，× stddev → N(0, stddev)，+ mean → N(mean, stddev)
        int lifespan = mean + (int) Math.round((sum - 6.0) * stddev);
        return Mth.clamp(lifespan, min, max);
    }

    /**
     * 将原版村民转换为人口 ItemStack。
     * 读取村民的年龄、职业等属性，按配置生成随机的寿命、性别、生命值、饱食度等。
     * 幼年村民会得到儿童配置值，成年村民得到成人配置值。
     *
     * @param villager       原版村民实体
     * @param populationItem 人口物品类型
     * @return 填充了完整 NBT 属性的人口 ItemStack
     */
    static ItemStack fromVillager(Villager villager, PopulationItem populationItem) {
        ItemStack stack = new ItemStack(populationItem);
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        RandomSource rand = villager.getRandom();

        boolean isBaby = villager.isBaby();
        int age = isBaby
                ? Math.max(0, (int) ((villager.getAge() / (double) Villager.BABY_START_AGE) * 18))
                : PopulationConfig.getAdultAge();

        tag.putInt(TAG_AGE, age);
        tag.putInt(TAG_LIFESPAN, generateLifespan(rand));
        tag.putBoolean(TAG_GENDER, rand.nextBoolean());
        tag.putInt(TAG_HEALTH, isBaby
                ? rand.nextIntBetweenInclusive(PopulationConfig.getChildHealthMin(), PopulationConfig.getChildHealthMax())
                : rand.nextIntBetweenInclusive(PopulationConfig.getAdultHealthMin(), PopulationConfig.getAdultHealthMax()));
        tag.putInt(TAG_FOOD, isBaby
                ? rand.nextIntBetweenInclusive(PopulationConfig.getChildFoodMin(), PopulationConfig.getChildFoodMax())
                : rand.nextIntBetweenInclusive(PopulationConfig.getAdultFoodMin(), PopulationConfig.getAdultFoodMax()));
        tag.putString(TAG_CAREER, isBaby
                ? CareerNames.UNEMPLOYED
                : CivilizationAPI.getCareerRegistry().fromVanilla(villager.getVillagerData().getProfession()).getName());
        tag.putInt(TAG_PROFICIENCY, isBaby
                ? PopulationConfig.getChildProficiency()
                : PopulationConfig.getAdultProficiency());
        tag.putDouble(TAG_MENTAL_STATE, isBaby
                ? rand.nextDouble() * (PopulationConfig.getChildMentalStateMax() - PopulationConfig.getChildMentalStateMin()) + PopulationConfig.getChildMentalStateMin()
                : PopulationConfig.getAdultMentalState());

        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        // 立即根据实际属性计算工作效率，避免初始值为 0 导致机器无法工作
        recalcEfficiency(stack);
        return stack;
    }

    /**
     * 人口年龄 +1。由机器、学院等功能在每轮处理时调用。
     * 若年龄达到或超过寿命，则摧毁该物品（死亡），否则重算工作效率。
     *
     * @param stack 人口 ItemStack
     * @return true 表示人口存活并成功增加年龄，false 表示已死亡（物品已销毁）
     */
    static boolean addAge(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.isEmpty()) return false;

        int lifespan = tag.getInt(TAG_LIFESPAN);
        if (lifespan <= 0) return false;

        int newAge = tag.getInt(TAG_AGE) + 1;
        if (newAge > lifespan) {
            NeoForge.EVENT_BUS.post(new PopulationDeathEvent(stack));
            stack.setCount(0);
            return false;
        }

        tag.putInt(TAG_AGE, newAge);
        int health = tag.getInt(TAG_HEALTH);
        double mental = tag.getDouble(TAG_MENTAL_STATE);
        int proficiency = tag.getInt(TAG_PROFICIENCY);
        double eff = WorkEfficiency.calculate(newAge, health, mental, proficiency);
        tag.putDouble(TAG_WORK_EFFICIENCY, eff);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    /**
     * 判断人口是否已死亡。
     * @param stack 人口 ItemStack
     * @return true 表示年龄已达到或超过寿命上限
     */
    static boolean isDead(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.isEmpty()) return false;
        int lifespan = tag.getInt(TAG_LIFESPAN);
        if (lifespan <= 0) return false;
        return tag.getInt(TAG_AGE) >= lifespan;
    }

    /**
     * 根据当前 NBT 中的年龄、生命值、精神状态和熟练度重新计算工作效率并写入标签。
     * @param stack 人口 ItemStack
     */
    static void recalcEfficiency(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int age = tag.getInt(TAG_AGE);
        int health = tag.getInt(TAG_HEALTH);
        double mental = tag.getDouble(TAG_MENTAL_STATE);
        int proficiency = tag.getInt(TAG_PROFICIENCY);
        double eff = WorkEfficiency.calculate(age, health, mental, proficiency);
        tag.putDouble(TAG_WORK_EFFICIENCY, eff);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 设置生命值并自动重算工作效率。
     * @param stack  人口 ItemStack
     * @param health 生命值（0-100）
     */
    static void setHealth(ItemStack stack, int health) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG_HEALTH, health);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        recalcEfficiency(stack);
    }

    /**
     * 设置饱食度（不影响工作效率）。
     * @param stack 人口 ItemStack
     * @param food  饱食度（0-100）
     */
    static void setFood(ItemStack stack, int food) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG_FOOD, food);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 设置熟练度并自动重算工作效率。
     * @param stack       人口 ItemStack
     * @param proficiency 熟练度（默认 100）
     */
    static void setProficiency(ItemStack stack, int proficiency) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(TAG_PROFICIENCY, proficiency);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        recalcEfficiency(stack);
    }

    /**
     * 设置精神状态并自动重算工作效率。
     * @param stack 人口 ItemStack
     * @param value 精神状态（0.0-1.0）
     */
    static void setMentalState(ItemStack stack, double value) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putDouble(TAG_MENTAL_STATE, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        recalcEfficiency(stack);
    }
}

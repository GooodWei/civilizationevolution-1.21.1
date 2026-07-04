package com.gooodwei.civilizationevolution.api.util;

import com.gooodwei.civilizationevolution.api.CivilizationAPI;
import com.gooodwei.civilizationevolution.server.population.Population;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 人口物品 NBT 标签的读写工具类。
 * 提供所有人口属性的 get/set 静态方法，供 {@link IPopulationMachine} 及其他类调用。
 *
 * <p>所有方法均直接操作 ItemStack 的 {@code CUSTOM_DATA} 数据组件，
 * 不检查物品类型——调用方应确保传入的是 {@link com.gooodwei.civilizationevolution.server.item.PopulationItem}。
 *
 * <p>修改年龄、生命值、熟练度、精神状态后会自动重算工作效率。
 */
public final class PopulationNBT {

    private PopulationNBT() {}

    // ==================== Getter ====================

    /**
     * 获取人口年龄（单位：年）。
     * @param stack 人口物品
     * @return 当前年龄，默认 0
     */
    public static int getAge(ItemStack stack) {
        return getOrCopyTag(stack).getInt(Population.TAG_AGE);
    }

    /**
     * 获取人口寿命上限（单位：年）。
     * @param stack 人口物品
     * @return 寿命，默认 0
     */
    public static int getLifespan(ItemStack stack) {
        return getOrCopyTag(stack).getInt(Population.TAG_LIFESPAN);
    }

    /**
     * 获取人口性别。
     * @param stack 人口物品
     * @return true = 男性，false = 女性
     */
    public static boolean getGender(ItemStack stack) {
        return getOrCopyTag(stack).getBoolean(Population.TAG_GENDER);
    }

    /**
     * 获取人口职业名称（如 "armorer"、"farmer"）。
     * @param stack 人口物品
     * @return 职业名称，默认空字符串
     */
    public static String getCareer(ItemStack stack) {
        return getOrCopyTag(stack).getString(Population.TAG_CAREER);
    }

    /**
     * 获取人口生命值（0-100）。
     * @param stack 人口物品
     * @return 生命值，默认 0
     */
    public static int getHealth(ItemStack stack) {
        return getOrCopyTag(stack).getInt(Population.TAG_HEALTH);
    }

    /**
     * 获取人口饱食度（0-100）。
     * @param stack 人口物品
     * @return 饱食度，默认 0
     */
    public static int getFood(ItemStack stack) {
        return getOrCopyTag(stack).getInt(Population.TAG_FOOD);
    }

    /**
     * 获取人口熟练度。
     * @param stack 人口物品
     * @return 熟练度，默认 0
     */
    public static int getProficiency(ItemStack stack) {
        return getOrCopyTag(stack).getInt(Population.TAG_PROFICIENCY);
    }

    /**
     * 获取人口当前工作效率（综合计算后的值）。
     * @param stack 人口物品
     * @return 工作效率，默认 0.0
     */
    public static double getWorkEfficiency(ItemStack stack) {
        return getOrCopyTag(stack).getDouble(Population.TAG_WORK_EFFICIENCY);
    }

    /**
     * 获取人口精神状态（0.0-1.0）。
     * @param stack 人口物品
     * @return 精神状态，默认 0.0
     */
    public static double getMentalState(ItemStack stack) {
        return getOrCopyTag(stack).getDouble(Population.TAG_MENTAL_STATE);
    }

    // ==================== Setter ====================

    /**
     * 设置年龄（修改后自动重算工作效率）。
     * @param stack 人口物品
     * @param age   年龄（单位：年）
     */
    public static void setAge(ItemStack stack, int age) {
        CompoundTag tag = getOrCopyTag(stack);
        tag.putInt(Population.TAG_AGE, age);
        saveTag(stack, tag);
        recalcEfficiency(stack);
    }

    /**
     * 设置寿命上限（不影响工作效率）。
     * @param stack    人口物品
     * @param lifespan 寿命上限（单位：年）
     */
    public static void setLifespan(ItemStack stack, int lifespan) {
        CompoundTag tag = getOrCopyTag(stack);
        tag.putInt(Population.TAG_LIFESPAN, lifespan);
        saveTag(stack, tag);
    }

    /**
     * 设置性别（不影响工作效率）。
     * @param stack  人口物品
     * @param gender true = 男性，false = 女性
     */
    public static void setGender(ItemStack stack, boolean gender) {
        CompoundTag tag = getOrCopyTag(stack);
        tag.putBoolean(Population.TAG_GENDER, gender);
        saveTag(stack, tag);
    }

    /**
     * 设置职业名称（不影响工作效率）。
     * @param stack  人口物品
     * @param career 职业名称（如 "armorer"、"farmer"）
     */
    public static void setCareer(ItemStack stack, String career) {
        CompoundTag tag = getOrCopyTag(stack);
        tag.putString(Population.TAG_CAREER, career);
        saveTag(stack, tag);
    }

    /**
     * 设置生命值（修改后自动重算工作效率）。
     * @param stack  人口物品
     * @param health 生命值（0-100）
     */
    public static void setHealth(ItemStack stack, int health) {
        CompoundTag tag = getOrCopyTag(stack);
        tag.putInt(Population.TAG_HEALTH, health);
        saveTag(stack, tag);
        recalcEfficiency(stack);
    }

    /**
     * 设置饱食度（不影响工作效率）。
     * @param stack 人口物品
     * @param food  饱食度（0-100）
     */
    public static void setFood(ItemStack stack, int food) {
        CompoundTag tag = getOrCopyTag(stack);
        tag.putInt(Population.TAG_FOOD, food);
        saveTag(stack, tag);
    }

    /**
     * 设置熟练度（修改后自动重算工作效率）。
     * @param stack       人口物品
     * @param proficiency 熟练度（默认 100）
     */
    public static void setProficiency(ItemStack stack, int proficiency) {
        CompoundTag tag = getOrCopyTag(stack);
        tag.putInt(Population.TAG_PROFICIENCY, proficiency);
        saveTag(stack, tag);
        recalcEfficiency(stack);
    }

    /**
     * 设置工作效率（直接写入，不触发重算）。
     * 通常在已知各因子值时直接调用此方法以避免重复计算。
     * @param stack 人口物品
     * @param value 工作效率值
     */
    public static void setWorkEfficiency(ItemStack stack, double value) {
        CompoundTag tag = getOrCopyTag(stack);
        tag.putDouble(Population.TAG_WORK_EFFICIENCY, value);
        saveTag(stack, tag);
    }

    /**
     * 设置精神状态（修改后自动重算工作效率）。
     * @param stack 人口物品
     * @param value 精神状态（0.0-1.0）
     */
    public static void setMentalState(ItemStack stack, double value) {
        CompoundTag tag = getOrCopyTag(stack);
        tag.putDouble(Population.TAG_MENTAL_STATE, value);
        saveTag(stack, tag);
        recalcEfficiency(stack);
    }

    /**
     * 获取人口所有职业的学徒经验映射。
     * 返回的 CompoundTag 键为职业名称，值为经验值（int）。
     * 若无学徒经验数据则返回空 CompoundTag。
     *
     * @param stack 人口物品
     * @return 职业经验 CompoundTag，永不为 null
     */
    public static CompoundTag getAllCareerExps(ItemStack stack) {
        return getOrCopyTag(stack).getCompound(Population.TAG_CAREER_EXPS);
    }

    /**
     * 获取人口在指定职业上的经验值。
     *
     * @param stack      人口物品
     * @param careerName 职业名称（如 "farmer"）
     * @return 该职业的经验值，默认 0
     */
    public static int getCareerExp(ItemStack stack, String careerName) {
        CompoundTag exps = getOrCopyTag(stack).getCompound(Population.TAG_CAREER_EXPS);
        return exps.getInt(careerName);
    }

    /**
     * 设置人口在指定职业上的经验值。
     *
     * @param stack      人口物品
     * @param careerName 职业名称（如 "farmer"）
     * @param exp        经验值
     */
    public static void setCareerExp(ItemStack stack, String careerName, int exp) {
        CompoundTag tag = getOrCopyTag(stack);
        CompoundTag exps = tag.getCompound(Population.TAG_CAREER_EXPS);
        if (exp <= 0 && !exps.contains(careerName)) {
            return; // 无需写入零值
        }
        exps.putInt(careerName, exp);
        tag.put(Population.TAG_CAREER_EXPS, exps);
        saveTag(stack, tag);
    }

    /**
     * 增加人口在指定职业上的经验值，返回增加后的值。
     *
     * @param stack      人口物品
     * @param careerName 职业名称（如 "farmer"）
     * @param amount     增加量（通常为 1）
     * @return 增加后的经验值
     */
    public static int addCareerExp(ItemStack stack, String careerName, int amount) {
        CompoundTag tag = getOrCopyTag(stack);
        CompoundTag exps = tag.getCompound(Population.TAG_CAREER_EXPS);
        int newExp = exps.getInt(careerName) + amount;
        exps.putInt(careerName, newExp);
        tag.put(Population.TAG_CAREER_EXPS, exps);
        saveTag(stack, tag);
        return newExp;
    }

    /**
     * 判断人口在指定职业上的经验是否达到阈值。
     *
     * @param stack      人口物品
     * @param careerName 职业名称
     * @param threshold  阈值
     * @return true 表示经验 ≥ 阈值
     */
    public static boolean hasCareerExpReached(ItemStack stack, String careerName, int threshold) {
        return getCareerExp(stack, careerName) >= threshold;
    }

    /**
     * 清除人口的所有职业学徒经验，减少 NBT 膨胀。
     * 转职完成后调用此方法以清空不再需要的经验数据。
     *
     * @param stack 人口物品
     */
    public static void clearAllCareerExps(ItemStack stack) {
        CompoundTag tag = getOrCopyTag(stack);
        tag.remove(Population.TAG_CAREER_EXPS);
        saveTag(stack, tag);
    }

    // ==================== 判定 ====================

    /**
     * 判断人口是否已被标记为死亡（检测 dead 布尔标签）。
     * @param stack 人口物品
     * @return true 表示已标记为死亡
     */
    public static boolean isDead(ItemStack stack) {
        return getOrCopyTag(stack).getBoolean(Population.TAG_DEAD);
    }

    /**
     * 将该人口标记为死亡（设置 dead 标签为 true）。
     * 注意：此方法不会销毁物品，仅设置 NBT 标记。
     * @param stack 人口物品
     */
    public static void markDead(ItemStack stack) {
        CompoundTag tag = getOrCopyTag(stack);
        tag.putBoolean(Population.TAG_DEAD, true);
        saveTag(stack, tag);
    }

    // ==================== 内部 ====================

    /** 从物品获取 NBT 标签的可修改副本（用于 Getter 和 Setter） */
    private static CompoundTag getOrCopyTag(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    /** 将修改后的 NBT 标签写回物品的 CUSTOM_DATA 数据组件 */
    private static void saveTag(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** 通过 CivilizationAPI 委托重新计算并写入工作效率 */
    private static void recalcEfficiency(ItemStack stack) {
        CivilizationAPI.getPopulationManager().recalcEfficiency(stack);
    }
}

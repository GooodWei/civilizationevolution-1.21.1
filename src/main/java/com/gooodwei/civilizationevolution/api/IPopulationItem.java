package com.gooodwei.civilizationevolution.api;

import com.gooodwei.civilizationevolution.api.career.Career;
import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import net.minecraft.world.item.ItemStack;

/**
 * 人口物品接口 —— 提供学徒经验管理的核心方法。
 *
 * <p>所有人口物品（{@link com.gooodwei.civilizationevolution.server.item.PopulationItem}）
 * 都应实现此接口。机器通过调用 {@link #addApprenticeExp(ItemStack, String, int)}
 * 委托学徒逻辑，无需关心父职业门控、经验累积、阈值检查、转职等内部细节。
 *
 * <p>未来非机器场景（村民交互、命令等）也可复用此方法。
 */
public interface IPopulationItem {

    /**
     * 给该人口累加目标职业的学徒经验，达标后自动转职。
     *
     * <p>此方法包含完整的三步逻辑：
     * <ol>
     *   <li><b>父职业门控</b>：当前职业必须是目标职业的父职业，否则直接返回</li>
     *   <li><b>累加经验</b>：通过 {@link PopulationNBT#addCareerExp} 累加经验值</li>
     *   <li><b>阈值检查转职</b>：经验 ≥ 阈值时，设置职业为目标职业并清除全部学徒经验</li>
     * </ol>
     *
     * <p>机器只需调用此方法，无需关心内部晋升逻辑。
     *
     * @param stack        人口物品
     * @param targetCareer 目标职业名称（如 "farmer"、"butcher"）
     * @param amount       本次增加的经验量（由机器的 apprentice_exp_per_cycle 决定）
     */
    default void addApprenticeExp(ItemStack stack, String targetCareer, int amount) {
        if (PopulationNBT.isDead(stack)) return;

        Career target = Career.byName(targetCareer);
        if (target == null) return;

        int threshold = target.getApprenticeExpThreshold();
        if (threshold <= 0) return; // ≤0 表示不可晋升

        // 1. 父职业门控：当前职业必须是目标职业的父职业
        String currentCareer = PopulationNBT.getCareer(stack);
        String requiredParent = target.getParentCareerName();
        if (requiredParent == null || !requiredParent.equals(currentCareer)) {
            return;
        }

        // 2. 累加经验
        int newExp = PopulationNBT.addCareerExp(stack, targetCareer, amount);

        // 3. 达标 → 转职 + 清除所有学徒经验
        if (newExp >= threshold) {
            PopulationNBT.setCareer(stack, targetCareer);
            PopulationNBT.clearAllCareerExps(stack);
        }
    }
}

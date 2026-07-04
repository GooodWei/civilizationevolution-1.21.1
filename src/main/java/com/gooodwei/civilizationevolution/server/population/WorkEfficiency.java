package com.gooodwei.civilizationevolution.server.population;

import com.gooodwei.civilizationevolution.api.IWorkEfficiencyCalculator;
import com.gooodwei.civilizationevolution.server.config.PopulationConfig;

/**
 * 根据四个因素计算人口工作效率：
 * 年龄、生命值、精神状态和熟练度。
 *
 * <p>年龄阈值引用 {@link PopulationConfig}：
 * 0~ADULT_AGE 线性增长，ADULT_AGE~RETIREMENT_AGE 保持 1.0，
 * RETIREMENT_AGE~MAX_WORK_AGE 线性衰减，MAX_WORK_AGE+ 为 0。
 *
 * <p>基准值（返回 1.0）：
 *   age=18~65, health=50, mentalState=0.5, proficiency=100
 */
public final class WorkEfficiency {

    /** IWorkEfficiencyCalculator 接口的单例实例，供 CivilizationAPI 暴露 */
    public static final IWorkEfficiencyCalculator INSTANCE = new IWorkEfficiencyCalculator() {
        @Override
        public double calculate(int age, int health, double mentalState, int proficiency) {
            return WorkEfficiency.calculate(age, health, mentalState, proficiency);
        }

        @Override
        public double ageFactor(int age) { return WorkEfficiency.ageFactor(age); }

        @Override
        public double healthFactor(int health) { return WorkEfficiency.healthFactor(health); }

        @Override
        public double mentalFactor(double mentalState) { return WorkEfficiency.mentalFactor(mentalState); }

        @Override
        public double proficiencyFactor(int proficiency) { return WorkEfficiency.proficiencyFactor(proficiency); }
    };

    private WorkEfficiency() {}

    /**
     * 计算工作效率。
     *
     * @param age          当前年龄（年）
     * @param health       生命值 0–100
     * @param mentalState  精神状态 0.0–1.0
     * @param proficiency  熟练度，默认 100，无上限
     * @return 工作效率（最低 0.0，无上限）
     */
    static double calculate(int age, int health, double mentalState, int proficiency) {
        return ageFactor(age) * healthFactor(health) * mentalFactor(mentalState) * proficiencyFactor(proficiency);
    }

    /**
     * 年龄因子：0~ADULT_AGE 线性 0→1，ADULT_AGE~RETIREMENT_AGE 恒为 1，
     * RETIREMENT_AGE~MAX_WORK_AGE 线性 1→0，MAX_WORK_AGE+ 为 0。
     *
     * @param age 当前年龄（年）
     * @return 年龄因子（0.0-1.0）
     */
    static double ageFactor(int age) {
        int adult = PopulationConfig.ADULT_AGE;
        int retire = PopulationConfig.RETIREMENT_AGE;
        int maxWork = PopulationConfig.MAX_WORK_AGE;
        if (age < adult) {
            return (double) age / adult;
        } else if (age <= retire) {
            return 1.0;
        } else if (age < maxWork) {
            return 1.0 - (double) (age - retire) / (maxWork - retire);
        } else {
            return 0.0;
        }
    }

    /**
     * 生命值因子：health / 100，生命值越高工作效率越高。
     * @param health 生命值（0-100）
     * @return 生命值因子（0.0-1.0）
     */
    static double healthFactor(int health) {
        return health / 100.0;
    }

    /**
     * 精神状态因子：mentalState × 2，精神状态越高工作效率越高。
     * @param mentalState 精神状态（0.0-1.0）
     * @return 精神状态因子（0.0-2.0）
     */
    static double mentalFactor(double mentalState) {
        return mentalState * 2.0;
    }

    /**
     * 熟练度因子：1 + log₂(proficiency / 100)，下限 0.01。
     * 熟练度越高，因子增长越缓慢（对数衰减）。
     * @param proficiency 熟练度，默认 100
     * @return 熟练度因子（最低 0.01，无上限）
     */
    static double proficiencyFactor(int proficiency) {
        double raw = 1.0 + Math.log(proficiency / 100.0) / Math.log(2);
        return Math.max(0.01, raw);
    }
}

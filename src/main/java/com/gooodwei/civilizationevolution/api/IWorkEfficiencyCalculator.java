package com.gooodwei.civilizationevolution.api;

/**
 * 工作效率计算接口。
 * 工作效率 = ageFactor × healthFactor × mentalFactor × proficiencyFactor。
 * 附属模组通过 {@link CivilizationAPI#getWorkEfficiencyCalculator()} 获取实例，
 * 或单独调用各因子方法自定义组合。
 */
public interface IWorkEfficiencyCalculator {

    /**
     * 综合计算工作效率（四个因子的乘积）。
     * @param age          当前年龄（年）
     * @param health       生命值（0-100）
     * @param mentalState  精神状态（0.0-1.0）
     * @param proficiency  熟练度，默认 100
     * @return 综合工作效率（最低 0.0）
     */
    double calculate(int age, int health, double mentalState, int proficiency);

    /**
     * 年龄因子：0-17 线性 0→1，18-65 恒为 1，66-84 线性 1→0，85+ 为 0。
     * @param age 当前年龄（年）
     * @return 年龄因子（0.0-1.0）
     */
    double ageFactor(int age);

    /**
     * 生命值因子：health / 100，生命值越高效率越高。
     * @param health 生命值（0-100）
     * @return 生命值因子（0.0-1.0）
     */
    double healthFactor(int health);

    /**
     * 精神状态因子：mentalState × 2，精神状态越高效率越高。
     * @param mentalState 精神状态（0.0-1.0）
     * @return 精神状态因子（0.0-2.0）
     */
    double mentalFactor(double mentalState);

    /**
     * 熟练度因子：1 + log₂(proficiency / 100)，下限 0.01。
     * 熟练度越高，因子增长越缓慢（对数衰减）。
     * @param proficiency 熟练度，默认 100
     * @return 熟练度因子（最低 0.01）
     */
    double proficiencyFactor(int proficiency);
}

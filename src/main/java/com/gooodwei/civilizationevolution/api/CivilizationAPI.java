package com.gooodwei.civilizationevolution.api;

import com.gooodwei.civilizationevolution.api.career.Career;
import com.gooodwei.civilizationevolution.server.population.Population;
import com.gooodwei.civilizationevolution.server.population.WorkEfficiency;

/**
 * CivilizationEvolution 模组的统一 API 入口。
 * 附属模组通过此类获取各功能接口的实例。
 *
 * <pre>{@code
 * // 使用示例
 * IPopulationManager pm = CivilizationAPI.getPopulationManager();
 * pm.addAge(stack);
 *
 * ICareerRegistry cr = CivilizationAPI.getCareerRegistry();
 * Career c = cr.byName("armorer");
 * }</pre>
 */
public final class CivilizationAPI {

    private CivilizationAPI() {}

    /** 获取人口管理器实例，用于创建、操作和查询人口物品 */
    public static IPopulationManager getPopulationManager() {
        return Population.INSTANCE;
    }

    /** 获取职业注册表实例，用于按名称查找职业或遍历所有职业 */
    public static ICareerRegistry getCareerRegistry() {
        return Career.REGISTRY_INSTANCE;
    }

    /** 获取工作效率计算器实例，用于按属性因子计算综合工作效率 */
    public static IWorkEfficiencyCalculator getWorkEfficiencyCalculator() {
        return WorkEfficiency.INSTANCE;
    }
}

package com.gooodwei.civilizationevolution.api.career;

/**
 * 所有已注册职业名称的字符串常量。
 *
 * <p>使用常量替代硬编码字符串，IDE 可自动补全、编译期发现拼写错误。
 * 新增职业时在此处添加对应常量。
 *
 * <h3>用法示例</h3>
 * <pre>{@code
 * // 之前：硬编码字符串，拼写错误编译不报错
 * protected String getWorkerCareer() { return "farmer"; }
 *
 * // 之后：使用常量，IDE 自动补全，拼写错误编译报错
 * protected String getWorkerCareer() { return CareerNames.FARMER; }
 * }</pre>
 *
 * <h3>职业树结构</h3>
 * <pre>
 * unemployed (根职业，Tier 0，不可晋升)
 * ├── armorer, butcher, cartographer, cleric, farmer, fisherman,
 * │   fletcher, leatherworker, librarian, mason, shepherd, toolsmith,
 * │   weaponsmith (Tier 1，父=unemployed)
 * └── (miner 的祖先)
 *     └── mason
 *         └── miner (Tier 2，父=mason)
 *
 * nitwit (根职业，Tier 0，不可晋升，无父职业)
 * </pre>
 *
 * @see Career
 */
public final class CareerNames {

    private CareerNames() {
        throw new UnsupportedOperationException("常量类，不可实例化");
    }

    // ==================== 根职业（Tier 0） ====================

    /** 无业 —— 所有 Tier 1 职业的根父职业，也是婴儿的初始职业 */
    public static final String UNEMPLOYED = "unemployed";

    /** 傻子 —— 独立根职业，无父职业，不可工作/晋升 */
    public static final String NITWIT = "nitwit";

    // ==================== Tier 1 职业（父=unemployed） ====================

    public static final String ARMORER       = "armorer";
    public static final String BUTCHER       = "butcher";
    public static final String CARTOGRAPHER  = "cartographer";
    public static final String CLERIC        = "cleric";
    public static final String FARMER        = "farmer";
    public static final String FISHERMAN     = "fisherman";
    public static final String FLETCHER      = "fletcher";
    public static final String LEATHERWORKER = "leatherworker";
    public static final String LIBRARIAN     = "librarian";
    public static final String MASON         = "mason";
    public static final String SHEPHERD      = "shepherd";
    public static final String TOOLSMITH     = "toolsmith";
    public static final String WEAPONSMITH   = "weaponsmith";

    // ==================== Tier 2 职业 ====================

    /** 矿工 —— 父=mason，Tier 2，晋升阈值 12 */
    public static final String MINER = "miner";
}

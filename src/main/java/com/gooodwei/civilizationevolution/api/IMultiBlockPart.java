package com.gooodwei.civilizationevolution.api;

import com.gooodwei.civilizationevolution.api.tier.Tier;

/**
 * 多方块结构零件接口。
 *
 * <p>所有参与多方块机器结构的方块（外壳、输入/输出接口等）必须实现此接口。
 * 零件的成型状态和控制器坐标由控制器 BE 统一追踪，
 * Block 是单例，不能存储位置相关状态。
 *
 * <p>用法：
 * <ul>
 *   <li>{@link #getPartTier()} 返回此零件的 Tier 等级</li>
 *   <li>{@link #getPartType()} 返回此零件的类型（用于多方块结构 JSON 的 alternatives 匹配）</li>
 * </ul>
 */
public interface IMultiBlockPart {

    // ==================== 零件类型常量 ====================

    /** 普通结构外壳方块 */
    String TYPE_MULTI_BLOCK_PART = "multi_block_part";
    /** 人口输入接口 */
    String TYPE_INPUT_HATCH = "input_hatch";
    /** 人口输出接口 */
    String TYPE_OUTPUT_HATCH = "output_hatch";
    /** 食物输入接口 */
    String TYPE_FOOD_HATCH = "food_hatch";
    /** 通用流体接口（向后兼容） */
    String TYPE_FLUID_HATCH = "fluid_hatch";
    /** 流体输入接口 */
    String TYPE_FLUID_INPUT_HATCH = "fluid_input_hatch";
    /** 流体输出接口 */
    String TYPE_FLUID_OUTPUT_HATCH = "fluid_output_hatch";

    // ==================== 抽象/默认方法 ====================

    /**
     * 此结构零件的 Tier 等级。
     *
     * <p>多方块成型验证时，控制器会检查每个零件：
     * {@code part.getPartTier().getLevel() <= controller.getTier().getLevel()}
     *
     * @return 此零件的 Tier 等级
     */
    Tier getPartTier();

    /**
     * 此结构零件的类型标识。
     *
     * <p>用于多方块结构 JSON 中 alternatives 匹配。
     * 默认返回 {@link #TYPE_MULTI_BLOCK_PART}（普通外壳方块），
     * hatch 子类应覆写返回对应的类型常量。
     *
     * @return 零件类型字符串（如 {@code "input_hatch"}）
     */
    default String getPartType() {
        return TYPE_MULTI_BLOCK_PART;
    }
}

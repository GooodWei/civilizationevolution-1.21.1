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
 * </ul>
 */
public interface IMultiBlockPart {

    /**
     * 此结构零件的 Tier 等级。
     *
     * <p>多方块成型验证时，控制器会检查每个零件：
     * {@code part.getPartTier().getLevel() <= controller.getTier().getLevel()}
     *
     * @return 此零件的 Tier 等级
     */
    Tier getPartTier();
}

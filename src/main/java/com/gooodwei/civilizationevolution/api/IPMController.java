package com.gooodwei.civilizationevolution.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.level.Level;

/**
 * 人口机器控制器接口 —— 绑定并统一调度 {@link IPopulationMachine} 的工作。
 *
 * <p>控制器负责：
 * <ul>
 *   <li><b>绑定管理</b> —— 绑定/解绑机器，维护绑定表</li>
 *   <li><b>启停控制</b> —— 单独启用/禁用某台机器的调度（不改变绑定关系）</li>
 *   <li><b>工作调度</b> —— 在自身的 tick 中以自定义节奏触发被绑定机器的
 *       {@link IPopulationMachine#executeWorkCycle(Level)}</li>
 * </ul>
 *
 * <p>典型实现：原始聚落（{@code PrimitiveSettlementBlockEntity}），
 * 使用 0–24000 的日晷进度，按每台机器的 {@code workTotalTime} 计算触发点。
 *
 * <p>未来可扩展其他调度模型（固定间隔、事件触发、分时轮转等），
 * 只需实现本接口并覆写 {@link #tick(Level)}。
 *
 * @see IPopulationMachine
 */
public interface IPMController {

    /** 返回控制器自身的 Container（通常直接返回 this） */
    Container getContainer();

    /** 最大可绑定机器数量 */
    int getMaxBindCount();

    /**
     * 此控制器的 Tier 等级。
     *
     * <p>控制器只能绑定 tier ≤ 自身 tier 的机器。
     * 附属模组实现此接口时可自定义控制器等级。
     *
     * @return Tier 等级（0 = 原始聚落，1 = 村庄，2+ = 更高级）
     */
    com.gooodwei.civilizationevolution.api.tier.Tier getTier();

    /** 当前已绑定机器数量 */
    int getBoundMachineCount();

    /**
     * 检查指定坐标的机器是否已绑定到此控制器。
     */
    boolean isMachineBound(BlockPos pos);

    /**
     * 绑定一台人口机器到此控制器。
     *
     * <p>实现应完成以下操作：
     * <ol>
     *   <li>验证绑定数量未超上限</li>
     *   <li>验证该坐标未被其他控制器绑定</li>
     *   <li>调用 {@code machine.setBound(true)} 标记目标机器</li>
     *   <li>将绑定信息存入控制器自身的绑定表</li>
     * </ol>
     *
     * @param pos     目标机器坐标
     * @param machine 目标机器实例
     * @return true 绑定成功，false 失败（已满、已绑定、超出距离等）
     */
    boolean bindMachine(BlockPos pos, IPopulationMachine machine);

    /**
     * 解绑指定坐标的机器。
     *
     * <p>实现应完成以下操作：
     * <ol>
     *   <li>调用 {@code machine.setBound(false)} 恢复目标机器独立状态</li>
     *   <li>从绑定表中移除该条目</li>
     * </ol>
     *
     * @param pos   目标机器坐标
     * @param level 当前世界（用于获取目标 BlockEntity）
     */
    void unbindMachine(BlockPos pos, Level level);

    /**
     * 启用或禁用指定机器的调度。
     * <p>禁用后机器仍保持绑定关系，但 tick 中跳过其工作触发。
     * 重新启用后机器按原触发规则继续调度。
     *
     * @param pos     目标机器坐标
     * @param enabled true 启用，false 暂停
     */
    void setMachineEnabled(BlockPos pos, boolean enabled);

    /**
     * 每 tick 由控制器方块实体的 serverTick 调用。
     * <p>控制器在此方法中遍历绑定表、检查触发条件、
     * 调用 {@link IPopulationMachine#executeWorkCycle(Level)} 推进工作。
     *
     * @param level 当前世界（服务端）
     */
    void tick(Level level);
}

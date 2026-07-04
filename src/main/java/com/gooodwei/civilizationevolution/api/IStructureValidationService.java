package com.gooodwei.civilizationevolution.api;

import net.minecraft.server.level.ServerLevel;

/**
 * 多方块结构验证服务接口 —— 解耦 API 层与 Server 层的直接依赖。
 *
 * <p>定义在 api/ 包中，使 {@link IMultiBlockMachine#tickRevalidation()} 可以通过
 * 此接口调用验证服务，而非硬引用 {@code StructureValidationService}。
 *
 * <p>实现类在服务端初始化时通过 {@link IMultiBlockMachine#VALIDATION_SERVICE} 注册。
 *
 * @see IMultiBlockMachine
 */
@FunctionalInterface
public interface IStructureValidationService {

    /**
     * 提交定时验证任务到后台线程。
     *
     * @param machine 待验证的多方块机器
     * @param level   服务端世界
     * @return true 表示已提交，false 表示跳过（已有验证进行中）
     */
    boolean submitPeriodicValidation(IMultiBlockMachine machine, ServerLevel level);
}

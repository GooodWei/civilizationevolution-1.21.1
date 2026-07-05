package com.gooodwei.civilizationevolution.api;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.Optional;

/**
 * 自适应轮询配方机 —— 根据机器活跃度调整配方检测频率。
 *
 * <p>热状态（活跃）：每 tick 检测配方
 * <p>冷状态（闲置）：每 2 秒检测一次配方
 * <p>打开 GUI 或匹配到配方 → 立即回到热状态
 *
 * @param <T> 配方类型
 * @param <I> 配方输入类型
 */
public interface IAdaptivePollingMachine<T extends Recipe<I>, I extends RecipeInput> {
    /** 热状态超时阈值（tick），超过后进入冷状态 */
    int HOT_IDLE_TIMEOUT = 320;
    /** 冷状态下配方检测间隔（tick） */
    int COLD_CHECK_INTERVAL = 40;

    // 配方
    Optional<T> findAndValidateRecipe();
    void executeRecipe(T recipe);
    int getRecipeWorkProgress();
    void resetWorkProgress();
    int getMachineTier();
    // 状态
    int getIdleTicks();
    void setIdleTicks(int ticks);
    // 前置条件
    boolean isMachineReady(); // 结构成型 + 已绑定
    /**
     * 每 tick 调用（由 BE 的 {@code serverTick} 委托）。
     *
     * <p>热状态：每 tick 检测并推进配方；
     * 冷状态：每 {@value #COLD_CHECK_INTERVAL} tick 检测一次。
     */
    default void tickAdaptivePolling() {
        if (!isMachineReady()) return;

        int idle = getIdleTicks();
        boolean isCold = idle >= HOT_IDLE_TIMEOUT;

        // 冷状态下按间隔检测，热状态下每 tick 检测
        if (isCold && idle % COLD_CHECK_INTERVAL != 0) {
            setIdleTicks(idle + 1);
            return;
        }

        Optional<T> recipe = findAndValidateRecipe();
        if (recipe.isPresent()) {
            // 匹配到配方 → 重置空闲计时器，回到热状态
            setIdleTicks(0);
            executeRecipe(recipe.get());
        } else {
            // 无配方 → 累加空闲计时
            setIdleTicks(idle + 1);
        }
    }

    /** 玩家打开 GUI 时调用：立即回到热状态 */
    default void onGuiOpened() {
        setIdleTicks(0);
    }
}

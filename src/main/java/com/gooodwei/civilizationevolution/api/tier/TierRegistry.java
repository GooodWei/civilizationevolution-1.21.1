package com.gooodwei.civilizationevolution.api.tier;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tier 等级注册表 —— 所有 Tier 的统一管理中心。
 *
 * <p>使用方式：
 * <ol>
 *   <li>模组初始化阶段调用 {@link #register(ResourceLocation, Tier)} 注册 Tier</li>
 *   <li>所有注册完成后调用 {@link #freeze()} 冻结注册表</li>
 *   <li>运行时通过 {@link #get(ResourceLocation)} / {@link #getByLevel(int)} 查找 Tier</li>
 * </ol>
 *
 * <p>内置 Tier 在 {@code CivilizationEvolution} 构造函数中注册，
 * 附属模组也可在自己的 {@code @Mod} 构造函数中注册新 Tier
 * （必须在 {@code freeze()} 之前）。
 *
 * <p>注册表使用 {@link LinkedHashMap} 保持注册顺序。
 */
public final class TierRegistry {

    /** 内部注册表（ID → Tier），LinkedHashMap 保持注册顺序 */
    private static final Map<ResourceLocation, Tier> REGISTRY = new LinkedHashMap<>();

    /** 是否已冻结 */
    private static boolean frozen = false;

    private TierRegistry() {}

    // ==================== 注册 ====================

    /**
     * 注册一个新 Tier。
     *
     * <p>调用时机：
     * <ul>
     *   <li>本模组：{@code CivilizationEvolution} 构造函数中</li>
     *   <li>附属模组：自身 {@code @Mod} 构造函数中（必须在 {@code freeze()} 之前）</li>
     * </ul>
     *
     * <p>ID 建议格式：{@code 模组id:tier名称}（如 "civilizationevolution:primitive"）。
     *
     * @param id   唯一标识符（不区分大小写，建议全小写）
     * @param tier Tier 实例
     * @return 注册的 Tier（链式调用用）
     * @throws IllegalStateException 注册表已冻结后调用
     * @throws IllegalArgumentException ID 已存在时抛出
     */
    public static Tier register(ResourceLocation id, Tier tier) {
        if (frozen) {
            throw new IllegalStateException(
                    "TierRegistry 已被冻结，无法注册新 Tier: " + id);
        }
        if (REGISTRY.containsKey(id)) {
            throw new IllegalArgumentException(
                    "Tier ID 已存在: " + id + " → " + REGISTRY.get(id));
        }
        REGISTRY.put(id, tier);
        return tier;
    }

    // ==================== 查找 ====================

    /**
     * 通过 ResourceLocation ID 查找 Tier。
     *
     * @param id 注册时的唯一标识符
     * @return 对应的 Tier，未找到时返回 {@code null}
     */
    public static Tier get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    /**
     * 通过数字等级查找 Tier（返回第一个匹配的）。
     * <p>注意：如果有多个 Tier 具有相同的 level，此方法返回注册顺序中最早的一个。
     *
     * @param level 数字等级
     * @return 对应的 Tier，未找到时返回 {@code null}
     */
    public static Tier getByLevel(int level) {
        for (Tier tier : REGISTRY.values()) {
            if (tier.getLevel() == level) {
                return tier;
            }
        }
        return null;
    }

    /**
     * 获取注册表只读视图。
     *
     * @return 所有已注册的 Tier（按注册顺序）
     */
    public static Collection<Tier> getAll() {
        return Collections.unmodifiableCollection(REGISTRY.values());
    }

    // ==================== 生命周期 ====================

    /**
     * 冻结注册表，此后调用 {@link #register} 将抛出异常。
     * <p>应在所有模组初始化完成后调用。
     */
    public static void freeze() {
        frozen = true;
    }

    /** @return 注册表是否已冻结 */
    public static boolean isFrozen() {
        return frozen;
    }

    /** @return 已注册的 Tier 数量 */
    public static int size() {
        return REGISTRY.size();
    }
}

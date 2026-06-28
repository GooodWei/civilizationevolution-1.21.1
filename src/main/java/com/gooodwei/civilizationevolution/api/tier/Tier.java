package com.gooodwei.civilizationevolution.api.tier;

import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * 文明演进 Tier 等级数据类。
 *
 * <p>每个 Tier 代表一个科技/文明阶段，拥有数字等级和可翻译的显示名称。
 * 通过 {@link TierRegistry} 注册和管理。
 *
 * <p>Tier 比较基于 {@link #level} 数值：
 * <ul>
 *   <li>level 越大，Tier 越高</li>
 *   <li>控制器只能绑定 tier ≤ 自身 tier 的机器</li>
 * </ul>
 *
 * <p>附属模组通过 {@code TierRegistry.register(id, new Tier(level, translationKey))}
 * 注册自定义 Tier。
 *
 * @see TierRegistry
 */
public final class Tier implements Comparable<Tier> {

    /** 数字等级（0 = 原始时代，1 = 村庄时代，以此类推） */
    private final int level;

    /** 翻译键（如 "tier.civilizationevolution.primitive"） */
    private final String translationKey;

    /**
     * 创建一个 Tier。
     *
     * @param level          数字等级
     * @param translationKey i18n 翻译键
     */
    public Tier(int level, String translationKey) {
        this.level = level;
        this.translationKey = translationKey;
    }

    /** @return 数字等级 */
    public int getLevel() {
        return level;
    }

    /** @return i18n 翻译键 */
    public String getTranslationKey() {
        return translationKey;
    }

    /** @return 用于 GUI/tooltip 显示的 Component */
    public Component getDisplayName() {
        return Component.translatable(translationKey);
    }

    // ==================== 比较 ====================

    @Override
    public int compareTo(Tier other) {
        return Integer.compare(this.level, other.level);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Tier other)) return false;
        return this.level == other.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(level);
    }

    @Override
    public String toString() {
        return "Tier{level=" + level + ", key='" + translationKey + "'}";
    }
}

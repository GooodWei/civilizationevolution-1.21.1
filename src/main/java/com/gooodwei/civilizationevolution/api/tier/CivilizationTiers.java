package com.gooodwei.civilizationevolution.api.tier;

import net.minecraft.resources.ResourceLocation;

/**
 * 本模组内置的 Tier 等级常量。
 *
 * <p>附属模组可通过 {@code ModTiers.PRIMITIVE}、{@code ModTiers.VILLAGE}
 * 直接引用本模组已注册的 Tier，无需通过 {@link TierRegistry#getByLevel(int)} 查找。
 *
 * <p>所有内置 Tier 在类加载时自动注册到 {@link TierRegistry}。
 * {@code CivilizationEvolution} 构造器中调用 {@link #init()} 触发类加载，
 * 确保在 {@link TierRegistry#freeze()} 之前完成注册。
 *
 * <p>使用示例：
 * <pre>{@code
 * // 附属模组引用本模组已定义的 Tier
 * public class MyMachine extends AbstractCampBlockEntity {
 *     Override public Tier getTier() { return ModTiers.VILLAGE; }
 * }
 * }</pre>
 */
public final class CivilizationTiers {

    /** Tier 0：原始时代 */
    public static final Tier PRIMITIVE = TierRegistry.register(
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "primitive"),
            new Tier(0, "tier.civilizationevolution.primitive"));

    /** Tier 1：村庄时代 */
    public static final Tier VILLAGE = TierRegistry.register(
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "village"),
            new Tier(1, "tier.civilizationevolution.village"));

    private CivilizationTiers() {}

    /**
     * 触发类加载，确保所有内置 Tier 已注册到 {@link TierRegistry}。
     * 在 {@code CivilizationEvolution} 构造器中、{@code TierRegistry.freeze()} 之前调用。
     */
    public static void init() {
        // 类加载即完成注册，无需额外操作
    }
}

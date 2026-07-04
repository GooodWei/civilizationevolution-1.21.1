package com.gooodwei.civilizationevolution.api.career;

import com.gooodwei.civilizationevolution.api.CivilizationAPI;
import com.gooodwei.civilizationevolution.api.ICareerRegistry;
import com.gooodwei.civilizationevolution.api.event.CareerRegisterEvent;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.neoforge.common.NeoForge;

import javax.annotation.Nullable;
import java.util.*;

/**
 * CivilizationEvolution 中所有职业的基类。
 * 每个初始职业在 {@code career/initial/} 下都有其自身的子类。
 * 未来的等级升级声明为其父职业的内部类。
 */
public abstract class Career {

    /**
     * 职业注册表，按名称映射到职业实例。
     * 附属模组应通过 {@link ICareerRegistry} 接口访问，避免直接操作此 Map。
     */
    static final Map<String, Career> REGISTRY = new LinkedHashMap<>();

    /** 自动递增的模型索引计数器，用于为每个职业分配唯一的 ItemProperty 覆盖值 */
    private static int nextModelIndex = 0;

    /** {@link ICareerRegistry} 接口的单例实例，供 {@link CivilizationAPI#getCareerRegistry()} 暴露给附属模组 */
    public static final ICareerRegistry REGISTRY_INSTANCE = new ICareerRegistry() {
        @Override
        public Career byName(String name) { return REGISTRY.get(name); }

        @Override
        public Collection<Career> allCareers() { return Collections.unmodifiableCollection(REGISTRY.values()); }

        @Override
        public Career fromVanilla(VillagerProfession prof) {
            for (Career c : REGISTRY.values()) {
                if (c.vanillaProfession == prof) return c;
            }
            return byName(CareerNames.UNEMPLOYED);
        }

        @Override
        public Map<String, Career> getRegistry() { return Collections.unmodifiableMap(REGISTRY); }
    };

    private final String name;
    private int tier;
    private final int modelIndex;
    @Nullable
    private final VillagerProfession vanillaProfession;
    /** 父职业名称（null 表示此为根职业，如 "unemployed" 等初始职业） */
    @Nullable
    private String parentCareerName = null;
    /** 子职业列表（由 CareerConfig 在配置加载后填充） */
    private final List<Career> children = new ArrayList<>();
    /** 晋升为该职业所需学徒经验阈值（0 = 不可晋升，需由 CareerConfig 覆盖为实际值） */
    private int apprenticeExpThreshold = 0;

    /**
     * 构造一个职业实例并自动注册到内部注册表。
     *
     * <p>注意：事件 {@link CareerRegisterEvent} 不再在构造器中触发，
     * 而是在所有 Career 构造完毕后由 {@link #fireRegisterEvents()} 统一发送。
     *
     * @param name              职业唯一名称（如 "armorer"）
     * @param tier              职业等级（0 = 无业/傻子，1 = 初始职业）
     * @param vanillaProfession 对应的原版村民职业，无对应则为 null
     */
    protected Career(String name, int tier, @Nullable VillagerProfession vanillaProfession) {
        this.name = name;
        this.tier = tier;
        this.vanillaProfession = vanillaProfession;
        this.modelIndex = nextModelIndex++;
        REGISTRY.put(name, this);
    }

    /**
     * 在所有 Career 实例构造完成后，统一向 NeoForge 事件总线发送 {@link CareerRegisterEvent}。
     * 应由 {@code CivilizationEvolution} 在初始化末尾调用。
     */
    public static void fireRegisterEvents() {
        for (Career career : REGISTRY.values()) {
            NeoForge.EVENT_BUS.post(new CareerRegisterEvent(career));
        }
    }

    /**
     * 设置父职业名称，供子类构造器中调用。
     *
     * <p>例如农民的子职业可在构造器中调用 {@code setParentCareerName("farmer")}，
     * 这样 {@link #isKindOf(String)} 可沿父链向上追溯到根职业。
     *
     * @param name 父职业名称（如 "farmer"）
     */
    public void setParentCareerName(String name) {
        this.parentCareerName = name;
    }

    /** @return 职业唯一名称（如 "armorer"） */
    public String getName() { return name; }

    /** @return 国际化翻译键，格式为 "career.civilizationevolution.<名称>" */
    public String getTranslationKey() { return "career.civilizationevolution." + name; }

    /** @return 职业等级（0 = 无业/傻子，1 = 初始职业） */
    public int getTier() { return tier; }

    /** 获取该职业的模型索引，用于 ItemProperty 切换贴图 */
    public int getModelIndex() { return modelIndex; }

    /** @return 对应的原版村民职业，无对应则为 null */
    @Nullable
    public VillagerProfession getVanillaProfession() { return vanillaProfession; }

    /** @return 父职业名称，null 表示此为根职业 */
    @Nullable
    public String getParentCareerName() { return parentCareerName; }

    /**
     * 设置职业等级，供 {@link com.gooodwei.civilizationevolution.server.config.CareerConfig}
     * 在配置加载后覆盖构造时的默认值。
     */
    public void setTier(int tier) { this.tier = tier; }

    /**
     * 添加一个子职业，由 CareerConfig 在配置加载后调用。
     */
    public void addChild(Career child) { this.children.add(child); }

    /** @return 直接子职业的只读列表 */
    public List<Career> getChildren() { return Collections.unmodifiableList(children); }

    /**
     * 递归获取所有后代职业（子职业、孙职业等）。
     * @return 所有后代的平铺列表
     */
    public List<Career> getDescendants() {
        List<Career> result = new ArrayList<>();
        for (Career child : children) {
            result.add(child);
            result.addAll(child.getDescendants());
        }
        return result;
    }

    /** @return 晋升为该职业所需学徒经验阈值（≤0 表示不可通过学徒晋升） */
    public int getApprenticeExpThreshold() { return apprenticeExpThreshold; }

    /**
     * 设置学徒经验阈值，供 CareerConfig 在配置加载后覆盖默认值。
     * @param threshold 晋升所需经验值（≤0 = 不可晋升）
     */
    public void setApprenticeExpThreshold(int threshold) { this.apprenticeExpThreshold = threshold; }

    /**
     * 判断此职业是否等于指定名称或由其派生（沿父链向上追溯）。
     *
     * <p>例如：若 "farmer_artisan" 的父职业是 "farmer"，
     * 则 {@code farmerArtisan.isKindOf("farmer")} 返回 {@code true}。
     *
     * @param baseName 基职业名称（如 "farmer"）
     * @return true 表示此职业等于 baseName 或由其派生
     */
    public boolean isKindOf(String baseName) {
        if (this.name.equals(baseName)) return true;
        if (this.parentCareerName == null) return false;
        Career parent = REGISTRY.get(this.parentCareerName);
        if (parent == null) return false;
        return parent.isKindOf(baseName);
    }

    // --- 静态便捷方法（向后兼容） ---

    /**
     * 按名称查找职业。
     * @param name 职业名称
     * @return 对应的 Career，未找到则为 null
     */
    @Nullable
    public static Career byName(String name) {
        return REGISTRY_INSTANCE.byName(name);
    }

    /**
     * 获取所有已注册职业的只读集合。
     * @return 不可修改的 Career 集合
     */
    static Collection<Career> allCareers() {
        return REGISTRY_INSTANCE.allCareers();
    }

    /**
     * 将原版 VillagerProfession 映射到模组的 Career。
     * 如果没有匹配项，则返回 {@code unemployed}。
     */
    static Career fromVanilla(VillagerProfession prof) {
        return REGISTRY_INSTANCE.fromVanilla(prof);
    }

    /**
     * 按名称判断一个职业是否等于或派生自指定的基职业。
     *
     * <p>遍历注册表中的父链，检查 {@code careerName} 是否等于 {@code baseName}
     * 或其任意祖先等于 {@code baseName}。
     *
     * <p>例如 {@code Career.isKindOf("farmer_artisan", "farmer")} 返回 {@code true}，
     * 前提是 "farmer_artisan" 的 {@code parentCareerName} 为 "farmer"。
     *
     * @param careerName 待检查的职业名称
     * @param baseName   基职业名称
     * @return true 表示 careerName 等于或派生自 baseName
     */
    public static boolean isKindOf(String careerName, String baseName) {
        Career career = REGISTRY.get(careerName);
        if (career == null) return false;
        return career.isKindOf(baseName);
    }
}

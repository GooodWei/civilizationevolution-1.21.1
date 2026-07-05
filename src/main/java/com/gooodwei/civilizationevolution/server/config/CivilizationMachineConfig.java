package com.gooodwei.civilizationevolution.server.config;

import com.gooodwei.civilizationevolution.api.util.SimpleYamlParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 从 civilizationMachine.yml 加载文明机器的配置。
 * 首次启动时自动在 config/civilizationevolution/ 下生成默认文件。
 *
 * <p>使用 Map 驱动架构，新增机器只需：
 * <ol>
 *   <li>添加 key 常量</li>
 *   <li>在 {@link #writeDefaults()} 中添加 yml 段落</li>
 *   <li>添加对应的便捷静态字段（可选）</li>
 * </ol>
 *
 * <p>配置格式：
 * <pre>{@code
 * camp:
 *   work_total_time: 12000
 *   age_increment: 1
 * }</pre>
 */
public final class CivilizationMachineConfig {

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("civilizationevolution");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("civilizationMachine.yml");

    // ==================== 机器 Key 常量 ====================

    public static final String CAMP = "camp";
    public static final String PRIMITIVE_HUNTING_GROUND = "primitive_hunting_ground";
    public static final String PRIMITIVE_CONTROLLER = "primitive_controller";
    public static final String PRIMITIVE_RANCH = "primitive_ranch";
    public static final String PRIMITIVE_FARM = "primitive_farm";
    public static final String PRIMITIVE_DOCTOR_CABIN = "primitive_doctor_cabin";
    public static final String VILLAGE_CONTROLLER = "village_controller";
    public static final String VILLAGE_QUARRY = "village_quarry";
    public static final String VILLAGE_CAMP = "village_camp";
    public static final String VILLAGE_HUNTING_GROUND = "village_hunting_ground";
    public static final String VILLAGE_RANCH = "village_ranch";
    public static final String VILLAGE_FARM = "village_farm";
    public static final String VILLAGE_DOCTOR_CABIN = "village_doctor_cabin";
    public static final String VILLAGE_HARVESTER = "village_harvester";
    public static final String PRIMITIVE_STORAGE_PIT = "primitive_storage_pit";
    public static final String VILLAGE_MILL = "village_mill";

    // ==================== 内部记录 ====================

    /**
     * 人口效率计算公式的对数底数，控制多人合作时效率增长的速度。
     *
     * <p>效率 = avg × (1 + log_b(n))
     * <ul>
     *   <li>avg = 总人口效率 / 工人数量（工人平均 NBT 效率）</li>
     *   <li>n   = 工人数量</li>
     *   <li>b   = 底数（本配置项）</li>
     * </ul>
     *
     * <p>b=3 时每 3 倍工人效率翻倍：1人→1.00x、3人→2.00x、9人→3.00x。
     * b=2 增长更快，b=e≈2.718 为自然对数，b=4 增长更缓。
     * 设为 1.0 时退化为 avg（仅平均，不考虑人数）。
     */
    public static double EFFICIENCY_LOG_BASE = 3.0;

    /**
     * 食物因子基准值（面包的校准营养值），低于此值时按比例计算，高于此值时使用对数公式。
     *
     * <p>校准营养值 = nutrition + saturationModifier × nutrition × 2。
     * 面包的校准值 = 5 + 0.6×5×2 = 11.0。
     */
    public static double FOOD_REFERENCE_VALUE = 11.0;

    /**
     * 食物因子的对数底数，控制高品质食物的边际递减速度。
     *
     * <p>食物因子公式（两段式）：
     * <pre>
     * avg &lt; R  →  factor = avg / R          （线性比例）
     * avg ≥ R  →  factor = 1 + log_b(avg/R)  （对数增长，品质每翻 b 倍 +1）
     * </pre>
     *
     * <ul>
     *   <li>avg = 工作人口消耗食物的人均校准营养值</li>
     *   <li>R   = food_reference_value（默认 11.0 = 面包）</li>
     *   <li>b   = food_log_base（默认 2.0，品质翻倍 = +1）</li>
     * </ul>
     *
     * <p>示例（b=2, R=11）：
     * <ul>
     *   <li>苹果（6.4） → 6.4/11 = 0.58</li>
     *   <li>面包（11.0）→ 1.00（基准）</li>
     *   <li>牛排（20.8）→ 1 + log₂(20.8/11) = 1.92</li>
     * </ul>
     */
    public static double FOOD_LOG_BASE = 2.0;

    /** 单台机器的配置项 */
    public record MachineSection(int workTotalTime, int ageIncrement, int maxAnimalCount,
                                 int waterPerCrop, int foodPerPopulation,
                                 int fluidLavaPerCycle, int fluidWaterPerCycle,
                                 int blocksPerCycleMultiplier,
                                 int careerExpThreshold,
                                 int healthFluctuateMin, int healthFluctuateMax,
                                 int efficiencyNoWeapon, int fedPerTypeMultiplier,
                                 int minParentAge, int maxParentAge,
                                 int miningHorizontalSize,
                                 int apprenticeExpPerCycle) {}

    /** 文明控制器机器的配置项 */
    public record ControllerSection(int maxBindCount, int maxBindRange, boolean allowCrossDimension) {}

    /** 储物容器的配置项 */
    private record StorageSection(List<String> pitWallTags, int maxWidth, int maxHeight) {}

    // ==================== 配置 Map ====================

    /** 机器配置表（按配置文件顺序保持 LinkedHashMap） */
    private static final Map<String, MachineSection> SECTIONS = new LinkedHashMap<>();
    private static final Map<String, ControllerSection> CONTROLLERS = new LinkedHashMap<>();
    private static final Map<String, StorageSection> STORAGE_SECTIONS = new LinkedHashMap<>();

    // ==================== 便捷访问器 ====================

    /** 按机器 key 获取 workTotalTime */
    public static int getWorkTotalTime(String machine) {
        MachineSection s = SECTIONS.get(machine);
        return s != null ? s.workTotalTime() : 12000;
    }

    /** 按机器 key 获取 ageIncrement */
    public static int getAgeIncrement(String machine) {
        MachineSection s = SECTIONS.get(machine);
        return s != null ? s.ageIncrement() : 1;
    }

    /** 按控制器 key 获取最大绑定机器数，未配置时默认 10 */
    public static int getMaxBindCount(String controller) {
        ControllerSection s = CONTROLLERS.get(controller);
        return s != null ? s.maxBindCount() : 10;
    }

    /** 按控制器 key 获取最大绑定距离（格），未配置时默认 64 */
    public static int getMaxBindRange(String controller) {
        ControllerSection s = CONTROLLERS.get(controller);
        return s != null ? s.maxBindRange() : 64;
    }

    /** 按控制器 key 获取是否允许跨维度绑定，未配置时默认 false */
    public static boolean isAllowCrossDimension(String controller) {
        ControllerSection s = CONTROLLERS.get(controller);
        return s != null && s.allowCrossDimension();
    }

    /** 按机器 key 获取最大动物数量限制，未配置时默认 0（0 = 不限制） */
    public static int getMaxAnimalCount(String machine) {
        MachineSection s = SECTIONS.get(machine);
        return s != null ? s.maxAnimalCount() : 0;
    }

    /** 按机器 key 获取每次催熟作物消耗的水量（mB），未配置时默认 250 */
    public static int getWaterPerCrop(String machine) {
        MachineSection s = SECTIONS.get(machine);
        return s != null ? s.waterPerCrop() : 250;
    }

    /** 按机器 key 获取单位人口食物消耗量，未配置时默认 1 */
    public static int getFoodPerPopulation(String machine) {
        MachineSection s = SECTIONS.get(machine);
        return s != null ? s.foodPerPopulation() : 1;
    }

    /** 按机器 key 获取每次工作消耗的岩浆量（mB），未配置时返回指定默认值 */
    public static int getFluidLavaPerCycle(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null && s.fluidLavaPerCycle() >= 0 ? s.fluidLavaPerCycle() : defaultVal;
    }

    /** 按机器 key 获取每次工作消耗的水量（mB），未配置时返回指定默认值 */
    public static int getFluidWaterPerCycle(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null && s.fluidWaterPerCycle() >= 0 ? s.fluidWaterPerCycle() : defaultVal;
    }

    /** 按机器 key 获取效率→方块数的乘数，未配置时返回指定默认值 */
    public static int getBlocksPerCycleMultiplier(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null && s.blocksPerCycleMultiplier() >= 0 ? s.blocksPerCycleMultiplier() : defaultVal;
    }

    /** 按机器 key 获取单位人口食物消耗量，未配置时返回指定默认值（重载） */
    public static int getFoodPerPopulation(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null && s.foodPerPopulation() >= 0 ? s.foodPerPopulation() : defaultVal;
    }

    /** 按机器 key 获取学徒转职经验阈值，未配置时返回指定默认值 */
    public static int getCareerExpThreshold(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null && s.careerExpThreshold() >= 0 ? s.careerExpThreshold() : defaultVal;
    }

    /** 按机器 key 获取健康度波动下限，未配置时返回指定默认值 */
    public static int getHealthFluctuateMin(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null ? s.healthFluctuateMin() : defaultVal;
    }

    /** 按机器 key 获取健康度波动上限，未配置时返回指定默认值 */
    public static int getHealthFluctuateMax(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null ? s.healthFluctuateMax() : defaultVal;
    }

    /** 按机器 key 获取无武器时效率百分比（0-100），未配置时返回指定默认值 */
    public static int getEfficiencyNoWeapon(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null ? s.efficiencyNoWeapon() : defaultVal;
    }

    /** 按机器 key 获取效率→喂养数乘数，未配置时返回指定默认值 */
    public static int getFedPerTypeMultiplier(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null && s.fedPerTypeMultiplier() >= 0 ? s.fedPerTypeMultiplier() : defaultVal;
    }

    /** 按机器 key 获取最低生育年龄，未配置时返回指定默认值 */
    public static int getMinParentAge(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null && s.minParentAge() >= 0 ? s.minParentAge() : defaultVal;
    }

    /** 按机器 key 获取最高生育年龄，未配置时返回指定默认值 */
    public static int getMaxParentAge(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null && s.maxParentAge() >= 0 ? s.maxParentAge() : defaultVal;
    }

    /** 按机器 key 获取水平挖掘范围边长，未配置时返回指定默认值 */
    public static int getMiningHorizontalSize(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null && s.miningHorizontalSize() >= 0 ? s.miningHorizontalSize() : defaultVal;
    }

    /** 按机器 key 获取每次工作周期给学徒的经验量，未配置时返回指定默认值 */
    public static int getApprenticeExpPerCycle(String machine, int defaultVal) {
        MachineSection s = SECTIONS.get(machine);
        return s != null && s.apprenticeExpPerCycle() >= 0 ? s.apprenticeExpPerCycle() : defaultVal;
    }

    // ==================== 储物容器访问器 ====================

    /** 按机器 key 获取墙壁方块标签白名单，未配置时返回空列表（空 = 无白名单） */
    public static List<String> getWallBlockTags(String machine) {
        StorageSection s = STORAGE_SECTIONS.get(machine);
        return s != null ? s.pitWallTags() : List.of();
    }

    /** 按机器 key 获取最大内部宽度，未配置时默认 7 */
    public static int getStoragePitMaxWidth(String machine) {
        StorageSection s = STORAGE_SECTIONS.get(machine);
        return s != null ? s.maxWidth() : 7;
    }

    /** 按机器 key 获取最大内部高度，未配置时默认 7 */
    public static int getStoragePitMaxHeight(String machine) {
        StorageSection s = STORAGE_SECTIONS.get(machine);
        return s != null ? s.maxHeight() : 7;
    }

    // ==================== 向后兼容字段（新代码建议直接用上面的方法） ====================

    public static int CAMP_WORK_TOTAL_TIME;
    public static int CAMP_AGE_INCREMENT;
    public static int PRIMITIVE_HUNTING_GROUND_TOTAL_TIME;
    public static int PRIMITIVE_HUNTING_GROUND_AGE_INCREMENT;
    public static int PRIMITIVE_RANCH_WORK_TOTAL_TIME;
    public static int PRIMITIVE_RANCH_AGE_INCREMENT;
    public static int PRIMITIVE_RANCH_MAX_ANIMAL_COUNT;

    // ==================== 初始化 ====================

    private CivilizationMachineConfig() {}

    public static void init() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefaults();
            }
            load();
            // 将 Map 值同步到向后兼容的静态字段
            syncLegacyFields();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize CivilizationMachine config", e);
        }
    }

    private static void syncLegacyFields() {
        CAMP_WORK_TOTAL_TIME = getWorkTotalTime(CAMP);
        CAMP_AGE_INCREMENT = getAgeIncrement(CAMP);
        PRIMITIVE_HUNTING_GROUND_TOTAL_TIME = getWorkTotalTime(PRIMITIVE_HUNTING_GROUND);
        PRIMITIVE_HUNTING_GROUND_AGE_INCREMENT = getAgeIncrement(PRIMITIVE_HUNTING_GROUND);
        PRIMITIVE_RANCH_WORK_TOTAL_TIME = getWorkTotalTime(PRIMITIVE_RANCH);
        PRIMITIVE_RANCH_AGE_INCREMENT = getAgeIncrement(PRIMITIVE_RANCH);
        PRIMITIVE_RANCH_MAX_ANIMAL_COUNT = getMaxAnimalCount(PRIMITIVE_RANCH);
    }

    // ==================== 文件读写 ====================

    private static void writeDefaults() throws IOException {
        String defaults = """
                # CivilizationEvolution 文明机器配置
                # 工作周期单位为 tick（20 tick = 1 秒）

                # ============================================================
                # 全局配置
                # ============================================================

                # ========== 人口效率计算公式 ==========
                #
                #   效率 = avg × (1 + log_b(n))
                #
                #   avg = 所有工人的 NBT 效率之和 / 工人数量 n（即平均效率）
                #   n   = 实际参与工作的工人数量（符合年龄和职业要求的人口）
                #   b   = 底数（本配置项），含义：每 b 倍工人数量，效率翻倍
                #
                # 示例（b=3 时）：
                #   1 个工人 → 效率 = avg × 1.00
                #   3 个工人 → 效率 = avg × 2.00
                #   9 个工人 → 效率 = avg × 3.00
                #
                # 调节指南：
                #   b=2   — 增长快，2 人即翻倍，适合小规模团战
                #   b=3   — 默认值，平衡，3 人翻倍
                #   b=4   — 增长缓，4 人翻倍，大型团队惩罚大
                #   b=1.0 — 退化为纯平均（avg），人数完全不产生加成
                efficiency_log_base: 3.0

                # ========== 食物因子计算公式 ==========
                #
                # 两段式公式（avg = 工作人口消耗食物的人均校准营养值）：
                #
                #   avg < R  →  factor = avg / R          （低于基准：线性比例）
                #   avg ≥ R  →  factor = 1 + log_b(avg/R)  （达到基准后：对数增长）
                #
                #   校准营养值 = nutrition + saturationModifier × nutrition × 2
                #   R = food_reference_value（基准值，默认 11.0 = 面包）
                #   b = food_log_base（底数，默认 2.0 = 品质翻倍 +1 效率）
                #
                # 示例（b=2, R=11）：
                #   腐肉（10.4）→ 10.4/11 = 0.95
                #   面包（11.0）→ 1.00（基准参照点）
                #   金胡萝卜（14.4）→ 1 + log₂(14.4/11) = 1.39
                #   牛排（20.8）→ 1 + log₂(20.8/11) = 1.92
                #
                # 调节指南：
                #   food_reference_value=11  — 以面包为基准
                #   food_log_base=2  — 品质翻倍 +1，增长适中
                #   food_log_base=3  — 品质翻 3 倍 +1，增长更缓
                food_reference_value: 11.0
                food_log_base: 2.0

                camp:
                  # 营地完成一次工作所需的 tick 数（12000 tick = 10 分钟）
                  work_total_time: 12000
                  # 营地每次工作后每个人口的年龄增长量
                  age_increment: 1
                  # 每个人口每次工作消耗的食物份数
                  food_per_population: 8
                  # 父代最低生育年龄
                  min_parent_age: 18
                  # 父代最高生育年龄
                  max_parent_age: 50
                  # 每次工作后人口健康度的随机波动范围
                  health_fluctuate_min: -10
                  health_fluctuate_max: 5
                  # 每次工作周期给学徒的经验量（营地不使用学徒系统，保留字段）
                  apprentice_exp_per_cycle: 1

                primitive_hunting_ground:
                  # 原始狩猎场完成一次工作所需的 tick 数（12000 tick = 10 分钟）
                  work_total_time: 12000
                  # 原始狩猎场每次工作后每个人口的年龄增长量
                  age_increment: 1
                  # 每个人口每次工作消耗的食物份数
                  food_per_population: 32
                  # 学徒累积多少经验后转职为屠夫
                  career_exp_threshold: 8
                  # 无武器时效率百分比（50 = 50% = 0.5 倍率）
                  efficiency_no_weapon: 50
                  # 每次工作后人口健康度的随机波动范围（负数 = 下降）
                  health_fluctuate_min: -5
                  health_fluctuate_max: -1
                  # 每次工作周期给学徒的经验量
                  apprentice_exp_per_cycle: 1

                primitive_ranch:
                  # 原始牧场完成一次工作所需的 tick 数（3000 tick = 2.5 分钟）
                  work_total_time: 3000
                  # 原始牧场每次工作后每个人口的年龄增长量
                  age_increment: 1
                  # 范围内最大动物数量，超过时取消当次工作（0 = 不限制）
                  max_animal_count: 24
                  # 每个人口每次工作消耗的食物份数
                  food_per_population: 2
                  # 效率→每种动物喂养数量的乘数（效率 × 此值 = 每种动物喂养数）
                  fed_per_type_multiplier: 3
                  # 学徒累积多少经验后转职为牧羊人
                  career_exp_threshold: 8
                  # 每次工作后人口健康度的随机波动范围（负数 = 下降）
                  health_fluctuate_min: -5
                  health_fluctuate_max: -1
                  # 每次工作周期给学徒的经验量
                  apprentice_exp_per_cycle: 1

                primitive_farm:
                  # 原始农场完成一次工作所需的 tick 数（1200 tick = 1 分钟）
                  work_total_time: 1200
                  # 原始农场每次工作后每个人口的年龄增长量
                  age_increment: 1
                  # 每个人口每次工作消耗的食物份数
                  food_per_population: 8
                  # 每次催熟作物消耗的水量（mB），每桶 = 1000 mB
                  water_per_crop: 10
                  # 学徒累积多少经验后转职为农民
                  career_exp_threshold: 8
                  # 每次工作后人口健康度的随机波动范围（负数 = 下降）
                  health_fluctuate_min: -5
                  health_fluctuate_max: -1
                  # 每次工作周期给学徒的经验量
                  apprentice_exp_per_cycle: 1

                primitive_doctor_cabin:
                  # 原始诊所完成一次工作所需的 tick 数（6000 tick = 5 分钟）
                  work_total_time: 6000
                  # 原始诊所每次工作后每个人口的年龄增长量
                  age_increment: 1
                  # 单位人口每次工作周期消耗的食物量
                  food_per_population: 1
                  # 学徒累积多少经验后转职为牧师
                  career_exp_threshold: 8
                  # 每次工作周期给学徒的经验量
                  apprentice_exp_per_cycle: 1

                primitive_storage_pit:
                  # 墙壁方块标签白名单，JSON 数组格式，空 = 任意实心完整方块
                  pit_wall_block_tags: []
                  # 最大内部宽度（奇数：3/5/7）
                  max_interior_width: 7
                  # 最大内部高度
                  max_interior_height: 7

                # 控制器配置
                primitive_controller:
                  # 最大可绑定机器数量
                  max_bind_count: 10
                  # 最大绑定距离（格）
                  max_bind_range: 64
                  # 是否允许跨维度绑定（true/false）
                  allow_cross_dimension: false

                village_quarry:
                  # 村庄采石场完成一次工作所需的 tick 数（200 tick = 10 秒）
                  work_total_time: 200
                  # 村庄采石场每次工作后每个人口的年龄增长量
                  age_increment: 1
                  # 每次工作消耗的岩浆量（mB），每桶 = 1000 mB
                  fluid_lava_per_cycle: 100
                  # 每次工作消耗的水量（mB）
                  fluid_water_per_cycle: 100
                  # 每个人口每次工作消耗的食物份数
                  food_per_population: 1
                  # 效率→每周期破坏方块数的乘数（效率 × 此值 = 每周期方块数）
                  blocks_per_cycle_multiplier: 2
                  # 学徒累积多少经验后转职为矿工
                  career_exp_threshold: 8
                  # 每次工作后人口健康度的随机波动范围（负数 = 下降）
                  health_fluctuate_min: -1
                  health_fluctuate_max: 0
                  # 每次工作周期给学徒的经验量
                  apprentice_exp_per_cycle: 1
                  # 水平挖掘范围边长（如 16 = 16×16 即 1 个区块）
                  mining_horizontal_size: 16

                village_camp:
                  # 村庄营地完成一次工作所需的 tick 数（原始 12000 的一半）
                  work_total_time: 6000
                  # 村庄营地每次工作后每个人口的年龄增长量（原始 1 的两倍）
                  age_increment: 2
                  # 每个人口每次工作消耗的食物份数（原始 8 的两倍）
                  food_per_population: 16
                  # 父代最低生育年龄（原始 18 的两倍）
                  min_parent_age: 36
                  # 父代最高生育年龄（原始 50 的两倍）
                  max_parent_age: 100
                  # 每次工作后人口健康度的随机波动范围（原始的两倍）
                  health_fluctuate_min: -10
                  health_fluctuate_max: 5
                  # 每次工作周期给学徒的经验量（原始 1 的两倍）
                  apprentice_exp_per_cycle: 2

                village_hunting_ground:
                  # 村庄狩猎场完成一次工作所需的 tick 数（原始 12000 的一半）
                  work_total_time: 6000
                  # 村庄狩猎场每次工作后每个人口的年龄增长量（原始 1 的两倍）
                  age_increment: 1
                  # 每个人口每次工作消耗的食物份数（原始 32 的两倍）
                  food_per_population: 64
                  # 学徒累积多少经验后转职为屠夫
                  career_exp_threshold: 8
                  # 无武器时效率百分比
                  efficiency_no_weapon: 50
                  # 每次工作后人口健康度的随机波动范围（原始的两倍）
                  health_fluctuate_min: -5
                  health_fluctuate_max: -1
                  # 每次工作周期给学徒的经验量（原始 1 的两倍）
                  apprentice_exp_per_cycle: 2

                village_ranch:
                  # 村庄牧场完成一次工作所需的 tick 数（原始 3000 的一半）
                  work_total_time: 1500
                  # 村庄牧场每次工作后每个人口的年龄增长量（原始 1 的两倍）
                  age_increment: 1
                  # 范围内最大动物数量，超过时取消当次工作（原始 24 的两倍）
                  max_animal_count: 24
                  # 每个人口每次工作消耗的食物份数（原始 2 的两倍）
                  food_per_population: 2
                  # 效率→每种动物喂养数量的乘数（原始 3 的两倍）
                  fed_per_type_multiplier: 6
                  # 学徒累积多少经验后转职为牧羊人
                  career_exp_threshold: 8
                  # 每次工作后人口健康度的随机波动范围（原始的两倍）
                  health_fluctuate_min: -5
                  health_fluctuate_max: -1
                  # 每次工作周期给学徒的经验量（原始 1 的两倍）
                  apprentice_exp_per_cycle: 2

                village_farm:
                  # 村庄农场完成一次工作所需的 tick 数（原始 1200 的一半）
                  work_total_time: 600
                  # 村庄农场每次工作后每个人口的年龄增长量（原始 1 的两倍）
                  age_increment: 1
                  # 每个人口每次工作消耗的食物份数（原始 8 的两倍）
                  food_per_population: 8
                  # 每催熟作物消耗的水量（mB），每桶 = 1000 mB
                  water_per_crop: 10
                  # 学徒累积多少经验后转职为农民
                  career_exp_threshold: 8
                  # 每次工作后人口健康度的随机波动范围（原始的两倍）
                  health_fluctuate_min: -5
                  health_fluctuate_max: -1
                  # 每次工作周期给学徒的经验量（原始 1 的两倍）
                  apprentice_exp_per_cycle: 2

                village_doctor_cabin:
                  # 村庄诊所完成一次工作所需的 tick 数（原始 6000 的一半）
                  work_total_time: 3000
                  # 村庄诊所每次工作后每个人口的年龄增长量（原始 1 的两倍）
                  age_increment: 1
                  # 单位人口每次工作周期消耗的食物量（原始 1 的两倍）
                  food_per_population: 1
                  # 学徒累积多少经验后转职为牧师
                  career_exp_threshold: 8
                  # 每次工作周期给学徒的经验量（原始 1 的两倍）
                  apprentice_exp_per_cycle: 2

                village_harvester:
                  #收割间隔tick数
                  work_total_time: 6000
                  #每次工作后人口年龄增长量
                  age_increment: 1
                  #每次工作消耗食物量
                  food_per_population: 32
                  # 学徒累积多少经验后转职为农民
                  career_exp_threshold: 8
                  # 每次工作后人口健康度的随机波动范围
                  health_fluctuate_min: -5
                  health_fluctuate_max: -1
                  # 每次工作周期给学徒的经验量
                  apprentice_exp_per_cycle: 2
                  # 每作物消耗的水量（mB），每桶 = 1000 mB
                  water_per_crop: 10

                village_controller:
                  # 最大可绑定机器数量
                  max_bind_count: 20
                  # 最大绑定距离（格）
                  max_bind_range: 128
                  # 是否允许跨维度绑定（true/false）
                  allow_cross_dimension: false
                  
                village_mill:
                    # 每次工作后人口年龄增长量
                    age_increment: 1
                    # 每个人口每次工作消耗的食物份数
                    food_per_population: 6
                    # 每次工作后人口健康度的随机波动范围
                    health_fluctuate_min: -3
                    health_fluctuate_max: -1
                    # 每次工作周期给学徒的经验量
                    apprentice_exp_per_cycle: 1
                """;
        Files.writeString(CONFIG_FILE, defaults);
    }

    private static void load() throws IOException {
        Map<String, Map<String, String>> sections = SimpleYamlParser.parse(CONFIG_FILE);
        for (var sectionEntry : sections.entrySet()) {
            String name = sectionEntry.getKey();
            Map<String, String> kv = sectionEntry.getValue();

            if ("global".equals(name)) {
                // 全局配置：读取效率计算公式底数
                String baseStr = kv.get("efficiency_log_base");
                if (baseStr != null && !baseStr.isEmpty()) {
                    try {
                        double base = Double.parseDouble(baseStr);
                        if (base > 0) EFFICIENCY_LOG_BASE = base;
                    } catch (NumberFormatException ignored) {}
                }
                // 食物因子基准值（面包校准值）
                String refStr = kv.get("food_reference_value");
                if (refStr != null && !refStr.isEmpty()) {
                    try {
                        double ref = Double.parseDouble(refStr);
                        if (ref > 0) FOOD_REFERENCE_VALUE = ref;
                    } catch (NumberFormatException ignored) {}
                }
                // 食物因子对数底数
                String foodBaseStr = kv.get("food_log_base");
                if (foodBaseStr != null && !foodBaseStr.isEmpty()) {
                    try {
                        double foodBase = Double.parseDouble(foodBaseStr);
                        if (foodBase > 0) FOOD_LOG_BASE = foodBase;
                    } catch (NumberFormatException ignored) {}
                }
            } else if (kv.containsKey("pit_wall_block_tags")) {
                // 储物容器 section
                STORAGE_SECTIONS.put(name, new StorageSection(
                        parseJsonArray(kv.get("pit_wall_block_tags")),
                        getInt(kv, "max_interior_width", 7),
                        getInt(kv, "max_interior_height", 7)));
            } else if (kv.containsKey("max_bind_count")) {
                // 控制器 section
                CONTROLLERS.put(name, new ControllerSection(
                        getInt(kv, "max_bind_count", 10),
                        getInt(kv, "max_bind_range", 64),
                        getBool(kv, "allow_cross_dimension", false)));
            } else {
                // 机器 section
                SECTIONS.put(name, new MachineSection(
                        getInt(kv, "work_total_time", 12000),
                        getInt(kv, "age_increment", 1),
                        getInt(kv, "max_animal_count", 0),
                        getInt(kv, "water_per_crop", 0),
                        getInt(kv, "food_per_population", 1),
                        getInt(kv, "fluid_lava_per_cycle", 0),
                        getInt(kv, "fluid_water_per_cycle", 0),
                        getInt(kv, "blocks_per_cycle_multiplier", 0),
                        getInt(kv, "career_exp_threshold", 0),
                        getInt(kv, "health_fluctuate_min", 0),
                        getInt(kv, "health_fluctuate_max", 0),
                        getInt(kv, "efficiency_no_weapon", 0),
                        getInt(kv, "fed_per_type_multiplier", 0),
                        getInt(kv, "min_parent_age", 0),
                        getInt(kv, "max_parent_age", 0),
                        getInt(kv, "mining_horizontal_size", 0),
                        getInt(kv, "apprentice_exp_per_cycle", 1)));
            }
        }
    }

    /** 从键值映射中读取 int，缺失或空值时返回默认值 */
    private static int getInt(Map<String, String> kv, String key, int defaultVal) {
        String val = kv.get(key);
        if (val == null || val.isEmpty()) return defaultVal;
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /** 从键值映射中读取 boolean，缺失时返回默认值 */
    private static boolean getBool(Map<String, String> kv, String key, boolean defaultVal) {
        String val = kv.get(key);
        if (val == null || val.isEmpty()) return defaultVal;
        return Boolean.parseBoolean(val);
    }

    /**
     * 解析 JSON 风格的字符串数组，如 '["a", "b"]'
     */
    private static List<String> parseJsonArray(String raw) {
        if (raw == null || raw.isBlank() || raw.equals("[]")) return List.of();
        String inner = raw.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
        if (inner.isBlank()) return List.of();
        return Arrays.stream(inner.split(","))
                .map(String::trim)
                .map(s -> s.replaceAll("^\"|\"$", "")) // 去掉首尾双引号
                .filter(s -> !s.isEmpty())
                .toList();
    }

}

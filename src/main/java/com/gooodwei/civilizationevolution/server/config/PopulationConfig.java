package com.gooodwei.civilizationevolution.server.config;

import net.neoforged.fml.loading.FMLPaths;

import com.gooodwei.civilizationevolution.api.util.SimpleYamlParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 人口默认属性配置，从 {@code population.yml} 加载。
 *
 * <p>首次启动时自动在 config 目录下生成带默认值的配置文件。
 * 分为<b>成人（adult）</b>和<b>儿童（child）</b>两组参数，
 * 控制新生成人口物品时的初始 NBT 值。
 *
 * <p>配置路径：{@code config/civilizationevolution/population.yml}
 */
public final class PopulationConfig {

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("civilizationevolution");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("population.yml");

    // ==================== 成人默认值 ====================

    /** 成人初始年龄（也是最低工作年龄） */
    private static int adultAge;
    /** 退休年龄：超过此年龄的人口不再工作，但仍占用槽位、消耗食物、每周期老化 */
    private static int retirementAge;
    /** 最大工作年龄：超过此年龄的人口工作效率降为零 */
    private static int maxWorkAge;
    /** 寿命下限（tick 随机范围） */
    private static int lifespanMin;
    /** 寿命上限 */
    private static int lifespanMax;
    /** 寿命均值（正态分布中心），generateLifespan() 使用 */
    private static int lifespanMean;
    /** 成人初始生命值下限 */
    private static int adultHealthMin;
    /** 成人初始生命值上限 */
    private static int adultHealthMax;
    /** 成人初始饱食度下限 */
    private static int adultFoodMin;
    /** 成人初始饱食度上限 */
    private static int adultFoodMax;
    /** 成人初始熟练度 */
    private static int adultProficiency;
    /** 成人初始工作效率（0.0 ~ N） */
    private static double adultWorkEfficiency;
    /** 成人初始心理状态（0.0 ~ 1.0） */
    private static double adultMentalState;

    // ==================== 儿童默认值 ====================

    /** 儿童初始生命值下限 */
    private static int childHealthMin;
    /** 儿童初始生命值上限 */
    private static int childHealthMax;
    /** 儿童初始饱食度下限 */
    private static int childFoodMin;
    /** 儿童初始饱食度上限 */
    private static int childFoodMax;
    /** 儿童初始熟练度 */
    private static int childProficiency;
    /** 儿童初始工作效率 */
    private static double childWorkEfficiency;
    /** 儿童初始心理状态下限 */
    private static double childMentalStateMin;
    /** 儿童初始心理状态上限 */
    private static double childMentalStateMax;

    private PopulationConfig() {}

    /**
     * 初始化配置：确保目录存在 → 无文件则生成默认 → 加载。
     * 由 {@code CivilizationEvolution} 构造阶段调用。
     */
    public static void init() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefaults();
            }
            load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize population config", e);
        }
    }

    /**
     * 写入默认配置文件。包含成人和儿童两组默认值。
     */
    private static void writeDefaults() throws IOException {
        String defaults = """
                adult:
                  age: 18
                  retirement_age: 65
                  max_work_age: 85
                  lifespan_mean: 75
                  lifespan_min: 15
                  lifespan_max: 110
                  health_min: 60
                  health_max: 100
                  food_min: 60
                  food_max: 80
                  proficiency: 100
                  work_efficiency: 0.0
                  mental_state: 1.0
                child:
                  health_min: 60
                  health_max: 100
                  food_min: 40
                  food_max: 60
                  proficiency: 100
                  work_efficiency: 0.0
                  mental_state_min: 0.8
                  mental_state_max: 1.0
                """;
        Files.writeString(CONFIG_FILE, defaults);
    }

    /**
     * 使用 {@link SimpleYamlParser} 解析配置文件，按 section 分发到 {@link #loadAdult} / {@link #loadChild}。
     */
    private static void load() throws IOException {
        Map<String, Map<String, String>> sections = SimpleYamlParser.parse(CONFIG_FILE);
        for (var sectionEntry : sections.entrySet()) {
            String section = sectionEntry.getKey();
            Map<String, String> kv = sectionEntry.getValue();
            switch (section) {
                case "adult" -> kv.forEach(PopulationConfig::loadAdult);
                case "child" -> kv.forEach(PopulationConfig::loadChild);
            }
        }
    }

    /** 解析成人 section 中的键值对，写入对应的静态字段 */
    private static void loadAdult(String key, String value) {
        switch (key) {
            case "age" -> adultAge = Integer.parseInt(value);
            case "retirement_age" -> retirementAge = Integer.parseInt(value);
            case "max_work_age" -> maxWorkAge = Integer.parseInt(value);
            case "lifespan_mean" -> lifespanMean = Integer.parseInt(value);
            case "lifespan_min" -> lifespanMin = Integer.parseInt(value);
            case "lifespan_max" -> lifespanMax = Integer.parseInt(value);
            case "health_min" -> adultHealthMin = Integer.parseInt(value);
            case "health_max" -> adultHealthMax = Integer.parseInt(value);
            case "food_min" -> adultFoodMin = Integer.parseInt(value);
            case "food_max" -> adultFoodMax = Integer.parseInt(value);
            case "proficiency" -> adultProficiency = Integer.parseInt(value);
            case "work_efficiency" -> adultWorkEfficiency = Double.parseDouble(value);
            case "mental_state" -> adultMentalState = Double.parseDouble(value);
        }
    }

    /** 解析儿童 section 中的键值对，写入对应的静态字段 */
    private static void loadChild(String key, String value) {
        switch (key) {
            case "health_min" -> childHealthMin = Integer.parseInt(value);
            case "health_max" -> childHealthMax = Integer.parseInt(value);
            case "food_min" -> childFoodMin = Integer.parseInt(value);
            case "food_max" -> childFoodMax = Integer.parseInt(value);
            case "proficiency" -> childProficiency = Integer.parseInt(value);
            case "work_efficiency" -> childWorkEfficiency = Double.parseDouble(value);
            case "mental_state_min" -> childMentalStateMin = Double.parseDouble(value);
            case "mental_state_max" -> childMentalStateMax = Double.parseDouble(value);
        }
    }

    // ==================== Getter 方法 ====================

    public static int getAdultAge() { return adultAge; }
    public static int getRetirementAge() { return retirementAge; }
    public static int getMaxWorkAge() { return maxWorkAge; }
    public static int getLifespanMin() { return lifespanMin; }
    public static int getLifespanMax() { return lifespanMax; }
    public static int getLifespanMean() { return lifespanMean; }
    public static int getAdultHealthMin() { return adultHealthMin; }
    public static int getAdultHealthMax() { return adultHealthMax; }
    public static int getAdultFoodMin() { return adultFoodMin; }
    public static int getAdultFoodMax() { return adultFoodMax; }
    public static int getAdultProficiency() { return adultProficiency; }
    public static double getAdultWorkEfficiency() { return adultWorkEfficiency; }
    public static double getAdultMentalState() { return adultMentalState; }
    public static int getChildHealthMin() { return childHealthMin; }
    public static int getChildHealthMax() { return childHealthMax; }
    public static int getChildFoodMin() { return childFoodMin; }
    public static int getChildFoodMax() { return childFoodMax; }
    public static int getChildProficiency() { return childProficiency; }
    public static double getChildWorkEfficiency() { return childWorkEfficiency; }
    public static double getChildMentalStateMin() { return childMentalStateMin; }
    public static double getChildMentalStateMax() { return childMentalStateMax; }
}

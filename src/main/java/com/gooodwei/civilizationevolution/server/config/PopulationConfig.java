package com.gooodwei.civilizationevolution.server.config;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
    public static int ADULT_AGE;
    /** 退休年龄：超过此年龄的人口不再工作，但仍占用槽位、消耗食物、每周期老化 */
    public static int RETIREMENT_AGE;
    /** 最大工作年龄：超过此年龄的人口工作效率降为零 */
    public static int MAX_WORK_AGE;
    /** 寿命下限（tick 随机范围） */
    public static int LIFESPAN_MIN;
    /** 寿命上限 */
    public static int LIFESPAN_MAX;
    /** 成人初始生命值下限 */
    public static int ADULT_HEALTH_MIN;
    /** 成人初始生命值上限 */
    public static int ADULT_HEALTH_MAX;
    /** 成人初始饱食度下限 */
    public static int ADULT_FOOD_MIN;
    /** 成人初始饱食度上限 */
    public static int ADULT_FOOD_MAX;
    /** 成人初始熟练度 */
    public static int ADULT_PROFICIENCY;
    /** 成人初始工作效率（0.0 ~ N） */
    public static double ADULT_WORK_EFFICIENCY;
    /** 成人初始心理状态（0.0 ~ 1.0） */
    public static double ADULT_MENTAL_STATE;

    // ==================== 儿童默认值 ====================

    /** 儿童初始生命值下限 */
    public static int CHILD_HEALTH_MIN;
    /** 儿童初始生命值上限 */
    public static int CHILD_HEALTH_MAX;
    /** 儿童初始饱食度下限 */
    public static int CHILD_FOOD_MIN;
    /** 儿童初始饱食度上限 */
    public static int CHILD_FOOD_MAX;
    /** 儿童初始熟练度 */
    public static int CHILD_PROFICIENCY;
    /** 儿童初始工作效率 */
    public static double CHILD_WORK_EFFICIENCY;
    /** 儿童初始心理状态下限 */
    public static double CHILD_MENTAL_STATE_MIN;
    /** 儿童初始心理状态上限 */
    public static double CHILD_MENTAL_STATE_MAX;

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
     * 从配置文件逐行解析，按 section 分发到 {@link #loadAdult} / {@link #loadChild}。
     * 支持 {@code #} 开头的注释行。
     */
    private static void load() throws IOException {
        String content = Files.readString(CONFIG_FILE);
        String[] lines = content.split("\\R");
        String currentSection = "";

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            if (trimmed.equals("adult:")) { currentSection = "adult"; continue; }
            if (trimmed.equals("child:")) { currentSection = "child"; continue; }

            int colon = trimmed.indexOf(':');
            if (colon == -1) continue;
            String key = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();

            switch (currentSection) {
                case "adult" -> loadAdult(key, value);
                case "child" -> loadChild(key, value);
            }
        }
    }

    /** 解析成人 section 中的键值对，写入对应的静态字段 */
    private static void loadAdult(String key, String value) {
        switch (key) {
            case "age" -> ADULT_AGE = Integer.parseInt(value);
            case "retirement_age" -> RETIREMENT_AGE = Integer.parseInt(value);
            case "max_work_age" -> MAX_WORK_AGE = Integer.parseInt(value);
            case "lifespan_min" -> LIFESPAN_MIN = Integer.parseInt(value);
            case "lifespan_max" -> LIFESPAN_MAX = Integer.parseInt(value);
            case "health_min" -> ADULT_HEALTH_MIN = Integer.parseInt(value);
            case "health_max" -> ADULT_HEALTH_MAX = Integer.parseInt(value);
            case "food_min" -> ADULT_FOOD_MIN = Integer.parseInt(value);
            case "food_max" -> ADULT_FOOD_MAX = Integer.parseInt(value);
            case "proficiency" -> ADULT_PROFICIENCY = Integer.parseInt(value);
            case "work_efficiency" -> ADULT_WORK_EFFICIENCY = Double.parseDouble(value);
            case "mental_state" -> ADULT_MENTAL_STATE = Double.parseDouble(value);
        }
    }

    /** 解析儿童 section 中的键值对，写入对应的静态字段 */
    private static void loadChild(String key, String value) {
        switch (key) {
            case "health_min" -> CHILD_HEALTH_MIN = Integer.parseInt(value);
            case "health_max" -> CHILD_HEALTH_MAX = Integer.parseInt(value);
            case "food_min" -> CHILD_FOOD_MIN = Integer.parseInt(value);
            case "food_max" -> CHILD_FOOD_MAX = Integer.parseInt(value);
            case "proficiency" -> CHILD_PROFICIENCY = Integer.parseInt(value);
            case "work_efficiency" -> CHILD_WORK_EFFICIENCY = Double.parseDouble(value);
            case "mental_state_min" -> CHILD_MENTAL_STATE_MIN = Double.parseDouble(value);
            case "mental_state_max" -> CHILD_MENTAL_STATE_MAX = Double.parseDouble(value);
        }
    }
}

package com.gooodwei.civilizationevolution.server.config;

import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从 populationMachine.yml 加载人口机器的配置。
 * 首次启动时自动在 config/civilizationevolution/ 下生成默认文件。
 *
 * <p>使用 Map 驱动架构，新增机器只需：
 * <ol>
 *   <li>在  中添加 key 常量</li>
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
public final class PopulationMachineConfig {

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("civilizationevolution");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("populationMachine.yml");

    // ==================== 机器 Key 常量 ====================

    public static final String CAMP = "camp";
    public static final String HUNTING_GROUND = "hunting_ground";
    public static final String PRIMITIVE_SETTLEMENT = "primitive_settlement";
    public static final String PRIMITIVE_RANCH = "primitive_ranch";

    // ==================== 内部记录 ====================

    /** 单台机器的配置项 */
    public record MachineSection(int workTotalTime, int ageIncrement, int maxAnimalCount) {}

    /** 文明控制器机器的配置项*/
    public record ControllerSection(int maxBindCount, int maxBindRange, boolean allowCrossDimension) {}

    /** 机器配置表（按配置文件顺序保持 LinkedHashMap） */
    private static final Map<String, MachineSection> SECTIONS = new LinkedHashMap<>();
    private static final Map<String, ControllerSection> CONTROLLERS = new LinkedHashMap<>();

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

    // ==================== 向后兼容字段（新代码建议直接用上面的方法） ====================

    public static int CAMP_WORK_TOTAL_TIME;
    public static int CAMP_AGE_INCREMENT;
    public static int HUNT_GROUND_TOTAL_TIME;
    public static int HUNT_GROUND_AGE_INCREMENT;
    public static int PRIMITIVE_RANCH_WORK_TOTAL_TIME;
    public static int PRIMITIVE_RANCH_AGE_INCREMENT;
    public static int PRIMITIVE_RANCH_MAX_ANIMAL_COUNT;

    // ==================== 初始化 ====================

    private PopulationMachineConfig() {}

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
            throw new RuntimeException("Failed to initialize populationMachine config", e);
        }
    }

    private static void syncLegacyFields() {
        CAMP_WORK_TOTAL_TIME = getWorkTotalTime(CAMP);
        CAMP_AGE_INCREMENT = getAgeIncrement(CAMP);
        HUNT_GROUND_TOTAL_TIME = getWorkTotalTime(HUNTING_GROUND);
        HUNT_GROUND_AGE_INCREMENT = getAgeIncrement(HUNTING_GROUND);
        PRIMITIVE_RANCH_WORK_TOTAL_TIME = getWorkTotalTime(PRIMITIVE_RANCH);
        PRIMITIVE_RANCH_AGE_INCREMENT = getAgeIncrement(PRIMITIVE_RANCH);
        PRIMITIVE_RANCH_MAX_ANIMAL_COUNT = getMaxAnimalCount(PRIMITIVE_RANCH);
    }

    // ==================== 文件读写 ====================

    private static void writeDefaults() throws IOException {
        String defaults = """
                # CivilizationEvolution 人口机器配置
                # 工作周期单位为 tick（20 tick = 1 秒）

                camp:
                  # 营地完成一次工作所需的 tick 数（12000 tick = 10 分钟）
                  work_total_time: 12000
                  # 营地每次工作后每个人口的年龄增长量
                  age_increment: 1

                hunting_ground:
                  # 狩猎场完成一次工作所需的 tick 数（12000 tick = 10 分钟）
                  work_total_time: 12000
                  # 狩猎场每次工作后每个人口的年龄增长量
                  age_increment: 1

                primitive_ranch:
                  # 原始牧场完成一次工作所需的 tick 数（3000 tick = 2.5 分钟）
                  work_total_time: 3000
                  # 原始牧场每次工作后每个人口的年龄增长量
                  age_increment: 1
                  # 范围内最大动物数量，超过时取消当次工作（0 = 不限制）
                  max_animal_count: 24

                # 控制器配置
                primitive_settlement:
                  # 最大可绑定机器数量
                  max_bind_count: 10
                  # 最大绑定距离（格）
                  max_bind_range: 64
                  # 是否允许跨维度绑定（true/false）
                  allow_cross_dimension: false
                """;
        Files.writeString(CONFIG_FILE, defaults);
    }

    private static void load() throws IOException {
        String content = Files.readString(CONFIG_FILE);
        String[] lines = content.split("\\R");
        String currentSection = "";
        boolean isController = false;
        // 机器字段
        int workTotalTime = 0;
        int ageIncrement = 0;
        int maxAnimalCount = 0;
        // 控制器字段
        int maxBindCount = 0;
        int maxBindRange = 0;
        boolean allowCrossDimension = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            if (trimmed.endsWith(":")) {
                // 遇到新 section 时先保存上一个
                if (!currentSection.isEmpty()) {
                    saveSection(currentSection, isController,
                            workTotalTime, ageIncrement, maxAnimalCount,
                            maxBindCount, maxBindRange, allowCrossDimension);
                }
                currentSection = trimmed.substring(0, trimmed.length() - 1).trim();
                isController = false;
                workTotalTime = 12000;
                ageIncrement = 1;
                maxAnimalCount = 0;
                maxBindCount = 10;
                maxBindRange = 64;
                allowCrossDimension = false;
                continue;
            }

            int colon = trimmed.indexOf(':');
            if (colon == -1) continue;
            String key = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();

            switch (key) {
                case "work_total_time" -> workTotalTime = Integer.parseInt(value);
                case "age_increment" -> ageIncrement = Integer.parseInt(value);
                case "max_animal_count" -> maxAnimalCount = Integer.parseInt(value);
                case "max_bind_count" -> { maxBindCount = Integer.parseInt(value); isController = true; }
                case "max_bind_range" -> { maxBindRange = Integer.parseInt(value); isController = true; }
                case "allow_cross_dimension" -> { allowCrossDimension = Boolean.parseBoolean(value); isController = true; }
            }
        }

        // 保存最后一个 section
        if (!currentSection.isEmpty()) {
            saveSection(currentSection, isController,
                    workTotalTime, ageIncrement, maxAnimalCount,
                    maxBindCount, maxBindRange, allowCrossDimension);
        }
    }

    /** 根据 section 类型保存到对应的 Map */
    private static void saveSection(String name, boolean isController,
                                     int workTotalTime, int ageIncrement, int maxAnimalCount,
                                     int maxBindCount, int maxBindRange, boolean allowCrossDimension) {
        if (isController) {
            CONTROLLERS.put(name, new ControllerSection(maxBindCount, maxBindRange, allowCrossDimension));
        } else {
            SECTIONS.put(name, new MachineSection(workTotalTime, ageIncrement, maxAnimalCount));
        }
    }
}

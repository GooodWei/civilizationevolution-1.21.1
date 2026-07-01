package com.gooodwei.civilizationevolution.server.config;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从 careers.yml 加载职业树配置。
 * 首次启动时自动在 config/civilizationevolution/ 下生成默认文件。
 *
 * <p>配置格式（自定义行解析，与 populationMachine.yml 模式一致）：
 * <pre>{@code
 * unemployed:
 *   parent:
 *   tier: 0
 *   apprentice_exp_threshold: 8
 *
 * farmer:
 *   parent: unemployed
 *   tier: 1
 *   apprentice_exp_threshold: 8
 * }</pre>
 *
 * <p>初始化顺序（关键）：必须先通过 {@code new XxxCareer()} 构造所有 Career 实例，
 * 再调用 {@link #init()}，最后调用 {@link #applyToCareers()} 构建父子关系。
 */
public final class CareerConfig {

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("civilizationevolution");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("careers.yml");

    /** 单条职业配置项 */
    public record CareerEntry(String parent, int tier, int apprenticeExpThreshold) {}

    /** 职业配置表（按配置文件顺序保持 LinkedHashMap） */
    private static final Map<String, CareerEntry> ENTRIES = new LinkedHashMap<>();

    private CareerConfig() {}

    /**
     * 初始化配置：创建默认文件（如不存在）并加载。
     * 此方法应在所有 {@link Career} 子类实例化完成后调用。
     */
    public static void init() {
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefaults();
            }
            load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize careers config", e);
        }
    }

    /**
     * 将配置中的职业树结构应用到已注册的 {@link Career} 实例。
     * <ol>
     *   <li>第一遍：设置每个 Career 的 parentCareerName、tier、apprenticeExpThreshold</li>
     *   <li>第二遍：根据 parentCareerName 填充 children 列表</li>
     * </ol>
     */
    public static void applyToCareers() {
        // 第一遍：设置属性
        for (var entry : ENTRIES.entrySet()) {
            String name = entry.getKey();
            CareerEntry cfg = entry.getValue();
            Career career = Career.byName(name);
            if (career == null) continue;

            if (cfg.parent() != null && !cfg.parent().isEmpty()) {
                career.setParentCareerName(cfg.parent());
            }
            career.setTier(cfg.tier());
            career.setApprenticeExpThreshold(cfg.apprenticeExpThreshold());
        }

        // 第二遍：构建 children 列表
        for (var entry : ENTRIES.entrySet()) {
            String name = entry.getKey();
            CareerEntry cfg = entry.getValue();
            if (cfg.parent() == null || cfg.parent().isEmpty()) continue;

            Career child = Career.byName(name);
            Career parent = Career.byName(cfg.parent());
            if (child != null && parent != null) {
                parent.addChild(child);
            }
        }
    }

    // ==================== 文件读写 ====================

    private static void writeDefaults() throws IOException {
        String defaults = """
                # CivilizationEvolution 职业树配置
                # parent: 父职业名称（空 = 根职业）
                # tier: 职业等级（子职业 tier > 父职业 tier）
                # apprentice_exp_threshold: 晋升所需学徒经验（-1 = 不可晋升）

                unemployed:
                  parent:
                  tier: 0
                  apprentice_exp_threshold: -1

                nitwit:
                  parent:
                  tier: 0
                  apprentice_exp_threshold: -1

                armorer:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                butcher:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                cartographer:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                cleric:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                farmer:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                fisherman:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                fletcher:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                leatherworker:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                librarian:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                mason:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                miner:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                shepherd:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                toolsmith:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8

                weaponsmith:
                  parent: unemployed
                  tier: 1
                  apprentice_exp_threshold: 8
                """;
        Files.writeString(CONFIG_FILE, defaults);
    }

    private static void load() throws IOException {
        String content = Files.readString(CONFIG_FILE);
        String[] lines = content.split("\\R");
        String currentSection = "";
        String parent = null;
        int tier = 0;
        int apprenticeExpThreshold = 8;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

            if (trimmed.endsWith(":")) {
                // 遇到新 section 时先保存上一个
                if (!currentSection.isEmpty()) {
                    ENTRIES.put(currentSection, new CareerEntry(parent, tier, apprenticeExpThreshold));
                }
                currentSection = trimmed.substring(0, trimmed.length() - 1).trim();
                parent = null;
                tier = 0;
                apprenticeExpThreshold = 8;
                continue;
            }

            int colon = trimmed.indexOf(':');
            if (colon == -1) continue;
            String key = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();

            switch (key) {
                case "parent" -> {
                    if (!value.isEmpty()) {
                        parent = value;
                    }
                }
                case "tier" -> tier = Integer.parseInt(value);
                case "apprentice_exp_threshold" -> apprenticeExpThreshold = Integer.parseInt(value);
            }
        }

        // 保存最后一个 section
        if (!currentSection.isEmpty()) {
            ENTRIES.put(currentSection, new CareerEntry(parent, tier, apprenticeExpThreshold));
        }
    }
}

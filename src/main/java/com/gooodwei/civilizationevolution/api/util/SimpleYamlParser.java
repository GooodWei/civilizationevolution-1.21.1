package com.gooodwei.civilizationevolution.api.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 简易 YAML 配置解析器。
 *
 * <p>支持两级结构（section → key: value），兼容注释行（{@code #} 开头）和空行。
 * 不依赖任何第三方 YAML 库，仅使用标准 JDK API。
 *
 * <p>配置格式示例：
 * <pre>{@code
 * # 注释
 * section_name:
 *   key1: value1
 *   key2: value2
 *
 * another_section:
 *   key: value
 * }</pre>
 *
 * <p>返回的映射保持插入顺序（{@link LinkedHashMap}），与配置文件中的出现顺序一致。
 */
public final class SimpleYamlParser {

    private SimpleYamlParser() {}

    /**
     * 解析 YAML 配置文件，返回 section → key → value 的嵌套映射。
     *
     * @param filePath 配置文件路径
     * @return 保持插入顺序的嵌套映射（section 名称 → key-value 对）
     * @throws IOException 文件读取失败时抛出
     */
    public static Map<String, Map<String, String>> parse(Path filePath) throws IOException {
        String content = Files.readString(filePath);
        String[] lines = content.split("\\R");

        // 使用 LinkedHashMap 保持 section 和 key 的插入顺序
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        String currentSection = null;
        Map<String, String> currentMap = null;

        for (String line : lines) {
            String trimmed = line.trim();

            // 跳过空行和注释行
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }

            // 检测 section 头部（以 ":" 结尾的行）
            if (trimmed.endsWith(":")) {
                currentSection = trimmed.substring(0, trimmed.length() - 1).trim();
                currentMap = new LinkedHashMap<>();
                result.put(currentSection, currentMap);
                continue;
            }

            // 解析 key: value 对
            int colon = trimmed.indexOf(':');
            if (colon == -1 || currentMap == null) {
                continue;
            }
            String key = trimmed.substring(0, colon).trim();
            String value = trimmed.substring(colon + 1).trim();
            currentMap.put(key, value);
        }

        return result;
    }
}

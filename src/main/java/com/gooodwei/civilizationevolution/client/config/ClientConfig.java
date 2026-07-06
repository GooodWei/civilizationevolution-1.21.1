package com.gooodwei.civilizationevolution.client.config;

import com.gooodwei.civilizationevolution.api.util.SimpleYamlParser;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 客户端配置文件管理器。
 *
 * <p>从 {@code config/civilizationevolution/client.yml} 加载客户端渲染和行为配置。
 * 首次启动时自动生成带默认值的配置文件。
 *
 * <p>配置路径：{@code config/civilizationevolution/client.yml}
 *
 * <h3>配置项</h3>
 * <table>
 *   <tr><th>section</th><th>key</th><th>默认值</th><th>说明</th></tr>
 *   <tr><td>preview</td><td>render_distance</td><td>16</td>
 *       <td>多方块结构预览渲染半径（格），每客户端可根据硬件调节</td></tr>
 * </table>
 */
public final class ClientConfig {

    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("civilizationevolution");
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("client.yml");

    /** 多方块结构预览渲染半径（格），默认 16 格（33×33×33 视野盒） */
    private static int previewRenderDistance = 16;

    private static boolean initialized;

    private ClientConfig() {}

    // ==================== 初始化 ====================

    /**
     * 初始化客户端配置：读取配置文件，不存在则生成默认。
     * 应在客户端启动时调用（{@code FMLClientSetupEvent} 或构造器）。
     *
     * <p>可重复调用（幂等），仅首次执行实际初始化。
     */
    public static void init() {
        if (initialized) return;
        initialized = true;

        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefaults();
            }
            load();
        } catch (IOException e) {
            // 配置文件损坏时使用默认值，不阻止游戏启动
            System.err.println("[CivEvo-Client] 无法加载客户端配置，使用默认值: " + e.getMessage());
        }
    }

    // ==================== 访问器 ====================

    /** 获取多方块结构预览渲染半径（格）。默认 16。 */
    public static int getPreviewRenderDistance() {
        return previewRenderDistance;
    }

    // ==================== 文件读写 ====================

    /** 生成默认客户端配置文件 */
    private static void writeDefaults() throws IOException {
        String defaults = """
                # 文明演进模组 —— 客户端配置
                # 此文件仅影响当前客户端的渲染和行为，不影响服务端逻辑。

                preview:
                  # 多方块结构预览渲染半径（格）
                  # 玩家周围的视野盒范围，默认 16 格（33×33×33）。
                  # 降低可提升帧率，增加可看到更完整的结构轮廓。
                  render_distance: 16
                """;
        Files.writeString(CONFIG_FILE, defaults);
    }

    /** 加载并解析客户端配置文件 */
    private static void load() throws IOException {
        Map<String, Map<String, String>> sections = SimpleYamlParser.parse(CONFIG_FILE);
        for (var sectionEntry : sections.entrySet()) {
            String section = sectionEntry.getKey();
            Map<String, String> kv = sectionEntry.getValue();
            switch (section) {
                case "preview" -> kv.forEach(ClientConfig::loadPreview);
            }
        }
    }

    /** 解析 preview section 中的键值对 */
    private static void loadPreview(String key, String value) {
        switch (key) {
            case "render_distance" -> {
                try {
                    int val = Integer.parseInt(value);
                    if (val < 2) val = 2;       // 最低 2 格（5×5×5 视野盒）
                    if (val > 32) val = 32;     // 最高 32 格（65×65×65 视野盒）
                    previewRenderDistance = val;
                } catch (NumberFormatException ignored) {
                    // 配置值格式错误，保留默认值
                }
            }
        }
    }
}

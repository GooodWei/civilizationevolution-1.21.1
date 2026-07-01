package com.gooodwei.civilizationevolution.server.config;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多方块机器结构配置管理器。
 *
 * <p>从 {@code config/civilizationevolution/multi_blocks.json} 加载所有多方块机器的
 * JSON 结构定义。首次启动时自动生成默认配置文件（含 {@code primitive_doctor_cabin}）。
 *
 * <p>内部使用 {@link ConcurrentHashMap} 缓存原始 JSON 字符串（key → JSON），
 * 实际的 {@code ParsedPattern} 解析由各 BE 按需懒加载完成。
 *
 * <h3>配置文件格式</h3>
 * <pre>{@code
 * {
 *   "structures": {
 *     "primitive_doctor_cabin": {
 *       "controller": [0, 0, 0],
 *       "pattern": { "y0": "CX,XX", "y1": "XX,XX" },
 *       "validate_interval": 30,
 *       "key": {
 *         "C": {"block": "self"},
 *         "X": {"type": "multi_block_part", "alternatives": ["I","F","O"], "min_count": 4},
 *         "I": {"type": "input_hatch", "max_count": 1},
 *         "F": {"type": "food_hatch", "max_count": 1},
 *         "O": {"type": "output_hatch", "max_count": 1}
 *       }
 *     }
 *   }
 * }
 * }</pre>
 */
public final class MultiBlockConfig {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 配置文件目录 */
    private static final Path CONFIG_DIR = FMLPaths.CONFIGDIR.get().resolve("civilizationevolution");
    /** 配置文件路径 */
    private static final Path CONFIG_FILE = CONFIG_DIR.resolve("multi_blocks.json");

    /** 结构 key → 原始 JSON 字符串缓存 */
    private static final ConcurrentHashMap<String, String> STRUCTURE_CACHE = new ConcurrentHashMap<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private MultiBlockConfig() {}

    // ==================== 初始化 ====================

    /**
     * 初始化配置：读取配置文件，不存在则生成默认。
     * 应在 {@code CivilizationEvolution} 构造器中调用。
     */
    public static void init() {
        STRUCTURE_CACHE.clear();
        try {
            Files.createDirectories(CONFIG_DIR);
            if (!Files.exists(CONFIG_FILE)) {
                writeDefaults();
            }
            load();
        } catch (Exception e) {
            LOGGER.error("无法初始化多方块结构配置", e);
            throw new RuntimeException("Failed to initialize multi_blocks config", e);
        }
    }

    // ==================== 访问 ====================

    /**
     * 获取指定结构 key 的原始 JSON 字符串。
     *
     * @param key 结构标识名（如 {@code "primitive_doctor_cabin"}）
     * @return 原始 JSON 字符串，未找到时返回 null
     */
    public static String getStructureJson(String key) {
        return STRUCTURE_CACHE.get(key);
    }

    /**
     * 获取所有已加载的结构 key。
     *
     * @return 结构 key 集合的快照
     */
    public static java.util.Set<String> getStructureKeys() {
        return java.util.Set.copyOf(STRUCTURE_CACHE.keySet());
    }

    // ==================== 文件读写 ====================

    /** 生成默认配置文件 */
    private static void writeDefaults() throws IOException {
        JsonObject root = new JsonObject();
        JsonObject structures = new JsonObject();

        // ========== primitive_doctor_cabin ==========
        JsonObject doctorCabin = new JsonObject();

        // controller
        JsonArray controller = new JsonArray();
        controller.add(0); controller.add(0); controller.add(0);
        doctorCabin.add("controller", controller);

        // pattern
        JsonObject pattern = new JsonObject();
        pattern.addProperty("y0", "CX,XX");
        pattern.addProperty("y1", "XX,XX");
        doctorCabin.add("pattern", pattern);

        // 定时验证间隔（秒），默认 30 秒
        doctorCabin.addProperty("validate_interval", 30);

        // key
        JsonObject key = new JsonObject();

        JsonObject cDef = new JsonObject();
        cDef.addProperty("block", "self");
        key.add("C", cDef);

        JsonObject xDef = new JsonObject();
        xDef.addProperty("type", "multi_block_part");
        JsonArray xAlts = new JsonArray();
        xAlts.add("I"); xAlts.add("F"); xAlts.add("O");
        xDef.add("alternatives", xAlts);
        xDef.addProperty("min_count", 4);
        key.add("X", xDef);

        JsonObject iDef = new JsonObject();
        iDef.addProperty("type", "input_hatch");
        iDef.addProperty("max_count", 1);
        key.add("I", iDef);

        JsonObject fDef = new JsonObject();
        fDef.addProperty("type", "food_hatch");
        fDef.addProperty("max_count", 1);
        key.add("F", fDef);

        JsonObject oDef = new JsonObject();
        oDef.addProperty("type", "output_hatch");
        oDef.addProperty("max_count", 1);
        key.add("O", oDef);

        doctorCabin.add("key", key);

        structures.add("primitive_doctor_cabin", doctorCabin);

        // ========== primitive_controller ==========
        JsonObject primitiveController = new JsonObject();

        // controller 坐标 [y, x, z] — 顶层中心
        JsonArray ctrlArr = new JsonArray();
        ctrlArr.add(1); ctrlArr.add(1); ctrlArr.add(1);
        primitiveController.add("controller", ctrlArr);

        // pattern — 3×3×2（底层全铺外壳，顶层仅中心控制器）
        JsonObject ctrlPattern = new JsonObject();
        ctrlPattern.addProperty("y0", "ABA,BAB,ABA");
        ctrlPattern.addProperty("y1", "   , C ,   ");
        primitiveController.add("pattern", ctrlPattern);

        primitiveController.addProperty("validate_interval", 30);

        // key
        JsonObject ctrlKey = new JsonObject();

        // 石砖变体（单字符 alternatives，供 A/B 共享引用）
        JsonObject aAlt1 = new JsonObject();
        aAlt1.addProperty("type", "minecraft:cracked_stone_bricks");
        ctrlKey.add("a", aAlt1);

        JsonObject aAlt2 = new JsonObject();
        aAlt2.addProperty("type", "minecraft:mossy_stone_bricks");
        ctrlKey.add("b", aAlt2);

        JsonObject aAlt3 = new JsonObject();
        aAlt3.addProperty("type", "minecraft:chiseled_stone_bricks");
        ctrlKey.add("c", aAlt3);

        // A — 石砖，可替换为任意变体
        JsonObject aDef = new JsonObject();
        aDef.addProperty("type", "minecraft:stone_bricks");
        JsonArray aAlts = new JsonArray();
        aAlts.add("a"); aAlts.add("b"); aAlts.add("c");
        aDef.add("alternatives", aAlts);
        ctrlKey.add("A", aDef);

        // B — 同 A，共享变体引用
        JsonObject bDef = new JsonObject();
        bDef.addProperty("type", "minecraft:stone_bricks");
        JsonArray bAlts = new JsonArray();
        bAlts.add("a"); bAlts.add("b"); bAlts.add("c");
        bDef.add("alternatives", bAlts);
        ctrlKey.add("B", bDef);

        JsonObject cCtrlDef = new JsonObject();
        cCtrlDef.addProperty("block", "self");
        ctrlKey.add("C", cCtrlDef);

        primitiveController.add("key", ctrlKey);

        structures.add("primitive_controller", primitiveController);

        // ========== village_quarry ==========
        JsonObject villageQuarry = new JsonObject();

        // controller [y, x, z] — 顶层中心
        JsonArray vqCtrl = new JsonArray();
        vqCtrl.add(3); vqCtrl.add(3); vqCtrl.add(3);
        villageQuarry.add("controller", vqCtrl);

        // pattern — 7×4×7（所有非控制器位置统一用 A，仓室通过 alternatives 替换）
        JsonObject vqPattern = new JsonObject();
        vqPattern.addProperty("y0", "A     A,       ,       ,       ,       ,       ,A     A");
        vqPattern.addProperty("y1", "       , A   A ,       ,       ,       , A   A ,       ");
        vqPattern.addProperty("y2", "       ,       ,  AAA  ,  AAA  ,  AAA  ,       ,       ");
        vqPattern.addProperty("y3", "       ,       ,       ,   E   ,       ,       ,       ");
        villageQuarry.add("pattern", vqPattern);

        villageQuarry.addProperty("validate_interval", 30);

        // key — A 为村庄外壳（min 8），c/f/i/o 为仓室替代（靠 min_count 保证最低数量）
        JsonObject vqKey = new JsonObject();

        JsonObject vqADef = new JsonObject();
        vqADef.addProperty("type", "civilizationevolution:village_structure_casing");
        vqADef.addProperty("min_count", 8);
        JsonArray vqAAlts = new JsonArray();
        vqAAlts.add("c"); vqAAlts.add("f"); vqAAlts.add("i"); vqAAlts.add("o");
        vqADef.add("alternatives", vqAAlts);
        vqKey.add("A", vqADef);

        JsonObject vqCDef = new JsonObject();
        vqCDef.addProperty("type", "fluid_input_hatch");
        vqCDef.addProperty("min_count", 2);
        vqKey.add("c", vqCDef);

        JsonObject vqFDef = new JsonObject();
        vqFDef.addProperty("type", "food_hatch");
        vqFDef.addProperty("min_count", 1);
        vqKey.add("f", vqFDef);

        JsonObject vqIDef = new JsonObject();
        vqIDef.addProperty("type", "input_hatch");
        vqIDef.addProperty("min_count", 1);
        vqKey.add("i", vqIDef);

        JsonObject vqODef = new JsonObject();
        vqODef.addProperty("type", "output_hatch");
        vqODef.addProperty("min_count", 1);
        vqKey.add("o", vqODef);

        JsonObject vqEDef = new JsonObject();
        vqEDef.addProperty("block", "self");
        vqKey.add("E", vqEDef);

        villageQuarry.add("key", vqKey);
        structures.add("village_quarry", villageQuarry);

        // ========== village_controller ==========
        // 9×9×7 庙宇/议事厅结构，全部使用原版方块
        JsonObject villageCtrl = new JsonObject();

        JsonArray vcCtrl = new JsonArray();
        vcCtrl.add(1); vcCtrl.add(4); vcCtrl.add(4);
        villageCtrl.add("controller", vcCtrl);

        JsonObject vcPattern = new JsonObject();
        vcPattern.addProperty("y0", "  ABBBA  ,  ABBBA  ,AAABBBAAA,BBBADABBB,BBBDADBBB,BBBADABBB,AAABBBAAA,  ABBBA  ,  ABBBA  ");
        vcPattern.addProperty("y1", "  A   A  ,         ,A       A,         ,    F    ,         ,A       A,         ,  A   A  ");
        vcPattern.addProperty("y2", "  A   A  ,         ,A       A,         ,         ,         ,A       A,         ,  A   A  ");
        vcPattern.addProperty("y3", "  A   A  ,         ,A       A,         ,         ,         ,A       A,         ,  A   A  ");
        vcPattern.addProperty("y4", "   ACA   ,  A   A  , AA   AA ,A       A,C       C,A       A, AA   AA ,  A   A  ,   ACA   ");
        vcPattern.addProperty("y5", "    A    ,   CCC   ,  AAAAA  , CA   AC ,ACA   ACA, CA   AC ,  AAAAA  ,   CCC   ,    A    ");
        vcPattern.addProperty("y6", "         ,    A    ,    A    ,   CEC   , AAECEAA ,   CEC   ,    A    ,    A    ,         ");
        villageCtrl.add("pattern", vcPattern);

        villageCtrl.addProperty("validate_interval", 30);

        JsonObject vcKey = new JsonObject();

        JsonObject vcADef = new JsonObject();
        vcADef.addProperty("type", "minecraft:stone_bricks");
        vcKey.add("A", vcADef);

        JsonObject vcBDef = new JsonObject();
        vcBDef.addProperty("type", "minecraft:oak_planks");
        vcKey.add("B", vcBDef);

        JsonObject vcCDef = new JsonObject();
        vcCDef.addProperty("type", "minecraft:glass");
        vcKey.add("C", vcCDef);

        JsonObject vcDDef = new JsonObject();
        vcDDef.addProperty("type", "minecraft:chiseled_stone_bricks");
        vcKey.add("D", vcDDef);

        JsonObject vcEDef = new JsonObject();
        vcEDef.addProperty("type", "minecraft:emerald_block");
        vcKey.add("E", vcEDef);

        JsonObject vcFDef = new JsonObject();
        vcFDef.addProperty("block", "self");
        vcKey.add("F", vcFDef);

        villageCtrl.add("key", vcKey);
        structures.add("village_controller", villageCtrl);

        root.add("structures", structures);

        String json = GSON.toJson(root);
        Files.writeString(CONFIG_FILE, json);
        LOGGER.info("已生成默认多方块结构配置：{}", CONFIG_FILE);
    }

    /** 加载配置文件到缓存 */
    private static void load() throws IOException {
        String content = Files.readString(CONFIG_FILE);
        JsonObject root;
        try {
            root = GSON.fromJson(content, JsonObject.class);
        } catch (JsonParseException e) {
            LOGGER.error("多方块结构配置文件 JSON 解析失败：{}", e.getMessage());
            throw new IOException("Invalid JSON in multi_blocks config", e);
        }

        if (root == null || !root.has("structures")) {
            LOGGER.warn("多方块结构配置文件缺少 \"structures\" 顶层键，没有结构被加载");
            return;
        }

        JsonObject structures = root.getAsJsonObject("structures");
        for (Map.Entry<String, JsonElement> entry : structures.entrySet()) {
            String key = entry.getKey();
            String jsonStr = entry.getValue().toString();
            STRUCTURE_CACHE.put(key, jsonStr);
        }

        LOGGER.info("已从 {} 加载 {} 个多方块结构", CONFIG_FILE, STRUCTURE_CACHE.size());
    }
}

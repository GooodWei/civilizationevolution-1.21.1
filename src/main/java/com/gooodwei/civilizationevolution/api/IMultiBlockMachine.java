package com.gooodwei.civilizationevolution.api;

import com.google.gson.*;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.config.MultiBlockConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 多方块机器接口 —— 将多方块结构的 JSON 解析、结构验证、定时重新验证等
 * 通用逻辑提取为 default 方法，供任何 {@link BlockEntity} 实现。
 *
 * <h3>设计目标</h3>
 * <ol>
 *   <li><b>解耦继承链</b>：不强制继承 {@code AbstractMachineBlockEntity}
 *       （及其 {@code IPopulationMachine} 接口），控制器可直接实现本接口</li>
 *   <li><b>状态封装</b>：多方块相关字段集中在 {@link MultiBlockState} 组合对象中，
 *       通过 {@link #mbs()} 访问</li>
 *   <li><b>向后兼容</b>：已有 {@code AbstractMultiBlockMachineBlockEntity} 及其子类零改动</li>
 * </ol>
 *
 * <h3>实现步骤（最小实现）</h3>
 * <ol>
 *   <li>持有 {@code private final MultiBlockState mbs = new MultiBlockState()}</li>
 *   <li>覆写 {@link #mbs()} 返回该字段</li>
 *   <li>覆写 {@link #getConfigKey()} 返回配置文件中的结构 key</li>
 *   <li>覆写 {@link #markChanged()} 调用 {@code setChanged()}</li>
 *   <li>覆写 {@link #dropAllItems()} 弹出容器内所有物品</li>
 *   <li>在 {@code saveAdditional / loadAdditional} 中调用
 *       {@link #saveMultiBlockNBT}/{@link #loadMultiBlockNBT}</li>
 *   <li>在 {@code onLoad} 中调用 {@link #onMultiBlockLoad()}</li>
 *   <li>在服务端 tick 中调用 {@link #tickRevalidation()}</li>
 * </ol>
 *
 * <h3>JSON 结构定义格式</h3>
 * <p>同 {@link MultiBlockConfig}，坐标系统：x = right，y = up，z = backward（facing 反方向）。
 * 空格字符表示该位置可为任意方块。
 *
 * @see MultiBlockState
 * @see IMultiBlockPart
 * @see MultiBlockConfig
 */
public interface IMultiBlockMachine {

    // ==================== 常量 ====================

    /** 默认定时验证间隔（tick），当配置未指定或解析失败时使用 */
    int DEFAULT_VALIDATE_INTERVAL_TICKS = 600; // 30 秒

    /**
     * 结构验证服务实现 —— 由服务端初始化代码注入。
     *
     * <p>定义在 {@link IStructureValidationService} 接口中，避免 api/ 层直接依赖
     * server/validation/ 包。服务端启动时通过
     * {@code IMultiBlockMachine.VALIDATION_SERVICE.set(StructureValidationService::submitPeriodicValidation)}
     * 注册。
     *
     * <p>使用 {@link java.util.concurrent.atomic.AtomicReference} 包装以绕过接口字段的隐式 final 限制。
     */
    java.util.concurrent.atomic.AtomicReference<IStructureValidationService> VALIDATION_SERVICE =
            new java.util.concurrent.atomic.AtomicReference<>();

    // ==================== 内部 Record ====================

    /**
     * key 条目的解析结果。
     *
     * @param type         零件类型字符串（如 {@code "multi_block_part"}）
     * @param alternatives 可替换的 key 字符列表（空列表 = 无 alternatives）
     * @param minCount     最低数量要求（-1 = 无限制）
     * @param maxCount     最高数量限制（{@link Integer#MAX_VALUE} = 无限制）
     */
    record KeyDefinition(
            String type,
            List<Character> alternatives,
            int minCount,
            int maxCount
    ) {}

    /**
     * 解析后的结构模式。
     *
     * @param width                X 方向宽度（每行字符数）
     * @param height               Y 方向高度（yN 键数量）
     * @param depth                Z 方向深度（逗号分隔的行数）
     * @param controllerY          控制器 Y 层索引
     * @param controllerX          控制器 X 列索引
     * @param controllerZ          控制器 Z 行索引
     * @param layerChars           [y][z][x] 三维字符数组，空格=' '表示任意方块
     * @param key                  字符 → 原始 JSON，保留兼容旧逻辑
     * @param keyDefs              字符 → 解析后的 KeyDefinition（含 type、alternatives、min/max）
     * @param validateIntervalTicks 定时验证间隔（tick），由配置文件中的 validate_interval（秒）转换
     * @param shareable             结构零件是否可被多个控制器共用（默认 true，未配置时最宽松策略）
     */
    record ParsedPattern(
            int width, int height, int depth,
            int controllerY, int controllerX, int controllerZ,
            char[][][] layerChars,
            Map<Character, JsonObject> key,
            Map<Character, KeyDefinition> keyDefs,
            int validateIntervalTicks,
            boolean shareable
    ) {}

    // ==================== 抽象方法（由 BlockEntity 提供） ====================

    /** 获取所在世界。BlockEntity 已有此方法 */
    @Nullable
    Level getLevel();

    /** 获取方块坐标。BlockEntity 已有此方法 */
    BlockPos getBlockPos();

    /** 获取方块状态。BlockEntity 已有此方法 */
    BlockState getBlockState();

    /** 获取此机器的 Tier 等级。控制器和人口机器均已有此方法 */
    Tier getTier();

    // ==================== 抽象方法（子类实现） ====================

    /**
     * 返回 {@link MultiBlockConfig} 中对应的结构标识 key。
     *
     * @return 配置文件中的结构 key（如 {@code "primitive_doctor_cabin"}）
     */
    String getConfigKey();

    /** @return 此机器的多方块状态对象 */
    MultiBlockState mbs();

    /** 标记方块实体数据已变更（调用 {@code setChanged()}） */
    void markChanged();

    /** 弹出容器内所有物品到控制器所在世界位置 */
    void dropAllItems();

    // ==================== 方向 ====================

    /**
     * 获取控制器/机器方块的正面朝向。
     *
     * <p>默认从方块状态的 {@link BlockStateProperties#HORIZONTAL_FACING} 属性读取。
     * 此属性由 {@code AbstractMachineBlock.FACING} 定义，控制器通过
     * {@code AbstractControllerBlock extends AbstractMachineBlock} 继承。
     *
     * @return 水平朝向（北/南/西/东）
     */
    default Direction getFacingDirection() {
        return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    // ==================== 坐标旋转 ====================

    /**
     * 将局部坐标 (x, y, z) 根据 facing 旋转为世界坐标偏移。
     *
     * <p>局部坐标系：x = right（facing 顺时针方向），y = up，
     * z = backward（facing 反方向，即结构在控制器背后延伸）。
     *
     * @param x      局部 X（right 方向，pattern 行内字符列）
     * @param y      局部 Y（up 方向，yN 层索引）
     * @param z      局部 Z（backward 方向，逗号分隔的行索引）
     * @param facing 控制器朝向（正面，面向玩家）
     * @return 相对于控制器位置的世界坐标偏移
     */
    default BlockPos rotateOffset(int x, int y, int z, Direction facing) {
        Direction right = facing.getClockWise();
        // Z 取反：结构在控制器背后（facing 反方向）延伸
        return new BlockPos(
                x * right.getStepX() - z * facing.getStepX(),
                y,
                x * right.getStepZ() - z * facing.getStepZ()
        );
    }

    /**
     * 获取指定局部坐标的世界绝对位置。
     *
     * @param x      局部 X
     * @param y      局部 Y
     * @param z      局部 Z
     * @param facing 控制器朝向
     * @return 世界绝对坐标
     */
    default BlockPos getWorldPos(int x, int y, int z, Direction facing) {
        return getBlockPos().offset(rotateOffset(x, y, z, facing));
    }

    /**
     * 将局部坐标 (lx, ly, lz) 根据 facing 旋转为世界绝对坐标。
     *
     * <p>静态版本，供 {@code StructureValidationService} 和 {@code CivilizationCommand}
     * 等非实例上下文使用。逻辑与 {@link #rotateOffset} + {@code controllerPos.offset(...)} 一致。
     *
     * @param lx            局部 X（right 方向）
     * @param ly            局部 Y（up 方向）
     * @param lz            局部 Z（backward 方向）
     * @param facing        结构正面朝向
     * @param controllerPos 控制器世界坐标
     * @return 世界绝对坐标
     */
    static BlockPos worldPosFromLocal(int lx, int ly, int lz,
                                       Direction facing, BlockPos controllerPos) {
        Direction right = facing.getClockWise();
        return controllerPos.offset(
                lx * right.getStepX() - lz * facing.getStepX(),
                ly,
                lx * right.getStepZ() - lz * facing.getStepZ());
    }

    // ==================== JSON 解析 ====================

    /**
     * 解析 {@link MultiBlockConfig} 中对应结构 key 的 JSON 字符串。
     *
     * <p>首次调用时解析并缓存为 {@link ParsedPattern}，后续直接返回缓存。
     * 解析失败时设置 {@link MultiBlockState#parseError} 并返回 null。
     *
     * @return 解析后的结构模式，解析失败时返回 null
     */
    default ParsedPattern parsePattern() {
        MultiBlockState state = mbs();
        if (state.cachedPattern != null) return state.cachedPattern;

        String configKey = getConfigKey();
        String json = MultiBlockConfig.getStructureJson(configKey);

        // 配置文件未加载或 key 不存在
        if (json == null) {
            state.parseError = "多方块结构配置 \"" + configKey + "\" 未在配置文件中找到，请检查 config/civilizationevolution/multi_blocks.json";
            return null;
        }

        JsonObject root;
        try {
            root = new Gson().fromJson(json, JsonObject.class);
        } catch (JsonParseException e) {
            state.parseError = "多方块结构 \"" + configKey + "\" JSON 解析失败：" + e.getMessage();
            return null;
        }

        try {
            state.cachedPattern = parsePatternFromJson(root);
            state.parseError = null; // 解析成功，清除错误
            return state.cachedPattern;

        } catch (JsonParseException | NumberFormatException e) {
            state.parseError = "多方块结构 \"" + configKey + "\" 解析失败：" + e.getMessage();
            return null;
        }
    }

    /**
     * 静态方法：解析任意结构 key 的 JSON，返回 ParsedPattern。
     * 不依赖 BlockEntity 实例，不缓存到 {@link MultiBlockState}。
     *
     * @param structureKey 配置文件中的结构 key（如 {@code "village_controller"}）
     * @return 解析后的结构模式，失败时返回 null
     */
    @Nullable
    static ParsedPattern parsePatternStatic(String structureKey) {
        String json = MultiBlockConfig.getStructureJson(structureKey);
        if (json == null) return null;

        JsonObject root;
        try {
            root = new Gson().fromJson(json, JsonObject.class);
        } catch (JsonParseException e) {
            return null;
        }

        try {
            return parsePatternFromJson(root);
        } catch (JsonParseException | NumberFormatException e) {
            return null;
        }
    }

    /**
     * 核心解析逻辑：从 JSON 对象解析为 ParsedPattern。
     * 纯函数，无副作用。从 {@link #parsePattern()} 提取以支持静态调用。
     *
     * @param root 结构的 JSON 根对象
     * @return 解析后的结构模式
     * @throws JsonParseException 如果 JSON 结构不合法
     * @throws NumberFormatException 如果数字字段格式错误
     */
    public static ParsedPattern parsePatternFromJson(JsonObject root) {
        // 1. 解析 controller 坐标 [y, x, z]
        JsonArray controllerArr = root.getAsJsonArray("controller");
        if (controllerArr == null || controllerArr.size() != 3)
            throw new JsonParseException("controller 必须是 [y, x, z] 三个整数的数组");
        int controllerY = controllerArr.get(0).getAsInt();
        int controllerX = controllerArr.get(1).getAsInt();
        int controllerZ = controllerArr.get(2).getAsInt();

        // 解析 code_width（debug 结构提取器在双字节模式下写入，默认 1）
        int codeWidth = root.has("code_width") ? root.get("code_width").getAsInt() : 1;
        if (codeWidth < 1)
            throw new JsonParseException("code_width 必须 >= 1，当前: " + codeWidth);

        // 2. 解析 pattern 对象（按 y0, y1, ... 排序 key）
        JsonObject patternObj = root.getAsJsonObject("pattern");
        if (patternObj == null)
            throw new JsonParseException("缺少 \"pattern\" 对象");
        List<Map.Entry<String, JsonElement>> sortedLayers = new ArrayList<>();
        for (Map.Entry<String, JsonElement> entry : patternObj.entrySet()) {
            sortedLayers.add(entry);
        }
        // 按 yN 的数字排序
        sortedLayers.sort(Comparator.comparingInt(e -> {
            String key = e.getKey();
            if (!key.startsWith("y"))
                throw new JsonParseException("pattern 的 key 必须以 'y' 开头，当前: " + key);
            return Integer.parseInt(key.substring(1));
        }));

        int height = sortedLayers.size();
        if (height == 0)
            throw new JsonParseException("pattern 至少需要一个 yN 层");

        // 验证 yN 从 0 开始且无跳跃
        for (int i = 0; i < height; i++) {
            String key = sortedLayers.get(i).getKey();
            int n = Integer.parseInt(key.substring(1));
            if (n != i)
                throw new JsonParseException("y 层必须连续从 0 开始，期望 y" + i + "，实际 " + key);
        }

        // 3. 解析每层的行（支持 code_width > 1 的多字符 token）
        List<String[]> allRows = new ArrayList<>();
        int rawWidth = -1; // 原始行字符宽度（code_width * 实际宽度）
        int width = -1;    // 实际逻辑宽度（单元格数）
        int depth = -1;

        for (Map.Entry<String, JsonElement> entry : sortedLayers) {
            String layerStr = entry.getValue().getAsString();
            String[] rows = layerStr.split(",", -1);
            allRows.add(rows);

            if (depth == -1) {
                depth = rows.length;
            } else if (rows.length != depth) {
                throw new JsonParseException(
                        "y 层 " + entry.getKey() + " 有 " + rows.length + " 行，期望 " + depth);
            }
            if (depth == 0)
                throw new JsonParseException("每层至少需要 1 行");

            for (String row : rows) {
                int rowLen = row.length();
                if (codeWidth > 1 && rowLen % codeWidth != 0) {
                    throw new JsonParseException(
                            "行 \"" + row + "\" 长度 " + rowLen + " 不能被 code_width(" + codeWidth + ") 整除");
                }
                int logicalWidth = codeWidth > 1 ? rowLen / codeWidth : rowLen;
                if (rawWidth == -1) {
                    rawWidth = rowLen;
                    width = logicalWidth;
                } else if (rowLen != rawWidth) {
                    throw new JsonParseException(
                            "行 \"" + row + "\" 长度为 " + rowLen + "，期望 " + rawWidth);
                }
            }
        }

        // token → 代理单字符的映射（仅 code_width > 1 时使用）
        java.util.Map<String, Character> tokenToSurrogate = new LinkedHashMap<>();
        int nextSurrogate = 0;
        // 全下划线 token（导出时的空气占位符）映射到空格
        tokenToSurrogate.put("_".repeat(Math.max(1, codeWidth)), ' ');

        // 4. 构建三维字符数组 [y][z][x]（code_width > 1 时用代理字符）
        char[][][] layerChars = new char[height][depth][width];
        for (int y = 0; y < height; y++) {
            String[] rows = allRows.get(y);
            for (int z = 0; z < depth; z++) {
                String row = rows[z];
                for (int x = 0; x < width; x++) {
                    if (codeWidth > 1) {
                        String token = row.substring(x * codeWidth, (x + 1) * codeWidth);
                        // 全下划线 → 空气
                        boolean allUnderscore = true;
                        for (int i = 0; i < token.length(); i++) {
                            if (token.charAt(i) != '_') { allUnderscore = false; break; }
                        }
                        if (allUnderscore) {
                            layerChars[y][z][x] = ' ';
                        } else {
                            Character surrogate = tokenToSurrogate.get(token);
                            if (surrogate == null) {
                                surrogate = (char) (0xE000 + nextSurrogate++);
                                tokenToSurrogate.put(token, surrogate);
                            }
                            layerChars[y][z][x] = surrogate;
                        }
                    } else {
                        layerChars[y][z][x] = row.charAt(x);
                    }
                }
            }
        }

        // 5. 解析 key 映射 → JsonObject + KeyDefinition
        JsonObject keyObj = root.getAsJsonObject("key");
        if (keyObj == null)
            throw new JsonParseException("缺少 \"key\" 对象");
        Map<Character, JsonObject> key = new LinkedHashMap<>();
        Map<Character, KeyDefinition> keyDefs = new LinkedHashMap<>();

        for (Map.Entry<String, JsonElement> entry : keyObj.entrySet()) {
            String keyStr = entry.getKey();
            final char c;
            if (codeWidth > 1) {
                // 多字符 key：从 token→代理字符 映射中查找
                Character surrogate = tokenToSurrogate.get(keyStr);
                if (surrogate == null || surrogate == ' ') {
                    throw new JsonParseException("key \"" + keyStr + "\" 未在 pattern 中出现，无法映射");
                }
                c = surrogate;
            } else {
                if (keyStr.length() != 1)
                    throw new JsonParseException("key 的键必须是单字符，当前: " + keyStr);
                c = keyStr.charAt(0);
                if (c == ' ')
                    throw new JsonParseException("空格字符不能用作 key 定义");
            }
            JsonObject def = entry.getValue().getAsJsonObject();
            key.put(c, def);

            // 解析 KeyDefinition
            String type = def.has("type") ? def.get("type").getAsString() : "multi_block_part";

            // 解析 alternatives
            List<Character> alternatives = new ArrayList<>();
            if (def.has("alternatives")) {
                JsonArray altArr = def.getAsJsonArray("alternatives");
                for (JsonElement altElem : altArr) {
                    String altStr = altElem.getAsString();
                    final char altChar;
                    if (codeWidth > 1) {
                        Character altSurrogate = tokenToSurrogate.get(altStr);
                        if (altSurrogate == null || altSurrogate == ' ') {
                            throw new JsonParseException("key '" + keyStr + "' 的 alternatives 引用 \""
                                    + altStr + "\" 未在 pattern 中出现");
                        }
                        altChar = altSurrogate;
                    } else {
                        if (altStr.length() != 1)
                            throw new JsonParseException("alternatives 中的值必须是单字符，当前: " + altStr);
                        altChar = altStr.charAt(0);
                    }
                    alternatives.add(altChar);
                }
            }

            // 解析 min_count / max_count（-1 = 无限制）
            int minCount = def.has("min_count") ? def.get("min_count").getAsInt() : -1;
            int maxCount = def.has("max_count") ? def.get("max_count").getAsInt() : Integer.MAX_VALUE;

            // 验证
            if (minCount <= 0 && def.has("min_count")) {
                throw new JsonParseException("key '" + c + "' 的 min_count 必须大于 0，当前: " + minCount);
            }
            if (maxCount <= 0 && def.has("max_count")) {
                throw new JsonParseException("key '" + c + "' 的 max_count 必须大于 0，当前: " + maxCount);
            }
            if (minCount > maxCount) {
                throw new JsonParseException("key '" + c + "' 的 min_count(" + minCount +
                        ") 不能大于 max_count(" + maxCount + ")");
            }

            keyDefs.put(c, new KeyDefinition(type, Collections.unmodifiableList(alternatives), minCount, maxCount));
        }

        // 6. 验证 alternatives 引用的字符在 key 中有定义
        for (Map.Entry<Character, KeyDefinition> entry : keyDefs.entrySet()) {
            for (char alt : entry.getValue().alternatives) {
                if (!keyDefs.containsKey(alt)) {
                    throw new JsonParseException("key '" + entry.getKey() + "' 的 alternatives 引用 '"
                            + alt + "' 未在 key 中定义");
                }
                // 阻止循环引用：alternative 指向的目标 key 自身不能有 alternatives
                if (!keyDefs.get(alt).alternatives.isEmpty()) {
                    throw new JsonParseException("key '" + alt + "' 有 alternatives，不能作为 '" +
                            entry.getKey() + "' 的 alternatives 目标（不支持嵌套 alternatives）");
                }
            }
        }

        // 7. 验证 controller 坐标有效
        if (controllerY < 0 || controllerY >= height ||
                controllerX < 0 || controllerX >= width ||
                controllerZ < 0 || controllerZ >= depth) {
            throw new JsonParseException("controller 坐标 [" + controllerY + "," + controllerX +
                    "," + controllerZ + "] 超出范围 width=" + width + " height=" + height + " depth=" + depth);
        }
        char controllerChar = layerChars[controllerY][controllerZ][controllerX];
        if (controllerChar == ' ') {
            throw new JsonParseException("controller 坐标 [" + controllerY + "," + controllerX +
                    "," + controllerZ + "] 为空格（任意方块），不允许");
        }
        if (!key.containsKey(controllerChar)) {
            throw new JsonParseException("controller 字符 '" + controllerChar + "' 未在 key 中定义");
        }

        // 8. 验证所有非空格字符在 key 中有定义
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    char c = layerChars[y][z][x];
                    if (c != ' ' && !key.containsKey(c)) {
                        throw new JsonParseException(
                                "位置 [" + y + "," + x + "," + z + "] 的字符 '" + c + "' 未在 key 中定义");
                    }
                }
            }
        }

        // 9. 验证 min_count 可行性：pattern 中该字符的出现次数 >= min_count
        // 仅检查 pattern 中直接出现的 key（alternatives-only 的 key 在运行时通过替换满足 min_count）
        Map<Character, Integer> charCounts = new HashMap<>();
        for (int y = 0; y < height; y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    char c = layerChars[y][z][x];
                    if (c != ' ') {
                        charCounts.merge(c, 1, Integer::sum);
                    }
                }
            }
        }
        for (Map.Entry<Character, KeyDefinition> entry : keyDefs.entrySet()) {
            char c = entry.getKey();
            KeyDefinition kd = entry.getValue();
            int patternCount = charCounts.getOrDefault(c, 0);
            // 跳过 pattern 中未直接出现的 key（纯 alternatives，运行时满足）
            if (patternCount == 0) continue;
            if (kd.minCount > 0 && patternCount < kd.minCount) {
                throw new JsonParseException("key '" + c + "' 的 min_count=" + kd.minCount +
                        " 但 pattern 中仅出现 " + patternCount + " 次");
            }
        }

        // 10. 解析 validate_interval（秒 → tick，默认 30 秒 = 600 tick）
        int validateIntervalTicks = DEFAULT_VALIDATE_INTERVAL_TICKS;
        if (root.has("validate_interval")) {
            int seconds = root.get("validate_interval").getAsInt();
            if (seconds <= 0)
                throw new JsonParseException("validate_interval 必须大于 0，当前: " + seconds);
            validateIntervalTicks = seconds * 20;
        }

        // 11. 解析 shareable（结构零件是否可被多个控制器共用，默认 true）
        boolean shareable = true;
        if (root.has("shareable")) {
            shareable = root.get("shareable").getAsBoolean();
        }

        return new ParsedPattern(width, height, depth,
                controllerY, controllerX, controllerZ, layerChars, key, keyDefs, validateIntervalTicks, shareable);
    }

    /** 清除缓存的解析结果（配置变更时调用） */
    default void invalidatePatternCache() {
        MultiBlockState state = mbs();
        state.cachedPattern = null;
        state.parseError = null;
    }

    // ==================== 零件解析辅助方法 ====================

    /**
     * 从指定位置解析 {@link IMultiBlockPart}。
     * 先检查 BlockEntity，再检查 Block 自身（支持无 BE 的外壳方块）。
     *
     * @param pos 世界坐标
     * @return 该位置的 IMultiBlockPart 实例，若均不匹配则返回 null
     */
    @Nullable
    default IMultiBlockPart resolveMultiBlockPart(BlockPos pos) {
        Level level = getLevel();
        if (level == null) return null;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IMultiBlockPart part) return part;
        if (level.getBlockState(pos).getBlock() instanceof IMultiBlockPart part) return part;
        return null;
    }

    /**
     * 构建给定字符在该位置允许的零件类型集合。
     *
     * <p>集合包含：
     * <ol>
     *   <li>该字符自身 key 定义的 type</li>
     *   <li>该字符 alternatives 列表中所有目标字符的 type</li>
     * </ol>
     *
     * @param c       pattern 中出现的字符
     * @param pattern 解析后的结构模式
     * @return 允许的零件类型集合
     */
    static Set<String> buildAllowedTypes(char c, ParsedPattern pattern) {
        Set<String> allowed = new LinkedHashSet<>();
        KeyDefinition kd = pattern.keyDefs.get(c);
        if (kd == null) return allowed;

        allowed.add(kd.type);
        for (char alt : kd.alternatives) {
            KeyDefinition altDef = pattern.keyDefs.get(alt);
            if (altDef != null) {
                allowed.add(altDef.type);
            }
        }
        return allowed;
    }

    /**
     * 根据实际零件类型找到匹配的 key 字符。
     *
     * <p>先检查 pattern 字符自身，再检查其 alternatives。
     * 用于 min_count / max_count 计数时确定增量到哪个 key。
     *
     * @param patternChar pattern 中该位置的原始字符
     * @param matchedTypeStr  该位置实际零件的类型
     * @param pattern     解析后的结构模式
     * @return 匹配的 key 字符
     */
    static Character findMatchingKey(char patternChar, @Nullable String matchedTypeStr, ParsedPattern pattern) {
        KeyDefinition selfDef = pattern.keyDefs.get(patternChar);
        if (selfDef != null && selfDef.type.equals(matchedTypeStr)) {
            return patternChar;
        }
        // 在 alternatives 中查找
        if (selfDef != null) {
            for (char alt : selfDef.alternatives) {
                KeyDefinition altDef = pattern.keyDefs.get(alt);
                if (altDef != null && altDef.type.equals(matchedTypeStr)) {
                    return alt;
                }
            }
        }
        // fallback：返回原始字符
        return patternChar;
    }

    /**
     * 判断 type 字符串是否为方块注册名匹配模式（含 ":" 且非 "tag:" 开头）。
     *
     * <p>与 {@link #isTagType(String)} 互斥，两者覆盖所有 type 分类场景。
     * 例如 {@code "minecraft:stone_bricks"} → true，{@code "tag:minecraft:stone_bricks"} → false。
     */
    static boolean isBlockOrTagType(String type) {
        return type.indexOf(':') >= 0 && !type.startsWith("tag:");
    }

    /**
     * 判断 type 字符串是否为 Block Tag 匹配模式。
     *
     * <p>规则：以 {@code "tag:"} 开头。
     * 例如 {@code "tag:minecraft:stone_bricks"} → true。
     */
    static boolean isTagType(String type) {
        return type.startsWith("tag:");
    }

    // ==================== 预览数据收集 ====================

    /**
     * 收集多方块结构所有非空格、非控制器自身位置的预览数据。
     *
     * <p>遍历已解析的 {@link ParsedPattern#layerChars}，跳过：
     * <ul>
     *   <li>空格字符（{@code ' '}）—— 无方块要求的位置</li>
     *   <li>{@code "block": "self"} 的 key —— 控制器自身所在位置</li>
     * </ul>
     *
     * <p>对每个有效位置调用 {@link #getWorldPos(int, int, int, Direction)}
     * 计算世界坐标，通过 {@link #buildAllowedTypes(char, ParsedPattern)}
     * 收集该位置接受的所有 type 字符串。
     *
     * <p>仅在<b>服务端</b>调用，用于构造发送到客户端的
     * {@code StructurePreviewPayload}。
     *
     * @return 预览位置数据列表（未解析到 pattern 时返回空列表）
     */
    default List<PreviewBlockInfo> collectPreviewPositions() {
        ParsedPattern pattern = parsePattern();
        if (pattern == null) return List.of();

        Direction facing = getFacingDirection();
        List<PreviewBlockInfo> result = new java.util.ArrayList<>();

        for (int y = 0; y < pattern.height; y++) {
            for (int z = 0; z < pattern.depth; z++) {
                for (int x = 0; x < pattern.width; x++) {
                    char c = pattern.layerChars[y][z][x];
                    if (c == ' ') continue;

                    KeyDefinition kd = pattern.keyDefs.get(c);
                    if (kd == null) continue;

                    // 跳过控制器自身（"block": "self"）
                    // 注意：parsePattern 对 "block" 字段不设置 type，需检查原始 JSON
                    if ("self".equals(kd.type)) continue;
                    JsonObject rawKeyDef = pattern.key.get(c);
                    if (rawKeyDef != null && rawKeyDef.has("block")
                            && "self".equals(rawKeyDef.get("block").getAsString())) {
                        continue;
                    }

                    BlockPos worldPos = getWorldPos(
                            x - pattern.controllerX,
                            y - pattern.controllerY,
                            z - pattern.controllerZ,
                            facing);

                    Set<String> allowedTypes = buildAllowedTypes(c, pattern);
                    result.add(new PreviewBlockInfo(worldPos,
                            List.copyOf(allowedTypes), c));
                }
            }
        }
        return result;
    }

    // ==================== 结构验证 ====================

    /**
     * 验证多方块结构是否完整。
     *
     * <p>遍历 pattern 中所有非空格、非 self 位置，
     * 检查对应世界坐标的方块是否实现了 {@link IMultiBlockPart} 且 Tier 合规。
     * 支持 alternatives（该位置可匹配 alternatives 中任一 key 的类型）。
     * Post-loop 检查所有 key 的 {@code min_count} / {@code max_count} 约束。
     * 任意位置的区块未加载时，自动调度延迟验证并返回 false。
     *
     * @return true 表示结构完整成型
     */
    default boolean validateStructure() {
        MultiBlockState state = mbs();
        ParsedPattern pattern;
        try {
            pattern = parsePattern();
            if (pattern == null) {
                state.structureFormed = false;
                return false;
            }
        } catch (Exception e) {
            state.structureFormed = false;
            return false;
        }

        Level level = getLevel();
        if (level == null) return false;

        Direction facing = getFacingDirection();

        // 临时列表，验证全部通过后替换正式缓存
        List<BlockPos> newInputHatches = new ArrayList<>();
        List<BlockPos> newOutputHatches = new ArrayList<>();
        List<BlockPos> newFoodHatches = new ArrayList<>();
        List<BlockPos> newFluidInputHatches = new ArrayList<>();
        List<BlockPos> newFluidOutputHatches = new ArrayList<>();
        List<BlockPos> newCasingPositions = new ArrayList<>();
        Set<BlockPos> newAllParts = new LinkedHashSet<>();

        // 各 key 字符在结构中实际出现的次数
        Map<Character, Integer> keyCounts = new HashMap<>();

        for (int y = 0; y < pattern.height; y++) {
            for (int z = 0; z < pattern.depth; z++) {
                for (int x = 0; x < pattern.width; x++) {
                    // 跳过控制器自身
                    if (y == pattern.controllerY && x == pattern.controllerX && z == pattern.controllerZ) {
                        continue;
                    }

                    char c = pattern.layerChars[y][z][x];
                    // 跳过空格（任意方块）
                    if (c == ' ') continue;

                    JsonObject keyDef = pattern.key.get(c);
                    String blockType = keyDef.has("block") ? keyDef.get("block").getAsString() : null;

                    // 跳过 self 标记
                    if ("self".equals(blockType)) continue;

                    BlockPos worldPos = getWorldPos(
                            x - pattern.controllerX, y - pattern.controllerY,
                            z - pattern.controllerZ, facing);

                    // 检查区块是否已加载
                    if (!level.isLoaded(worldPos)) {
                        scheduleRevalidation(40);
                        return false;
                    }

                    // 获取该位置的允许类型集合（基础类型 + alternatives）
                    Set<String> allowedTypes = buildAllowedTypes(c, pattern);

                    // 遍历所有允许类型，任一匹配即通过
                    boolean matched = false;
                    String matchedTypeStr = null;

                    for (String typeStr : allowedTypes) {
                        if (isBlockOrTagType(typeStr)) {
                            // 方块注册名匹配（含 ":" 且非 "tag:" 开头）
                            String blockName = BuiltInRegistries.BLOCK
                                    .getKey(level.getBlockState(worldPos).getBlock()).toString();
                            if (blockName.equals(typeStr)) {
                                matched = true;
                                matchedTypeStr = typeStr;
                                break;
                            }
                        } else if (isTagType(typeStr)) {
                            // Block Tag 匹配（"tag:" 前缀）
                            String tagStr = typeStr.substring(4);
                            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK,
                                    ResourceLocation.parse(tagStr));
                            if (level.getBlockState(worldPos).is(tagKey)) {
                                matched = true;
                                matchedTypeStr = typeStr;
                                break;
                            }
                        } else {
                            // IMultiBlockPart 零件类型匹配（无 ":"）
                            IMultiBlockPart part = resolveMultiBlockPart(worldPos);
                            if (part != null
                                    && part.getPartTier().getLevel() <= getTier().getLevel()
                                    && part.getPartType().equals(typeStr)) {
                                matched = true;
                                matchedTypeStr = typeStr;
                                break;
                            }
                        }
                    }

                    // 检查零件认领（非共享模式下的独占性）
                    if (matched) {
                        IMultiBlockPart matchedPart = resolveMultiBlockPart(worldPos);
                        if (matchedPart != null) {
                            BlockPos existingOwner = matchedPart.getOwningController(level, worldPos);
                            if (existingOwner != null && !existingOwner.equals(getBlockPos())) {
                                if (!pattern.shareable()) {
                                    // 非共享模式：已被其他控制器认领 → 此位置匹配失败
                                    matched = false;
                                    matchedTypeStr = null;
                                }
                                // 共享模式：允许共用，matched 保持 true
                            }
                        }
                    }

                    if (!matched) {
                        state.structureFormed = false;
                        notifyPartsUnformed();
                        return false;
                    }

                    // 按角色分类缓存
                    newAllParts.add(worldPos);
                    if (isBlockOrTagType(matchedTypeStr) || isTagType(matchedTypeStr)) {
                        // 方块/标签类型 → 归类为外壳
                        newCasingPositions.add(worldPos);
                    } else {
                        switch (matchedTypeStr) {
                            case IMultiBlockPart.TYPE_INPUT_HATCH -> newInputHatches.add(worldPos);
                            case IMultiBlockPart.TYPE_OUTPUT_HATCH -> newOutputHatches.add(worldPos);
                            case IMultiBlockPart.TYPE_FOOD_HATCH -> newFoodHatches.add(worldPos);
                            case IMultiBlockPart.TYPE_FLUID_INPUT_HATCH -> newFluidInputHatches.add(worldPos);
                            case IMultiBlockPart.TYPE_FLUID_OUTPUT_HATCH -> newFluidOutputHatches.add(worldPos);
                            default -> newCasingPositions.add(worldPos);
                        }
                    }

                    // 找到与 matchedTypeStr 匹配的 key 字符（用于 min/max 计数）
                    Character matchedKey = findMatchingKey(c, matchedTypeStr, pattern);
                    keyCounts.merge(matchedKey, 1, Integer::sum);
                }
            }
        }

        // 验证 min_count / max_count 约束
        for (Map.Entry<Character, KeyDefinition> entry : pattern.keyDefs.entrySet()) {
            char c = entry.getKey();
            KeyDefinition kd = entry.getValue();
            int count = keyCounts.getOrDefault(c, 0);

            if (kd.minCount > 0 && count < kd.minCount) {
                state.structureFormed = false;
                notifyPartsUnformed();
                return false;
            }
            if (kd.maxCount < Integer.MAX_VALUE && count > kd.maxCount) {
                state.structureFormed = false;
                notifyPartsUnformed();
                return false;
            }
        }

        // 全部通过 → 更新缓存
        state.inputHatches.clear();
        state.inputHatches.addAll(newInputHatches);
        state.outputHatches.clear();
        state.outputHatches.addAll(newOutputHatches);
        state.foodHatches.clear();
        state.foodHatches.addAll(newFoodHatches);
        state.fluidInputHatches.clear();
        state.fluidInputHatches.addAll(newFluidInputHatches);
        state.fluidOutputHatches.clear();
        state.fluidOutputHatches.addAll(newFluidOutputHatches);
        state.casingPositions.clear();
        state.casingPositions.addAll(newCasingPositions);
        state.allPartPositions.clear();
        state.allPartPositions.addAll(newAllParts);

        // 认领所有匹配的零件（全部验证通过后才执行，避免部分认领后验证失败导致孤儿认领）
        for (BlockPos partPos : newAllParts) {
            IMultiBlockPart part = resolveMultiBlockPart(partPos);
            if (part != null) {
                part.claimPart(level, partPos, getBlockPos());
            }
        }

        state.structureFormed = true;
        markChanged();
        return true;
    }

    // ==================== 验证调度 ====================

    /**
     * 每 tick 处理延迟验证倒计时和定时验证。
     * 实现类的 {@code serverTick} 应在合适位置调用此方法。
     *
     * <p>两类验证：
     * <ul>
     *   <li><b>延迟验证</b>：倒计时归零时触发（用于区块加载后延迟重试），在主线程执行</li>
     *   <li><b>定时验证</b>：间隔由配置文件的 {@code validate_interval}（秒）决定，默认 30 秒，
     *       通过 {@link com.gooodwei.civilizationevolution.server.validation.StructureValidationService}
     *       提交到后台线程执行，避免大型结构验证造成主线程卡顿</li>
     * </ul>
     */
    default void tickRevalidation() {
        MultiBlockState state = mbs();

        // 延迟验证（区块未加载时调度）—— 保留在主线程，触发频率低
        if (state.revalidationDelay > 0) {
            state.revalidationDelay--;
            if (state.revalidationDelay == 0) {
                validateStructure();
                markChanged();
            }
        }

        // 定时验证 —— 提交到后台线程，避免巨型结构造成主线程卡顿
        int interval = state.cachedPattern != null
                ? state.cachedPattern.validateIntervalTicks
                : DEFAULT_VALIDATE_INTERVAL_TICKS;
        state.periodicValidationTimer++;
        if (state.periodicValidationTimer >= interval) {
            state.periodicValidationTimer = 0;
            Level lvl = getLevel();
            if (lvl instanceof net.minecraft.server.level.ServerLevel sl) {
                var service = VALIDATION_SERVICE.get();
                if (service != null) {
                    service.submitPeriodicValidation(this, sl);
                }
            }
        }
    }

    /**
     * 调度延迟重新验证。
     *
     * @param delayTicks 延迟 tick 数（通常 40 tick = 2 秒）
     */
    default void scheduleRevalidation(int delayTicks) {
        mbs().revalidationDelay = delayTicks;
    }

    // ==================== 生命周期 Hook ====================

    /**
     * 在 BlockEntity 的 {@code onLoad()} 中调用。
     * 延迟 40 tick（2 秒）后重新验证结构，给周围 chunk 加载留时间。
     */
    default void onMultiBlockLoad() {
        scheduleRevalidation(40);
    }

    // ==================== 结构破坏处理 ====================

    /**
     * 当控制器自身被破坏时调用，重置结构成型状态并弹出内部物品。
     *
     * @param partPos 被破坏的方块世界坐标（通常为控制器自身位置）
     */
    default void onStructurePartBroken(BlockPos partPos) {
        MultiBlockState state = mbs();
        if (!state.structureFormed) return;
        state.structureFormed = false;
        // 弹出所有内部物品到控制器位置
        dropAllItems();
        // 通知所有已缓存零件解除成型状态
        notifyPartsUnformed();
        // 清除缓存
        state.clearPartCaches();
    }

    /**
     * 通知所有已缓存零件解除成型状态。
     *
     * <p>遍历 {@link MultiBlockState#allPartPositions} 中的所有零件位置，
     * 调用 {@link IMultiBlockPart#releasePart} 释放认领。
     * releasePart 内部做条件检查——仅释放由本控制器认领的零件，
     * 不会误释放其他控制器认领的零件。
     */
    default void notifyPartsUnformed() {
        MultiBlockState state = mbs();
        Level level = getLevel();
        if (level == null) return;
        BlockPos myPos = getBlockPos();
        for (BlockPos partPos : state.allPartPositions) {
            IMultiBlockPart part = resolveMultiBlockPart(partPos);
            if (part != null) {
                part.releasePart(level, partPos, myPos);
            }
        }
    }

    // ==================== 状态查询 ====================

    /** 查询结构是否已成型 */
    default boolean isStructureFormed() {
        return mbs().structureFormed;
    }

    /** 是否存在解析错误 */
    default boolean hasParseError() {
        return mbs().parseError != null;
    }

    /** 获取解析错误信息 */
    @Nullable
    default String getParseError() {
        return mbs().parseError;
    }

    // ==================== 零件位置访问器 ====================

    /** 获取所有输入接口的世界坐标列表 */
    default List<BlockPos> getInputHatches() {
        return Collections.unmodifiableList(mbs().inputHatches);
    }

    /** 获取所有输出接口的世界坐标列表 */
    default List<BlockPos> getOutputHatches() {
        return Collections.unmodifiableList(mbs().outputHatches);
    }

    /** 获取所有食物接口的世界坐标列表 */
    default List<BlockPos> getFoodHatches() {
        return Collections.unmodifiableList(mbs().foodHatches);
    }

    /** 获取所有流体输入接口的世界坐标列表 */
    default List<BlockPos> getFluidInputHatches() {
        return Collections.unmodifiableList(mbs().fluidInputHatches);
    }

    /** 获取所有流体输出接口的世界坐标列表 */
    default List<BlockPos> getFluidOutputHatches() {
        return Collections.unmodifiableList(mbs().fluidOutputHatches);
    }

    /** 获取所有外壳方块的世界坐标列表 */
    default List<BlockPos> getCasingPositions() {
        return Collections.unmodifiableList(mbs().casingPositions);
    }

    // ==================== NBT Hook ====================

    /**
     * 在 BlockEntity 的 {@code saveAdditional} 中调用。
     *
     * @param tag 目标 CompoundTag
     */
    default void saveMultiBlockNBT(CompoundTag tag) {
        mbs().saveToNBT(tag);
    }

    /**
     * 在 BlockEntity 的 {@code loadAdditional} 中调用。
     *
     * @param tag 源 CompoundTag
     */
    default void loadMultiBlockNBT(CompoundTag tag) {
        mbs().loadFromNBT(tag);
    }
}

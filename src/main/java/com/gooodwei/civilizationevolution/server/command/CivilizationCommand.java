package com.gooodwei.civilizationevolution.server.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.api.CivilizationAPI;
import com.gooodwei.civilizationevolution.api.IMultiBlockMachine;
import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import com.gooodwei.civilizationevolution.api.career.Career;
import com.gooodwei.civilizationevolution.api.component.DebugStructureData;
import com.gooodwei.civilizationevolution.server.config.MultiBlockConfig;
import com.gooodwei.civilizationevolution.server.item.DebugStructureGetterItem;
import com.gooodwei.civilizationevolution.server.population.Population;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

/**
 * {@code /civilization} 命令 —— 用于查看和修改手持人口物品的属性。
 *
 * <p>命令树：
 * <pre>
 * /civilization population get &lt;field&gt;   — 读取单个字段
 * /civilization population set &lt;field&gt; &lt;value&gt; — 设置单个字段
 * /civilization population read              — 读取所有字段
 * </pre>
 *
 * <p>支持的字段名由 {@link #FIELDS} 静态映射定义，覆盖年龄、寿命、
 * 健康度、饱食度、职业、熟练度、工作效率、心理状态、性别和死亡标记。
 */
@EventBusSubscriber(modid = CivilizationEvolution.MODID)
public final class CivilizationCommand {

    /** 命令可操作的字段名 → 读取器和类型信息的映射 */
    private static final Map<String, FieldInfo> FIELDS = new LinkedHashMap<>();

    static {
        FIELDS.put("age",             new FieldInfo(t -> t.getInt(Population.TAG_AGE),             "int"));
        FIELDS.put("lifespan",        new FieldInfo(t -> t.getInt(Population.TAG_LIFESPAN),        "int"));
        FIELDS.put("health",          new FieldInfo(t -> t.getInt(Population.TAG_HEALTH),           "int"));
        FIELDS.put("food",            new FieldInfo(t -> t.getInt(Population.TAG_FOOD),             "int"));
        FIELDS.put("career",          new FieldInfo(t -> t.getString(Population.TAG_CAREER),         "string"));
        FIELDS.put("proficiency",     new FieldInfo(t -> t.getInt(Population.TAG_PROFICIENCY),      "int"));
        FIELDS.put("workEfficiency",  new FieldInfo(t -> t.getDouble(Population.TAG_WORK_EFFICIENCY), "double"));
        FIELDS.put("mentalState",     new FieldInfo(t -> t.getDouble(Population.TAG_MENTAL_STATE),  "double"));
        FIELDS.put("gender",          new FieldInfo(t -> t.getBoolean(Population.TAG_GENDER),       "bool"));
        FIELDS.put("dead",            new FieldInfo(t -> t.getBoolean(Population.TAG_DEAD),          "bool"));
    }

    private static final ResourceLocation POPULATION_ID =
            ResourceLocation.fromNamespaceAndPath(CivilizationEvolution.MODID, "population");

    /** 自动填充玩家准星对准方块的 X 坐标 */
    private static final SuggestionProvider<CommandSourceStack> TARGET_BLOCK_X = (ctx, builder) -> {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            HitResult hit = player.pick(20.0D, 0.0F, false);
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                builder.suggest(String.valueOf(blockHit.getBlockPos().getX()));
            }
        } catch (CommandSyntaxException ignored) {}
        return builder.buildFuture();
    };

    /** 自动填充玩家准星对准方块的 Y 坐标 */
    private static final SuggestionProvider<CommandSourceStack> TARGET_BLOCK_Y = (ctx, builder) -> {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            HitResult hit = player.pick(20.0D, 0.0F, false);
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                builder.suggest(String.valueOf(blockHit.getBlockPos().getY()));
            }
        } catch (CommandSyntaxException ignored) {}
        return builder.buildFuture();
    };

    /** 自动填充玩家准星对准方块的 Z 坐标 */
    private static final SuggestionProvider<CommandSourceStack> TARGET_BLOCK_Z = (ctx, builder) -> {
        try {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            HitResult hit = player.pick(20.0D, 0.0F, false);
            if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                builder.suggest(String.valueOf(blockHit.getBlockPos().getZ()));
            }
        } catch (CommandSyntaxException ignored) {}
        return builder.buildFuture();
    };

    /** 自动填充 multi_blocks.json 中所有可用的结构 key */
    private static final SuggestionProvider<CommandSourceStack> STRUCTURE_KEY_SUGGESTIONS = (ctx, builder) -> {
        for (String key : MultiBlockConfig.getStructureKeys()) {
            builder.suggest(key);
        }
        return builder.buildFuture();
    };

    /** 可用作 key 的单字节字符池（按优先级排列） */
    private static final char[] SINGLE_CHAR_POOL = buildSingleCharPool();

    /** 输出结构 JSON 的目录 */
    private static final Path STRUCTURE_OUTPUT_DIR =
            FMLPaths.CONFIGDIR.get().resolve("civilizationevolution").resolve("debug").resolve("structures");

    private CivilizationCommand() {}

    /** 构造单字节字符池：A-Z + a-z + 0-9 + 安全符号，排除空格和逗号 */
    private static char[] buildSingleCharPool() {
        StringBuilder sb = new StringBuilder();
        // A-Z
        for (char c = 'A'; c <= 'Z'; c++) sb.append(c);
        // a-z
        for (char c = 'a'; c <= 'z'; c++) sb.append(c);
        // 0-9
        for (char c = '0'; c <= '9'; c++) sb.append(c);
        // 安全 ASCII 符号（排除空格 ' '、逗号 ',' 和双字节填充符 '_'）
        String safeSymbols = "!@#$%^&*()-=+[]{}|;:'.<>?/~";
        sb.append(safeSymbols);
        return sb.toString().toCharArray();
    }

    /**
     * 注册命令到命令调度器。
     * 需要游戏大师权限（{@code LEVEL_GAMEMASTERS}）。
     */
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("civilization")
                .requires(src -> src.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("population")
                    .then(Commands.literal("get")
                        .then(Commands.argument("field", StringArgumentType.word())
                            .executes(ctx -> doGet(ctx.getSource(), StringArgumentType.getString(ctx, "field")))
                        )
                    )
                    .then(Commands.literal("set")
                        .then(Commands.argument("field", StringArgumentType.word())
                            .then(Commands.argument("value", StringArgumentType.greedyString())
                                .executes(ctx -> doSet(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "field"),
                                    StringArgumentType.getString(ctx, "value")
                                ))
                            )
                        )
                    )
                    .then(Commands.literal("read")
                        .executes(ctx -> doRead(ctx.getSource()))
                    )
                    .executes(ctx -> {
                        ctx.getSource().sendSuccess(() -> Component.literal(
                            "/civilization population <get|set|read> [field] [value]"
                        ), false);
                        return Command.SINGLE_SUCCESS;
                    })
                )
        );

        // /civilization admin getStructure <x> <y> <z> <name>
        dispatcher.register(
            Commands.literal("civilization")
                .requires(src -> src.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("admin")
                    .then(Commands.literal("getStructure")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                            .suggests(TARGET_BLOCK_X)
                            .then(Commands.argument("y", IntegerArgumentType.integer())
                                .suggests(TARGET_BLOCK_Y)
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                    .suggests(TARGET_BLOCK_Z)
                                    .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(ctx -> doGetStructure(
                                            ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "x"),
                                            IntegerArgumentType.getInteger(ctx, "y"),
                                            IntegerArgumentType.getInteger(ctx, "z"),
                                            StringArgumentType.getString(ctx, "name")
                                        ))
                                    )
                                )
                            )
                        )
                    )
                    .then(Commands.literal("setStructure")
                        .then(Commands.argument("x", IntegerArgumentType.integer())
                            .suggests(TARGET_BLOCK_X)
                            .then(Commands.argument("y", IntegerArgumentType.integer())
                                .suggests(TARGET_BLOCK_Y)
                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                    .suggests(TARGET_BLOCK_Z)
                                    .then(Commands.argument("structureKey", StringArgumentType.word())
                                        .suggests(STRUCTURE_KEY_SUGGESTIONS)
                                        .executes(ctx -> doSetStructure(
                                            ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "x"),
                                            IntegerArgumentType.getInteger(ctx, "y"),
                                            IntegerArgumentType.getInteger(ctx, "z"),
                                            StringArgumentType.getString(ctx, "structureKey")
                                        ))
                                    )
                                )
                            )
                        )
                    )
                )
        );
    }

    /** 读取主手人口物品的指定字段值并输出到聊天栏 */
    private static int doGet(CommandSourceStack src, String field) throws CommandSyntaxException {
        ItemStack stack = src.getPlayerOrException().getMainHandItem();
        if (!stack.is(BuiltInRegistries.ITEM.get(POPULATION_ID))) {
            src.sendFailure(Component.literal("You must hold a population item in your main hand."));
            return 0;
        }

        FieldInfo info = FIELDS.get(field);
        if (info == null) {
            src.sendFailure(Component.literal("Unknown field: " + field));
            return 0;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        Object value = info.reader.apply(tag);
        src.sendSuccess(() -> Component.literal(field + ": ")
                .append(formatFieldValue(field, value)), false);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 设置主手人口物品的指定字段值。
     * 根据字段类型自动将字符串解析为 int/double/bool/string，
     * 设置后自动调用 {@code recalcEfficiency} 重新计算工作效率。
     */
    private static int doSet(CommandSourceStack src, String field, String rawValue) throws CommandSyntaxException {
        ItemStack stack = src.getPlayerOrException().getMainHandItem();
        if (!stack.is(BuiltInRegistries.ITEM.get(POPULATION_ID))) {
            src.sendFailure(Component.literal("You must hold a population item in your main hand."));
            return 0;
        }

        FieldInfo info = FIELDS.get(field);
        if (info == null) {
            src.sendFailure(Component.literal("Unknown field: " + field));
            return 0;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        try {
            switch (info.type) {
                case "int" -> tag.putInt(field, Integer.parseInt(rawValue));
                case "double" -> tag.putDouble(field, Double.parseDouble(rawValue));
                case "string" -> tag.putString(field, rawValue);
                case "bool" -> tag.putBoolean(field, Boolean.parseBoolean(rawValue));
            }
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            // 在任何属性变更后重新计算工作效率
            CivilizationAPI.getPopulationManager().recalcEfficiency(stack);
            src.sendSuccess(() -> Component.literal("Set " + field + " to " + rawValue), true);
            return Command.SINGLE_SUCCESS;
        } catch (NumberFormatException e) {
            src.sendFailure(Component.literal("Invalid value for " + info.type + ": " + rawValue));
            return 0;
        }
    }

    /** 读取主手人口物品的所有字段并以表格形式输出 */
    private static int doRead(CommandSourceStack src) throws CommandSyntaxException {
        ItemStack stack = src.getPlayerOrException().getMainHandItem();
        if (!stack.is(BuiltInRegistries.ITEM.get(POPULATION_ID))) {
            src.sendFailure(Component.literal("You must hold a population item in your main hand."));
            return 0;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        src.sendSuccess(() -> {
            Component result = Component.literal("=== Population Data ===");
            for (var entry : FIELDS.entrySet()) {
                Object value = entry.getValue().reader.apply(tag);
                result = result.copy().append("\n")
                        .append(Component.literal(entry.getKey() + ": "))
                        .append(formatFieldValue(entry.getKey(), value));
            }
            return result;
        }, false);
        return Command.SINGLE_SUCCESS;
    }

    /**
     * 将字段原始值格式化为可读的 Component。
     * 职业字段翻译为本地化名，性别翻译为男/女，其余字段直接 toString。
     */
    private static Component formatFieldValue(String field, Object raw) {
        return switch (field) {
            case "career" -> {
                Career c = CivilizationAPI.getCareerRegistry().byName((String) raw);
                yield c != null
                        ? Component.translatable(c.getTranslationKey())
                        : Component.literal((String) raw);
            }
            case "gender" -> Component.translatable(
                    "tooltip.civilizationevolution.population.gender."
                            + (((Boolean) raw) ? "male" : "female"));
            default -> Component.literal(String.valueOf(raw));
        };
    }

    /** 字段元信息：从 CompoundTag 读取值的函数 + 值类型标识（用于 set 时解析） */
    private record FieldInfo(Function<CompoundTag, Object> reader, String type) {}

    // ==================== 结构导出命令 ====================

    /**
     * 执行结构导出命令。
     *
     * @param src  命令来源
     * @param cx   控制器 X 坐标
     * @param cy   控制器 Y 坐标
     * @param cz   控制器 Z 坐标
     * @param name 结构名称
     */
    private static int doGetStructure(CommandSourceStack src, int cx, int cy, int cz, String name)
            throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        ItemStack stack = player.getMainHandItem();

        // 1. 检查手持物品
        if (!(stack.getItem() instanceof DebugStructureGetterItem)) {
            src.sendFailure(Component.translatable(
                    "msg.civilizationevolution.debug_structure_getter.not_holding"));
            return 0;
        }

        // 2. 读取坐标数据
        DebugStructureData data = DebugStructureGetterItem.getData(stack);
        if (!data.hasPos1() || !data.hasPos2()) {
            src.sendFailure(Component.translatable(
                    "msg.civilizationevolution.debug_structure_getter.no_positions"));
            return 0;
        }

        // 3. 检查维度一致
        if (!data.dim1().equals(data.dim2())) {
            src.sendFailure(Component.translatable(
                    "msg.civilizationevolution.debug_structure_getter.dimension_mismatch"));
            return 0;
        }

        // 4. 检查与玩家当前维度一致
        String currentDim = player.level().dimension().location().toString();
        if (!currentDim.equals(data.dim1())) {
            src.sendFailure(Component.translatable(
                    "msg.civilizationevolution.debug_structure_getter.dimension_mismatch"));
            return 0;
        }

        ServerLevel level = player.serverLevel();

        // 5. 计算包围盒
        BlockPos controllerPos = new BlockPos(cx, cy, cz);
        int minX = Math.min(data.x1(), data.x2());
        int minY = Math.min(data.y1(), data.y2());
        int minZ = Math.min(data.z1(), data.z2());
        int maxX = Math.max(data.x1(), data.x2());
        int maxY = Math.max(data.y1(), data.y2());
        int maxZ = Math.max(data.z1(), data.z2());

        // 6. 验证控制器坐标在包围盒内
        if (cx < minX || cx > maxX || cy < minY || cy > maxY || cz < minZ || cz > maxZ) {
            src.sendFailure(Component.translatable(
                    "msg.civilizationevolution.debug_structure_getter.controller_out_of_bounds"));
            return 0;
        }

        // 7. 检查包围盒大小
        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        if (sizeX > 128 || sizeY > 128 || sizeZ > 128) {
            src.sendFailure(Component.translatable(
                    "msg.civilizationevolution.debug_structure_getter.too_large"));
            return 0;
        }

        // 8. Chunk 可用性检查 + 收集 LevelChunk 引用
        int minChunkX = minX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkX = maxX >> 4;
        int maxChunkZ = maxZ >> 4;

        Map<ChunkPos, LevelChunk> chunkMap = new HashMap<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    src.sendFailure(Component.translatable(
                            "msg.civilizationevolution.debug_structure_getter.chunk_not_loaded"));
                    return 0;
                }
                chunkMap.put(new ChunkPos(chunkX, chunkZ), chunk);
            }
        }

        // 9. 同步导出（直接在主线程执行，确保 chunk 数据读取安全和文件写入可靠）
        try {
            // 收集方块类型
            Map<String, List<int[]>> typeToLocalPos = new LinkedHashMap<>();

            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int x = minX; x <= maxX; x++) {
                        BlockPos worldPos = new BlockPos(x, y, z);
                        int chunkX = x >> 4;
                        int chunkZ = z >> 4;
                        LevelChunk chunk = chunkMap.get(new ChunkPos(chunkX, chunkZ));
                        if (chunk == null) continue;

                        BlockState state = chunk.getBlockState(worldPos);
                        // 局部坐标（包围盒最小角为原点，保证全部非负）
                        int lx = x - minX;
                        int ly = y - minY;
                        int lz = z - minZ;

                        String typeId;
                        if (worldPos.equals(controllerPos)) {
                            typeId = "__CONTROLLER__";
                        } else if (state.isAir()) {
                            typeId = "__AIR__";
                        } else {
                            BlockEntity be = chunk.getBlockEntity(worldPos);
                            if (be instanceof IMultiBlockPart part) {
                                typeId = "part:" + part.getPartType();
                            } else if (state.getBlock() instanceof IMultiBlockPart part) {
                                typeId = "part:" + part.getPartType();
                            } else {
                                typeId = "block:" + BuiltInRegistries.BLOCK.getKey(state.getBlock());
                            }
                        }

                        typeToLocalPos.computeIfAbsent(typeId, k -> new ArrayList<>())
                                .add(new int[]{lx, ly, lz});
                    }
                }
            }

            // 分配字符编码
            Map<String, String> typeToCode = assignCodes(typeToLocalPos);

            // 判断是否使用双字节模式
            boolean doubleChar = typeToCode.values().stream().anyMatch(s -> s.length() == 2);

            // 构建 pattern 三维字符数组
            char[][][] pattern = new char[sizeY][sizeZ][sizeX * (doubleChar ? 2 : 1)];
            // 初始化为空格
            for (int y = 0; y < sizeY; y++) {
                for (int z = 0; z < sizeZ; z++) {
                    int width = sizeX * (doubleChar ? 2 : 1);
                    for (int x = 0; x < width; x++) {
                        pattern[y][z][x] = ' ';
                    }
                }
            }

            // 填充 pattern
            for (var entry : typeToLocalPos.entrySet()) {
                String typeId = entry.getKey();
                String code = typeToCode.get(typeId);
                if (code == null || "__AIR__".equals(typeId)) continue;

                for (int[] pos : entry.getValue()) {
                    int lx = pos[0];
                    int ly = pos[1];
                    int lz = pos[2];
                    // 验证局部坐标范围
                    if (ly < 0 || ly >= sizeY || lz < 0 || lz >= sizeZ || lx < 0 || lx >= sizeX) continue;

                    if (doubleChar) {
                        int cx2 = lx * 2;
                        if (code.length() == 2) {
                            pattern[ly][lz][cx2] = code.charAt(0);
                            pattern[ly][lz][cx2 + 1] = code.charAt(1);
                        } else {
                            // 单字节 code：填充 _ 后缀
                            pattern[ly][lz][cx2] = code.charAt(0);
                            pattern[ly][lz][cx2 + 1] = '_';
                        }
                    } else {
                        pattern[ly][lz][lx] = code.charAt(0);
                    }
                }
            }

            // 通配符位置（空气）在双字节模式下写为 __
            if (doubleChar) {
                for (int y = 0; y < sizeY; y++) {
                    for (int z = 0; z < sizeZ; z++) {
                        for (int x = 0; x < sizeX; x++) {
                            if (pattern[y][z][x * 2] == ' ' && pattern[y][z][x * 2 + 1] == ' ') {
                                pattern[y][z][x * 2] = '_';
                                pattern[y][z][x * 2 + 1] = '_';
                            }
                        }
                    }
                }
            }

            // 生成 JSON
            JsonObject root = new JsonObject();
            JsonObject structures = new JsonObject();
            JsonObject structure = new JsonObject();

            // controller（局部坐标，包围盒最小角为原点）
            JsonArray controllerArr = new JsonArray();
            controllerArr.add(controllerPos.getY() - minY);
            controllerArr.add(controllerPos.getX() - minX);
            controllerArr.add(controllerPos.getZ() - minZ);
            structure.add("controller", controllerArr);

            // pattern
            JsonObject patternObj = new JsonObject();
            for (int y = 0; y < sizeY; y++) {
                StringBuilder layer = new StringBuilder();
                for (int z = 0; z < sizeZ; z++) {
                    if (z > 0) layer.append(',');
                    for (int x = 0; x < (doubleChar ? sizeX * 2 : sizeX); x++) {
                        layer.append(pattern[y][z][x]);
                    }
                }
                patternObj.addProperty("y" + y, layer.toString());
            }
            structure.add("pattern", patternObj);

            // key
            JsonObject keyObj = new JsonObject();
            for (var entry : typeToCode.entrySet()) {
                String typeId = entry.getKey();
                String code = entry.getValue();
                if ("__AIR__".equals(typeId)) continue;

                JsonObject keyDef = new JsonObject();
                if ("__CONTROLLER__".equals(typeId)) {
                    keyDef.addProperty("block", "self");
                } else {
                    // 去掉前缀 part: 或 block:
                    String actualType = typeId;
                    if (actualType.startsWith("part:")) {
                        actualType = actualType.substring(5);
                    } else if (actualType.startsWith("block:")) {
                        actualType = actualType.substring(6);
                    }
                    keyDef.addProperty("type", actualType);
                }
                keyObj.add(code, keyDef);
            }
            structure.add("key", keyObj);

            if (doubleChar) {
                structure.addProperty("code_width", 2);
            }

            structure.addProperty("validate_interval", 30);
            structures.add(name, structure);
            root.add("structures", structures);

            // 写入文件
            Files.createDirectories(STRUCTURE_OUTPUT_DIR);
            Path outputFile = STRUCTURE_OUTPUT_DIR.resolve(name + ".json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(outputFile, gson.toJson(root));

            src.sendSuccess(() -> Component.translatable(
                    "msg.civilizationevolution.debug_structure_getter.exported",
                    outputFile.toString()), false);

        } catch (Exception e) {
            src.sendFailure(Component.literal("导出失败: " + e.getMessage()));
            CivilizationEvolution.LOGGER.error("导出结构数据失败", e);
        }

        return Command.SINGLE_SUCCESS;
    }

    /**
     * 按类型出现频率降序分配字符编码。
     * 高频类型优先分配单字节短 code。
     */
    private static Map<String, String> assignCodes(Map<String, List<int[]>> typeToLocalPos) {
        // 按出现频率降序排列
        List<Map.Entry<String, List<int[]>>> sorted = new ArrayList<>(typeToLocalPos.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));

        Map<String, String> result = new LinkedHashMap<>();
        int singleIdx = 0;
        int doubleIdx = 0;

        for (var entry : sorted) {
            String typeId = entry.getKey();
            if ("__AIR__".equals(typeId)) continue;

            if (singleIdx < SINGLE_CHAR_POOL.length) {
                // 单字节
                result.put(typeId, String.valueOf(SINGLE_CHAR_POOL[singleIdx++]));
            } else {
                // 双字节回退：AA, AB, ... AZ, A0, A1, ...
                char c1 = SINGLE_CHAR_POOL[doubleIdx / SINGLE_CHAR_POOL.length];
                char c2 = SINGLE_CHAR_POOL[doubleIdx % SINGLE_CHAR_POOL.length];
                result.put(typeId, "" + c1 + c2);
                doubleIdx++;
            }
        }

        return result;
    }

    // ==================== 结构生成命令 ====================

    /**
     * 执行结构生成命令：在指定坐标根据结构 key 放置完整的控制器+多方块结构。
     *
     * @param src          命令来源
     * @param cx           控制器 X 世界坐标
     * @param cy           控制器 Y 世界坐标
     * @param cz           控制器 Z 世界坐标
     * @param structureKey 配置文件中的结构 key
     */
    private static int doSetStructure(CommandSourceStack src, int cx, int cy, int cz, String structureKey)
            throws CommandSyntaxException {
        // 1. 必须由玩家执行（需要朝向）
        ServerPlayer player = src.getPlayerOrException();

        // 2. 验证 structureKey 存在
        String structureJson = MultiBlockConfig.getStructureJson(structureKey);
        if (structureJson == null) {
            String validKeys = String.join(", ", MultiBlockConfig.getStructureKeys());
            src.sendFailure(Component.literal(
                    "Unknown structure key: " + structureKey + ". Valid keys: " + validKeys));
            return 0;
        }

        // 3. 解析结构 pattern
        IMultiBlockMachine.ParsedPattern pattern = IMultiBlockMachine.parsePatternStatic(structureKey);
        if (pattern == null) {
            src.sendFailure(Component.literal("Failed to parse structure key: " + structureKey));
            return 0;
        }

        ServerLevel level = player.serverLevel();
        BlockPos controllerPos = new BlockPos(cx, cy, cz);

        // 4. 检查区块已加载
        if (!level.isLoaded(controllerPos)) {
            src.sendFailure(Component.literal("Chunk not loaded at controller position: "
                    + controllerPos.toShortString()));
            return 0;
        }

        // 5. 取玩家水平朝向作为结构 facing 方向
        Direction facing = player.getDirection();

        // 6. 解析控制器方块并放置
        ResourceLocation controllerBlockId = ResourceLocation.fromNamespaceAndPath(
                CivilizationEvolution.MODID, structureKey);
        Block controllerBlock = BuiltInRegistries.BLOCK.get(controllerBlockId);
        if (controllerBlock == BuiltInRegistries.BLOCK.get(
                ResourceLocation.fromNamespaceAndPath("minecraft", "air"))) {
            src.sendFailure(Component.literal("Controller block not found for key: " + structureKey));
            return 0;
        }
        BlockState controllerState = controllerBlock.defaultBlockState();
        if (controllerState.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            controllerState = controllerState.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
        }
        level.setBlock(controllerPos, controllerState, Block.UPDATE_ALL);

        int placed = 0;
        int skipped = 0;

        // 7. 遍历 pattern，放置所有非空格、非 self 位置的方块
        for (int y = 0; y < pattern.height(); y++) {
            for (int z = 0; z < pattern.depth(); z++) {
                for (int x = 0; x < pattern.width(); x++) {
                    char c = pattern.layerChars()[y][z][x];

                    // 跳过空格（该位置可为任意方块）
                    if (c == ' ') continue;

                    // 跳过控制器自身（"block": "self"）
                    IMultiBlockMachine.KeyDefinition kd = pattern.keyDefs().get(c);
                    if (kd == null) continue;
                    if ("self".equals(kd.type())) continue;

                    // 跳过控制器局部坐标（已在步骤 6 放置）
                    if (y == pattern.controllerY()
                            && x == pattern.controllerX()
                            && z == pattern.controllerZ()) {
                        continue;
                    }

                    // 计算世界坐标
                    BlockPos worldPos = getWorldPosStatic(
                            x - pattern.controllerX(),
                            y - pattern.controllerY(),
                            z - pattern.controllerZ(),
                            facing, controllerPos);

                    // 解析要放置的 BlockState
                    BlockState targetState = resolveBlockState(c, pattern, level);
                    if (targetState == null) {
                        skipped++;
                        continue;
                    }

                    // 如果当前位置已是正确方块则跳过
                    BlockState existingState = level.getBlockState(worldPos);
                    if (existingState.getBlock() == targetState.getBlock()) {
                        continue;
                    }

                    // 放置方块
                    level.setBlock(worldPos, targetState, Block.UPDATE_ALL);
                    placed++;
                }
            }
        }

        // 8. 执行放置后验证
        boolean allMatch = performStructureValidation(pattern, facing, controllerPos, level);

        // 9. 反馈结果
        int finalPlaced = placed;
        int finalSkipped = skipped;
        src.sendSuccess(() -> Component.literal(
                "Structure \"" + structureKey + "\": placed " + (finalPlaced + 1) + " blocks (含控制器)"
                        + (finalSkipped > 0 ? ", skipped " + finalSkipped : "")
                        + ". Validation: " + (allMatch ? "PASSED" : "FAILED")),
                true);

        return Command.SINGLE_SUCCESS;
    }

    /**
     * 根据 key 字符定义解析实际要放置的 BlockState。
     *
     * @param c       pattern 中的字符
     * @param pattern 解析后的结构模式
     * @param level   世界实例（用于 tag 解析）
     * @return 解析出的 BlockState，无法解析时返回 null
     */
    private static BlockState resolveBlockState(char c,
            IMultiBlockMachine.ParsedPattern pattern, ServerLevel level) {
        IMultiBlockMachine.KeyDefinition kd = pattern.keyDefs().get(c);
        if (kd == null) return null;

        String type = kd.type();

        // 1. 方块注册名（含 ":" 且不以 "tag:" 开头）
        if (type.indexOf(':') >= 0 && !type.startsWith("tag:")) {
            ResourceLocation rl = ResourceLocation.parse(type);
            Block block = BuiltInRegistries.BLOCK.get(rl);
            if (block == BuiltInRegistries.BLOCK.get(
                    ResourceLocation.fromNamespaceAndPath("minecraft", "air"))) {
                return null;
            }
            return block.defaultBlockState();
        }

        // 2. Block Tag（以 "tag:" 开头）
        if (type.startsWith("tag:")) {
            String tagStr = type.substring(4);
            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, ResourceLocation.parse(tagStr));
            var optionalBlocks = level.registryAccess()
                    .registryOrThrow(Registries.BLOCK)
                    .getTag(tagKey);
            if (optionalBlocks.isPresent()) {
                for (var holder : optionalBlocks.get()) {
                    return holder.value().defaultBlockState();
                }
            }
            return null;
        }

        // 3. 零件类型（不含 ":"）
        return resolveBlockStateFromPartType(type);
    }

    /**
     * 从零件类型字符串扫描 BuiltInRegistries.BLOCK 找到匹配的方块。
     * 选择 tier 最低的匹配方块。
     *
     * @param partType 零件类型字符串（如 "multi_block_part"）
     * @return 匹配的 BlockState，未找到时返回 null
     */
    private static BlockState resolveBlockStateFromPartType(String partType) {
        Block bestBlock = null;
        int bestTier = Integer.MAX_VALUE;

        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            Block block = entry.getValue();
            if (block instanceof IMultiBlockPart part) {
                if (part.getPartType().equals(partType)) {
                    int tier = part.getPartTier().getLevel();
                    if (bestBlock == null || tier < bestTier) {
                        bestBlock = block;
                        bestTier = tier;
                    }
                }
            }
        }

        return bestBlock != null ? bestBlock.defaultBlockState() : null;
    }

    /**
     * 将 pattern 局部坐标（相对控制器）根据 facing 旋转为世界绝对坐标。
     * 复用 {@link IMultiBlockMachine#rotateOffset(int, int, int, Direction)} 的数学逻辑。
     *
     * @param lx            局部 X（right 方向）
     * @param ly            局部 Y（up 方向）
     * @param lz            局部 Z（backward 方向）
     * @param facing        结构正面朝向
     * @param controllerPos 控制器世界坐标
     * @return 世界绝对坐标
     */
    private static BlockPos getWorldPosStatic(int lx, int ly, int lz,
            Direction facing, BlockPos controllerPos) {
        Direction right = facing.getClockWise();
        return controllerPos.offset(
                lx * right.getStepX() - lz * facing.getStepX(),
                ly,
                lx * right.getStepZ() - lz * facing.getStepZ());
    }

    /**
     * 放置后验证：检查结构每个位置是否匹配 pattern。
     *
     * @return true 表示所有位置均匹配
     */
    private static boolean performStructureValidation(
            IMultiBlockMachine.ParsedPattern pattern, Direction facing,
            BlockPos controllerPos, ServerLevel level) {
        boolean allMatch = true;

        for (int y = 0; y < pattern.height(); y++) {
            for (int z = 0; z < pattern.depth(); z++) {
                for (int x = 0; x < pattern.width(); x++) {
                    char c = pattern.layerChars()[y][z][x];
                    if (c == ' ') continue;
                    if (y == pattern.controllerY()
                            && x == pattern.controllerX()
                            && z == pattern.controllerZ()) continue;

                    IMultiBlockMachine.KeyDefinition kd = pattern.keyDefs().get(c);
                    if (kd == null) continue;
                    if ("self".equals(kd.type())) continue;

                    BlockPos worldPos = getWorldPosStatic(
                            x - pattern.controllerX(),
                            y - pattern.controllerY(),
                            z - pattern.controllerZ(),
                            facing, controllerPos);

                    // 构建允许的类型集合
                    Set<String> allowed = new LinkedHashSet<>();
                    allowed.add(kd.type());
                    for (char alt : kd.alternatives()) {
                        IMultiBlockMachine.KeyDefinition altDef = pattern.keyDefs().get(alt);
                        if (altDef != null) allowed.add(altDef.type());
                    }

                    // 检查当前方块
                    BlockState currentState = level.getBlockState(worldPos);
                    boolean matched = false;
                    for (String typeStr : allowed) {
                        if (typeStr.indexOf(':') >= 0 && !typeStr.startsWith("tag:")) {
                            String regName = BuiltInRegistries.BLOCK
                                    .getKey(currentState.getBlock()).toString();
                            if (regName.equals(typeStr)) {
                                matched = true;
                                break;
                            }
                        } else if (typeStr.startsWith("tag:")) {
                            String tagStr = typeStr.substring(4);
                            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK,
                                    ResourceLocation.parse(tagStr));
                            if (currentState.is(tagKey)) {
                                matched = true;
                                break;
                            }
                        } else {
                            // 零件类型：检查 BlockEntity 和 Block
                            BlockEntity be = level.getBlockEntity(worldPos);
                            if (be instanceof IMultiBlockPart part
                                    && part.getPartType().equals(typeStr)) {
                                matched = true;
                                break;
                            }
                            if (currentState.getBlock() instanceof IMultiBlockPart part
                                    && part.getPartType().equals(typeStr)) {
                                matched = true;
                                break;
                            }
                        }
                    }
                    if (!matched) {
                        allMatch = false;
                    }
                }
            }
        }

        return allMatch;
    }
}

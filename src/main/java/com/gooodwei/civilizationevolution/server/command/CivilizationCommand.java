package com.gooodwei.civilizationevolution.server.command;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.api.CivilizationAPI;
import com.gooodwei.civilizationevolution.api.career.Career;
import com.gooodwei.civilizationevolution.server.population.Population;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.LinkedHashMap;
import java.util.Map;
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

    private CivilizationCommand() {}

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
}

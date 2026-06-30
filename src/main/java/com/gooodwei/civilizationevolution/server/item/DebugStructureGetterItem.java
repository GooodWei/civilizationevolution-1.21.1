package com.gooodwei.civilizationevolution.server.item;

import com.gooodwei.civilizationevolution.api.component.DebugStructureData;
import com.gooodwei.civilizationevolution.api.component.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 结构调试获取器 —— 用于框选多方块结构区域并导出结构 JSON。
 *
 * <p>使用方式：
 * <ul>
 *   <li>右键方块 → 设定坐标1（pos1）</li>
 *   <li>Shift+右键方块 → 设定坐标2（pos2）</li>
 *   <li>手持已设定两个坐标的物品，执行 {@code /civilization admin getStructure &lt;x&gt; &lt;y&gt; &lt;z&gt; &lt;name&gt;} 导出结构</li>
 * </ul>
 *
 * <p>坐标数据通过 {@link DebugStructureData} DataComponent 持久化到物品上。
 */
public class DebugStructureGetterItem extends Item {

    public DebugStructureGetterItem(Properties properties) {
        super(properties);
    }

    // ==================== 物品提示 ====================

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        DebugStructureData data = getData(stack);
        if (data.hasPos1()) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.civilizationevolution.debug_structure_getter.pos1",
                    data.x1(), data.y1(), data.z1(), data.dim1())
                    .withStyle(ChatFormatting.AQUA));
        }
        if (data.hasPos2()) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.civilizationevolution.debug_structure_getter.pos2",
                    data.x2(), data.y2(), data.z2(), data.dim2())
                    .withStyle(ChatFormatting.GOLD));
        }
        if (!data.hasPos1() && !data.hasPos2()) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.civilizationevolution.debug_structure_getter.no_pos")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 从物品 DataComponent 读取坐标数据，未设定时返回 {@link DebugStructureData#EMPTY}。
     */
    public static DebugStructureData getData(ItemStack stack) {
        DebugStructureData data = stack.get(ModDataComponents.DEBUG_STRUCTURE_DATA);
        return data != null ? data : DebugStructureData.EMPTY;
    }

    /**
     * 将坐标数据写入物品 DataComponent。
     */
    public static void setData(ItemStack stack, DebugStructureData data) {
        stack.set(ModDataComponents.DEBUG_STRUCTURE_DATA, data);
    }

    // ==================== 交互 ====================

    /**
     * 处理右键方块的坐标记录逻辑（仅服务端执行）。
     * <p>
     * 供两处调用：
     * <ul>
     *   <li>{@link #useOn(UseOnContext)} —— 无 GUI 的普通方块（方块
     *       {@code useItemOn} 返回 {@code PASS_TO_DEFAULT_BLOCK_INTERACTION} 时）</li>
     *   <li>{@code PlayerInteractEvent.RightClickBlock} 事件处理器 ——
     *       带 GUI 的方块（方块 {@code useItemOn} 拦截了交互，物品
     *       {@code useOn} 根本不会被调用），在方块处理前就抢先取消事件并执行记录</li>
     * </ul>
     *
     * @param level  世界
     * @param player 玩家
     * @param stack  手持物品
     * @param pos    被点击的方块坐标
     */
    public static void handleBlockClick(Level level, Player player, ItemStack stack, BlockPos pos) {
        if (level.isClientSide) return;

        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        String dim = level.dimension().location().toString();

        DebugStructureData current = getData(stack);

        if (player.isShiftKeyDown()) {
            DebugStructureData updated = current.withPos2(x, y, z, dim);
            setData(stack, updated);
            player.sendSystemMessage(Component.translatable(
                    "msg.civilizationevolution.debug_structure_getter.pos2_set",
                    x, y, z, dim));
        } else {
            DebugStructureData updated = current.withPos1(x, y, z, dim);
            setData(stack, updated);
            player.sendSystemMessage(Component.translatable(
                    "msg.civilizationevolution.debug_structure_getter.pos1_set",
                    x, y, z, dim));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        // 服务端记录坐标（仅对无 GUI 的方块会走到此路径；
        // 带 GUI 的方块由 PlayerInteractEvent.RightClickBlock 事件处理器抢先拦截）
        handleBlockClick(context.getLevel(), player, context.getItemInHand(), context.getClickedPos());

        // SUCCESS 确保阻止方块原有交互（如打开 GUI）
        return InteractionResult.SUCCESS;
    }
}

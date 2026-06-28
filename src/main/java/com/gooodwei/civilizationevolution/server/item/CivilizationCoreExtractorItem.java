package com.gooodwei.civilizationevolution.server.item;

import com.gooodwei.civilizationevolution.api.IPopulationMachine;
import com.gooodwei.civilizationevolution.server.registry.ItemRegistry;
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
 * 文明核心提取器 —— 从已绑定核心的机器中提取文明核心。
 *
 * <p>使用方式：
 * <ul>
 *   <li>Shift+右键已绑定文明核心的机器 → 提取器消失，获得一个与机器绑定的核心 UUID 相同的文明核心</li>
 *   <li>机器不会被解绑，仅复制核心 UUID</li>
 * </ul>
 */
public class CivilizationCoreExtractorItem extends Item {

    public CivilizationCoreExtractorItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        tooltipComponents.add(Component.translatable(
                "tooltip.civilizationevolution.civilization_core_extractor").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;

        // 仅 Shift+右键 触发提取
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        if (!level.isClientSide) {
            if (level.getBlockEntity(pos) instanceof IPopulationMachine machine && machine.isBound()) {
                String uuid = machine.getBoundCoreUuid();
                if (uuid != null) {
                    // 消耗 1 个提取器
                    stack.shrink(1);
                    // 创建带相同 UUID 的文明核心
                    ItemStack coreStack = new ItemStack(ItemRegistry.CIVILIZATION_CORE.get());
                    CivilizationCoreItem.setUuid(coreStack, uuid);
                    // 给予玩家，背包满时掉落
                    if (!player.getInventory().add(coreStack)) {
                        player.drop(coreStack, false);
                    }
                    player.sendSystemMessage(Component.translatable(
                            "msg.civilizationevolution.extractor.success",
                            uuid.substring(0, Math.min(8, uuid.length()))));
                }
            } else {
                player.sendSystemMessage(Component.translatable(
                        "msg.civilizationevolution.extractor.not_bound").withStyle(ChatFormatting.RED));
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

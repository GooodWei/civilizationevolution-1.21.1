package com.gooodwei.civilizationevolution.server.item;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.block.AbstractMachineBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * 带 Tier 信息的方块物品 —— 在 tooltip 中显示所属时代。
 *
 * <p>所有具有 Tier 等级的机器方块都应使用此类（或子类）作为 BlockItem，
 * 在物品栏中 hover 时会显示对应的 Tier 名称。
 *
 * <p>Tier 等级直接从 {@link Block} 参数派生（通过
 * {@link AbstractMachineBlock#getTier()}），无需手动传入，
 * 确保物品 tooltip 与方块实体的 tier 始终一致。
 *
 * <p>使用示例：
 * <pre>{@code
 * ITEMS.registerItem("primitive_camp", properties ->
 *     new TieredBlockItem(BlockRegistry.PRIMITIVE_CAMP_BLOCK.get(), properties));
 * }</pre>
 */
public class TieredBlockItem extends BlockItem {

    /**
     * 创建带 Tier 信息的方块物品。
     *
     * @param block      对应的方块（需为 {@link AbstractMachineBlock} 子类，否则默认 Tier 0）
     * @param properties 物品属性
     */
    public TieredBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    /**
     * 从方块派生 Tier 等级。
     *
     * <p>若方块为 {@link AbstractMachineBlock} 子类，返回其 {@code getTier()}；
     * 否则返回 {@link CivilizationTiers#PRIMITIVE} 作为兜底。
     *
     * @return 该机器的 Tier 等级
     */
    public Tier getTier() {
        if (getBlock() instanceof AbstractMachineBlock machineBlock) {
            return machineBlock.getTier();
        }
        return CivilizationTiers.PRIMITIVE;
    }

    /**
     * 在物品 tooltip 中追加 Tier 信息。
     *
     * @param stack            物品堆
     * @param context          tooltip 上下文
     * @param tooltipComponents 当前 tooltip 行列表
     * @param tooltipFlag      tooltip 标志（高级/普通）
     */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                 List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        Tier tier = getTier();
        if (tier != null) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.civilizationevolution.tier",
                    tier.getDisplayName()).withStyle(ChatFormatting.GOLD));
        }
    }
}

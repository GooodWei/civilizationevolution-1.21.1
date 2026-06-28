package com.gooodwei.civilizationevolution.server.item;

import com.gooodwei.civilizationevolution.api.tier.Tier;
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
 * <p>使用示例：
 * <pre>{@code
 * ITEMS.registerItem("camp", properties ->
 *     new TieredBlockItem(BlockRegistry.CAMP_BLOCK.get(), properties, someTier));
 * }</pre>
 */
public class TieredBlockItem extends BlockItem {

    /** 该物品对应机器的 Tier 等级 */
    private final Tier tier;

    /**
     * 创建带 Tier 信息的方块物品。
     *
     * @param block      对应的方块
     * @param properties 物品属性
     * @param tier       该机器的 Tier 等级
     */
    public TieredBlockItem(Block block, Properties properties, Tier tier) {
        super(block, properties);
        this.tier = tier;
    }

    /** @return 该机器的 Tier 等级 */
    public Tier getTier() {
        return tier;
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
        if (tier != null) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.civilizationevolution.tier",
                    tier.getDisplayName()).withStyle(ChatFormatting.GOLD));
        }
    }
}

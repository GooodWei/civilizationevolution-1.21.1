package com.gooodwei.civilizationevolution.server.item;

import com.gooodwei.civilizationevolution.api.component.ModDataComponents;
import com.gooodwei.civilizationevolution.server.coredata.CoreDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 文明核心物品 —— 文明控制器的"硬盘"，存储所有调度数据。
 *
 * <p>参考 AE2 存储磁盘的设计：
 * <ul>
 *   <li>物品 NBT 中仅携带一个 UUID 字符串（通过 {@link ModDataComponents#CIVILIZATION_CORE_ID}）</li>
 *   <li>首次放入控制器时自动分配 UUID 并在 {@link CoreDataManager} 中创建对应数据文件</li>
 *   <li>所有业务数据（绑定机器列表、统计等）独立存储在服务器文件系统中</li>
 *   <li>核心转移到更高级控制器时，通过 UUID 自动继承所有历史数据</li>
 * </ul>
 */
public class CivilizationCoreItem extends Item {

    public CivilizationCoreItem(Properties properties) {
        super(properties.stacksTo(1).fireResistant());
    }

    /**
     * 获取物品中存储的 UUID 字符串。
     * @return UUID 字符串，未初始化时返回 null
     */
    public static String getUuid(ItemStack stack) {
        return stack.get(ModDataComponents.CIVILIZATION_CORE_ID.get());
    }

    /**
     * 为物品设置 UUID。
     */
    public static void setUuid(ItemStack stack, String uuid) {
        stack.set(ModDataComponents.CIVILIZATION_CORE_ID.get(), uuid);
    }

    /**
     * 检查物品是否已初始化（是否已有 UUID）。
     */
    public static boolean hasUuid(ItemStack stack) {
        return getUuid(stack) != null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        String uuid = getUuid(stack);
        if (uuid != null) {
            // 显示 UUID 前 8 位作为简略标识
            tooltipComponents.add(Component.translatable(
                    "item.civilizationevolution.civilization_core.uuid",
                    Component.literal(uuid.substring(0, Math.min(8, uuid.length())))));
        } else {
            tooltipComponents.add(Component.translatable("item.civilizationevolution.civilization_core.empty"));
        }
    }
}

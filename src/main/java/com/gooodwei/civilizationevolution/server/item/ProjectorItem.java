package com.gooodwei.civilizationevolution.server.item;

import net.minecraft.world.item.Item;

/**
 * 多方块结构投影仪 —— 用于切换多方块机器结构预览渲染。
 *
 * <p>玩家手持投影仪右键多方块机器核心时：
 * <ul>
 *   <li>结构未成型 → 切换结构预览渲染（替代旧版木棍+Shift 操作）</li>
 *   <li>结构已成型 → 正常打开 GUI</li>
 * </ul>
 *
 * <p>无存储状态，仅作为交互判定的物品标识。
 */
public class ProjectorItem extends Item {

    public ProjectorItem(Properties properties) {
        super(properties);
    }
}

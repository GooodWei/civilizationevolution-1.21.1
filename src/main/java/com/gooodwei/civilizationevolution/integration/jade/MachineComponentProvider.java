package com.gooodwei.civilizationevolution.integration.jade;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 客户端 Tooltip 组件 —— 在机器名称下方显示 Tier 和绑定状态。
 *
 * <p>显示格式（示例）：
 * <pre>
 * 时代：原始时代 (Lv.0)          &lt;-- 蓝色
 * 控制器：minecraft:overworld (0, 64, 0)  &lt;-- 蓝色，已绑定
 * 未绑定控制器                    &lt;-- 灰色，未绑定
 * </pre>
 *
 * <p>所有文本均通过 {@link Component#translatable} 实现 i18n 支持。
 */
public class MachineComponentProvider implements IBlockComponentProvider {

    static final MachineComponentProvider INSTANCE = new MachineComponentProvider();

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(CivilizationEvolution.MODID, "machine");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();

        // ---- 第一行：Tier 名称 ----
        String tierName = data.getString("TierName");
        int tierLevel = data.getInt("TierLevel");

        if (!tierName.isEmpty()) {
            tooltip.add(Component.translatable(
                    "jade.civilizationevolution.tier",
                    Component.translatable(tierName),
                    tierLevel));
        }

        // ---- 第二行：绑定状态 ----
        boolean isBound = data.getBoolean("IsBound");
        if (isBound) {
            String dim = data.getString("ControllerDimension");
            String pos = data.getString("ControllerPos");

            if (!dim.isEmpty() || !pos.isEmpty()) {
                String location = "";
                if (!dim.isEmpty() && !pos.isEmpty()) {
                    location = dim + " " + pos;
                } else if (!dim.isEmpty()) {
                    location = dim;
                } else {
                    location = pos;
                }
                tooltip.add(Component.translatable(
                        "jade.civilizationevolution.bound", location));
            } else {
                String shortUuid = data.getString("BoundUuidShort");
                tooltip.add(Component.translatable(
                        "jade.civilizationevolution.bound", shortUuid != null ? shortUuid : "?"));
            }
        } else {
            tooltip.add(Component.translatable("jade.civilizationevolution.not_bound"));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}

package com.gooodwei.civilizationevolution.integration.jade;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 客户端 Tooltip 组件 —— 在方块名称下方显示 Tier 和绑定状态。
 *
 * <p>显示格式（示例）：
 * <pre>
 * 时代：原始时代 (Lv.0)          &lt;-- 蓝色（所有有 Tier 的方块）
 * 控制器：minecraft:overworld (0, 64, 0)  &lt;-- 蓝色，已绑定（机器）
 * 未绑定控制器                    &lt;-- 灰色，未绑定（机器）
 * 文明核心：550e8400              &lt;-- 蓝色（控制器）
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

        // ---- 第一行：Tier 名称（所有有等级信息的方块均显示） ----
        String tierName = data.getString("TierName");
        int tierLevel = data.getInt("TierLevel");

        // 兜底：无 BE 的 IMultiBlockPart（结构外壳等）直接读取 Block 的 Tier
        if (tierName.isEmpty() && accessor.getBlock() instanceof IMultiBlockPart part) {
            Tier tier = part.getPartTier();
            tierName = tier.getTranslationKey();
            tierLevel = tier.getLevel();
        }

        if (!tierName.isEmpty()) {
            tooltip.add(Component.translatable(
                    "jade.civilizationevolution.tier",
                    Component.translatable(tierName),
                    tierLevel));
        }

        // ---- 控制器方块：显示文明核心 UUID ----
        if (data.getBoolean("IsController")) {
            if (data.getBoolean("HasCore")) {
                String shortUuid = data.getString("CoreUuidShort");
                tooltip.add(Component.translatable(
                        "jade.civilizationevolution.core_uuid", shortUuid));
            } else {
                tooltip.add(Component.translatable("jade.civilizationevolution.no_core"));
            }
            return;
        }

        // ---- 普通机器：显示绑定状态（仅 IPopulationMachine 有此数据） ----
        if (data.contains("IsBound")) {
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
        // 仓室、结构外壳等纯 IMultiBlockPart → 仅显示 Tier 行，不显示绑定信息

        // ---- 储物坑：显示已使用槽位 / 总槽位 ----
        if (data.getBoolean("IsStoragePit")) {
            int used = data.getInt("StorageUsed");
            int total = data.getInt("StorageTotal");
            int width = data.getInt("StorageWidth");
            int height = data.getInt("StorageHeight");
            String info = width + "×" + width + "×" + height + "  "
                    + Component.translatable("gui.civilizationevolution.used_slots").getString()
                    + " " + used + "/" + total;
            tooltip.add(Component.literal(info));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}

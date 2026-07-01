package com.gooodwei.civilizationevolution.integration.jade;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import com.gooodwei.civilizationevolution.api.IPopulationMachine;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.controller.AbstractControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/**
 * Jade 服务端数据提供者 —— 从所有有 Tier 等级的方块/BE 中提取信息。
 *
 * <p>收集的数据通过 NBT 同步到客户端：
 * <ul>
 *   <li>{@code TierName} — Tier 翻译键，客户端用于 {@code Component.translatable()}</li>
 *   <li>{@code TierLevel} — Tier 数字等级</li>
 *   <li>{@code IsController} — 是否为控制器方块</li>
 *   <li>{@code HasCore} — 控制器是否插入了文明核心</li>
 *   <li>{@code CoreUuidShort} — 文明核心 UUID 的前 8 位（已插核心时）</li>
 *   <li>{@code IsBound} — 机器是否已绑定控制器</li>
 *   <li>{@code BoundUuidShort} — 绑定核心 UUID 的前 8 位（已绑定时）</li>
 *   <li>{@code ControllerPos} — 控制器世界坐标（已绑定时）</li>
 *   <li>{@code ControllerDimension} — 控制器所在维度 ID（已绑定时）</li>
 * </ul>
 */
public class MachineDataProvider implements IServerDataProvider<BlockAccessor> {

    static final MachineDataProvider INSTANCE = new MachineDataProvider();

    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(CivilizationEvolution.MODID, "machine_data");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();

        // ---- 1. 提取 Tier 等级 ----
        Tier tier = null;

        // 先尝试从 IMultiBlockPart（仓室 BE、结构外壳方块等）获取
        if (be instanceof IMultiBlockPart part) {
            tier = part.getPartTier();
        }
        // 再尝试从 Block 自身获取（无 BE 的结构外壳）
        if (tier == null && accessor.getBlock() instanceof IMultiBlockPart part) {
            tier = part.getPartTier();
        }
        // 再尝试从 IPopulationMachine（范围机器）获取
        if (tier == null && be instanceof IPopulationMachine machine) {
            tier = machine.getTier();
        }

        // 未取到 Tier → 不是本模组的方块，跳过
        if (tier == null) return;

        data.putString("TierName", tier.getTranslationKey());
        data.putInt("TierLevel", tier.getLevel());

        // ---- 2. 控制器方块：文明核心 UUID ----
        if (be instanceof AbstractControllerBlockEntity controller) {
            data.putBoolean("IsController", true);

            String uuid = controller.getCurrentUuid();
            if (uuid != null) {
                data.putBoolean("HasCore", true);
                data.putString("CoreUuidShort", uuid.substring(0, Math.min(8, uuid.length())));
            } else {
                data.putBoolean("HasCore", false);
            }
            return;
        }

        // ---- 3. 普通机器：绑定信息 ----
        if (be instanceof IPopulationMachine machine) {
            data.putBoolean("IsController", false);
            boolean bound = machine.isBound();
            data.putBoolean("IsBound", bound);

            if (bound) {
                String uuid = machine.getBoundCoreUuid();
                if (uuid != null) {
                    data.putString("BoundUuidShort", uuid.substring(0, Math.min(8, uuid.length())));
                }

                String dimension = machine.getBoundControllerDimension();
                if (dimension != null) {
                    data.putString("ControllerDimension", dimension);
                }

                if (uuid != null) {
                    BlockPos pos = AbstractControllerBlockEntity.getCoreLocation(uuid);
                    if (pos != null) {
                        data.putString("ControllerPos", pos.toShortString());
                    }
                }
            }
        }
        // 仓室、结构外壳等纯 IMultiBlockPart → 仅显示 Tier，无绑定信息
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}

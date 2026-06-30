package com.gooodwei.civilizationevolution.integration.jade;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
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
 * Jade 服务端数据提供者 —— 从机器 BlockEntity 中提取 Tier 和绑定信息。
 *
 * <p>收集的数据通过 NBT 同步到客户端：
 * <ul>
 *   <li>{@code TierName} — Tier 翻译键，客户端用于 {@code Component.translatable()}</li>
 *   <li>{@code TierLevel} — Tier 数字等级</li>
 *   <li>{@code IsBound} — 是否已绑定控制器（1/0）</li>
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

        // ---- 控制器方块：显示文明核心 UUID ----
        if (be instanceof AbstractControllerBlockEntity controller) {
            Tier tier = controller.getTier();
            data.putString("TierName", tier.getTranslationKey());
            data.putInt("TierLevel", tier.getLevel());
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

        // ---- 普通机器：显示绑定信息 ----
        if (!(be instanceof IPopulationMachine machine)) return;

        Tier tier = machine.getTier();
        data.putString("TierName", tier.getTranslationKey());
        data.putInt("TierLevel", tier.getLevel());
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

            // 通过 UUID 查找控制器坐标
            if (uuid != null) {
                BlockPos pos = AbstractControllerBlockEntity.getCoreLocation(uuid);
                if (pos != null) {
                    data.putString("ControllerPos", pos.toShortString());
                }
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}

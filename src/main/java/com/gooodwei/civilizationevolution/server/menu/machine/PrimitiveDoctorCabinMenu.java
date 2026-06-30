package com.gooodwei.civilizationevolution.server.menu.machine;

import com.gooodwei.civilizationevolution.server.blockentity.multiblock.AbstractHospitalBlockEntity;
import com.gooodwei.civilizationevolution.server.menu.slot.PopulationItemSlot;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 原始诊所的菜单。
 *
 * <p>2 个治疗槽位（仅接受人口物品）+ 玩家背包 + 快捷栏。
 * 3 个 ContainerData 字段：workProgress、workTotalTime、healthThreshold。
 */
public class PrimitiveDoctorCabinMenu extends MachineMenu {

    private final ContainerData data;
    private final AbstractHospitalBlockEntity be;

    // ==================== 服务端构造器 ====================

    public PrimitiveDoctorCabinMenu(int containerId, Inventory playerInventory,
                                     AbstractHospitalBlockEntity be, ContainerData data) {
        super(MenuRegistry.PRIMITIVE_DOCTOR_CABIN_MENU.get(), containerId);
        this.be = be;
        this.data = data;
        addDataSlots(data);

        // 2 个治疗槽位（位于 GUI 中央偏上）
        // TODO：当前槽位坐标为牧场占位 GUI 的临时值，诊所专用 GUI 设计完成后需调整
        this.addSlot(new PopulationItemSlot(be, 0, 62, 35));
        this.addSlot(new PopulationItemSlot(be, 1, 98, 35));

        // 玩家背包 + 快捷栏
        addPlayerSlots(playerInventory, 84);
    }

    // ==================== 客户端构造器（fromNetwork） ====================

    public static PrimitiveDoctorCabinMenu fromNetwork(int containerId, Inventory playerInventory,
                                                         RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = playerInventory.player.level().getBlockEntity(pos);
        if (be instanceof AbstractHospitalBlockEntity hospital) {
            return new PrimitiveDoctorCabinMenu(containerId, playerInventory,
                    hospital, new SimpleContainerData(3));
        }
        // fallback: 创建一个无 BE 的菜单（不应发生）
        return new PrimitiveDoctorCabinMenu(containerId, playerInventory, null, new SimpleContainerData(3));
    }

    // ==================== 访问器 ====================

    /** 工作进度比例（0.0 ~ 1.0），用于 GUI 进度条渲染 */
    public float getWorkProgressRatio() {
        int total = data.get(1);
        if (total == 0) return 0f;
        return (float) data.get(0) / total;
    }

    /** 当前健康阈值（0-100） */
    public int getHealthThreshold() {
        return data.get(2);
    }

    /** 获取关联的方块实体 */
    public AbstractHospitalBlockEntity getBlockEntity() {
        return be;
    }

    // ==================== 快速移动 ====================

    @Override
    protected int machineSlotCount() {
        return 2;
    }

    // ==================== 有效性检查 ====================

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

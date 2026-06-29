package com.gooodwei.civilizationevolution.server.blockentity.fieldmachine;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractHospitalBlockEntity;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.PrimitiveDoctorCabinMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 原始诊所方块实体 —— Tier 0 医院。
 *
 * <p>2×2×2 多方块结构，2 个治疗槽位，
 * 仅接受牧师和无业人口，进行正向健康度治疗。
 * 健康度超过子 GUI 设定的阈值后自动移入输出接口。
 *
 * @see AbstractHospitalBlockEntity
 */
public class PrimitiveDoctorCabinBlockEntity extends AbstractHospitalBlockEntity {

    /**
     * 原始诊所的构造器。
     *
     * @param type  BlockEntity 类型
     * @param pos   方块坐标
     * @param state 方块状态
     */
    public PrimitiveDoctorCabinBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    // ==================== Tier & Config ====================

    @Override
    public Tier getTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    /** 配置文件中此机器的 key */
    private static final String CONFIG_KEY = "primitive_doctor_cabin";

    public String getMachineConfigKey() {
        return CONFIG_KEY;
    }

    @Override
    public int getWorkTotalTime() {
        return PopulationMachineConfig.getWorkTotalTime(CONFIG_KEY);
    }

    @Override
    public int getAgeIncrement() {
        return PopulationMachineConfig.getAgeIncrement(CONFIG_KEY);
    }

    // ==================== Tick & Menu ====================

    /**
     * 服务端 tick 入口，注册为 BlockEntity 的 ticker。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   PrimitiveDoctorCabinBlockEntity be) {
        AbstractHospitalBlockEntity.serverTick(level, pos, state, be);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PrimitiveDoctorCabinMenu(containerId, inventory, this, this.data);
    }
}

package com.gooodwei.civilizationevolution.server.blockentity.multiblock;

import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.config.CivilizationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.machine.VillageDoctorCabinMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 村庄诊所方块实体 —— Tier 1 医院。
 *
 * <p>2×2×2 多方块结构，3 个医生槽位（牧师/无业），
 * 病人在输入仓室原地处理，每周期健康波动，
 * 健康度超过子 GUI 设定的阈值后自动移入输出仓室。
 *
 * @see AbstractHospitalBlockEntity
 */
public class VillageDoctorCabinBlockEntity extends AbstractHospitalBlockEntity {

    /**
     * 村庄诊所的构造器。
     *
     * @param pos   方块坐标
     * @param state 方块状态
     */
    public VillageDoctorCabinBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.VILLAGE_DOCTOR_CABIN.get(), pos, state);
    }

    // ==================== Tier & Config ====================

    @Override
    public Tier getTier() {
        return CivilizationTiers.VILLAGE;
    }

    /** 配置文件中此机器的 key */
    private static final String CONFIG_KEY = CivilizationMachineConfig.VILLAGE_DOCTOR_CABIN;

    @Override
    public String getConfigKey() {
        return CONFIG_KEY;
    }

    public String getMachineConfigKey() {
        return CONFIG_KEY;
    }

    @Override
    public String getWorkerCareer() {
        return CareerNames.CLERIC;
    }

    @Override
    public int getWorkTotalTime() {
        return CivilizationMachineConfig.getWorkTotalTime(CONFIG_KEY);
    }

    @Override
    public int getAgeIncrement() {
        return CivilizationMachineConfig.getAgeIncrement(CONFIG_KEY);
    }

    @Override
    protected int getFoodPerPopulation() {
        return CivilizationMachineConfig.getFoodPerPopulation(CONFIG_KEY);
    }

    // ==================== Tick & Menu ====================

    /**
     * 服务端 tick 入口，注册为 BlockEntity 的 ticker。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   VillageDoctorCabinBlockEntity be) {
        AbstractHospitalBlockEntity.serverTick(level, pos, state, be);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new VillageDoctorCabinMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.village_doctor_cabin");
    }
}

package com.gooodwei.civilizationevolution.server.blockentity.multiblock;

import com.gooodwei.civilizationevolution.api.IClientUpdateReceiver;
import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.machine.VillageQuarryMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 村庄采石场方块实体 —— Tier 1 多方块矿井。
 *
 * <p>7×4×7 多方块结构，使用镐子逐层挖掘区块（16×16）获取资源。
 * 通用采掘逻辑已提取至 {@link AbstractQuarryBlockEntity}。
 *
 * <h3>槽位布局</h3>
 * <ul>
 *   <li>槽位 0-2：人口输入槽（矿工职业）</li>
 *   <li>槽位 3：镐槽（仅接受 {@code #minecraft:pickaxes} 标签物品）</li>
 * </ul>
 */
public class VillageQuarryBlockEntity extends AbstractQuarryBlockEntity
        implements IClientUpdateReceiver {

    /** 客户端同步字段编号：最低保留数量 */
    public static final int FIELD_MIN_KEEP_NUMBER = 0;

    // ==================== 构造器 ====================

    public VillageQuarryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 4);
    }

    // ==================== AbstractQuarryBlockEntity 抽象方法 ====================

    @Override
    protected int getPickaxeSlot() {
        return 3;
    }

    @Override
    protected List<Integer> getPopulationSlots() {
        return List.of(0, 1, 2);
    }

    // ==================== IMultiBlockMachine ====================

    @Override
    public String getConfigKey() {
        return PopulationMachineConfig.VILLAGE_QUARRY;
    }

    @Override
    public String getWorkerCareer() {
        return CareerNames.MASON;
    }


    // ==================== Tier & Config ====================

    @Override
    public Tier getTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    public int getWorkTotalTime() {
        return PopulationMachineConfig.getWorkTotalTime(getConfigKey());
    }

    @Override
    public int getAgeIncrement() {
        return PopulationMachineConfig.getAgeIncrement(getConfigKey());
    }

    // ==================== serverTick ====================

    /**
     * 服务端 tick 入口，注册为 BlockEntity 的 ticker。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   VillageQuarryBlockEntity be) {
        AbstractQuarryBlockEntity.serverTick(level, pos, state, be);
    }

    // ==================== IClientUpdateReceiver ====================

    @Override
    public void onClientUpdate(int fieldId, CompoundTag data) {
        if (fieldId == FIELD_MIN_KEEP_NUMBER) {
            int value = data.getInt("v");
            if (value < 0) value = 0;
            this.minKeepNumber = value;
            this.setChanged();
        }
    }

    // ==================== Menu ====================

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        if (!isStructureFormed()) return null;
        return new VillageQuarryMenu(containerId, inventory, this, data);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.village_quarry");
    }
}

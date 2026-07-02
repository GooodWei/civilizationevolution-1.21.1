package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.machine.VillageFarmMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 村庄农场方块实体（Tier 1）。
 *
 * <p>继承自 {@link AbstractFarmBlockEntity}，仅提供 Tier 1 特有的参数。
 * 所有农场业务逻辑（作物催熟、水管理、食物消耗）由父类提供。
 *
 * <p>槽位布局（共 9 个，与村庄牧场相同）：
 * <ul>
 *   <li>槽位 0-5：食物输入槽（2×3）</li>
 *   <li>槽位 6-8：人口输入槽（农民）</li>
 * </ul>
 *
 * <p>Tier 1 参数：
 * <ul>
 *   <li>储罐容量：16000 mB（16 桶）</li>
 *   <li>工作范围：上下各 4 格</li>
 * </ul>
 */
public class VillageFarmBlockEntity extends AbstractFarmBlockEntity {

    /** 机器槽位总数 */
    public static final int SIZE = 9;

    /** Tier 1 储罐容量：16000 mB = 16 桶 */
    private static final long TANK_CAPACITY = 16000L;

    /**
     * 构造村庄农场方块实体。
     *
     * @param pos   方块坐标
     * @param state 方块状态
     */
    public VillageFarmBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.VILLAGE_FARM.get(), pos, state, SIZE);
    }

    // ==================== 抽象方法实现 ====================

    @Override
    public Tier getTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    protected String getMachineConfigKey() {
        return PopulationMachineConfig.VILLAGE_FARM;
    }

    /**
     * 农场工作要求的职业名称
     */
    @Override
    public String getWorkerCareer() {
        return CareerNames.FARMER;
    }

    @Override
    public boolean isPopulationSlot(int slot) {
        return slot >= 6 && slot <= 8;
    }

    @Override
    public List<Integer> populationSlots() {
        return List.of(6, 7, 8);
    }

    @Override
    public boolean isFoodSlot(int slot) {
        return slot >= 0 && slot <= 5;
    }

    @Override
    public long getTankCapacity() {
        return TANK_CAPACITY;
    }

    @Override
    protected int getVerticalUpOffset() {
        return 4;
    }

    @Override
    protected int getVerticalDownOffset() {
        return 4;
    }

    // ==================== serverTick ====================

    /**
     * 服务端每 tick 调用，委托给 {@link AbstractRangeMachineBlockEntity#serverTick}。
     * <p>处理粒子边框显示和工作进度推进。
     *
     * @param level 当前世界
     * @param pos   方块坐标
     * @param state 方块状态
     * @param be    此方块实体
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   VillageFarmBlockEntity be) {
        AbstractRangeMachineBlockEntity.serverTick(level, pos, state, be);
    }

    // ==================== GUI ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.civilizationevolution.village_farm");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.village_farm");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new VillageFarmMenu(containerId, inventory, this, this.data);
    }
}

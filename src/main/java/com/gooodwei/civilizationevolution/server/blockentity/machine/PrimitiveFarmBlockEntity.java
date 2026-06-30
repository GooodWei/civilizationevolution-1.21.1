package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.machine.AbstractFarmBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.machine.AbstractRangeMachineBlockEntity;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveFarmMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 原始农场方块实体（Tier 0）。
 *
 * <p>继承自 {@link AbstractFarmBlockEntity}，仅提供 Tier 0 特有的参数。
 * 所有农场业务逻辑（作物催熟、水管理、食物消耗）由父类提供。
 *
 * <p>槽位布局（共 9 个，与原始牧场相同）：
 * <ul>
 *   <li>槽位 0-5：食物输入槽（2×3）</li>
 *   <li>槽位 6-8：人口输入槽（农民）</li>
 * </ul>
 *
 * <p>Tier 0 参数：
 * <ul>
 *   <li>储罐容量：8000 mB（8 桶）</li>
 *   <li>人口食物消耗：8（每人每次工作消耗 8 个食物）</li>
 * </ul>
 */
public class PrimitiveFarmBlockEntity extends AbstractFarmBlockEntity {

    /** 机器槽位总数 */
    public static final int SIZE = 9;

    /** Tier 0 储罐容量：8000 mB = 8 桶 */
    private static final long TANK_CAPACITY = 8000L;

    /**
     * 构造原始农场方块实体。
     *
     * @param pos   方块坐标
     * @param state 方块状态
     */
    public PrimitiveFarmBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.PRIMITIVE_FARM.get(), pos, state, SIZE);
    }

    // ==================== 抽象方法实现 ====================

    @Override
    public Tier getTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    @Override
    protected String getMachineConfigKey() {
        return PopulationMachineConfig.PRIMITIVE_FARM;
    }

    @Override
    protected int getFoodPerPopulation() {
        return 8;
    }

    @Override
    public long getTankCapacity() {
        return TANK_CAPACITY;
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
                                   PrimitiveFarmBlockEntity be) {
        AbstractRangeMachineBlockEntity.serverTick(level, pos, state, be);
    }

    // ==================== GUI ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.civilizationevolution.primitive_farm");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.primitive_farm");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PrimitiveFarmMenu(containerId, inventory, this, this.data);
    }
}

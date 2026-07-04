package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.config.CivilizationMachineConfig;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class VillageHarvesterBlockEntity extends AbstractHarvesterBlockEntity {
    /** Tier 1 储罐容量：16000 mB = 16 桶 */
    private static final long TANK_CAPACITY = 16000;
    /** 机器槽位总数：6 食物 + 3 人口 + 1 武器 + 9 输出 = 19 */
    public static final int SIZE = 19;

    public VillageHarvesterBlockEntity( BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.VILLAGE_HARVESTER.get(), pos, state, SIZE);
    }

    @Override
    public boolean isOutputSlot(int slot) {
        return slot >= 10 && slot <= 18;
    }

    @Override
    protected String getMachineConfigKey() {
        return CivilizationMachineConfig.VILLAGE_HARVESTER;
    }

    /**
     * 储罐总容量（mB），由 Tier 决定。
     * <p>例如：Tier 0 = 8000 mB（8 桶），Tier 1 = 16000 mB（16 桶）。
     *
     * @return 储罐容量（mB）
     */
    @Override
    public long getTankCapacity() {
        return TANK_CAPACITY;
    }

    @Override
    public String getWorkerCareer() {
        return CareerNames.FARMER;
    }

    @Override
    public Tier getTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    public boolean isPopulationSlot(int slot) {
        return slot >= 6 && slot <= 8;
    }

    @Override
    public boolean isFoodSlot(int slot) {
        return slot >= 0 && slot <= 5;
    }

    @Override
    protected int getVerticalUpOffset() {
        return 4;
    }

    @Override
    protected int getVerticalDownOffset() {
        return 4;
    }

    @Override
    public List<Integer> populationSlots() {
        return List.of(6, 7, 8);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.civilizationevolution.village_harvester");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.village_harvester");
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VillageHarvesterBlockEntity blockEntity) {
        AbstractHarvesterBlockEntity.serverTick(level, pos, state, blockEntity);
    }

    /**
     * 接收客户端发来的字段更新（由 {@code UpdateMachineFieldPayload} 携带）。
     * 运行在服务端。新增字段时只需在实现类中新增 case 分支。
     *
     * @param fieldId 字段编号（各 BE 自行定义 FIELD_XXX 常量）
     * @param data    客户端提交的数据，通过 CompoundTag 携带任意类型
     */
    @Override
    public void onClientUpdate(int fieldId, CompoundTag data) {

    }
}

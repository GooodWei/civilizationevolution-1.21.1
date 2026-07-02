package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class VillageHarvesterBlockEntity extends AbstractHarvesterBlockEntity {
    protected VillageHarvesterBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
    }

    @Override
    protected String getMachineConfigKey() {
        return PopulationMachineConfig.VILLAGE_HARVESTER;
    }

    /**
     * 储罐总容量（mB），由 Tier 决定。
     * <p>例如：Tier 0 = 8000 mB（8 桶），Tier 1 = 16000 mB（16 桶）。
     *
     * @return 储罐容量（mB）
     */
    @Override
    public long getTankCapacity() {
        return 16000;
    }

    @Override
    public String getWorkerCareer() {
        return CareerNames.FARMER;
    }
}

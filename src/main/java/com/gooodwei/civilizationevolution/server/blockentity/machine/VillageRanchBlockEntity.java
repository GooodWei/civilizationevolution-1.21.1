package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.config.CivilizationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.machine.VillageRanchMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.gooodwei.civilizationevolution.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 村庄牧场方块实体（Tier 1）。
 *
 * <p>继承自 {@link AbstractRanchBlockEntity}，仅提供 Tier 1 特有的参数。
 * 所有牧场业务逻辑（动物扫描、喂养、食物消耗）由父类提供。
 *
 * <p>槽位布局（共 9 个）：
 * <ul>
 *   <li>槽位 0-5：食物输入槽（2×3）</li>
 *   <li>槽位 6-8：人口输入槽（牧人）</li>
 * </ul>
 *
 * <p>无输出槽位，无武器槽位。
 */
public class VillageRanchBlockEntity extends AbstractRanchBlockEntity {
    public static final int SIZE = 9;

    public VillageRanchBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.VILLAGE_RANCH.get(), pos, blockState, SIZE);
    }

    // ==================== 抽象方法实现 ====================

    @Override
    public Tier getTier() {
        return CivilizationTiers.VILLAGE;
    }

    @Override
    protected String getMachineConfigKey() {
        return CivilizationMachineConfig.VILLAGE_RANCH;
    }

    @Override
    public String getWorkerCareer() {
        return CareerNames.SHEPHERD;
    }

    @Override
    protected TagKey<Block> getConflictTag() {
        return ModTags.PRIMITIVE_RANCH_CONFLICTS;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   VillageRanchBlockEntity be) {
        AbstractRanchBlockEntity.serverTick(level, pos, state, be);
    }

    // ==================== GUI ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.civilizationevolution.village_ranch");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.village_ranch");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new VillageRanchMenu(containerId, inventory, this, this.data);
    }
}

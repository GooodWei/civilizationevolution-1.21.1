package com.gooodwei.civilizationevolution.server.blockentity.fieldmachine;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractHuntingGroundBlockEntity;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.PrimitiveHuntingGroundMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.gooodwei.civilizationevolution.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 原始狩猎场方块实体（Tier 0）。
 *
 * <p>继承自 {@link AbstractHuntingGroundBlockEntity}，仅提供 Tier 0 特有的参数。
 * 所有狩猎业务逻辑（实体扫描、战利品生成、食物消耗）由父类提供。
 *
 * <p>槽位布局（共 19 个）：
 * <ul>
 *   <li>槽位 0-5：食物输入槽</li>
 *   <li>槽位 6-8：人口输入槽（猎人）</li>
 *   <li>槽位 9：武器槽</li>
 *   <li>槽位 10-18：输出槽（战利品）</li>
 * </ul>
 */
public class PrimitiveHuntingGroundBlockEntity extends AbstractHuntingGroundBlockEntity {
    public static final int SIZE = 19;

    public PrimitiveHuntingGroundBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.PRIMITIVE_HUNTING_GROUND.get(), pos, blockState, SIZE);
    }

    // ==================== 抽象方法实现 ====================

    @Override
    public Tier getTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    @Override
    protected String getMachineConfigKey() {
        return PopulationMachineConfig.PRIMITIVE_HUNTING_GROUND;
    }

    @Override
    protected int getFoodPerPopulation() {
        return 32;
    }

    @Override
    protected TagKey<Block> getConflictTag() {
        return ModTags.HUNTING_GROUND_CONFLICTS;
    }

    // ==================== serverTick ====================

    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   PrimitiveHuntingGroundBlockEntity be) {
        AbstractHuntingGroundBlockEntity.serverTick(level, pos, state, be);
    }

    // ==================== GUI ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.civilizationevolution.primitive_hunting_ground");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.primitive_hunting_ground");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new PrimitiveHuntingGroundMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PrimitiveHuntingGroundMenu(containerId, inventory, this, this.data);
    }
}

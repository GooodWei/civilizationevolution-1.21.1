package com.gooodwei.civilizationevolution.server.blockentity.multiblock;

import com.gooodwei.civilizationevolution.api.IAdaptivePollingMachine;
import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.block.machine.AbstractMachineBlock;
import com.gooodwei.civilizationevolution.server.config.CivilizationMachineConfig;
import com.gooodwei.civilizationevolution.server.config.MultiBlockConfig;
import com.gooodwei.civilizationevolution.server.recipe.GrindingRecipe;
import com.gooodwei.civilizationevolution.server.recipe.GrindingRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;


public class AbstractMillBlockEntity extends AbstractMultiBlockMachineBlockEntity implements IAdaptivePollingMachine<GrindingRecipe, GrindingRecipeInput> {
    private int idleTicks;
    private int recipeWorkProgress;
    private GrindingRecipe currentRecipe;


    protected AbstractMillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractMillBlockEntity be) {
        be.tickAdaptivePolling();
    }

    /**
     * 返回 {@link MultiBlockConfig} 中对应的结构标识 key。
     *
     * @return 配置文件中的结构 key（如 {@code "primitive_doctor_cabin"}）
     */
    @Override
    public String getConfigKey() {
        return "";
    }

    @Override
    public boolean isSelfScheduled() {
        return true;
    }

    /**
     * 完成一次工作所需的总 tick 数
     */
    @Override
    public int getWorkTotalTime() {
        return 0;
    }

    /**
     * 每次工作周期完成后每个人口的年龄增长量
     */
    @Override
    public int getAgeIncrement() {
        return CivilizationMachineConfig.getAgeIncrement(getConfigKey());
    }

    /**
     * 此机器的 Tier 等级。
     *
     * <p>每个具体机器子类<b>必须</b>覆写此方法，显式声明所属时代。
     * 与对应 Block 的 {@link AbstractMachineBlock#getTier()} 保持相同值，
     * 确保绑定逻辑与物品 tooltip 一致。
     *
     * @return 此机器的 Tier 等级
     */
    @Override
    public Tier getTier() {
        return null;
    }

    /**
     * 判断某槽位是否接受人口物品。
     * 在 {@code CampMenu} 中用于 {@code Slot.mayPlace} 限制，
     * 在 {@code canPlaceItem} 中用于漏斗等自动化限制。
     *
     * @param slot
     */
    @Override
    public boolean isPopulationSlot(int slot) {
        return slot < 4;
    }

    /**
     * 返回所有人口槽位的索引列表。
     * 实现类应返回不可变列表，如 {@code List.of(4, 5)}。
     * 接口内部遍历方法均基于此列表，避免每次全容器扫描。
     */
    @Override
    public List<Integer> populationSlots() {
        return List.of(0, 1, 2, 3);
    }

    /**
     * 判断某槽位是否仅允许代码产出（玩家 / 漏斗均不可放入）。
     *
     * @param slot
     */
    @Override
    public boolean isOutputSlot(int slot) {
        return false;
    }

    /**
     * 本机器要求的工作职业名称。
     * 每个具体机器<b>必须</b>覆写，显式声明所需职业。
     *
     * @return 职业名称常量（如 {@code CareerNames.FARMER}）
     */
    @Override
    public String getWorkerCareer() {
        return CareerNames.MASON;
    }

    @Override
    protected Component getDefaultName() {
        return null;
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return null;
    }

    @Override
    public Optional<GrindingRecipe> findAndValidateRecipe() {
        return Optional.empty();
    }

    @Override
    public void executeRecipe(GrindingRecipe recipe) {

    }

    @Override
    public int getRecipeWorkProgress() {
        return recipeWorkProgress;
    }

    @Override
    public void resetWorkProgress() {
        this.recipeWorkProgress = 0;
        this.currentRecipe = null;
    }

    @Override
    public int getMachineTier() {
        return getTier().getLevel();
    }

    @Override
    public int getIdleTicks() {
        return idleTicks;
    }

    @Override
    public void setIdleTicks(int ticks) {
        this.idleTicks = ticks;
    }

    @Override
    public boolean isMachineReady() {
        return mbs().structureFormed && isBound();
    }
}

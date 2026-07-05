package com.gooodwei.civilizationevolution.server.blockentity.multiblock;

import com.gooodwei.civilizationevolution.api.IAdaptivePollingMachine;
import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.block.machine.AbstractMachineBlock;
import com.gooodwei.civilizationevolution.server.config.CivilizationMachineConfig;
import com.gooodwei.civilizationevolution.server.config.MultiBlockConfig;
import com.gooodwei.civilizationevolution.server.recipe.GrindingOutput;
import com.gooodwei.civilizationevolution.server.recipe.GrindingRecipe;
import com.gooodwei.civilizationevolution.server.recipe.GrindingRecipeInput;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Optional;


public class AbstractMillBlockEntity extends AbstractMultiBlockMachineBlockEntity implements IAdaptivePollingMachine<GrindingRecipe, GrindingRecipeInput> {
    private int idleTicks;
    private int recipeWorkProgress;
    private GrindingRecipe currentRecipe;
    /** 当前配方输入物品所在的仓位置（跨 tick 追踪消耗来源） */
    private BlockPos inputHatchPos;
    /** 当前配方输入物品所在的槽位索引 */
    private int inputSlot = -1;
    /** 客户端同步数据 */
    protected final ContainerData data;


    protected AbstractMillBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
        this.data = createData();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AbstractMillBlockEntity be) {
        be.tickAdaptivePolling();
    }

    protected ContainerData createData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> getRecipeWorkProgress();
                    case 1 -> getCurrentWorkTime();
                    default -> 0;
                };
            }
            @Override
            public void set(int index, int value) { /* 只读 */ }
            @Override
            public int getCount() { return 2; }
        };
    }

    /** 获取当前配方的工作总时间（供 ContainerData 和 GUI 使用） */
    protected int getCurrentWorkTime() {
        return currentRecipe != null ? currentRecipe.workTime() : 1;
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

    @Override
    public boolean canWork() {
        return isBound() && !getAvailableWorkers().isEmpty();
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
        Level level = getLevel();
        if (level == null || level.isClientSide) {
            return Optional.empty();
        }
        // 已有进行中配方 → 验证输入是否仍存在
        if (currentRecipe != null && inputHatchPos != null && inputSlot >= 0){
            BlockEntity be = level.getBlockEntity(inputHatchPos);
            if (be instanceof Container container && inputSlot < container.getContainerSize()){
                ItemStack stack = container.getItem(inputSlot);
                if (currentRecipe.matches(new GrindingRecipeInput(stack), level)) {
                    return Optional.of(currentRecipe);
                }
            }
            // 输入被移除 → 取消当前配方
            resetWorkProgress();
            return Optional.empty();
        }
        // 扫描所有物品输入仓，查找新配方
        int machineTier = getMachineTier();
        for (BlockPos hatchPos : getItemInputHatches()) {
            BlockEntity be = level.getBlockEntity(hatchPos);
            if (!(be instanceof Container container)) continue;

            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) continue;

                Optional<GrindingRecipe> match = GrindingRecipe.findMatch(level, stack);
                if (match.isPresent()) {
                    GrindingRecipe recipe = match.get();
                    if (recipe.minHandleTier() <= machineTier) {
                        this.currentRecipe = recipe;
                        this.inputHatchPos = hatchPos;
                        this.inputSlot = slot;
                        return Optional.of(recipe);
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * 每 tick 推进配方工作进度。
     *
     * <p>首 tick 消耗 1 个输入物品；
     * 进度达到 {@link GrindingRecipe#workTime()} 时产出物品并重置。
     */
    @Override
    public void executeRecipe(GrindingRecipe recipe) {
        // 首 tick：消耗输入物品
        if (recipeWorkProgress == 0 && inputHatchPos != null) {
            Level level = getLevel();
            if (level != null) {
                BlockEntity be = level.getBlockEntity(inputHatchPos);
                if (be instanceof Container container) {
                    ItemStack stack = container.getItem(inputSlot);
                    if (!stack.isEmpty()) {
                        stack.shrink(1);
                        container.setItem(inputSlot, stack.isEmpty() ? ItemStack.EMPTY : stack);
                    }
                }
            }
        }

        recipeWorkProgress++;

        if (recipeWorkProgress >= recipe.workTime()) {
            produceOutputs(recipe);
            resetWorkProgress();
        }

        setChanged();
    }

    /**
     * 将配方产物插入所有物品输出仓。
     *
     * <p>若机器 Tier 达到产物的 {@code allowExtraOutputTier} 门槛，
     * 则按效率倍率增产；{@code allowExtraOutputTier == -1} 表示不受效率影响。
     */
    private void produceOutputs(GrindingRecipe recipe) {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        int machineTier = getMachineTier();
        // 计算本周期工作效率（含食物消耗）
        float efficiency = calculateEfficiency();
        List<BlockPos> outputHatches = getItemOutputHatches();

        for (GrindingOutput output : recipe.output()) {
            int baseCount = output.item().getCount();
            int actualCount = baseCount;

            // 效率增产计算
            int threshold = output.allowExtraOutputTier();
            if (threshold >= 0 && machineTier >= threshold) {
                // 效率增产：基础数量 × 效率，至少保底基础数量
                actualCount = Math.max(baseCount, (int) Math.floor(baseCount * efficiency));
            }

            // 插入输出仓
            int remaining = actualCount;
            for (BlockPos hatchPos : outputHatches) {
                if (remaining <= 0) break;
                BlockEntity be = level.getBlockEntity(hatchPos);
                if (!(be instanceof Container container)) continue;

                for (int slot = 0; slot < container.getContainerSize() && remaining > 0; slot++) {
                    ItemStack slotStack = container.getItem(slot);
                    ItemStack outputStack = output.item().copy();
                    outputStack.setCount(1);

                    if (slotStack.isEmpty()) {
                        // 空槽位 → 直接放入
                        outputStack.setCount(Math.min(remaining, outputStack.getMaxStackSize()));
                        container.setItem(slot, outputStack);
                        remaining -= outputStack.getCount();
                    } else if (ItemStack.isSameItemSameComponents(slotStack, outputStack)
                            && slotStack.getCount() < slotStack.getMaxStackSize()) {
                        // 同物品 → 堆叠
                        int canAdd = Math.min(remaining, slotStack.getMaxStackSize() - slotStack.getCount());
                        slotStack.grow(canAdd);
                        remaining -= canAdd;
                    }
                }
            }
        }
    }

    @Override
    public float consumeAndGetFoodFactor() {
        List<ItemStack> workers = getAvailableWorkers();
        return consumeFoodFromHatchesWithFallback(
                workers.size() * getFoodPerPopulation(),
                workers.size() * getFoodPerPopulation());
    }

    @Override
    public int getRecipeWorkProgress() {
        return recipeWorkProgress;
    }

    @Override
    public void resetWorkProgress() {
        this.recipeWorkProgress = 0;
        this.currentRecipe = null;
        this.inputHatchPos = null;
        this.inputSlot = -1;
    }

    @Override
    public int getMachineTier() {
        Tier tier = getTier();
        return tier != null ? tier.getLevel() : 0;
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

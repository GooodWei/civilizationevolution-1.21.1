package com.gooodwei.civilizationevolution.server.blockentity.multiblock;

import com.gooodwei.civilizationevolution.api.IMultiBlockMachine;
import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import com.gooodwei.civilizationevolution.api.MultiBlockState;
import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import com.gooodwei.civilizationevolution.server.blockentity.machine.AbstractMachineBlockEntity;
import com.gooodwei.civilizationevolution.server.config.MultiBlockConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 多方块机器通用父类。
 *
 * <p>位于 {@link AbstractMachineBlockEntity} 和具体多方块机器
 * （如 {@code AbstractHospitalBlockEntity}）之间。
 * 多方块核心逻辑已提取到 {@link IMultiBlockMachine} 接口的 default 方法中，
 * 本类仅持有 {@link MultiBlockState} 状态对象并实现接口契约。
 *
 * <p>子类只需覆写 {@link #getConfigKey()} 返回
 * {@link MultiBlockConfig} 中对应的结构 key。
 *
 * <h3>JSON 结构定义格式</h3>
 * <p>配置文件位于 {@code config/civilizationevolution/multi_blocks.json}，
 * 详见 {@link IMultiBlockMachine} 和 {@link MultiBlockConfig}。
 *
 * @see IMultiBlockMachine
 * @see IMultiBlockPart
 * @see MultiBlockConfig
 */
public abstract class AbstractMultiBlockMachineBlockEntity
        extends AbstractMachineBlockEntity
        implements IMultiBlockMachine {

    /** 多方块运行时状态（结构成型标识、位置缓存、解析结果等） */
    private final MultiBlockState mbs = new MultiBlockState();

    // ==================== 构造器 ====================

    protected AbstractMultiBlockMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
    }

    // ==================== 子类必须覆写 ====================

    /**
     * 返回 {@link MultiBlockConfig} 中对应的结构标识 key。
     *
     * @return 配置文件中的结构 key（如 {@code "primitive_doctor_cabin"}）
     */
    @Override
    public abstract String getConfigKey();

    // ==================== IMultiBlockMachine 契约 ====================

    @Override
    public MultiBlockState mbs() {
        return mbs;
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    public void dropAllItems() {
        if (getLevel() instanceof ServerLevel sl) {
            for (int i = 0; i < getContainerSize(); i++) {
                ItemStack stack = getItem(i);
                if (!stack.isEmpty()) {
                    Block.popResource(sl, worldPosition, stack);
                    setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    // ==================== 生命周期 ====================

    @Override
    public void onLoad() {
        super.onLoad();
        // 延迟 40 tick（2 秒）后重新验证结构，给周围 chunk 加载留时间
        onMultiBlockLoad();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveMultiBlockNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadMultiBlockNBT(tag);
    }

    // ==================== 食物系统（仓室来源） ====================

    /**
     * 从食物仓室消耗食物并计算食物因子，含完整回退链。
     *
     * <p>此方法镜像 {@code IPopulationMachine.consumeFoodWithFallback}，
     * 但食物来源为多方块食物仓室（而非内部食物槽位）。
     *
     * <p><b>正常路径（食物充足）：</b>
     * <ol>
     *   <li>从食物仓室消耗食物，仅合格部分（eligibleNeeded）计入营养值</li>
     *   <li>按统一两段式公式计算食物因子：
     *       avg = 人均校准营养值，avg &lt; R → avg/R，avg ≥ R → 1+log_b(avg/R)</li>
     *   <li>额外喂饱食度 &lt; 100 的人口</li>
     * </ol>
     *
     * <p><b>回退路径（食物不足）：</b>
     * <ol>
     *   <li>不消耗任何食物物品</li>
     *   <li>调用 {@code applyFoodFallback}：扣除 NBT 饱食度 → 扣除生命值 → 标记死亡</li>
     * </ol>
     *
     * @param totalNeeded   总消耗份数（所有需吃饭的人口 × foodPerPopulation）
     * @param eligibleNeeded 合格消耗份数（仅工作人口 × foodPerPopulation，参与因子计算）
     * @return 食物因子（正常公式值 / 0.75 / 0.5 / 0）
     */
    protected float consumeFoodFromHatchesWithFallback(int totalNeeded, int eligibleNeeded) {
        Level level = getLevel();
        if (level == null) return 0;
        List<BlockPos> hatches = getFoodHatches();
        if (hatches.isEmpty() || totalNeeded <= 0) return 1.0f;

        // ===== 第一步：统计可用食物总量 =====
        int available = 0;
        for (BlockPos hatchPos : hatches) {
            BlockEntity be = level.getBlockEntity(hatchPos);
            if (!(be instanceof com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractFoodInputHatchBlockEntity hatch))
                continue;
            for (int i = 0; i < hatch.getContainerSize(); i++) {
                ItemStack stack = hatch.getItem(i);
                if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                    available += stack.getCount();
                    if (available >= totalNeeded) break;
                }
            }
            if (available >= totalNeeded) break;
        }

        // ===== 第二步：食物充足 → 正常路径 =====
        if (available >= totalNeeded) {
            int remaining = totalNeeded;
            int eligibleRemaining = eligibleNeeded;
            int eligibleConsumed = 0;
            float totalNutrition = 0;
            float totalSaturation = 0;

            for (BlockPos hatchPos : hatches) {
                if (remaining <= 0) break;
                BlockEntity be = level.getBlockEntity(hatchPos);
                if (!(be instanceof com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractFoodInputHatchBlockEntity hatch))
                    continue;
                for (int i = 0; i < hatch.getContainerSize() && remaining > 0; i++) {
                    ItemStack stack = hatch.getItem(i);
                    if (stack.isEmpty() || !stack.has(DataComponents.FOOD)) continue;

                    FoodProperties food = stack.getFoodProperties(null);
                    float nutrition = food != null ? food.nutrition() : 0;
                    // 实际饱和度 = nutrition × saturationModifier × 2
                    float saturation = food != null ? nutrition * food.saturation() * 2 : 0;

                    int toRemove = Math.min(stack.getCount(), remaining);
                    stack.shrink(toRemove);
                    remaining -= toRemove;
                    hatch.setChanged();

                    int eligiblePortion = Math.min(toRemove, eligibleRemaining);
                    if (eligiblePortion > 0) {
                        totalNutrition += nutrition * eligiblePortion;
                        totalSaturation += saturation * eligiblePortion;
                        eligibleRemaining -= eligiblePortion;
                        eligibleConsumed += eligiblePortion;
                    }
                }
            }

            // 使用统一两段式食物因子公式
            double avgNutrition = eligibleConsumed > 0
                    ? (totalNutrition + totalSaturation) / eligibleConsumed : 0;
            float foodFactor = (float) (Math.round(
                    com.gooodwei.civilizationevolution.api.IPopulationMachine.calculateFoodFactor(avgNutrition)
                    * 1000.0) / 1000.0);

            // 额外喂食：对每个饱食度 < 100 的人口，喂一份食物
            for (int slot : populationSlots()) {
                ItemStack popStack = getContainer().getItem(slot);
                if (popStack.isEmpty() || PopulationNBT.isDead(popStack)) continue;
                if (PopulationNBT.getFood(popStack) >= 100) continue;

                for (BlockPos hatchPos : hatches) {
                    BlockEntity be = level.getBlockEntity(hatchPos);
                    if (!(be instanceof com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractFoodInputHatchBlockEntity hatch))
                        continue;
                    for (int i = 0; i < hatch.getContainerSize(); i++) {
                        ItemStack foodStack = hatch.getItem(i);
                        if (foodStack.isEmpty() || !foodStack.has(DataComponents.FOOD)) continue;

                        FoodProperties foodProps = foodStack.getFoodProperties(null);
                        int addedValue = (int) (foodProps.nutrition()
                                + foodProps.nutrition() * foodProps.saturation() * 2);
                        foodStack.shrink(1);
                        hatch.setChanged();

                        int newFood = PopulationNBT.getFood(popStack) + addedValue;
                        PopulationNBT.setFood(popStack, newFood);
                        break; // 该人口已喂，处理下一个人口
                    }
                    if (PopulationNBT.getFood(popStack) >= 100) break; // 已喂饱 → 下一个槽位
                }
            }

            return foodFactor;
        }

        // ===== 第三步：食物不足 → 回退路径 =====
        List<ItemStack> popStacks = new ArrayList<>();
        for (int slot : populationSlots()) {
            ItemStack popStack = getContainer().getItem(slot);
            if (!popStack.isEmpty()) popStacks.add(popStack);
        }
        return applyFoodFallback(popStacks);
    }
}

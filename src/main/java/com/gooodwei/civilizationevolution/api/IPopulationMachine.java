package com.gooodwei.civilizationevolution.api;

import com.gooodwei.civilizationevolution.api.career.Career;
import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import com.gooodwei.civilizationevolution.server.config.PopulationConfig;
import com.gooodwei.civilizationevolution.server.item.PopulationItem;
import com.gooodwei.civilizationevolution.server.population.Population;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 人口机器接口 —— 所有使用人口物品进行工作的方块实体都应实现此接口。
 *
 * <p>定义了三条核心契约：
 * <ul>
 *   <li><b>槽位分类</b> —— 哪些槽位接收人口物品、哪些槽位仅输出</li>
 *   <li><b>工作判定</b> —— 当前是否满足工作条件</li>
 *   <li><b>人口老化</b> —— 工作周期结束后所有人口年龄 +1</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * public class CampBlockEntity extends BaseContainerBlockEntity
 *         implements MenuProvider, IPopulationMachine {
 *
 *     Override public Container getContainer() { return this; }
 *     Override public int getWorkTotalTime() { return 200; }
 *     Override public boolean isPopulationSlot(int slot) { return slot == 4 || slot == 5; }
 *     Override public boolean isOutputSlot(int slot) { return slot >= 6; }
 *
 *     // canWork() 按需覆写
 * }
 * }</pre>
 *
 * @see IPopulationManager#addAge(ItemStack)
 */
public interface IPopulationMachine {

    // ==================== 基础信息 ====================

    /** 返回方块实体自身的 Container（通常直接 return this） */
    Container getContainer();

    /** 完成一次工作所需的总 tick 数 */
    int getWorkTotalTime();

    /** 每次工作周期完成后每个人口的年龄增长量 */
    int getAgeIncrement();

    /**
     * 此机器的 Tier 等级。
     *
     * <p>Tier 0 = 原始时代（营地、狩猎场、原始牧场），
     * Tier 1 = 村庄时代，以此类推。
     * 控制器只能绑定 tier ≤ 自身 tier 的机器。
     *
     * <p>附属模组可覆写此方法自定义机器等级。
     * 默认返回{@link com.gooodwei.civilizationevolution.api.tier.TierRegistry#getByLevel(int) 获取 Tier 0}（原始时代）。
     */
    default com.gooodwei.civilizationevolution.api.tier.Tier getTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    // ==================== 槽位分类 ====================

    /**
     * 判断某槽位是否接受人口物品。
     * 在 {@code CampMenu} 中用于 {@code Slot.mayPlace} 限制，
     * 在 {@code canPlaceItem} 中用于漏斗等自动化限制。
     */
    boolean isPopulationSlot(int slot);

    /**
     * 返回所有人口槽位的索引列表。
     * 实现类应返回不可变列表，如 {@code List.of(4, 5)}。
     * 接口内部遍历方法均基于此列表，避免每次全容器扫描。
     */
    List<Integer> populationSlots();

    /**
     * 判断某槽位是否仅允许代码产出（玩家 / 漏斗均不可放入）。
     */
    boolean isOutputSlot(int slot);

    /**
     * 判断某槽位是否用于存放食物。
     * 默认返回 false，需要食物系统的机器应覆写。
     */
    default boolean isFoodSlot(int slot) {
        return false;
    }

    // ==================== 食物系统 ====================

    /**
     * 检查食物槽位中是否有足够食物供所有人工作。
     *
     * @param foodPerPopulation 每个人口槽位每次工作消耗的食物量
     * @return true 表示食物充足
     */
    default boolean hasEnoughFood(int foodPerPopulation) {
        Container c = getContainer();
        int needed = countPopulationSlots() * foodPerPopulation;

        int total = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (!isFoodSlot(i)) continue;
            ItemStack stack = c.getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                total += stack.getCount();
                if (total >= needed) return true;
            }
        }
        return false;
    }

    /**
     * 从食物槽位中消耗食物，并计算<b>食物因子</b>用于调节工作效率。
     *
     * <p>先统计消耗食物的饱食度+饱和度总和，再交给 {@code formula} 计算最终因子。
     * 不同机器可传入不同的公式（开根号、取对数等）。
     *
     * <p>调用前必须确保 {@link #hasEnoughFood(int)} 返回 true。
     *
     * @param foodPerPopulation 每个人口槽位每次工作消耗的食物量
     * @param formula 食物因子计算公式，输入为消耗食物的 (饱食度+饱和度) 总和，
     *                输出为食物因子
     * @return 食物因子（0.0 ~ N），取小数点后三位
     */
    default float consumeAndGetFoodFactor(int foodPerPopulation,
                                          java.util.function.DoubleUnaryOperator formula) {
        Container c = getContainer();
        int needed = countPopulationSlots() * foodPerPopulation;

        int remaining = needed;
        float totalNutrition = 0;
        float totalSaturation = 0;

        for (int i = 0; i < c.getContainerSize() && remaining > 0; i++) {
            if (!isFoodSlot(i)) continue;
            ItemStack stack = c.getItem(i);
            if (stack.isEmpty() || !stack.has(DataComponents.FOOD)) continue;

            FoodProperties food = stack.getFoodProperties(null);
            float nutrition = food != null ? food.nutrition() : 0;
            // 实际饱和度 = nutrition × saturationModifier × 2
            float saturation = food != null ? nutrition * food.saturation() : 0;

            int toRemove = Math.min(stack.getCount(), remaining);
            stack.shrink(toRemove);
            remaining -= toRemove;

            totalNutrition += nutrition * toRemove;
            totalSaturation += saturation * toRemove;
        }

        double factor = formula.applyAsDouble(totalNutrition + totalSaturation);
        return (float) (Math.round(factor * 1000.0) / 1000.0);
    }

    /**
     * 统一食物消耗方法，含完整三级回退链和人口喂食。
     *
     * <p><b>食物充足时（正常路径）：</b>
     * <ol>
     *   <li>消耗食物物品用于本次工作，按 {@code formula} 计算食物因子</li>
     *   <li>额外遍历所有人口槽位：若某人口 NBT 饱食度 &lt; 100，额外消耗一份食物，
     *       将其 {@code nutrition + saturation × 2} 加到该人口 NBT 饱食度上</li>
     *   <li>喂食只喂饱食度 &lt; 100 的人口，喂食后允许超过 100</li>
     *   <li>喂食消耗的食物<b>不参与</b>食物因子计算</li>
     * </ol>
     *
     * <p><b>食物不足时（回退路径）：</b>
     * <ol>
     *   <li>不消耗任何食物物品</li>
     *   <li>从每个人口物品的 NBT 饱食度扣除 16 点 → 食物因子 0.75</li>
     *   <li>饱食度不足 16 时，先扣尽饱食度，差额从生命值扣除 → 食物因子 0.5</li>
     *   <li>生命值降至 ≤ 0 时标记人口死亡</li>
     * </ol>
     *
     * <p>食物因子取最差情况：只要有任何一个人口扣了生命值，整体因子就是 0.5。
     *
     * @param foodPerPopulation 每个人口槽位每次工作消耗的食物物品量
     * @param formula           正常路径的食物因子计算公式
     * @param eligibleCount     符合工作要求的人口数量（影响营养值计算比例）。
     *                          传 -1 表示所有已填满的人口槽位均计入（营地模式）
     * @return 食物因子（正常公式值 / 0.75 / 0.5 / 0）
     */
    default float consumeFoodWithFallback(int foodPerPopulation,
                                          java.util.function.DoubleUnaryOperator formula,
                                          int eligibleCount) {
        Container c = getContainer();
        int filledSlots = countPopulationSlots();
        if (filledSlots == 0) return 0;

        int actualEligible = (eligibleCount < 0) ? filledSlots : eligibleCount;
        int totalNeeded = filledSlots * foodPerPopulation;
        int eligibleNeeded = Math.min(actualEligible * foodPerPopulation, totalNeeded);

        // ===== 第一步：统计可用食物物品总量 =====
        int available = 0;
        for (int i = 0; i < c.getContainerSize(); i++) {
            if (!isFoodSlot(i)) continue;
            ItemStack stack = c.getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                available += stack.getCount();
                if (available >= totalNeeded) break; // 提前退出
            }
        }

        // ===== 第二步：食物充足 → 正常路径 =====
        if (available >= totalNeeded) {
            // 2a. 消耗食物物品用于工作（复用现有逻辑）
            int remaining = totalNeeded;
            int eligibleRemaining = eligibleNeeded;
            float totalNutrition = 0;
            float totalSaturation = 0;

            for (int i = 0; i < c.getContainerSize() && remaining > 0; i++) {
                if (!isFoodSlot(i)) continue;
                ItemStack stack = c.getItem(i);
                if (stack.isEmpty() || !stack.has(DataComponents.FOOD)) continue;

                FoodProperties food = stack.getFoodProperties(null);
                float nutrition = food != null ? food.nutrition() : 0;
                float saturation = food != null ? nutrition * food.saturation() : 0;

                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;

                int eligiblePortion = Math.min(toRemove, eligibleRemaining);
                if (eligiblePortion > 0) {
                    totalNutrition += nutrition * eligiblePortion;
                    totalSaturation += saturation * eligiblePortion;
                    eligibleRemaining -= eligiblePortion;
                }
            }

            double factor = formula.applyAsDouble(totalNutrition + totalSaturation);
            float foodFactor = (float) (Math.round(factor * 1000.0) / 1000.0);

            // 2b. 额外喂食：对每个饱食度 < 100 的人口，喂一份食物
            for (int slot : populationSlots()) {
                ItemStack popStack = getPopulationStackUnchecked(slot);
                if (popStack == null || PopulationNBT.isDead(popStack)) continue;
                if (PopulationNBT.getFood(popStack) >= 100) continue;

                // 从食物槽位取一份食物来喂
                for (int i = 0; i < c.getContainerSize(); i++) {
                    if (!isFoodSlot(i)) continue;
                    ItemStack foodStack = c.getItem(i);
                    if (foodStack.isEmpty() || !foodStack.has(DataComponents.FOOD)) continue;

                    FoodProperties foodProps = foodStack.getFoodProperties(null);
                    int addedValue = (int) (foodProps.nutrition()
                            + foodProps.nutrition() * foodProps.saturation() * 2);
                    foodStack.shrink(1);

                    int newFood = PopulationNBT.getFood(popStack) + addedValue;
                    PopulationNBT.setFood(popStack, newFood);
                    break; // 该人口已喂，处理下一个人口
                }
            }

            return foodFactor;
        }

        // ===== 第三步：食物不足 → 回退路径 =====
        // 不消耗任何食物物品
        boolean anyHealthUsed = false;
        boolean anyFoodValueUsed = false;

        for (int slot : populationSlots()) {
            ItemStack popStack = getPopulationStackUnchecked(slot);
            if (popStack == null || PopulationNBT.isDead(popStack)) continue;

            int currentFood = PopulationNBT.getFood(popStack);
            int currentHealth = PopulationNBT.getHealth(popStack);

            int need = 16;
            int fromFood = Math.min(currentFood, need);
            if (fromFood > 0) {
                PopulationNBT.setFood(popStack, currentFood - fromFood);
                need -= fromFood;
                anyFoodValueUsed = true;
            }

            if (need > 0) {
                int newHealth = currentHealth - need;
                if (newHealth <= 0) {
                    PopulationNBT.markDead(popStack);
                } else {
                    PopulationNBT.setHealth(popStack, newHealth);
                }
                anyHealthUsed = true;
            }
        }

        if (anyHealthUsed) return 0.5f;
        if (anyFoodValueUsed) return 0.75f;
        return 0;
    }

    // ==================== 工作逻辑 ====================

    /**
     * 执行一次工作周期。由聚落控制器调度触发。
     * <p>默认仅处理人口年龄增长，子类应覆写加入健康波动和具体工作逻辑。
     *
     * @param level 当前世界（服务端）
     */
    default void executeWorkCycle(Level level) {
        ageAllPopulations(getAgeIncrement());
    }

    /** 此机器是否已被聚落控制器绑定 */
    boolean isBound();

    /** 设置此机器的绑定状态（由控制器调用） */
    void setBound(boolean bound);

    /** 获取此机器绑定的核心 UUID，未绑定时返回 null */
    String getBoundCoreUuid();

    /** 设置此机器绑定的核心 UUID（绑定/解绑时由控制器调用） */
    void setBoundCoreUuid(String uuid);

    /** 获取绑定的控制器所在维度 ID（如 "minecraft:overworld"），未绑定时返回 null */
    String getBoundControllerDimension();

    /** 设置绑定的控制器所在维度 ID（绑定/解绑时由控制器调用） */
    void setBoundControllerDimension(String dimension);

    /**
     * 机器是否由自身 serverTick 驱动工作周期（而非由控制器调度）。
     *
     * <p>默认返回 {@code false}——机器由控制器统一调度工作周期。
     * 若机器在 serverTick 中有独立的 {@code workProgress} 和
     * {@code executeWorkCycle} 触发逻辑，应覆写返回 {@code true}。
     * 控制器遍历绑定机器时会跳过自调度机器的 {@code executeWorkCycle}，
     * 避免和工作进度双重触发。
     *
     * @return true 表示机器自调度，控制器不应调用 executeWorkCycle
     */
    default boolean isSelfScheduled() {
        return false;
    }

    /**
     * 检查是否满足工作条件。
     * 未绑定控制器时一定返回 false；绑定后默认检查所有人口槽位均非空且为 {@link PopulationItem}，
     * 具体机器应覆写（如检查性别、年龄、职业等）。
     */
    default boolean canWork() {
        if (!isBound()) return false;
        Container c = getContainer();
        for (int slot : populationSlots()) {
            ItemStack stack = c.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof PopulationItem)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 所有人口槽位中的人口年龄按指定量增长。
     * <p>
     * 年龄增长前会先计算新年龄是否达到寿命上限：
     * <ul>
     *   <li>已达寿命 → 跳过（NBT 冻结，不再变化）</li>
     *   <li>即将达寿命 → 年龄锁定为寿命值，冻结</li>
     *   <li>安全范围 → 逐岁调用 {@link IPopulationManager#addAge}</li>
     * </ul>
     * 人口死亡后<b>不会移除物品</b>，仅冻结其所有 NBT 标签。
     *
     * @param increment 每人增加的岁数（通常由配置决定）
     */
    default void ageAllPopulations(int increment) {
        Container c = getContainer();
        IPopulationManager pm = CivilizationAPI.getPopulationManager();
        for (int slot : populationSlots()) {
            ItemStack stack = c.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof PopulationItem)) continue;
            // 已死亡 → 冻结，跳过
            if (PopulationNBT.isDead(stack)) continue;

            int newAge = PopulationNBT.getAge(stack) + increment;
            int lifespan = PopulationNBT.getLifespan(stack);

            if (newAge > lifespan) {
                // 超过寿命 → 标记死亡
                PopulationNBT.markDead(stack);
            } else {
                for (int a = 0; a < increment; a++) {
                    pm.addAge(stack);
                }
            }
        }
    }

    // ==================== 人口属性读写（委托 PopulationNBT） ====================

    // ---- Getter（单槽位）----

    default int getPopulationAge(int slot) {
        ItemStack stack = getPopulationStack(slot);
        return stack != null ? PopulationNBT.getAge(stack) : 0;
    }

    default int getPopulationLifespan(int slot) {
        ItemStack stack = getPopulationStack(slot);
        return stack != null ? PopulationNBT.getLifespan(stack) : 0;
    }

    default boolean getPopulationGender(int slot) {
        ItemStack stack = getPopulationStack(slot);
        return stack != null && PopulationNBT.getGender(stack);
    }

    default String getPopulationCareer(int slot) {
        ItemStack stack = getPopulationStack(slot);
        return stack != null ? PopulationNBT.getCareer(stack) : "";
    }

    default int getPopulationHealth(int slot) {
        ItemStack stack = getPopulationStack(slot);
        return stack != null ? PopulationNBT.getHealth(stack) : 0;
    }

    default int getPopulationFood(int slot) {
        ItemStack stack = getPopulationStack(slot);
        return stack != null ? PopulationNBT.getFood(stack) : 0;
    }

    default int getPopulationProficiency(int slot) {
        ItemStack stack = getPopulationStack(slot);
        return stack != null ? PopulationNBT.getProficiency(stack) : 0;
    }

    default double getPopulationWorkEfficiency(int slot) {
        ItemStack stack = getPopulationStack(slot);
        return stack != null ? PopulationNBT.getWorkEfficiency(stack) : 0.0;
    }

    default double getPopulationMentalState(int slot) {
        ItemStack stack = getPopulationStack(slot);
        return stack != null ? PopulationNBT.getMentalState(stack) : 0.0;
    }

    /** 判断指定槽位中的人口是否已死亡 */
    default boolean isPopulationDead(int slot) {
        ItemStack stack = getPopulationStack(slot);
        return stack == null || PopulationNBT.isDead(stack);
    }

    // ---- Setter（单槽位）----

    default void setPopulationAge(int slot, int age) {
        ItemStack stack = getPopulationStack(slot);
        if (stack != null) PopulationNBT.setAge(stack, age);
    }

    default void setPopulationLifespan(int slot, int lifespan) {
        ItemStack stack = getPopulationStack(slot);
        if (stack != null) PopulationNBT.setLifespan(stack, lifespan);
    }

    default void setPopulationGender(int slot, boolean gender) {
        ItemStack stack = getPopulationStack(slot);
        if (stack != null) PopulationNBT.setGender(stack, gender);
    }

    default void setPopulationCareer(int slot, String career) {
        ItemStack stack = getPopulationStack(slot);
        if (stack != null) PopulationNBT.setCareer(stack, career);
    }

    default void setPopulationHealth(int slot, int health) {
        ItemStack stack = getPopulationStack(slot);
        if (stack != null) PopulationNBT.setHealth(stack, health);
    }

    default void setPopulationFood(int slot, int food) {
        ItemStack stack = getPopulationStack(slot);
        if (stack != null) PopulationNBT.setFood(stack, food);
    }

    default void setPopulationProficiency(int slot, int proficiency) {
        ItemStack stack = getPopulationStack(slot);
        if (stack != null) PopulationNBT.setProficiency(stack, proficiency);
    }

    default void setPopulationWorkEfficiency(int slot, double value) {
        ItemStack stack = getPopulationStack(slot);
        if (stack != null) PopulationNBT.setWorkEfficiency(stack, value);
    }

    default void setPopulationMentalState(int slot, double value) {
        ItemStack stack = getPopulationStack(slot);
        if (stack != null) PopulationNBT.setMentalState(stack, value);
    }

    // ---- 波动 ----

    /**
     * 对所有存活人口的<b>健康度</b>进行随机波动。
     * <ul>
     *   <li>波动值在 [{@code minDelta}, {@code maxDelta}] 区间内随机选取</li>
     *   <li>波动后健康度最高不超过 100</li>
     *   <li>低于 0 → 标记该人口死亡（年龄设为寿命值，冻结）</li>
     *   <li>已死亡人口跳过不处理</li>
     * </ul>
     *
     * @param minDelta 波动下限（可为负数）
     * @param maxDelta 波动上限
     */
    default void fluctuateHealth(int minDelta, int maxDelta) {
        Container c = getContainer();
        RandomSource rand = RandomSource.create();
        for (int slot : populationSlots()) {
            ItemStack stack = c.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof PopulationItem)) continue;
            if (PopulationNBT.isDead(stack)) continue;

            int delta = rand.nextIntBetweenInclusive(minDelta, maxDelta);
            int newHealth = PopulationNBT.getHealth(stack) + delta;

            if (newHealth < 0) {
                // 健康度归零 → 标记死亡
                PopulationNBT.markDead(stack);
            } else {
                PopulationNBT.setHealth(stack, Math.min(newHealth, 100));
            }
        }
    }

    /**
     * 对所有存活人口的<b>饱食度</b>进行随机波动。
     * 波动后低于 0 取 0，最高不超过 100（<b>不会标记死亡</b>）。
     *
     * @param minDelta 波动下限（可为负数）
     * @param maxDelta 波动上限
     */
    default void fluctuateFood(int minDelta, int maxDelta) {
        Container c = getContainer();
        RandomSource rand = RandomSource.create();
        for (int slot : populationSlots()) {
            ItemStack stack = c.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof PopulationItem)) continue;
            if (PopulationNBT.isDead(stack)) continue;

            int delta = rand.nextIntBetweenInclusive(minDelta, maxDelta);
            int newFood = PopulationNBT.getFood(stack) + delta;

            PopulationNBT.setFood(stack, newFood < 0 ? 0 : Math.min(newFood, 100));
        }
    }

    /**
     * 对所有存活人口的<b>心理状态</b>进行随机波动。
     * 参数单位为百分之一（例：-5 = -0.05），波动后低于 0 取 0.0，最高不超过 1.0（<b>不会标记死亡</b>）。
     *
     * @param minDelta 波动下限（百分之一，可为负数）
     * @param maxDelta 波动上限（百分之一）
     */
    default void fluctuateMentalState(int minDelta, int maxDelta) {
        Container c = getContainer();
        RandomSource rand = RandomSource.create();
        for (int slot : populationSlots()) {
            ItemStack stack = c.getItem(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof PopulationItem)) continue;
            if (PopulationNBT.isDead(stack)) continue;

            double delta = rand.nextIntBetweenInclusive(minDelta, maxDelta) / 100.0;
            double newMental = PopulationNBT.getMentalState(stack) + delta;

            PopulationNBT.setMentalState(stack, newMental < 0.0 ? 0.0 : Math.min(newMental, 1.0));
        }
    }

    // ---- Setter（全部人口槽位）----

    default void setAllPopulationsAge(int age)              { forEachPopulationSlot(slot -> setPopulationAge(slot, age)); }
    default void setAllPopulationsLifespan(int lifespan)    { forEachPopulationSlot(slot -> setPopulationLifespan(slot, lifespan)); }
    default void setAllPopulationsGender(boolean gender)    { forEachPopulationSlot(slot -> setPopulationGender(slot, gender)); }
    default void setAllPopulationsCareer(String career)     { forEachPopulationSlot(slot -> setPopulationCareer(slot, career)); }
    default void setAllPopulationsHealth(int health)        { forEachPopulationSlot(slot -> setPopulationHealth(slot, health)); }
    default void setAllPopulationsFood(int food)            { forEachPopulationSlot(slot -> setPopulationFood(slot, food)); }
    default void setAllPopulationsProficiency(int proficiency) { forEachPopulationSlot(slot -> setPopulationProficiency(slot, proficiency)); }
    default void setAllPopulationsWorkEfficiency(double value) { forEachPopulationSlot(slot -> setPopulationWorkEfficiency(slot, value)); }
    default void setAllPopulationsMentalState(double value) { forEachPopulationSlot(slot -> setPopulationMentalState(slot, value)); }

    // ==================== 内部辅助 ====================

    /**
     * 获取指定人口槽位中的物品，若非人口槽位 / 空物品 / 非 PopulationItem 则返回 null。
     */
    private ItemStack getPopulationStack(int slot) {
        if (!populationSlots().contains(slot)) return null;
        return getPopulationStackUnchecked(slot);
    }

    /**
     * 获取人口槽位物品 —— 不检查槽位是否在 {@link #populationSlots()} 中。
     * 供已确认遍历人口槽位的内部方法使用，避免重复 contains 扫描。
     */
    private ItemStack getPopulationStackUnchecked(int slot) {
        Container c = getContainer();
        ItemStack stack = c.getItem(slot);
        if (stack.isEmpty() || !(stack.getItem() instanceof PopulationItem)) return null;
        return stack;
    }

    /** 统计当前已放入人口物品的槽位数量。 */
    private int countPopulationSlots() {
        int count = 0;
        for (int slot : populationSlots()) {
            if (getPopulationStackUnchecked(slot) != null) {
                count++;
            }
        }
        return count;
    }

    /** 遍历所有人口槽位，对每个人口物品执行操作。 */
    private void forEachPopulationSlot(java.util.function.IntConsumer action) {
        for (int slot : populationSlots()) {
            if (getPopulationStackUnchecked(slot) != null) {
                action.accept(slot);
            }
        }
    }

    // ==================== 槽位限制辅助 ====================

    /**
     * 供 {@code Container.canPlaceItem} 调用的统一槽位限制。
     * 子类的 {@code canPlaceItem(int, ItemStack)} 只需一行委托。
     *
     * <pre>{@code
     * &#64;Override
     * public boolean canPlaceItem(int slot, ItemStack stack) {
     *     return checkSlotRestriction(slot, stack) && super.canPlaceItem(slot, stack);
     * }
     * }</pre>
     */
    default boolean checkSlotRestriction(int slot, ItemStack stack) {
        if (isPopulationSlot(slot)) {
            return stack.getItem() instanceof PopulationItem;
        }
        if (isOutputSlot(slot)) {
            return false;
        }
        return true;
    }

    // ==================== 职业经验系统 ====================

    /**
     * 获取人口在指定职业上的经验值。
     *
     * <p>每个职业独立累计经验，例如一个失业人口可能在农场获得了 7 点农民经验、
     * 在教堂获得了 3 点牧师经验，互不干扰。
     *
     * <p>附属模组和各类机器可直接通过 {@code IPopulationMachine} 实例调用此方法，
     * 无需导入 {@link PopulationNBT}。
     *
     * @param stack      人口物品
     * @param careerName 职业名称（如 "farmer"）
     * @return 该职业的经验值，默认 0
     */
    default int getCareerExperience(ItemStack stack, String careerName) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag exps = tag.getCompound(Population.TAG_CAREER_EXPS);
        return exps.getInt(careerName);
    }

    /**
     * 增加人口在指定职业上的经验值，返回增加后的值。
     *
     * <p>典型用法：机器在工作周期给槽位中的失业人口增加目标职业经验。
     *
     * <pre>{@code
     * int newExp = addCareerExperience(stack, "farmer", 1);
     * if (newExp >= 8) {
     *     PopulationNBT.setCareer(stack, "farmer");
     *     setCareerExperience(stack, "farmer", 0);
     * }
     * }</pre>
     *
     * @param stack      人口物品
     * @param careerName 职业名称（如 "farmer"）
     * @param amount     增加量（通常为 1）
     * @return 增加后的经验值
     */
    default int addCareerExperience(ItemStack stack, String careerName, int amount) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag exps = tag.getCompound(Population.TAG_CAREER_EXPS);
        int newExp = exps.getInt(careerName) + amount;
        exps.putInt(careerName, newExp);
        tag.put(Population.TAG_CAREER_EXPS, exps);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return newExp;
    }

    /**
     * 设置人口在指定职业上的经验值。
     *
     * <p>通常用于转职后清零经验。
     *
     * @param stack      人口物品
     * @param careerName 职业名称（如 "farmer"）
     * @param exp        经验值（0 表示清零）
     */
    default void setCareerExperience(ItemStack stack, String careerName, int exp) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        CompoundTag exps = tag.getCompound(Population.TAG_CAREER_EXPS);
        exps.putInt(careerName, exp);
        tag.put(Population.TAG_CAREER_EXPS, exps);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /**
     * 判断人口在指定职业上的经验是否达到阈值。
     *
     * @param stack      人口物品
     * @param careerName 职业名称
     * @param threshold  阈值
     * @return true 表示经验 ≥ 阈值
     */
    default boolean hasCareerExpReached(ItemStack stack, String careerName, int threshold) {
        return getCareerExperience(stack, careerName) >= threshold;
    }

    /**
     * 获得职业经验的人口基础条件检查。
     *
     * @deprecated 父职业门控逻辑已内聚到 {@link IPopulationItem#addApprenticeExp} 内部，
     *             此方法不再被调用，保留仅供外部参考。
     *
     * @param stack 人口物品
     * @return true 表示该人口可以获得职业经验
     */
    @Deprecated
    default boolean canGainCareerExperience(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PopulationItem)) return false;
        if (PopulationNBT.isDead(stack)) return false;
        return CareerNames.UNEMPLOYED.equals(PopulationNBT.getCareer(stack));
    }

    /**
     * 遍历所有人口槽位，委托 {@link IPopulationItem#addApprenticeExp} 处理学徒经验。
     *
     * <p>此方法仅做遍历委托，不含任何门控/晋升逻辑。
     * 父职业门控、经验累积、阈值检查、转职、清除经验均由
     * {@link IPopulationItem#addApprenticeExp(ItemStack, String, int)} 内部处理。
     *
     * <p>典型用法：在 {@code executeWorkCycle()} 的效率计算之前调用，
     * 确保新转职人口立即参与当次工作。
     *
     * <pre>{@code
     * addApprenticeExpToPopulationSlots(getWorkerCareer(), getApprenticeExpPerCycle());
     * }</pre>
     *
     * @param targetCareer 目标职业名称（如 "farmer"、"butcher"）
     * @param amount       每次工作周期给予的经验量
     */
    default void addApprenticeExpToPopulationSlots(String targetCareer, int amount) {
        for (int slot : populationSlots()) {
            ItemStack stack = getContainer().getItem(slot);
            if (stack.getItem() instanceof IPopulationItem popItem) {
                popItem.addApprenticeExp(stack, targetCareer, amount);
            }
        }
    }

    // ==================== 职业要求 ====================

    /**
     * 本机器要求的工作职业名称。
     *
     * <p>所有机器都必须声明其所需职业。返回 {@link com.gooodwei.civilizationevolution.api.career.CareerNames#UNEMPLOYED}
     * 表示接受任何常规职业的人口（不含 nitwit）。
     *
     * <p>此方法在接口层为抽象方法，强制所有实现类显式声明职业要求。
     * 每个抽象父类必须保持为 abstract，每个具体机器类必须覆写。
     *
     * @return 职业名称常量（如 {@code CareerNames.FARMER}、{@code CareerNames.BUTCHER}）
     */
    String getWorkerCareer();

    // ==================== 辅助计算 ====================

    /**
     * 计算工人列表的总工作效率。
     * <p>供 {@code executeWorkCycle} 中计算 {@code efficiency = foodFactor × totalWorkEfficiency} 使用。
     *
     * @param workers 可用工人列表（通常来自 {@code getAvailableWorkers()}）
     * @return 总工作效率（Σ 每个工人的 {@link PopulationNBT#getWorkEfficiency}）
     */
    default double calculateTotalWorkEfficiency(List<ItemStack> workers) {
        double total = 0;
        for (ItemStack worker : workers) {
            total += PopulationNBT.getWorkEfficiency(worker);
        }
        return total;
    }

    // ==================== 物品过滤工具 ====================

    /**
     * 使用自定义条件过滤物品列表。
     * <p>典型用法：筛选可放入机器某槽位的物品、筛选特定标签的物品等。
     *
     * <pre>{@code
     * List<ItemStack> foods = filterAvailable(drops, stack -> stack.getItem() instanceof FoodItem);
     * }</pre>
     *
     * @param stacks    待过滤的物品列表
     * @param predicate 过滤条件（返回 true 保留，false 丢弃）
     * @return 过滤后的物品列表（可能为空）
     */
    default List<ItemStack> filterAvailable(List<ItemStack> stacks, Predicate<ItemStack> predicate) {
        return stacks.stream()
                .filter(predicate)
                .collect(java.util.stream.Collectors.toList());
    }

    // ==================== 工人筛选 ====================

    /**
     * 从人口槽位中筛选适合工作的人口物品。
     *
     * <p>条件：PopulationItem、未死亡、年龄在 [{@link PopulationConfig#ADULT_AGE},
     * {@link PopulationConfig#RETIREMENT_AGE}] 区间内。
     * 若 {@link #getWorkerCareer()} 返回非空非空字符串，则追加职业匹配条件
     *（通过 {@link Career#isKindOf} 沿父链追溯）。
     *
     * @return 符合工作条件的人口物品列表（可能为空）
     */
    default List<ItemStack> getAvailableWorkers() {
        Container c = getContainer();
        List<ItemStack> all = new ArrayList<>();
        for (int slot : populationSlots()) {
            ItemStack stack = c.getItem(slot);
            if (!stack.isEmpty()) {
                all.add(stack);
            }
        }
        List<ItemStack> eligible = filterAvailable(all, stack ->
                stack.getItem() instanceof PopulationItem
                        && !PopulationNBT.isDead(stack)
                        && PopulationNBT.getAge(stack) >= PopulationConfig.getAdultAge()
                        && PopulationNBT.getAge(stack) <= PopulationConfig.getRetirementAge());

        String career = getWorkerCareer();
        if (career != null && !career.isEmpty()) {
            eligible = filterAvailable(eligible, stack ->
                    Career.isKindOf(PopulationNBT.getCareer(stack), career));
        }
        return eligible;
    }

    // ==================== 工作效率计算 ====================

    /**
     * 计算本周期工作效率：食物因子 × Σ人口工作效率。
     *
     * <p>封装了"消耗食物 → 获取食物因子 → 计算总效率"的标准流程，
     * 供 Ranch/HG/Farm/Harvester 等机器的 {@code executeWorkCycle} 使用。
     *
     * @param foodPerPopulation 每个人口槽位每次工作消耗的食物量
     * @return 工作效率（0.0 ~ N）
     */
    default float calculateWorkEfficiency(int foodPerPopulation) {
        List<ItemStack> workers = getAvailableWorkers();
        float foodFactor = consumeFoodWithFallback(foodPerPopulation, Math::sqrt, workers.size());
        return foodFactor * (float) calculateTotalWorkEfficiency(workers);
    }

    // ==================== 物品输出路由 ====================

    /**
     * 将掉落物尝试放入输出槽，先合并已有同类堆叠，再找空槽。
     * 输出空间不足时，剩余部分以掉落物形式弹出到方块上方。
     *
     * @param level 当前世界
     * @param pos   方块坐标（掉落物弹出到其上方）
     * @param drops 待输出的物品列表
     */
    default void outputOrDrop(Level level, BlockPos pos, List<ItemStack> drops) {
        Container c = getContainer();
        for (ItemStack drop : drops) {
            ItemStack remaining = drop.copy();
            // 第一步：合并到已有同类物品的输出槽
            for (int i = 0; i < c.getContainerSize() && !remaining.isEmpty(); i++) {
                if (!isOutputSlot(i)) continue;
                ItemStack slotStack = c.getItem(i);
                if (ItemStack.isSameItemSameComponents(slotStack, remaining)) {
                    int space = slotStack.getMaxStackSize() - slotStack.getCount();
                    int toMove = Math.min(space, remaining.getCount());
                    if (toMove > 0) {
                        slotStack.grow(toMove);
                        remaining.shrink(toMove);
                    }
                }
            }
            // 第二步：放入空输出槽
            for (int i = 0; i < c.getContainerSize() && !remaining.isEmpty(); i++) {
                if (!isOutputSlot(i)) continue;
                if (c.getItem(i).isEmpty()) {
                    int toMove = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                    c.setItem(i, remaining.copyWithCount(toMove));
                    remaining.shrink(toMove);
                }
            }
            // 第三步：还有剩余则掉落
            if (!remaining.isEmpty()) {
                Block.popResource(level, pos.above(), remaining);
            }
        }
    }
}

package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.career.CareerNames;
import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import com.gooodwei.civilizationevolution.server.config.CivilizationMachineConfig;
import com.gooodwei.civilizationevolution.server.population.Population;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 营地类机器的抽象父类 —— 提供人口繁殖的通用实现。
 *
 * <p>营地通过一对异性成年人口进行繁殖。工作周期触发时：
 * <ol>
 *   <li>所有人口年龄增长 + 健康度波动</li>
 *   <li>检查工作条件（父母存活、食物充足、性别/年龄匹配）</li>
 *   <li>消耗食物，计算食物因子</li>
 *   <li>生成子嗣（数量 = 父母工作效率乘积 × 食物因子）</li>
 * </ol>
 *
 * <p>子类只需提供配置 key、食物消耗量和 Tier 等级等差异化参数。
 * 如需定制繁殖公式或年龄范围，可覆写对应的 {@code getXxx()} 方法。
 *
 * <p>继承层次：
 * <pre>
 * BaseContainerBlockEntity
 *  └── AbstractMachineBlockEntity
 *       └── AbstractCampBlockEntity (本类)
 *            └── PrimitiveCampBlockEntity (Tier 0 原始营地)
 *            └── SmallCampBlockEntity (Tier 1+ 小型营地，附属模组)
 * </pre>
 *
 * @see AbstractMachineBlockEntity
 */
public abstract class AbstractCampBlockEntity extends AbstractMachineBlockEntity {

    // ==================== 构造器 ====================

    protected AbstractCampBlockEntity(BlockEntityType<?> type,
                                      BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
    }

    // ==================== Tier 特定抽象方法 ====================

    /** 配置文件中此机器的 section key（如 "camp"、"small_camp"） */
    protected abstract String getMachineConfigKey();

    /** 营地工作要求的职业名称（每个具体营地类必须覆写） */
    @Override
    public abstract String getWorkerCareer();

    /** 每个人口每次工作消耗的食物份数，优先从配置读取 */
    @Override
    public int getFoodPerPopulation() {
        return CivilizationMachineConfig.getFoodPerPopulation(getMachineConfigKey(), 8);
    }

    // ==================== 可覆写方法（有默认值） ====================

    /** 父代槽位 A 的索引 */
    protected int getParentSlotA() { return 4; }

    /** 父代槽位 B 的索引 */
    protected int getParentSlotB() { return 5; }

    /**
     * 营地效率 = 食物因子（覆写默认的 食物因子 × 人口效率 公式）。
     *
     * <p>营地采用乘法繁殖模型：子嗣数 = 父A效率 × 父B效率 × 食物因子。
     * 人口效率由两亲本各自的工作效率相乘来体现，不在此方法中计算。
     * eligibleCount = -1 表示所有已填充槽位的父母均计入食物因子。
     */
    @Override
    public float calculateEfficiency() {
        float result = consumeFoodWithFallback(getFoodPerPopulation(), -1);
        recordEfficiency(result);
        return result;
    }

    /** 父代最低生育年龄，优先从配置读取 */
    protected int getMinParentAge() {
        return CivilizationMachineConfig.getMinParentAge(getMachineConfigKey(), 18);
    }

    /** 父代最高生育年龄，优先从配置读取 */
    protected int getMaxParentAge() {
        return CivilizationMachineConfig.getMaxParentAge(getMachineConfigKey(), 50);
    }

    /** 健康度波动下限，优先从配置读取（营地波动幅度较大） */
    @Override
    protected int getHealthFluctuateMin() {
        return CivilizationMachineConfig.getHealthFluctuateMin(getMachineConfigKey(), -10);
    }

    /** 健康度波动上限，优先从配置读取（营地波动幅度较大） */
    @Override
    protected int getHealthFluctuateMax() {
        return CivilizationMachineConfig.getHealthFluctuateMax(getMachineConfigKey(), 5);
    }

    // ==================== IPopulationMachine 实现 ====================

    @Override
    public int getWorkTotalTime() {
        return CivilizationMachineConfig.getWorkTotalTime(getMachineConfigKey());
    }

    @Override
    public int getAgeIncrement() {
        return CivilizationMachineConfig.getAgeIncrement(getMachineConfigKey());
    }

    @Override
    public boolean isPopulationSlot(int slot) {
        return slot == getParentSlotA() || slot == getParentSlotB();
    }

    @Override
    public List<Integer> populationSlots() {
        return List.of(getParentSlotA(), getParentSlotB());
    }

    @Override
    public boolean isOutputSlot(int slot) {
        return slot > getParentSlotB();
    }

    @Override
    public boolean isFoodSlot(int slot) {
        return slot >= 0 && slot < getParentSlotA();
    }

    @Override
    public boolean canWork() {
        if (!super.canWork()) return false;
        // 必须双方存活
        if (isPopulationDead(getParentSlotA()) || isPopulationDead(getParentSlotB())) {
            return false;
        }
        // 营地特有：异性 + 年龄在生育范围内
        int ageA = getPopulationAge(getParentSlotA());
        int ageB = getPopulationAge(getParentSlotB());
        return getPopulationGender(getParentSlotA()) != getPopulationGender(getParentSlotB())
                && ageA >= getMinParentAge() && ageA <= getMaxParentAge()
                && ageB >= getMinParentAge() && ageB <= getMaxParentAge();
    }

    // ==================== 工作周期 ====================

    @Override
    public void executeWorkCycle(Level level) {
        this.ageAllPopulations(this.getAgeIncrement());
        this.fluctuateHealth(getHealthFluctuateMin(), getHealthFluctuateMax());

        if (this.canWork()) {
            float foodFactor = calculateEfficiency();
            this.doReproduction(level, foodFactor);
        }
        BlockPos pos = this.getBlockPos();
        BlockState state = this.getBlockState();
        setChanged(level, pos, state);
    }

    // ==================== 繁殖逻辑 ====================

    /**
     * 执行繁殖：读取父代人口物品，计算子嗣数量并产出。
     */
    private void doReproduction(Level level, float foodFactor) {
        ItemStack parentA = getItem(getParentSlotA());
        ItemStack parentB = getItem(getParentSlotB());

        int count = (int) Math.floor(
                PopulationNBT.getWorkEfficiency(parentA)
                        * PopulationNBT.getWorkEfficiency(parentB)
                        * foodFactor);

        for (int i = 0; i < count; i++) {
            int slot = findEmptyOutputSlot();
            if (slot == -1) {
                break;
            }
            setItem(slot, reproduction(level, parentA, parentB));
        }
    }

    /**
     * 基于父母生成一个子嗣 ItemStack。
     * 性别和寿命随机，其余属性取父母平均，心理状态固定 100。
     */
    private ItemStack reproduction(Level level, ItemStack parentA, ItemStack parentB) {
        ItemStack baby = new ItemStack(parentA.getItem());
        RandomSource rand = level.getRandom();

        PopulationNBT.setAge(baby, 0);
        PopulationNBT.setLifespan(baby, Population.generateLifespan(rand));
        PopulationNBT.setGender(baby, rand.nextBoolean());
        PopulationNBT.setCareer(baby, CareerNames.UNEMPLOYED);
        PopulationNBT.setHealth(baby, (
                PopulationNBT.getHealth(parentA) + PopulationNBT.getHealth(parentB)) / 2);
        PopulationNBT.setFood(baby, (
                PopulationNBT.getFood(parentA) + PopulationNBT.getFood(parentB)) / 2);
        PopulationNBT.setProficiency(baby, (
                PopulationNBT.getProficiency(parentA) + PopulationNBT.getProficiency(parentB)) / 2);
        PopulationNBT.setWorkEfficiency(baby, (
                PopulationNBT.getWorkEfficiency(parentA) + PopulationNBT.getWorkEfficiency(parentB)) / 2.0);
        PopulationNBT.setMentalState(baby, 100.0);

        return baby;
    }

    /**
     * 在输出槽中查找第一个空位。
     * @return 空槽位索引，未找到时返回 -1
     */
    private int findEmptyOutputSlot() {
        for (int i = 0; i < getContainerSize(); i++) {
            if (isOutputSlot(i) && getItem(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    // ==================== GUI 辅助 ====================

    /**
     * 获取当前工作进度比例（0.0 ~ 1.0），供 GUI 进度条使用。
     */
    public float getWorkProgressRatio() {
        int total = getWorkTotalTime();
        return total == 0 ? 0f : (float) workProgress / total;
    }
}

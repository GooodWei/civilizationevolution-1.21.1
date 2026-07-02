package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.range.RangeScanner;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

import java.util.Map;
import java.util.function.Predicate;

/**
 * 牧场类机器的抽象父类 —— 提供动物喂养逻辑的通用实现。
 *
 * <p>牧场消费食物和人手，在所在区块 Y±2 范围内喂养动物。
 * 喂养策略：
 * <ol>
 *   <li><b>优先幼年动物</b> —— 加速成长</li>
 *   <li><b>剩余额度喂成年动物</b> —— 不在生育冷却中的进入繁殖模式</li>
 *   <li>额度耗尽即刻跳出</li>
 * </ol>
 *
 * <p>无输出槽位，无武器槽位。范围内动物总数超过配置上限时取消当次工作。
 *
 * <p>子类只需提供配置 key、食物消耗量和 Tier 等级等差异化参数。
 *
 * <p>继承层次：
 * <pre>
 * BaseContainerBlockEntity
 *  └── AbstractMachineBlockEntity
 *       └── AbstractRangeMachineBlockEntity
 *            └── AbstractRanchBlockEntity (本类)
 *                 └── PrimitiveRanchBlockEntity (Tier 0 牧场)
 * </pre>
 *
 * @see AbstractRangeMachineBlockEntity
 */
public abstract class AbstractRanchBlockEntity extends AbstractRangeMachineBlockEntity {

    /** 客户端同步字段编号：最低保留数量（预留） */
    public static final int FIELD_MIN_KEEP_NUMBER = 0;

    // ==================== 构造器 ====================

    protected AbstractRanchBlockEntity(BlockEntityType<?> type,
                                       BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
    }

    // ==================== Tier 特定抽象方法 ====================

    /** 配置文件中此机器的 section key（如 "primitive_ranch"） */
    protected abstract String getMachineConfigKey();

    /** 每个人口每次工作消耗的食物份数，优先从配置读取 */
    protected int getFoodPerPopulation() {
        return PopulationMachineConfig.getFoodPerPopulation(getMachineConfigKey(), 2);
    }

    // ==================== 可覆写方法（有默认值） ====================

    /** 牧场工作要求的职业名称（每个具体牧场类必须覆写） */
    @Override
    public abstract String getWorkerCareer();

    /** 每次工作周期给学徒的经验量，优先从配置读取 */
    @Override
    protected int getApprenticeExpPerCycle() {
        return PopulationMachineConfig.getApprenticeExpPerCycle(getMachineConfigKey(), 1);
    }

    /** 健康度波动下限，优先从配置读取 */
    @Override
    protected int getHealthFluctuateMin() {
        return PopulationMachineConfig.getHealthFluctuateMin(getMachineConfigKey(), -5);
    }

    /** 健康度波动上限，优先从配置读取 */
    @Override
    protected int getHealthFluctuateMax() {
        return PopulationMachineConfig.getHealthFluctuateMax(getMachineConfigKey(), -1);
    }

    /** 效率 × 此倍数 = 每种动物的喂养数量，优先从配置读取 */
    protected int getFedPerTypeMultiplier() {
        return PopulationMachineConfig.getFedPerTypeMultiplier(getMachineConfigKey(), 3);
    }

    // ==================== ContainerData ====================

    @Override
    protected ContainerData createData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> workProgress;
                    case 1 -> getWorkTotalTime();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> workProgress = value;
                    default -> { }
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    // ==================== IPopulationMachine 实现 ====================

    @Override
    public int getWorkTotalTime() {
        return PopulationMachineConfig.getWorkTotalTime(getMachineConfigKey());
    }

    @Override
    public int getAgeIncrement() {
        return PopulationMachineConfig.getAgeIncrement(getMachineConfigKey());
    }

    @Override
    public boolean isPopulationSlot(int slot) {
        return slot >= 6 && slot <= 8;
    }

    @Override
    public List<Integer> populationSlots() {
        return List.of(6, 7, 8);
    }

    @Override
    public boolean isOutputSlot(int slot) {
        return false;
    }

    @Override
    public boolean isFoodSlot(int slot) {
        return slot >= 0 && slot <= 5;
    }

    // canWork 继承 AbstractRangeMachineBlockEntity（bind + 有可用工人）
    // 食物检查已移除 —— consumeFoodWithFallback 自动处理食物不足回退

    // ==================== 工作周期 ====================

    @Override
    public void executeWorkCycle(Level level) {
        this.ageAllPopulations(this.getAgeIncrement());
        this.fluctuateHealth(getHealthFluctuateMin(), getHealthFluctuateMax());

        // 冲突检测
        if (level instanceof ServerLevel serverLevel) {
            this.scanAndMarkConflicts(serverLevel);
        }

        BlockPos pos = this.getBlockPos();

        if (level instanceof ServerLevel serverLevel) {
            int maxAnimals = getMaxAnimalCount();
            if (maxAnimals > 0) {
                AABB range = getSelectionRange();
                List<Animal> allAnimals = RangeScanner.getAnimals(serverLevel, range);
                if (allAnimals.size() > maxAnimals) {
                    this.workProgress = 0;
                    setChanged(level, pos, level.getBlockState(pos));
                    return;
                }
            }

            if (this.canWork()) {
                // 学徒系统：调用 IPopulationItem.addApprenticeExp 处理晋级
                addApprenticeExpToPopulationSlots(getWorkerCareer(), getApprenticeExpPerCycle());

                AABB range = getSelectionRange();
                var grouped = RangeScanner.getEntitiesGroupedByKey(
                        serverLevel, range, Animal.class,
                        canBeFed(),
                        Animal::getClass);

                if (!grouped.isEmpty()) {
                    float foodFactor = consumeFoodWithFallback(
                            getFoodPerPopulation(),
                            total -> Math.sqrt(total),
                            getAvailableWorkers().size());
                    double totalWorkEfficiency = calculateTotalWorkEfficiency(this.getAvailableWorkers());
                    float efficiency = foodFactor * (float) totalWorkEfficiency;
                    feedAnimals(grouped, serverLevel, efficiency);
                }
            }
        }

        this.workProgress = 0;
        setChanged(level, pos, level.getBlockState(pos));
    }

    // ==================== 喂养逻辑 ====================

    /**
     * 返回一个过滤器，筛选<b>可喂养</b>的动物。
     * <p>同时满足以下任一条件即为可喂养：
     * <ul>
     *   <li>幼年动物 —— 喂食可加速成长</li>
     *   <li>成年动物不在生育冷却时间内 —— 喂食可进入繁殖模式</li>
     * </ul>
     *
     * @return 可传入 {@link RangeScanner#getEntitiesGroupedByKey} 的 Predicate
     */
    public static Predicate<Animal> canBeFed() {
        return animal -> animal.isBaby() || animal.getAge() <= 0;
    }

    /**
     * 喂养分组后的动物。
     *
     * <p>喂养策略：
     * <ol>
     *   <li><b>优先幼年动物</b> —— 加速成长，剩余喂养额度用完则跳过</li>
     *   <li><b>剩余额度喂成年动物</b> —— 不在生育冷却中的进入繁殖模式</li>
     *   <li>额度耗尽即刻跳出，不浪费遍历</li>
     * </ol>
     *
     * @param grouped    按类型分组的可喂养动物
     * @param level      服务端世界
     * @param efficiency 总工作效率（= 食物因子 × Σ人口工作效率）
     */
    private void feedAnimals(
            Map<? extends Class<? extends Animal>, List<Animal>> grouped,
            ServerLevel level,
            float efficiency) {
        int fedPerType = Math.max(1, (int) (efficiency * getFedPerTypeMultiplier()));

        for (List<Animal> animals : grouped.values()) {
            int remaining = fedPerType;

            // 第一遍：优先喂养幼年动物
            for (Animal animal : animals) {
                if (!animal.isBaby()) continue;
                if (remaining <= 0) break;
                animal.ageUp(Math.max(1, (int) ((-animal.getAge()) * 0.3F)));
                remaining--;
            }

            // 额度耗尽，该类型跳过成年动物
            if (remaining <= 0) continue;

            // 第二遍：喂养成年动物（不在生育冷却中的）
            for (Animal animal : animals) {
                if (animal.isBaby()) continue;
                if (animal.getAge() != 0) continue;
                if (remaining <= 0) break;
                animal.setInLove(null);
                remaining--;
            }
        }
    }

    /**
     * 获取范围内最大动物数量限制（0 = 不限制）。
     * 默认从配置文件读取，子类可覆写。
     */
    protected int getMaxAnimalCount() {
        return PopulationMachineConfig.getMaxAnimalCount(getMachineConfigKey());
    }

    // ==================== IClientUpdateReceiver ====================

    @Override
    public void onClientUpdate(int fieldId, CompoundTag data) {
        // 预留：后续可在此处理客户端设置
    }
}

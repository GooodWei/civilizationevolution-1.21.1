package com.gooodwei.civilizationevolution.server.blockentity.fieldmachine;

import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractRangeMachineBlockEntity;

import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import com.gooodwei.civilizationevolution.api.range.RangeScanner;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.item.PopulationItem;
import com.gooodwei.civilizationevolution.server.menu.PrimitiveRanchMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.gooodwei.civilizationevolution.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * 原始牧场方块实体 —— 消费食物和人手，在所在区块 Y±2 范围内喂养动物。
 *
 * <p>槽位布局（共 9 个）：
 * <ul>
 *   <li>槽位 0-5：食物输入槽（2×3）</li>
 *   <li>槽位 6-8：人口输入槽（牧人）</li>
 * </ul>
 *
 * <p>无输出槽位，无武器槽位。
 */
public class PrimitiveRanchBlockEntity extends AbstractRangeMachineBlockEntity {
    public static final int SIZE = 9;
    /** 客户端同步字段编号：最低保留数量（预留） */
    public static final int FIELD_MIN_KEEP_NUMBER = 0;

    public PrimitiveRanchBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.PRIMITIVE_RANCH.get(), pos, blockState, SIZE);
    }

    // ==================== 抽象方法实现 ====================

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
                    default -> {
                    }
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    protected TagKey<Block> getConflictTag() {
        return ModTags.PRIMITIVE_RANCH_CONFLICTS;
    }

    // ==================== serverTick ====================

    /**
     * 服务端每 tick 调用。委托父类的通用 tick 逻辑。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   PrimitiveRanchBlockEntity be) {
        AbstractRangeMachineBlockEntity.serverTick(level, pos, state, be);
    }

    // ==================== IPopulationMachine 实现 ====================

    @Override
    public int getWorkTotalTime() {
        return PopulationMachineConfig.getWorkTotalTime(PopulationMachineConfig.PRIMITIVE_RANCH);
    }

    @Override
    public int getAgeIncrement() {
        return PopulationMachineConfig.getAgeIncrement(PopulationMachineConfig.PRIMITIVE_RANCH);
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

    @Override
    public boolean canWork() {
        return super.canWork() && hasEnoughFood(2);
    }

    // ==================== 工作周期 ====================

    /**
     * 执行一次喂养工作周期。
     */
    @Override
    public void executeWorkCycle(Level level) {
        this.ageAllPopulations(this.getAgeIncrement());
        this.fluctuateHealth(-5, -1);

        // 冲突检测
        if (level instanceof ServerLevel serverLevel) {
            this.scanAndMarkConflicts(serverLevel);
        }

        BlockPos pos = this.getBlockPos();

        // 检查范围内动物总数是否超限
        if (level instanceof ServerLevel serverLevel) {
            int maxAnimals = PopulationMachineConfig.getMaxAnimalCount(
                    PopulationMachineConfig.PRIMITIVE_RANCH);
            if (maxAnimals > 0) {
                AABB range = getSelectionRange();
                List<Animal> allAnimals = RangeScanner.getAnimals(serverLevel, range);
                if (allAnimals.size() > maxAnimals) {
                    this.workProgress = 0;
                    setChanged(level, pos, level.getBlockState(pos));
                    return;
                }
            }

            // 只有在 canWork 通过后才执行喂养
            if (this.canWork()) {
                AABB range = getSelectionRange();
                var grouped = RangeScanner.getEntitiesGroupedByKey(
                        serverLevel, range, Animal.class,
                        canBeFed(),
                        Animal::getClass);

                if (!grouped.isEmpty()) {
                    float foodFactor = consumeFoodForRanch();
                    double totalWorkEfficiency = 0;
                    for (ItemStack worker : this.getAvailableWorkers()) {
                        totalWorkEfficiency += PopulationNBT.getWorkEfficiency(worker);
                    }
                    float efficiency = foodFactor * (float) totalWorkEfficiency;
                    feedAnimals(grouped, serverLevel, efficiency);
                }
            }
        }

        this.workProgress = 0;
        setChanged(level, pos, level.getBlockState(pos));
    }

    // ==================== 食物消耗 ====================

    /**
     * 原始牧场专用的食物消耗逻辑。
     *
     * <p>规则：
     * <ul>
     *   <li>每个已放入人口物品的槽位消耗 2 个食物（无论是否符合工作要求）</li>
     *   <li>只有符合工作要求的人口槽位消耗的食物才计入食物因子计算</li>
     * </ul>
     *
     * @return 食物因子（仅由符合要求的人口消耗的食物决定）
     */
    private float consumeFoodForRanch() {
        int totalPopSlots = (int) populationSlots().stream()
                .filter(slot -> !getItem(slot).isEmpty()).count();
        int eligibleCount = getAvailableWorkers().size();

        int totalNeeded = totalPopSlots * 2;
        int eligibleNeeded = eligibleCount * 2;

        int remaining = totalNeeded;
        int eligibleRemaining = eligibleNeeded;
        float totalNutrition = 0;
        float totalSaturation = 0;

        for (int i = 0; i < getContainerSize() && remaining > 0; i++) {
            if (!isFoodSlot(i)) continue;
            ItemStack stack = getItem(i);
            if (stack.isEmpty() || !stack.has(DataComponents.FOOD)) continue;

            FoodProperties food = stack.getFoodProperties(null);
            float nutrition = food != null ? food.nutrition() : 0;
            float saturation = food != null ? nutrition * food.saturation() * 2 : 0;

            int toRemove = Math.min(stack.getCount(), remaining);
            stack.shrink(toRemove);
            remaining -= toRemove;

            // 只有符合要求人口的份额才计入食物因子
            int eligiblePortion = Math.min(toRemove, eligibleRemaining);
            if (eligiblePortion > 0) {
                totalNutrition += nutrition * eligiblePortion;
                totalSaturation += saturation * eligiblePortion;
                eligibleRemaining -= eligiblePortion;
            }
        }

        double factor = Math.sqrt(totalNutrition + totalSaturation);
        return (float) (Math.round(factor * 1000.0) / 1000.0);
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
        int fedPerType = Math.max(1, (int) (efficiency * 3));

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

    // ==================== GUI / 网络 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.civilizationevolution.primitive_ranch");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.primitive_ranch");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PrimitiveRanchMenu(containerId, inventory, this, this.data);
    }

    /**
     * 接收客户端发来的字段更新（预留）。
     */
    @Override
    public void onClientUpdate(int fieldId, CompoundTag data) {
        // 预留：后续可在此处理客户端设置
    }

}

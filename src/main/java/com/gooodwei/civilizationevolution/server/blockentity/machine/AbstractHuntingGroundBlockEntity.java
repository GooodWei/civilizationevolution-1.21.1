package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.career.Career;
import com.gooodwei.civilizationevolution.api.range.RangeScanner;
import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 狩猎场类机器的抽象父类 —— 提供狩猎逻辑的通用实现。
 *
 * <p>狩猎场消费食物和人手，在所在区块 Y±2 范围内狩猎成年动物。
 * 通过战利品表生成掉落物，支持武器抢夺加成和效率倍率。
 *
 * <p>子类只需提供配置 key、食物消耗量和 Tier 等级等差异化参数。
 * 如需定制工作范围，可覆写 {@link #getHorizontalChunkRadius()} 等方法。
 *
 * <p>继承层次：
 * <pre>
 * BaseContainerBlockEntity
 *  └── AbstractMachineBlockEntity
 *       └── AbstractRangeMachineBlockEntity
 *            └── AbstractHuntingGroundBlockEntity (本类)
 *                 └── PrimitiveHuntingGroundBlockEntity (Tier 0 原始狩猎场)
 * </pre>
 *
 * @see AbstractRangeMachineBlockEntity
 */
public abstract class AbstractHuntingGroundBlockEntity extends AbstractRangeMachineBlockEntity {

    /** 客户端同步字段编号：最低保留数量 */
    public static final int FIELD_MIN_KEEP_NUMBER = 0;

    /** 每种动物最低保留数量（低于此数不猎杀） */
    protected int minKeepNumber;

    // ==================== 构造器 ====================

    protected AbstractHuntingGroundBlockEntity(BlockEntityType<?> type,
                                               BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
    }

    // ==================== Tier 特定抽象方法 ====================

    /** 配置文件中此机器的 section key（如 "primitive_hunting_ground"） */
    protected abstract String getMachineConfigKey();

    /** 每个人口每次工作消耗的食物份数，优先从配置读取 */
    protected int getFoodPerPopulation() {
        return PopulationMachineConfig.getFoodPerPopulation(getMachineConfigKey(), 32);
    }

    // ==================== 可覆写方法（有默认值） ====================

    /** 狩猎场工作要求的职业名称 */
    @Override
    protected String getWorkerCareer() { return "butcher"; }

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

    /** 无武器时效率百分比（50 = 0.5），优先从配置读取 */
    protected float getEfficiencyNoWeapon() {
        return PopulationMachineConfig.getEfficiencyNoWeapon(getMachineConfigKey(), 50) / 100.0f;
    }

    /** 武器槽位索引 */
    protected int getWeaponSlot() { return 9; }

    // ==================== ContainerData ====================

    @Override
    protected ContainerData createData() {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> workProgress;
                    case 1 -> getWorkTotalTime();
                    case 2 -> minKeepNumber;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> workProgress = value;
                    case 2 -> minKeepNumber = value;
                    default -> { }
                }
            }

            @Override
            public int getCount() {
                return 3;
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
        return slot >= 10 && slot <= 18;
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

        // 任务完成前做最后一次冲突检测
        if (level instanceof ServerLevel serverLevel) {
            this.scanAndMarkConflicts(serverLevel);
        }

        BlockPos pos = this.getBlockPos();
        if (this.canWork() && level instanceof ServerLevel serverLevel) {
            // 学徒系统：调用 IPopulationItem.addApprenticeExp 处理晋级
            addApprenticeExpToPopulationSlots(getWorkerCareer(), getApprenticeExpPerCycle());

            AABB range = getSelectionRange();
            var grouped = RangeScanner.getEntitiesGroupedByKey(
                    serverLevel, range, Animal.class,
                    animal -> !animal.isBaby(),
                    Animal::getClass);

            if (!grouped.isEmpty()) {
                ItemStack weapon = this.getItem(getWeaponSlot());
                float foodFactor = consumeFoodWithFallback(
                        getFoodPerPopulation(),
                        total -> Math.sqrt(total),
                        getAvailableWorkers().size());
                double totalWorkEfficiency = calculateTotalWorkEfficiency(this.getAvailableWorkers());
                float efficiency = foodFactor * (float) totalWorkEfficiency;
                if (weapon.isEmpty()) {
                    efficiency *= getEfficiencyNoWeapon();
                }
                int durabilityPerKill = this.getAvailableWorkers().size();
                List<ItemStack> drops = hunt(grouped, this.minKeepNumber, serverLevel,
                        weapon, efficiency, durabilityPerKill);
                outputOrDrop(serverLevel, pos, this, drops);
            }
        }
        this.workProgress = 0;
        BlockState state = this.getBlockState();
        setChanged(level, pos, state);
    }

    // ==================== 狩猎逻辑（静态） ====================

    /**
     * 执行狩猎：杀死范围内超出最低保留数量的动物，并产出战利品。
     *
     * <p>狩猎数量由 {@code efficiency} 的整数部分决定（如 efficiency=5.67 → 杀 5 只），
     * 每只动物的战利品数量再乘以 {@code efficiency}。
     *
     * @param map              按类型分组的成年动物
     * @param minKeepNumber    每种动物最低保留数量
     * @param level            服务端世界
     * @param weapon           猎人使用的武器（影响战利品抢夺附魔）
     * @param efficiency       工作效率（整数部分=猎杀数量，整体=战利品倍率）
     * @param durabilityPerKill 每杀一只动物消耗的武器耐久
     * @return 战利品列表（已应用效率加成）
     */
    public static List<ItemStack> hunt(ConcurrentHashMap<? extends Class<? extends Animal>, List<Animal>> map,
                                       int minKeepNumber, ServerLevel level,
                                       ItemStack weapon, float efficiency,
                                       int durabilityPerKill) {
        List<ItemStack> allDrops = new ArrayList<>();
        int maxToHunt = Math.max(1, (int) efficiency);

        int hunted = 0;
        outer:
        for (List<Animal> animals : map.values()) {
            if (animals.size() <= minKeepNumber) continue;
            List<Animal> toHunt = new ArrayList<>(animals.subList(minKeepNumber, animals.size()));
            for (Animal animal : toHunt) {
                if (hunted >= maxToHunt) break outer;

                var lootTableId = animal.getLootTable();
                LootTable lootTable = level.getServer().reloadableRegistries()
                        .getLootTable(lootTableId);
                LootParams lootParams = new LootParams.Builder(level)
                        .withParameter(LootContextParams.THIS_ENTITY, animal)
                        .withParameter(LootContextParams.ORIGIN, animal.position())
                        .withParameter(LootContextParams.DAMAGE_SOURCE,
                                level.damageSources().generic())
                        .withParameter(LootContextParams.TOOL, weapon)
                        .create(LootContextParamSets.ENTITY);
                List<ItemStack> rawDrops = lootTable.getRandomItems(lootParams);

                for (ItemStack drop : rawDrops) {
                    int newCount = Math.round(drop.getCount() * efficiency);
                    if (newCount <= 0) continue;
                    int maxStack = drop.getMaxStackSize();
                    int remaining = newCount;
                    while (remaining > 0) {
                        int stackSize = Math.min(remaining, maxStack);
                        allDrops.add(drop.copyWithCount(stackSize));
                        remaining -= stackSize;
                    }
                }

                animal.discard();
                hunted++;
            }
        }

        int totalDamage = hunted * durabilityPerKill;
        if (totalDamage > 0 && weapon.isDamageableItem()) {
            weapon.hurtAndBreak(totalDamage, level, null,
                    item -> weapon.shrink(1));
        }
        return allDrops;
    }

    /**
     * 将战利品尝试放入输出槽，先合并已有同类堆叠，再找空槽。
     * 输出空间不足时，剩余部分以掉落物形式弹出到方块上方。
     */
    protected static void outputOrDrop(ServerLevel level, BlockPos pos,
                                        AbstractHuntingGroundBlockEntity be, List<ItemStack> drops) {
        for (ItemStack drop : drops) {
            ItemStack remaining = drop.copy();
            // 第一步：合并到已有同类物品的输出槽
            for (int i = 0; i < be.getContainerSize() && !remaining.isEmpty(); i++) {
                if (!be.isOutputSlot(i)) continue;
                ItemStack slotStack = be.getItem(i);
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
            for (int i = 0; i < be.getContainerSize() && !remaining.isEmpty(); i++) {
                if (!be.isOutputSlot(i)) continue;
                if (be.getItem(i).isEmpty()) {
                    int toMove = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                    be.setItem(i, remaining.copyWithCount(toMove));
                    remaining.shrink(toMove);
                }
            }
            // 第三步：还有剩余则掉落
            if (!remaining.isEmpty()) {
                Block.popResource(level, pos.above(), remaining);
            }
        }
    }

    // ==================== IClientUpdateReceiver ====================

    @Override
    public void onClientUpdate(int fieldId, CompoundTag data) {
        switch (fieldId) {
            case FIELD_MIN_KEEP_NUMBER -> {
                int value = data.getInt("v");
                if (value < 0) value = 0;
                this.minKeepNumber = value;
                setChanged();
            }
            default -> { }
        }
    }

    // ==================== NBT 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("MinKeepNumber", minKeepNumber);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        minKeepNumber = tag.getInt("MinKeepNumber");
    }
}

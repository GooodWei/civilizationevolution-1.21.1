package com.gooodwei.civilizationevolution.server.blockentity.fieldmachine;

import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractRangeMachineBlockEntity;

import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import com.gooodwei.civilizationevolution.api.range.RangeScanner;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.item.PopulationItem;
import com.gooodwei.civilizationevolution.server.menu.HuntingGroundMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.gooodwei.civilizationevolution.tags.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * 狩猎场方块实体 —— 消费食物和人手，在所在区块 Y±2 范围内狩猎成年动物。
 *
 * <p>槽位布局（共 19 个）：
 * <ul>
 *   <li>槽位 0-5：食物输入槽</li>
 *   <li>槽位 6-8：人口输入槽（猎人）</li>
 *   <li>槽位 9：武器槽</li>
 *   <li>槽位 10-18：输出槽（战利品）</li>
 * </ul>
 */
public class HuntingGroundBlockEntity extends AbstractRangeMachineBlockEntity {
    public static final int SIZE = 19;
    /** 客户端同步字段编号：最低保留数量 */
    public static final int FIELD_MIN_KEEP_NUMBER = 0;
    /** 每种动物最低保留数量（低于此数不猎杀） */
    private int minKeepNumber;

    public HuntingGroundBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.HUNT_GROUND.get(), pos, blockState, SIZE);
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
                    case 2 -> minKeepNumber;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> workProgress = value;
                    case 2 -> minKeepNumber = value;
                    default -> {
                    }
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    @Override
    protected TagKey<Block> getConflictTag() {
        return ModTags.HUNTING_GROUND_CONFLICTS;
    }

    // ==================== serverTick ====================

    /**
     * 服务端每 tick 调用。委托父类的通用 tick 逻辑。
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state,
                                   HuntingGroundBlockEntity be) {
        AbstractRangeMachineBlockEntity.serverTick(level, pos, state, be);
    }

    // ==================== IPopulationMachine 实现 ====================

    @Override
    public int getWorkTotalTime() {
        return PopulationMachineConfig.HUNT_GROUND_TOTAL_TIME;
    }

    @Override
    public int getAgeIncrement() {
        return PopulationMachineConfig.HUNT_GROUND_AGE_INCREMENT;
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

    @Override
    public boolean canWork() {
        return super.canWork() && hasEnoughFood(32);
    }

    // ==================== 工作周期 ====================

    /**
     * 执行一次狩猎工作周期。
     */
    @Override
    public void executeWorkCycle(Level level) {
        this.ageAllPopulations(this.getAgeIncrement());
        this.fluctuateHealth(-5, -1);

        // 任务完成前做最后一次冲突检测
        if (level instanceof ServerLevel serverLevel) {
            this.scanAndMarkConflicts(serverLevel);
        }

        BlockPos pos = this.getBlockPos();
        if (this.canWork() && level instanceof ServerLevel serverLevel) {
            AABB range = getSelectionRange();
            var grouped = RangeScanner.getEntitiesGroupedByKey(
                    serverLevel, range, Animal.class,
                    animal -> !animal.isBaby(),
                    Animal::getClass);

            if (!grouped.isEmpty()) {
                ItemStack weapon = this.getItem(9);
                float foodFactor = consumeFoodForHunt();
                double totalWorkEfficiency = 0;
                for (ItemStack worker : this.getAvailableWorkers()) {
                    totalWorkEfficiency += PopulationNBT.getWorkEfficiency(worker);
                }
                float efficiency = foodFactor * (float) totalWorkEfficiency;
                // 无武器时效率减半
                if (weapon.isEmpty()) {
                    efficiency *= 0.5F;
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

    // ==================== 食物消耗 ====================

    /**
     * 狩猎场专用的食物消耗逻辑。
     *
     * <p>规则：
     * <ul>
     *   <li>每个人口槽位消耗 32 个食物（无论是否符合工作要求）</li>
     *   <li>只有符合工作要求的人口槽位消耗的食物才计入食物因子计算</li>
     * </ul>
     *
     * @return 食物因子（仅由符合要求的人口消耗的食物决定）
     */
    private float consumeFoodForHunt() {
        int totalPopSlots = populationSlots().size();
        int eligibleCount = getAvailableWorkers().size();

        int totalNeeded = totalPopSlots * 32;
        int eligibleNeeded = eligibleCount * 32;

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

    // ==================== 狩猎逻辑 ====================

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
    public static List<ItemStack> hunt(Map<Class<? extends Animal>, List<Animal>> map,
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

                // 通过战利品表生成掉落物（含武器抢夺加成）
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

                // 应用效率加成，超出最大堆叠数时自动拆分
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

                // 静默移除实体（无死亡动画、无自然掉落）
                animal.discard();
                hunted++;
            }
        }

        // 武器耐久消耗（按实际猎杀数量计算）
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
    private static void outputOrDrop(ServerLevel level, BlockPos pos,
                                      HuntingGroundBlockEntity be, List<ItemStack> drops) {
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

    // ==================== GUI / 网络 ====================

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.civilizationevolution.hunting_ground");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.hunting_ground");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new HuntingGroundMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new HuntingGroundMenu(containerId, inventory, this, this.data);
    }

    /**
     * 接收客户端发来的字段更新（由 {@code UpdateMachineFieldPayload} 携带）。
     */
    @Override
    public void onClientUpdate(int fieldId, CompoundTag data) {
        switch (fieldId) {
            case FIELD_MIN_KEEP_NUMBER -> {
                int value = data.getInt("v");
                if (value < 0) value = 0;
                this.minKeepNumber = value;
                setChanged();
            }
            default -> {
            }
        }
    }

    // ==================== NBT ====================

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

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return checkSlotRestriction(slot, stack) && super.canPlaceItem(slot, stack);
    }
}

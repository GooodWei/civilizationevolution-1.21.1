package com.gooodwei.civilizationevolution.server.blockentity.multiblock;

import com.gooodwei.civilizationevolution.api.CivilizationAPI;
import com.gooodwei.civilizationevolution.api.IClientUpdateReceiver;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveDoctorCabinMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 医院多方块机器的抽象基类。
 *
 * <p>继承 {@link AbstractMultiBlockMachineBlockEntity}，实现医院特有的：
 * <ul>
 *   <li><b>医生槽位</b>（2 个）：接受牧师或无业人口，永久停留，每周期年龄+1、获得牧师职业经验</li>
 *   <li><b>病人原地处理</b>：病人在输入仓室中原地被处理（不进入控制器槽位），
 *       每周期年龄+1、按机器效率进行健康波动</li>
 *   <li><b>健康达标转移</b>：健康波动后健康度 ≥ 阈值的病人自动移入输出仓室</li>
 *   <li><b>食物消耗</b>：从食物仓室消耗 =（病人数 + 医生数）× 单位人口食物消耗</li>
 *   <li><b>机器效率</b>：医生平均工作效率 × 食物因子（按医生消耗食物营养值计算）</li>
 *   <li><b>健康阈值管理</b>（子 GUI 设定 0-100）</li>
 * </ul>
 *
 * <p>子类只需提供 Tier 具体的参数（配置 key、菜单等）。
 *
 * @see PrimitiveDoctorCabinMenu
 */
public abstract class AbstractHospitalBlockEntity extends AbstractMultiBlockMachineBlockEntity
        implements IClientUpdateReceiver {

    /** 默认健康阈值（0-100） */
    protected int healthThreshold = 40;
    /** 治疗槽位数量 */
    public static final int SIZE = 2;

    /** 用于客户端同步的 ContainerData，由 createData() 初始化 */
    protected final ContainerData data;

    // ==================== 构造器 ====================

    protected AbstractHospitalBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, SIZE);
        this.data = createData();
    }

    // ==================== 结构模式定义 ====================

    /**
     * 返回 {@code config/civilizationevolution/multi_blocks.json} 中对应的结构 key。
     */
    @Override
    public String getConfigKey() {
        return "primitive_doctor_cabin";
    }

    // ==================== serverTick ====================

    /**
     * 服务端每 tick 调用。
     * 子类的静态 {@code serverTick} 方法应委托到此方法。
     */
    protected static <T extends AbstractHospitalBlockEntity> void serverTick(
            Level level, BlockPos pos, BlockState state, T be) {
        // 1. 处理延迟验证倒计时
        be.tickRevalidation();

        // 2. 绑定后进度条推进
        if (be.isBound() && be.workProgress < be.getWorkTotalTime()) {
            be.workProgress++;
            setChanged(level, pos, state);
        }

        // 3. 结构成型 + 绑定时：持续将已达标病人移出（兜底即时响应）
        if (be.isStructureFormed() && be.isBound()) {
            be.routeItemsToOutputHatches();
        }

        // 4. 进度完成时执行工作周期
        if (be.workProgress >= be.getWorkTotalTime() && be.canWork()) {
            be.executeWorkCycle(level);
        }
    }

    // ==================== 工作周期 ====================

    /**
     * 执行一次工作周期：
     * <ol>
     *   <li>医生成长：年龄+1（寿命检查，达寿命标记死亡而非销毁）、获得牧师职业经验（无业者 8 点后转职）</li>
     *   <li>计算机器效率：医生平均工作效率 × 食物因子</li>
     *   <li>消耗食物：从食物仓室按总人口数消耗</li>
     *   <li>病人处理：年龄+1（寿命检查）、健康波动、达标者移入输出仓室</li>
     * </ol>
     */
    @Override
    public void executeWorkCycle(Level level) {
        // ===== 1. 医生年龄+1（使用 ageAllPopulations 保持与系统一致的寿命→标记死亡逻辑） =====
        ageAllPopulations(getAgeIncrement());

        // ===== 2. 医生学徒经验（委托 IPopulationItem.addApprenticeExp） =====
        addApprenticeExpToPopulationSlots("cleric", getApprenticeExpPerCycle());

        // ===== 3. 计算机器效率 =====
        List<ItemStack> doctors = getActiveDoctors();
        double avgEfficiency = 0.5; // 无医生时默认 0.5
        if (!doctors.isEmpty()) {
            avgEfficiency = doctors.stream()
                    .mapToDouble(PopulationNBT::getWorkEfficiency)
                    .average().orElse(0.5);
        }

        // ===== 4. 消耗食物并计算食物因子 =====
        int patientCount = countInputHatchPatients();
        int totalMouths = patientCount + doctors.size();
        float foodFactor = consumeFoodAndGetFactor(totalMouths, doctors.size());

        double machineEfficiency = avgEfficiency * foodFactor;

        // ===== 5. 处理病人 =====
        for (BlockPos hatchPos : getInputHatches()) {
            BlockEntity be = level.getBlockEntity(hatchPos);
            if (!(be instanceof com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractPopulationInputHatchBlockEntity hatch))
                continue;

            ItemStack stack = hatch.getItem(0);
            if (stack.isEmpty() || PopulationNBT.isDead(stack)) continue;

            // 年龄+1（先查寿命再老化，与 ageAllPopulations 逻辑一致）
            int currentAge = PopulationNBT.getAge(stack);
            int lifespan = PopulationNBT.getLifespan(stack);
            if (currentAge + 1 > lifespan) {
                PopulationNBT.markDead(stack);
            } else {
                CivilizationAPI.getPopulationManager().addAge(stack);
            }

            // 已死亡的病人不再进行健康波动
            if (PopulationNBT.isDead(stack)) {
                hatch.setChanged();
                continue;
            }

            // 健康波动（效率影响波动范围：高效率 → 更正向，低效率 → 更负向）
            int minDelta = (int) Math.round(-5 / Math.max(machineEfficiency, 0.2));
            int maxDelta = (int) Math.round(10 * machineEfficiency);
            int delta = level.random.nextIntBetweenInclusive(minDelta, maxDelta);
            int newHealth = Math.clamp(PopulationNBT.getHealth(stack) + delta, 0, 100);

            if (newHealth <= 0) {
                PopulationNBT.markDead(stack);
            } else {
                PopulationNBT.setHealth(stack, newHealth);
            }

            // 达标 → 转移至输出仓
            if (!PopulationNBT.isDead(stack) && PopulationNBT.getHealth(stack) >= healthThreshold) {
                transferToOutputHatch(stack, hatch);
            }

            hatch.setChanged();
        }

        // 重置进度
        this.workProgress = 0;
        setChanged();
    }

    // ==================== Item 路由 ====================

    /**
     * 将健康度超过阈值的病人从输入仓室移入输出仓室。
     * 兜底逻辑：每个 tick 调用，即时响应手动放入已达标的病人。
     */
    protected void routeItemsToOutputHatches() {
        if (level == null) return;

        for (BlockPos hatchPos : getInputHatches()) {
            BlockEntity be = level.getBlockEntity(hatchPos);
            if (!(be instanceof com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractPopulationInputHatchBlockEntity hatch))
                continue;

            ItemStack stack = hatch.getItem(0);
            if (!stack.isEmpty() && !PopulationNBT.isDead(stack)
                    && PopulationNBT.getHealth(stack) >= healthThreshold) {
                transferToOutputHatch(stack, hatch);
            }
        }
    }

    /**
     * 将一个病人从输入仓室转移到输出仓室。
     *
     * @param stack       病人人口物品
     * @param sourceHatch 来源输入仓室
     */
    private void transferToOutputHatch(ItemStack stack,
                                        com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractPopulationInputHatchBlockEntity sourceHatch) {
        for (BlockPos hatchPos : getOutputHatches()) {
            BlockEntity be = level.getBlockEntity(hatchPos);
            if (!(be instanceof com.gooodwei.civilizationevolution.server.blockentity.hatch.PrimitivePopulationOutputHatchBlockEntity outHatch))
                continue;

            ItemStack outStack = outHatch.getItem(0);
            if (outStack.isEmpty()) {
                outHatch.setItem(0, stack.copy());
                sourceHatch.setItem(0, ItemStack.EMPTY);
                sourceHatch.setChanged();
                outHatch.setChanged();
                return;
            }
        }
    }

    // ==================== 工作条件 ====================

    @Override
    public boolean canWork() {
        return isStructureFormed() && super.canWork() && !getActiveDoctors().isEmpty();
    }

    /**
     * 获取活跃医生列表（槽位 0、1 中存活且职业为牧师或无业的人口）。
     */
    protected List<ItemStack> getActiveDoctors() {
        List<ItemStack> doctors = new ArrayList<>();
        for (int slot : populationSlots()) {
            ItemStack stack = getItem(slot);
            if (stack.isEmpty() || PopulationNBT.isDead(stack)) continue;
            String career = PopulationNBT.getCareer(stack);
            if ("cleric".equals(career) || "unemployed".equals(career)) {
                doctors.add(stack);
            }
        }
        return doctors;
    }

    /**
     * 统计所有输入仓室中存活病人数量。
     */
    protected int countInputHatchPatients() {
        if (level == null) return 0;
        int count = 0;
        for (BlockPos hatchPos : getInputHatches()) {
            BlockEntity be = level.getBlockEntity(hatchPos);
            if (!(be instanceof com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractPopulationInputHatchBlockEntity hatch))
                continue;
            ItemStack stack = hatch.getItem(0);
            if (!stack.isEmpty() && !PopulationNBT.isDead(stack)) {
                count++;
            }
        }
        return count;
    }

    // ==================== 食物系统 ====================

    /**
     * 获取单位人口食物消耗量，优先从配置读取。
     */
    protected int getFoodPerPopulation() {
        return PopulationMachineConfig.getFoodPerPopulation(getConfigKey(), 1);
    }

    /**
     * 每次工作周期给学徒的经验量，优先从配置读取。
     */
    protected int getApprenticeExpPerCycle() {
        return PopulationMachineConfig.getApprenticeExpPerCycle(getConfigKey(), 1);
    }

    /**
     * 从食物仓室消耗食物并计算食物因子。
     *
     * <p>总消耗 = totalMouths × foodPerPopulation。
     * 食物因子 = sqrt(医生部分营养值 / 176.0)，仅按医生消耗的食物营养值计算。
     *
     * @param totalMouths 总人口数（医生 + 病人）
     * @param doctorCount 活跃医生数
     * @return 食物因子（0.0 ~ N），取小数点后三位
     */
    private float consumeFoodAndGetFactor(int totalMouths, int doctorCount) {
        int fp = getFoodPerPopulation();
        int totalNeeded = totalMouths * fp;
        int doctorNeeded = doctorCount * fp;

        if (totalNeeded <= 0 || getFoodHatches().isEmpty()) return 1.0f;

        float doctorNutrition = 0;
        int remaining = totalNeeded;

        for (BlockPos hatchPos : getFoodHatches()) {
            if (remaining <= 0) break;
            BlockEntity be = level.getBlockEntity(hatchPos);
            if (!(be instanceof com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractFoodInputHatchBlockEntity hatch))
                continue;

            for (int i = 0; i < hatch.getContainerSize() && remaining > 0; i++) {
                ItemStack stack = hatch.getItem(i);
                if (stack.isEmpty() || !stack.has(DataComponents.FOOD)) continue;

                FoodProperties food = stack.getFoodProperties(null);
                float nutrition = food != null ? food.nutrition() : 0;
                float saturation = food != null ? nutrition * food.saturation() * 2 : 0;
                float unitNutrition = nutrition + saturation;

                int toRemove = Math.min(stack.getCount(), remaining);
                stack.shrink(toRemove);
                remaining -= toRemove;

                // 医生部分按比例计入营养因子
                float docRatio = totalNeeded > 0 ? (float) doctorNeeded / totalNeeded : 0;
                doctorNutrition += unitNutrition * toRemove * docRatio;

                hatch.setChanged();
            }
        }

        double factor = Math.sqrt(doctorNutrition / 176.0);
        return (float) (Math.round(factor * 1000.0) / 1000.0);
    }

    // ==================== 槽位分类 ====================

    @Override
    public boolean isPopulationSlot(int slot) {
        return slot == 0 || slot == 1;
    }

    @Override
    public List<Integer> populationSlots() {
        return List.of(0, 1);
    }

    @Override
    public boolean isFoodSlot(int slot) {
        return false; // 食物来源于多方块食物仓室，非内部槽位
    }

    @Override
    public boolean isOutputSlot(int slot) {
        return false; // 医生槽位不是输出槽位
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
                    case 2 -> healthThreshold;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> workProgress = value;
                    case 2 -> healthThreshold = Math.clamp(value, 0, 100);
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    // ==================== 客户端字段更新 ====================

    public void onClientUpdate(int fieldId, CompoundTag data) {
        if (fieldId == 0) {
            // FIELD_HEALTH_THRESHOLD = 0
            healthThreshold = Math.clamp(data.getInt("value"), 0, 100);
            setChanged();
        }
    }

    // ==================== 菜单 ====================

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        if (!isStructureFormed()) return null;
        return new PrimitiveDoctorCabinMenu(containerId, inventory, this, this.data);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.primitive_doctor_cabin");
    }

    // ==================== NBT 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("HealthThreshold", healthThreshold);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        healthThreshold = tag.getInt("HealthThreshold");
        if (healthThreshold == 0 && tag.contains("HealthThreshold")) {
            // 已正确加载
        } else if (healthThreshold == 0) {
            healthThreshold = 40; // 默认值
        }
    }

    // ==================== 抽象方法（子类实现） ====================

    @Override
    public abstract Tier getTier();

    @Override
    public abstract int getWorkTotalTime();

    @Override
    public abstract int getAgeIncrement();
}

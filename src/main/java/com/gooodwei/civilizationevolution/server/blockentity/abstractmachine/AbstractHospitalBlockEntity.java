package com.gooodwei.civilizationevolution.server.blockentity.abstractmachine;

import com.gooodwei.civilizationevolution.api.IClientUpdateReceiver;
import com.gooodwei.civilizationevolution.api.career.Career;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.menu.PrimitiveDoctorCabinMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
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
 *   <li>人口健康度正向治疗（仅正向波动，不降健康度）</li>
 *   <li>从输入接口拉取人口 → 治疗槽位 → 推送到输出接口的 item 路由</li>
 *   <li>健康阈值管理（子 GUI 设定，治疗后超过阈值的人口移入输出接口）</li>
 *   <li>职业过滤（仅接受牧师和无业人口）</li>
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

    @Override
    protected String getStructurePattern() {
        return """
        {
          "controller": [0, 0, 0],
          "pattern": {
            "y0": "CX,XX",
            "y1": "XX,XX"
          },
          "key": {
            "C": {"block": "self"},
            "X": {"type": "multi_block_part"}
          }
        }
        """;
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

        // 3. 结构成型 + 绑定时：从输入接口拉取 → 推送到输出接口
        if (be.structureFormed && be.isBound()) {
            be.routeItemsFromInputHatches();
            be.routeItemsToOutputHatches();
        }

        // 4. 进度完成时执行工作周期
        if (be.workProgress >= be.getWorkTotalTime() && be.canWork()) {
            be.executeWorkCycle(level);
        }
    }

    // ==================== 工作周期 ====================

    @Override
    public void executeWorkCycle(Level level) {
        // 重新验证结构
        if (!validateStructure()) {
            this.workProgress = 0;
            return;
        }

        // 对治疗槽位中的人口进行正向健康度治疗
        for (int slot : populationSlots()) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                int currentHealth = PopulationNBT.getHealth(stack);
                int healAmount = 1 + level.random.nextInt(10);
                int newHealth = Math.min(currentHealth + healAmount, 100);
                PopulationNBT.setHealth(stack, newHealth);
            }
        }

        // 增加年龄
        ageAllPopulations(getAgeIncrement());

        // 重置进度
        this.workProgress = 0;
        setChanged();
    }

    // ==================== Item 路由 ====================

    /**
     * 从所有输入接口拉取人口物品，填充空的治疗槽位。
     */
    protected void routeItemsFromInputHatches() {
        if (level == null) return;

        for (BlockPos hatchPos : getInputHatches()) {
            if (bothSlotsFull()) break;

            BlockEntity be = level.getBlockEntity(hatchPos);
            if (be instanceof com.gooodwei.civilizationevolution.server.blockentity.hatch.PrimitivePopulationInputHatchBlockEntity hatch) {
                ItemStack stack = hatch.getItem(0);
                if (!stack.isEmpty() && isValidPatient(stack)) {
                    moveToEmptyTreatmentSlot(stack, hatch);
                }
            }
        }
    }

    /**
     * 将健康度超过阈值的治疗槽位物品移入输出接口。
     */
    protected void routeItemsToOutputHatches() {
        if (level == null) return;

        for (int slot : populationSlots()) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty() && PopulationNBT.getHealth(stack) > healthThreshold) {
                for (BlockPos hatchPos : getOutputHatches()) {
                    BlockEntity be = level.getBlockEntity(hatchPos);
                    if (be instanceof com.gooodwei.civilizationevolution.server.blockentity.hatch.PrimitivePopulationOutputHatchBlockEntity hatch) {
                        ItemStack hatchStack = hatch.getItem(0);
                        if (hatchStack.isEmpty()) {
                            hatch.setItem(0, stack.copy());
                            setItem(slot, ItemStack.EMPTY);
                            setChanged();
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 将物品移到空的治疗槽位。
     */
    private void moveToEmptyTreatmentSlot(ItemStack stack, net.minecraft.world.Container source) {
        for (int slot : populationSlots()) {
            if (getItem(slot).isEmpty()) {
                setItem(slot, stack.copy());
                source.setItem(0, ItemStack.EMPTY);
                setChanged();
                return;
            }
        }
    }

    /** 检查两个治疗槽位是否已满 */
    private boolean bothSlotsFull() {
        for (int slot : populationSlots()) {
            if (getItem(slot).isEmpty()) return false;
        }
        return true;
    }

    /** 检查物品是否为有效的患者（PopulationItem、未死亡、年龄合适） */
    private boolean isValidPatient(ItemStack stack) {
        if (PopulationNBT.isDead(stack)) return false;
        int age = PopulationNBT.getAge(stack);
        return age >= 18 && age <= 65;
    }

    // ==================== 工作条件 ====================

    @Override
    public boolean canWork() {
        return structureFormed && super.canWork() && !getAvailableWorkers().isEmpty();
    }

    /**
     * 获取可用工作者列表，仅接受牧师和无业人口。
     */
    protected List<ItemStack> getAvailableWorkers() {
        List<ItemStack> all = new ArrayList<>();
        for (int slot : populationSlots()) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) all.add(stack);
        }
        return filterAvailable(all, stack ->
                Career.isKindOf(PopulationNBT.getCareer(stack), "cleric") ||
                Career.isKindOf(PopulationNBT.getCareer(stack), "unemployed"));
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
        return false; // Tier 0 诊所不消耗食物
    }

    @Override
    public boolean isOutputSlot(int slot) {
        return false; // 治疗槽位不是输出槽位
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
        if (!structureFormed) return null;
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

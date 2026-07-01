package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.IPopulationMachine;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 所有机器的抽象父类 —— 提供物品槽位、工作进度、绑定状态的通用实现。
 *
 * <p>子类分两条线：
 * <ul>
 *   <li>{@link AbstractCampBlockEntity} —— 无工作范围，直接继承</li>
 *   <li>{@link AbstractRangeMachineBlockEntity} —— 有工作范围+冲突检测+粒子边框</li>
 * </ul>
 *
 * <p>子类只需实现槽位分类方法、{@link #canWork()}、{@link #executeWorkCycle}、
 * {@link #createMenu} 和 {@link #getDisplayName()}。
 */
public abstract class AbstractMachineBlockEntity
        extends BaseContainerBlockEntity
        implements IPopulationMachine, MenuProvider {

    // ==================== 共享字段 ====================

    /** 物品槽位列表（final，构造器中初始化） */
    protected final NonNullList<ItemStack> items;
    /** 当前工作进度（tick 计数器） */
    protected int workProgress;
    /** 是否已被聚落控制器绑定 */
    protected boolean isBound = false;
    /** 绑定的核心 UUID，未绑定时为 null */
    protected String boundCoreUuid = null;
    /** 绑定的控制器所在维度 ID（如 "minecraft:overworld"），未绑定时为 null */
    protected String boundControllerDimension = null;

    // ==================== 构造器 ====================

    /**
     * @param type  BlockEntity 类型
     * @param pos   方块坐标
     * @param state 方块状态
     * @param size  容器槽位总数
     */
    protected AbstractMachineBlockEntity(BlockEntityType<?> type,
                                          BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    // ==================== 模板方法 ====================

    /**
     * 创建 ContainerData，供需要进度条同步的子类覆写。
     * 默认返回 0 字段（不使用进度条同步）。
     */
    protected ContainerData createData() {
        return new ContainerData() {
            @Override public int get(int index) { return 0; }
            @Override public void set(int index, int value) { }
            @Override public int getCount() { return 0; }
        };
    }

    // ==================== IPopulationMachine — 绑定 ====================

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
    public abstract Tier getTier();

    @Override
    public Container getContainer() {
        return this;
    }

    @Override
    public boolean isBound() {
        return this.isBound;
    }

    @Override
    public void setBound(boolean bound) {
        this.isBound = bound;
    }

    @Override
    public String getBoundCoreUuid() {
        return this.boundCoreUuid;
    }

    @Override
    public void setBoundCoreUuid(String uuid) {
        this.boundCoreUuid = uuid;
    }

    @Override
    public String getBoundControllerDimension() {
        return this.boundControllerDimension;
    }

    @Override
    public void setBoundControllerDimension(String dimension) {
        this.boundControllerDimension = dimension;
    }

    /**
     * 基础工作条件：已绑定。
     * 子类应覆写以追加冲突检查、食物检查、人口条件等。
     */
    @Override
    public boolean canWork() {
        return isBound();
    }

    // ==================== 健康度波动（子类可覆写） ====================

    /** 每次工作周期后人口健康度随机波动下限（默认 -5） */
    protected int getHealthFluctuateMin() {
        return -5;
    }

    /** 每次工作周期后人口健康度随机波动上限（默认 -1） */
    protected int getHealthFluctuateMax() {
        return -1;
    }

    // ==================== Container boilerplate ====================

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        for (int i = 0; i < this.items.size() && i < list.size(); i++) {
            this.items.set(i, list.get(i));
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player);
    }

    /**
     * 统一槽位限制：人口槽位仅接受 PopulationItem，输出槽位禁止放入。
     * 子类直接委托即可。
     */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return checkSlotRestriction(slot, stack) && super.canPlaceItem(slot, stack);
    }

    // ==================== NBT 持久化 ====================

    /**
     * 保存公共字段。子类应 {@code super.saveAdditional(tag, registries)} 后追加自有字段。
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("WorkProgress", workProgress);
        tag.putBoolean("IsBound", isBound);
        if (boundCoreUuid != null) {
            tag.putString("BoundCoreUuid", boundCoreUuid);
        }
        if (boundControllerDimension != null) {
            tag.putString("BoundControllerDimension", boundControllerDimension);
        }
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    /**
     * 加载公共字段。子类应 {@code super.loadAdditional(tag, registries)} 后加载自有字段。
     */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workProgress = tag.getInt("WorkProgress");
        isBound = tag.getBoolean("IsBound");
        String uuid = tag.getString("BoundCoreUuid");
        boundCoreUuid = uuid.isEmpty() ? null : uuid;
        String dimension = tag.getString("BoundControllerDimension");
        boundControllerDimension = dimension.isEmpty() ? null : dimension;
        ContainerHelper.loadAllItems(tag, items, registries);
    }
}

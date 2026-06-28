package com.gooodwei.civilizationevolution.server.blockentity.abstractmachine;

import com.gooodwei.civilizationevolution.api.IClientUpdateReceiver;
import com.gooodwei.civilizationevolution.api.util.ParticleBorderHelper;
import com.gooodwei.civilizationevolution.api.util.PopulationNBT;
import com.gooodwei.civilizationevolution.server.item.PopulationItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * 工作范围机器的抽象父类 —— 所有拥有工作范围、冲突检测、粒子边框的机器都继承此类。
 *
 * <p>子类只需实现：
 * <ul>
 *   <li>{@link #createData()} —— 提供 ContainerData（字段数因机器而异）</li>
 *   <li>{@link #getConflictTag()} —— 冲突检测用的 Block Tag</li>
 *   <li>{@link #getWorkTotalTime()} / {@link #getAgeIncrement()} —— 工作参数</li>
 *   <li>{@link #isPopulationSlot(int)} / {@link #populationSlots()} / {@link #isOutputSlot(int)} / {@link #isFoodSlot(int)} —— 槽位分类</li>
 *   <li>{@link #canWork()} / {@link #executeWorkCycle(Level)} —— 工作逻辑</li>
 *   <li>{@link #createMenu(int, net.minecraft.world.entity.player.Inventory)} —— GUI</li>
 *   <li>{@link #getDisplayName()} / {@link #getDefaultName()} —— 名称</li>
 * </ul>
 *
 * <p>如需定制工作范围，覆写 {@link #getHorizontalChunkRadius()} / {@link #getVerticalUpOffset()} / {@link #getVerticalDownOffset()}。
 *
 * @see AbstractMachineBlockEntity
 * @see IClientUpdateReceiver
 */
public abstract class AbstractRangeMachineBlockEntity
        extends AbstractMachineBlockEntity
        implements IClientUpdateReceiver {

    // ==================== 范围专属字段 ====================

    /** 剩余显示区块边框的 tick 数（0 = 不显示），每 5 tick 发送一轮粒子 */
    protected int showBorderTicks;
    /**
     * 冲突封禁标记：范围内有其他命中 {@link #getConflictTag()} 标签的
     * {@link AbstractRangeMachineBlockEntity} 时设为 true
     */
    protected boolean blockedByConflict;

    /** 客户端同步数据，由子类通过 {@link #createData()} 提供 */
    protected final ContainerData data;

    // ==================== 构造器 ====================

    /**
     * @param type  BlockEntity 类型
     * @param pos   方块坐标
     * @param state 方块状态
     * @param size  容器槽位总数
     */
    protected AbstractRangeMachineBlockEntity(BlockEntityType<?> type,
                                               BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
        this.data = createData();
    }

    // ==================== 抽象方法（子类实现） ====================

    /**
     * 子类创建 ContainerData（字段数因机器而异，HG=3, Ranch=2）。
     * 覆写父类默认的空实现。
     */
    @Override
    protected abstract ContainerData createData();

    /** 冲突检测用的 Block Tag */
    protected abstract TagKey<Block> getConflictTag();

    // ==================== 工作范围（子类可覆写） ====================

    /**
     * 水平范围半径（单位：区块数）。
     * 0 = 仅自身所在区块（1×1 区块），n = (2n+1)×(2n+1) 区块。
     * 默认 0。
     */
    protected int getHorizontalChunkRadius() {
        return 0;
    }

    /**
     * 垂直向上偏移（单位：格），基于机器方块 Y 坐标。
     * 正值 = 向上扩展。默认 2。
     */
    protected int getVerticalUpOffset() {
        return 2;
    }

    /**
     * 垂直向下偏移（单位：格），基于机器方块 Y 坐标。
     * 正值 = 向下扩展。默认 2。
     */
    protected int getVerticalDownOffset() {
        return 2;
    }

    /**
     * 实例方法：自动 clamp Y 轴到世界最低/最高建筑高度。
     * 子类只需覆写三个 {@code getXxx} 方法即可定制范围。
     */
    protected AABB getSelectionRange() {
        AABB raw = getSelectionRange(getBlockPos(),
                getHorizontalChunkRadius(),
                getVerticalUpOffset(),
                getVerticalDownOffset());
        if (this.level != null) {
            double clampedMinY = Math.max(raw.minY, this.level.getMinBuildHeight());
            double clampedMaxY = Math.min(raw.maxY, this.level.getMaxBuildHeight());
            return new AABB(raw.minX, clampedMinY, raw.minZ,
                    raw.maxX, clampedMaxY, raw.maxZ);
        }
        return raw;
    }

    /**
     * 静态便捷方法：水平半径 0（单区块），垂直 ±2。
     */
    public static AABB getSelectionRange(BlockPos pos) {
        return getSelectionRange(pos, 0, 2, 2);
    }

    /**
     * 静态方法：计算以 pos 所在区块为中心的 (2hR+1)×(2hR+1) 区块、
     * Y 轴 [pos.y - downOffset, pos.y + upOffset + 1) 的 AABB。
     * <b>不做世界高度 clamp</b>，需要 clamp 的调用方应使用实例方法 {@link #getSelectionRange()}。
     */
    public static AABB getSelectionRange(BlockPos pos,
                                          int hChunkRadius,
                                          int upOffset,
                                          int downOffset) {
        ChunkPos cp = new ChunkPos(pos);
        int minX = cp.getMinBlockX() - hChunkRadius * 16;
        int maxX = cp.getMaxBlockX() + 1 + hChunkRadius * 16;
        int minZ = cp.getMinBlockZ() - hChunkRadius * 16;
        int maxZ = cp.getMaxBlockZ() + 1 + hChunkRadius * 16;
        return new AABB(minX, pos.getY() - downOffset, minZ,
                maxX, pos.getY() + upOffset + 1, maxZ);
    }

    // ==================== 冲突检测 ====================

    /**
     * 扫描本机器工作范围，查找命中 {@link #getConflictTag()} 标签的
     * 其他 {@link AbstractRangeMachineBlockEntity}。将自身及所有冲突方块的
     * {@link #blockedByConflict} 设为 true，并为所有冲突方块触发红色粒子边框。
     */
    protected void scanAndMarkConflicts(ServerLevel level) {
        AABB aabb = getSelectionRange();
        TagKey<Block> tag = getConflictTag();

        boolean wasBlocked = this.blockedByConflict;
        List<AbstractRangeMachineBlockEntity> conflicts = new ArrayList<>();

        for (int x = (int) aabb.minX; x < (int) aabb.maxX; x++) {
            for (int z = (int) aabb.minZ; z < (int) aabb.maxZ; z++) {
                for (int y = (int) aabb.minY; y < (int) aabb.maxY; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (p.equals(worldPosition)) continue;
                    if (level.getBlockState(p).is(tag)) {
                        if (level.getBlockEntity(p) instanceof AbstractRangeMachineBlockEntity other
                                && other.getConflictTag() == tag) {
                            conflicts.add(other);
                        }
                    }
                }
            }
        }

        boolean hasConflict = !conflicts.isEmpty();
        this.blockedByConflict = hasConflict;

        for (AbstractRangeMachineBlockEntity other : conflicts) {
            other.blockedByConflict = true;
            other.showBorderTicks = 200;
        }

        if (hasConflict && !wasBlocked) {
            this.showBorderTicks = 200;
        }
    }

    /**
     * 沿区块边界发送粒子框，委托 {@link ParticleBorderHelper}。
     * <p>使用 {@code be.getSelectionRange()} 实例方法（自动适配子类定制范围）。
     * 颜色由 {@link #blockedByConflict} 决定：绿色 = 无冲突，红色 = 有冲突。
     */
    public static void showChunkBorder(ServerLevel level, BlockPos pos,
                                        AbstractRangeMachineBlockEntity be) {
        AABB aabb = be.getSelectionRange();
        TagKey<Block> tag = be.getConflictTag();
        boolean hasConflict = be.blockedByConflict;

        List<BlockPos> others = List.of();
        if (hasConflict) {
            List<BlockPos> list = new ArrayList<>();
            for (int x = (int) aabb.minX; x < (int) aabb.maxX; x++) {
                for (int z = (int) aabb.minZ; z < (int) aabb.maxZ; z++) {
                    for (int y = (int) aabb.minY; y < (int) aabb.maxY; y++) {
                        BlockPos p = new BlockPos(x, y, z);
                        if (!p.equals(pos) && level.getBlockState(p).is(tag)) {
                            list.add(p);
                        }
                    }
                }
            }
            others = list;
        }

        DustParticleOptions borderP = new DustParticleOptions(
                new Vector3f(hasConflict ? 0.8F : 0.1F, hasConflict ? 0.1F : 0.8F, 0.1F), 2.0F);

        ParticleBorderHelper.drawBoxEdges(level, aabb, borderP);

        if (hasConflict) {
            DustParticleOptions hl = new DustParticleOptions(new Vector3f(1.0F, 0.3F, 0.0F), 3.0F);
            ParticleBorderHelper.highlightBlocks(level, others, hl);
        }
    }

    /**
     * 放置或 GUI 关闭时调用 —— 扫描冲突、标记所有冲突方块、触发粒子边框。
     * 仅在服务端调用。
     */
    public void onPlacedOrOpened(ServerLevel level) {
        scanAndMarkConflicts(level);
        this.showBorderTicks = 200;
    }

    /**
     * 开始显示区块边框，持续 10 秒（不扫描冲突，仅设置计时器）。
     */
    public void startShowingBorder() {
        this.showBorderTicks = 200;
    }

    // ==================== 人员管理 ====================

    /**
     * 从人口槽位中筛选适合工作的人口物品。
     * <p>条件：PopulationItem、年龄 18-65、未死亡。
     *
     * @return 符合工作条件的人口物品列表（可能为空）
     */
    protected List<ItemStack> getAvailableWorkers() {
        List<ItemStack> all = new ArrayList<>();
        for (int slot : populationSlots()) {
            ItemStack stack = getItem(slot);
            if (!stack.isEmpty()) {
                all.add(stack);
            }
        }
        return filterAvailable(all, stack ->
                stack.getItem() instanceof PopulationItem
                        && !PopulationNBT.isDead(stack)
                        && PopulationNBT.getAge(stack) >= 18
                        && PopulationNBT.getAge(stack) <= 65);
    }

    // ==================== canWork 范围守卫 ====================

    /**
     * 在父类绑定检查基础上，追加冲突封禁和可用人口检查。
     * 子类应继续追加食物等条件。
     */
    @Override
    public boolean canWork() {
        if (blockedByConflict) return false;
        return super.canWork() && !getAvailableWorkers().isEmpty();
    }

    // ==================== NBT（仅追加范围专属字段） ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("BlockedByConflict", blockedByConflict);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        blockedByConflict = tag.getBoolean("BlockedByConflict");
    }

    // ==================== serverTick 基础逻辑 ====================

    /**
     * 服务端每 tick 调用。处理粒子边框倒计时和进度条推进。
     * 子类的静态 {@code serverTick} 方法应委托到此方法。
     */
    protected static <T extends AbstractRangeMachineBlockEntity> void serverTick(
            Level level, BlockPos pos, BlockState state, T be) {
        // 粒子边框显示倒计时
        if (be.showBorderTicks > 0) {
            be.showBorderTicks--;
            if (be.showBorderTicks % 5 == 0 && level instanceof ServerLevel serverLevel) {
                showChunkBorder(serverLevel, pos, be);
            }
        }

        // 绑定后 workProgress 持续推进用于 GUI 进度条，到达上限后停滞等待控制器触发
        if (be.isBound() && be.workProgress < be.getWorkTotalTime()) {
            be.workProgress++;
            setChanged(level, pos, state);
        }
    }
}

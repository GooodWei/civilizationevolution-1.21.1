package com.gooodwei.civilizationevolution.api;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 多方块机器的运行时状态组合对象。
 *
 * <p>将原本分散在 {@code AbstractMultiBlockMachineBlockEntity} 中的多方块相关字段
 * 集中到一个对象中，供 {@link IMultiBlockMachine} 接口的 default 方法通过
 * {@link IMultiBlockMachine#mbs()} 访问。
 *
 * <p>需持久化的字段（目前仅 {@link #structureFormed}）通过 {@link #saveToNBT}/{@link #loadFromNBT}
 * 读写，其余字段（位置缓存、解析结果等）在结构验证时重建。
 *
 * @see IMultiBlockMachine
 */
public class MultiBlockState {

    /** NBT key：结构是否完整成型 */
    public static final String TAG_STRUCTURE_FORMED = "StructureFormed";

    /** 结构是否完整成型 */
    public boolean structureFormed;

    /** 延迟验证倒计时（tick），大于 0 时每 tick 递减，归零时执行验证 */
    public int revalidationDelay;

    /** 定时验证计时器（tick），到达间隔时触发一次结构验证 */
    public int periodicValidationTimer;

    /** 解析后的结构模式缓存，避免重复解析 JSON。解析失败时为 null */
    @Nullable
    public IMultiBlockMachine.ParsedPattern cachedPattern;

    /** 结构解析错误信息（null 表示解析成功）。供方块交互时红字提示玩家。 */
    @Nullable
    public String parseError;

    /** 结构零件位置缓存（世界坐标），按角色分类。每次 validateStructure() 成功后重建 */
    public final List<BlockPos> populationInputHatches = new ArrayList<>(4);
    public final List<BlockPos> populationOutputHatches = new ArrayList<>(4);
    public final List<BlockPos> itemInputHatches = new ArrayList<>(4);
    public final List<BlockPos> itemOutputHatches = new ArrayList<>(4);
    public final List<BlockPos> foodHatches = new ArrayList<>(4);
    public final List<BlockPos> fluidInputHatches = new ArrayList<>(4);
    public final List<BlockPos> fluidOutputHatches = new ArrayList<>(4);
    /** 外壳方块通常数量较多，预分配 32 容量 */
    public final List<BlockPos> casingPositions = new ArrayList<>(32);

    /** 所有已成型零件位置（不分类型），用于破坏时通知所有零件 */
    public final Set<BlockPos> allPartPositions = new LinkedHashSet<>(64);

    // ==================== NBT 持久化 ====================

    /**
     * 将持久化字段写入 NBT。
     *
     * <p>当前仅持久化 {@link #structureFormed}，其余字段在服务器启动后通过
     * 结构验证重建。
     *
     * @param tag 目标 CompoundTag
     */
    public void saveToNBT(CompoundTag tag) {
        tag.putBoolean(TAG_STRUCTURE_FORMED, structureFormed);
    }

    /**
     * 从 NBT 读取持久化字段。
     *
     * <p>{@link #structureFormed} 从 NBT 加载作为初始参考值，
     * {@link IMultiBlockMachine#onMultiBlockLoad()} 的延迟验证会修正。
     *
     * @param tag 源 CompoundTag
     */
    public void loadFromNBT(CompoundTag tag) {
        structureFormed = tag.getBoolean(TAG_STRUCTURE_FORMED);
    }

    // ==================== 缓存清理 ====================

    /** 清除所有零件位置缓存和解析缓存 */
    public void clearPartCaches() {
        populationInputHatches.clear();
        populationOutputHatches.clear();
        foodHatches.clear();
        fluidInputHatches.clear();
        fluidOutputHatches.clear();
        casingPositions.clear();
        allPartPositions.clear();
        itemInputHatches.clear();
        itemOutputHatches.clear();
    }
}

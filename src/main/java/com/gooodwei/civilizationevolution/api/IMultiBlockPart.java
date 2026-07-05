package com.gooodwei.civilizationevolution.api;

import com.gooodwei.civilizationevolution.api.tier.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * 多方块结构零件接口。
 *
 * <p>所有参与多方块机器结构的方块（外壳、输入/输出接口等）必须实现此接口。
 * 零件的成型状态和控制器坐标由控制器 BE 统一追踪，
 * Block 是单例，不能存储位置相关状态。
 *
 * <p>用法：
 * <ul>
 *   <li>{@link #getPartTier()} 返回此零件的 Tier 等级</li>
 *   <li>{@link #getPartType()} 返回此零件的类型（用于多方块结构 JSON 的 alternatives 匹配）</li>
 *   <li>{@link #getOwningController(Level, BlockPos)} / {@link #claimPart(Level, BlockPos, BlockPos)} /
 *       {@link #releasePart(Level, BlockPos, BlockPos)} 管理零件所有权，
 *       默认委托给 {@link PartOwnershipTracker}。有 BlockEntity 的零件（hatch）应覆写以使用自身 NBT 持久化字段</li>
 * </ul>
 *
 * @see PartOwnershipTracker
 */
public interface IMultiBlockPart {

    // ==================== 零件类型常量 ====================

    /** 普通结构外壳方块 */
    String TYPE_MULTI_BLOCK_PART = "multi_block_part";
    /** 人口输入接口 */
    String TYPE_POPULATION_INPUT_HATCH = "population_input_hatch";
    /** 人口输出接口 */
    String TYPE_POPULATION_OUTPUT_HATCH = "population_output_hatch";
    /** 食物输入接口 */
    String TYPE_FOOD_HATCH = "food_hatch";
    /** 通用流体接口（向后兼容） */
    String TYPE_FLUID_HATCH = "fluid_hatch";
    /** 流体输入接口 */
    String TYPE_FLUID_INPUT_HATCH = "fluid_input_hatch";
    /** 流体输出接口 */
    String TYPE_FLUID_OUTPUT_HATCH = "fluid_output_hatch";
    /** 物品输入接口 */
    String TYPE_ITEM_INPUT_HATCH = "item_input_hatch";
    /** 物品输出接口 */
    String TYPE_ITEM_OUTPUT_HATCH = "item_output_hatch";

    // ==================== 抽象/默认方法 ====================

    /**
     * 此结构零件的 Tier 等级。
     *
     * <p>多方块成型验证时，控制器会检查每个零件：
     * {@code part.getPartTier().getLevel() <= controller.getTier().getLevel()}
     *
     * @return 此零件的 Tier 等级
     */
    Tier getPartTier();

    /**
     * 此结构零件的类型标识。
     *
     * <p>用于多方块结构 JSON 中 alternatives 匹配。
     * 默认返回 {@link #TYPE_MULTI_BLOCK_PART}（普通外壳方块），
     * hatch 子类应覆写返回对应的类型常量。
     *
     * @return 零件类型字符串（如 {@code "input_hatch"}）
     */
    default String getPartType() {
        return TYPE_MULTI_BLOCK_PART;
    }

    // ==================== 所有权管理方法 ====================

    /**
     * 获取当前认领此零件的控制器坐标。
     *
     * <p>默认实现委托给 {@link PartOwnershipTracker#getOwner(Level, BlockPos)}
     * （服务于无 BlockEntity 的外壳方块）。
     * 有 BlockEntity 的零件（hatch）应覆写此方法以返回自身 NBT 持久化的认领信息，
     * 以获得更好的性能和数据持久性。
     *
     * @param level   所在世界
     * @param partPos 此零件世界坐标（用于非 BE 零件的 key 生成）
     * @return 控制器坐标，未认领时返回 null
     */
    @Nullable
    default BlockPos getOwningController(Level level, BlockPos partPos) {
        return PartOwnershipTracker.getOwner(level, partPos);
    }

    /**
     * 由控制器认领此零件。
     *
     * <p>默认实现委托给 {@link PartOwnershipTracker#setOwner(Level, BlockPos, BlockPos)}。
     * 有 BlockEntity 的零件应覆写此方法以将认领信息持久化到自身 NBT 中。
     *
     * @param level         所在世界
     * @param partPos       此零件世界坐标
     * @param controllerPos 认领此零件的控制器坐标
     */
    default void claimPart(Level level, BlockPos partPos, BlockPos controllerPos) {
        PartOwnershipTracker.setOwner(level, partPos, controllerPos);
    }

    /**
     * 释放此零件（仅当调用者是当前拥有者时才释放）。
     *
     * <p>默认实现委托给 {@link PartOwnershipTracker#releaseOwner(Level, BlockPos, BlockPos)}。
     * 有 BlockEntity 的零件应覆写此方法以清除自身 NBT 中的认领信息。
     *
     * @param level         所在世界
     * @param partPos       此零件世界坐标
     * @param controllerPos 要释放的控制器坐标
     */
    default void releasePart(Level level, BlockPos partPos, BlockPos controllerPos) {
        PartOwnershipTracker.releaseOwner(level, partPos, controllerPos);
    }
}

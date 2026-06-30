package com.gooodwei.civilizationevolution.client.preview;

import com.gooodwei.civilizationevolution.api.PreviewBlockInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.List;

/**
 * 客户端多方块结构预览状态单例。
 *
 * <p>存储当前活跃的预览数据，供 {@code StructurePreviewRenderer} 每帧读取。
 * 通过 {@link #start} / {@link #stop} 管理生命周期，预览默认 15 秒后自动过期。
 */
public final class PreviewState {

    /** 预览持续时间（tick），15 秒 */
    public static final int DURATION_TICKS = 300;

    /** 玩家距控制器最大距离（超出自动停止预览） */
    public static final double MAX_DISTANCE_SQ = 32.0 * 32.0;

    private static BlockPos controllerPos;
    private static List<PreviewBlockInfo> blocks = Collections.emptyList();
    private static long expireTick;
    private static boolean active;

    private PreviewState() {}

    /**
     * 开启预览。
     *
     * @param controller 控制器世界坐标
     * @param previewBlocks 预览位置数据列表
     */
    public static void start(BlockPos controller, List<PreviewBlockInfo> previewBlocks) {
        controllerPos = controller;
        blocks = List.copyOf(previewBlocks);
        Minecraft mc = Minecraft.getInstance();
        expireTick = (mc.level != null) ? mc.level.getGameTime() + DURATION_TICKS : 0;
        active = true;
    }

    /** 停止并清除预览数据。 */
    public static void stop() {
        active = false;
        controllerPos = null;
        blocks = Collections.emptyList();
        expireTick = 0;
    }

    /** 预览是否处于活跃状态。 */
    public static boolean isActive() {
        return active && controllerPos != null && !blocks.isEmpty();
    }

    /** 获取控制器世界坐标。 */
    public static BlockPos getControllerPos() {
        return controllerPos;
    }

    /** 获取预览位置数据列表（不可变）。 */
    public static List<PreviewBlockInfo> getBlocks() {
        return blocks;
    }

    /** 获取预览过期刻。 */
    public static long getExpireTick() {
        return expireTick;
    }

    /**
     * 检查玩家是否距控制器过远（超出 {@link #MAX_DISTANCE_SQ}）。
     *
     * @return true 表示距离过远，应停止预览
     */
    public static boolean isTooFar() {
        if (controllerPos == null) return true;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        return mc.player.distanceToSqr(
                controllerPos.getX() + 0.5,
                controllerPos.getY() + 0.5,
                controllerPos.getZ() + 0.5) > MAX_DISTANCE_SQ;
    }
}

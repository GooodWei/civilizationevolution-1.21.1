package com.gooodwei.civilizationevolution.client.preview;

import com.gooodwei.civilizationevolution.api.PreviewBlockInfo;
import com.gooodwei.civilizationevolution.client.config.ClientConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 客户端多方块结构预览状态单例。
 *
 * <p>存储当前活跃的预览数据，供 {@code StructurePreviewRenderer} 每帧读取。
 *
 * <h3>视野盒过滤</h3>
 * <p>不渲染全部预览位置，仅渲染玩家周围由
 * {@link ClientConfig#getPreviewRenderDistance()} 配置的半径内的方块。默认半径 16 格
 * （33×33×33 视野盒）。玩家跨越方块边界时重新过滤并缓存，避免每帧遍历全量位置。
 *
 * <h3>生命周期</h3>
 * <p>预览持久化，不再定时取消。仅在以下情况关闭：
 * <ul>
 *   <li>玩家手持投影仪再次右键控制器（手动关闭）</li>
 *   <li>玩家跨维度</li>
 *   <li>玩家退出并重进世界</li>
 * </ul>
 */
public final class PreviewState {

    // 预览渲染半径由 ClientConfig 提供，不再使用硬编码常量

    private static BlockPos controllerPos;
    private static List<PreviewBlockInfo> blocks = Collections.emptyList();
    private static boolean active;

    /** 上次过滤时的视野盒中心（方块坐标），用于缓存失效判断 */
    private static BlockPos lastViewportCenter;
    /** 缓存的视野盒内可见方块列表 */
    private static List<PreviewBlockInfo> cachedVisibleBlocks = Collections.emptyList();

    private PreviewState() {}

    /**
     * 开启预览。
     *
     * @param controller    控制器世界坐标
     * @param previewBlocks 全部预览位置数据列表（服务端发送的完整集合）
     */
    public static void start(BlockPos controller, List<PreviewBlockInfo> previewBlocks) {
        controllerPos = controller;
        blocks = List.copyOf(previewBlocks);
        lastViewportCenter = null;
        cachedVisibleBlocks = Collections.emptyList();
        active = true;
    }

    /** 停止并清除预览数据。 */
    public static void stop() {
        active = false;
        controllerPos = null;
        blocks = Collections.emptyList();
        lastViewportCenter = null;
        cachedVisibleBlocks = Collections.emptyList();
    }

    /** 预览是否处于活跃状态。 */
    public static boolean isActive() {
        return active && controllerPos != null && !blocks.isEmpty();
    }

    /** 获取控制器世界坐标。 */
    public static BlockPos getControllerPos() {
        return controllerPos;
    }

    /**
     * 获取当前视野盒内的可见预览方块。
     *
     * <p>以玩家位置为中心，取 {@link ClientConfig#getPreviewRenderDistance()} 半径内的预览位置。
     * 仅在玩家跨越方块边界时重新过滤并缓存结果。
     *
     * @param viewerPos 玩家/摄像机世界坐标
     * @return 视野盒内的预览方块列表（不可变）
     */
    public static List<PreviewBlockInfo> getVisibleBlocks(Vec3 viewerPos) {
        if (!active || blocks.isEmpty()) return Collections.emptyList();

        BlockPos viewerBlock = BlockPos.containing(viewerPos);

        // 玩家未跨越方块边界 → 返回缓存
        if (viewerBlock.equals(lastViewportCenter)) {
            return cachedVisibleBlocks;
        }

        int r = ClientConfig.getPreviewRenderDistance();
        // 计算视野盒范围
        int minX = viewerBlock.getX() - r;
        int minY = viewerBlock.getY() - r;
        int minZ = viewerBlock.getZ() - r;
        int maxX = viewerBlock.getX() + r;
        int maxY = viewerBlock.getY() + r;
        int maxZ = viewerBlock.getZ() + r;

        List<PreviewBlockInfo> visible = new ArrayList<>();
        for (PreviewBlockInfo info : blocks) {
            BlockPos pos = info.pos();
            if (pos.getX() >= minX && pos.getX() <= maxX
                    && pos.getY() >= minY && pos.getY() <= maxY
                    && pos.getZ() >= minZ && pos.getZ() <= maxZ) {
                visible.add(info);
            }
        }

        lastViewportCenter = viewerBlock;
        cachedVisibleBlocks = Collections.unmodifiableList(visible);
        return cachedVisibleBlocks;
    }
}

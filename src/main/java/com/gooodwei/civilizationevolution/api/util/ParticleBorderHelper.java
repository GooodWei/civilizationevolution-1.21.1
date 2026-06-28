package com.gooodwei.civilizationevolution.api.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;

import java.util.Collection;

/**
 * 粒子边框渲染工具类，供各机器和客户端高亮功能复用。
 *
 * <p>接受任意 {@link ParticleOptions} 实现，可配合自定义粒子类型
 * （如 {@link HighlightParticleOptions}）实现透视效果。
 */
public final class ParticleBorderHelper {

    private ParticleBorderHelper() {}

    /**
     * 沿两点间直线发送粒子，步长 0.5 格。
     *
     * @param level 服务端世界
     * @param p     粒子选项（颜色 + 大小）
     * @param x1    起点 X
     * @param y1    起点 Y
     * @param z1    起点 Z
     * @param x2    终点 X
     * @param y2    终点 Y
     * @param z2    终点 Z
     */
    public static void drawLine(ServerLevel level, ParticleOptions p,
                                double x1, double y1, double z1,
                                double x2, double y2, double z2) {
        double dist = Math.sqrt(
                (x2 - x1) * (x2 - x1) +
                        (y2 - y1) * (y2 - y1) +
                        (z2 - z1) * (z2 - z1));
        int steps = (int) (dist / 0.5);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            level.sendParticles(p,
                    x1 + (x2 - x1) * t,
                    y1 + (y2 - y1) * t,
                    z1 + (z2 - z1) * t,
                    1, 0, 0, 0, 0);
        }
    }

    /**
     * 绘制 AABB 的 12 条边。
     *
     * @param level 服务端世界
     * @param aabb  边框范围（min 为闭区间，max 为开区间）
     * @param p     粒子选项（颜色 + 大小）
     */
    public static void drawBoxEdges(ServerLevel level, AABB aabb, ParticleOptions p) {
        double minX = aabb.minX, maxX = aabb.maxX;
        double minY = aabb.minY, maxY = aabb.maxY;
        double minZ = aabb.minZ, maxZ = aabb.maxZ;

        // 底面 4 条边
        drawLine(level, p, minX, minY, minZ, maxX, minY, minZ);
        drawLine(level, p, maxX, minY, minZ, maxX, minY, maxZ);
        drawLine(level, p, maxX, minY, maxZ, minX, minY, maxZ);
        drawLine(level, p, minX, minY, maxZ, minX, minY, minZ);

        // 顶面 4 条边
        drawLine(level, p, minX, maxY, minZ, maxX, maxY, minZ);
        drawLine(level, p, maxX, maxY, minZ, maxX, maxY, maxZ);
        drawLine(level, p, maxX, maxY, maxZ, minX, maxY, maxZ);
        drawLine(level, p, minX, maxY, maxZ, minX, maxY, minZ);

        // 4 条竖直边
        drawLine(level, p, minX, minY, minZ, minX, maxY, minZ);
        drawLine(level, p, maxX, minY, minZ, maxX, maxY, minZ);
        drawLine(level, p, maxX, minY, maxZ, maxX, maxY, maxZ);
        drawLine(level, p, minX, minY, maxZ, minX, maxY, maxZ);
    }

    /**
     * 高亮指定方块位置集合（在每个方块中心发送粒子）。
     *
     * @param level     服务端世界
     * @param positions 需要高亮的方块位置
     * @param p         粒子选项（颜色 + 大小）
     */
    public static void highlightBlocks(ServerLevel level, Collection<BlockPos> positions, ParticleOptions p) {
        for (BlockPos pos : positions) {
            double cx = pos.getX() + 0.5;
            double cy = pos.getY() + 0.5;
            double cz = pos.getZ() + 0.5;
            for (int i = 0; i < 4; i++) {
                level.sendParticles(p, cx, cy, cz, 2, 0.25, 0.25, 0.25, 0);
            }
        }
    }
}

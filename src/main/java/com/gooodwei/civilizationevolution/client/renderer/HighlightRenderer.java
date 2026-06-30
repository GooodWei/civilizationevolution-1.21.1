package com.gooodwei.civilizationevolution.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Vector3f;

/**
 * 客户端高亮线框渲染器（完全独立，不依赖任何 GUI 或 Screen）。
 *
 * <p>核心设计：
 * <ul>
 *   <li>定时器基于游戏刻（{@code ClientLevel.getGameTime()}），不受 GUI 开关影响</li>
 *   <li>通过 {@link ClientTickEvent.Post} 自己倒计时和生成粒子</li>
 *   <li>通过 {@link RenderLevelStageEvent.Stage#AFTER_PARTICLES} 绘制可透视方块的金色线框</li>
 *   <li>外部只需调用 {@link #setTarget(BlockPos)} 即可启动 10 秒高亮</li>
 * </ul>
 *
 * <p>使用示例：
 * <pre>{@code
 * HighlightRenderer.setTarget(machinePos);
 * }</pre>
 */
@EventBusSubscriber(value = Dist.CLIENT)
public class HighlightRenderer {

    /** 高亮持续游戏刻数（10 秒 = 200 tick） */
    private static final int DURATION_TICKS = 200;
    /** 粒子生成间隔（每 5 tick 一轮） */
    private static final int PARTICLE_INTERVAL = 5;

    /** 当前高亮目标坐标，null 表示无高亮 */
    private static volatile BlockPos target;
    /** 高亮结束的游戏刻（高亮期间有效） */
    private static long endGameTick;
    /** 距下一轮粒子生成的倒计时 */
    private static int particleCooldown;

    private HighlightRenderer() {}

    // ==================== 公开 API ====================

    /**
     * 启动 10 秒高亮。可在 GUI 内或 GUI 外调用，渲染器会独立维护计时。
     *
     * @param pos 目标机器坐标
     */
    public static void setTarget(BlockPos pos) {
        target = pos;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            endGameTick = mc.level.getGameTime() + DURATION_TICKS;
        } else {
            endGameTick = 0;
        }
        particleCooldown = 0; // 下一 tick 立即生成首轮粒子
    }

    /** 立即清除高亮。 */
    public static void clearTarget() {
        target = null;
        endGameTick = 0;
        particleCooldown = 0;
    }

    // ==================== 每客户端 tick：倒计时 + 粒子 ====================

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (target == null) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        long now = level.getGameTime();
        if (now >= endGameTick) {
            target = null;
            return;
        }

        // 每 PARTICLE_INTERVAL tick 生成一轮粒子
        particleCooldown--;
        if (particleCooldown <= 0) {
            particleCooldown = PARTICLE_INTERVAL;
            spawnParticles(level);
        }
    }

    // ==================== 世界渲染阶段：可透视金色线框 ====================

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (target == null) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        long now = mc.level.getGameTime();
        long remaining = endGameTick - now;
        if (remaining <= 0) {
            target = null;
            return;
        }

        Vec3 cam = event.getCamera().getPosition();
        double dx = target.getX() - cam.x;
        double dy = target.getY() - cam.y;
        double dz = target.getZ() - cam.z;

        // 脉冲：随时间波动（基于剩余时间，更快的变化率使效果更明显）
        float pulse = 0.55F + 0.45F * (float) Math.abs(Math.sin(remaining * 0.12));
        float r = 1.0F, g = 0.7F, b = 0.1F;

        float x0 = (float) dx;
        float y0 = (float) dy;
        float z0 = (float) dz;
        float x1 = x0 + 1.0F;
        float y1 = y0 + 1.0F;
        float z1 = z0 + 1.0F;

        // ---- 保存渲染状态 ----
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();

        try {
            // ---- 第一遍：半透明面（提供"幽灵方块"透视效果） ----
            BufferBuilder faceBuilder = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            float faceAlpha = pulse * 0.18F;
            addQuad(faceBuilder, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, r, g, b, faceAlpha); // 底面
            addQuad(faceBuilder, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, r, g, b, faceAlpha); // 顶面
            addQuad(faceBuilder, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, r, g, b, faceAlpha); // 北面
            addQuad(faceBuilder, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, r, g, b, faceAlpha); // 南面
            addQuad(faceBuilder, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, r, g, b, faceAlpha); // 东面
            addQuad(faceBuilder, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, r, g, b, faceAlpha); // 西面
            BufferUploader.drawWithShader(faceBuilder.buildOrThrow());

            // ---- 第二遍：实线边框 ----
            BufferBuilder lineBuilder = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR);
            float lineAlpha = pulse * 0.9F;
            // 底面 4 条边
            addLine(lineBuilder, x0, y0, z0, x1, y0, z0, r, g, b, lineAlpha);
            addLine(lineBuilder, x1, y0, z0, x1, y0, z1, r, g, b, lineAlpha);
            addLine(lineBuilder, x1, y0, z1, x0, y0, z1, r, g, b, lineAlpha);
            addLine(lineBuilder, x0, y0, z1, x0, y0, z0, r, g, b, lineAlpha);
            // 顶面 4 条边
            addLine(lineBuilder, x0, y1, z0, x1, y1, z0, r, g, b, lineAlpha);
            addLine(lineBuilder, x1, y1, z0, x1, y1, z1, r, g, b, lineAlpha);
            addLine(lineBuilder, x1, y1, z1, x0, y1, z1, r, g, b, lineAlpha);
            addLine(lineBuilder, x0, y1, z1, x0, y1, z0, r, g, b, lineAlpha);
            // 4 条竖直边
            addLine(lineBuilder, x0, y0, z0, x0, y1, z0, r, g, b, lineAlpha);
            addLine(lineBuilder, x1, y0, z0, x1, y1, z0, r, g, b, lineAlpha);
            addLine(lineBuilder, x1, y0, z1, x1, y1, z1, r, g, b, lineAlpha);
            addLine(lineBuilder, x0, y0, z1, x0, y1, z1, r, g, b, lineAlpha);
            BufferUploader.drawWithShader(lineBuilder.buildOrThrow());

        } finally {
            // ---- 恢复渲染状态 ----
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
    }

    // ==================== 粒子生成 ====================

    /**
     * 在目标方块 12 条棱边上沿直线生成金色粉尘粒子。
     */
    private static void spawnParticles(ClientLevel level) {
        BlockPos pos = target;
        if (pos == null) return;

        AABB box = new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
        DustParticleOptions particle = new DustParticleOptions(
                new Vector3f(1.0F, 0.7F, 0.1F), 2.0F);

        double minX = box.minX, maxX = box.maxX;
        double minY = box.minY, maxY = box.maxY;
        double minZ = box.minZ, maxZ = box.maxZ;

        spawnLine(minX, minY, minZ, maxX, minY, minZ, particle, level); // 底前
        spawnLine(maxX, minY, minZ, maxX, minY, maxZ, particle, level); // 底右
        spawnLine(maxX, minY, maxZ, minX, minY, maxZ, particle, level); // 底后
        spawnLine(minX, minY, maxZ, minX, minY, minZ, particle, level); // 底左
        spawnLine(minX, maxY, minZ, maxX, maxY, minZ, particle, level); // 顶前
        spawnLine(maxX, maxY, minZ, maxX, maxY, maxZ, particle, level); // 顶右
        spawnLine(maxX, maxY, maxZ, minX, maxY, maxZ, particle, level); // 顶后
        spawnLine(minX, maxY, maxZ, minX, maxY, minZ, particle, level); // 顶左
        spawnLine(minX, minY, minZ, minX, maxY, minZ, particle, level); // 竖左前
        spawnLine(maxX, minY, minZ, maxX, maxY, minZ, particle, level); // 竖右前
        spawnLine(maxX, minY, maxZ, maxX, maxY, maxZ, particle, level); // 竖右后
        spawnLine(minX, minY, maxZ, minX, maxY, maxZ, particle, level); // 竖左后
    }

    /** 沿两点间直线等距生成粒子 */
    private static void spawnLine(double x1, double y1, double z1,
                                  double x2, double y2, double z2,
                                  DustParticleOptions options, ClientLevel level) {
        double dist = Math.sqrt(
                (x2 - x1) * (x2 - x1) +
                (y2 - y1) * (y2 - y1) +
                (z2 - z1) * (z2 - z1));
        int steps = (int) (dist / 0.5);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            level.addParticle(options,
                    x1 + (x2 - x1) * t,
                    y1 + (y2 - y1) * t,
                    z1 + (z2 - z1) * t,
                    0, 0, 0);
        }
    }

    // ==================== 渲染辅助 ====================

    private static void addLine(BufferBuilder builder,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float r, float g, float b, float a) {
        builder.addVertex(x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(x2, y2, z2).setColor(r, g, b, a);
    }

    private static void addQuad(BufferBuilder builder,
                                float x0, float y0, float z0,
                                float x1, float y1, float z1,
                                float x2, float y2, float z2,
                                float x3, float y3, float z3,
                                float r, float g, float b, float a) {
        builder.addVertex(x0, y0, z0).setColor(r, g, b, a);
        builder.addVertex(x1, y1, z1).setColor(r, g, b, a);
        builder.addVertex(x2, y2, z2).setColor(r, g, b, a);
        builder.addVertex(x3, y3, z3).setColor(r, g, b, a);
    }
}

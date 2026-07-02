package com.gooodwei.civilizationevolution.client.renderer;

import com.gooodwei.civilizationevolution.server.block.controller.AbstractControllerBlock;
import com.gooodwei.civilizationevolution.server.blockentity.controller.AbstractControllerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

/**
 * 控制器信标光柱渲染器（BlockEntityRenderer）。
 *
 * <p>当控制器方块满足以下条件时，渲染一道旋转的彩虹渐变光柱：
 * <ol>
 *   <li>{@link AbstractControllerBlock#BEAM_ACTIVE} 为 {@code true}</li>
 *   <li>即：方块收到红石信号 且 内部文明核心 UUID 有效</li>
 * </ol>
 *
 * <h3>光柱属性</h3>
 * <ul>
 *   <li><b>结构</b>：双层菱形光束，内层不透明（半径 0.2，alpha=1.0），
 *       外层半透明光晕（半径 0.25，alpha=0.15）——与原版信标一致</li>
 *   <li><b>高度</b>：从方块顶部直达世界最高点（穿透所有方块）</li>
 *   <li><b>颜色</b>：基于游戏时间正弦波生成的彩虹渐变色</li>
 *   <li><b>旋转</b>：绕 Y 轴持续旋转，速度 2.25°/tick（与原版一致）</li>
 * </ul>
 *
 * <p>通过 {@link MultiBufferSource#getBuffer(RenderType)} 获取顶点消费者，
 * 使用 {@link RenderType#beaconBeam(ResourceLocation, boolean)} 渲染类型，
 * 与原版信标完全一致的渲染管线，不干扰全局 GL 状态。
 *
 * @see AbstractControllerBlock
 * @see AbstractControllerBlockEntity
 */
public class ControllerBeamRenderer implements BlockEntityRenderer<AbstractControllerBlockEntity> {

    /** 原版信标光束纹理 */
    private static final ResourceLocation BEAM_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/beacon_beam.png");

    /** 内层不透明光束半宽（格），与原版信标一致 */
    private static final float BEAM_RADIUS = 0.2F;

    /** 外层半透明光晕半宽（格），比内层略大形成光晕包裹效果 */
    private static final float GLOW_RADIUS = 0.25F;

    /** 外层光晕 alpha（原版为 32/255 ≈ 0.125） */
    private static final float GLOW_ALPHA = 0.15F;

    /** 光束旋转速度（度/tick） */
    private static final float ROTATION_SPEED = 2.25F;

    /**
     * BER 构造器，由 NeoForge 的 {@link BlockEntityRendererProvider} 调用。
     *
     * @param context 渲染器上下文（当前未使用，预留扩展）
     */
    public ControllerBeamRenderer(BlockEntityRendererProvider.Context context) {
    }

    /**
     * 渲染控制器信标光柱。
     *
     * <p>双层渲染（与原版信标一致）：
     * <ol>
     *   <li><b>不透明通道</b>（blur=false）：使用 alpha test 而非 alpha blend，
     *       渲染在云层之前，确保光柱遮挡背后的云</li>
     *   <li><b>半透明通道</b>（blur=true）：alpha blend 叠加在不透明通道外，
     *       产生发光柔化效果</li>
     * </ol>
     *
     * @param be           控制器方块实体
     * @param partialTick  部分 tick 插值
     * @param poseStack    矩阵栈
     * @param bufferSource 多缓冲源
     * @param packedLight  光照信息
     * @param packedOverlay 覆盖信息
     */
    @Override
    public void render(AbstractControllerBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        if (!be.getBlockState().getValue(AbstractControllerBlock.BEAM_ACTIVE)) {
            return;
        }

        Level level = be.getLevel();
        if (level == null) return;

        long gameTime = level.getGameTime();
        float time = gameTime + partialTick;

        // ---- 彩虹色相计算 ----
        float t = time / 20.0F;
        float r = (float) (Math.sin(t * 0.5) * 0.5 + 0.5);
        float g = (float) (Math.sin(t * 0.5 + Math.PI * 2.0 / 3.0) * 0.5 + 0.5);
        float b = (float) (Math.sin(t * 0.5 + Math.PI * 4.0 / 3.0) * 0.5 + 0.5);

        // ---- 光束总高度 ----
        float totalHeight = level.getMaxBuildHeight() - be.getBlockPos().getY() - 1.0F;
        if (totalHeight <= 0) return;

        // ---- 旋转角度（与原版信标一致的 2.25°/tick） ----
        float rotation = time * ROTATION_SPEED;
        double cos = Math.cos(Math.toRadians(rotation));
        double sin = Math.sin(Math.toRadians(rotation));

        // ---- 平移至方块中心、向上偏移 1 格 ----
        poseStack.pushPose();
        poseStack.translate(0.5, 1.0, 0.5);

        Matrix4f matrix = poseStack.last().pose();

        // ====== 第一通道：不透明内层光束（半径 0.2，alpha=1.0） ======
        // blur=false → alpha test，遮挡云层
        RenderType opaqueType = RenderType.beaconBeam(BEAM_TEXTURE, false);
        VertexConsumer opaqueConsumer = bufferSource.getBuffer(opaqueType);
        float[][] beamCorners = computeDiamondCorners(BEAM_RADIUS, cos, sin);
        drawBeamQuads(opaqueConsumer, matrix, beamCorners, totalHeight, r, g, b, 1.0F);

        // ====== 第二通道：半透明外层光晕（半径 0.25，低 alpha） ======
        // blur=true → alpha blend，包裹在内层外产生柔光
        RenderType glowType = RenderType.beaconBeam(BEAM_TEXTURE, true);
        VertexConsumer glowConsumer = bufferSource.getBuffer(glowType);
        float[][] glowCorners = computeDiamondCorners(GLOW_RADIUS, cos, sin);
        drawBeamQuads(glowConsumer, matrix, glowCorners, totalHeight, r, g, b, GLOW_ALPHA);

        poseStack.popPose();
    }

    /**
     * 计算旋转后的菱形四角点（与原版信标一致）。
     *
     * <p>未旋转时为 (0, r), (r, 0), (0, -r), (-r, 0)，
     * 即正方形旋转 45° 后的菱形布局。
     */
    private float[][] computeDiamondCorners(float radius, double cos, double sin) {
        // 未旋转的菱形四角（顺时针排列）
        float[] lx = {0.0F, radius, 0.0F, -radius};
        float[] lz = {radius, 0.0F, -radius, 0.0F};
        float[][] corners = new float[4][2];
        for (int i = 0; i < 4; i++) {
            corners[i][0] = (float) (lx[i] * cos - lz[i] * sin);
            corners[i][1] = (float) (lx[i] * sin + lz[i] * cos);
        }
        return corners;
    }

    /**
     * 绘制光柱的 4 个垂直面四边形。
     *
     * @param consumer    顶点消费者
     * @param matrix      当前变换矩阵
     * @param corners     光柱 4 个角点（2D 坐标数组，[i][0]=x, [i][1]=z）
     * @param totalHeight 光柱总高度
     * @param r           红色分量（0-1）
     * @param g           绿色分量（0-1）
     * @param b           蓝色分量（0-1）
     * @param alpha       透明度（0-1）
     */
    private void drawBeamQuads(VertexConsumer consumer, Matrix4f matrix,
                               float[][] corners, float totalHeight,
                               float r, float g, float b, float alpha) {
        float v0 = 0.0F;
        float v1 = totalHeight;

        int fullBright = LightTexture.FULL_BRIGHT;
        int uv2u = fullBright & 0xFFFF;
        int uv2v = fullBright >> 16 & 0xFFFF;

        for (int side = 0; side < 4; side++) {
            int next = (side + 1) % 4;
            float u0 = side == 0 || side == 2 ? 0.0F : 0.5F;
            float u1 = side == 0 || side == 2 ? 0.5F : 1.0F;

            // 计算面法线（垂直于面朝外）
            float dx = corners[next][0] - corners[side][0];
            float dz = corners[next][1] - corners[side][1];
            float len = (float) Math.sqrt(dx * dx + dz * dz);
            float nx = dz / len;
            float nz = -dx / len;

            // 底-左 → 底-右 → 顶-右 → 顶-左（四边形顶点）
            consumer.addVertex(matrix, corners[side][0], 0.0F, corners[side][1])
                    .setUv(u0, v0).setColor(r, g, b, alpha)
                    .setUv2(uv2u, uv2v).setNormal(nx, 0.0F, nz);
            consumer.addVertex(matrix, corners[next][0], 0.0F, corners[next][1])
                    .setUv(u1, v0).setColor(r, g, b, alpha)
                    .setUv2(uv2u, uv2v).setNormal(nx, 0.0F, nz);
            consumer.addVertex(matrix, corners[next][0], totalHeight, corners[next][1])
                    .setUv(u1, v1).setColor(r, g, b, alpha)
                    .setUv2(uv2u, uv2v).setNormal(nx, 0.0F, nz);
            consumer.addVertex(matrix, corners[side][0], totalHeight, corners[side][1])
                    .setUv(u0, v1).setColor(r, g, b, alpha)
                    .setUv2(uv2u, uv2v).setNormal(nx, 0.0F, nz);
        }
    }

    /**
     * 光束极高，需在屏幕外也渲染。
     */
    @Override
    public boolean shouldRenderOffScreen(AbstractControllerBlockEntity be) {
        return true;
    }

    /**
     * 返回覆盖整个光柱高度的渲染包围盒，确保光柱不被视锥体剔除。
     *
     * <p>默认的包围盒仅为方块本身的 1×1×1 区域。
     * 当玩家抬头看天时，控制器方块可能不在视锥体内，
     * 但光柱仍应可见——因此包围盒必须向上延伸至世界最高点。
     *
     * <p>此方法由 {@code ClientHooks.isBlockEntityRendererVisible} 调用，
     * 与 {@link #shouldRenderOffScreen} 配合确保光柱始终可见。
     *
     * @param be 控制器方块实体
     * @return 覆盖完整光柱的 AABB
     */
    @Override
    public AABB getRenderBoundingBox(AbstractControllerBlockEntity be) {
        Level level = be.getLevel();
        int worldTop = level != null ? level.getMaxBuildHeight() : 320;
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 1, worldTop, pos.getZ() + 1);
    }
}

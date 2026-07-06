package com.gooodwei.civilizationevolution.client.renderer;

import com.gooodwei.civilizationevolution.api.IMultiBlockPart;
import com.gooodwei.civilizationevolution.api.PreviewBlockInfo;
import com.gooodwei.civilizationevolution.client.preview.PreviewState;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 多方块结构预览渲染器。
 *
 * <p>概念借鉴 StructureLib 的两遍渲染（depth ON → depth OFF），
 * 使用原版
 * 渲染带有实际方块纹理的半透明模型。
 *
 * <p>着色规则（通过 {@code RenderSystem.setShaderColor} 染色）：
 * <ul>
 *   <li><b>正常色</b> — 方块已正确放置</li>
 *   <li><b>蓝色调</b> — 位置为空（尚未放置）</li>
 *   <li><b>红色调</b> — 放错了方块类型</li>
 * </ul>
 *
 * <h3>视野盒过滤</h3>
 * <p>仅渲染玩家周围由客户端配置文件设定的半径内的预览方块，
 * 由 {@link PreviewState#getVisibleBlocks(Vec3)} 动态过滤。
 * 避免超大结构渲染全部位置造成客户端帧率崩溃。
 *
 * <h3>生命周期</h3>
 * <p>预览持久化，不再定时取消。{@link ClientTickEvent.Post} 仅检测世界变化
 * （跨维度/重进世界），不检查过期或距离。手动关闭由服务端
 * {@code StructurePreviewPayload} 空列表触发。
 */
@EventBusSubscriber(value = Dist.CLIENT)
public final class StructurePreviewRenderer {

    /** 第一遍（depth ON）alpha */
    private static final float ALPHA_OPAQUE = 0.5F;
    /** 第二遍（depth OFF）alpha */
    private static final float ALPHA_GHOST = 0.18F;

    /** 位置状态枚举 */
    private enum Status { CORRECT, MISSING, WRONG }

    /** 上一 tick 的客户端世界引用，用于检测跨维度/重进世界 */
    private static ClientLevel lastLevel;

    /**
     * partType → 代表方块状态缓存。
     * 首次渲染时扫描所有 {@link BlockEntityType}，为每种 partType
     * 选取最低 tier 的 {@link IMultiBlockPart} 对应方块。
     */
    private static final Map<String, BlockState> PART_TYPE_REPRESENTATIVES = new HashMap<>();
    private static volatile boolean partTypesScanned;

    private StructurePreviewRenderer() {}

    /**
     * 扫描所有注册的 {@link BlockEntityType}，为每种
     * {@link IMultiBlockPart#getPartType()} 找到最低 tier 的方块。
     * 仅首次调用时执行，结果缓存到 {@link #PART_TYPE_REPRESENTATIVES}。
     *
     * <p>通过反射读取 {@code BlockEntityType} 的私有 {@code validBlocks}
     * 字段获取每个 BE 类型对应的方块集合。
     */
    @SuppressWarnings("unchecked")
    private static void ensurePartTypesScanned() {
        if (partTypesScanned) return;
        partTypesScanned = true;

        Field validBlocksField = null;
        try {
            validBlocksField = BlockEntityType.class.getDeclaredField("validBlocks");
            validBlocksField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            return; // 无法访问，跳过扫描
        }

        Map<String, Integer> bestTiers = new HashMap<>();

        for (BlockEntityType<?> beType : BuiltInRegistries.BLOCK_ENTITY_TYPE) {
            Set<Block> validBlocks;
            try {
                validBlocks = (Set<Block>) validBlocksField.get(beType);
            } catch (IllegalAccessException e) {
                continue;
            }
            if (validBlocks == null || validBlocks.isEmpty()) continue;

            Block sampleBlock = validBlocks.iterator().next();
            BlockState sampleState = sampleBlock.defaultBlockState();

            BlockEntity be = beType.create(BlockPos.ZERO, sampleState);
            if (be instanceof IMultiBlockPart part) {
                String partType = part.getPartType();
                int tier = part.getPartTier().getLevel();

                Integer best = bestTiers.get(partType);
                if (best == null || tier < best) {
                    bestTiers.put(partType, tier);
                    PART_TYPE_REPRESENTATIVES.put(partType, sampleState);
                }
            }
        }
    }

    // ==================== 每客户端 tick：世界变化检测 ====================

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;

        // 检测世界变化（跨维度 / 重进世界）→ 停止预览
        if (lastLevel != null && level != lastLevel) {
            PreviewState.stop();
        }
        lastLevel = level;
    }

    // ==================== 世界渲染 ====================

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!PreviewState.isActive()) return;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        // 获取当前视野盒内的可见预览方块（以玩家位置为中心 11³）
        List<PreviewBlockInfo> blocks = PreviewState.getVisibleBlocks(event.getCamera().getPosition());
        BlockPos controllerPos = PreviewState.getControllerPos();
        if (blocks.isEmpty()) return;

        Vec3 cam = event.getCamera().getPosition();
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        // 将 solid 类 RenderType 重定向到 translucent，确保 alpha 生效
        MultiBufferSource translucentSource = new TranslucentRedirect(bufferSource);

        // ---- 保存渲染状态 ----
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        try {
            // ========== 第一遍：depthTest ON ==========
            RenderSystem.enableDepthTest();
            renderPreviewBlocks(blocks, controllerPos, level, cam, poseStack,
                    translucentSource, mc, ALPHA_OPAQUE);
            bufferSource.endBatch(RenderType.translucent());

            // ========== 第二遍：depthTest OFF（透视效果） ==========
            RenderSystem.disableDepthTest();
            renderPreviewBlocks(blocks, controllerPos, level, cam, poseStack,
                    translucentSource, mc, ALPHA_GHOST);
            bufferSource.endBatch(RenderType.translucent());

        } finally {
            // ---- 恢复渲染状态 ----
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
        }
    }

    /** 遍历所有预览位置并渲染对应方块模型 */
    private static void renderPreviewBlocks(List<PreviewBlockInfo> blocks,
                                            BlockPos controllerPos, ClientLevel level,
                                            Vec3 cam, PoseStack poseStack,
                                            MultiBufferSource bufferSource, Minecraft mc,
                                            float alphaMul) {
        for (PreviewBlockInfo info : blocks) {
            BlockState previewState = resolvePreviewBlockState(info, level, blocks);
            if (previewState == null) continue;

            Status status = classify(info.pos(), info.expectedTypes(), level, controllerPos);
            float[] tint = getTint(status, alphaMul);

            poseStack.pushPose();
            // 平移到目标方块位置
            poseStack.translate(
                    info.pos().getX() - cam.x,
                    info.pos().getY() - cam.y,
                    info.pos().getZ() - cam.z);
            // 75% 缩放并居中（剩余 25% 均分到各边）
            poseStack.translate(0.125, 0.125, 0.125);
            poseStack.scale(0.75F, 0.75F, 0.75F);

            RenderSystem.setShaderColor(tint[0], tint[1], tint[2], tint[3]);

            mc.getBlockRenderer().renderSingleBlock(
                    previewState, poseStack, bufferSource,
                    LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);

            poseStack.popPose();
        }
    }

    // ==================== 方块状态解析 ====================

    /**
     * 解析用于预览渲染的 {@link BlockState}。
     *
     * <p>解析优先级：
     * <ol>
     *   <li>当前位置已有方块（不论对错）→ 直接用其模型</li>
     *   <li>expectedTypes 中有方块注册名 → 取第一个</li>
     *   <li>expectedTypes 中有 Block Tag → 取 tag 第一个成员</li>
     *   <li>expectedTypes 中有 part 类型 → 扫描注册表中最低 tier 的代表方块</li>
     *   <li>无法确定 → 不渲染</li>
     * </ol>
     *
     * @param info   当前预览位置数据
     * @param level  客户端世界
     * @param allBlocks  所有预览位置（保留兼容，当前未使用）
     */
    private static BlockState resolvePreviewBlockState(PreviewBlockInfo info,
                                                        ClientLevel level,
                                                        List<PreviewBlockInfo> allBlocks) {
        // 1. 当前位置已有方块 → 直接使用（让玩家看到自己放了什么）
        BlockState currentState = level.getBlockState(info.pos());
        if (!currentState.isAir()) {
            return currentState;
        }

        // 2. 从 expectedTypes 查找方块注册名类型
        for (String type : info.expectedTypes()) {
            if (type.indexOf(':') >= 0 && !type.startsWith("tag:")) {
                ResourceLocation id = ResourceLocation.parse(type);
                Block block = BuiltInRegistries.BLOCK.get(id);
                if (block != null && block != Blocks.AIR) {
                    return block.defaultBlockState();
                }
            }
        }

        // 3. 从 expectedTypes 查找 Block Tag 类型
        for (String type : info.expectedTypes()) {
            if (type.startsWith("tag:")) {
                String tagStr = type.substring(4);
                TagKey<Block> tagKey = TagKey.create(Registries.BLOCK,
                        ResourceLocation.parse(tagStr));
                Optional<Block> first = BuiltInRegistries.BLOCK.getTag(tagKey)
                        .stream()
                        .flatMap(tag -> tag.stream().map(Holder::value))
                        .filter(b -> b != Blocks.AIR)
                        .findFirst();
                if (first.isPresent()) {
                    return first.get().defaultBlockState();
                }
            }
        }

        // 4. 对于 part 类型（如 multi_block_part、input_hatch），
        //    扫描全部 BlockEntityType 找到最低 tier 的代表方块模型
        ensurePartTypesScanned();
        for (String type : info.expectedTypes()) {
            BlockState representative = PART_TYPE_REPRESENTATIVES.get(type);
            if (representative != null) {
                return representative;
            }
        }

        // 5. 无法确定方块模型 → 不渲染
        return null;
    }

    // ==================== 状态分类 ====================

    /**
     * 根据世界当前位置的方块状态判断预览状态。
     */
    private static Status classify(BlockPos pos, List<String> expectedTypes,
                                   ClientLevel level, BlockPos controllerPos) {
        // 控制器自身位置按 CORRECT 着色
        if (pos.equals(controllerPos)) {
            return Status.CORRECT;
        }

        BlockState state = level.getBlockState(pos);

        if (state.isAir()) {
            return Status.MISSING;
        }

        for (String expectedType : expectedTypes) {
            if (matchesExpectedType(expectedType, state, level, pos)) {
                return Status.CORRECT;
            }
        }

        return Status.WRONG;
    }

    /**
     * 检查当前方块状态是否匹配给定的预期类型。
     */
    private static boolean matchesExpectedType(String expectedType, BlockState state,
                                               ClientLevel level, BlockPos pos) {
        if (expectedType.indexOf(':') >= 0 && !expectedType.startsWith("tag:")) {
            String blockName = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            return blockName.equals(expectedType);
        }

        if (expectedType.startsWith("tag:")) {
            String tagStr = expectedType.substring(4);
            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK,
                    ResourceLocation.parse(tagStr));
            return state.is(tagKey);
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof IMultiBlockPart part) {
            return part.getPartType().equals(expectedType);
        }

        return false;
    }

    // ==================== 颜色染色 ====================

    /**
     * 获取状态对应的 RGBA 染色值（与 alphaMul 相乘）。
     */
    private static float[] getTint(Status status, float alphaMul) {
        return switch (status) {
            case CORRECT -> new float[]{1.0F, 1.0F, 1.0F, alphaMul};
            case MISSING -> new float[]{0.4F, 0.7F, 1.0F, alphaMul};
            case WRONG   -> new float[]{1.0F, 0.3F, 0.3F, alphaMul};
        };
    }

    // ==================== MultiBufferSource 包装：solid → translucent ====================

    /**
     * 将 solid / cutout 类 RenderType 重定向到 translucent，
     * 确保 {@code renderSingleBlock} 渲染的固体面也能应用透明度。
     */
    private record TranslucentRedirect(MultiBufferSource.BufferSource delegate)
            implements MultiBufferSource {

        @Override
        public VertexConsumer getBuffer(RenderType renderType) {
            if (renderType == RenderType.solid() ||
                    renderType == RenderType.cutout() ||
                    renderType == RenderType.cutoutMipped()) {
                return delegate.getBuffer(RenderType.translucent());
            }
            return delegate.getBuffer(renderType);
        }
    }
}

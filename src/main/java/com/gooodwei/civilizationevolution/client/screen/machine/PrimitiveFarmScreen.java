package com.gooodwei.civilizationevolution.client.screen.machine;

import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveFarmMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 原始农场 GUI 的客户端 Screen。
 *
 * <p>除背景贴图外，还会渲染工作进度条和水位条。
 * 后续可按需添加设置按钮（如最小保留数量等）。
 *
 * <p>TODO：添加水位条渲染（利用 {@link PrimitiveFarmMenu#getWaterAmount()} 获取储水量）。
 * TODO：设计农场专用 GUI 纹理（当前使用牧场占位纹理）。
 */
@OnlyIn(Dist.CLIENT)
public class PrimitiveFarmScreen extends AbstractContainerScreen<PrimitiveFarmMenu> {

    /** 原始农场 GUI 背景贴图（TODO：替换为农场专用纹理，当前为牧场占位副本） */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/container/primitive_farm.png");

    /** 工作进度条贴图（复用通用进度条纹理） */
    private static final ResourceLocation PROGRESS =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/progress/progress.png");

    /** 进度条最大像素宽度 */
    private static final int PROGRESS_WIDTH = 24;

    /**
     * 构造原始农场 Screen。
     *
     * @param menu            服务端对应的 {@link PrimitiveFarmMenu}
     * @param playerInventory 玩家物品栏
     * @param title           界面标题（不渲染）
     */
    public PrimitiveFarmScreen(PrimitiveFarmMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /**
     * 渲染背景层：背景贴图和工作进度条。
     *
     * @param guiGraphics 渲染上下文
     * @param partialTick 当前帧的部分 tick 插值
     * @param mouseX      鼠标 X 坐标（相对于屏幕）
     * @param mouseY      鼠标 Y 坐标（相对于屏幕）
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // 渲染工作进度条
        float ratio = this.menu.getWorkProgressRatio();
        int progressPixels = (int) (ratio * PROGRESS_WIDTH);
        if (progressPixels > 0) {
            guiGraphics.blit(PROGRESS, leftPos + 80, topPos + 35,
                    progressPixels, 16, 0, 0, progressPixels, 16, 24, 16);
        }
        // TODO：渲染水位条（使用 this.menu.getWaterAmount()）
    }

    /**
     * 渲染文字标签层（空实现，后续可在此绘制储水量数字）。
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 不渲染默认文字标签
        // TODO：渲染储水量数字标签
    }

    /**
     * 主渲染入口。
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}

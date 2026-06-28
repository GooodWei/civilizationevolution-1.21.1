package com.gooodwei.civilizationevolution.client.screen;

import com.gooodwei.civilizationevolution.server.menu.PrimitiveRanchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 原始牧场 GUI 的客户端 Screen。
 *
 * <p>除背景贴图外，还会渲染工作进度条。
 * 后续可按需添加设置按钮（如最小保留数量等）。
 */
@OnlyIn(Dist.CLIENT)
public class PrimitiveRanchScreen extends AbstractContainerScreen<PrimitiveRanchMenu> {

    /** 原始牧场 GUI 背景贴图 */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/container/primitive_ranch.png");

    /** 工作进度条贴图 */
    private static final ResourceLocation PROGRESS =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/progress/progress.png");

    /** 进度条最大像素宽度 */
    private static final int PROGRESS_WIDTH = 24;

    /**
     * 构造原始牧场 Screen。
     *
     * @param menu            服务端对应的 {@link PrimitiveRanchMenu}
     * @param playerInventory 玩家物品栏
     * @param title           界面标题（不渲染）
     */
    public PrimitiveRanchScreen(PrimitiveRanchMenu menu, Inventory playerInventory, Component title) {
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
        float ratio = this.menu.getWorkProgressRatio();
        int progressPixels = (int) (ratio * PROGRESS_WIDTH);
        if (progressPixels > 0) {
            guiGraphics.blit(PROGRESS, leftPos + 80, topPos + 35,
                    progressPixels, 16, 0, 0, progressPixels, 16, 24, 16);
        }
    }

    /**
     * 渲染文字标签层（空实现）。
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 不渲染默认文字标签
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

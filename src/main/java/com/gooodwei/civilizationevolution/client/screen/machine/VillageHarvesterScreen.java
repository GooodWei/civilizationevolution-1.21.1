package com.gooodwei.civilizationevolution.client.screen.machine;

import com.gooodwei.civilizationevolution.server.menu.machine.VillageHarvesterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 村庄收割机 GUI 的客户端 Screen。
 *
 * <p>背景贴图暂时使用原始狩猎场的纹理作为占位（TODO：替换为收割机专用纹理）。
 * 与狩猎场不同，收割机<b>没有</b>子 GUI（无最小保留数量设置），仅渲染进度条。
 * </p>
 *
 * <h3>GUI 布局（imageWidth=176, imageHeight=166）</h3>
 * <ul>
 *   <li>槽位 0-5：食物输入槽（2×3）</li>
 *   <li>槽位 6-8：人口输入槽</li>
 *   <li>槽位 9：武器槽</li>
 *   <li>槽位 10-18：产物输出槽（3×3）</li>
 *   <li>进度条：位于 (80, 35)，宽 24px</li>
 * </ul>
 */
@OnlyIn(Dist.CLIENT)
public class VillageHarvesterScreen extends AbstractContainerScreen<VillageHarvesterMenu> {

    /** GUI 背景贴图（TODO：替换为收割机专用纹理，当前为原始狩猎场占位） */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/container/primitive_hunting_ground.png");

    /** 工作进度条贴图 */
    private static final ResourceLocation PROGRESS =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/progress/progress.png");

    /** 进度条最大像素宽度 */
    private static final int PROGRESS_WIDTH = 24;

    /**
     * 构造村庄收割机 Screen。
     *
     * @param menu            服务端对应的 {@link VillageHarvesterMenu}
     * @param playerInventory 玩家物品栏
     * @param title           界面标题（不渲染，由 {@link #renderLabels} 空实现屏蔽）
     */
    public VillageHarvesterScreen(VillageHarvesterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /**
     * 渲染背景层：背景贴图和工作进度条。
     *
     * <p>进度条根据 {@link VillageHarvesterMenu#getWorkProgressRatio()} 返回的比例动态裁剪宽度。
     *
     * @param guiGraphics 渲染上下文
     * @param partialTick 当前帧的部分 tick 插值
     * @param mouseX      鼠标 X 坐标（相对于屏幕）
     * @param mouseY      鼠标 Y 坐标（相对于屏幕）
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 渲染背景贴图
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // 渲染工作进度条
        float ratio = this.menu.getWorkProgressRatio();
        int progressPixels = (int) (ratio * PROGRESS_WIDTH);
        if (progressPixels > 0) {
            guiGraphics.blit(PROGRESS, leftPos + 80, topPos + 35,
                    progressPixels, 16,
                    0, 0, progressPixels, 16,
                    24, 16);
        }
    }

    /**
     * 渲染文字标签层（空实现）。
     *
     * <p>收割机 GUI 不需要原版的容器标题和物品栏名称，故留空。
     *
     * @param graphics 渲染上下文
     * @param mouseX   鼠标 X 坐标（相对于 GUI 左上角）
     * @param mouseY   鼠标 Y 坐标（相对于 GUI 左上角）
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 不渲染任何文字标签（容器标题、物品栏）
    }

    /**
     * 主渲染入口。先调用父类渲染背景和槽位，再渲染工具提示。
     *
     * @param graphics    渲染上下文
     * @param mouseX      鼠标 X 坐标（相对于屏幕）
     * @param mouseY      鼠标 Y 坐标（相对于屏幕）
     * @param partialTick 当前帧的部分 tick 插值
     */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}

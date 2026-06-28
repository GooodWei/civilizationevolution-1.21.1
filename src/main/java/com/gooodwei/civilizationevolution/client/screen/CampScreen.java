package com.gooodwei.civilizationevolution.client.screen;

import com.gooodwei.civilizationevolution.server.menu.CampMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 营地 GUI 的客户端 Screen。
 *
 * <p>负责渲染营地容器的背景贴图。营地 GUI 不使用默认的文字标签（标题、物品栏名称），
 * 因此覆写了 {@link #renderLabels} 为空实现。</p>
 */
public class CampScreen extends AbstractContainerScreen<CampMenu> {

    /** 营地 GUI 背景贴图纹理路径 */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "textures/gui/container/campblock.png");

    /**
     * 构造营地 Screen。
     *
     * @param menu            服务端对应的 {@link CampMenu}
     * @param playerInventory 玩家物品栏
     * @param title           界面标题（不渲染，由 {@link #renderLabels} 空实现屏蔽）
     */
    public CampScreen(CampMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    /**
     * 渲染 GUI 背景贴图。
     *
     * <p>将营地 PNG 纹理绘制到屏幕对应位置，纹理尺寸由 {@link #imageWidth} 和 {@link #imageHeight} 决定。</p>
     *
     * @param graphics    渲染上下文
     * @param partialTick 当前帧的部分 tick 插值
     * @param mouseX      鼠标 X 坐标（相对于屏幕）
     * @param mouseY      鼠标 Y 坐标（相对于屏幕）
     */
    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    /**
     * 渲染文字标签层（空实现）。
     *
     * <p>营地 GUI 不需要原版的容器标题和物品栏名称，故留空。</p>
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

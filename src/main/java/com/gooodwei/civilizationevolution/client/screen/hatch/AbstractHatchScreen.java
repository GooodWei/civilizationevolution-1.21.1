package com.gooodwei.civilizationevolution.client.screen.hatch;

import com.gooodwei.civilizationevolution.server.menu.hatch.AbstractHatchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 所有 hatch（多方块接口）GUI 界面的抽象基类。
 *
 * <p>提供 176×166 标准容器界面尺寸、背景渲染和工具提示。
 * 具体 hatch 只需覆写 {@link #getGuiTexture()} 返回对应 GUI 贴图。
 *
 * @param <T> 对应的 hatch 菜单类型
 */
public abstract class AbstractHatchScreen<T extends AbstractHatchMenu> extends AbstractContainerScreen<T> {

    /** GUI 标题颜色（原版默认暗灰色） */
    public static final int TITLE_COLOR = 0x404040;

    protected AbstractHatchScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /** 返回此 hatch 类型对应的 GUI 背景纹理 */
    protected abstract ResourceLocation getGuiTexture();

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(getGuiTexture(), leftPos, topPos, 0, 0, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 不渲染默认文字标签
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}

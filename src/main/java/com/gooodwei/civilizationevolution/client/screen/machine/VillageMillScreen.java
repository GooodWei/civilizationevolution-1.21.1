package com.gooodwei.civilizationevolution.client.screen.machine;

import com.gooodwei.civilizationevolution.server.menu.machine.VillageMillMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * 村庄磨坊 GUI 的客户端 Screen。
 *
 * <p>渲染 4 个石匠人口槽位 + 工作进度条。
 * 背景贴图暂用 village_quarry.png 占位。
 */
@OnlyIn(Dist.CLIENT)
public class VillageMillScreen extends AbstractContainerScreen<VillageMillMenu> {

    /** 磨坊 GUI 背景贴图（占位纹理，后续替换美术资源） */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/container/village_mill.png");

    /** 工作进度条贴图 */
    private static final ResourceLocation PROGRESS =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/progress/progress.png");

    /** 进度条最大像素宽度 */
    private static final int PROGRESS_WIDTH = 24;

    public VillageMillScreen(VillageMillMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        float ratio = this.menu.getWorkProgressRatio();
        int progressPixels = (int) (ratio * PROGRESS_WIDTH);
        if (progressPixels > 0) {
            guiGraphics.blit(PROGRESS, leftPos + 76, topPos + 46,
                    progressPixels, 16, 0, 0, progressPixels, 16, 24, 16);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 不渲染文字标签
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}

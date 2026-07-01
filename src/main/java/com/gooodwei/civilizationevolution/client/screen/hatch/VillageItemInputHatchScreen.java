package com.gooodwei.civilizationevolution.client.screen.hatch;

import com.gooodwei.civilizationevolution.server.menu.hatch.VillageItemInputHatchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 村庄物品输入接口 GUI（27 槽潜影盒布局）。
 *
 * <p>复用原版潜影盒纹理，3×9 槽位网格。
 */
public class VillageItemInputHatchScreen extends AbstractHatchScreen<VillageItemInputHatchMenu> {

    /** 原版潜影盒纹理 */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/gui/container/shulker_box.png");

    public VillageItemInputHatchScreen(VillageItemInputHatchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 167;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return TEXTURE;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 仅在顶部绘制标题，不绘制"物品栏"标签（潜影盒纹理自带）
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }
}

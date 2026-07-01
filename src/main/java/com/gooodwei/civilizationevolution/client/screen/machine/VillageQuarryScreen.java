package com.gooodwei.civilizationevolution.client.screen.machine;

import com.gooodwei.civilizationevolution.client.screen.IntegerInputScreen;
import com.gooodwei.civilizationevolution.network.UpdateMachineFieldPayload;
import com.gooodwei.civilizationevolution.server.blockentity.multiblock.VillageQuarryBlockEntity;
import com.gooodwei.civilizationevolution.server.menu.machine.VillageQuarryMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 村庄采石场 GUI 的客户端 Screen。
 *
 * <p>复用狩猎场 GUI 布局，武器槽改为镐槽。
 * 除背景贴图外，还会渲染工作进度条和一个输入按钮（预留）。
 */
@OnlyIn(Dist.CLIENT)
public class VillageQuarryScreen extends AbstractContainerScreen<VillageQuarryMenu> {

    /** 采石场 GUI 背景贴图（占位纹理，后续替换美术资源） */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "textures/gui/container/village_quarry.png");

    /** 工作进度条贴图 */
    private static final ResourceLocation PROGRESS =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "textures/gui/progress/progress.png");

    /** 输入按钮贴图 */
    private static final ResourceLocation INPUTBUTTON =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "textures/gui/button/input_button.png");

    /** 进度条最大像素宽度 */
    private static final int PROGRESS_WIDTH = 24;

    public VillageQuarryScreen(VillageQuarryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        float ratio = this.menu.getWorkProgressRatio();
        int progressPixels = (int) (ratio * PROGRESS_WIDTH);
        if (progressPixels > 0) {
            guiGraphics.blit(PROGRESS, leftPos + 80, topPos + 35, progressPixels, 16, 0, 0, progressPixels, 16, 24, 16);
        }
        guiGraphics.blit(INPUTBUTTON,
                leftPos + 80, topPos + 56,
                0, 0,
                16, 16,
                16, 16);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHoveringButton(mouseX, mouseY)) {
            Minecraft.getInstance().setScreen(new IntegerInputScreen(
                    Component.translatable("gui.civilizationevolution.set_min_keep"),
                    this,
                    this.menu.getMinKeepNumber(),
                    value -> {
                        CompoundTag data = new CompoundTag();
                        data.putInt("v", value);
                        PacketDistributor.sendToServer(new UpdateMachineFieldPayload(
                                this.menu.getBlockPos(),
                                VillageQuarryBlockEntity.FIELD_MIN_KEEP_NUMBER,
                                data));
                    }
            ));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean isHoveringButton(double mouseX, double mouseY) {
        int x = leftPos + 80;
        int y = topPos + 56;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 不渲染任何文字标签
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}

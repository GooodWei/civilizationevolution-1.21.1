package com.gooodwei.civilizationevolution.client.screen.machine;
import com.gooodwei.civilizationevolution.client.screen.IntegerInputScreen;


import com.gooodwei.civilizationevolution.network.UpdateMachineFieldPayload;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveDoctorCabinMenu;
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
 * 原始诊所 GUI 的客户端 Screen。
 *
 * <p>渲染背景贴图、工作进度条和健康阈值输入按钮。
 * 点击输入按钮打开 {@link IntegerInputScreen}，允许玩家设置健康阈值（0-100），
 * 确认后通过 {@link UpdateMachineFieldPayload} 将值发送到服务端。</p>
 *
 * <p>TODO：当前 GUI 背景纹理是原始牧场（primitive_ranch.png）的副本占位，
 * 需要设计诊所专用 GUI 纹理。详见 {@code dontpush/gui_design_tasks.md}。</p>
 */
@OnlyIn(Dist.CLIENT)
public class PrimitiveDoctorCabinScreen extends AbstractContainerScreen<PrimitiveDoctorCabinMenu> {

    /** 原始诊所 GUI 背景贴图（TODO：替换为诊所专用纹理，当前为牧场占位副本） */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "textures/gui/container/primitive_doctor_cabin.png");

    /** 工作进度条贴图 */
    private static final ResourceLocation PROGRESS =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "textures/gui/progress/progress.png");

    /** 输入按钮贴图（设置健康阈值） */
    private static final ResourceLocation INPUTBUTTON =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "textures/gui/button/input_button.png");

    /** 进度条最大像素宽度 */
    private static final int PROGRESS_WIDTH = 24;

    /** 健康阈值字段 ID（与 BE 中一致） */
    public static final int FIELD_HEALTH_THRESHOLD = 0;

    /**
     * 构造原始诊所 Screen。
     *
     * @param menu            服务端对应的 {@link PrimitiveDoctorCabinMenu}
     * @param playerInventory 玩家物品栏
     * @param title           界面标题
     */
    public PrimitiveDoctorCabinScreen(PrimitiveDoctorCabinMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /**
     * 渲染背景层：背景贴图、工作进度条和输入按钮。
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // 工作进度条
        float ratio = this.menu.getWorkProgressRatio();
        int progressPixels = (int) (ratio * PROGRESS_WIDTH);
        if (progressPixels > 0) {
            guiGraphics.blit(PROGRESS, leftPos + 80, topPos + 35,
                    progressPixels, 16,
                    0, 0, progressPixels, 16, 24, 16);
        }

        // 健康阈值输入按钮
        guiGraphics.blit(INPUTBUTTON,
                leftPos + 80, topPos + 56,
                0, 0,
                16, 16,
                16, 16);
    }

    /**
     * 处理鼠标点击：若点击输入按钮则打开健康阈值设定子 GUI。
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isHoveringButton(mouseX, mouseY)) {
            Minecraft.getInstance().setScreen(new IntegerInputScreen(
                    Component.translatable("gui.civilizationevolution.set_health_threshold"),
                    this,
                    this.menu.getHealthThreshold(),
                    value -> {
                        CompoundTag data = new CompoundTag();
                        data.putInt("value", value);
                        PacketDistributor.sendToServer(new UpdateMachineFieldPayload(
                                this.menu.getBlockEntity().getBlockPos(),
                                FIELD_HEALTH_THRESHOLD,
                                data));
                    }
            ));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 判断鼠标是否悬停在输入按钮上。
     */
    private boolean isHoveringButton(double mouseX, double mouseY) {
        int x = leftPos + 80;
        int y = topPos + 56;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    /**
     * 渲染文字标签（空实现，不显示默认标题）。
     */
    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 不渲染任何文字标签
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

package com.gooodwei.civilizationevolution.client.screen;

import com.gooodwei.civilizationevolution.network.UpdateMachineFieldPayload;
import com.gooodwei.civilizationevolution.server.blockentity.fieldmachine.HuntingGroundBlockEntity;
import com.gooodwei.civilizationevolution.server.menu.HuntingGroundMenu;
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
 * 狩猎场 GUI 的客户端 Screen。
 *
 * <p>除背景贴图外，还会渲染工作进度条和一个输入按钮。
 * 点击输入按钮会打开 {@link IntegerInputScreen}，允许玩家设置最小保留数量，
 * 确认后通过 {@link UpdateMachineFieldPayload} 将值发送到服务端。</p>
 */
@OnlyIn(Dist.CLIENT)
public class HuntingGroundScreen extends AbstractContainerScreen<HuntingGroundMenu> {

    /** 狩猎场 GUI 背景贴图 */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "textures/gui/container/hunting_ground.png");

    /** 工作进度条贴图 */
    private static final ResourceLocation PROGRESS =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "textures/gui/progress/progress.png");

    /** 输入按钮贴图（设置最小保留数量） */
    private static final ResourceLocation INPUTBUTTON =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution", "textures/gui/button/input_button.png");

    /** 进度条最大像素宽度 */
    private static final int PROGRESS_WIDTH = 24;

    /**
     * 构造狩猎场 Screen。
     *
     * @param menu            服务端对应的 {@link HuntingGroundMenu}
     * @param playerInventory 玩家物品栏
     * @param title           界面标题（不渲染，由 {@link #renderLabels} 空实现屏蔽）
     */
    public HuntingGroundScreen(HuntingGroundMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    /**
     * 渲染背景层：背景贴图、工作进度条和输入按钮。
     *
     * <p>进度条根据 {@link HuntingGroundMenu#getWorkProgressRatio()} 返回的比例动态裁剪宽度。</p>
     *
     * @param guiGraphics 渲染上下文
     * @param v           当前帧的部分 tick 插值
     * @param mouseX      鼠标 X 坐标（相对于屏幕）
     * @param mouseY      鼠标 Y 坐标（相对于屏幕）
     */
    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        float ratio = this.menu.getWorkProgressRatio();
        int progressPixels = (int) (ratio * PROGRESS_WIDTH);
        if (progressPixels > 0) {
            guiGraphics.blit(PROGRESS, leftPos + 80, topPos + 35, progressPixels, 16, 0, 0, progressPixels, 16, 24, 16);
        }
        boolean hover = isHoveringButton(mouseX, mouseY);
        guiGraphics.blit(INPUTBUTTON,
                        leftPos + 80, topPos + 56,
                        0, 0,
                        16, 16,
                        16, 16
                        );

    }

    /**
     * 处理鼠标点击事件。
     *
     * <p>若左键点击输入按钮区域，则打开 {@link IntegerInputScreen} 让玩家设置最小保留数量，
     * 确认后通过 {@link UpdateMachineFieldPayload} 将值发送至服务端。</p>
     *
     * @param mouseX 鼠标 X 坐标（相对于屏幕）
     * @param mouseY 鼠标 Y 坐标（相对于屏幕）
     * @param button 鼠标按键（0=左键）
     * @return 若点击了输入按钮则返回 {@code true}，否则委托父类处理
     */
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
                                HuntingGroundBlockEntity.FIELD_MIN_KEEP_NUMBER,
                                data));
                    }
            ));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * 判断鼠标坐标是否落在输入按钮区域（16×16 像素）内。
     *
     * @param mouseX 鼠标 X 坐标（相对于屏幕）
     * @param mouseY 鼠标 Y 坐标（相对于屏幕）
     * @return 鼠标悬停在按钮区域内返回 {@code true}
     */
    private boolean isHoveringButton(double mouseX, double mouseY) {
        int x = leftPos + 80;
        int y = topPos + 56;
        return mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16;
    }

    /**
     * 渲染文字标签层（空实现）。
     *
     * <p>狩猎场 GUI 不需要原版的容器标题和物品栏名称，故留空。</p>
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

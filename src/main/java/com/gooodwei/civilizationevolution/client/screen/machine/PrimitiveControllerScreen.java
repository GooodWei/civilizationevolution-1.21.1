package com.gooodwei.civilizationevolution.client.screen.machine;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.client.ClientPayloadHandler;
import com.gooodwei.civilizationevolution.client.renderer.HighlightRenderer;
import com.gooodwei.civilizationevolution.network.SyncMachineListPayload;
import com.gooodwei.civilizationevolution.network.UpdateMachineFieldPayload;
import com.gooodwei.civilizationevolution.server.blockentity.controller.AbstractControllerBlockEntity;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class PrimitiveControllerScreen extends AbstractContainerScreen<PrimitiveControllerMenu> {

    /** 纹理文件 256×256 */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(CivilizationEvolution.MODID,
                    "textures/gui/container/primitive_controller.png");

    // ==================== 进度条 ====================
    private static final int PROGRESSBAR_X = 31;
    private static final int PROGRESSBAR_Y = 91;
    private static final int PROGRESSBAR_LENGTH = 95;
    private static final int PROGRESSBAR_HEIGHT = 3;
    private static final int BAR_X = 0;
    private static final int BAR_Y = 242;

    // ==================== 右侧面板 ====================
    private static final int RIGHT_PANEL_X = 32;
    private static final int RIGHT_PANEL_Y = 8;
    private static final int RIGHT_PANEL_WIDTH = 124;
    private static final int RIGHT_PANEL_HEIGHT = 65;

    // ==================== 滚动条 ====================
    private static final int SCROLL_BAR_X = 152;
    private static final int SCROLL_BAR_Y = 8;
    private static final int SCROLL_BAR_WIDTH = 9;
    private static final int SCROLL_BAR_HEIGHT = 65;
    private static final int SCROLL_THUMB_X = 125;
    private static final int SCROLL_THUMB_Y = 196;
    private static final int SCROLL_THUMB_DARK_X = 135;
    private static final int SCROLL_THUMB_DARK_Y = 196;
    private static final int SCROLL_THUMB_WIDTH = 9;
    private static final int SCROLL_THUMB_HEIGHT = 16;

    // ==================== 每行 ====================
    private static final int ROW_X = 0;
    private static final int ROW_Y = 196;
    private static final int ROW_WIDTH = 124;
    private static final int ROW_HEIGHT = 21;
    private static final int ROW_DARK_X = 0;
    private static final int ROW_DARK_Y = 218;

    // ==================== 按钮尺寸 ====================
    /** 按钮区域在每行中的布局常量 */
    private static final int BTN_WIDTH = 14;
    private static final int BTN_HEIGHT = 10;
    private static final int BTN_ROW_START_X = 5;
    private static final int BTN_ROW_Y_OFFSET = 6;

    /** 按钮 UV 位置（通用小按钮纹理，定义在纹理 256×256 中）*/
    private static final int BTN_UNBIND_U = 0;
    private static final int BTN_UNBIND_V = 248;
    private static final int BTN_START_U = 16;
    private static final int BTN_START_V = 248;
    private static final int BTN_STOP_U = 32;
    private static final int BTN_STOP_V = 248;
    private static final int BTN_HIGHLIGHT_U = 48;
    private static final int BTN_HIGHLIGHT_V = 248;

    /** 可见行数 */
    private static final int VISIBLE_ROWS = 3;

    /** 强制使用 Unicode 矢量字体的 Style，0.5 倍缩放时仍然锐利 */
    private static final Style UNIFORM_STYLE = Style.EMPTY
            .withFont(ResourceLocation.withDefaultNamespace("uniform"));

    // ==================== 运行时状态 ====================

    /** 从服务端同步的机器列表 */
    private List<SyncMachineListPayload.MachineEntry> machines = List.of();
    /** 滚动偏移（0 为顶部） */
    private int scrollOffset;

    public PrimitiveControllerScreen(PrimitiveControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 196;
        // 从缓存加载机器列表（处理 SyncMachineListPayload 先于 Screen 到达的时序问题）
        this.machines = ClientPayloadHandler.getCachedMachineList();
    }

    // ==================== 公开 API（供 ClientPayloadHandler 调用） ====================

    /**
     * 接收服务端推送的机器列表，刷新右侧面板。
     */
    public void updateMachineList(List<SyncMachineListPayload.MachineEntry> list) {
        this.machines = list != null ? list : List.of();
        // 滚动位置裁剪：列表变短时回弹
        int maxOffset = Math.max(0, this.machines.size() - VISIBLE_ROWS);
        if (scrollOffset > maxOffset) {
            scrollOffset = maxOffset;
        }
    }

    // ==================== 每 tick ====================

    @Override
    public void containerTick() {
        super.containerTick();
    }

    // ==================== 渲染 ====================

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // 主背景
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight, 256, 256);

        // 进度条
        float ratio = this.menu.getWorkProgressRatio();
        int progressPixels = (int) (ratio * PROGRESSBAR_LENGTH);
        if (progressPixels > 0) {
            guiGraphics.blit(TEXTURE, leftPos + PROGRESSBAR_X, topPos + PROGRESSBAR_Y,
                    BAR_X, BAR_Y, progressPixels, PROGRESSBAR_HEIGHT, 256, 256);
        }

        // 右侧面板：渲染机器列表
        renderMachineList(guiGraphics, mouseX, mouseY);

        // 滚动条
        renderScrollBar(guiGraphics, mouseX, mouseY);
    }

    /**
     * 渲染右侧面板中的机器列表行。
     */
    private void renderMachineList(GuiGraphics graphics, int mouseX, int mouseY) {
        int panelLeft = leftPos + RIGHT_PANEL_X;
        int panelTop = topPos + RIGHT_PANEL_Y;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int machineIndex = scrollOffset + i;
            if (machineIndex >= machines.size()) break;

            SyncMachineListPayload.MachineEntry entry = machines.get(machineIndex);
            int rowY = panelTop + i * ROW_HEIGHT;

            // 行背景（悬停时深色变体）
            boolean isRowHovered = mouseX >= panelLeft && mouseX < panelLeft + ROW_WIDTH
                    && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;
            int rowV = isRowHovered ? ROW_DARK_Y : ROW_Y;
            graphics.blit(TEXTURE, panelLeft, rowY, ROW_X, rowV,
                    ROW_WIDTH, ROW_HEIGHT, 256, 256);

            // 第一行：机器名称（强制 Unicode 矢量字体，中文不再显示为方块字）
            String displayText = entry.machineName();
            int textColor = entry.enabled() ? 0xCCCCCC : 0x888888;
            graphics.drawString(this.font,
                    Component.literal(displayText).withStyle(UNIFORM_STYLE).getVisualOrderText(),
                    panelLeft + BTN_ROW_START_X, rowY + 1, textColor);

            // 第二行：坐标（0.5 倍缩放，强制 Unicode 矢量字体，缩放后依然锐利）
            int subColor = entry.enabled() ? 0x5599FF : 0x334466;
            String coordText = String.format("(%d, %d, %d)",
                    entry.pos().getX(), entry.pos().getY(), entry.pos().getZ());
            int tx = panelLeft + BTN_ROW_START_X + 4;
            int tyCoord = rowY + 11;
            graphics.pose().pushPose();
            graphics.pose().translate(tx, tyCoord, 0);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.drawString(this.font,
                    Component.literal(coordText).withStyle(UNIFORM_STYLE).getVisualOrderText(),
                    0, 0, subColor);
            graphics.pose().popPose();

            // 第三行：维度（0.5 倍缩放，强制 Unicode 矢量字体）
            int tyDim = rowY + 16;
            graphics.pose().pushPose();
            graphics.pose().translate(tx, tyDim, 0);
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            graphics.drawString(this.font,
                    Component.literal(entry.dimension()).withStyle(UNIFORM_STYLE).getVisualOrderText(),
                    0, 0, subColor);
            graphics.pose().popPose();

            // 状态指示器（绿色=启用，灰色=停止）
            int statusColor = entry.enabled() ? 0xFF00FF00 : 0xFF888888;
            graphics.fill(panelLeft + ROW_WIDTH - 60, rowY + 4,
                    panelLeft + ROW_WIDTH - 54, rowY + 10, statusColor);

            // 按钮
            renderRowButtons(graphics, entry, panelLeft, rowY, mouseX, mouseY, isRowHovered);
        }
    }

    /**
     * 渲染单行的三个操作按钮（启动/停止、解绑、高亮）。
     */
    private void renderRowButtons(GuiGraphics graphics, SyncMachineListPayload.MachineEntry entry,
                                   int panelLeft, int rowY, int mouseX, int mouseY,
                                   boolean isRowHovered) {
        int btnBaseX = panelLeft + ROW_WIDTH - 60;
        int btnY = rowY + BTN_ROW_Y_OFFSET;

        // 按钮布局：[启动/停止] [解绑] [高亮]
        drawSmallButton(graphics, btnBaseX, btnY,
                entry.enabled() ? Component.translatable("gui.civilizationevolution.stop").getString()
                        : Component.translatable("gui.civilizationevolution.start").getString(),
                mouseX, mouseY);
        drawSmallButton(graphics, btnBaseX + 18, btnY,
                Component.translatable("gui.civilizationevolution.unbind").getString(),
                mouseX, mouseY);
        drawSmallButton(graphics, btnBaseX + 36, btnY,
                Component.translatable("gui.civilizationevolution.highlight").getString(),
                mouseX, mouseY);
    }

    /**
     * 绘制一个小文字按钮，使用 0.7 倍缩放 + 矢量字体，悬停时变色。
     */
    private void drawSmallButton(GuiGraphics graphics, int x, int y, String text,
                                  int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX < x + 14 && mouseY >= y && mouseY < y + 10;
        int bg = hover ? 0xFF555555 : 0xFF333333;
        graphics.fill(x, y, x + 14, y + 10, bg);

        graphics.pose().pushPose();
        graphics.pose().translate(x + 1, y + 1, 0);
        graphics.pose().scale(0.7F, 0.7F, 1.0F);
        graphics.drawString(this.font,
                Component.literal(text).withStyle(UNIFORM_STYLE).getVisualOrderText(),
                0, 0, hover ? 0xFFFF55 : 0xCCCCCC);
        graphics.pose().popPose();
    }

    /**
     * 渲染滚动条滑块。
     */
    private void renderScrollBar(GuiGraphics graphics, int mouseX, int mouseY) {
        int barLeft = leftPos + SCROLL_BAR_X;
        int barTop = topPos + SCROLL_BAR_Y;

        // 滑块仅在列表超过可见行时渲染
        if (machines.size() <= VISIBLE_ROWS) return;

        boolean isHovered = mouseX >= barLeft && mouseX < barLeft + SCROLL_BAR_WIDTH
                && mouseY >= barTop && mouseY < barTop + SCROLL_BAR_HEIGHT;

        int maxOffset = machines.size() - VISIBLE_ROWS;
        float scrollRatio = (float) scrollOffset / maxOffset;
        int thumbTravel = SCROLL_BAR_HEIGHT - SCROLL_THUMB_HEIGHT;
        int thumbY = barTop + (int) (scrollRatio * thumbTravel);

        int thumbU = isHovered ? SCROLL_THUMB_DARK_X : SCROLL_THUMB_X;
        graphics.blit(TEXTURE,
                barLeft + 6, thumbY,
                thumbU, SCROLL_THUMB_Y,
                SCROLL_THUMB_WIDTH, SCROLL_THUMB_HEIGHT,
                256, 256);
    }

    // ==================== 交互 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);

        int panelLeft = leftPos + RIGHT_PANEL_X;
        int panelTop = topPos + RIGHT_PANEL_Y;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            int machineIndex = scrollOffset + i;
            if (machineIndex >= machines.size()) break;

            SyncMachineListPayload.MachineEntry entry = machines.get(machineIndex);
            int rowY = panelTop + i * ROW_HEIGHT;

            // 检查点击是否在本行内
            if (mouseX < panelLeft || mouseX >= panelLeft + ROW_WIDTH
                    || mouseY < rowY || mouseY >= rowY + ROW_HEIGHT) {
                continue;
            }

            int btnBaseX = panelLeft + ROW_WIDTH - 60;
            int btnY = rowY + BTN_ROW_Y_OFFSET;
            BlockPos pos = entry.pos();

            // 启动/停止按钮
            if (isInButton(mouseX, mouseY, btnBaseX, btnY)) {
                sendEnablePacket(pos, !entry.enabled());
                return true;
            }
            // 解绑按钮
            if (isInButton(mouseX, mouseY, btnBaseX + 18, btnY)) {
                sendUnbindPacket(pos);
                return true;
            }
            // 高亮按钮
            if (isInButton(mouseX, mouseY, btnBaseX + 36, btnY)) {
                startHighlight(pos);
                return true;
            }
        }

        // 检查是否点击了滚动条轨道
        if (mouseX >= leftPos + SCROLL_BAR_X && mouseX < leftPos + SCROLL_BAR_X + SCROLL_BAR_WIDTH
                && mouseY >= topPos + SCROLL_BAR_Y && mouseY < topPos + SCROLL_BAR_Y + SCROLL_BAR_HEIGHT) {
            handleScrollBarClick(mouseY);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (machines.size() > VISIBLE_ROWS) {
            scrollOffset -= (int) Math.signum(scrollY);
            int maxOffset = Math.max(0, machines.size() - VISIBLE_ROWS);
            scrollOffset = Math.clamp(scrollOffset, 0, maxOffset);
        }
        return true;
    }

    // ==================== 按钮判断 / 网络发包 ====================

    private boolean isInButton(double mouseX, double mouseY, int btnX, int btnY) {
        return mouseX >= btnX && mouseX < btnX + 14 && mouseY >= btnY && mouseY < btnY + 10;
    }

    /**
     * 发送解绑包：Payload 的 pos 是原始控制器 BE 的位置，目标机器坐标在 data 中。
     */
    private void sendUnbindPacket(BlockPos targetPos) {
        CompoundTag data = new CompoundTag();
        data.putLong("pos", targetPos.asLong());
        // Payload 的 pos 必须是原始控制器的位置，NetworkHandler 据此查找 BE
        PacketDistributor.sendToServer(new UpdateMachineFieldPayload(
                this.menu.getBlockPos(),
                AbstractControllerBlockEntity.FIELD_UNBIND_MACHINE, data));
    }

    /**
     * 发送启停包：Payload 的 pos 是原始控制器 BE 的位置，目标机器坐标在 data 中。
     */
    private void sendEnablePacket(BlockPos targetPos, boolean enabled) {
        CompoundTag data = new CompoundTag();
        data.putLong("pos", targetPos.asLong());
        data.putBoolean("enabled", enabled);
        PacketDistributor.sendToServer(new UpdateMachineFieldPayload(
                this.menu.getBlockPos(),
                AbstractControllerBlockEntity.FIELD_SET_ENABLED, data));
    }

    // ==================== 高亮 ====================

    /**
     * 开始高亮目标机器（委托给 {@link HighlightRenderer}，由它独立维护 10 秒的
     * 粒子生成和可透视方块的金色线框，不依赖 GUI 开关状态）。
     */
    private void startHighlight(BlockPos pos) {
        HighlightRenderer.setTarget(pos);
    }

    // ==================== 滚动条点击处理 ====================

    private void handleScrollBarClick(double mouseY) {
        int barTop = topPos + SCROLL_BAR_Y;
        int maxOffset = machines.size() - VISIBLE_ROWS;
        if (maxOffset <= 0) return;

        float clickRatio = (float) (mouseY - barTop) / SCROLL_BAR_HEIGHT;
        clickRatio = Math.clamp(clickRatio, 0.0F, 1.0F);
        scrollOffset = Math.round(clickRatio * maxOffset);
    }

    // ==================== 标准覆写 ====================

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 不渲染默认文字标签
    }
}

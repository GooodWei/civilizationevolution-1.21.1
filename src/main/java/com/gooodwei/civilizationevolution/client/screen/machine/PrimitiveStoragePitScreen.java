package com.gooodwei.civilizationevolution.client.screen.machine;

import com.gooodwei.civilizationevolution.api.util.CountAbbreviator;
import com.gooodwei.civilizationevolution.client.screen.hatch.AbstractHatchScreen;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveStoragePitMenu;
import com.gooodwei.civilizationevolution.server.menu.slot.OversizedSlot;
import com.gooodwei.civilizationevolution.network.ScrollStoragePitPayload;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 储物坑 GUI 客户端 Screen（照搬 AE2 合成终端布局）。
 *
 * <p>滚动条交互完整实现：
 * <ul>
 *   <li>鼠标滚轮 → 上下滚动 1 行</li>
 *   <li>拖拽滑块 → 连续滚动</li>
 *   <li>点击轨道空白处 → 翻页（上下 4 行）</li>
 * </ul>
 * 滚动偏移通过 {@link ScrollStoragePitPayload} 同步到服务端，
 * 保证两端槽位索引一致。
 */
public class PrimitiveStoragePitScreen extends AbstractContainerScreen<PrimitiveStoragePitMenu> {
    /** 背景贴图 */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/container/primitive_storage_pit.png");
    /** 滚动条滑块贴图（12×15） */
    private static final ResourceLocation SCROLLBAR_HANDLE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/sprites/big_scroller.png");

    // AE2 背景贴图分段尺寸
    private static final int HEADER_H = 17;
    private static final int FIRST_ROW_H = 18;
    private static final int ROW_H = 18;
    private static final int LAST_ROW_H = 18;
    private static final int BOTTOM_H = 180;
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    // 滑块尺寸
    private static final int HANDLE_W = 12;
    private static final int HANDLE_H = 15;

    // AE2 widget 位置
    private static final int SEARCH_X = 80;
    private static final int SEARCH_Y = 4;
    private static final int SEARCH_W = 89;
    private static final int SEARCH_H = 12;
    private static final int SCROLLBAR_X = 175;
    private static final int SCROLLBAR_Y = 18;

    private EditBox searchBox;
    private int scrollOffset = 0;
    /** 滚动条拖拽状态 */
    private boolean scrollbarDragging = false;
    /** 拖拽时鼠标 Y 相对滑块顶部的偏移 */
    private int scrollbarDragOffset = 0;

    public PrimitiveStoragePitScreen(PrimitiveStoragePitMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.imageWidth = PrimitiveStoragePitMenu.IMAGE_WIDTH;
        this.imageHeight = PrimitiveStoragePitMenu.IMAGE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.searchBox = new EditBox(this.font,
                leftPos + SEARCH_X, topPos + SEARCH_Y, SEARCH_W, SEARCH_H,
                Component.translatable("gui.civilizationevolution.search"));
        this.searchBox.setBordered(false);
        this.searchBox.setMaxLength(50);
        this.searchBox.setHint(Component.translatable("gui.civilizationevolution.search_hint"));
        this.addRenderableWidget(searchBox);
    }

    // ==================== 背景渲染 ====================

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;

        // AE2 背景贴图 6 段渲染
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, HEADER_H, TEX_W, TEX_H);
        y += HEADER_H;
        graphics.blit(TEXTURE, x, y, 0, HEADER_H, imageWidth, FIRST_ROW_H, TEX_W, TEX_H);
        y += FIRST_ROW_H;
        int rowSrcY = HEADER_H + FIRST_ROW_H;
        for (int i = 0; i < 2; i++) {
            graphics.blit(TEXTURE, x, y, 0, rowSrcY, imageWidth, ROW_H, TEX_W, TEX_H);
            y += ROW_H;
        }
        int lastRowSrcY = rowSrcY + ROW_H;
        graphics.blit(TEXTURE, x, y, 0, lastRowSrcY, imageWidth, LAST_ROW_H, TEX_W, TEX_H);
        y += LAST_ROW_H;
        int bottomSrcY = lastRowSrcY + LAST_ROW_H;
        graphics.blit(TEXTURE, x, y, 0, bottomSrcY, imageWidth, BOTTOM_H, TEX_W, TEX_H);

        renderScrollbar(graphics);
    }

    // ==================== 滚动条渲染与交互 ====================

    private void renderScrollbar(GuiGraphics graphics) {
        int maxScroll = getMaxScroll();
        if (maxScroll == 0) return;

        int trackHeight = PrimitiveStoragePitMenu.getVisibleRows() * ROW_H - 2;
        int scrollX = leftPos + SCROLLBAR_X;
        int scrollY = topPos + SCROLLBAR_Y;

        float ratio = (float) scrollOffset / maxScroll;
        int handleY = scrollY + (int) (ratio * (trackHeight - HANDLE_H));

        graphics.blit(SCROLLBAR_HANDLE, scrollX, handleY,
                0, 0, HANDLE_W, HANDLE_H, HANDLE_W, HANDLE_H);
    }

    /** 判断鼠标是否在搜索框区域内 */
    private boolean isMouseOverSearchBox(double mouseX, double mouseY) {
        int sx = leftPos + SEARCH_X;
        int sy = topPos + SEARCH_Y;
        return mouseX >= sx && mouseX <= sx + SEARCH_W
                && mouseY >= sy && mouseY <= sy + SEARCH_H;
    }

    /** 判断鼠标是否在滚动条轨道区域内 */
    private boolean isMouseOverScrollbar(double mouseX, double mouseY) {
        int maxScroll = getMaxScroll();
        if (maxScroll == 0) return false;
        int trackHeight = PrimitiveStoragePitMenu.getVisibleRows() * ROW_H - 2;
        int sx = leftPos + SCROLLBAR_X;
        int sy = topPos + SCROLLBAR_Y;
        return mouseX >= sx && mouseX <= sx + HANDLE_W
                && mouseY >= sy && mouseY <= sy + trackHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && isMouseOverScrollbar(mouseX, mouseY)) {
            int maxScroll = getMaxScroll();
            int trackHeight = PrimitiveStoragePitMenu.getVisibleRows() * ROW_H - 2;
            int scrollY = topPos + SCROLLBAR_Y;
            float ratio = (float) scrollOffset / Math.max(1, maxScroll);
            int handleY = scrollY + (int) (ratio * (trackHeight - HANDLE_H));

            if (mouseY < handleY) {
                // 点击滑块上方 → 向上翻一页
                setScrollOffset(Math.max(0, scrollOffset - PrimitiveStoragePitMenu.getVisibleRows()));
            } else if (mouseY <= handleY + HANDLE_H) {
                // 点击滑块本身 → 开始拖拽
                scrollbarDragging = true;
                scrollbarDragOffset = (int) mouseY - handleY;
            } else {
                // 点击滑块下方 → 向下翻一页
                setScrollOffset(Math.min(maxScroll, scrollOffset + PrimitiveStoragePitMenu.getVisibleRows()));
            }
            return true;
        }

        // 搜索框聚焦逻辑：点击搜索框区域 → 聚焦；点击其他区域 → 取消聚焦
        if (isMouseOverSearchBox(mouseX, mouseY)) {
            this.searchBox.setFocused(true);
            return true;
        } else if (this.searchBox.isFocused()) {
            this.searchBox.setFocused(false);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (scrollbarDragging) {
            int maxScroll = getMaxScroll();
            int trackHeight = PrimitiveStoragePitMenu.getVisibleRows() * ROW_H - 2;
            int scrollY = topPos + SCROLLBAR_Y;
            double handleTop = mouseY - scrollY - scrollbarDragOffset;
            double ratio = Math.clamp(handleTop / (trackHeight - HANDLE_H), 0.0, 1.0);
            int newOffset = (int) Math.round(ratio * maxScroll);
            if (newOffset != scrollOffset) {
                setScrollOffset(newOffset);
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && scrollbarDragging) {
            scrollbarDragging = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ==================== 文字叠加层 ====================

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, AbstractHatchScreen.TITLE_COLOR, false);

        var data = menu.getData();
        if (data.get(2) != 0) {
            int w = data.get(0);
            int h = data.get(1);
            int used = data.get(3);
            int total = data.get(4);

            String info = String.format("%d×%d×%d  %s  %d/%d",
                    w, w, h,
                    Component.translatable("gui.civilizationevolution.used_slots").getString(),
                    used, total);
            graphics.drawString(this.font, info, 8, imageHeight - 177, AbstractHatchScreen.TITLE_COLOR, false);
        }
    }

    // ==================== 槽位渲染（SS 方案：源头抑制原版计数） ====================

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.getCount() > 99 && slot instanceof OversizedSlot) {
            int x = slot.x;
            int y = slot.y;

            if (slot.isActive()) {
                // 渲染物品图标
                graphics.renderItem(stack, x, y, slot.x + slot.y * this.imageWidth);
                // 传 "" 抑制原版 count 文字渲染
                graphics.renderItemDecorations(this.font, stack, x, y, "");
                // 绘制格式化数量
                renderOversizedCount(graphics, stack.getCount(), x, y);
            }
        } else {
            super.renderSlot(graphics, slot);
        }
    }

    private void renderOversizedCount(GuiGraphics graphics, int count, int slotX, int slotY) {
        String text = CountAbbreviator.abbreviate(count);
        int textWidth = this.font.width(text);
        float scale = Math.min(1.0F, 16.0F / textWidth);

        PoseStack poseStack = graphics.pose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, 200.0F);

        if (scale < 1.0F) {
            poseStack.scale(scale, scale, 1.0F);
            graphics.drawString(this.font, text,
                    (int) ((slotX + 17 - textWidth * scale) / scale),
                    (int) ((slotY + 9) / scale),
                    0xFFFFFF, true);
        } else {
            graphics.drawString(this.font, text,
                    slotX + 17 - textWidth,
                    slotY + 9,
                    0xFFFFFF, true);
        }

        poseStack.popPose();
    }

    // ==================== 鼠标滚轮 ====================

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = getMaxScroll();
        if (maxScroll > 0) {
            int newOffset = Math.clamp(scrollOffset - (int) Math.signum(scrollY), 0, maxScroll);
            if (newOffset != scrollOffset) {
                setScrollOffset(newOffset);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ==================== 键盘 ====================

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.searchBox.isFocused()) {
            this.searchBox.setFocused(false);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ==================== 内部工具方法 ====================

    /** 计算最大滚动行数（0-based） */
    private int getMaxScroll() {
        int totalSlots = menu.getTotalSlots();
        return Math.max(0, (totalSlots - 1) / PrimitiveStoragePitMenu.getStorageCols());
    }

    /**
     * 设置滚动偏移，更新客户端槽位并向服务端同步。
     *
     * <p>客户端调用 {@link PrimitiveStoragePitMenu#rebuildStorageSlots} 更新本端槽位，
     * 同时发送 {@link ScrollStoragePitPayload} 通知服务端同步重建。
     */
    private void setScrollOffset(int newOffset) {
        this.scrollOffset = newOffset;
        menu.rebuildStorageSlots(newOffset);
        // 同步到服务端，保证两端槽位索引一致
        PacketDistributor.sendToServer(new ScrollStoragePitPayload(newOffset));
    }
}

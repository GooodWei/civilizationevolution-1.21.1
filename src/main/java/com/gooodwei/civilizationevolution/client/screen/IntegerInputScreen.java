package com.gooodwei.civilizationevolution.client.screen;

import com.gooodwei.civilizationevolution.client.screen.hatch.AbstractHatchScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

/**
 * 通用整数输入覆盖弹窗。
 * 确认后将解析后的整数值通过回调返回，由调用方决定后续行为（发送 Payload 等）。
 *
 * <p>使用示例：
 * <pre>{@code
 * new IntegerInputScreen(
 *     Component.translatable("gui.civilizationevolution.set_min_keep"),
 *     this,        // 关闭后返回的 Screen
 *     currentValue,
 *     value -> {
 *         CompoundTag data = new CompoundTag();
 *         data.putInt("v", value);
 *         PacketDistributor.sendToServer(new UpdateMachineFieldPayload(pos, fieldId, data));
 *     }
 * );
 * }</pre>
 */
public class IntegerInputScreen extends Screen {
    /** 弹窗面板尺寸 */
    private static final int PANEL_WIDTH = 150;
    private static final int PANEL_HEIGHT = 80;
    /** 半透明背景色（0x80000000 = 50% 黑色） */
    private static final int OVERLAY_COLOR = 0x80000000;
    /** 弹窗背景色 */
    private static final int PANEL_COLOR = 0xFFC6C6C6;

    /** 关闭后返回的上一 Screen */
    private final Screen lastScreen;
    /** 确认回调，参数为解析后的整数值 */
    private final Consumer<Integer> onConfirm;
    /** EditBox 预填充的初始值 */
    private final String initialValue;

    private EditBox editBox;

    /**
     * @param title        弹窗标题
     * @param lastScreen   关闭后返回的 Screen
     * @param currentValue EditBox 预填充的当前值
     * @param onConfirm    确认回调，接收解析后的整数值
     */
    public IntegerInputScreen(Component title, Screen lastScreen,
                              int currentValue, Consumer<Integer> onConfirm) {
        super(title);
        this.lastScreen = lastScreen;
        this.initialValue = String.valueOf(currentValue);
        this.onConfirm = onConfirm;
    }

    /**
     * 初始化弹窗控件。
     *
     * <p>创建数字输入框（仅允许输入数字）、确认按钮和取消按钮，
     * 并居中放置在屏幕中央的面板区域内。</p>
     */
    @Override
    protected void init() {
        super.init();
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        this.editBox = new EditBox(
                this.font, panelX + 10, panelY + 25, PANEL_WIDTH - 20, 20, Component.empty()
        );
        this.editBox.setValue(initialValue);
        this.editBox.setFilter(text -> text.isEmpty() || text.matches("\\d+"));
        this.addRenderableWidget(this.editBox);

        // 确认按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.civilizationevolution.confirm"),
                btn -> handleConfirm()
        ).pos(panelX + 10, panelY + 52).size(60, 20).build());

        // 取消按钮
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.civilizationevolution.cancel"),
                btn -> onClose()
        ).pos(panelX + 80, panelY + 52).size(60, 20).build());
    }

    /**
     * 处理确认操作：解析输入框文本为整数，调用回调，返回上一 Screen。
     *
     * <p>输入为空时等同于取消（直接关闭）。输入格式错误理论上不会发生
     * （{@link EditBox#setFilter} 已限制仅数字可输入），但仍做安全捕获。</p>
     */
    private void handleConfirm() {
        String text = this.editBox.getValue();
        if (text.isEmpty()) {
            onClose();
            return;
        }
        try {
            int value = Integer.parseInt(text);
            this.onConfirm.accept(value);
        } catch (NumberFormatException ignored) {
            // 输入无效（filter 已限制纯数字，理论上不会触发）
        }
        Minecraft.getInstance().setScreen(this.lastScreen);
    }

    /**
     * 渲染弹窗：半透明遮罩 + 灰色面板 + 标题 + 控件。
     *
     * <p>不调用 {@code super.render}，以跳过原版 Screen 的渐变背景。</p>
     *
     * @param guiGraphics 渲染上下文
     * @param mouseX      鼠标 X 坐标（相对于屏幕）
     * @param mouseY      鼠标 Y 坐标（相对于屏幕）
     * @param partialTick 当前帧的部分 tick 插值
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 半透明遮罩层
        guiGraphics.fill(0, 0, this.width, this.height, OVERLAY_COLOR);

        // 弹窗面板
        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        guiGraphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, PANEL_COLOR);

        // 标题文字
        guiGraphics.drawCenteredString(this.font, this.title,
                this.width / 2, panelY + 8, AbstractHatchScreen.TITLE_COLOR);

        // 控件渲染（不调用 super.render 以跳过原版渐变背景）
        for (Renderable widget : this.renderables) {
            widget.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * 处理键盘按键事件。
     *
     * <p>ESC 键直接关闭弹窗，其余键优先交给输入框处理。</p>
     *
     * @param keyCode   按键码
     * @param scanCode  扫描码
     * @param modifiers 修饰键标志位
     * @return 事件已被消费则返回 {@code true}
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        // EditBox 优先处理键盘输入
        if (this.editBox.keyPressed(keyCode, scanCode, modifiers)
                || this.editBox.canConsumeInput()) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * 关闭弹窗，返回调用方传入的上一个 Screen。
     */
    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(this.lastScreen);
    }

    /**
     * 此弹窗不暂停游戏（返回 {@code false}）。
     *
     * @return 始终返回 {@code false}
     */
    @Override
    public boolean isPauseScreen() {
        return false; // 不暂停游戏
    }
}

package com.gooodwei.civilizationevolution.client.screen.hatch;

import com.gooodwei.civilizationevolution.server.menu.hatch.VillageFoodInputHatchMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 村庄食物输入接口 GUI（Tier 1）。
 *
 * <p>TODO：当前 GUI 背景纹理复用原始食物输入接口的占位纹理，
 * 需要设计村庄食物输入接口专用 GUI 纹理。
 */
public class VillageFoodInputHatchScreen extends AbstractHatchScreen<VillageFoodInputHatchMenu> {

    /** TODO：替换为村庄食物输入专用纹理，当前为原始食物输入占位副本 */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/container/primitive_food_input_hatch.png");

    public VillageFoodInputHatchScreen(VillageFoodInputHatchMenu menu,
                                       Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return TEXTURE;
    }
}

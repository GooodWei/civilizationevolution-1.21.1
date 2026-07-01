package com.gooodwei.civilizationevolution.client.screen.hatch;

import com.gooodwei.civilizationevolution.server.menu.hatch.ItemInputHatchMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 物品输入接口 GUI。
 *
 * <p>TODO：当前 GUI 背景纹理是占位纹理，需要设计 hatch 物品输入接口专用 GUI 纹理。</p>
 */
public class ItemInputHatchScreen extends AbstractHatchScreen<ItemInputHatchMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/container/primitive_food_input_hatch.png");

    public ItemInputHatchScreen(ItemInputHatchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return TEXTURE;
    }
}

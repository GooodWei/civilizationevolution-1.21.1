package com.gooodwei.civilizationevolution.client.screen.hatch;

import com.gooodwei.civilizationevolution.server.menu.hatch.ItemOutputHatchMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 物品输出接口 GUI。
 *
 * <p>TODO：当前 GUI 背景纹理是占位纹理，需要设计 hatch 物品输出接口专用 GUI 纹理。</p>
 */
public class ItemOutputHatchScreen extends AbstractHatchScreen<ItemOutputHatchMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/container/primitive_population_output_hatch.png");

    public ItemOutputHatchScreen(ItemOutputHatchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return TEXTURE;
    }
}

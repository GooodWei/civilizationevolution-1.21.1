package com.gooodwei.civilizationevolution.client.screen.hatch;

import com.gooodwei.civilizationevolution.server.menu.hatch.PrimitivePopulationInputHatchMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 原始人口输入接口 GUI。
 *
 * <p>TODO：当前 GUI 背景纹理是原始营地（primitive_camp.png）的副本占位，
 * 需要设计 hatch 输入接口专用 GUI 纹理。详见 {@code dontpush/gui_design_tasks.md}。</p>
 */
public class PrimitivePopulationInputHatchScreen extends AbstractHatchScreen<PrimitivePopulationInputHatchMenu> {

    /** TODO：替换为 hatch 人口输入专用纹理，当前为营地占位副本 */
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("civilizationevolution",
                    "textures/gui/container/primitive_population_input_hatch.png");

    public PrimitivePopulationInputHatchScreen(PrimitivePopulationInputHatchMenu menu,
                                                Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected ResourceLocation getGuiTexture() {
        return TEXTURE;
    }
}

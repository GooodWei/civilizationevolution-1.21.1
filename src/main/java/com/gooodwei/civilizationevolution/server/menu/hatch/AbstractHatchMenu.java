package com.gooodwei.civilizationevolution.server.menu.hatch;

import com.gooodwei.civilizationevolution.server.menu.machine.MachineMenu;


import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

/**
 * 所有 hatch（多方块接口）菜单的抽象基类。
 *
 * <p>继承 {@link MachineMenu}，持有 Container 引用供 Screen 查询。
 * 具体 hatch 菜单只需在构造器中添加对应槽位即可。
 */
public abstract class AbstractHatchMenu extends MachineMenu {

    /** 单槽 hatch 的 GUI 槽位坐标（居中） */
    protected static final int SINGLE_SLOT_X = 80;
    protected static final int SINGLE_SLOT_Y = 35;

    /** hatch 的容器，供 Screen 访问 */
    public final Container container;

    protected AbstractHatchMenu(MenuType<?> type, int containerId, Container container) {
        super(type, containerId);
        this.container = container;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}

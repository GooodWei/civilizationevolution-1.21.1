package com.gooodwei.civilizationevolution.integration.jade;

import com.gooodwei.civilizationevolution.server.block.AbstractMachineBlock;
import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractMachineBlockEntity;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * 文明演进 Jade 联动插件入口。
 *
 * <p>通过 {@code @WailaPlugin} 注解由 Jade 自动发现，无需任何显式注册。
 * 仅在客户端安装 Jade 时生效，未安装时类不会被加载（因所有引用均为 Jade API，
 * 无 Jade 时调用链不会到达此类）。
 *
 * <p>服务端：注册 {@link MachineDataProvider}，从机器 BE 收集 Tier 和绑定信息。
 * 客户端：注册 {@link MachineComponentProvider}，在 tooltip 中渲染收集的数据。
 */
@WailaPlugin
public class CivEvoJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(
                MachineDataProvider.INSTANCE, AbstractMachineBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(
                MachineComponentProvider.INSTANCE, AbstractMachineBlock.class);
    }
}

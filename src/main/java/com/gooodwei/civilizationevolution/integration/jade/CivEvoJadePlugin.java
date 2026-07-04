package com.gooodwei.civilizationevolution.integration.jade;

import com.gooodwei.civilizationevolution.server.block.hatch.AbstractHatchBlock;
import com.gooodwei.civilizationevolution.server.block.machine.AbstractMachineBlock;
import com.gooodwei.civilizationevolution.server.block.part.AbstractMultiBlockPart;
import com.gooodwei.civilizationevolution.server.blockentity.controller.AbstractControllerBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.hatch.AbstractHatchBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.machine.AbstractMachineBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.multiblock.AbstractMultiBlockMachineBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.multiblock.PrimitiveStoragePitBlockEntity;
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
 * <p>服务端：注册 {@link MachineDataProvider}，从所有有 Tier 的方块/BE 收集信息。
 * <p>客户端：注册 {@link MachineComponentProvider}，在 tooltip 中渲染 Tier 和绑定信息。
 */
@WailaPlugin
public class CivEvoJadePlugin implements IWailaPlugin {

    @Override
    public void register(IWailaCommonRegistration registration) {
        // 范围机器 BE（Camp、Ranch、HuntingGround、Farm）
        registration.registerBlockDataProvider(
                MachineDataProvider.INSTANCE, AbstractMachineBlockEntity.class);
        // 控制器 BE（PrimitiveController、VillageController）
        registration.registerBlockDataProvider(
                MachineDataProvider.INSTANCE, AbstractControllerBlockEntity.class);
        // 仓室 BE（食物、人口、物品、流体输入输出）
        registration.registerBlockDataProvider(
                MachineDataProvider.INSTANCE, AbstractHatchBlockEntity.class);
        // 多方块机器 BE（VillageQuarry、DoctorCabin 等）
        registration.registerBlockDataProvider(
                MachineDataProvider.INSTANCE, AbstractMultiBlockMachineBlockEntity.class);
        // 储物坑 BE（非 IPopulationMachine，需单独注册）
        registration.registerBlockDataProvider(
                MachineDataProvider.INSTANCE, PrimitiveStoragePitBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        // 机器方块
        registration.registerBlockComponent(
                MachineComponentProvider.INSTANCE, AbstractMachineBlock.class);
        // 仓室方块
        registration.registerBlockComponent(
                MachineComponentProvider.INSTANCE, AbstractHatchBlock.class);
        // 多方块结构零件（控制器方块、结构外壳等，部分无 BE）
        registration.registerBlockComponent(
                MachineComponentProvider.INSTANCE, AbstractMultiBlockPart.class);
        // PrimitiveStoragePit 已继承 AbstractMachineBlock（已注册），无需重复注册
    }
}

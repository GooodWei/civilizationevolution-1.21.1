package com.gooodwei.civilizationevolution.server.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * BlockEntity 能力（Capability）注册中心。
 *
 * <p>为农场方块和流体仓室注册 {@link Capabilities.FluidHandler#BLOCK} 流体能力，
 * 使所有物流模组（Pipez、Mekanism、AE2、Integrated Dynamics 等）的管道
 * 均能通过 NeoForge 标准接口与储水罐交互。
 *
 * <p>新增流体能力的步骤：
 * <ol>
 *   <li>在此类的 {@link #registerCapabilities(RegisterCapabilitiesEvent)} 中添加注册</li>
 *   <li>确保目标 BlockEntity 已实现 {@code getFluidHandler()} 方法</li>
 * </ol>
 */
public final class CapabilityRegistry {

    private CapabilityRegistry() {
        throw new UnsupportedOperationException("注册类，不可实例化");
    }

    /**
     * 注册所有 BlockEntity 的流体能力。
     *
     * <p>此方法作为 {@link RegisterCapabilitiesEvent} 的监听器，
     * 在 {@code CivilizationEvolution} 构造器中通过
     * {@code modEventBus.addListener(CapabilityRegistry::registerCapabilities)} 注册。
     *
     * @param event 能力注册事件
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 为原始农场注册流体能力（所有方向均可输入水）
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.PRIMITIVE_FARM.get(),
                (be, direction) -> be.getFluidHandler()
        );
        // 为村庄农场注册流体能力（所有方向均可输入水）
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.VILLAGE_FARM.get(),
                (be, direction) -> be.getFluidHandler()
        );
        // 为原始流体输入仓室注册流体能力
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.PRIMITIVE_FLUID_INPUT_HATCH.get(),
                (be, direction) -> be.getFluidHandler()
        );
        // 为村庄流体输入仓室注册流体能力
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.VILLAGE_FLUID_INPUT_HATCH.get(),
                (be, direction) -> be.getFluidHandler()
        );
        // 为原始流体输出仓室注册流体能力
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.PRIMITIVE_FLUID_OUTPUT_HATCH.get(),
                (be, direction) -> be.getFluidHandler()
        );
        // 为村庄流体输出仓室注册流体能力
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.VILLAGE_FLUID_OUTPUT_HATCH.get(),
                (be, direction) -> be.getFluidHandler()
        );
        // 为村庄收割机注册流体能力（所有方向均可输入水）
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.VILLAGE_HARVESTER.get(),
                (be, direction) -> be.getFluidHandler()
        );
    }
}

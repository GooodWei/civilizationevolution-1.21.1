package com.gooodwei.civilizationevolution.network;

import com.gooodwei.civilizationevolution.api.IClientUpdateReceiver;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;


/**
 * 网络包注册与处理中心。
 *
 * <p>负责注册所有自定义 Payload 的编解码器，并将客户端→服务端 Payload
 * 路由到对应 BlockEntity（通过 {@link IClientUpdateReceiver} 接口解耦）。
 * 新增 Payload 时在此注册即可，无需修改其他网络层代码。
 */
public class NetworkHandler {

    /**
     * 在 NeoForge 网络初始化事件中注册 Payload。
     * 当前仅注册客户端→服务端的 {@link UpdateMachineFieldPayload}。
     *
     * @param event NeoForge 网络注册事件
     */
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");
        registrar.playToServer(
                UpdateMachineFieldPayload.TYPE,
                UpdateMachineFieldPayload.STREAM_CODEC,
                NetworkHandler::handleMachineUpdate
        );
    }

    /**
     * 处理客户端发来的机器字段更新请求。
     *
     * <p>安全检查：
     * <ul>
     *   <li>目标方块所在区块必须已加载</li>
     *   <li>玩家与目标方块的距离不超过 8 格（防止远程篡改）</li>
     * </ul>
     * 通过后将 Payload 委托给目标 BE 的 {@link IClientUpdateReceiver#onClientUpdate}。
     */
    private static void handleMachineUpdate(final UpdateMachineFieldPayload payload, final IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        // 安全检查：方块已加载 + 距离不超过 8 格
        if (!player.serverLevel().isLoaded(payload.pos())) return;
        if (player.distanceToSqr(
                payload.pos().getX() + 0.5,
                payload.pos().getY() + 0.5,
                payload.pos().getZ() + 0.5) > 64.0) return;

        // 委托给目标 BlockEntity 处理（通过接口解耦）
        BlockEntity be = player.serverLevel().getBlockEntity(payload.pos());
        if (be instanceof IClientUpdateReceiver receiver) {
            receiver.onClientUpdate(payload.fieldId(), payload.data());
        }
    }

}

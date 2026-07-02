package com.gooodwei.civilizationevolution.network;

import com.gooodwei.civilizationevolution.api.IClientUpdateReceiver;
import com.gooodwei.civilizationevolution.client.ClientPayloadHandler;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;


/**
 * 网络层唯一对外门面。
 *
 * <p>统一管理所有 Payload 注册和发送操作：
 * <ul>
 *   <li>C→S —— {@link #register(RegisterPayloadHandlersEvent)}（由 MOD 总线触发）</li>
 *   <li>S→C —— {@link #registerClientPayloads(RegisterPayloadHandlersEvent)}（由客户端入口调用）</li>
 *   <li>发送 —— {@link #sendToPlayer(ServerPlayer, CustomPacketPayload)}</li>
 * </ul>
 *
 * <p>外部代码不应直接引用 {@link PacketDistributor}，统一通过此类发送。
 */
public class NetworkHandler {

    /**
     * 注册客户端→服务端 Payload（由 MOD 事件总线触发，双端均执行）。
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
     * 注册服务端→客户端 Payload（仅客户端侧调用）。
     * 由 {@code CivilizationEvolutionClient} 构造器中的 MOD 事件总线触发。
     */
    public static void registerClientPayloads(final RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1.0.0");
        registrar.playToClient(
                SyncMachineListPayload.TYPE,
                SyncMachineListPayload.STREAM_CODEC,
                ClientPayloadHandler::handleSyncMachineList
        );
        registrar.playToClient(
                StructurePreviewPayload.TYPE,
                StructurePreviewPayload.STREAM_CODEC,
                ClientPayloadHandler::handleStructurePreview
        );
    }

    /**
     * 向指定玩家发送 Payload。
     *
     * @param player  目标玩家
     * @param payload 待发送的 Payload
     */
    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        PacketDistributor.sendToPlayer(player, payload);
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
        if (!player.serverLevel().isLoaded(payload.pos())) return;
        if (player.distanceToSqr(
                payload.pos().getX() + 0.5,
                payload.pos().getY() + 0.5,
                payload.pos().getZ() + 0.5) > 64.0) return;

        BlockEntity be = player.serverLevel().getBlockEntity(payload.pos());
        if (be instanceof IClientUpdateReceiver receiver) {
            receiver.onClientUpdate(payload.fieldId(), payload.data());
        }
    }

}

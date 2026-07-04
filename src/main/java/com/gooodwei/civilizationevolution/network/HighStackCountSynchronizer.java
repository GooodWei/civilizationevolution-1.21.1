package com.gooodwei.civilizationevolution.network;

import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 替换原版 {@link ContainerSynchronizer}，使用自定义 oversized Payload 同步容器内容。
 * 照搬 SophisticatedCore 的同步器模式。
 *
 * <p>原版同步器使用 {@link ClientboundContainerSetSlotPacket}，
 * 该包的 STREAM_CODEC 不验证 count 范围，理论上可以传输 oversized 物品。
 * 但为了统一使用 {@link ItemStack#OPTIONAL_STREAM_CODEC} 并保证未来兼容性，
 * 本同步器使用自定义的 {@link SyncOversizedStacksPayload} 和 {@link SyncOversizedSlotPayload}。
 *
 * <p>手持物品（carried）使用原版包同步——手持物品不会超过 maxStackSize（64/16）。
 */
public class HighStackCountSynchronizer implements ContainerSynchronizer {
    private final ServerPlayer player;

    public HighStackCountSynchronizer(ServerPlayer player) {
        this.player = player;
    }

    /** 发送完整容器内容（窗口首次打开时） */
    @Override
    public void sendInitialData(AbstractContainerMenu menu, NonNullList<ItemStack> stacks,
                                 ItemStack carried, int[] dataSlots) {
        PacketDistributor.sendToPlayer(player,
                new SyncOversizedStacksPayload(menu.containerId, menu.incrementStateId(), stacks, carried));
    }

    /** 发送单个槽位变化 */
    @Override
    public void sendSlotChange(AbstractContainerMenu menu, int slotIndex, ItemStack stack) {
        PacketDistributor.sendToPlayer(player,
                new SyncOversizedSlotPayload(menu.containerId, menu.incrementStateId(), slotIndex, stack));
    }

    /** 发送手持物品变化（手持物品不会 oversize，使用原版包） */
    @Override
    public void sendCarriedChange(AbstractContainerMenu menu, ItemStack stack) {
        player.connection.send(
                new ClientboundContainerSetSlotPacket(-1, menu.incrementStateId(), -1, stack));
    }

    /** 发送数据槽位变化（ContainerData 同步，使用原版包） */
    @Override
    public void sendDataChange(AbstractContainerMenu menu, int dataSlot, int value) {
        player.connection.send(
                new ClientboundContainerSetDataPacket(menu.containerId, dataSlot, value));
    }
}

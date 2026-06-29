package com.gooodwei.civilizationevolution.client;

import com.gooodwei.civilizationevolution.client.screen.PrimitiveControllerScreen;
import com.gooodwei.civilizationevolution.network.SyncMachineListPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Collections;
import java.util.List;

/**
 * 客户端 Payload 处理器。
 * 仅在物理客户端加载，处理服务端→客户端 Payload。
 */
public class ClientPayloadHandler {

    /** 缓存的机器列表，用于 Screen 打开前收到的数据同步 */
    private static List<SyncMachineListPayload.MachineEntry> cachedMachineList = Collections.emptyList();

    /**
     * 获取缓存的机器列表，供 Screen 初始化时调用。
     */
    public static List<SyncMachineListPayload.MachineEntry> getCachedMachineList() {
        return cachedMachineList;
    }

    /**
     * 处理机器列表同步：缓存数据并传递给当前打开的 PrimitiveControllerScreen。
     */
    public static void handleSyncMachineList(final SyncMachineListPayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            cachedMachineList = payload.machines();
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof PrimitiveControllerScreen screen) {
                screen.updateMachineList(payload.machines());
            }
        });
    }
}

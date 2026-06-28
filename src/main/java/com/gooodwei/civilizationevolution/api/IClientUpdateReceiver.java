package com.gooodwei.civilizationevolution.api;

import net.minecraft.nbt.CompoundTag;

/**
 * 可接收客户端字段更新的 BlockEntity 接口。
 * 实现此接口后，{@code NetworkHandler} 自动将客户端 Payload 路由到对应 BE。
 *
 * <p>新增 BE 类型时，只需实现此接口并覆写 {@link #onClientUpdate(int, CompoundTag)}，
 * 无需修改网络层代码。
 *
 * @see com.gooodwei.civilizationevolution.network.UpdateMachineFieldPayload
 * @see com.gooodwei.civilizationevolution.network.NetworkHandler
 */
public interface IClientUpdateReceiver {

    /**
     * 接收客户端发来的字段更新（由 {@code UpdateMachineFieldPayload} 携带）。
     * 运行在服务端。新增字段时只需在实现类中新增 case 分支。
     *
     * @param fieldId 字段编号（各 BE 自行定义 FIELD_XXX 常量）
     * @param data    客户端提交的数据，通过 CompoundTag 携带任意类型
     */
    void onClientUpdate(int fieldId, CompoundTag data);
}

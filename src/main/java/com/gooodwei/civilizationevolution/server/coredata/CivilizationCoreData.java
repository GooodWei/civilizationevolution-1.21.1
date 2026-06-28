package com.gooodwei.civilizationevolution.server.coredata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.core.BlockPos;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * 文明核心对应的持久化数据模型。
 *
 * <p>每个文明核心物品通过 UUID 绑定到一个此类实例，
 * 数据以 JSON 文件形式存储在服务器 config 目录下。
 *
 * <p>设计关键：物品 NBT 中仅保存 UUID 字符串，所有业务数据
 * 均在此类中维护并独立持久化。核心物品转移到更高级控制器时，
 * 数据通过 UUID 自动继承。
 */
public class CivilizationCoreData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MACHINE_LIST_TYPE = new TypeToken<List<BoundMachineEntry>>() {}.getType();

    /** 核心的 UUID 字符串 */
    private String uuid;

    /** 当前所在控制器类型标识（如 "primitive_settlement"） */
    private String controllerType;

    /** 绑定机器列表 */
    private List<BoundMachineEntry> boundMachines;

    /** 累积统计（预留扩展） */
    private transient boolean dirty;

    public CivilizationCoreData() {
        this.boundMachines = new ArrayList<>();
    }

    public CivilizationCoreData(String uuid, String controllerType) {
        this.uuid = uuid;
        this.controllerType = controllerType;
        this.boundMachines = new ArrayList<>();
    }

    // ==================== JSON 序列化 ====================

    public String toJson() {
        return GSON.toJson(this);
    }

    public static CivilizationCoreData fromJson(String json) {
        CivilizationCoreData data = GSON.fromJson(json, CivilizationCoreData.class);
        if (data.boundMachines == null) {
            data.boundMachines = new ArrayList<>();
        }
        return data;
    }

    // ==================== 绑定机器操作 ====================

    public void addMachine(BlockPos pos, int nextTriggerProgress, boolean enabled, String machineType, int tier) {
        boundMachines.add(new BoundMachineEntry(pos, nextTriggerProgress, enabled, machineType, tier));
        markDirty();
    }

    public boolean removeMachine(BlockPos pos) {
        long posLong = pos.asLong();
        boolean removed = boundMachines.removeIf(bm -> bm.pos == posLong);
        if (removed) markDirty();
        return removed;
    }

    public BoundMachineEntry getMachine(BlockPos pos) {
        long posLong = pos.asLong();
        for (BoundMachineEntry bm : boundMachines) {
            if (bm.pos == posLong) return bm;
        }
        return null;
    }

    public boolean hasMachine(BlockPos pos) {
        return getMachine(pos) != null;
    }

    public void setMachineEnabled(BlockPos pos, boolean enabled) {
        BoundMachineEntry bm = getMachine(pos);
        if (bm != null) {
            bm.enabled = enabled;
            markDirty();
        }
    }

    // ==================== Getter/Setter ====================

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public String getControllerType() { return controllerType; }
    public void setControllerType(String controllerType) { this.controllerType = controllerType; }

    public List<BoundMachineEntry> getBoundMachines() { return boundMachines; }
    public void setBoundMachines(List<BoundMachineEntry> boundMachines) { this.boundMachines = boundMachines; }

    // ==================== 脏标记 ====================

    public boolean isDirty() { return dirty; }
    public void markDirty() { this.dirty = true; }
    public void markClean() { this.dirty = false; }

    // ==================== 绑定机器条目 ====================

    /**
     * 单个绑定机器的持久化条目。
     * 使用 long 序列化 BlockPos（通过 {@link BlockPos#asLong()}），
     * 由 {@code PrimitiveSettlementBlockEntity} 在运行时直接使用。
     */
    public static class BoundMachineEntry {
        public long pos;            // BlockPos.asLong()
        public int nextTriggerProgress;
        public boolean enabled;
        /** 机器 BlockEntityType 的注册名（如 "civilizationevolution:camp"），用于验证机器类型是否匹配 */
        public String machineType;
        /** 绑定时机器的 Tier 等级，用于核心迁移后的调度兼容性判断 */
        public int tier;

        public BoundMachineEntry() {}

        public BoundMachineEntry(BlockPos pos, int nextTriggerProgress, boolean enabled, String machineType, int tier) {
            this.pos = pos.asLong();
            this.nextTriggerProgress = nextTriggerProgress;
            this.enabled = enabled;
            this.machineType = machineType;
            this.tier = tier;
        }

        public BlockPos getBlockPos() {
            return BlockPos.of(pos);
        }
    }
}

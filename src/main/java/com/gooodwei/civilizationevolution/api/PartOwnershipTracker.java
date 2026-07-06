package com.gooodwei.civilizationevolution.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多方块零件认领追踪器（静态，世界维度范围的 Map）。
 *
 * <p>服务于无 {@link net.minecraft.world.level.block.entity.BlockEntity} 的多方块零件
 * （如 {@code AbstractStructureCasing}），这些零件无法存储位置相关状态（Block 是单例）。
 * 有 BlockEntity 的零件（hatch）应覆写 {@link IMultiBlockPart} 的认领方法，
 * 使用自身 NBT 持久化字段，以获得更好的性能和数据持久性。
 *
 * <p>线程安全：MC 服务端主线程单线程，但使用 {@link ConcurrentHashMap}
 * 以防 {@link com.gooodwei.civilizationevolution.server.validation.StructureValidationService} 未来扩展。
 *
 * @see IMultiBlockPart
 */
public final class PartOwnershipTracker {

    /**
     * Key: "dimensionLocation|posX,posY,posZ" → 控制器坐标
     * 使用 dimension location 区分不同维度，使用坐标序列化
     */
    private static final Map<String, BlockPos> OWNERSHIP = new ConcurrentHashMap<>();

    private PartOwnershipTracker() {}

    /** 生成维度内唯一的 key */
    static String makeKey(Level level, BlockPos pos) {
        return level.dimension().location() + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    /**
     * 查询零件的当前拥有者控制器坐标。
     *
     * @param level   所在世界
     * @param partPos 零件世界坐标
     * @return 控制器坐标，未认领时返回 null
     */
    @Nullable
    public static BlockPos getOwner(Level level, BlockPos partPos) {
        return OWNERSHIP.get(makeKey(level, partPos));
    }

    /**
     * 设置零件的拥有者控制器。
     *
     * @param level         所在世界
     * @param partPos       零件世界坐标
     * @param controllerPos 认领此零件的控制器坐标
     */
    public static void setOwner(Level level, BlockPos partPos, BlockPos controllerPos) {
        OWNERSHIP.put(makeKey(level, partPos), controllerPos);
    }

    /**
     * 释放认领（仅当当前拥有者与指定控制器匹配时）。
     * 使用 {@link ConcurrentHashMap#remove(Object, Object)} 原子条件删除。
     *
     * @param level         所在世界
     * @param partPos       零件世界坐标
     * @param controllerPos 要释放的控制器坐标
     * @return true 表示成功释放，false 表示当前拥有者不匹配或未被认领
     */
    public static boolean releaseOwner(Level level, BlockPos partPos, BlockPos controllerPos) {
        return OWNERSHIP.remove(makeKey(level, partPos), controllerPos);
    }

    /**
     * 原子地移除并返回零件的认领记录。
     * 用于方块破坏事件中快速查找受影响的控制器。
     *
     * @param level   所在世界
     * @param partPos 零件世界坐标
     * @return 之前的控制器坐标，未认领时返回 null
     */
    @Nullable
    public static BlockPos removeOwner(Level level, BlockPos partPos) {
        return OWNERSHIP.remove(makeKey(level, partPos));
    }

    /**
     * 获取指定维度的所有认领记录。
     * 用于方块破坏事件中查找受影响的控制器。
     *
     * @param level 所在世界
     * @return partPos → controllerPos 映射的快照
     */
    public static Map<BlockPos, BlockPos> getOwnersInDimension(Level level) {
        String prefix = level.dimension().location() + "|";
        Map<BlockPos, BlockPos> result = new HashMap<>();
        for (Map.Entry<String, BlockPos> entry : OWNERSHIP.entrySet()) {
            if (entry.getKey().startsWith(prefix)) {
                // 解析 "dim|partX,partY,partZ" → BlockPos
                String[] parts = entry.getKey().substring(prefix.length()).split(",");
                BlockPos partPos = new BlockPos(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]));
                result.put(partPos, entry.getValue());
            }
        }
        return result;
    }

    /**
     * 清除指定维度的所有认领记录（世界卸载时调用）。
     *
     * @param level 要清除的世界维度
     */
    public static void clearDimension(Level level) {
        String prefix = level.dimension().location() + "|";
        OWNERSHIP.keySet().removeIf(k -> k.startsWith(prefix));
    }
}

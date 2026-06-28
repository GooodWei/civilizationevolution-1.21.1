package com.gooodwei.civilizationevolution.server.coredata;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文明核心数据文件管理器。
 *
 * <p>负责以 JSON 文件形式读写 {@link CivilizationCoreData} 实例。
 * 数据存储于：{@code <world>/civilizationevolution/coredata/<uuid>.json}
 *
 * <p>核心设计：
 * <ul>
 *   <li>内存中维护一个 {@code Map<String, CivilizationCoreData>} 缓存，避免频繁文件 I/O</li>
 *   <li>每次 {@link #markDirty(String)} 调用后，由调度器定期落盘</li>
 *   <li>{@link #saveAll()} 在服务器停止时由外部调用</li>
 *   <li>UUID 作为 key，物品 NBT 中仅存此 UUID 字符串</li>
 * </ul>
 */
public class CoreDataManager {

    /** 世界目录下的子路径 */
    private static final String DATA_SUBDIR = "civilizationevolution/coredata";

    /** 内存缓存：UUID → CivilizationCoreData */
    private static final Map<String, CivilizationCoreData> CACHE = new ConcurrentHashMap<>();

    /** 记录哪些 UUID 的数据在内存中被修改过 */
    private static final java.util.Set<String> DIRTY_SET = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());

    private static Path dataDir;

    // ==================== 初始化 ====================

    /**
     * 在世界加载后调用，设定数据存储目录。
     * @param worldPath 世界目录路径
     */
    public static void init(Path worldPath) {
        dataDir = worldPath.resolve(DATA_SUBDIR);
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            CivilizationEvolution.LOGGER.error("无法创建核心数据目录: {}", dataDir, e);
        }
        CACHE.clear();
        DIRTY_SET.clear();
    }

    /**
     * 检查是否已初始化。
     */
    public static boolean isReady() {
        return dataDir != null;
    }

    // ==================== 生成 UUID ====================

    /**
     * 为新的文明核心物品生成一个唯一的 UUID 字符串。
     */
    public static String generateUuid() {
        return UUID.randomUUID().toString();
    }

    // ==================== 数据读写 ====================

    /**
     * 获取或创建指定 UUID 对应的核心数据。
     * 优先从内存缓存读取，其次从磁盘 JSON 文件加载，
     * 若均无则返回 null（由调用方决定是否新建）。
     *
     * @param uuid 核心 UUID 字符串
     * @return 对应的 CivilizationCoreData，或 null
     */
    public static CivilizationCoreData getOrLoad(String uuid) {
        if (uuid == null || uuid.isEmpty()) return null;

        // 内存缓存命中
        if (CACHE.containsKey(uuid)) {
            return CACHE.get(uuid);
        }

        // 尝试从磁盘加载
        CivilizationCoreData loaded = loadFromDisk(uuid);
        if (loaded != null) {
            CACHE.put(uuid, loaded);
        }
        return loaded;
    }

    /**
     * 创建新的核心数据并存入缓存。
     *
     * @param uuid           UUID 字符串
     * @param controllerType 控制器类型标识
     * @return 新建的 CivilizationCoreData
     */
    public static CivilizationCoreData createNew(String uuid, String controllerType) {
        CivilizationCoreData data = new CivilizationCoreData(uuid, controllerType);
        CACHE.put(uuid, data);
        markDirty(uuid);
        return data;
    }

    /**
     * 将指定 UUID 的数据标记为脏，下次 saveAll 时落盘。
     */
    public static void markDirty(String uuid) {
        DIRTY_SET.add(uuid);
    }

    /**
     * 将所有脏数据写入磁盘。
     * 在服务器 tick 末尾或 saveAllData 事件时调用。
     */
    public static void saveDirty() {
        if (!isReady()) return;

        for (String uuid : DIRTY_SET) {
            CivilizationCoreData data = CACHE.get(uuid);
            if (data == null) continue;
            saveToDisk(uuid, data);
        }
        DIRTY_SET.clear();
    }

    /**
     * 保存所有缓存数据（无论脏不脏）并清空标记。
     * 在服务器停止时调用。
     */
    public static void saveAll() {
        if (!isReady()) return;

        for (Map.Entry<String, CivilizationCoreData> entry : CACHE.entrySet()) {
            saveToDisk(entry.getKey(), entry.getValue());
        }
        DIRTY_SET.clear();
    }

    // ==================== 内部方法 ====================

    private static Path getFilePath(String uuid) {
        return dataDir.resolve(uuid + ".json");
    }

    private static void saveToDisk(String uuid, CivilizationCoreData data) {
        try {
            String json = data.toJson();
            Files.writeString(getFilePath(uuid), json);
        } catch (IOException e) {
            CivilizationEvolution.LOGGER.error("保存核心数据失败: uuid={}", uuid, e);
        }
    }

    private static CivilizationCoreData loadFromDisk(String uuid) {
        Path path = getFilePath(uuid);
        if (!Files.exists(path)) return null;

        try {
            String json = Files.readString(path);
            return CivilizationCoreData.fromJson(json);
        } catch (IOException e) {
            CivilizationEvolution.LOGGER.error("读取核心数据失败: uuid={}", uuid, e);
            return null;
        }
    }

    /**
     * 删除指定 UUID 的磁盘文件（核心物品被销毁时清理）。
     */
    public static void deleteData(String uuid) {
        CACHE.remove(uuid);
        DIRTY_SET.remove(uuid);
        if (!isReady()) return;
        try {
            Files.deleteIfExists(getFilePath(uuid));
        } catch (IOException e) {
            CivilizationEvolution.LOGGER.error("删除核心数据失败: uuid={}", uuid, e);
        }
    }

    /**
     * 检查指定 UUID 是否有缓存数据（用于判断核心是否已初始化）。
     */
    public static boolean isCached(String uuid) {
        return CACHE.containsKey(uuid);
    }
}

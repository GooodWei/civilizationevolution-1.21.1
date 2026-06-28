package com.gooodwei.civilizationevolution.api.range;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 范围扫描工具类 —— 在工作范围（AABB）内查询实体、方块、玩家等，
 * 并提供分组、过滤等整理方法。
 *
 * <p>所有方法均为静态，不持有状态。后续可按需扩展：
 * <ul>
 *   <li>检测特定方块（如矿石、树木）</li>
 *   <li>检测范围内是否有玩家</li>
 *   <li>……</li>
 * </ul>
 */
public final class RangeScanner {

    private RangeScanner() {
        // 工具类，禁止实例化
    }

    // ==================== 实体查询 ====================

    /**
     * 获取范围内的所有实体（按指定类型筛选）。
     *
     * @param level 服务端世界
     * @param range 查询范围 AABB
     * @param clazz 要筛选的实体子类
     * @param <T>   实体子类型
     * @return 范围内符合类型的实体列表（可能为空）
     */
    public static <T extends Entity> List<T> getEntities(
            ServerLevel level, AABB range, Class<T> clazz) {
        return level.getEntitiesOfClass(clazz, range);
    }

    /**
     * 获取范围内的所有 {@link Animal} 实体（便捷方法）。
     *
     * @param level 服务端世界
     * @param range 查询范围 AABB
     * @return 范围内所有 Animal 实体的列表（可能为空）
     */
    public static List<Animal> getAnimals(ServerLevel level, AABB range) {
        return getEntities(level, range, Animal.class);
    }

    // ==================== 分组 ====================

    /**
     * 获取范围内实体，经可选过滤后，按指定键分组。
     *
     * <p>典型用法 —— 按动物子类分组并排除幼年动物：
     * <pre>{@code
     * var grouped = RangeScanner.getEntitiesGroupedByKey(
     *         serverLevel, range, Animal.class,
     *         animal -> !animal.isBaby(),  // 过滤器
     *         Animal::getClass);           // 分组键
     * }</pre>
     *
     * @param level      服务端世界
     * @param range      查询范围 AABB
     * @param clazz      要筛选的实体子类
     * @param filter     可选过滤器，传入 {@code null} 则不过滤直接分组
     * @param classifier 分组键提取器（如 {@code Animal::getClass}）
     * @param <T>        实体子类型
     * @param <K>        分组键类型
     * @return 按指定键分组的 ConcurrentHashMap（可能为空 Map）
     */
    public static <T extends Entity, K> ConcurrentHashMap<K, List<T>> getEntitiesGroupedByKey(
            ServerLevel level, AABB range,
            Class<T> clazz,
            Predicate<T> filter,
            Function<T, K> classifier) {
        var stream = getEntities(level, range, clazz).stream();
        if (filter != null) {
            stream = stream.filter(filter);
        }
        return stream.collect(Collectors.groupingBy(
                classifier, ConcurrentHashMap::new, Collectors.toList()));
    }
}

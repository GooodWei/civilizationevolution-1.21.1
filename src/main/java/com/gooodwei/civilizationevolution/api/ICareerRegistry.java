package com.gooodwei.civilizationevolution.api;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.minecraft.world.entity.npc.VillagerProfession;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;

/**
 * 职业注册表接口。
 * 提供职业的注册、查询和遍历功能。
 * 附属模组通过 {@link CivilizationAPI#getCareerRegistry()} 获取实例，
 * 或通过继承 {@link Career} 并调用其构造函数自动注册新职业。
 */
public interface ICareerRegistry {

    /**
     * 按名称查找职业。
     * @param name 职业名称（如 "armorer"）
     * @return 对应的 Career，未找到返回 null
     */
    @Nullable
    Career byName(String name);

    /**
     * 获取所有已注册职业的只读视图。
     * @return 不可修改的 Career 集合
     */
    Collection<Career> allCareers();

    /**
     * 将原版村民职业映射到模组职业。
     * @param prof 原版村民职业
     * @return 对应的 Career，无匹配时返回 "unemployed"
     */
    Career fromVanilla(VillagerProfession prof);

    /**
     * 获取底层注册表 Map 的引用。
     * 附属模组可直接读写（如移除职业、遍历所有条目），但应谨慎使用。
     * @return 职业名称到 Career 实例的 Map
     */
    Map<String, Career> getRegistry();
}

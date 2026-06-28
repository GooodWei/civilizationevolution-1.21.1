package com.gooodwei.civilizationevolution.api;

import com.gooodwei.civilizationevolution.server.item.PopulationItem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;

/**
 * 人口管理接口。
 * 提供人口的创建、年龄增长、死亡判定、属性读写和工作效率重算等功能。
 * 附属模组通过 {@link CivilizationAPI#getPopulationManager()} 获取实例。
 */
public interface IPopulationManager {

    /**
     * 将原版村民转换为人口 ItemStack。
     * @param villager 原版村民实体
     * @param item     人口物品类型
     * @return 填充了完整 NBT 属性的新 ItemStack
     */
    ItemStack fromVillager(Villager villager, PopulationItem item);

    /**
     * 人口年龄 +1，若年龄达到寿命则销毁物品并发出死亡事件。
     * @param stack 人口 ItemStack
     * @return true 表示人口存活并增加了年龄，false 表示已死亡（物品被销毁）
     */
    boolean addAge(ItemStack stack);

    /**
     * 判断人口是否已死亡。
     * @param stack 人口 ItemStack
     * @return true 表示年龄已达到或超过寿命上限
     */
    boolean isDead(ItemStack stack);

    /**
     * 根据当前 NBT 中的年龄、生命值、精神状态和熟练度重新计算工作效率。
     * @param stack 人口 ItemStack
     */
    void recalcEfficiency(ItemStack stack);

    /**
     * 设置生命值并自动重算工作效率。
     * @param stack  人口 ItemStack
     * @param health 生命值（0-100）
     */
    void setHealth(ItemStack stack, int health);

    /**
     * 设置饱食度（不影响工作效率）。
     * @param stack 人口 ItemStack
     * @param food  饱食度（0-100）
     */
    void setFood(ItemStack stack, int food);

    /**
     * 设置熟练度并自动重算工作效率。
     * @param stack       人口 ItemStack
     * @param proficiency 熟练度，默认 100
     */
    void setProficiency(ItemStack stack, int proficiency);

    /**
     * 设置精神状态并自动重算工作效率。
     * @param stack 人口 ItemStack
     * @param value 精神状态（0.0-1.0）
     */
    void setMentalState(ItemStack stack, double value);
}

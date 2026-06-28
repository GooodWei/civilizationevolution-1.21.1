package com.gooodwei.civilizationevolution.api.event;

import com.gooodwei.civilizationevolution.api.career.Career;
import net.neoforged.bus.api.Event;

/**
 * 职业注册事件，在 Career 构造函数注册职业后发出。
 * 附属模组可监听此事件以获知新职业的注册。
 */
public class CareerRegisterEvent extends Event {

    private final Career career;

    /**
     * 构造职业注册事件。
     * @param career 已完成注册的 Career 实例
     */
    public CareerRegisterEvent(Career career) {
        this.career = career;
    }

    /** @return 被注册的职业实例 */
    public Career getCareer() {
        return career;
    }
}

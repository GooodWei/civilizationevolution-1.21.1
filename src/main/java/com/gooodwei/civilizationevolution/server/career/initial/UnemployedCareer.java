package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;

/**
 * 无业游民职业（0 级基础职业）。
 * 无对应原版职业，无特殊技能，可从村民失业状态转换而来。
 */
public class UnemployedCareer extends Career {
    public UnemployedCareer() {
        super("unemployed", 0, null);
    }
}

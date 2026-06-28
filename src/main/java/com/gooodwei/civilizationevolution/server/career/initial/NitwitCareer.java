package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;

/**
 * 傻子职业（0 级基础职业）。
 * 无对应原版职业，无工作能力，不可升级。
 */
public class NitwitCareer extends Career {
    public NitwitCareer() {
        super("nitwit", 0, null);
    }
}

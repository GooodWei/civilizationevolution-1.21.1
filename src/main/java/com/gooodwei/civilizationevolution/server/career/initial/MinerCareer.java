package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;

/**
 * 矿工职业（2 级进阶职业）。
 * 父职业为石匠（mason），负责采石场和矿井的方块挖掘工作。
 * 通过 careers.yml 配置设置 parent=mason, tier=2。
 */
public class MinerCareer extends Career {
    public MinerCareer() {
        super("miner", 2, null);
    }
}

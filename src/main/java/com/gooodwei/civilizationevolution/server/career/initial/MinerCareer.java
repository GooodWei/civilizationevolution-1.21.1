package com.gooodwei.civilizationevolution.server.career.initial;

import com.gooodwei.civilizationevolution.api.career.Career;

/**
 * 矿工职业（1 级初始职业）。
 * 无对应原版村民职业，负责采石场和矿井的方块挖掘工作。
 */
public class MinerCareer extends Career {
    public MinerCareer() {
        super("miner", 1, null);
    }
}

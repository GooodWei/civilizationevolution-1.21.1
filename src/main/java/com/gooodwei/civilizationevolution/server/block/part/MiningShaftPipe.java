package com.gooodwei.civilizationevolution.server.block.part;

import net.minecraft.world.level.block.Block;

/**
 * 矿井管道 —— 无 BlockEntity 的结构方块，用于村庄采石场的矿井系统。
 *
 * <p>黑曜石级别硬度（50），需钻石及以上镐子挖掘，爆炸抗性极高（1200），
 * 无掉落物。不在创造模式物品栏中，仅作为多方块结构的一部分存在。
 */
public class MiningShaftPipe extends Block {

    public MiningShaftPipe(Properties properties) {
        super(properties);
    }
}

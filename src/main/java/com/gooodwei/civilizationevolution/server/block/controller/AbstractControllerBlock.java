package com.gooodwei.civilizationevolution.server.block.controller;
import com.gooodwei.civilizationevolution.server.block.machine.AbstractMachineBlock;


import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 控制器方块的抽象中间层 —— 在 {@link AbstractMachineBlock} 基础上
 * 添加信标光柱所需的 {@link #BEAM_ACTIVE} 方块状态属性。
 *
 * <p>继承层次：
 * <pre>
 * AbstractMachineBlock (FACING)
 *  └── AbstractControllerBlock (BEAM_ACTIVE)  ← 本类
 *       ├── PrimitiveController
 *       └── VillageController
 * </pre>
 *
 * <p>非控制器机器（营地、牧场等）直接继承 {@link AbstractMachineBlock}，
 * 不受此属性影响。
 *
 * <p>{@code BEAM_ACTIVE} 由服务端 Tick 根据以下条件切换：
 * <ol>
 *   <li>控制器内有文明核心物品（UUID 非空）</li>
 *   <li>方块收到红石信号（任意强度）</li>
 *   <li>多方块结构已成型（{@link IMultiBlockMachine#isStructureFormed()}）</li>
 * </ol>
 * 三者同时满足时设为 {@code true}，否则为 {@code false}。
 * 客户端 {@link com.gooodwei.civilizationevolution.client.renderer.ControllerBeamRenderer}
 * 读取此属性决定是否渲染信标光柱。
 *
 * @see AbstractMachineBlock
 * @see PrimitiveController
 * @see VillageController
 */
public abstract class AbstractControllerBlock extends AbstractMachineBlock {

    /**
     * 信标光柱激活状态属性。
     * 客户端渲染器读取此值决定是否绘制彩虹旋转光束。
     */
    public static final BooleanProperty BEAM_ACTIVE = BooleanProperty.create("beam_active");

    /**
     * @param properties 方块属性（硬度、爆破阻力等）
     */
    protected AbstractControllerBlock(Properties properties) {
        super(properties);
        // 注册默认方块状态：朝向北方，光束关闭
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(BEAM_ACTIVE, false));
    }

    /**
     * 注册方块状态定义，包含水平朝向和光束激活状态两个属性。
     *
     * @param builder 方块状态构建器
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, BEAM_ACTIVE);
    }
}

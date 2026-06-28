package com.gooodwei.civilizationevolution.server.block;

import com.gooodwei.civilizationevolution.server.blockentity.abstractmachine.AbstractHuntingGroundBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.fieldmachine.HuntingGroundBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 狩猎场方块 —— 具有 BlockEntity 和冲突检测的机器方块。
 *
 * <p>狩猎场在放置和打开 GUI 时会触发区块内同类型机器的冲突检测（通过 {@link com.gooodwei.civilizationevolution.tags.ModTags} 中的
 * {@code hunting_ground_conflicts} Block Tag 匹配）。冲突时所有相关方块显示红色粒子边框并暂停工作。
 *
 * <p>ticker 仅在服务端运行，且仅当方块实体已绑定时才执行 ServerTick。</p>
 */
public class HuntingGround extends AbstractMachineBlock {
    /** 序列化编解码器 */
    public static final MapCodec<HuntingGround> CODEC = simpleCodec(HuntingGround::new);

    /**
     * @param properties 方块属性（硬度、爆破阻力等）
     */
    public HuntingGround(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends AbstractMachineBlock> codec() {
        return CODEC;
    }

    /**
     * 创建狩猎场方块实体。
     *
     * @param blockPos   方块坐标
     * @param blockState 方块状态
     * @return 新的 {@link HuntingGroundBlockEntity} 实例
     */
    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new HuntingGroundBlockEntity(blockPos, blockState);
    }

    /**
     * 方块放置后触发冲突扫描（仅服务端）。
     *
     * @param level  所在世界
     * @param pos    方块坐标
     * @param state  方块状态
     * @param placer 放置者（可为 {@code null}）
     * @param stack  使用的物品堆
     */
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof AbstractHuntingGroundBlockEntity be) {
            be.onPlacedOrOpened((ServerLevel) level);
        }
    }

    /**
     * 获取方块实体的 Ticker（仅服务端运行）。
     *
     * <p>始终调用 serverTick 以处理粒子边框显示和工作进度推进。
     * 工作进度仅在已绑定时推进，粒子边框不受绑定状态影响。</p>
     */
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null; // 仅服务端 tick
        }
        return createTickerHelper(type, BlockEntityRegistry.HUNT_GROUND.get(),
                (lvl, pos, st, be) -> HuntingGroundBlockEntity.serverTick(lvl, pos, st, be));
    }

    /**
     * 打开 GUI 前的钩子：扫描冲突并刷新粒子边框。
     *
     * @param level 所在世界
     * @param pos   方块坐标
     */
    @Override
    protected void preOpenMenu(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof HuntingGroundBlockEntity hbe) {
            hbe.onPlacedOrOpened((ServerLevel) level);
        }
    }
}

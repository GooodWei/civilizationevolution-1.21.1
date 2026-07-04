package com.gooodwei.civilizationevolution.server.block.machine;

import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.multiblock.PrimitiveDoctorCabinBlockEntity;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 原始诊所多方块控制器方块 —— Tier 0。
 *
 * <p>2×2×2 多方块结构，控制器位于结构左下角。
 * 右键打开 GUI（需结构完整成型），破坏结构时弹出所有内部物品。
 */
public class PrimitiveDoctorCabin extends AbstractMachineBlock {

    public static final MapCodec<PrimitiveDoctorCabin> CODEC =
            simpleCodec(PrimitiveDoctorCabin::new);

    public PrimitiveDoctorCabin(Properties properties) {
        super(properties);
    }

    // ==================== BlockEntity ====================

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PrimitiveDoctorCabinBlockEntity(
                BlockEntityRegistry.PRIMITIVE_DOCTOR_CABIN.get(), pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return  RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                    BlockEntityType<T> type) {
        return level.isClientSide() ? null :
                createTickerHelper(type, BlockEntityRegistry.PRIMITIVE_DOCTOR_CABIN.get(),
                        PrimitiveDoctorCabinBlockEntity::serverTick);
    }

    // ==================== Codec & Tier ====================

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public Tier getTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    // ==================== 交互钩子 ====================

    @Override
    protected void preOpenMenu(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PrimitiveDoctorCabinBlockEntity cabin && !cabin.isStructureFormed()) {
            cabin.validateStructure();
        }
    }

    @Override
    protected boolean canOpenMenu(Level level, BlockPos pos, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PrimitiveDoctorCabinBlockEntity cabin) {
            if (cabin.hasParseError()) {
                player.sendSystemMessage(Component.literal(cabin.getParseError()).withStyle(ChatFormatting.RED));
                return false;
            }
            if (!cabin.isStructureFormed()) {
                player.displayClientMessage(
                        Component.translatable("msg.civilizationevolution.structure_incomplete"), true);
                return false;
            }
        }
        return true;
    }

    // ==================== 破坏处理 ====================

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                             BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PrimitiveDoctorCabinBlockEntity cabin) {
                cabin.onStructurePartBroken(pos);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

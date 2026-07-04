package com.gooodwei.civilizationevolution.server.block.machine;

import com.gooodwei.civilizationevolution.api.IPopulationMachine;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.server.blockentity.multiblock.PrimitiveStoragePitBlockEntity;
import com.gooodwei.civilizationevolution.server.item.CivilizationCoreExtractorItem;
import com.gooodwei.civilizationevolution.server.item.ConnectorItem;
import com.gooodwei.civilizationevolution.server.item.ProjectorItem;
import com.gooodwei.civilizationevolution.server.registry.TieredBlockItem;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 储物坑控制器方块 —— Tier 0 原始时代多方块储物系统。
 *
 * <p>可变尺寸的中空盒子结构，顶部面中心放置控制器，
 * 内部空气对应物理槽位，使用类型存储模型（类似 AE2/Storage Drawers）。
 *
 * <p>右键打开 GUI（需结构完整成型），破坏结构时物品保留在掉落物中。
 */
public class PrimitiveStoragePit extends AbstractMachineBlock {
    public static final MapCodec<PrimitiveStoragePit> CODEC = simpleCodec(PrimitiveStoragePit::new);

    public PrimitiveStoragePit(Properties properties) {
        super(properties);
    }

    /**
     * 获取此方块的 Tier 等级。
     *
     * <p>供 {@link TieredBlockItem}
     * 在物品 tooltip 中显示时代名称，与 BE 的
     * {@link IPopulationMachine#getTier()} 保持一致。
     *
     * @return 此方块对应的 Tier 等级
     */
    @Override
    public Tier getTier() {
        return CivilizationTiers.PRIMITIVE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new PrimitiveStoragePitBlockEntity(blockPos, blockState);
    }

    /**
     * 储物坑无定时 tick 逻辑，始终返回 null。
     * 结构验证仅在放置时和开 GUI 时触发。
     */
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T>
            type) {
        return null;
    }

    // ==================== 交互 ====================

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state,
                                                       @NotNull Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        // 特殊物品 → 跳过 GUI
        if (stack.getItem() instanceof ConnectorItem
                || stack.getItem() instanceof ProjectorItem
                || (stack.getItem() instanceof CivilizationCoreExtractorItem && player.isShiftKeyDown())
                || player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PrimitiveStoragePitBlockEntity pit) {
                // 每次打开 GUI 都重新验证结构（防止墙壁被破坏或内部被填充后仍能打开）
                boolean valid = pit.validateStructure();
                if (!pit.isStructureFormed()) {
                    String errorKey = pit.getValidationError();
                    player.displayClientMessage(
                            Component.translatable(errorKey != null
                                    ? errorKey : "msg.civilizationevolution.structure_incomplete"), true);
                    return ItemInteractionResult.sidedSuccess(false);
                }
                preOpenMenu(level, pos);
                player.openMenu(pit, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeInt(pit.getContainerSize());
                });
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    // ==================== 破坏掉落全部物品 ====================

    /**
     * 破坏储物坑时弹出内部所有物品，方块本身作为纯净物品掉落。
     *
     * <p>不保留 NBT 数据到方块掉落物（与潜影盒不同）。结构成型时，
     * 遍历全部槽位，将 oversized 物品栈（count 可达 1728）分批按
     * {@code maxStackSize}（64）拆分弹出，确保掉落物可被原版系统正确处理。
     *
     * <p><b>注意：不调用 {@code super.playerWillDestroy()}</b>。
     * {@link AbstractMachineBlock#playerWillDestroy} 会额外产生一个带
     * {@code collectComponents()} 数据的方块掉落物，导致双倍掉落。
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!level.isClientSide && !player.isCreative()) {
            // 弹出储物坑中的所有物品
            if (be instanceof PrimitiveStoragePitBlockEntity pit && pit.isStructureFormed()) {
                for (int i = 0; i < pit.getContainerSize(); i++) {
                    ItemStack stack = pit.getItem(i);
                    if (stack.isEmpty()) continue;

                    int maxPerStack = stack.getMaxStackSize();
                    // 超大堆叠分批弹出
                    while (stack.getCount() > maxPerStack) {
                        ItemStack split = stack.split(maxPerStack);
                        ItemEntity itemDrop = new ItemEntity(level,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, split);
                        itemDrop.setDefaultPickUpDelay();
                        level.addFreshEntity(itemDrop);
                    }
                    if (!stack.isEmpty()) {
                        ItemEntity itemDrop = new ItemEntity(level,
                                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
                        itemDrop.setDefaultPickUpDelay();
                        level.addFreshEntity(itemDrop);
                    }
                }
                pit.clearContent();
            }
            // 掉落方块本身（不含 NBT 数据）
            ItemEntity blockDrop = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(this));
            blockDrop.setDefaultPickUpDelay();
            level.addFreshEntity(blockDrop);
        }
        // 不调用 super.playerWillDestroy() —— AbstractMachineBlock 会额外掉落物品
        return state;
    }
}

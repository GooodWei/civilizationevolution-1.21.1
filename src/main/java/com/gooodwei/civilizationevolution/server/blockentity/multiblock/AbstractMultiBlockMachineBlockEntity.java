package com.gooodwei.civilizationevolution.server.blockentity.multiblock;
import com.gooodwei.civilizationevolution.server.blockentity.machine.AbstractMachineBlockEntity;


import com.gooodwei.civilizationevolution.api.IMultiBlockMachine;
import com.gooodwei.civilizationevolution.api.MultiBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 多方块机器通用父类。
 *
 * <p>位于 {@link AbstractMachineBlockEntity} 和具体多方块机器
 * （如 {@code AbstractHospitalBlockEntity}）之间。
 * 多方块核心逻辑已提取到 {@link IMultiBlockMachine} 接口的 default 方法中，
 * 本类仅持有 {@link MultiBlockState} 状态对象并实现接口契约。
 *
 * <p>子类只需覆写 {@link #getConfigKey()} 返回
 * {@link MultiBlockConfig} 中对应的结构 key。
 *
 * <h3>JSON 结构定义格式</h3>
 * <p>配置文件位于 {@code config/civilizationevolution/multi_blocks.json}，
 * 详见 {@link IMultiBlockMachine} 和 {@link MultiBlockConfig}。
 *
 * @see IMultiBlockMachine
 * @see IMultiBlockPart
 * @see MultiBlockConfig
 */
public abstract class AbstractMultiBlockMachineBlockEntity
        extends AbstractMachineBlockEntity
        implements IMultiBlockMachine {

    /** 多方块运行时状态（结构成型标识、位置缓存、解析结果等） */
    private final MultiBlockState mbs = new MultiBlockState();

    // ==================== 构造器 ====================

    protected AbstractMultiBlockMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int size) {
        super(type, pos, state, size);
    }

    // ==================== 子类必须覆写 ====================

    /**
     * 返回 {@link MultiBlockConfig} 中对应的结构标识 key。
     *
     * @return 配置文件中的结构 key（如 {@code "primitive_doctor_cabin"}）
     */
    @Override
    public abstract String getConfigKey();

    // ==================== IMultiBlockMachine 契约 ====================

    @Override
    public MultiBlockState mbs() {
        return mbs;
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    public void dropAllItems() {
        if (getLevel() instanceof ServerLevel sl) {
            for (int i = 0; i < getContainerSize(); i++) {
                ItemStack stack = getItem(i);
                if (!stack.isEmpty()) {
                    Block.popResource(sl, worldPosition, stack);
                    setItem(i, ItemStack.EMPTY);
                }
            }
        }
    }

    // ==================== 生命周期 ====================

    @Override
    public void onLoad() {
        super.onLoad();
        // 延迟 40 tick（2 秒）后重新验证结构，给周围 chunk 加载留时间
        onMultiBlockLoad();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveMultiBlockNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadMultiBlockNBT(tag);
    }
}

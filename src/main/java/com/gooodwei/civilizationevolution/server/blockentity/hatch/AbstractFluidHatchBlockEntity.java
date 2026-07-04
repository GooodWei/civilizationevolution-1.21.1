package com.gooodwei.civilizationevolution.server.blockentity.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

/**
 * 流体接口的抽象 BE 基类。
 *
 * <p>0 个物品槽位（纯流体存储），默认容量 8000 mB。
 * 提供 IFluidHandler 供物流管道交互，子类决定 fill/drain 方向。
 * 无 GUI（createMenu 返回 null，右键无界面）。
 */
public abstract class AbstractFluidHatchBlockEntity extends AbstractHatchBlockEntity {

    /** 当前储液量（mB） */
    protected long fluidAmount = 0;
    /** 当前存储的流体类型（持久化） */
    protected Fluid storedFluid = Fluids.EMPTY;

    protected AbstractFluidHatchBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, 0);
    }

    // ==================== Container（0 槽位） ====================

    @Override
    public int getMaxStackSize() {
        return 0;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return false;
    }

    // getPartType() 由子类覆写为 TYPE_FLUID_INPUT_HATCH 或 TYPE_FLUID_OUTPUT_HATCH

    @Override
    protected String getContainerName() {
        return "container.civilizationevolution.fluid_hatch";
    }

    // ==================== MenuProvider（无 GUI） ====================

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return null; // 流体接口通过 IFluidHandler 交互，无 GUI
    }

    // ==================== IFluidHandler（子类覆写方向逻辑） ====================

    /** 获取此 hatch 的流体处理器 */
    public abstract IFluidHandler getFluidHandler();

    /** 获取当前储液量（mB） */
    public long getFluidAmount() {
        return fluidAmount;
    }

    /** 获取当前存储的流体类型 */
    public Fluid getStoredFluid() {
        return storedFluid;
    }

    /**
     * 获取此 hatch 的储罐容量（mB）。
     * 子类可覆写以提供不同容量（如 Tier 1 更大）。
     *
     * @return 储罐容量（mB），默认 8000（8 桶）
     */
    protected long getDefaultTankCapacity() {
        return 8000;
    }

    /**
     * 安全地将 long 型液量截断为 int（上限 Integer.MAX_VALUE）。
     * 避免多次重复的 {@code (int) Math.min(value, Integer.MAX_VALUE)} 模式。
     */
    protected static int toIntAmount(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    /**
     * 检查是否可以向此仓室填充指定流体。
     * 同时校验流体类型兼容性和容量余量，供所有填充路径（管道、桶、多方块控制器）统一使用。
     *
     * @param stack 待填充的流体
     * @return true 表示允许填充（仓室为空或流体相同，且未满）
     */
    public boolean canFill(FluidStack stack) {
        if (stack.isEmpty()) return false;
        // 流体类型兼容：仓室为空或已有相同流体
        if (storedFluid != Fluids.EMPTY && !stack.is(storedFluid)) return false;
        // 容量检查
        if (fluidAmount >= getDefaultTankCapacity()) return false;
        return true;
    }

    /**
     * 供多方块控制器内部调用的直接提取方法。
     * 绕过方向限制直接操作储罐，输入/输出接口均可使用。
     *
     * <p><b>修复了 drain bug</b>：在清空 storedFluid 之前保存引用，
     * 确保返回的 FluidStack 携带正确的流体类型。
     */
    public FluidStack drainInternal(int maxDrain) {
        if (fluidAmount <= 0 || maxDrain <= 0) return FluidStack.EMPTY;
        int toDrain = Math.min(maxDrain, toIntAmount(fluidAmount));
        Fluid savedFluid = storedFluid; // 保存引用，防止清空后丢失
        fluidAmount -= toDrain;
        if (fluidAmount <= 0) {
            storedFluid = Fluids.EMPTY;
        }
        setChanged();
        return new FluidStack(savedFluid, toDrain);
    }

    /**
     * 供多方块控制器或桶交互直接填充的方法。
     * 绕过方向限制直接操作储罐，输入/输出接口均可使用。
     */
    public int fillInternal(FluidStack stack) {
        if (!canFill(stack)) return 0;
        long capacity = getDefaultTankCapacity();
        long canFill = capacity - fluidAmount;
        if (canFill <= 0) return 0;
        int toFill = (int) Math.min(canFill, (long) stack.getAmount());
        if (storedFluid == Fluids.EMPTY) {
            storedFluid = stack.getFluid();
        }
        fluidAmount += toFill;
        setChanged();
        return toFill;
    }

    /**
     * 创建带方向限制的流体处理器。
     *
     * <p>流体接口有两类：
     * <ul>
     *   <li><b>输入接口（isInput=true）</b>：允许外部管道填充（fill），拒绝外部提取（drain 返回空）</li>
     *   <li><b>输出接口（isInput=false）</b>：允许外部管道提取（drain），拒绝外部填充（fill 返回 0）</li>
     * </ul>
     *
     * <p>内部调用（多方块控制器、桶交互）使用 {@link #fillInternal} / {@link #drainInternal}，
     * 绕过此方向限制。
     *
     * @param isInput true 为输入接口，false 为输出接口
     * @return 带方向限制的 IFluidHandler 实例
     */
    protected IFluidHandler createFluidHandler(boolean isInput) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int tank) {
                if (fluidAmount <= 0) return FluidStack.EMPTY;
                return new FluidStack(storedFluid, toIntAmount(fluidAmount));
            }

            @Override
            public int getTankCapacity(int tank) {
                return toIntAmount(getDefaultTankCapacity());
            }

            @Override
            public boolean isFluidValid(int tank, FluidStack stack) {
                return storedFluid == Fluids.EMPTY || stack.is(storedFluid);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (!isInput) return 0; // 输出接口拒绝外部填充
                if (!canFill(resource)) return 0;
                long capacity = getDefaultTankCapacity();
                long canFill = capacity - fluidAmount;
                if (canFill <= 0) return 0;
                int toFill = (int) Math.min(canFill, (long) resource.getAmount());
                if (action.execute()) {
                    if (storedFluid == Fluids.EMPTY) {
                        storedFluid = resource.getFluid();
                    }
                    fluidAmount += toFill;
                    setChanged();
                }
                return toFill;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (isInput) return FluidStack.EMPTY; // 输入接口拒绝外部提取
                if (resource.isEmpty() || fluidAmount <= 0) return FluidStack.EMPTY;
                if (storedFluid != Fluids.EMPTY && !resource.is(storedFluid)) return FluidStack.EMPTY;
                int toDrain = Math.min(resource.getAmount(), toIntAmount(fluidAmount));
                Fluid savedFluid = storedFluid; // 保存引用，防清空后丢失
                if (action.execute()) {
                    fluidAmount -= toDrain;
                    if (fluidAmount <= 0) storedFluid = Fluids.EMPTY;
                    setChanged();
                }
                return new FluidStack(savedFluid, toDrain);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                if (isInput) return FluidStack.EMPTY; // 输入接口拒绝外部提取
                if (fluidAmount <= 0 || maxDrain <= 0) return FluidStack.EMPTY;
                int toDrain = Math.min(maxDrain, toIntAmount(fluidAmount));
                Fluid savedFluid = storedFluid; // 保存引用，防清空后丢失
                if (action.execute()) {
                    fluidAmount -= toDrain;
                    if (fluidAmount <= 0) storedFluid = Fluids.EMPTY;
                    setChanged();
                }
                return new FluidStack(savedFluid, toDrain);
            }
        };
    }

    // ==================== 桶交互（供 Block.useItemOn 调用） ====================

    /**
     * 处理玩家手持桶/流体容器右键流体舱室的交互。
     * 由 {@code AbstractFluidInputHatchBlock} 和 {@code AbstractFluidOutputHatchBlock}
     * 的 {@code useItemOn} 调用。
     *
     * <p>两种操作方向：
     * <ol>
     *   <li><b>手持流体容器（水桶等）→ 向 hatch 注液</b>：
     *       通过 {@code IFluidHandlerItem} 抽取手持物品中的流体，
     *       调用 {@link #fillInternal} 注入 hatch（绕过方向限制）</li>
     *   <li><b>手持空桶 → 从 hatch 捞液</b>：
     *       检查 hatch 中是否有流体且有对应的桶物品，
     *       调用 {@link #drainInternal} 抽取 1000 mB</li>
     * </ol>
     *
     * @param level   当前世界
     * @param pos     舱室方块坐标
     * @param player  交互玩家
     * @param hand    交互手
     * @param stack   手持物品
     * @param hatchBe 舱室方块实体
     * @return SUCCESS 表示已处理桶交互，PASS_TO_DEFAULT_BLOCK_INTERACTION 表示未处理
     */
    public static ItemInteractionResult handleBucketInteraction(Level level, BlockPos pos,
                                                                 Player player, InteractionHand hand,
                                                                 ItemStack stack,
                                                                 AbstractFluidHatchBlockEntity hatchBe) {
        // ===== 1. 手持流体容器（水桶、岩浆桶等）→ 向 hatch 注液 =====
        IFluidHandlerItem itemFluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (itemFluidHandler != null) {
            // 模拟抽取手持物品中的全部流体
            FluidStack simulatedDrain = itemFluidHandler.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
            if (!simulatedDrain.isEmpty()) {
                int toFill = Math.min(simulatedDrain.getAmount(), 1000); // 每次最多 1 桶
                FluidStack fillStack = new FluidStack(simulatedDrain.getFluid(), toFill);

                // 统一校验：流体类型兼容 + 容量
                if (!hatchBe.canFill(fillStack)) {
                    return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
                }

                if (!level.isClientSide) {
                    // 先填充仓室，再按实际填充量抽取手持物品（避免双重填充）
                    int actuallyFilled = hatchBe.fillInternal(new FluidStack(fillStack.getFluid(), toFill));
                    if (actuallyFilled > 0) {
                        itemFluidHandler.drain(new FluidStack(fillStack.getFluid(), actuallyFilled), IFluidHandler.FluidAction.EXECUTE);
                        if (!player.isCreative()) {
                            player.setItemInHand(hand, itemFluidHandler.getContainer());
                        }
                    }
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        // ===== 2. 空桶 → 从 hatch 捞流体 =====
        if (stack.is(Items.BUCKET)) {
            Fluid storedFluid = hatchBe.getStoredFluid();
            if (storedFluid != Fluids.EMPTY && hatchBe.getFluidAmount() >= 1000) {
                Item bucketItem = storedFluid.getBucket();
                if (bucketItem != Items.AIR) {
                    if (!level.isClientSide) {
                        hatchBe.drainInternal(1000);
                        stack.shrink(1);
                        ItemStack filledBucket = new ItemStack(bucketItem);
                        if (stack.isEmpty()) {
                            player.setItemInHand(hand, filledBucket);
                        } else if (!player.getInventory().add(filledBucket)) {
                            player.drop(filledBucket, false);
                        }
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }

        // ===== 3. 手持满桶 → 空桶（桶中有流体，但可能不是标准流体容器格式） =====
        // 已由步骤 1 通过 IFluidHandlerItem 处理，此处仅兜底
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    // ==================== NBT ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("FluidAmount", fluidAmount);
        if (storedFluid != Fluids.EMPTY) {
            tag.putString("StoredFluid", BuiltInRegistries.FLUID.getKey(storedFluid).toString());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluidAmount = tag.getLong("FluidAmount");
        if (tag.contains("StoredFluid")) {
            ResourceLocation rl = ResourceLocation.parse(tag.getString("StoredFluid"));
            storedFluid = BuiltInRegistries.FLUID.getOptional(rl).orElse(Fluids.EMPTY);
        } else {
            storedFluid = Fluids.EMPTY;
        }
    }
}

package com.gooodwei.civilizationevolution.server.blockentity.multiblock;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.server.config.CivilizationMachineConfig;
import com.gooodwei.civilizationevolution.api.inventory.OversizedStackHandler;
import com.gooodwei.civilizationevolution.server.menu.machine.PrimitiveStoragePitMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PrimitiveStoragePitBlockEntity extends BaseContainerBlockEntity implements MenuProvider {
    private static final int STACKS_PER_TYPE = 27; // 每种类型最多 27 组

    // ==================== 结构状态字段 ====================

    private int interiorWidth = 0;   // 内部宽度（=深度，奇数）
    private int interiorHeight = 0;  // 内部高度
    private boolean structureFormed = false;
    @Nullable
    private String validationError = null; // validateStructure() 失败原因（用于 GUI 提示）

    // ==================== 库存（使用 OversizedStackHandler，支持 count > 99） ====================

    private final OversizedStackHandler handler = new OversizedStackHandler(0);

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> interiorWidth;
                case 1 -> interiorHeight;
                case 2 -> structureFormed ? 1 : 0;
                case 3 -> getUsedSlotCount();
                case 4 -> getContainerSize();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // scrollOffset 由客户端 Screen 自行维护，不通过 ContainerData 同步
        }

        @Override
        public int getCount() {
            return 5; // 5 个同步字段（不含 scrollOffset）
        }
    };

    // ==================== 结构验证 ====================

    /**
     * 程序化扫描验证储物坑多方块结构。与固定模板的 JSON pattern 验证不同，
     * 本方法从控制器位置向四周扫描，自动检测可变尺寸的中空盒子。
     *
     * @return true 表示结构有效且已记录尺寸
     */
    public boolean validateStructure() {
        Level level1 = this.level;
        if (level1 == null || level1.isClientSide) {
            // CivilizationEvolution.LOGGER.warn("[储物坑] validateStructure 失败：level={}, isClientSide={}",
            //         level1, level1 != null ? level1.isClientSide : "N/A");
            return false;
        }

        validationError = null;

        // 验证前先标记为无效，确保步骤检查不依赖旧状态
        if (structureFormed) {
            structureFormed = false;
            interiorWidth = 0;
            interiorHeight = 0;
            setChanged();
        }

        int cx = worldPosition.getX();
        int cy = worldPosition.getY();
        int cz = worldPosition.getZ();
        String key = CivilizationMachineConfig.PRIMITIVE_STORAGE_PIT;
        // CivilizationEvolution.LOGGER.info("[储物坑] 开始验证 at ({},{},{})", cx, cy, cz);

        // 1. 控制器下方必须是空气（内部空间起点）
        BlockState belowState = level1.getBlockState(new BlockPos(cx, cy - 1, cz));
        if (!belowState.isAir()) {
            // CivilizationEvolution.LOGGER.warn("[储物坑] 步骤1 失败：下方方块不是空气，是 {}", belowState.getBlock());
            return false;
        }
        // CivilizationEvolution.LOGGER.info("[储物坑] 步骤1 通过：下方是空气");
        // 2. 向下扫描找到地板 Y（非空气方块即地板，包括基岩）
        int floorY = cy - 1;
        while (floorY > level1.getMinBuildHeight()
                && level1.getBlockState(new BlockPos(cx, floorY, cz)).isAir()) {
            floorY--;
        }
        // 扫描结束后，如果最底部的方块仍然是空气 → 掉入虚空，无地板
        if (level1.getBlockState(new BlockPos(cx, floorY, cz)).isAir()) {
            // CivilizationEvolution.LOGGER.warn("[储物坑] 步骤2 失败：扫描到世界底部仍未找到地板 floorY={}", floorY);
            return false;
        }
        int iHeight = cy - 1 - floorY;
        // CivilizationEvolution.LOGGER.info("[储物坑] 步骤2 通过：floorY={}, 内部高度 iHeight={}", floorY, iHeight);
        // 3. 验证高度约束
        int maxHeight = CivilizationMachineConfig.getStoragePitMaxHeight(key);
        if (iHeight < 1 || iHeight > maxHeight) {
            // CivilizationEvolution.LOGGER.warn("[储物坑] 步骤3 失败：高度={} 不在 [1,{}] 范围内", iHeight, maxHeight);
            return false;
        }
        // CivilizationEvolution.LOGGER.info("[储物坑] 步骤3 通过：高度约束满足");
        // 4. 水平扫描找到墙壁边界（使用任意一个内部 Y 层）
        int scanY = floorY + 1;
        // +X 方向
        int rightX = cx;
        while (level1.getBlockState(new BlockPos(rightX + 1, scanY, cz)).isAir()) rightX++;
        rightX++; // 墙壁方块位置
        // -X 方向
        int leftX = cx;
        while (level1.getBlockState(new BlockPos(leftX - 1, scanY, cz)).isAir()) leftX--;
        leftX--; // 墙壁方块位置
        // +Z 方向
        int frontZ = cz;
        while (level1.getBlockState(new BlockPos(cx, scanY, frontZ + 1)).isAir()) frontZ++;
        frontZ++;
        // -Z 方向
        int backZ = cz;
        while (level1.getBlockState(new BlockPos(cx, scanY, backZ - 1)).isAir()) backZ--;
        backZ--;
        int iWidthX = rightX - leftX - 1;
        int iWidthZ = frontZ - backZ - 1;
        // CivilizationEvolution.LOGGER.info("[储物坑] 步骤4：边界 leftX={}, rightX={}, backZ={}, frontZ={}, 内部宽X={}, 内部宽Z={}",
        //         leftX, rightX, backZ, frontZ, iWidthX, iWidthZ);
        // 5. 验证宽度约束（正方形 + 奇数 + 范围）
        int maxWidth = CivilizationMachineConfig.getStoragePitMaxWidth(key);
        if (iWidthX != iWidthZ) {
            // CivilizationEvolution.LOGGER.warn("[储物坑] 步骤5 失败：宽X({}) != 宽Z({})，不是正方形", iWidthX, iWidthZ);
            return false;
        }
        if (iWidthX < 3 || iWidthX > maxWidth) {
            // CivilizationEvolution.LOGGER.warn("[储物坑] 步骤5 失败：宽度={} 不在 [3,{}] 范围内", iWidthX, maxWidth);
            return false;
        }
        if (iWidthX % 2 == 0) {
            // CivilizationEvolution.LOGGER.warn("[储物坑] 步骤5 失败：宽度={} 是偶数", iWidthX);
            return false;
        }
        // CivilizationEvolution.LOGGER.info("[储物坑] 步骤5 通过：正方形 {}×{}", iWidthX, iWidthZ);

        // 6. 遍历整个外边界盒，逐一验证每个方块（任一不符立即退出）
        List<String> wallTags = CivilizationMachineConfig.getWallBlockTags(key);
        // CivilizationEvolution.LOGGER.info("[储物坑] 步骤6：开始遍历边界盒，wallTags={}", wallTags);
        int extXMin = leftX;
        int extXMax = rightX;
        int extYMin = floorY;
        int extYMax = cy;
        int extZMin = backZ;
        int extZMax = frontZ;
        for (int x = extXMin; x <= extXMax; x++) {
            for (int y = extYMin; y <= extYMax; y++) {
                for (int z = extZMin; z <= extZMax; z++) {
                    // 跳过控制器自身
                    if (x == cx && y == cy && z == cz) continue;

                    BlockState bs = level1.getBlockState(new BlockPos(x, y, z));
                    boolean isSurface = (x == extXMin || x == extXMax
                            || z == extZMin || z == extZMax
                            || y == extYMin || y == extYMax);

                    if (isSurface) {
                        // 表面方块：必须是有效墙壁
                        if (!isValidWallBlock(bs, wallTags, new BlockPos(x, y, z))) {
                            // CivilizationEvolution.LOGGER.warn("[储物坑] 步骤6 失败：墙壁方块 ({},{},{}) 无效，方块={}",
                            //         x, y, z, bs.getBlock());
                            return false;
                        }
                    } else {
                        // 内部方块：必须是空气
                        if (!bs.isAir()) {
                            // CivilizationEvolution.LOGGER.warn("[储物坑] 步骤6 失败：内部方块 ({},{},{}) 不是空气，是 {}",
                            //         x, y, z, bs.getBlock());
                            return false;
                        }
                    }
                }
            }
        }
        // 7. 通过 → 检查容量变化
        // CivilizationEvolution.LOGGER.info("[储物坑] 步骤6 通过：所有方块验证完成");
        int newSize = iWidthX * iWidthX * iHeight;
        int oldHandlerSize = handler.getSlots();

        if (newSize < oldHandlerSize) {
            // 体积缩小：检查是否有物品在新尺寸之外的槽位中
            NonNullList<ItemStack> stacks = handler.getStacks();
            for (int i = newSize; i < oldHandlerSize; i++) {
                if (!stacks.get(i).isEmpty()) {
                    // CivilizationEvolution.LOGGER.warn(
                    //         "[储物坑] 结构缩小被拒绝：槽位 {} 有物品，新尺寸={} 旧尺寸={}",
                    //         i, newSize, oldHandlerSize);
                    validationError = "msg.civilizationevolution.storage_pit_insufficient_space";
                    return false;
                }
            }
            // CivilizationEvolution.LOGGER.info("[储物坑] 结构缩小：{}→{}，多余槽位为空，允许缩小",
            //         oldHandlerSize, newSize);
        }

        // 通过 → 记录尺寸（无论缩小/扩大/不变都走 rebuildInventory 迁移物品）
        this.interiorWidth = iWidthX;
        this.interiorHeight = iHeight;
        this.structureFormed = true;
        rebuildInventory();
        setChanged();
        // CivilizationEvolution.LOGGER.info("[储物坑] 验证成功！内部尺寸 {}×{}×{}, 总槽位={}",
        //         iWidthX, iWidthX, iHeight, getContainerSize());
        return true;
    }

    // ==================== 库存管理 ====================

    /** 结构成型后重建物品槽位列表，保留已有物品 */
    private void rebuildInventory() {
        int newSize = getContainerSize();
        // 保存旧栈
        NonNullList<ItemStack> oldStacks = NonNullList.withSize(handler.getSlots(), ItemStack.EMPTY);
        for (int i = 0; i < handler.getSlots(); i++) {
            oldStacks.set(i, handler.getStackInSlot(i).copy());
        }
        // 调整大小
        handler.setSize(newSize);
        handler.setBaseSlotLimit(64 * STACKS_PER_TYPE);
        // 恢复已有物品
        for (int i = 0; i < Math.min(oldStacks.size(), newSize); i++) {
            handler.setStackInSlot(i, oldStacks.get(i));
        }
    }

    /**
     * 验证墙壁方块是否合法。
     * 1. 完整碰撞箱 + 非流体（硬性要求）
     * 2. 若 wallTags 非空：方块必须匹配至少一个标签
     */
    private boolean isValidWallBlock(BlockState state, List<String> wallTags, BlockPos pos) {
        // 硬性要求
        if (level != null && !state.isCollisionShapeFullBlock(level, pos))
            return false;
        if (!state.getFluidState().isEmpty()) return false;
        // 标签白名单（空 = 不限制）
        if (wallTags.isEmpty()) return true;
        // 检查标签匹配
        for (String tag : wallTags) {
            if (state.is(TagKey.create(Registries.BLOCK, ResourceLocation.parse(tag)))) {
                return true;
            }
        }
        return false;
    }

    public PrimitiveStoragePitBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.PRIMITIVE_STORAGE_PIT.get(), pos, blockState);
    }

    /**
     * 方块放置后延迟触发结构验证。
     * 延迟 40 tick 以确保周围 chunk 加载完毕。
     */
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            // CivilizationEvolution.LOGGER.info("[储物坑] onLoad 触发，计划延迟验证 at {}", worldPosition);
            // 改为在玩家首次右键时触发验证（见 PrimitiveStoragePitBlock.useItemOn）
        }
    }

    // ==================== Container 接口（委托给 OversizedStackHandler） ====================

    @Override
    protected NonNullList<ItemStack> getItems() {
        return handler.getStacks();
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        handler.setStacks(nonNullList);
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return null;
    }

    @Override
    public int getContainerSize() {
        return structureFormed ? interiorWidth * interiorWidth * interiorHeight : 0;
    }

    @Override
    public boolean stillValid(Player player) {
        // 结构未成型时不可打开 GUI
        if (!structureFormed) return false;
        return super.stillValid(player);
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        ItemStack existing = handler.getStackInSlot(slot);
        // 空槽位可放入任意物品；已有物品的槽位仅接受同种物品
        return existing.isEmpty() || ItemStack.isSameItemSameComponents(existing, stack);
    }

    @Override
    public int getMaxStackSize() {
        return handler.getBaseSlotLimit(); // 64 * 27 = 1728
    }

    // ==================== 物品操作 ====================

    /** 已使用槽位数（非空物理槽位） */
    public int getUsedSlotCount() {
        return (int) handler.getStacks().stream().filter(s -> !s.isEmpty()).count();
    }

    /**
     * 尝试将物品插入储物坑（溢出分配算法）。
     * 优先填入已有同种物品的槽位，不足时使用空槽位。
     *
     * @return 无法放入的剩余物品（全部放入时返回 EMPTY）
     */
    public ItemStack tryInsert(ItemStack stack) {
        if (!structureFormed) return stack.copy();

        int remaining = stack.getCount();
        ItemStack typeCheck = stack.copy();
        typeCheck.setCount(1);

        NonNullList<ItemStack> items = handler.getStacks();
        int maxStack = getMaxStackSize();

        // 第1步：填入已有同种物品的槽位（按剩余空间降序）
        List<Integer> sameType = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()
                    && ItemStack.isSameItemSameComponents(items.get(i), typeCheck)) {
                sameType.add(i);
            }
        }
        sameType.sort((a, b) -> {
            int roomA = maxStack - items.get(a).getCount();
            int roomB = maxStack - items.get(b).getCount();
            return Integer.compare(roomB, roomA);
        });
        for (int slot : sameType) {
            if (remaining <= 0) break;
            ItemStack inSlot = items.get(slot);
            int space = maxStack - inSlot.getCount();
            int toAdd = Math.min(remaining, space);
            inSlot.grow(toAdd);
            remaining -= toAdd;
        }

        // 第2步：使用空槽位
        if (remaining > 0) {
            for (int i = 0; i < items.size(); i++) {
                if (remaining <= 0) break;
                if (items.get(i).isEmpty()) {
                    ItemStack newStack = stack.copy();
                    newStack.setCount(Math.min(remaining, maxStack));
                    items.set(i, newStack);
                    remaining -= newStack.getCount();
                }
            }
        }

        if (remaining <= 0) {
            setChanged();
            return ItemStack.EMPTY;
        }
        setChanged();
        ItemStack leftover = stack.copy();
        leftover.setCount(remaining);
        return leftover;
    }

    /**
     * 从储物坑中提取指定数量的某种物品。
     * 从剩余最少的同种槽位开始提取。
     */
    public ItemStack tryExtract(ItemStack type, int amount) {
        if (!structureFormed || amount <= 0) return ItemStack.EMPTY;

        ItemStack typeCheck = type.copy();
        typeCheck.setCount(1);
        ItemStack result = type.copy();
        result.setCount(0);
        int toExtract = amount;

        NonNullList<ItemStack> items = handler.getStacks();

        // 找所有同种物品槽位，按数量升序（剩余少的先提取）
        List<Integer> sameType = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (!items.get(i).isEmpty()
                    && ItemStack.isSameItemSameComponents(items.get(i), typeCheck)) {
                sameType.add(i);
            }
        }
        sameType.sort((a, b) -> Integer.compare(items.get(a).getCount(), items.get(b).getCount()));

        for (int slot : sameType) {
            if (toExtract <= 0) break;
            ItemStack inSlot = items.get(slot);
            int take = Math.min(toExtract, inSlot.getCount());
            result.grow(take);
            inSlot.shrink(take);
            toExtract -= take;
            if (inSlot.isEmpty()) {
                items.set(slot, ItemStack.EMPTY);
            }
        }

        setChanged();
        return result;
    }

    // ==================== NBT 持久化（使用 OversizedStackCodec，count 不限于 99） ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("StructureFormed", structureFormed);
        tag.putInt("InteriorWidth", interiorWidth);
        tag.putInt("InteriorHeight", interiorHeight);
        // 使用 OversizedStackHandler 序列化物品（count 不限于 99）
        // serializeNBT 返回 {Size: int, Items: ListTag}，写入主 tag 中
        CompoundTag handlerTag = handler.serializeNBT(registries);
        tag.putInt("InventorySize", handlerTag.getInt("Size"));
        tag.put("Items", handlerTag.getList("Items", 10));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        structureFormed = tag.getBoolean("StructureFormed");
        interiorWidth = tag.getInt("InteriorWidth");
        interiorHeight = tag.getInt("InteriorHeight");

        // 兼容旧格式（Counts 数组，由 copyWithCount(1) 生成）和新格式（OversizedStackCodec）
        if (tag.contains("InventorySize")) {
            // 新格式：用 OversizedStackHandler 反序列化
            CompoundTag handlerTag = new CompoundTag();
            handlerTag.putInt("Size", tag.getInt("InventorySize"));
            handlerTag.put("Items", tag.getList("Items", 10));
            handler.deserializeNBT(registries, handlerTag);
        } else if (tag.contains("Counts")) {
            // 旧格式兼容：copyWithCount(1) + Counts[] 格式，转换为新格式
            int size = structureFormed ? interiorWidth * interiorWidth * interiorHeight : 0;
            handler.setSize(size);
            net.minecraft.nbt.ListTag itemsTag = tag.getList("Items", 10);
            int[] counts = tag.getIntArray("Counts");
            for (int i = 0; i < itemsTag.size(); i++) {
                CompoundTag itemTag = itemsTag.getCompound(i);
                int slot = itemTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < handler.getSlots()) {
                    ItemStack stack = ItemStack.parse(registries, itemTag).orElse(ItemStack.EMPTY);
                    if (!stack.isEmpty() && slot < counts.length) {
                        stack.setCount(counts[slot]);
                        handler.setStackInSlot(slot, stack);
                    }
                }
            }
        }
        // 恢复后设置容量
        handler.setBaseSlotLimit(64 * STACKS_PER_TYPE);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.civilizationevolution.primitive_storage_pit");
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.primitive_storage_pit");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player) {
        if (!structureFormed) return null; // 结构无效时不开 GUI
        return new PrimitiveStoragePitMenu(containerId, playerInv, this, data);
    }

    /**
     * 预分配客户端库存列表大小。
     * 客户端 BE 不加载 NBT，handler 的 stacks 列表默认为空，
     * 但 Menu 中的 StoragePitSlot 需要按 index 访问物品，
     * 必须在创建槽位前确保列表容量足够。
     */
    public void ensureInventorySize(int size) {
        if (handler.getSlots() < size) {
            NonNullList<ItemStack> oldStacks = NonNullList.withSize(handler.getSlots(), ItemStack.EMPTY);
            for (int i = 0; i < handler.getSlots(); i++) {
                oldStacks.set(i, handler.getStackInSlot(i));
            }
            handler.setSize(size);
            for (int i = 0; i < oldStacks.size(); i++) {
                handler.setStackInSlot(i, oldStacks.get(i));
            }
        }
    }

    // ==================== 访问器 ====================

    public boolean isStructureFormed() { return structureFormed; }
    public int getInteriorWidth() { return interiorWidth; }
    public int getInteriorHeight() { return interiorHeight; }
    @Nullable
    public String getValidationError() { return validationError; }

    public ContainerData getData() {
        return data;
    }

    /** 获取内部 OversizedStackHandler（供 OversizedSlot 等使用） */
    public OversizedStackHandler getHandler() {
        return handler;
    }
}

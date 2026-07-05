package com.gooodwei.civilizationevolution.server.menu.machine;

import com.gooodwei.civilizationevolution.api.util.OversizedMenuHelper;
import com.gooodwei.civilizationevolution.server.blockentity.multiblock.PrimitiveStoragePitBlockEntity;
import com.gooodwei.civilizationevolution.server.menu.slot.OversizedSlot;
import com.gooodwei.civilizationevolution.server.menu.slot.StoragePitSlot;
import com.gooodwei.civilizationevolution.server.registry.MenuRegistry;
import com.gooodwei.civilizationevolution.network.HighStackCountSynchronizer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 储物坑 GUI 的容器菜单，AE2 合成终端布局。
 *
 * <p>固定槽位（不可变索引）：
 * <ul>
 *   <li>0-8：3×3 合成格</li>
 *   <li>9：合成结果槽</li>
 *   <li>10-36：玩家背包（27 格）</li>
 *   <li>37-45：玩家快捷栏（9 格）</li>
 * </ul>
 *
 * <p>动态槽位（索引 46+）：存储槽位，根据结构体积决定总数，每页最多显示 36 个（9 列 × 4 行）。
 * 滚动时通过 {@link #rebuildStorageSlots} 重建可见槽位，客户端通过
 * {@code ScrollStoragePitPayload} 同步到服务端，保证两端槽位索引一致。
 */
public class PrimitiveStoragePitMenu extends AbstractContainerMenu {
    // ==================== AE2 合成终端布局常量 ====================
    /** GUI 总宽度（匹配 AE2 crafting.png 纹理） */
    public static final int IMAGE_WIDTH = 195;
    /** GUI 总高度（4 行存储格：header 17 + firstRow 18 + row×2 36 + lastRow 18 + bottom 180） */
    public static final int IMAGE_HEIGHT = 269;

    private static final int CRAFTING_GRID_SIZE = 9;
    private static final int RESULT_SLOT = 9;
    private static final int PLAYER_INV_START = 10;
    private static final int PLAYER_HOTBAR_START = 37;
    /** 第一个存储槽位在容器中的索引（紧跟固定槽位之后） */
    public static final int STORAGE_START = 46;

    private static final int STORAGE_COLS = 9;
    private static final int VISIBLE_ROWS = 4;

    // 从 AE2 crafting_terminal.json 提取的精确坐标
    private static final int CRAFTING_GRID_X = 26;
    private static final int CRAFTING_GRID_Y = IMAGE_HEIGHT - 158; // = 111
    private static final int CRAFTING_RESULT_X = 134;
    private static final int CRAFTING_RESULT_Y = IMAGE_HEIGHT - 140; // = 129
    private static final int PLAYER_INV_X = 8;
    private static final int PLAYER_INV_Y = IMAGE_HEIGHT - 84; // = 185
    private static final int PLAYER_HOTBAR_Y = IMAGE_HEIGHT - 26; // = 243
    private static final int STORAGE_X = 8; // AE2 getSlotPos: x = 7 + col * 18

    // ==================== 字段 ====================

    private final PrimitiveStoragePitBlockEntity blockEntity;
    private final Player player;
    private final ContainerData data;
    private final CraftingContainer craftingContainer = new TransientCraftingContainer(this, 3, 3);
    private final ResultContainer resultContainer = new ResultContainer();
    private final int totalSlots; // 客户端从 buffer 读取，服务端从 BE 获取
    private int scrollOffset = 0; // 当前滚动偏移（客户端维护，通过 ScrollSyncPayload 同步到服务端）
    private RecipeHolder<CraftingRecipe> currentRecipe;
    private CraftingInput lastCheckedInput;

    /** 服务端构造器 */
    public PrimitiveStoragePitMenu(int containerId, Inventory playerInv, PrimitiveStoragePitBlockEntity be, ContainerData data) {
        super(MenuRegistry.PRIMITIVE_STORAGE_PIT_MENU.get(), containerId);
        this.blockEntity = be;
        this.player = playerInv.player;
        this.data = data;
        this.totalSlots = be.getContainerSize();
        addDataSlots(data);
        // 1. 合成格 3×3（索引 0-8）—— AE2: left=26, bottom=158
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(craftingContainer, col + row * 3,
                        CRAFTING_GRID_X + col * 18, CRAFTING_GRID_Y + row * 18));
            }
        }
        // 2. 合成结果槽（索引 9）—— AE2: left=134, bottom=140
        this.addSlot(new ResultSlot(playerInv.player, craftingContainer, resultContainer, 0,
                CRAFTING_RESULT_X, CRAFTING_RESULT_Y) {
            @Override
            public void onTake(Player player, ItemStack stack) {
                consumeCraftingIngredients(player);
                super.onTake(player, stack);
            }
        });
        // 3. 玩家背包（索引 10-36）+ 快捷栏（索引 37-45）—— AE2: bottom=84 / bottom=26
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, PLAYER_INV_X + col * 18, PLAYER_HOTBAR_Y));
        }
        // 4. 存储槽位（索引 46+）——每次滚动时重建，客户端通过 ScrollSyncPayload 同步
        rebuildStorageSlots(0);
    }

    /** 客户端构造器（从网络包反序列化） */
    private PrimitiveStoragePitMenu(int containerId, Inventory playerInv, PrimitiveStoragePitBlockEntity be,
                                    ContainerData data, int totalSlots) {
        super(MenuRegistry.PRIMITIVE_STORAGE_PIT_MENU.get(), containerId);
        this.blockEntity = be;
        this.player = playerInv.player;
        this.data = data;
        this.totalSlots = totalSlots;
        addDataSlots(data);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(craftingContainer, col + row * 3,
                        CRAFTING_GRID_X + col * 18, CRAFTING_GRID_Y + row * 18));
            }
        }
        this.addSlot(new ResultSlot(playerInv.player, craftingContainer, resultContainer, 0,
                CRAFTING_RESULT_X, CRAFTING_RESULT_Y) {
            @Override
            public void onTake(Player player, ItemStack stack) {
                consumeCraftingIngredients(player);
                super.onTake(player, stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9,
                        PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInv, col, PLAYER_INV_X + col * 18, PLAYER_HOTBAR_Y));
        }
        // 存储槽位
        rebuildStorageSlots(0);
    }

    // ==================== 客户端构造器 ====================

    public static PrimitiveStoragePitMenu fromNetwork(int containerId, Inventory playerInv,
                                                      RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int totalSlots = buf.readInt();
        var be = (PrimitiveStoragePitBlockEntity) playerInv.player.level()
                .getBlockEntity(pos);
        // 客户端 BE 不加载 NBT，items 列表为空 → 预分配大小避免 Slot.getItem() 越界
        if (be != null) {
            be.ensureInventorySize(totalSlots);
        }
        return new PrimitiveStoragePitMenu(containerId, playerInv, be, new SimpleContainerData(5), totalSlots);
    }

    // ==================== 动态存储槽位 ====================

    /**
     * 根据滚动偏移重建可见存储槽位。使用 AE2 getSlotPos 精确坐标。
     *
     * <p>此方法在客户端和服务端均调用。客户端在滚动时调用并通过
     * {@code ScrollStoragePitPayload} 同步到服务端，保证两端槽位索引一致。
     */
    public void rebuildStorageSlots(int scrollOffset) {
        this.scrollOffset = scrollOffset;

        // 移除旧存储槽位（保留固定槽位 0-45）
        while (this.slots.size() > STORAGE_START) {
            this.slots.removeLast();
        }

        int startIndex = scrollOffset * STORAGE_COLS;

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            // AE2 getSlotPos 公式：y = header(17) + (row>0 ? firstRow(18)+(row-1)*rowH(18) : 1)
            int y = row == 0 ? 18 : 35 + (row - 1) * 18;
            for (int col = 0; col < STORAGE_COLS; col++) {
                int index = startIndex + row * STORAGE_COLS + col;
                if (index < totalSlots) {
                    this.addSlot(new StoragePitSlot(blockEntity.getHandler(), index, STORAGE_X + col * 18, y));
                } else {
                    // 恒定槽位数占位：指向最后一个有效索引的空位，
                    // isActive()=false 阻止渲染和交互，防止滚动竞态导致总槽位数变化
                    this.addSlot(new StoragePitSlot(blockEntity.getHandler(), totalSlots - 1,
                            STORAGE_X + col * 18, y) {
                        @Override
                        public boolean isActive() {
                            return false;
                        }

                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return false;
                        }

                        @Override
                        public boolean mayPickup(Player player) {
                            return false;
                        }
                    });
                }
            }
        }
    }

    /** 返回存储槽位总数 */
    public int getTotalSlots() {
        return totalSlots;
    }

    /** 返回当前滚动偏移 */
    public int getScrollOffset() {
        return scrollOffset;
    }

    // ==================== 合成逻辑 ====================

    @Override
    public void slotsChanged(Container inventory) {
        if (inventory == craftingContainer) {
            Level level = blockEntity.getLevel();
            if (level != null && !level.isClientSide) {
                updateRecipe(level);
            }
        }
        super.slotsChanged(inventory);
    }

    private void updateRecipe(Level level) {
        List<ItemStack> inputs = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            inputs.add(craftingContainer.getItem(i).copy());
        }
        CraftingInput testInput = CraftingInput.of(3, 3, inputs);

        if (Objects.equals(lastCheckedInput, testInput)) return;
        lastCheckedInput = testInput;

        currentRecipe = level.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, testInput, level).orElse(null);

        if (currentRecipe != null) {
            resultContainer.setItem(0, currentRecipe.value().assemble(testInput, level.registryAccess()));
        } else {
            resultContainer.setItem(0, ItemStack.EMPTY);
        }
    }

    /** 从存储中消耗一次合成的材料 */
    private void consumeCraftingIngredients(Player player) {
        if (currentRecipe == null) return;
        var ingredients = currentRecipe.value().getIngredients();
        for (var ingredient : ingredients) {
            if (ingredient.isEmpty()) continue;
            // 在 BE 存储中查找并消耗 1 个匹配物品
            for (int i = 0; i < blockEntity.getContainerSize(); i++) {
                ItemStack stored = blockEntity.getItem(i);
                if (!stored.isEmpty() && ingredient.test(stored)) {
                    blockEntity.tryExtract(stored, 1);
                    break; // 找到就处理下一个原料
                }
            }
        }
        clearCraftingGrid(player);
    }

    private List<ItemStack> getRecipeIngredients(int multiplier) {
        List<ItemStack> result = new ArrayList<>();
        if (currentRecipe == null) return result;
        for (int i = 0; i < 9; i++) {
            ItemStack inSlot = craftingContainer.getItem(i);
            if (!inSlot.isEmpty()) {
                ItemStack needed = inSlot.copy();
                needed.setCount(needed.getCount() * multiplier);
                result.add(needed);
            }
        }
        return result;
    }

    private void clearCraftingGrid(Player player) {
        for (int i = 0; i < 9; i++) {
            ItemStack remaining = craftingContainer.getItem(i).copy();
            if (!remaining.isEmpty()) {
                remaining.shrink(1); // 每个槽位消耗 1 个
                if (remaining.isEmpty()) {
                    craftingContainer.setItem(i, ItemStack.EMPTY);
                } else {
                    craftingContainer.setItem(i, remaining);
                }
            }
        }
        // 重新检查配方
        Level level = blockEntity.getLevel();
        if (level != null && !level.isClientSide) {
            updateRecipe(level);
        }
    }

    // ==================== Shift+点击快速移动 ====================

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return result;

        ItemStack stackInSlot = slot.getItem();
        result = stackInSlot.copy();

        if (index == RESULT_SLOT) {
            ItemStack original = stackInSlot.copy();
            if (!this.moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_HOTBAR_START + 9, true)) {
                return ItemStack.EMPTY;
            }
            stackInSlot = original;
            slot.onTake(player, stackInSlot);
        } else if (index < STORAGE_START) {
            // 合成格/玩家背包 → 存储槽位
            ItemStack leftover = blockEntity.tryInsert(stackInSlot);
            if (leftover.getCount() == stackInSlot.getCount()) {
                return ItemStack.EMPTY;
            }
            stackInSlot.setCount(leftover.getCount());
        } else if (slot instanceof OversizedSlot) {
            // 存储槽位 → 玩家背包：一次只移动一组
            if (!OversizedMenuHelper.quickMoveFromOversizedSlot(
                    this, slot, PLAYER_INV_START, PLAYER_HOTBAR_START + 8)) {
                return ItemStack.EMPTY;
            }
            return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(stackInSlot, PLAYER_INV_START, PLAYER_HOTBAR_START + 9, true)) {
                return ItemStack.EMPTY;
            }
        }

        if (stackInSlot.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    // ==================== clicked() 覆盖：PICKUP / SWAP / QUICK_MOVE ====================

    /**
     * 覆盖 {@link AbstractContainerMenu#clicked}，针对 {@link OversizedSlot}
     * 完整接管 PICKUP（左/右键）和 SWAP（数字键）的处理逻辑。
     *
     * <p>委托给 {@link OversizedMenuHelper#handleOversizedSlotClick}。
     */
    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (OversizedMenuHelper.handleOversizedSlotClick(this, slotId, dragType, clickType, player)) {
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.stillValid(player);
    }

    /**
     * 注入自定义同步器，使 oversized 物品栈能通过网络正确同步。
     */
    @Override
    public void setSynchronizer(ContainerSynchronizer synchronizer) {
        if (player instanceof ServerPlayer serverPlayer) {
            super.setSynchronizer(new HighStackCountSynchronizer(serverPlayer));
            return;
        }
        super.setSynchronizer(synchronizer);
    }

    // ==================== 访问器 ====================

    public PrimitiveStoragePitBlockEntity getBlockEntity() { return blockEntity; }
    public ContainerData getData() { return data; }

    public static int getStorageCols() { return STORAGE_COLS; }
    public static int getVisibleRows() { return VISIBLE_ROWS; }
}

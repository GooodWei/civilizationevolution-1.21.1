package com.gooodwei.civilizationevolution.server.blockentity.machine;

import com.gooodwei.civilizationevolution.api.IClientUpdateReceiver;
import com.gooodwei.civilizationevolution.api.IPMController;
import com.gooodwei.civilizationevolution.api.IPopulationMachine;
import com.gooodwei.civilizationevolution.network.SyncMachineListPayload;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.coredata.CivilizationCoreData;
import com.gooodwei.civilizationevolution.server.coredata.CoreDataManager;
import com.gooodwei.civilizationevolution.server.item.CivilizationCoreItem;
import com.gooodwei.civilizationevolution.server.menu.PrimitiveSettlementMenu;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * 原始聚落方块实体 —— 文明控制器的初级阶段。
 *
 * <p>核心职责：
 * <ul>
 *   <li>管理文明核心物品槽位（slot 0），读取/生成 UUID</li>
 *   <li>通过 {@link CoreDataManager} 加载/持久化绑定机器列表</li>
 *   <li>以日晷进度（0–24000）为周期统一调度所有绑定机器</li>
 *   <li>为 GUI 提供 {@link ContainerData} 同步数据</li>
 * </ul>
 *
 * <p>数据分布（参考 AE2 存储磁盘模式）：
 * <ul>
 *   <li><b>物品 DataComponent</b>：UUID 字符串（唯一标识，随物品转移）</li>
 *   <li><b>外部 JSON 文件</b>：绑定机器列表、统计等业务数据（通过 CoreDataManager 读写）</li>
 *   <li><b>BE NBT</b>：仅 workProgress（调度进度），物品由父类管理</li>
 * </ul>
 *
 * <p>核心物品被取走时，控制器立即失去所有功能，但数据文件保留。
 * 同一核心放入另一个控制器后，通过 UUID 自动继承所有历史数据。
 */
public class PrimitiveSettlementBlockEntity extends BaseContainerBlockEntity
        implements MenuProvider, IPMController, IClientUpdateReceiver {

    public static final int SIZE = 1; // 仅文明核心槽位

    /** IClientUpdateReceiver 字段编号 */
    public static final int FIELD_UNBIND_MACHINE = 0;
    public static final int FIELD_SET_ENABLED = 1;

    private static final int DAY_TICKS = 24000;
    private static final int SLOT_CORE = 0;
    private static final String CONTROLLER_TYPE = "primitive_settlement";
    /** CoreDataManager 脏数据落盘间隔（tick，600 = 30 秒） */
    /** 区块强加载半径（1 = 3×3 区块） */
    private static final int CHUNK_LOAD_RADIUS = 1;
    /** 机器绑定验证间隔（200 tick = 10 秒） */
    private static final int VALIDATION_INTERVAL = 200;

    private static final int SAVE_INTERVAL = 600;

    /** 核心 UUID → 控制器坐标索引，供连接器查找活跃控制器 */
    private static final java.util.Map<String, BlockPos> CORE_LOCATIONS = new java.util.concurrent.ConcurrentHashMap<>();

    public final ContainerData data;
    private int workProgress;
    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    /** 运行时缓存：当前核心对应的数据（从 CoreDataManager 加载） */
    private CivilizationCoreData coreData;
    /** 当前核心的 UUID（从物品 DataComponent 读取，用于检测核心是否更换） */
    private String currentUuid;
    /** 脏数据落盘计数器（静态共享，避免每个 BE 独立调用 saveDirty） */
    private static int saveCounter = 0;
    /** 机器验证计数器（静态共享） */
    private static int validationCounter = 0;
    /** 是否已强加载周边区块（用于避免重复调用 setChunkForced） */
    private boolean chunksForced;

    public PrimitiveSettlementBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistry.PRIMITIVE_SETTLEMENT.get(), pos, blockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> workProgress;
                    case 1 -> getBoundMachineCount();
                    case 2 -> DAY_TICKS;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    workProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    // ==================== 区块加载 ====================

    @Override
    public void onLoad() {
        super.onLoad();
        // 区块加载后强加载 3×3 区域，确保绑定的机器可被访问
        if (getLevel() instanceof ServerLevel serverLevel && !chunksForced) {
            forceLoadChunks(serverLevel);
            chunksForced = true;
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // 方块被移除时释放区块强加载
        if (getLevel() instanceof ServerLevel serverLevel && chunksForced) {
            releaseChunkForces(serverLevel);
            chunksForced = false;
        }
    }

    /** 强加载以本方块为中心的 3×3 区块 */
    private void forceLoadChunks(ServerLevel level) {
        ChunkPos center = new ChunkPos(worldPosition);
        for (int dx = -CHUNK_LOAD_RADIUS; dx <= CHUNK_LOAD_RADIUS; dx++) {
            for (int dz = -CHUNK_LOAD_RADIUS; dz <= CHUNK_LOAD_RADIUS; dz++) {
                level.setChunkForced(center.x + dx, center.z + dz, true);
            }
        }
    }

    /** 释放以本方块为中心的 3×3 区块强加载 */
    private void releaseChunkForces(ServerLevel level) {
        ChunkPos center = new ChunkPos(worldPosition);
        for (int dx = -CHUNK_LOAD_RADIUS; dx <= CHUNK_LOAD_RADIUS; dx++) {
            for (int dz = -CHUNK_LOAD_RADIUS; dz <= CHUNK_LOAD_RADIUS; dz++) {
                level.setChunkForced(center.x + dx, center.z + dz, false);
            }
        }
    }

    // ==================== 核心槽位管理 ====================

    /**
     * 判断指定槽位是否为文明核心槽位。
     */
    public boolean isCoreSlot(int slot) {
        return slot == SLOT_CORE;
    }

    /**
     * 限制核心槽位仅接受文明核心物品。
     * 其他槽位走父类默认逻辑（允许任意物品）。
     */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (isCoreSlot(slot)) {
            return stack.getItem() instanceof CivilizationCoreItem;
        }
        return super.canPlaceItem(slot, stack);
    }

    /**
     * 物品放入槽位时触发，核心槽位变更时<b>立即同步加载或清空</b>核心数据。
     *
     * <p>覆写原因：serverTick 每 tick 才执行一次，若玩家放入核心后立刻
     * 通过 GUI/网络包查询绑定列表，此时 coreData 可能尚未加载。
     * 在 setItem 中同步加载可消除这一 tick 的延迟。
     *
     * <p>安全性：
     * <ul>
     *   <li>仅在服务端执行（{@code level.isClientSide} 检查）</li>
     *   <li>CoreDataManager 未就绪时跳过（世界加载阶段由 serverTick 兜底）</li>
     *   <li>父类 setItem 已调用 setChanged，此处不重复标记（仅数据变更时补调）</li>
     * </ul>
     */
    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);

        if (!isCoreSlot(slot)) return;

        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        if (!CoreDataManager.isReady()) return; // 世界加载中，由 serverTick 兜底

        if (stack.isEmpty() || !(stack.getItem() instanceof CivilizationCoreItem)) {
            // 核心被取出 → 清空运行时状态（绑定数据保留在 JSON 文件中，核心放回时自动恢复）
            if (coreData != null) {
                CORE_LOCATIONS.remove(currentUuid);
                coreData = null;
                currentUuid = null;
                setChanged();
                notifyViewersEmpty();
            }
            return;
        }

        // 放入的是文明核心物品
        String uuid = CivilizationCoreItem.getUuid(stack);

        if (uuid == null) {
            // 新核心无 UUID → 自动生成
            uuid = CoreDataManager.generateUuid();
            CivilizationCoreItem.setUuid(stack, uuid);
            coreData = CoreDataManager.createNew(uuid, CONTROLLER_TYPE);
            currentUuid = uuid;
            CORE_LOCATIONS.put(uuid, getBlockPos());
            setChanged();
        } else if (!uuid.equals(currentUuid)) {
            // 更换了核心 → 更新索引
            if (currentUuid != null) CORE_LOCATIONS.remove(currentUuid);
            coreData = CoreDataManager.getOrLoad(uuid);
            if (coreData == null) {
                coreData = CoreDataManager.createNew(uuid, CONTROLLER_TYPE);
            }
            currentUuid = uuid;
            CORE_LOCATIONS.put(uuid, getBlockPos());
            setChanged();
        }
        // 核心数据已就绪，立即同步到正在查看 GUI 的玩家
        notifyViewersSync();
    }

    // ==================== 服务端 Tick ====================

    /**
     * 每 tick 由方块 ticker 调度（仅服务端）。
     *
     * <p>流程：
     * <ol>
     *   <li>检查核心槽位是否有文明核心物品 → 无则清空运行时状态并返回</li>
     *   <li>核心无 UUID → 自动生成 UUID 并创建 CoreData</li>
     *   <li>UUID 变更（更换了核心）→ 从 CoreDataManager 加载新数据</li>
     *   <li>执行调度 tick</li>
     *   <li>定期将脏数据落盘</li>
     * </ol>
     */
    public static void serverTick(Level level, BlockPos pos, BlockState blockState, PrimitiveSettlementBlockEntity be) {
        ItemStack coreStack = be.items.get(SLOT_CORE);

        // 无核心或物品类型不对 → 清空运行时状态
        if (coreStack.isEmpty() || !(coreStack.getItem() instanceof CivilizationCoreItem)) {
            if (be.coreData != null) {
                CORE_LOCATIONS.remove(be.currentUuid);
                be.coreData = null;
                be.currentUuid = null;
                setChanged(level, pos, blockState);
            }
            return;
        }

        String uuid = CivilizationCoreItem.getUuid(coreStack);

        // 核心尚未初始化 UUID → 自动生成并写入物品
        if (uuid == null) {
            uuid = CoreDataManager.generateUuid();
            CivilizationCoreItem.setUuid(coreStack, uuid);
            be.coreData = CoreDataManager.createNew(uuid, CONTROLLER_TYPE);
            be.currentUuid = uuid;
            CORE_LOCATIONS.put(uuid, pos);
            setChanged(level, pos, blockState);
            return;
        }

        // UUID 变更（玩家更换了核心物品）→ 加载/创建新数据
        if (!uuid.equals(be.currentUuid)) {
            if (be.currentUuid != null) CORE_LOCATIONS.remove(be.currentUuid);
            be.coreData = CoreDataManager.getOrLoad(uuid);
            if (be.coreData == null) {
                // 物品有 UUID 但数据文件丢失（如手动删除），重新创建空数据
                be.coreData = CoreDataManager.createNew(uuid, CONTROLLER_TYPE);
            }
            be.currentUuid = uuid;
            CORE_LOCATIONS.put(uuid, pos);
        }

        // 兜底：确保 coreData 非空（首次加载 NBT 后 currentUuid 为 null）
        if (be.coreData == null) {
            be.coreData = CoreDataManager.getOrLoad(uuid);
            if (be.coreData == null) {
                be.coreData = CoreDataManager.createNew(uuid, CONTROLLER_TYPE);
            }
            be.currentUuid = uuid;
        }

        // 执行调度
        be.tick(level);

        // 定期验证绑定机器是否仍然存在且类型匹配
        validationCounter++;
        if (validationCounter >= VALIDATION_INTERVAL) {
            be.validateBoundMachines((ServerLevel) level);
            validationCounter = 0;
        }

        // 定期将脏数据落盘
        saveCounter++;
        if (saveCounter >= SAVE_INTERVAL) {
            CoreDataManager.saveDirty();
            saveCounter = 0;
        }

        setChanged(level, pos, blockState);
    }

    // ==================== IPMController 实现 ====================

    @Override
    public Container getContainer() {
        return this;
    }

    @Override
    public int getMaxBindCount() {
        return PopulationMachineConfig.getMaxBindCount(CONTROLLER_TYPE);
    }

    @Override
    public int getBoundMachineCount() {
        if (coreData == null) return 0;
        return coreData.getBoundMachines().size();
    }

    /** 获取当前核心的 UUID，无核心时返回 null。供连接器等外部调用。 */
    public String getCurrentUuid() {
        return currentUuid;
    }

    /** 查询持有指定核心 UUID 的控制器坐标，未找到时返回 null。 */
    public static BlockPos getCoreLocation(String uuid) {
        return CORE_LOCATIONS.get(uuid);
    }

    @Override
    public boolean isMachineBound(BlockPos pos) {
        if (coreData == null) return false;
        return coreData.hasMachine(pos);
    }

    @Override
    public boolean bindMachine(BlockPos pos, IPopulationMachine machine) {
        // 无核心数据 → 拒绝
        if (coreData == null) return false;
        // 机器已被其他控制器绑定 → 拒绝
        if (machine.isBound()) return false;
        // 已绑定
        if (coreData.hasMachine(pos)) return false;
        // 已满
        if (coreData.getBoundMachines().size() >= getMaxBindCount()) return false;

        machine.setBound(true);
        machine.setBoundCoreUuid(currentUuid);
        int next = (workProgress + machine.getWorkTotalTime()) % DAY_TICKS;
        // 记录机器 BlockEntityType 注册名，用于后续验证
        String machineType = "";
        if (machine instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
            machineType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString();
        }
        coreData.addMachine(pos, next, true, machineType);
        CoreDataManager.markDirty(currentUuid);
        setChanged();
        return true;
    }

    @Override
    public void unbindMachine(BlockPos pos, Level level) {
        if (coreData == null) return;

        if (level.getBlockEntity(pos) instanceof IPopulationMachine machine) {
            machine.setBound(false);
            machine.setBoundCoreUuid(null);
        }
        coreData.removeMachine(pos);
        CoreDataManager.markDirty(currentUuid);
        setChanged();
    }

    @Override
    public void setMachineEnabled(BlockPos pos, boolean enabled) {
        if (coreData == null) return;
        coreData.setMachineEnabled(pos, enabled);
        CoreDataManager.markDirty(currentUuid);
        setChanged();
    }

    @Override
    public void tick(Level level) {
        if (coreData == null) return;

        this.workProgress++;

        // 遍历绑定机器，检查触发条件
        for (CivilizationCoreData.BoundMachineEntry bm : coreData.getBoundMachines()) {
            if (!bm.enabled || !level.isLoaded(bm.getBlockPos())) continue;

            if (this.workProgress == bm.nextTriggerProgress) {
                BlockPos pos = bm.getBlockPos();
                // 先确保区块已加载（防止边缘情况）
                if (!level.isLoaded(pos)) continue;
                // 验证机器是否存在且类型匹配
                if (!isMachineValid(level, pos, bm)) {
                    coreData.removeMachine(pos);
                    CoreDataManager.markDirty(currentUuid);
                    setChanged();
                    continue;
                }
                if (level.getBlockEntity(pos) instanceof IPopulationMachine machine) {
                    machine.executeWorkCycle(level);
                    bm.nextTriggerProgress = (this.workProgress + machine.getWorkTotalTime()) % DAY_TICKS;
                    CoreDataManager.markDirty(currentUuid);
                }
            }
        }

        if (this.workProgress >= DAY_TICKS) {
            this.workProgress = 0;
        }
    }

    // ==================== IClientUpdateReceiver ====================

    /**
     * 接收客户端发来的字段更新（解绑/启停）。
     * 所有操作完成后自动向打开 GUI 的玩家同步最新列表。
     *
     * @param fieldId 字段编号（本类 FIELD_XXX 常量）
     * @param data    客户端提交的数据，CompoundTag 中 key 为 "pos"(long) 和 "enabled"(boolean)
     */
    @Override
    public void onClientUpdate(int fieldId, CompoundTag data) {
        if (coreData == null) return;

        BlockPos targetPos = BlockPos.of(data.getLong("pos"));

        switch (fieldId) {
            case FIELD_UNBIND_MACHINE -> {
                unbindMachine(targetPos, getLevel());
            }
            case FIELD_SET_ENABLED -> {
                boolean enabled = data.getBoolean("enabled");
                setMachineEnabled(targetPos, enabled);
            }
            default -> {
                return; // 未知字段，不触发同步
            }
        }

        // 操作成功后向所有附近的玩家同步（若 GUI 仍打开则更新）
        if (getLevel() != null) {
            for (Player player : getLevel().players()) {
                if (player instanceof ServerPlayer sp
                        && sp.containerMenu instanceof PrimitiveSettlementMenu menu
                        && menu.getBlockPos().equals(worldPosition)) {
                    syncToPlayer(sp);
                }
            }
        }
    }

    // ==================== 网络同步 ====================

    /**
     * 向指定玩家发送当前绑定机器列表。
     * 在 GUI 打开时和每次解绑/启停操作后调用。
     */
    public void syncToPlayer(ServerPlayer player) {
        if (coreData == null || getLevel() == null) return;

        // 获取当前控制器所在维度
        String dimension = getLevel().dimension().location().toString();
        List<SyncMachineListPayload.MachineEntry> entries = new ArrayList<>();
        for (CivilizationCoreData.BoundMachineEntry bm : coreData.getBoundMachines()) {
            BlockPos machinePos = bm.getBlockPos();
            // 从目标 BE 获取显示名称
            String displayName = machinePos.toShortString(); // 默认显示坐标
            if (getLevel().getBlockEntity(machinePos) instanceof MenuProvider provider) {
                displayName = provider.getDisplayName().getString();
            }
            entries.add(new SyncMachineListPayload.MachineEntry(
                    machinePos, bm.enabled, displayName, dimension));
        }

        PacketDistributor.sendToPlayer(player,
                new SyncMachineListPayload(entries));
    }

    /**
     * 核心被取出时，向所有正在查看此控制器 GUI 的玩家发送空列表。
     */
    private void notifyViewersEmpty() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        SyncMachineListPayload empty = new SyncMachineListPayload(List.of());
        for (ServerPlayer sp : serverLevel.players()) {
            if (sp.containerMenu instanceof PrimitiveSettlementMenu menu
                    && menu.getBlockPos().equals(getBlockPos())) {
                PacketDistributor.sendToPlayer(sp, empty);
            }
        }
    }

    /**
     * 核心数据就绪时，向所有正在查看此控制器 GUI 的玩家同步机器列表。
     */
    public void notifyViewersSync() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        for (ServerPlayer sp : serverLevel.players()) {
            if (sp.containerMenu instanceof PrimitiveSettlementMenu menu
                    && menu.getBlockPos().equals(getBlockPos())) {
                syncToPlayer(sp);
            }
        }
    }

    // ==================== 机器验证 ====================

    /**
     * 验证指定位置的机器是否存在且类型与绑定时记录的一致。
     *
     * @return true 表示机器有效，false 表示应被移除
     */
    private boolean isMachineValid(Level level, BlockPos pos, CivilizationCoreData.BoundMachineEntry entry) {
        if (!(level.getBlockEntity(pos) instanceof IPopulationMachine)) {
            return false;
        }
        // 类型匹配检查：若存储了类型信息，则必须一致
        if (entry.machineType != null && !entry.machineType.isEmpty()
                && level.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
            String currentType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString();
            if (!currentType.equals(entry.machineType)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 遍历所有绑定机器，移除不存在或类型不匹配的条目。
     * 由 serverTick 每 200 tick 调用一次。
     */
    private void validateBoundMachines(ServerLevel level) {
        if (coreData == null) return;

        List<BlockPos> toRemove = new ArrayList<>();
        for (CivilizationCoreData.BoundMachineEntry entry : coreData.getBoundMachines()) {
            BlockPos pos = entry.getBlockPos();
            // 确保区块已加载后再检查
            if (!level.isLoaded(pos)) {
                // 即使区块未加载，也尝试通过强制加载来访问
                // 如果已经调用了 forceLoadChunks，通常不会走到这里
                continue;
            }
            if (!isMachineValid(level, pos, entry)) {
                toRemove.add(pos);
            }
        }

        for (BlockPos pos : toRemove) {
            // 解除目标机器的绑定标记
            if (level.getBlockEntity(pos) instanceof IPopulationMachine machine) {
                machine.setBound(false);
                machine.setBoundCoreUuid(null);
            }
            coreData.removeMachine(pos);
        }

        if (!toRemove.isEmpty()) {
            CoreDataManager.markDirty(currentUuid);
            setChanged();
        }
    }

    // ==================== NBT 持久化 ====================

    /**
     * 保存 BE 数据。
     * 显式调用 ContainerHelper 保存物品（子类 items 字段遮蔽父类字段，
     * 不能依赖 super.saveAdditional）。
     * 绑定机器列表存储在外部 JSON 文件中，不存入 BE NBT。
     */
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("WorkProgress", workProgress);
        ContainerHelper.saveAllItems(tag, items, registries);
    }

    /**
     * 加载 BE 数据。
     * coreData 在服务端 tick 中按需从 CoreDataManager 加载。
     */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workProgress = tag.getInt("WorkProgress");
        ContainerHelper.loadAllItems(tag, items, registries);
    }

    // ==================== BaseContainerBlockEntity 抽象方法 ====================

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.civilizationevolution.primitive_settlement");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> nonNullList) {
        this.items = nonNullList;
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    /**
     * 玩家打开 GUI 时创建菜单，并立即同步机器列表。
     * 无核心时显式发送空列表，清除客户端可能残留的缓存数据。
     */
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        AbstractContainerMenu menu = new PrimitiveSettlementMenu(containerId, playerInventory, this, this.data);
        if (player instanceof ServerPlayer sp) {
            if (coreData != null) {
                syncToPlayer(sp);
            } else {
                // 无核心 → 发送空列表清空客户端缓存
                PacketDistributor.sendToPlayer(sp, new SyncMachineListPayload(List.of()));
            }
        }
        return menu;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new PrimitiveSettlementMenu(containerId, inventory, this, this.data);
    }
}

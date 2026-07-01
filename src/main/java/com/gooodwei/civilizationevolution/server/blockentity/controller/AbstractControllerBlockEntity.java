package com.gooodwei.civilizationevolution.server.blockentity.controller;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.api.IClientUpdateReceiver;
import com.gooodwei.civilizationevolution.api.IMultiBlockMachine;
import com.gooodwei.civilizationevolution.api.IPMController;
import com.gooodwei.civilizationevolution.api.IPopulationMachine;
import com.gooodwei.civilizationevolution.api.MultiBlockState;
import com.gooodwei.civilizationevolution.api.tier.Tier;
import com.gooodwei.civilizationevolution.network.NetworkHandler;
import com.gooodwei.civilizationevolution.network.SyncMachineListPayload;
import com.gooodwei.civilizationevolution.server.blockentity.controller.PrimitiveControllerBlockEntity;
import com.gooodwei.civilizationevolution.server.blockentity.controller.VillageControllerBlockEntity;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.block.controller.AbstractControllerBlock;
import com.gooodwei.civilizationevolution.server.coredata.CivilizationCoreData;
import com.gooodwei.civilizationevolution.server.coredata.CoreDataManager;
import com.gooodwei.civilizationevolution.server.item.CivilizationCoreItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 文明控制器抽象父类 —— 提供所有控制器的通用实现。
 *
 * <p>控制器负责：
 * <ul>
 *   <li>管理文明核心物品槽位（slot 0），读取/生成 UUID</li>
 *   <li>通过 {@link CoreDataManager} 加载/持久化绑定机器列表</li>
 *   <li>以日晷进度（0–24000）为周期统一调度所有绑定机器</li>
 *   <li>为 GUI 提供 {@link ContainerData} 同步数据</li>
 *   <li>绑定/解绑/启停机器</li>
 *   <li>区块强加载确保绑定机器可访问</li>
 * </ul>
 *
 * <p>子类只需实现类型特定的抽象方法（控制器类型、区块半径、菜单创建等）。
 *
 * <p>数据分布（参考 AE2 存储磁盘模式）：
 * <ul>
 *   <li><b>物品 DataComponent</b>：UUID 字符串（唯一标识，随物品转移）</li>
 *   <li><b>外部 JSON 文件</b>：绑定机器列表、统计等业务数据（通过 CoreDataManager 读写）</li>
 *   <li><b>BE NBT</b>：仅 workProgress（调度进度），物品由父类管理</li>
 * </ul>
 *
 * @see PrimitiveControllerBlockEntity
 * @see VillageControllerBlockEntity
 */
public abstract class AbstractControllerBlockEntity
        extends BaseContainerBlockEntity
        implements MenuProvider, IPMController, IClientUpdateReceiver, IMultiBlockMachine {

    // ==================== 通用常量 ====================

    /** 一日总 tick 数 */
    protected static final int DAY_TICKS = 24000;
    /** 文明核心物品所在槽位索引 */
    protected static final int SLOT_CORE = 0;
    /** 机器绑定验证间隔（200 tick = 10 秒） */
    private static final int VALIDATION_INTERVAL = 200;
    /** 脏数据落盘间隔（600 tick = 30 秒） */
    private static final int SAVE_INTERVAL = 600;

    // ==================== IClientUpdateReceiver 字段编号 ====================

    /** 客户端请求解绑机器 */
    public static final int FIELD_UNBIND_MACHINE = 0;
    /** 客户端请求启用/禁用机器 */
    public static final int FIELD_SET_ENABLED = 1;

    // ==================== 静态状态（所有控制器共享） ====================

    /** 核心 UUID → 控制器坐标索引，供连接器查找活跃控制器 */
    private static final java.util.Map<String, BlockPos> CORE_LOCATIONS =
            new java.util.concurrent.ConcurrentHashMap<>();

    /** 脏数据落盘计数器（静态共享，避免每个 BE 独立调用 saveDirty） */
    private static int saveCounter = 0;
    /** 机器验证计数器（静态共享） */
    private static int validationCounter = 0;

    // ==================== 实例字段 ====================

    /** 物品槽位列表 */
    protected final NonNullList<ItemStack> items;
    /** 当前工作进度（0–24000） */
    protected int workProgress;
    /** 供 GUI 同步的 ContainerData */
    public final ContainerData data;
    /** 运行时缓存：当前核心对应的数据（从 CoreDataManager 加载） */
    protected CivilizationCoreData coreData;
    /** 当前核心的 UUID（从物品 DataComponent 读取） */
    protected String currentUuid;
    /** 是否已强加载周边区块 */
    protected boolean chunksForced;

    /** 多方块运行时状态（结构成型标识、位置缓存、解析结果等） */
    private final MultiBlockState mbs = new MultiBlockState();

    // ==================== 构造器 ====================

    /**
     * @param type  BlockEntity 类型
     * @param pos   方块坐标
     * @param state 方块状态
     * @param size  容器槽位总数
     */
    protected AbstractControllerBlockEntity(BlockEntityType<?> type,
                                             BlockPos pos, BlockState state, int size) {
        super(type, pos, state);
        this.items = NonNullList.withSize(size, ItemStack.EMPTY);
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

    // ==================== 抽象方法（子类实现） ====================

    /** 控制器类型标识，用于 CoreDataManager 和配置查找 */
    public abstract String getControllerType();

    /** 区块强加载半径（1 = 3×3 区块，2 = 5×5 区块） */
    protected abstract int getChunkLoadRadius();

    /**
     * 判断指定玩家是否正在查看本控制器的 GUI。
     * 每个子类检查自己的 Menu 类型和坐标匹配。
     */
    protected abstract boolean isViewingController(ServerPlayer sp);

    /**
     * 返回 {@link com.gooodwei.civilizationevolution.server.config.MultiBlockConfig} 中对应的结构标识 key。
     *
     * @return 配置文件中的结构 key（如 {@code "primitive_controller"}）
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
        // 控制器仅有一个核心槽位，弹出核心物品
        if (getLevel() instanceof net.minecraft.server.level.ServerLevel sl) {
            ItemStack coreStack = items.get(SLOT_CORE);
            if (!coreStack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(sl, worldPosition, coreStack);
                items.set(SLOT_CORE, ItemStack.EMPTY);
            }
        }
    }

    // ==================== 区块加载 ====================

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel && !chunksForced) {
            forceLoadChunks(serverLevel);
            chunksForced = true;
        }
        // 多方块结构延迟验证（给周围 chunk 加载留时间）
        onMultiBlockLoad();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (getLevel() instanceof ServerLevel serverLevel && chunksForced) {
            releaseChunkForces(serverLevel);
            chunksForced = false;
        }
    }

    /** 强加载以本方块为中心的区域 */
    private void forceLoadChunks(ServerLevel level) {
        int r = getChunkLoadRadius();
        ChunkPos center = new ChunkPos(worldPosition);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                level.setChunkForced(center.x + dx, center.z + dz, true);
            }
        }
    }

    /** 释放以本方块为中心的区域强加载 */
    private void releaseChunkForces(ServerLevel level) {
        int r = getChunkLoadRadius();
        ChunkPos center = new ChunkPos(worldPosition);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
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
     */
    @Override
    public void setItem(int slot, ItemStack stack) {
        super.setItem(slot, stack);

        if (!isCoreSlot(slot)) return;

        Level level = getLevel();
        if (level == null || level.isClientSide) return;
        if (!CoreDataManager.isReady()) return;

        if (stack.isEmpty() || !(stack.getItem() instanceof CivilizationCoreItem)) {
            if (coreData != null) {
                CORE_LOCATIONS.remove(currentUuid);
                coreData = null;
                currentUuid = null;
                setChanged();
                notifyViewersEmpty();
            }
            return;
        }

        String uuid = CivilizationCoreItem.getUuid(stack);

        boolean isNewCore = (uuid == null) || !uuid.equals(currentUuid);

        if (uuid == null) {
            uuid = CoreDataManager.generateUuid();
            CivilizationCoreItem.setUuid(stack, uuid);
            coreData = CoreDataManager.createNew(uuid, getControllerType());
            currentUuid = uuid;
            CORE_LOCATIONS.put(uuid, getBlockPos());
            setChanged();
        } else if (!uuid.equals(currentUuid)) {
            if (currentUuid != null) CORE_LOCATIONS.remove(currentUuid);
            coreData = CoreDataManager.getOrLoad(uuid);
            if (coreData == null) {
                coreData = CoreDataManager.createNew(uuid, getControllerType());
            }
            currentUuid = uuid;
            CORE_LOCATIONS.put(uuid, getBlockPos());
            setChanged();
        }

        // 核心首次放入或更换时，向所有已绑定机器推送当前控制器的维度和坐标（更新 Jade 显示）
        if (isNewCore && coreData != null) {
            syncBoundMachinesLocation((ServerLevel) level);
        }

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
    public static <T extends AbstractControllerBlockEntity> void controllerServerTick(
            Level level, BlockPos pos, BlockState blockState, T be) {
        // 多方块结构延迟/定时验证
        be.tickRevalidation();

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
        boolean coreDataJustSet = false;

        // 核心尚未初始化 UUID → 自动生成并写入物品
        if (uuid == null) {
            uuid = CoreDataManager.generateUuid();
            CivilizationCoreItem.setUuid(coreStack, uuid);
            be.coreData = CoreDataManager.createNew(uuid, be.getControllerType());
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
                be.coreData = CoreDataManager.createNew(uuid, be.getControllerType());
            }
            be.currentUuid = uuid;
            CORE_LOCATIONS.put(uuid, pos);
            coreDataJustSet = true;
        }

        // 兜底：确保 coreData 非空
        if (be.coreData == null) {
            be.coreData = CoreDataManager.getOrLoad(uuid);
            if (be.coreData == null) {
                be.coreData = CoreDataManager.createNew(uuid, be.getControllerType());
            }
            be.currentUuid = uuid;
            coreDataJustSet = true;
        }

        // 核心数据刚完成初始化 → 向所有已绑定机器推送当前控制器位置（更新 Jade 显示）
        if (coreDataJustSet) {
            be.syncBoundMachinesLocation((ServerLevel) level);
        }

        // 只有结构成型时才执行机器调度（绑定/解绑仍可通过连接器操作）
        if (be.isStructureFormed()) {
            be.tick(level);
        }

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

        // 信标光柱开关：红石信号 + 核心 UUID 同时满足时激活
        boolean shouldBeam = be.currentUuid != null && level.hasNeighborSignal(pos);
        if (blockState.hasProperty(AbstractControllerBlock.BEAM_ACTIVE)
                && blockState.getValue(AbstractControllerBlock.BEAM_ACTIVE) != shouldBeam) {
            level.setBlock(pos, blockState.setValue(AbstractControllerBlock.BEAM_ACTIVE, shouldBeam), 3);
        }

        setChanged(level, pos, blockState);
    }

    // ==================== IPMController 实现 ====================

    /**
     * 此控制器的 Tier 等级。
     *
     * <p>每个具体控制器子类<b>必须</b>覆写此方法，显式声明所属时代。
     * 与对应 Block 的 {@link AbstractMachineBlock#getTier()} 保持相同值，
     * 确保绑定逻辑与物品 tooltip 一致。
     *
     * @return 此控制器的 Tier 等级
     */
    @Override
    public abstract Tier getTier();

    @Override
    public Container getContainer() {
        return this;
    }

    @Override
    public int getMaxBindCount() {
        return PopulationMachineConfig.getMaxBindCount(getControllerType());
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
        if (coreData == null) {
            CivilizationEvolution.LOGGER.warn("bindMachine 失败：coreData 为 null，控制器位于 {}", getBlockPos());
            return false;
        }
        if (machine.isBound()) {
            CivilizationEvolution.LOGGER.warn("bindMachine 失败：机器已绑定，机位={}，已绑核心={}", pos, machine.getBoundCoreUuid());
            return false;
        }
        if (coreData.hasMachine(pos)) {
            CivilizationEvolution.LOGGER.warn("bindMachine 失败：核心数据中已存在该机位 {}", pos);
            return false;
        }
        if (coreData.getBoundMachines().size() >= getMaxBindCount()) {
            CivilizationEvolution.LOGGER.warn("bindMachine 失败：已达最大绑定数 {}/{}", coreData.getBoundMachines().size(), getMaxBindCount());
            return false;
        }
        // Tier 检查：控制器只能绑定 ≤ 自身 tier 的机器
        if (machine.getTier().getLevel() > getTier().getLevel()) {
            CivilizationEvolution.LOGGER.warn("bindMachine 失败：Tier 不匹配，机器 tier={} > 控制器 tier={}",
                    machine.getTier().getLevel(), getTier().getLevel());
            return false;
        }
        // 距离检查：机器必须在控制器的最大绑定范围内
        int maxRange = PopulationMachineConfig.getMaxBindRange(getControllerType());
        if (maxRange > 0 && !pos.closerThan(getBlockPos(), maxRange + 1)) {
            CivilizationEvolution.LOGGER.warn("bindMachine 失败：距离超出范围，机位={}，控制器={}，距离={}，最大={}",
                    pos, getBlockPos(), Math.sqrt(pos.distSqr(getBlockPos())), maxRange);
            return false;
        }

        machine.setBound(true);
        machine.setBoundCoreUuid(currentUuid);
        machine.setBoundControllerDimension(getLevel().dimension().location().toString());
        int next = (workProgress + machine.getWorkTotalTime()) % DAY_TICKS;
        // 记录机器 BlockEntityType 注册名，用于后续验证
        String machineType = "";
        if (machine instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
            machineType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString();
        }
        coreData.addMachine(pos, next, true, machineType, machine.getTier().getLevel());
        CoreDataManager.markDirty(currentUuid);
        CoreDataManager.saveDirty();
        setChanged();
        return true;
    }

    @Override
    public void unbindMachine(BlockPos pos, Level level) {
        if (coreData == null) return;

        if (level.getBlockEntity(pos) instanceof IPopulationMachine machine) {
            machine.setBound(false);
            machine.setBoundCoreUuid(null);
            machine.setBoundControllerDimension(null);
        }
        coreData.removeMachine(pos);
        CoreDataManager.markDirty(currentUuid);
        CoreDataManager.saveDirty();
        setChanged();
    }

    @Override
    public void setMachineEnabled(BlockPos pos, boolean enabled) {
        if (coreData == null) return;
        coreData.setMachineEnabled(pos, enabled);
        CoreDataManager.markDirty(currentUuid);
        CoreDataManager.saveDirty();
        setChanged();
    }

    @Override
    public void tick(Level level) {
        if (coreData == null) return;

        this.workProgress++;

        // 缓存最大绑定距离，避免循环内重复查询配置
        int maxRange = PopulationMachineConfig.getMaxBindRange(getControllerType());

        for (CivilizationCoreData.BoundMachineEntry bm : coreData.getBoundMachines()) {
            if (!bm.enabled || !level.isLoaded(bm.getBlockPos())) continue;
            // 高级机器：不调度工作（但仍在列表中，验证照常执行）
            if (bm.tier > getTier().getLevel()) continue;
            // 超出绑定范围：不调度工作但不从列表中移除（核心移回原控制器后可恢复）
            if (maxRange > 0 && !bm.getBlockPos().closerThan(getBlockPos(), maxRange + 1)) continue;

            if (this.workProgress == bm.nextTriggerProgress) {
                BlockPos pos = bm.getBlockPos();
                if (!level.isLoaded(pos)) continue;
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
                return;
            }
        }

        // 操作成功后向所有附近玩家同步
        if (getLevel() != null) {
            for (Player player : getLevel().players()) {
                if (player instanceof ServerPlayer sp && isViewingController(sp)) {
                    syncToPlayer(sp);
                }
            }
        }
    }

    // ==================== 网络同步 ====================

    /**
     * 向指定玩家发送当前绑定机器列表。
     */
    public void syncToPlayer(ServerPlayer player) {
        if (coreData == null || getLevel() == null) return;

        String dimension = getLevel().dimension().location().toString();
        List<SyncMachineListPayload.MachineEntry> entries = new ArrayList<>();
        for (CivilizationCoreData.BoundMachineEntry bm : coreData.getBoundMachines()) {
            BlockPos machinePos = bm.getBlockPos();
            String displayName = machinePos.toShortString();
            if (getLevel().getBlockEntity(machinePos) instanceof MenuProvider provider) {
                displayName = provider.getDisplayName().getString();
            }
            entries.add(new SyncMachineListPayload.MachineEntry(
                    machinePos, bm.enabled, displayName, dimension));
        }

        NetworkHandler.sendToPlayer(player,
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
            if (isViewingController(sp)) {
                NetworkHandler.sendToPlayer(sp, empty);
            }
        }
    }

    /**
     * 向所有已绑定到此核心的机器推送当前控制器的维度和坐标，
     * 确保 Jade 工具提示显示正确的控制器位置。
     *
     * <p>核心被放入新控制器时自动调用，无论多方块结构是否成型。
     * 仅更新已加载区块中的机器，未加载的机器在区块加载后由
     * {@link #validateBoundMachines} 兜底。
     */
    void syncBoundMachinesLocation(ServerLevel serverLevel) {
        if (coreData == null || currentUuid == null) return;
        String dimension = serverLevel.dimension().location().toString();

        for (CivilizationCoreData.BoundMachineEntry entry : coreData.getBoundMachines()) {
            BlockPos machinePos = entry.getBlockPos();
            if (!serverLevel.isLoaded(machinePos)) continue;
            if (serverLevel.getBlockEntity(machinePos) instanceof IPopulationMachine machine) {
                // 仅更新仍绑定到此核心的机器（双重校验，防止核心数据与机器状态不一致）
                if (currentUuid.equals(machine.getBoundCoreUuid())) {
                    machine.setBoundControllerDimension(dimension);
                }
            }
        }
        CivilizationEvolution.LOGGER.info("已向 {} 台机器推送控制器位置更新（维度={}，控制器={}）",
                coreData.getBoundMachines().size(), dimension, getBlockPos());
    }

    /**
     * 核心数据就绪时，向所有正在查看此控制器 GUI 的玩家同步机器列表。
     */
    public void notifyViewersSync() {
        Level level = getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;
        for (ServerPlayer sp : serverLevel.players()) {
            if (isViewingController(sp)) {
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
    protected boolean isMachineValid(Level level, BlockPos pos,
                                    CivilizationCoreData.BoundMachineEntry entry) {
        if (!(level.getBlockEntity(pos) instanceof IPopulationMachine)) {
            return false;
        }
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
     */
    protected void validateBoundMachines(ServerLevel level) {
        if (coreData == null) return;

        List<BlockPos> toRemove = new ArrayList<>();
        for (CivilizationCoreData.BoundMachineEntry entry : coreData.getBoundMachines()) {
            BlockPos pos = entry.getBlockPos();
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (!isMachineValid(level, pos, entry)) {
                toRemove.add(pos);
            }
        }

        for (BlockPos pos : toRemove) {
            if (level.getBlockEntity(pos) instanceof IPopulationMachine machine) {
                machine.setBound(false);
                machine.setBoundCoreUuid(null);
                machine.setBoundControllerDimension(null);
            }
            coreData.removeMachine(pos);
        }

        if (!toRemove.isEmpty()) {
            CoreDataManager.markDirty(currentUuid);
            setChanged();
        }
    }

    // ==================== NBT 持久化 ====================

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("WorkProgress", workProgress);
        ContainerHelper.saveAllItems(tag, items, registries);
        saveMultiBlockNBT(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        workProgress = tag.getInt("WorkProgress");
        ContainerHelper.loadAllItems(tag, items, registries);
        loadMultiBlockNBT(tag);
    }

    // ==================== Container boilerplate ====================

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> list) {
        for (int i = 0; i < this.items.size() && i < list.size(); i++) {
            this.items.set(i, list.get(i));
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    // ==================== Menu ====================

    /**
     * 玩家打开 GUI 时创建菜单，并立即同步机器列表。
     * 无核心时显式发送空列表，清除客户端可能残留的缓存数据。
     */
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        AbstractContainerMenu menu = createMenu(containerId, playerInventory);
        if (player instanceof ServerPlayer sp) {
            if (coreData != null) {
                syncToPlayer(sp);
            } else {
                NetworkHandler.sendToPlayer(sp, new SyncMachineListPayload(List.of()));
            }
        }
        return menu;
    }
}

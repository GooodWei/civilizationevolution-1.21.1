package com.gooodwei.civilizationevolution;

import com.gooodwei.civilizationevolution.api.CivilizationAPI;
import com.gooodwei.civilizationevolution.api.IMultiBlockMachine;
import com.gooodwei.civilizationevolution.api.PartOwnershipTracker;
import com.gooodwei.civilizationevolution.api.PreviewBlockInfo;
import com.gooodwei.civilizationevolution.api.career.Career;
import com.gooodwei.civilizationevolution.api.tier.CivilizationTiers;
import com.gooodwei.civilizationevolution.api.tier.TierRegistry;
import com.gooodwei.civilizationevolution.network.NetworkHandler;
import com.gooodwei.civilizationevolution.network.StructurePreviewPayload;
import com.gooodwei.civilizationevolution.server.career.initial.*;
import com.gooodwei.civilizationevolution.server.config.CareerConfig;
import com.gooodwei.civilizationevolution.server.config.CivilizationMachineConfig;
import com.gooodwei.civilizationevolution.server.config.MultiBlockConfig;
import com.gooodwei.civilizationevolution.server.config.PopulationConfig;
import com.gooodwei.civilizationevolution.server.coredata.CoreDataManager;
import com.gooodwei.civilizationevolution.server.item.CivilizationCoreItem;
import com.gooodwei.civilizationevolution.server.item.DebugStructureGetterItem;
import com.gooodwei.civilizationevolution.server.item.ProjectorItem;
import com.gooodwei.civilizationevolution.server.registry.BlockEntityRegistry;
import com.gooodwei.civilizationevolution.server.registry.ModRecipeTypes;
import com.gooodwei.civilizationevolution.server.registry.Registry;
import com.gooodwei.civilizationevolution.server.validation.StructureValidationService;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文明演进模组主入口类。
 *
 * <p>初始化顺序：
 * <ol>
 *   <li>加载 {@link PopulationConfig} 和 {@link CivilizationMachineConfig}</li>
 *   <li>注册所有初始职业</li>
 *   <li>注册方块、物品、BE、菜单、创造标签页</li>
 *   <li>注册网络 Payload 处理器</li>
 *   <li>注册 NeoForge 事件监听（服务器启动/停止时初始化/保存 CoreDataManager）</li>
 * </ol>
 */
@Mod(CivilizationEvolution.MODID)
public class CivilizationEvolution {

    /** 模组 ID，需与 {@code META-INF/neoforge.mods.toml} 中的条目一致 */
    public static final String MODID = "civilizationevolution";

    /** SLF4J 日志记录器 */
    public static final Logger LOGGER = LogUtils.getLogger();

    /** 跟踪当前正在预览多方块结构的玩家 UUID（用于切换开关） */
    private static final Set<UUID> PREVIEWING_PLAYERS = ConcurrentHashMap.newKeySet();

    /**
     * 模组构造器。FML 自动识别并注入 {@link IEventBus} 和 {@link ModContainer} 参数。
     *
     * @param modEventBus 模组事件总线
     * @param modContainer 模组容器
     */
    public CivilizationEvolution(IEventBus modEventBus, ModContainer modContainer) {
        // 初始化配置
        PopulationConfig.init();
        CivilizationMachineConfig.init();
        MultiBlockConfig.init();

        // 初始化所有职业（构造函数会自动注册到内部注册表中）
        initializeCareers();

        // 通过中央 Registry 注册所有方块、物品、创造标签页等
        Registry.registerAll(modEventBus);

        // 注册 commonSetup 方法和网络处理器
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(NetworkHandler::register);
        modEventBus.addListener(this::registerCapabilities);
        ModRecipeTypes.register(modEventBus);

        // 注册自身以监听服务器事件（onServerStarting / onServerStarted / onServerStopping）
        NeoForge.EVENT_BUS.register(this);

        // 触发内置 Tier 类加载注册
        CivilizationTiers.init();

        // 冻结 Tier 注册表（附属模组应在此之前注册自己的 Tier）
        TierRegistry.freeze();
        LOGGER.info("TierRegistry 已冻结，共注册 {} 个 Tier", TierRegistry.size());

        // 注入多方块结构验证服务，解耦 api/ 层对 server/ 层的硬依赖
        IMultiBlockMachine.VALIDATION_SERVICE.set(StructureValidationService::submitPeriodicValidation);
    }

    /**
     * 触发每个初始职业的类加载，使其将自身注册到
     * {@link Career} 的内部注册表中。
     */
    private static void initializeCareers() {
        new UnemployedCareer();
        new NitwitCareer();
        new ArmorerCareer();
        new ButcherCareer();
        new CartographerCareer();
        new ClericCareer();
        new FarmerCareer();
        new FishermanCareer();
        new FletcherCareer();
        new LeatherworkerCareer();
        new LibrarianCareer();
        new MasonCareer();
        new ShepherdCareer();
        new ToolsmithCareer();
        new WeaponsmithCareer();
        new MinerCareer();
        LOGGER.info("Registered {} careers", CivilizationAPI.getCareerRegistry().allCareers().size());

        // 加载职业树配置并应用（必须在所有 Career 构造完成后调用）
        CareerConfig.init();
        CareerConfig.applyToCareers();

        // 在所有 Career 构造和配置加载完成后，统一发送注册事件
        Career.fireRegisterEvents();
    }

    /** 模组通用初始化（逻辑端通用的设置） */
    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    /**
     * 注册方块实体的能力（Capability）。
     *
     * <p>为农场方块（原始农场、村庄农场）和流体仓室注册
     * {@link Capabilities.FluidHandler#BLOCK} 流体能力，
     * 使所有物流模组（Pipez、Mekanism、AE2、Integrated Dynamics 等）的管道
     * 均能通过 NeoForge 标准接口向储水罐输入水。
     *
     * @param event 能力注册事件
     */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 为原始农场注册流体能力（所有方向均可输入水）
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.PRIMITIVE_FARM.get(),
                (be, direction) -> be.getFluidHandler()
        );
        // 为村庄农场注册流体能力（所有方向均可输入水）
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.VILLAGE_FARM.get(),
                (be, direction) -> be.getFluidHandler()
        );
        // 为四个流体仓室注册流体能力
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.PRIMITIVE_FLUID_INPUT_HATCH.get(),
                (be, direction) -> be.getFluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.VILLAGE_FLUID_INPUT_HATCH.get(),
                (be, direction) -> be.getFluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.PRIMITIVE_FLUID_OUTPUT_HATCH.get(),
                (be, direction) -> be.getFluidHandler()
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.VILLAGE_FLUID_OUTPUT_HATCH.get(),
                (be, direction) -> be.getFluidHandler()
        );
        // 为村庄收割机注册流体能力（所有方向均可输入水）
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                BlockEntityRegistry.VILLAGE_HARVESTER.get(),
                (be, direction) -> be.getFluidHandler()
        );
        LOGGER.info("已注册农场和流体仓室流体能力（Capabilities.FluidHandler.BLOCK）");
    }

    /** 服务器启动中事件：日志输出 */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    /**
     * 服务器启动完成事件：初始化 CoreDataManager 的数据目录。
     * 此时世界目录已就绪，可以安全创建 coredata 子目录。
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        Path worldPath = event.getServer().getWorldPath(LevelResource.ROOT);
        CoreDataManager.init(worldPath);
    }

    /** 服务器停止事件：保存所有核心数据到磁盘，关闭验证线程池 */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        CoreDataManager.saveAll();
        com.gooodwei.civilizationevolution.server.validation.StructureValidationService.shutdown();
    }

    /**
     * 世界卸载事件：清除该维度的零件认领记录。
     *
     * <p>防止 {@link PartOwnershipTracker} 中残留过期条目。
     * 例如玩家离开末地时，末地维度中的所有外壳方块认领记录将被清除。
     */
    @SubscribeEvent
    public void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            PartOwnershipTracker.clearDimension(level);
        }
    }

    /**
     * 实体加入世界事件：为文明核心掉落物设置保护。
     *
     * <p>三个层次的保护：
     * <ul>
     *   <li>{@code fireResistant()} 在物品层面免疫火焰和岩浆</li>
     *   <li>{@code setInvulnerable(true)} 在实体层面免疫仙人掌、铁砧、爆炸等伤害</li>
     *   <li>{@code lifespan = Integer.MAX_VALUE} 阻止游戏 5 分钟后自动清理掉落物</li>
     * </ul>
     *
     * @param event 实体加入世界事件
     */
    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof ItemEntity itemEntity
                && itemEntity.getItem().getItem() instanceof CivilizationCoreItem) {
            itemEntity.setInvulnerable(true);
            itemEntity.lifespan = Integer.MAX_VALUE;
        }
    }

    /**
     * 右键方块事件 —— 为结构调试获取器抢先拦截带 GUI 的方块交互。
     *
     * <p>在 Minecraft 1.21.x 中，方块 {@code useItemOn} 的优先级高于物品
     * {@code useOn}。对于带 GUI 的方块（箱子、工作台、本模组机器等），
     * 方块直接在 {@code useItemOn} 中打开菜单并返回 SUCCESS/CONSUME，
     * 物品的 {@code useOn} 根本不会被调到。因此必须在方块处理<b>之前</b>
     * 通过此事件取消交互并执行坐标记录。
     *
     * <p>仅在服务端取消事件（客户端侧放行以正常发送网络包到服务端）。
     *
     * @param event 右键方块事件
     */
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 结构调试获取器 → 拦截 GUI，记录坐标
        if (event.getItemStack().getItem() instanceof DebugStructureGetterItem) {
            DebugStructureGetterItem.handleBlockClick(
                    event.getLevel(),
                    event.getEntity(),
                    event.getItemStack(),
                    event.getPos());
            if (!event.getLevel().isClientSide) {
                event.setCanceled(true);
            }
            return;
        }

        // 手持多方块结构投影仪右键多方块机器核心 → 切换结构预览渲染
        if (event.getItemStack().getItem() instanceof ProjectorItem) {
            handlePreviewToggle(event);
            return;
        }
    }

    /**
     * 处理手持木棍 Shift+右键多方块控制器：切换结构预览。
     *
     * <p>通用性检查（类型 + 结构成型状态）在双端都执行，
     * 确保客户端侧也能阻止 GUI 打开（各子类可能重写了 {@code useItemOn}）。
     * 实际预览切换逻辑仅服务端执行。
     *
     * @param event 右键方块事件
     */
    private void handlePreviewToggle(PlayerInteractEvent.RightClickBlock event) {
        // 仅对 IMultiBlockMachine 生效（双端检查）
        if (!(event.getLevel().getBlockEntity(event.getPos()) instanceof IMultiBlockMachine machine)) {
            return;
        }

        // 结构已成型 → 不触发预览，正常打开 GUI（双端检查）
        if (machine.isStructureFormed()) {
            return;
        }

        // 取消事件，阻止 GUI 打开（双端都必须取消，因为子类可能重写了 useItemOn）
        event.setCanceled(true);

        // 以下仅服务端执行
        if (event.getLevel().isClientSide) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        UUID playerId = player.getUUID();

        if (PREVIEWING_PLAYERS.contains(playerId)) {
            // 已显示预览 → 关闭
            PREVIEWING_PLAYERS.remove(playerId);
            NetworkHandler.sendToPlayer(player,
                    StructurePreviewPayload.stop(event.getPos()));
        } else {
            // 未显示 → 收集数据并开启预览
            List<PreviewBlockInfo> blocks = machine.collectPreviewPositions();
            if (blocks.isEmpty()) return;

            PREVIEWING_PLAYERS.add(playerId);
            NetworkHandler.sendToPlayer(player,
                    new StructurePreviewPayload(event.getPos(), blocks));
        }
    }
}

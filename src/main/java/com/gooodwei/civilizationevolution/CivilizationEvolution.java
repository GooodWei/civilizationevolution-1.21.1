package com.gooodwei.civilizationevolution;

import com.gooodwei.civilizationevolution.api.career.Career;
import com.gooodwei.civilizationevolution.api.CivilizationAPI;
import com.gooodwei.civilizationevolution.api.tier.ModTiers;
import com.gooodwei.civilizationevolution.api.tier.TierRegistry;
import com.gooodwei.civilizationevolution.network.NetworkHandler;
import com.gooodwei.civilizationevolution.server.career.initial.*;
import com.gooodwei.civilizationevolution.server.config.PopulationConfig;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.coredata.CoreDataManager;
import com.gooodwei.civilizationevolution.server.registry.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.nio.file.Path;

/**
 * 文明演进模组主入口类。
 *
 * <p>初始化顺序：
 * <ol>
 *   <li>加载 {@link PopulationConfig} 和 {@link PopulationMachineConfig}</li>
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

    /**
     * 模组构造器。FML 自动识别并注入 {@link IEventBus} 和 {@link ModContainer} 参数。
     *
     * @param modEventBus 模组事件总线
     * @param modContainer 模组容器
     */
    public CivilizationEvolution(IEventBus modEventBus, ModContainer modContainer) {
        // 初始化配置
        PopulationConfig.init();
        PopulationMachineConfig.init();

        // 初始化所有职业（构造函数会自动注册到内部注册表中）
        initializeCareers();

        // 通过中央 Registry 注册所有方块、物品、创造标签页等
        Registry.registerAll(modEventBus);

        // 注册 commonSetup 方法和网络处理器
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(NetworkHandler::register);

        // 注册自身以监听服务器事件（onServerStarting / onServerStarted / onServerStopping）
        NeoForge.EVENT_BUS.register(this);

        // 触发内置 Tier 类加载注册
        ModTiers.init();

        // 冻结 Tier 注册表（附属模组应在此之前注册自己的 Tier）
        TierRegistry.freeze();
        LOGGER.info("TierRegistry 已冻结，共注册 {} 个 Tier", TierRegistry.size());
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
        LOGGER.info("Registered {} careers", CivilizationAPI.getCareerRegistry().allCareers().size());
    }

    /** 模组通用初始化（逻辑端通用的设置） */
    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    /** 服务器启动中事件：日志输出 */
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("HELLO from server starting");
    }

    /**
     * 服务器启动完成事件：初始化 CoreDataManager 的数据目录。
     * 此时世界目录已就绪，可以安全创建 coredata 子目录。
     */
    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        Path worldPath = event.getServer().getWorldPath(LevelResource.ROOT);
        CoreDataManager.init(worldPath);
        LOGGER.info("CoreDataManager 已初始化，数据目录：{}",
                worldPath.getParent().resolve("civilizationevolution/coredata"));
    }

    /** 服务器停止事件：保存所有核心数据到磁盘 */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        CoreDataManager.saveAll();
        LOGGER.info("CoreDataManager 已保存所有数据");
    }
}

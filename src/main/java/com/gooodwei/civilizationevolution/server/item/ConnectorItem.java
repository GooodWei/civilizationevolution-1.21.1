package com.gooodwei.civilizationevolution.server.item;

import com.gooodwei.civilizationevolution.CivilizationEvolution;
import com.gooodwei.civilizationevolution.api.component.ConnectorTarget;
import com.gooodwei.civilizationevolution.api.IPMController;
import com.gooodwei.civilizationevolution.api.IPopulationMachine;
import com.gooodwei.civilizationevolution.api.component.ModDataComponents;
import com.gooodwei.civilizationevolution.server.blockentity.controller.AbstractControllerBlockEntity;
import com.gooodwei.civilizationevolution.server.config.PopulationMachineConfig;
import com.gooodwei.civilizationevolution.server.coredata.CivilizationCoreData;
import com.gooodwei.civilizationevolution.server.coredata.CoreDataManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/**
 * 连接器 —— 用于将人口机器绑定到文明核心。
 *
 * <p>使用方式：
 * <ul>
 *   <li>Shift+右键控制器方块 → 记录控制器中文明核心的 UUID</li>
 *   <li>右键机器方块 → 将机器绑定到已记录的核心</li>
 *   <li>Shift+右键空气 → 清除记录的核心 UUID</li>
 * </ul>
 *
 * <p>绑定数据保存在核心的 JSON 数据文件中，核心移动到新控制器时自动继承。
 */
public class ConnectorItem extends Item {

    public ConnectorItem(Properties properties) {
        super(properties);
    }

    // ==================== 物品提示 ====================

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        String uuid = getTargetUuid(stack);
        if (uuid != null) {
            tooltipComponents.add(Component.translatable(
                    "tooltip.civilizationevolution.connector.bound_uuid",
                    uuid).withStyle(ChatFormatting.GRAY));
        }
    }

    // ==================== 工具方法 ====================

    /** 从物品 DataComponent 读取目标核心 UUID，无目标时返回 null */
    public static String getTargetUuid(ItemStack stack) {
        ConnectorTarget target = stack.get(ModDataComponents.CONNECTOR_TARGET);
        return target != null ? target.coreUuid() : null;
    }

    /** 将目标核心 UUID 写入物品 DataComponent */
    public static void setTarget(ItemStack stack, String uuid) {
        stack.set(ModDataComponents.CONNECTOR_TARGET, new ConnectorTarget(uuid));
    }

    /** 清除物品上的目标核心 UUID */
    public static void clearTarget(ItemStack stack) {
        stack.remove(ModDataComponents.CONNECTOR_TARGET);
    }

    // ==================== 交互 ====================

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack itemInHand = player.getItemInHand(hand);
        if (itemInHand.getItem() instanceof ConnectorItem && player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                clearTarget(itemInHand);
                player.sendSystemMessage(Component.translatable("msg.civilizationevolution.connector.target_cleared"));
            }
            return InteractionResultHolder.sidedSuccess(itemInHand, level.isClientSide);
        }
        return InteractionResultHolder.pass(itemInHand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clickedPos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null) return InteractionResult.PASS;

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                // --- Shift+右键：从控制器读取核心 UUID ---
                BlockEntity be = level.getBlockEntity(clickedPos);
                if (be instanceof AbstractControllerBlockEntity controller) {
                    ItemStack coreStack = controller.getItem(0);
                    String uuid = CivilizationCoreItem.getUuid(coreStack);
                    if (uuid != null) {
                        setTarget(stack, uuid);
                        player.sendSystemMessage(Component.translatable(
                                "msg.civilizationevolution.connector.target_set",
                                uuid.substring(0, Math.min(8, uuid.length()))));
                    } else {
                        player.sendSystemMessage(Component.translatable(
                                "msg.civilizationevolution.connector.no_core_in_controller")
                                .withStyle(ChatFormatting.RED));
                    }
                }
            } else {
                // --- 普通右键：绑定机器到核心 ---
                String uuid = getTargetUuid(stack);
                if (uuid == null) {
                    player.sendSystemMessage(Component.translatable(
                            "msg.civilizationevolution.connector.no_target"));
                } else {
                    doBind(player, (ServerLevel) level, uuid, clickedPos, stack);
                }
            }
        }

        if (player.isShiftKeyDown()) {
            BlockEntity be = level.getBlockEntity(clickedPos);
            if (be instanceof AbstractControllerBlockEntity) {
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            return InteractionResult.PASS;
        }

        String uuid = ConnectorItem.getTargetUuid(stack);
        if (uuid != null) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /**
     * 将机器绑定到指定 UUID 的核心。
     * 优先通过持有该核心的控制器执行绑定，若无活跃控制器则直接写入核心数据文件。
     */
    private void doBind(Player player, ServerLevel level, String coreUuid,
                        BlockPos machinePos, ItemStack stack) {

        // 1. 加载核心数据
        CivilizationCoreData coreData = CoreDataManager.getOrLoad(coreUuid);
        if (coreData == null) {
            player.sendSystemMessage(Component.translatable(
                    "msg.civilizationevolution.connector.core_not_found")
                    .withStyle(ChatFormatting.RED));
            ConnectorItem.clearTarget(stack);
            return;
        }

        // 2. 目标方块是不是机器？
        BlockEntity machineBe = level.getBlockEntity(machinePos);
        if (!(machineBe instanceof IPopulationMachine machine)) {
            return;
        }

        // 3. 机器已绑定 → 根据旧核心 UUID 决定解绑还是切换
        if (machine.isBound()) {
            String oldUuid = machine.getBoundCoreUuid();
            if (oldUuid != null && oldUuid.equals(coreUuid)) {
                // 相同核心 → 解绑
                doUnbind(player, level, coreUuid, machinePos, machine, machineBe);
            } else {
                // 不同核心 → 先解绑旧核心，再绑定到新核心
                doSwitchBind(player, level, coreUuid, machinePos, machine, machineBe);
            }
            return;
        }

        // 4. 机器未绑定但核心数据中有记录 → 机器状态丢失，从核心数据恢复绑定
        //    核心数据是权威来源，机器的本地 isBound 状态可能因方块替换等原因丢失
        if (coreData.hasMachine(machinePos)) {
            CivilizationEvolution.LOGGER.info("doBind: 从核心数据恢复机器绑定状态，机位={}", machinePos);
            machine.setBound(true);
            machine.setBoundCoreUuid(coreUuid);
            // 尝试从活跃控制器获取维度信息
            AbstractControllerBlockEntity controller = findController(level, coreUuid);
            if (controller != null) {
                machine.setBoundControllerDimension(level.dimension().location().toString());
            }
            // 若当前无活跃控制器，维度信息将在核心下次放入控制器时由 syncBoundMachinesLocation 推送
            machineBe.setChanged();
            player.sendSystemMessage(Component.translatable(
                    "msg.civilizationevolution.connector.bind_success",
                    machinePos.getX(), machinePos.getY(), machinePos.getZ()));
            return;
        }

        // 5. 执行全新绑定
        if (performBind(player, level, coreUuid, machinePos, machine, machineBe, coreData)) {
            player.sendSystemMessage(Component.translatable(
                    "msg.civilizationevolution.connector.bind_success",
                    machinePos.getX(), machinePos.getY(), machinePos.getZ()));
        }
    }

    /**
     * 从核心中解绑机器（连接器记录的 UUID 与机器绑定的 UUID 相同）。
     */
    private void doUnbind(Player player, ServerLevel level, String coreUuid,
                          BlockPos machinePos, IPopulationMachine machine, BlockEntity machineBe) {
        // 查找持有此核心的活跃控制器
        AbstractControllerBlockEntity controller = findController(level, coreUuid);
        if (controller != null) {
            controller.unbindMachine(machinePos, level);
            controller.setChanged();
            controller.notifyViewersSync();
        } else {
            // 无活跃控制器 → 直接从核心数据文件中移除并立即落盘
            CivilizationCoreData coreData = CoreDataManager.getOrLoad(coreUuid);
            if (coreData != null) {
                coreData.removeMachine(machinePos);
                CoreDataManager.markDirty(coreUuid);
                CoreDataManager.saveDirty();
            }
            machine.setBound(false);
            machine.setBoundCoreUuid(null);
            machine.setBoundControllerDimension(null);
            machineBe.setChanged();
        }
        player.sendSystemMessage(Component.translatable(
                "msg.civilizationevolution.connector.unbind_success",
                machinePos.getX(), machinePos.getY(), machinePos.getZ()));
    }

    /**
     * 切换绑定：清理旧核心残留并绑定到新核心。
     * 实际清理和绑定由 {@link #performBind} 统一完成。
     */
    private void doSwitchBind(Player player, ServerLevel level, String newUuid,
                              BlockPos machinePos, IPopulationMachine machine, BlockEntity machineBe) {
        CivilizationCoreData newData = CoreDataManager.getOrLoad(newUuid);
        if (newData == null) {
            player.sendSystemMessage(Component.translatable(
                    "msg.civilizationevolution.connector.core_not_found")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (performBind(player, level, newUuid, machinePos, machine, machineBe, newData)) {
            player.sendSystemMessage(Component.translatable(
                    "msg.civilizationevolution.connector.switch_success",
                    machinePos.getX(), machinePos.getY(), machinePos.getZ()));
        }
    }

    /**
     * 执行绑定操作。
     * 绑定前会先清理该机器在<b>任何</b>旧核心中的残留绑定数据，
     * 确保不会出现一台机器被多个核心同时控制的局面。
     *
     * @return true 绑定成功，false 失败
     */
    private boolean performBind(Player player, ServerLevel level, String coreUuid,
                             BlockPos machinePos, IPopulationMachine machine,
                             BlockEntity machineBe, CivilizationCoreData coreData) {
        // ---- 0. 先清理旧核心中的残留绑定（兜底保障） ----
        String oldUuid = machine.getBoundCoreUuid();
        if (oldUuid != null && !oldUuid.equals(coreUuid)) {
            AbstractControllerBlockEntity oldController = findController(level, oldUuid);
            if (oldController != null) {
                oldController.unbindMachine(machinePos, level);
                oldController.setChanged();
                oldController.notifyViewersSync();
            } else {
                CivilizationCoreData oldData = CoreDataManager.getOrLoad(oldUuid);
                if (oldData != null) {
                    oldData.removeMachine(machinePos);
                    CoreDataManager.markDirty(oldUuid);
                    CoreDataManager.saveDirty();
                }
            }
        }
        // 无论之前什么状态，绑定前统一重置机器
        machine.setBound(false);
        machine.setBoundCoreUuid(null);

        // ---- 1. 执行绑定 ----
        AbstractControllerBlockEntity controller = findController(level, coreUuid);

        if (controller != null) {
            CivilizationEvolution.LOGGER.info("performBind: 找到控制器位于 {}，尝试绑定机位 {}", controller.getBlockPos(), machinePos);
            // Tier 预检查：控制器只能绑定 ≤ 自身 tier 的机器
            if (machine.getTier().getLevel() > controller.getTier().getLevel()) {
                player.sendSystemMessage(Component.translatable(
                        "msg.civilizationevolution.connector.tier_mismatch",
                        machine.getTier().getLevel(), controller.getTier().getLevel())
                        .withStyle(ChatFormatting.RED));
                return false;
            }
            // 距离预检查（在进 bindMachine 之前，给玩家具体的"超出范围"提示）
            int maxRange = PopulationMachineConfig.getMaxBindRange(controller.getControllerType());
            if (maxRange > 0 && !machinePos.closerThan(controller.getBlockPos(), maxRange + 1)) {
                player.sendSystemMessage(Component.translatable(
                        "msg.civilizationevolution.connector.out_of_range", maxRange)
                        .withStyle(ChatFormatting.RED));
                return false;
            }

            // 通过控制器绑定（含 Tier、数量上限等检查）
            boolean success = controller.bindMachine(machinePos, machine);
            if (success) {
                controller.setChanged();
                if (player instanceof ServerPlayer sp) controller.syncToPlayer(sp);
            } else {
                CivilizationEvolution.LOGGER.warn("performBind: controller.bindMachine 返回 false，机位={}", machinePos);
                player.sendSystemMessage(Component.translatable(
                        "msg.civilizationevolution.connector.bind_failed")
                        .withStyle(ChatFormatting.RED));
            }
            return success;
        } else {
            // 无活跃控制器 → 直接写入核心数据文件
            String controllerType = coreData.getControllerType();
            if (controllerType == null) controllerType = "primitive_controller";
            if (coreData.getBoundMachines().size() >= PopulationMachineConfig.getMaxBindCount(controllerType)) {
                player.sendSystemMessage(Component.translatable(
                        "msg.civilizationevolution.connector.controller_full")
                        .withStyle(ChatFormatting.RED));
                return false;
            }

            // 距离检查：通过核心最后一次记录的控制器位置判断
            BlockPos lastKnownPos = AbstractControllerBlockEntity.getCoreLocation(coreUuid);
            int maxRange = PopulationMachineConfig.getMaxBindRange(controllerType);
            if (lastKnownPos != null && maxRange > 0 && !machinePos.closerThan(lastKnownPos, maxRange + 1)) {
                player.sendSystemMessage(Component.translatable(
                        "msg.civilizationevolution.connector.out_of_range", maxRange)
                        .withStyle(ChatFormatting.RED));
                return false;
            }

            machine.setBound(true);
            machine.setBoundCoreUuid(coreUuid);
            machine.setBoundControllerDimension(level.dimension().location().toString());
            String machineType = "";
            if (machineBe instanceof BlockEntity be) {
                machineType = BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).toString();
            }
            coreData.addMachine(machinePos, 0, true, machineType, machine.getTier().getLevel());
            CoreDataManager.markDirty(coreUuid);
            CoreDataManager.saveDirty();
            machineBe.setChanged();
            return true;
        }
    }

    /**
     * 通过核心 UUID 索引查找持有该核心的活跃控制器。
     * @return 找到的控制器，未找到时返回 null
     */
    private static AbstractControllerBlockEntity findController(ServerLevel level, String uuid) {
        BlockPos pos = AbstractControllerBlockEntity.getCoreLocation(uuid);
        if (pos != null && level.getBlockEntity(pos) instanceof AbstractControllerBlockEntity controller
                && uuid.equals(controller.getCurrentUuid())) {
            return controller;
        }
        return null;
    }
}

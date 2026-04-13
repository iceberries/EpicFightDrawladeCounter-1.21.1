package com.ice_berry.drawlade_counter;

import com.ice_berry.drawlade_counter.capability.WeaponFlowProvider;
import com.ice_berry.drawlade_counter.combat.ParryCounterHandler;
import com.ice_berry.drawlade_counter.combat.SupportAttackHandler;
import com.ice_berry.drawlade_counter.config.Config;
import com.ice_berry.drawlade_counter.config.DataDrivenLoader;
import com.ice_berry.drawlade_counter.datagen.EFDCDatagen;
import com.ice_berry.drawlade_counter.network.NetworkHandler;

import com.ice_berry.drawlade_counter.until.epicfight.EFEventsHandler;
import com.ice_berry.drawlade_counter.until.epicfight.EFSkillIntegration;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

@Mod(EFDCMod.MODID)
public class EFDCMod {
    public static final String MODID = "epicfightdrawladecounter";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("epicfight_drawlade_counter", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.epicfightdrawladecounter"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());
            }).build());

    public EFDCMod(IEventBus modEventBus, ModContainer modContainer) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        // MOD 总线
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(EFDCMod::onRegisterCapabilities);
        modEventBus.addListener(this::onRegisterPayloads);
        modEventBus.addListener(EFDCDatagen::gatherData);

        // GAME 总线
        NeoForge.EVENT_BUS.addListener(EFDCMod::onPlayerClone);
        NeoForge.EVENT_BUS.addListener(EFDCMod::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(EFDCMod::onServerTick);

        // 监听 LivingIncomingDamageEvent（双优先级：继承 EF 原版格挡机制）
        // HIGH: 在 EF 处理前激活 GuardSkill（兼容耐力/动画/音效）
        // LOW:  在 EF 处理后检测格挡结果，触发弹反反击或通知客户端
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, EFDCMod::onDamagePreGuard);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOW, EFDCMod::onDamagePostParry);
        // NORMAL: 检测 EF 原生格挡命中（非弹反模式）
        NeoForge.EVENT_BUS.addListener(EFDCMod::onGuardHitNotify);

        // 配置
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 初始化（检测 Epic Fight）
        SupportAttackHandler.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(EFEventsHandler::register);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            event.accept(EXAMPLE_BLOCK_ITEM);
        }
    }

    @SubscribeEvent
    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new DataDrivenLoader());
    }

    // #region Capability 注册

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
                WeaponFlowProvider.WEAPON_FLOW_CAPABILITY,
                EntityType.PLAYER,
                new WeaponFlowProvider()
        );
    }

    // #endregion

    // #region 网络包注册

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        NetworkHandler.register(event);
    }

    // #endregion

    // #region 玩家事件

    private static void onPlayerClone(PlayerEvent.Clone event) {
        WeaponFlowProvider.copyCapability(event.getOriginal(), event.getEntity());
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        WeaponFlowProvider.saveCapability(event.getEntity());
        WeaponFlowProvider.removeCapability(event.getEntity().getUUID());
        EFEventsHandler.cleanup(event.getEntity().getUUID());
    }

    // #endregion

    // #region 弹反格挡检测（双优先级处理器）

    /**
     * HIGH 优先级处理器：在 EF 处理伤害之前激活 GuardSkill。
     * 当玩家有待定弹反标记时，在伤害事件到达 EF 之前
     * 调用 requestHold 激活格挡，使 EF 的 TAKE_DAMAGE_INCOME
     * 监听器能检测到格挡状态并正常执行原版格挡逻辑
     * （耐力消耗、格挡动画、正面检测、音效粒子等）。
     *
     * @param event NeoForge 传入伤害事件
     */
    private static void onDamagePreGuard(LivingIncomingDamageEvent event) {
        if (!ModList.get().isLoaded("epicfight")) return;
        if (!Config.PERFECT_GUARD_ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        LivingEntity pendingTarget = EFEventsHandler.getPendingParryTarget(serverPlayer);
        if (pendingTarget == null) return;

        // 验证攻击者是否为弹反目标
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity le
                ? le : null;
        LOGGER.debug("[EFDC] HIGH: player={}, pendingTarget={}, attacker={}",
                serverPlayer.getName().getString(),
                pendingTarget.getName().getString(),
                attacker != null ? attacker.getName().getString() : "null");
        if (attacker == null || attacker != pendingTarget) return;

        // 在 EF 处理前激活 GuardSkill（兼容耐力和动画）
        boolean activated = EFSkillIntegration.forceActivateGuard(serverPlayer);
        LOGGER.debug("[EFDC] HIGH: forceActivateGuard 结果={}", activated);
    }

    /**
     * LOW 优先级处理器：在 EF 处理伤害之后检测弹反结果。
     * 如果 EF 的 GuardSkill 成功格挡（事件被取消），触发弹反反击
     * 并取消我们激活的格挡状态。
     * 如果 GuardSkill 未激活或格挡失败，回退到直接取消伤害。
     *
     * @param event NeoForge 传入伤害事件
     */
    private static void onDamagePostParry(LivingIncomingDamageEvent event) {
        if (!ModList.get().isLoaded("epicfight")) return;
        if (!Config.PERFECT_GUARD_ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        LivingEntity pendingTarget = EFEventsHandler.getPendingParryTarget(serverPlayer);
        if (pendingTarget == null) return;

        // 验证攻击者是否为弹反目标
        LivingEntity attacker = event.getSource().getEntity() instanceof LivingEntity le
                ? le : null;
        if (attacker == null || attacker != pendingTarget) return;

        LOGGER.debug("[EFDC] LOW: player={}, canceled={}, attacker={}",
                serverPlayer.getName().getString(), event.isCanceled(),
                attacker.getName().getString());

        if (event.isCanceled()) {
            LOGGER.debug("[EFDC] LOW: EF 格挡成功，触发反击");
            EFEventsHandler.clearPendingParry(serverPlayer);
            EFSkillIntegration.cancelGuard(serverPlayer);
            ParryCounterHandler.triggerPerfectGuard(serverPlayer, attacker, null);
        } else {
            LOGGER.debug("[EFDC] LOW: EF 格挡失败，回退直接取消伤害");
            event.setCanceled(true);
            EFEventsHandler.clearPendingParry(serverPlayer);
            ParryCounterHandler.triggerPerfectGuard(serverPlayer, attacker, null);
        }
    }

    // #endregion

    // #region EF 原生格挡命中通知

    /**
     * 监听 NeoForge 的 LivingIncomingDamageEvent（NORMAL 优先级）。
     * 当前仅用于调试日志，弹反窗口由客户端 isEntityAttacking 检测驱动。
     *
     * @param event NeoForge 传入伤害事件
     */
    private static void onGuardHitNotify(LivingIncomingDamageEvent event) {
        if (!ModList.get().isLoaded("epicfight")) return;
        if (!Config.PERFECT_GUARD_ENABLED.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        if (EFEventsHandler.getPendingParryTarget(serverPlayer) != null) return;

        boolean isCanceled = event.isCanceled();
        boolean guardActivated = EFSkillIntegration.isGuardSkillActivated(serverPlayer);
        LOGGER.debug("[EFDC] NORMAL: player={}, canceled={}, guardActivated={}",
                serverPlayer.getName().getString(), isCanceled, guardActivated);
    }

    // #endregion

    // #region 服务端 Tick

    private static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        if (server == null) return;

        for (var player : server.getPlayerList().getPlayers()) {
            var cap = WeaponFlowProvider.getCapability(player);
            if (cap != null) {
                cap.tick();
            }
        }
    }

    // #endregion
}


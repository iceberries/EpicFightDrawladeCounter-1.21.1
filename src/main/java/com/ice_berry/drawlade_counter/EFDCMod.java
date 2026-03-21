package com.ice_berry.drawlade_counter;

import com.ice_berry.drawlade_counter.capability.WeaponFlowProvider;
import com.ice_berry.drawlade_counter.combat.SupportAttackHandler;
import com.ice_berry.drawlade_counter.config.Config;
import com.ice_berry.drawlade_counter.config.DataDrivenLoader;
import com.ice_berry.drawlade_counter.datagen.EFDCDatagen;
import com.ice_berry.drawlade_counter.network.NetworkHandler;
import com.ice_berry.drawlade_counter.until.epicfight.EFEventsHandler;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
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

        // 配置
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 初始化（检测 Epic Fight）
        SupportAttackHandler.init();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("EFDC Common Setup");
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

    // ==================== Capability 注册 ====================

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.registerEntity(
                WeaponFlowProvider.WEAPON_FLOW_CAPABILITY,
                EntityType.PLAYER,
                new WeaponFlowProvider()
        );
        LOGGER.info("EFDC WeaponFlow Capability registered");
    }

    // ==================== 网络包注册 ====================

    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        NetworkHandler.register(event);
    }

    // ==================== 玩家事件 ====================

    private static void onPlayerClone(PlayerEvent.Clone event) {
        WeaponFlowProvider.copyCapability(event.getOriginal(), event.getEntity());
    }

    private static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        WeaponFlowProvider.saveCapability(event.getEntity());
        WeaponFlowProvider.removeCapability(event.getEntity().getUUID());
    }

    // ==================== 服务端 Tick ====================

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
}

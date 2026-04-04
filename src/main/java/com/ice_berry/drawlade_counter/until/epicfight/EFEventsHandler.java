package com.ice_berry.drawlade_counter.until.epicfight;

import com.ice_berry.drawlade_counter.EFDCMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * Epic Fight 事件监听器
 * 监听 EF 的事件总线，在关键时刻注入武器流转逻辑。
 */
public final class EFEventsHandler {

    private static boolean registered = false;

    private EFEventsHandler() {}

    /**
     * 尝试向 Epic Fight 的事件总线注册监听器
     * 应在 FMLCommonSetupEvent 中调用。
     */
    public static void register() {
        if (!ModList.get().isLoaded("epicfight")) {
            EFDCMod.LOGGER.info("Epic Fight not loaded, skipping EF event registration");
            return;
        }

        if (registered) return;

        try {
            registerNeoForgeEvents();
            registered = true;
            EFDCMod.LOGGER.info("Epic Fight event handlers registered successfully");
        } catch (Exception e) {
            EFDCMod.LOGGER.error("Failed to register Epic Fight event handlers", e);
        }
    }

    /**
     * 注册 EF 的 NeoForge 事件监听
     * EF 在 NeoForge 1.21.1 中将其事件挂在 NeoForge 事件总线上。
     */
    private static void registerNeoForgeEvents() {
        try {
            // EF 在 NeoForge 上的事件类
            // 例如 yesman.epicfight.api.neoevent.PlayerAttackEvent 等
            // 这里为将来的扩展预留注册点

            Class<?> neoEventPackage = Class.forName("yesman.epicfight.api.neoevent.PlayerAttackEvent");
            EFDCMod.LOGGER.info("Epic Fight neoevent package found, event integration available");

        } catch (ClassNotFoundException e) {
            EFDCMod.LOGGER.warn("Epic Fight neoevent classes not found - using fallback mode");
        }
    }
}

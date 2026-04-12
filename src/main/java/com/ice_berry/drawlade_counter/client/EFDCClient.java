package com.ice_berry.drawlade_counter.client;

import com.ice_berry.drawlade_counter.EFDCMod;
import com.ice_berry.drawlade_counter.capability.IWeaponFlowCapability;
import com.ice_berry.drawlade_counter.capability.WeaponFlowProvider;
import com.ice_berry.drawlade_counter.client.gui.PerfectGuardCrosshairOverlay;
import com.ice_berry.drawlade_counter.client.gui.WeaponFlowHud;
import com.ice_berry.drawlade_counter.client.input.EFDCKeyBindings;
import com.ice_berry.drawlade_counter.client.input.WeaponSwitchHandler;
import com.ice_berry.drawlade_counter.until.epicfight.EFSkillIntegration;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent.Post;

@Mod(value = EFDCMod.MODID, dist = Dist.CLIENT)
public class EFDCClient {

    public EFDCClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(EFDCClient::onClientSetup);
        modEventBus.addListener(EFDCClient::registerKeyMappings);
        modEventBus.addListener(EFDCClient::registerGuiLayers);

        NeoForge.EVENT_BUS.addListener(EFDCClient::onKeyInput);
        NeoForge.EVENT_BUS.addListener(EFDCClient::onMouseScroll);
        NeoForge.EVENT_BUS.addListener(EFDCClient::onClientTick);
    }

    static void onClientSetup(FMLClientSetupEvent event) {
    }

    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(EFDCKeyBindings.PREVIOUS_WEAPON);
        event.register(EFDCKeyBindings.NEXT_WEAPON);
    }

    /**
     * 注册 HUD 图层
     */
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        WeaponFlowHud.register(event);
        PerfectGuardCrosshairOverlay.register(event);
    }

    /**
     * 键盘输入 — 处理武器切换按键
     */
    static void onKeyInput(InputEvent.Key event) {
        WeaponSwitchHandler.onKeyInput(event);
    }

    /**
     * 鼠标滚轮 — 动画播放中拦截滚轮切换快捷栏
     */
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (EFSkillIntegration.isPlayerInAttackAnimation(mc.player)) {
            if (event.getScrollDeltaY() != 0) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 客户端 Tick — 武器流转检测 + 冷却
     */
    static void onClientTick(Post event) {
        WeaponSwitchHandler.onClientTick(event);

        // 客户端冷却倒计时
        var mc = Minecraft.getInstance();
        if (mc.player != null && mc.screen == null) {
            IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(mc.player);
            if (cap != null) {
                cap.tick();
            }
        }
    }
}

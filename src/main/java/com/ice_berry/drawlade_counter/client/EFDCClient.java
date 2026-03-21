package com.ice_berry.drawlade_counter.client;

import com.ice_berry.drawlade_counter.EFDCMod;
import com.ice_berry.drawlade_counter.client.input.EFDCKeyBindings;
import com.ice_berry.drawlade_counter.client.input.WeaponSwitchHandler;
import com.ice_berry.drawlade_counter.client.render.AttackEffectRenderer;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
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

        // MOD 总线事件
        modEventBus.addListener(EFDCClient::onClientSetup);
        modEventBus.addListener(EFDCClient::registerKeyMappings);

        // GAME 总线事件（客户端）
        NeoForge.EVENT_BUS.addListener(EFDCClient::onKeyInput);
        NeoForge.EVENT_BUS.addListener(EFDCClient::onClientTick);
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        EFDCMod.LOGGER.info("EFDC Client Setup");
    }

    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(EFDCKeyBindings.PREVIOUS_WEAPON);
        event.register(EFDCKeyBindings.NEXT_WEAPON);
        EFDCMod.LOGGER.info("EFDC Key Mappings registered");
    }

    /**
     * 键盘输入事件 — 处理武器切换按键
     */
    static void onKeyInput(InputEvent.Key event) {
        WeaponSwitchHandler.onKeyInput(event);
    }

    /**
     * 客户端 Tick — 武器流转检测 + 攻击特效渲染
     */
    static void onClientTick(Post event) {
        WeaponSwitchHandler.onClientTick(event);
        AttackEffectRenderer.tick();
    }
}

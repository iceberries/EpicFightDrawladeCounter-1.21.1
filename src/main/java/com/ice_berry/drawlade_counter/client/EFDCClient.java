package com.ice_berry.drawlade_counter.client;

import com.ice_berry.drawlade_counter.EFDCMod;
import com.ice_berry.drawlade_counter.capability.IWeaponFlowCapability;
import com.ice_berry.drawlade_counter.capability.WeaponFlowProvider;
import com.ice_berry.drawlade_counter.client.gui.WeaponFlowHud;
import com.ice_berry.drawlade_counter.client.input.EFDCKeyBindings;
import com.ice_berry.drawlade_counter.client.input.WeaponSwitchHandler;
import com.ice_berry.drawlade_counter.client.render.AttackEffectRenderer;
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

        // MOD 总线事件
        modEventBus.addListener(EFDCClient::onClientSetup);
        modEventBus.addListener(EFDCClient::registerKeyMappings);
        modEventBus.addListener(EFDCClient::registerGuiLayers);

        // GAME 总线事件（客户端）
        NeoForge.EVENT_BUS.addListener(EFDCClient::onKeyInput);
        NeoForge.EVENT_BUS.addListener(EFDCClient::onMouseScroll);
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
     * 注册 HUD 图层 —— 武器流转槽位指示器
     */
    static void registerGuiLayers(RegisterGuiLayersEvent event) {
        WeaponFlowHud.register(event);
        EFDCMod.LOGGER.info("EFDC GUI Layers registered");
    }

    /**
     * 键盘输入事件 — 处理武器切换按键
     * 流转键的动画拦截在 WeaponSwitchHandler 中处理
     */
    static void onKeyInput(InputEvent.Key event) {
        WeaponSwitchHandler.onKeyInput(event);
    }

    /**
     * 鼠标滚轮事件 — 动画播放中拦截滚轮切换快捷栏
     */
    static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 动画播放中拦截鼠标滚轮，阻止切换快捷栏
        if (EFSkillIntegration.isPlayerInAttackAnimation(mc.player)) {
            double delta = event.getScrollDeltaY();
            if (delta != 0) {
                event.setCanceled(true);
            }
        }
    }

    /**
     * 客户端 Tick — 武器流转检测 + 冷却倒计时 + 攻击特效渲染
     */
    static void onClientTick(Post event) {
        WeaponSwitchHandler.onClientTick(event);

        // 客户端冷却倒计时（用于 HUD 冷却指示器动画）
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.screen == null) {
            IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(mc.player);
            if (cap != null) {
                cap.tick();
            }
        }

        AttackEffectRenderer.tick();
    }
}

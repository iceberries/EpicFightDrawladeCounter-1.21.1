package com.ice_berry.drawlade_counter.client;

import com.ice_berry.drawlade_counter.EFDCMod;
import com.ice_berry.drawlade_counter.client.input.EFDCKeyBindings;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = EFDCMod.MODID, dist = Dist.CLIENT)
public class EFDCClient {

    public EFDCClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(EFDCClient::onClientSetup);
        modEventBus.addListener(EFDCClient::registerKeyMappings);
    }

    static void onClientSetup(FMLClientSetupEvent event) {
        EFDCMod.LOGGER.info("EFDC Client Setup");
        EFDCMod.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(EFDCKeyBindings.PREVIOUS_WEAPON);
        event.register(EFDCKeyBindings.NEXT_WEAPON);
        EFDCMod.LOGGER.info("EFDC Key Mappings registered");
    }
}

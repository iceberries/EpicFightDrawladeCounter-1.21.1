package com.ice_berry.drawlade_counter.network;

import com.ice_berry.drawlade_counter.EFDCMod;
import com.ice_berry.drawlade_counter.network.packets.CooldownUpdatePacket;
import com.ice_berry.drawlade_counter.network.packets.TriggerEffectPacket;
import com.ice_berry.drawlade_counter.network.packets.WeaponSwitchPacket;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.bus.api.SubscribeEvent;

/**
 * 网络处理器
 * 集中注册所有 C2S / S2C 数据包，并提供发送工具方法。
 */
public class NetworkHandler {
    
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar("1");
        
        // 注册 WeaponSwitchPacket（客户端 → 服务端）
        registrar.playToServer(
                WeaponSwitchPacket.TYPE,
                WeaponSwitchPacket.STREAM_CODEC,
                WeaponSwitchPacket::handle
        );

        // 注册 TriggerEffectPacket（服务端 → 客户端）
        registrar.playToClient(
                TriggerEffectPacket.TYPE,
                TriggerEffectPacket.STREAM_CODEC,
                NetworkHandler::handleTriggerEffect  // 方法引用
        );
        
        // 注册 CooldownUpdatePacket（同样方式）
        registrar.playToClient(
                CooldownUpdatePacket.TYPE,
                CooldownUpdatePacket.STREAM_CODEC,
                NetworkHandler::handleCooldownUpdate
        );
    }
    
    // 客户端处理 TriggerEffectPacket
    private static void handleTriggerEffect(TriggerEffectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 在这里执行客户端特效逻辑
            // ClientEffectHandler.playParticle(...)
        });
    }
    
    private static void handleCooldownUpdate(CooldownUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // 更新客户端冷却显示
        });
    }
    
    // 发送方法（静态工具方法）
    public static void sendToClient(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}

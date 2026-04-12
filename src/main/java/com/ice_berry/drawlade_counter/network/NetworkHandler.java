package com.ice_berry.drawlade_counter.network;

import com.ice_berry.drawlade_counter.network.packets.CooldownUpdatePacket;
import com.ice_berry.drawlade_counter.network.packets.GuardHitPacket;
import com.ice_berry.drawlade_counter.network.packets.PerfectGuardStatePacket;
import com.ice_berry.drawlade_counter.network.packets.PerfectGuardTriggerPacket;
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

        // 注册 CooldownUpdatePacket（服务端 → 客户端）
        registrar.playToClient(
                CooldownUpdatePacket.TYPE,
                CooldownUpdatePacket.STREAM_CODEC,
                NetworkHandler::handleCooldownUpdate
        );

        // 注册 PerfectGuardStatePacket（服务端 → 客户端）
        registrar.playToClient(
                PerfectGuardStatePacket.TYPE,
                PerfectGuardStatePacket.STREAM_CODEC,
                NetworkHandler::handlePerfectGuardState
        );

        // 注册 GuardHitPacket（服务端 → 客户端）
        registrar.playToClient(
                GuardHitPacket.TYPE,
                GuardHitPacket.STREAM_CODEC,
                NetworkHandler::handleGuardHit
        );

        // 注册 PerfectGuardTriggerPacket（客户端 → 服务端）
        registrar.playToServer(
                PerfectGuardTriggerPacket.TYPE,
                PerfectGuardTriggerPacket.STREAM_CODEC,
                PerfectGuardTriggerPacket::handle
        );
    }
    
    private static void handleCooldownUpdate(CooldownUpdatePacket packet, IPayloadContext context) {
        CooldownUpdatePacket.handle(packet, context);
    }

    // 客户端处理 PerfectGuardStatePacket
    private static void handlePerfectGuardState(PerfectGuardStatePacket packet, IPayloadContext context) {
        PerfectGuardStatePacket.handle(packet, context);
    }

    // 客户端处理 GuardHitPacket
    private static void handleGuardHit(GuardHitPacket packet, IPayloadContext context) {
        GuardHitPacket.handle(packet, context);
    }
    
    // 发送方法（静态工具方法）
    public static void sendToClient(ServerPlayer player, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayer(player, packet);
    }
}

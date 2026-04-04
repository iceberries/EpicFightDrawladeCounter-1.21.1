package com.ice_berry.drawlade_counter.network.packets;

import com.ice_berry.drawlade_counter.combat.WeaponFlowManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 武器切换数据包 (C2S)
 * 客户端按键后发送到服务端，请求切换武器。
 * 携带切换前的旧槽位索引，确保服务端在集成服务器（客户端和服务端共享 Capability 实例）下
 * 也能正确计算新旧槽位，避免冷却设置到错误的槽位。
 */
public record WeaponSwitchPacket(int oldSlot, int direction) implements CustomPacketPayload {

    public static final Type<WeaponSwitchPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("epicfightdrawladecounter", "weapon_switch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponSwitchPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeByte(packet.oldSlot);
                        buf.writeByte(packet.direction);
                    },
                    buf -> new WeaponSwitchPacket(buf.readByte(), buf.readByte())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 服务端处理：触发武器切换逻辑
     */
    public static void handle(WeaponSwitchPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                WeaponFlowManager.switchWeapon(serverPlayer, packet.oldSlot, packet.direction);
            }
        });
    }
}

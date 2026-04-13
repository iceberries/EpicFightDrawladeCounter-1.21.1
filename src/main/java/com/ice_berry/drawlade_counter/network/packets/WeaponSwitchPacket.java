package com.ice_berry.drawlade_counter.network.packets;

import com.ice_berry.drawlade_counter.combat.WeaponFlowManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 武器切换数据包 (C2S)
 * 携带旧槽位索引，确保服务端正确计算新旧槽位。
 * skipSupportAttack 标志用于弹反窗口触发的切换，跳过特殊支援攻击。
 */
public record WeaponSwitchPacket(int oldSlot, int direction, boolean skipSupportAttack) implements CustomPacketPayload {

    public static final Type<WeaponSwitchPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("epicfightdrawladecounter", "weapon_switch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WeaponSwitchPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeByte(packet.oldSlot);
                        buf.writeByte(packet.direction);
                        buf.writeBoolean(packet.skipSupportAttack);
                    },
                    buf -> new WeaponSwitchPacket(buf.readByte(), buf.readByte(), buf.readBoolean())
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
                WeaponFlowManager.switchWeapon(serverPlayer, packet.oldSlot, packet.direction,
                        packet.skipSupportAttack);
            }
        });
    }
}

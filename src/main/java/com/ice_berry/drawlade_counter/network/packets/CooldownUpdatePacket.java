package com.ice_berry.drawlade_counter.network.packets;

import com.ice_berry.drawlade_counter.capability.IWeaponFlowCapability;
import com.ice_berry.drawlade_counter.capability.WeaponFlowProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 冷却更新数据包 (S2C)
 * 服务端通知客户端某个武器槽位的冷却状态。
 *
 * <p>协议：{@code [byte slot, varint cooldownTicks]}</p>
 */
public record CooldownUpdatePacket(int slot, int cooldownTicks) implements CustomPacketPayload {

    public static final Type<CooldownUpdatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("epicfightdrawladecounter", "cooldown_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CooldownUpdatePacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeByte(packet.slot);
                        buf.writeVarInt(packet.cooldownTicks);
                    },
                    buf -> new CooldownUpdatePacket(buf.readByte(), buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 客户端处理：更新本地 Capability 中的冷却数据
     * 用于 HUD 渲染冷却指示器。
     */
    public static void handle(CooldownUpdatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);
            if (cap != null) {
                cap.setSlotCooldown(packet.slot, packet.cooldownTicks);
            }
        });
    }
}

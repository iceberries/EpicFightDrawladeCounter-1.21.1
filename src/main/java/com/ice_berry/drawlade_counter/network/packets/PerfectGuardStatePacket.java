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
 * 完美格挡状态同步数据包 (S2C)
 * 服务端通知客户端玩家是否持有强化攻击状态及剩余时间。
 */
public record PerfectGuardStatePacket(byte active, int remainingTicks) implements CustomPacketPayload {

    public static final Type<PerfectGuardStatePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    "epicfightdrawladecounter", "perfect_guard_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PerfectGuardStatePacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeByte(packet.active);
                        buf.writeVarInt(packet.remainingTicks);
                    },
                    buf -> new PerfectGuardStatePacket(buf.readByte(), buf.readVarInt())
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 客户端处理：同步本地 Capability 中的完美格挡状态
     *
     * @param packet  收到的数据包
     * @param context 载荷上下文
     */
    public static void handle(PerfectGuardStatePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player == null) return;

            IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);
            if (cap != null) {
                cap.setPerfectGuardActive(packet.active() != 0);
                cap.setPerfectGuardRemainingTicks(packet.remainingTicks());
            }
        });
    }
}

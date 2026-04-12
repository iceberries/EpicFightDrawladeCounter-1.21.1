package com.ice_berry.drawlade_counter.network.packets;

import com.ice_berry.drawlade_counter.client.input.WeaponSwitchHandler;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 格挡命中通知数据包 (S2C)
 * 服务端检测到玩家格挡成功后，通知客户端开启完美格挡判定窗口。
 */
public record GuardHitPacket() implements CustomPacketPayload {

    public static final Type<GuardHitPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    "epicfightdrawladecounter", "guard_hit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GuardHitPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {},
                    buf -> new GuardHitPacket()
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 客户端处理：收到格挡命中通知后，开启完美格挡判定窗口
     *
     * @param packet  收到的数据包
     * @param context 载荷上下文
     */
    public static void handle(GuardHitPacket packet, IPayloadContext context) {
        context.enqueueWork(WeaponSwitchHandler::onGuardHit);
    }
}

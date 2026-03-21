package com.ice_berry.drawlade_counter.network.packets;

import com.ice_berry.drawlade_counter.client.render.ParticleManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 触发粒子特效数据包 (S2C)
 * 服务端执行支援攻击后通知客户端播放粒子效果。
 *
 * <p>协议：{@code [int x, int y, int z, utf8 particleType, float dirX, float dirY, float dirZ]}</p>
 */
public record TriggerEffectPacket(int x, int y, int z, String particleType,
                                  float dirX, float dirY, float dirZ) implements CustomPacketPayload {

    public static final Type<TriggerEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("epicfightdrawladecounter", "trigger_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TriggerEffectPacket> STREAM_CODEC =
            StreamCodec.of(
                    (buf, packet) -> {
                        buf.writeInt(packet.x);
                        buf.writeInt(packet.y);
                        buf.writeInt(packet.z);
                        buf.writeUtf(packet.particleType);
                        buf.writeFloat(packet.dirX);
                        buf.writeFloat(packet.dirY);
                        buf.writeFloat(packet.dirZ);
                    },
                    buf -> new TriggerEffectPacket(
                            buf.readInt(), buf.readInt(), buf.readInt(),
                            buf.readUtf(),
                            buf.readFloat(), buf.readFloat(), buf.readFloat()
                    )
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 客户端处理：在指定位置生成粒子
     */
    public static void handle(TriggerEffectPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ParticleManager.spawnParticles(
                    packet.x, packet.y, packet.z,
                    packet.particleType(),
                    packet.dirX(), packet.dirY(), packet.dirZ()
            );
        });
    }
}

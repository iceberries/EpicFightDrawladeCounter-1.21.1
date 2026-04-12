package com.ice_berry.drawlade_counter.network.packets;

import com.ice_berry.drawlade_counter.combat.ParryCounterHandler;
import com.ice_berry.drawlade_counter.until.epicfight.EFEventsHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;

/**
 * 完美格挡触发数据包 (C2S)
 * 客户端在格挡判定窗口内按下武器流转键时发送到服务端。
 */
public record PerfectGuardTriggerPacket() implements CustomPacketPayload {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final Type<PerfectGuardTriggerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(
                    "epicfightdrawladecounter", "perfect_guard_trigger"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PerfectGuardTriggerPacket> STREAM_CODEC =
            StreamCodec.unit(new PerfectGuardTriggerPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 服务端处理：验证触发条件，设置待定弹反或立即触发格挡反击
     *
     * @param packet  收到的数据包
     * @param context 载荷上下文
     */
    public static void handle(PerfectGuardTriggerPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) return;

            LOGGER.debug("[EFDC] 收到 PerfectGuardTriggerPacket, player={}",
                    serverPlayer.getName().getString());
            LivingEntity attacker = EFEventsHandler.handlePerfectGuardTrigger(serverPlayer);
            if (attacker != null && attacker.isAlive()) {
                LOGGER.debug("[EFDC] 格挡反击模式: 立即触发反击, attacker={}",
                        attacker.getName().getString());
                ParryCounterHandler.triggerPerfectGuard(serverPlayer, attacker, null);
            } else if (attacker == null) {
                LOGGER.debug("[EFDC] 弹反模式: 待定标记已设置，等待怪物攻击到达");
            }
        });
    }
}

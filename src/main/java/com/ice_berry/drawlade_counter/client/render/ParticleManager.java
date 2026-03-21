package com.ice_berry.drawlade_counter.client.render;

import com.ice_berry.drawlade_counter.EFDCMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;

/**
 * 粒子特效管理器
 * 在客户端生成支援攻击的粒子效果。
 *
 * <p>支持的粒子类型：</p>
 * <ul>
 *   <li>{@code crit} — 暴击粒子</li>
 *   <li>{@code sweep} — 横扫粒子</li>
 *   <li>{@code cloud} — 云雾粒子</li>
 *   <li>{@code flame} — 火焰粒子</li>
 *   <li>{@code magic} — 魔法粒子</li>
 *   <li>{@code enchant} — 附魔粒子</li>
 *   <li>{@code totem} — 图腾粒子</li>
 *   <li>{@code sonic_boom} — 音爆粒子</li>
 * </ul>
 */
public final class ParticleManager {

    private ParticleManager() {}

    /**
     * 在指定位置生成粒子效果
     *
     * @param x          X 坐标
     * @param y          Y 坐标
     * @param z          Z 坐标
     * @param particleType 粒子类型标识符
     * @param dirX       方向 X 分量
     * @param dirY       方向 Y 分量
     * @param dirZ       方向 Z 分量
     */
    public static void spawnParticles(int x, int y, int z,
                                       String particleType,
                                       float dirX, float dirY, float dirZ) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;

        SimpleParticleType particle = resolveParticleType(particleType);
        if (particle == null) {
            EFDCMod.LOGGER.warn("Unknown particle type: {}, using crit as fallback", particleType);
            particle = ParticleTypes.CRIT;
        }

        // 在攻击位置周围生成粒子
        double centerX = x + 0.5;
        double centerY = y + 0.5;
        double centerZ = z + 0.5;

        int count = getParticleCount(particleType);

        for (int i = 0; i < count; i++) {
            double offsetX = (Math.random() - 0.5) * 1.5;
            double offsetY = (Math.random() - 0.5) * 1.5;
            double offsetZ = (Math.random() - 0.5) * 1.5;

            double velX = dirX * 0.3 + (Math.random() - 0.5) * 0.2;
            double velY = dirY * 0.3 + Math.random() * 0.2;
            double velZ = dirZ * 0.3 + (Math.random() - 0.5) * 0.2;

            level.addParticle(
                    particle,
                    centerX + offsetX, centerY + offsetY, centerZ + offsetZ,
                    velX, velY, velZ
            );
        }
    }

    /**
     * 解析粒子类型字符串为 SimpleParticleType
     */
    private static SimpleParticleType resolveParticleType(String type) {
        return switch (type) {
            case "crit" -> ParticleTypes.CRIT;
            case "crit_magic", "magic_crit" -> ParticleTypes.ENCHANTED_HIT;
            case "sweep" -> ParticleTypes.SWEEP_ATTACK;
            case "cloud" -> ParticleTypes.CLOUD;
            case "flame" -> ParticleTypes.FLAME;
            case "magic" -> ParticleTypes.ENCHANT;
            case "enchant" -> ParticleTypes.ENCHANT;
            case "totem" -> ParticleTypes.TOTEM_OF_UNDYING;
            case "sonic_boom" -> ParticleTypes.SONIC_BOOM;
            case "heart" -> ParticleTypes.HEART;
            case "damage_indicator" -> ParticleTypes.DAMAGE_INDICATOR;
            default -> null;
        };
    }

    /**
     * 根据粒子类型返回生成数量
     */
    private static int getParticleCount(String type) {
        return switch (type) {
            case "sweep" -> 5;  // 横扫粒子较少
            case "sonic_boom" -> 3;
            default -> 15;      // 默认数量
        };
    }
}

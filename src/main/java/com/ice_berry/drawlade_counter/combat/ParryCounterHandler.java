package com.ice_berry.drawlade_counter.combat;

import com.ice_berry.drawlade_counter.api.events.PerfectGuardEvent;
import com.ice_berry.drawlade_counter.capability.IWeaponFlowCapability;
import com.ice_berry.drawlade_counter.capability.WeaponFlowProvider;
import com.ice_berry.drawlade_counter.config.Config;
import com.ice_berry.drawlade_counter.config.DataDrivenLoader;
import com.ice_berry.drawlade_counter.network.NetworkHandler;
import com.ice_berry.drawlade_counter.network.packets.PerfectGuardStatePacket;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * 完美格挡反击处理器
 * 负责完美格挡的判定、强化攻击管理和反击执行逻辑。
 */
public final class ParryCounterHandler {

    /** 强化攻击的 NBT 标记 key */
    private static final String TAG_PERFECT_GUARD = "efdc_perfect_guard_active";

    /** 强化攻击额外伤害倍率 NBT key */
    private static final String TAG_GUARD_BONUS_MULT = "efdc_guard_bonus_mult";

    /** 强化攻击持续 tick（3秒） */
    private static final int ENHANCED_ATTACK_DURATION = 60;

    private ParryCounterHandler() {}

    // #region 完美格挡触发

    /**
     * 触发完美格挡反击
     * 赋予玩家强化攻击并执行反击效果。
     *
     * @param player      执行完美格挡的玩家
     * @param attacker    发起攻击的实体
     * @param counterData 完美格挡反击数据（null 则自动匹配）
     */
    public static void triggerPerfectGuard(Player player, LivingEntity attacker,
                                           PerfectGuardCounterData counterData) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        // 尝试匹配数据包反击数据
        PerfectGuardCounterData data = counterData != null ? counterData : resolveCounterData(player.getMainHandItem());

        // 投递事件，允许取消或修改伤害
        if (!PerfectGuardEvent.fire(player, attacker, data.getDamageMultiplier(), 0)) return;

        // 赋予强化攻击并执行反击
        grantEnhancedAttack(player, data.getDamageMultiplier());
        executeCounterAttack(player, attacker, data, serverLevel);
    }

    // #endregion

    // #region 强化攻击管理

    /**
     * 赋予玩家一次强化普通攻击
     *
     * @param player    玩家
     * @param bonusMult 额外伤害倍率
     */
    private static void grantEnhancedAttack(Player player, float bonusMult) {
        player.getPersistentData().putFloat(TAG_PERFECT_GUARD, 1.0F);
        player.getPersistentData().putFloat(TAG_GUARD_BONUS_MULT, bonusMult);

        // 同步 Capability 状态
        IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);
        if (cap != null) {
            cap.setPerfectGuardActive(true);
            cap.setPerfectGuardRemainingTicks(ENHANCED_ATTACK_DURATION);
        }

        // 通知客户端
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToClient(serverPlayer,
                    new PerfectGuardStatePacket((byte) 1, ENHANCED_ATTACK_DURATION));
        }
    }

    /**
     * 检查玩家是否持有强化攻击标记
     *
     * @param player 玩家
     * @return true 表示有可用的强化攻击
     */
    public static boolean hasEnhancedAttack(Player player) {
        return player.getPersistentData().contains(TAG_PERFECT_GUARD)
                && player.getPersistentData().getFloat(TAG_PERFECT_GUARD) > 0;
    }

    /**
     * 消耗玩家的强化攻击标记，并获取额外伤害倍率
     *
     * @param player 玩家
     * @return 额外伤害倍率，无标记时返回 0
     */
    public static float consumeEnhancedAttack(Player player) {
        float bonus = player.getPersistentData().getFloat(TAG_GUARD_BONUS_MULT);
        player.getPersistentData().remove(TAG_PERFECT_GUARD);
        player.getPersistentData().remove(TAG_GUARD_BONUS_MULT);

        // 同步清除 Capability
        IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);
        if (cap != null) {
            cap.setPerfectGuardActive(false);
            cap.setPerfectGuardRemainingTicks(0);
        }

        // 通知客户端清除
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToClient(serverPlayer, new PerfectGuardStatePacket((byte) 0, 0));
        }
        return bonus;
    }

    // #endregion

    // #region 反击执行

    /**
     * 执行反击攻击逻辑
     *
     * @param player      玩家
     * @param attacker    原攻击者（优先目标）
     * @param data        反击数据
     * @param serverLevel 服务端世界
     */
    private static void executeCounterAttack(Player player, LivingEntity attacker,
                                             PerfectGuardCounterData data, ServerLevel serverLevel) {
        List<LivingEntity> targets = findCounterTargets(player, data.getRange());
        if (targets.isEmpty()) return;

        // 计算反击伤害
        float baseDamage = player.getAttackStrengthScale(0.5F);
        float weaponDamage = player.getMainHandItem().isEmpty() ? 1.0F
                : (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float totalDamage = (baseDamage + weaponDamage) * data.getDamageMultiplier();

        // 对目标造成伤害和效果
        for (LivingEntity target : targets) {
            if (target == player || !target.isAlive()) continue;

            target.hurt(serverLevel.damageSources().playerAttack(player), totalDamage);

            Vec3 knockbackDir = target.position().subtract(player.position())
                    .normalize().add(0, 0.2, 0).normalize()
                    .scale(data.getKnockback());
            target.knockback(data.getKnockback() * 2.0F, -knockbackDir.x, -knockbackDir.z);
            target.invulnerableTime = 0;

            applyExtraEffects(target, data);
        }

        playCounterSound(player, data.getSound());
    }

    /**
     * 施加额外的 MobEffect
     *
     * @param target 目标实体
     * @param data   反击数据
     */
    private static void applyExtraEffects(LivingEntity target, PerfectGuardCounterData data) {
        List<String> effects = data.getApplyEffects();
        if (effects.isEmpty()) return;

        for (String effectId : effects) {
            try {
                ResourceLocation effectLoc = ResourceLocation.parse(effectId);
                var holder = BuiltInRegistries.MOB_EFFECT.getHolder(effectLoc);
                if (holder.isPresent()) {
                    target.addEffect(new MobEffectInstance(
                            holder.get(), data.getEffectDuration(), data.getEffectAmplifier()));
                }
            } catch (Exception e) {
                // 效果施加失败，静默忽略
            }
        }
    }

    // #endregion

    // #region 目标搜索

    /**
     * 搜索反击范围内的目标（180 度扇形）
     *
     * @param player 玩家
     * @param range  搜索范围（格）
     * @return 目标实体列表
     */
    private static List<LivingEntity> findCounterTargets(Player player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive() && entity.isAttackable());

        List<LivingEntity> targets = new ArrayList<>();
        Vec3 lookVec = player.getLookAngle();
        double maxAngle = Math.toRadians(90);

        for (LivingEntity entity : entities) {
            Vec3 toTarget = entity.position().add(0, entity.getEyeHeight() / 2, 0)
                    .subtract(player.position().add(0, player.getEyeHeight(), 0));
            toTarget = toTarget.normalize();
            double angle = Math.acos(lookVec.dot(toTarget));
            if (angle <= maxAngle) {
                targets.add(entity);
            }
        }
        return targets;
    }

    // #endregion

    // #region 音效播放

    /**
     * 播放反击音效，失败时回退到默认暴击音效
     */
    private static void playCounterSound(Player player, String soundId) {
        try {
            ResourceLocation soundLoc = ResourceLocation.parse(soundId);
            var holder = BuiltInRegistries.SOUND_EVENT.getHolder(soundLoc);
            if (holder.isPresent()) {
                player.level().playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        holder.get().value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                playFallbackCritSound(player);
            }
        } catch (Exception e) {
            playFallbackCritSound(player);
        }
    }

    /**
     * 播放回退暴击音效
     */
    private static void playFallbackCritSound(Player player) {
        player.level().playSound(null,
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    // #endregion

    // #region 数据包匹配

    /**
     * 根据武器匹配反击数据，无匹配时使用默认配置
     */
    private static PerfectGuardCounterData resolveCounterData(ItemStack weapon) {
        PerfectGuardCounterData data = DataDrivenLoader.findParryCounter(weapon);
        if (data != null) return data;
        return createDefaultCounterData();
    }

    /**
     * 创建默认反击数据（使用配置默认值）
     */
    private static PerfectGuardCounterData createDefaultCounterData() {
        return new PerfectGuardCounterData(
                ResourceLocation.parse("epicfightdrawladecounter:default_parry_counter"),
                null,
                Config.PERFECT_GUARD_DAMAGE_MULTIPLIER.get().floatValue(),
                3.0, 0.5F,
                "minecraft:entity.player.attack.crit", "crit",
                List.of(), 200, 0,
                null, 10);
    }

    // #endregion
}

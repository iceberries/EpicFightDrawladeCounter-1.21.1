package com.ice_berry.drawlade_counter.api.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 完美格挡反击事件
 * 当完美格挡判定成功、准备触发反击时派发。
 * 可取消 —— 取消后本次反击不执行。
 * 可通过 setDamageMultiplier 修改反击伤害倍率。
 */
public class PerfectGuardEvent extends CounterAttackEvent {

    /** 完美格挡判定的时间差（毫秒），用于第三方判断格挡精度 */
    private final long timingDiffMs;

    /**
     * 构造完美格挡反击事件
     *
     * @param player          执行完美格挡的玩家
     * @param attacker        发起攻击的实体
     * @param damageMultiplier 反击伤害倍率
     * @param timingDiffMs    格挡时间差（毫秒），正值表示格挡略早，负值表示略晚
     */
    public PerfectGuardEvent(Player player, LivingEntity attacker,
                             float damageMultiplier, long timingDiffMs) {
        super(player, attacker, damageMultiplier);
        this.timingDiffMs = timingDiffMs;
    }

    /**
     * 获取格挡时间差（毫秒）
     * 正值表示玩家格挡略早于攻击命中，负值表示略晚。
     * 绝对值越小表示格挡时机越精确。
     *
     * @return 时间差（毫秒）
     */
    public long getTimingDiffMs() {
        return timingDiffMs;
    }

    /**
     * 投递完美格挡反击事件
     *
     * @param player          执行完美格挡的玩家
     * @param attacker        发起攻击的实体
     * @param damageMultiplier 反击伤害倍率
     * @param timingDiffMs    格挡时间差（毫秒）
     * @return 事件是否未被取消
     */
    public static boolean fire(Player player, LivingEntity attacker,
                               float damageMultiplier, long timingDiffMs) {
        PerfectGuardEvent event = new PerfectGuardEvent(
                player, attacker, damageMultiplier, timingDiffMs);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCancelled();
    }
}

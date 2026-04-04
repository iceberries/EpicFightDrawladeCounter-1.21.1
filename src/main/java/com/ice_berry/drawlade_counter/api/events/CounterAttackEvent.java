package com.ice_berry.drawlade_counter.api.events;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 反击事件基类（闪避反击 / 格挡反击 的父类）
 */
public abstract class CounterAttackEvent extends PlayerEvent {

    private final LivingEntity attacker;
    private float damageMultiplier;
    private boolean cancelled;

    protected CounterAttackEvent(Player player, LivingEntity attacker, float damageMultiplier) {
        super(player);
        this.attacker = attacker;
        this.damageMultiplier = damageMultiplier;
    }

    /** 发动反击的攻击者 */
    public LivingEntity getAttacker() { return attacker; }

    public float getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(float damageMultiplier) { this.damageMultiplier = damageMultiplier; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }
}

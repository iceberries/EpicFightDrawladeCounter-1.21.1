package com.ice_berry.drawlade_counter.api.events;

import com.ice_berry.drawlade_counter.combat.SupportAttackData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

/**
 * 支援攻击事件
 * 当武器切换触发支援攻击时派发。
 * 可取消 —— 取消后本次支援攻击不执行。
 * 可通过 setDamageMultiplier 修改伤害倍率。
 */
public class SupportAttackEvent extends PlayerEvent {

    private final SupportAttackData attackData;
    private final ItemStack fromWeapon;
    private final ItemStack toWeapon;
    private final List<LivingEntity> targets;
    private float damageMultiplier;
    private boolean cancelled;

    public SupportAttackEvent(Player player, SupportAttackData attackData,
                              ItemStack fromWeapon, ItemStack toWeapon,
                              List<LivingEntity> targets, float damageMultiplier) {
        super(player);
        this.attackData = attackData;
        this.fromWeapon = fromWeapon;
        this.toWeapon = toWeapon;
        this.targets = targets;
        this.damageMultiplier = damageMultiplier;
    }

    /** 当前生效的支援攻击数据定义 */
    public SupportAttackData getAttackData() { return attackData; }

    /** 切换前的武器 */
    public ItemStack getFromWeapon() { return fromWeapon; }

    /** 切换后的武器 */
    public ItemStack getToWeapon() { return toWeapon; }

    /** 本次支援攻击命中的目标列表（只读） */
    public List<LivingEntity> getTargets() { return targets; }

    /** 获取/设置伤害倍率 */
    public float getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(float damageMultiplier) { this.damageMultiplier = damageMultiplier; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    /**
     * 投递支援攻击事件
     *
     * @return 事件是否未被取消
     */
    public static boolean fire(Player player, SupportAttackData attackData,
                               ItemStack fromWeapon, ItemStack toWeapon,
                               List<LivingEntity> targets, float damageMultiplier) {
        SupportAttackEvent event = new SupportAttackEvent(
                player, attackData, fromWeapon, toWeapon, targets, damageMultiplier);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCancelled();
    }
}

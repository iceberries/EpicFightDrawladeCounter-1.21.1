package com.ice_berry.drawlade_counter.api.events;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * 武器切换事件
 * 当玩家在武器流转系统中切换武器时触发。
 * 可取消 —— 取消后不执行切换逻辑和支援攻击判定。
 */
public class WeaponSwitchEvent extends PlayerEvent {

    private final int fromSlot;
    private final int toSlot;
    private final ItemStack fromWeapon;
    private final ItemStack toWeapon;
    private boolean cancelled;

    public WeaponSwitchEvent(Player player, int fromSlot, int toSlot,
                             ItemStack fromWeapon, ItemStack toWeapon) {
        super(player);
        this.fromSlot = fromSlot;
        this.toSlot = toSlot;
        this.fromWeapon = fromWeapon;
        this.toWeapon = toWeapon;
    }

    /** 切换前的流转槽位索引 */
    public int getFromSlot() { return fromSlot; }

    /** 切换后的流转槽位索引 */
    public int getToSlot() { return toSlot; }

    /** 切换前手持武器（可能为空） */
    public ItemStack getFromWeapon() { return fromWeapon; }

    /** 切换后手持武器（可能为空） */
    public ItemStack getToWeapon() { return toWeapon; }

    public boolean isCancelled() { return cancelled; }
    public void setCancelled(boolean cancelled) { this.cancelled = cancelled; }

    /**
     * 向事件总线投递武器切换事件
     *
     * @return true 表示事件未被取消
     */
    public static boolean fire(Player player, int fromSlot, int toSlot,
                               ItemStack fromWeapon, ItemStack toWeapon) {
        WeaponSwitchEvent event = new WeaponSwitchEvent(player, fromSlot, toSlot, fromWeapon, toWeapon);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCancelled();
    }
}

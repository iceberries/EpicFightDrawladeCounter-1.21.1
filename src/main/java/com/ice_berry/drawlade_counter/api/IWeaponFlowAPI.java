package com.ice_berry.drawlade_counter.api;

import com.ice_berry.drawlade_counter.capability.IWeaponFlowCapability;
import com.ice_berry.drawlade_counter.combat.PerfectGuardCounterData;
import com.ice_berry.drawlade_counter.combat.ParryCounterHandler;
import com.ice_berry.drawlade_counter.combat.SupportAttackData;
import com.ice_berry.drawlade_counter.config.DataDrivenLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Collection;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * 武器流转系统的公开 API
 * 供其他模组查询武器流转状态和支援攻击配置。
 */
public final class IWeaponFlowAPI {

    private IWeaponFlowAPI() {}

    /**
     * 获取玩家的武器流转 Capability
     *
     * @return Capability 实例，若玩家无该能力则返回 null
     */
    @Nullable
    public static IWeaponFlowCapability getWeaponFlow(Player player) {
        return com.ice_berry.drawlade_counter.capability.WeaponFlowProvider.getCapability(player);
    }

    /**
     * 根据武器对查找匹配的支援攻击数据
     *
     * @param from 原武器 ItemStack
     * @param to   切换后的武器 ItemStack
     * @return 匹配的支援攻击数据，无匹配返回 null
     */
    @Nullable
    public static SupportAttackData findSupportAttack(ItemStack from, ItemStack to) {
        return DataDrivenLoader.findSupportAttack(from, to);
    }

    /**
     * 根据武器 Item 查找支援攻击数据
     *
     * @param fromItem 原武器 Item
     * @param toItem   切换后的武器 Item
     * @return 匹配的支援攻击数据，无匹配返回 null
     */
    @Nullable
    public static SupportAttackData findSupportAttack(Item fromItem, Item toItem) {
        return DataDrivenLoader.findSupportAttack(fromItem, toItem);
    }

    /**
     * 获取所有已加载的支援攻击数据（只读快照）
     */
    public static Collection<SupportAttackData> getAllSupportAttacks() {
        return DataDrivenLoader.getAllSupportAttacks();
    }

    /**
     * 通过 ResourceLocation 查找指定支援攻击数据
     *
     * @param id 数据 ID（如 epicfightdrawladecounter:sword_to_axe）
     * @return 对应数据，不存在返回 null
     */
    @Nullable
    public static SupportAttackData getSupportAttack(ResourceLocation id) {
        return DataDrivenLoader.getSupportAttack(id);
    }

    // #region 完美格挡 API

    /**
     * 检查玩家是否持有强化攻击状态（完美格挡触发后）
     *
     * @param player 玩家
     * @return true 表示强化攻击有效
     */
    public static boolean hasEnhancedAttack(Player player) {
        return ParryCounterHandler.hasEnhancedAttack(player);
    }

    /**
     * 消耗玩家的强化攻击标记并获取额外伤害倍率
     * 消耗后标记被移除。
     *
     * @param player 玩家
     * @return 额外伤害倍率，无标记时返回 0
     */
    public static float consumeEnhancedAttack(Player player) {
        return ParryCounterHandler.consumeEnhancedAttack(player);
    }

    /**
     * 手动触发完美格挡反击（供第三方模组调用）
     *
     * @param player      执行完美格挡的玩家
     * @param attacker    攻击者
     * @param counterData 反击数据（null 则自动匹配）
     */
    public static void triggerPerfectGuard(Player player,
                                           net.minecraft.world.entity.LivingEntity attacker,
                                           @Nullable PerfectGuardCounterData counterData) {
        ParryCounterHandler.triggerPerfectGuard(player, attacker, counterData);
    }

    /**
     * 通过 ID 查找完美格挡反击数据
     *
     * @param id 数据 ID
     * @return 对应数据，不存在返回 null
     */
    @Nullable
    public static PerfectGuardCounterData getParryCounter(ResourceLocation id) {
        return DataDrivenLoader.getParryCounter(id);
    }

    /**
     * 根据武器查找匹配的完美格挡反击数据
     *
     * @param weapon 武器 ItemStack
     * @return 匹配数据，无匹配返回 null
     */
    @Nullable
    public static PerfectGuardCounterData findParryCounter(ItemStack weapon) {
        return DataDrivenLoader.findParryCounter(weapon);
    }

    /**
     * 获取所有已加载的完美格挡反击数据（只读快照）
     *
     * @return 数据集合
     */
    public static Collection<PerfectGuardCounterData> getAllParryCounters() {
        return DataDrivenLoader.getAllParryCounters();
    }
}

package com.ice_berry.drawlade_counter.api;

import com.ice_berry.drawlade_counter.capability.IWeaponFlowCapability;
import com.ice_berry.drawlade_counter.combat.SupportAttackData;
import com.ice_berry.drawlade_counter.config.DataDrivenLoader;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
}

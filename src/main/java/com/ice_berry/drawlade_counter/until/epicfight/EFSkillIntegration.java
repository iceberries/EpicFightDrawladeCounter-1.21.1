package com.ice_berry.drawlade_counter.until.epicfight;

import com.ice_berry.drawlade_counter.EFDCMod;
import net.neoforged.fml.ModList;
import net.minecraft.world.item.ItemStack;

/**
 * Epic Fight 技能系统集成
 * 提供与 EF 技能系统对接的工具方法。
 */
public final class EFSkillIntegration {

    private EFSkillIntegration() {}

    /**
     * 检查玩家当前是否被 Epic Fight 动画锁定（不允许切换物品）
     * 通过 LivingEntityPatch.getEntityState().canSwitchHoldingItem() 判断，
     * 攻击动画、眩晕、击倒等状态下返回 false。
     *
     * @param player 要检查的玩家
     * @return true 表示被锁定，不允许切换武器；EF 未加载时返回 false
     */
    public static boolean isPlayerInAttackAnimation(Object player) {
        if (!ModList.get().isLoaded("epicfight")) return false;

        try {
            Class<?> capsClass = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> patchClass = Class.forName("yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");

            // EF 21.12.5: getEntityPatch(Entity, Class) — 参数是 Entity 不是 LivingEntity
            var getPatch = capsClass.getMethod("getEntityPatch",
                    net.minecraft.world.entity.Entity.class, Class.class);
            Object patch = getPatch.invoke(null, player, patchClass);

            if (patch == null) return false;

            // 调用 LivingEntityPatch.getEntityState().canSwitchHoldingItem()
            var getEntityState = patchClass.getMethod("getEntityState");
            Object entityState = getEntityState.invoke(patch);

            var canSwitch = entityState.getClass().getMethod("canSwitchHoldingItem");
            boolean result = (boolean) canSwitch.invoke(entityState);

            // canSwitchHoldingItem() 返回 false 表示被动画锁定
            return !result;

        } catch (ClassNotFoundException e) {
            EFDCMod.LOGGER.warn("Epic Fight classes not found, animation lock check disabled");
            return false;
        } catch (NoSuchMethodException e) {
            EFDCMod.LOGGER.warn("Epic Fight API method signature mismatch: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            EFDCMod.LOGGER.warn("Failed to check EF animation state", e);
            return false;
        }
    }

    /**
     * 获取 EF 武器的攻击伤害值
     * 如果 EF 未加载或获取失败，返回 0（调用者应回退到原版值）。
     *
     * @param weaponStack 武器 ItemStack
     * @return EF 计算的攻击伤害值，获取失败返回 0
     */
    public static float getEFWeaponDamage(Object weaponStack) {
        if (!ModList.get().isLoaded("epicfight")) return 0F;

        try {
            EFDCMod.LOGGER.debug("getEFWeaponDamage: EF 21.12.5 does not expose direct damage method, returning 0");
            return 0F;
        } catch (Exception e) {
            return 0F;
        }
    }
}

package com.ice_berry.drawlade_counter.until.epicfight;

import com.ice_berry.drawlade_counter.EFDCMod;
import net.neoforged.fml.ModList;

/**
 * Epic Fight 技能系统集成
 * 提供与 EF 技能系统对接的工具方法。
 *
 * <p>阶段二功能：</p>
 * <ul>
 *   <li>查询玩家是否处于 EF 战斗动画中（用于判断是否允许切换武器）</li>
 *   <li>获取 EF 武器属性（用于伤害计算）</li>
 * </ul>
 *
 * <p>所有方法通过反射安全调用，EF 未加载时返回安全的默认值。</p>
 */
public final class EFSkillIntegration {

    private EFSkillIntegration() {}

    /**
     * 检查玩家当前是否正在执行 Epic Fight 的攻击动画
     * 如果玩家正在攻击动画中，可能需要延迟武器切换。
     *
     * @param player 要检查的玩家
     * @return true 表示正在攻击动画中；EF 未加载时返回 false
     */
    public static boolean isPlayerInAttackAnimation(Object player) {
        if (!ModList.get().isLoaded("epicfight")) return false;

        try {
            Class<?> capsClass = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> patchClass = Class.forName("yesman.epicfight.world.entity.LivingEntityPatch");

            var getPatch = capsClass.getMethod("getEntityPatch",
                    net.minecraft.world.entity.LivingEntity.class, Class.class);
            Object patch = getPatch.invoke(null, player, patchClass);

            if (patch == null) return false;

            // 检查 entityPatch.getAnimator().isPlaying()
            Class<?> animatorClass = Class.forName("yesman.epicfight.api.animation.Animator");
            var getAnimator = patchClass.getMethod("getAnimator");
            Object animator = getAnimator.invoke(patch);

            var isPlaying = animatorClass.getMethod("isPlaying");
            return (boolean) isPlaying.invoke(animator);

        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            EFDCMod.LOGGER.debug("Failed to check EF animation state", e);
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
            Class<?> itemCapClass = Class.forName("yesman.epicfight.gameasset.Weapons");
            Class<?> capabilityClass = Class.forName("yesman.epicfight.api.data.rewriter.ItemCapability");

            var getWeaponCap = itemCapClass.getMethod("getCapability", net.minecraft.world.item.ItemStack.class);
            Object cap = getWeaponCap.invoke(null, weaponStack);

            if (cap == null) return 0F;

            var getDamage = capabilityClass.getMethod("getDamageValue", net.minecraft.world.item.ItemStack.class);
            return (float) getDamage.invoke(cap, weaponStack);

        } catch (ClassNotFoundException e) {
            return 0F;
        } catch (Exception e) {
            EFDCMod.LOGGER.debug("Failed to get EF weapon damage", e);
            return 0F;
        }
    }
}

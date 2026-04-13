package com.ice_berry.drawlade_counter.combat;

import com.ice_berry.drawlade_counter.api.events.SupportAttackEvent;
import com.ice_berry.drawlade_counter.network.NetworkHandler;
import com.ice_berry.drawlade_counter.network.packets.CooldownUpdatePacket;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 支援攻击处理器
 * 负责执行支援攻击的全部逻辑，EF 为软依赖。
 */
public final class SupportAttackHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 检测 Epic Fight 是否可用
     */
    public static boolean isEpicFightLoaded() {
        return ModList.get().isLoaded("epicfight");
    }

    /**
     * 初始化时检测 EF 并记录日志
     */
    public static void init() {
        // EF 加载状态在运行时实时检测，此处仅做启动日志
    }

    /**
     * 执行支援攻击
     *
     * @param player      执行者
     * @param attackData  支援攻击数据
     * @param fromWeapon  原武器
     * @param toWeapon    切换后的武器
     * @param flowSlot    当前流转槽位（用于设置冷却）
     */
    public static void executeSupportAttack(Player player, SupportAttackData attackData,
                                            ItemStack fromWeapon, ItemStack toWeapon, int flowSlot) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        boolean useEFAttack = isEpicFightLoaded() && attackData.hasEfAnimation();

        // 投递事件（允许第三方取消或修改）
        boolean notCancelled = SupportAttackEvent.fire(
                player, attackData, fromWeapon, toWeapon, List.of(), attackData.getDamageMultiplier());
        if (!notCancelled) return;

        if (useEFAttack) {
            boolean efPlayed = playEpicFightAnimation(player, attackData, toWeapon);
            if (!efPlayed) {
                // EF 动画播放失败，回退到原版伤害模式
                executeVanillaAttack(player, attackData, toWeapon, serverLevel);
            }
        } else {
            // 原版模式：搜索目标并造成伤害
            executeVanillaAttack(player, attackData, toWeapon, serverLevel);
        }

        // 公共逻辑：音效和冷却
        playSound(player, attackData.getSound());

        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToClient(serverPlayer, new CooldownUpdatePacket(flowSlot, attackData.getCooldownTicks()));
        }
    }

    /**
     * 执行原版模式的伤害逻辑（搜索目标 + 造成伤害）
     *
     * @param player      执行者
     * @param attackData  支援攻击数据
     * @param toWeapon    当前手持武器
     * @param serverLevel 服务端世界
     */
    private static void executeVanillaAttack(Player player, SupportAttackData attackData,
                                             ItemStack toWeapon, ServerLevel serverLevel) {
        List<LivingEntity> targets = findTargets(player, attackData.getRange());
        if (targets.isEmpty()) return;

        float baseDamage = player.getAttackStrengthScale(0.5F);
        float weaponDamage = toWeapon.isEmpty() ? 1.0F
                : (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float totalDamage = (baseDamage + weaponDamage) * attackData.getDamageMultiplier();

        for (LivingEntity target : targets) {
            if (target == player || !target.isAlive()) continue;

            Vec3 knockbackDir = target.position().subtract(player.position())
                    .normalize().add(0, 0.2, 0).normalize()
                    .scale(attackData.getKnockback());

            target.hurt(serverLevel.damageSources().playerAttack(player), totalDamage);
            target.knockback(
                    attackData.getKnockback() * 2.0F,
                    -knockbackDir.x, -knockbackDir.z);
            target.invulnerableTime = 0;
        }
    }

    /**
     * 搜索范围内的有效目标（180 度扇形）
     */
    private static List<LivingEntity> findTargets(Player player, double range) {
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

    /**
     * 播放攻击音效，失败时回退到默认横扫音效
     */
    private static void playSound(Player player, String soundId) {
        try {
            ResourceLocation soundLoc = ResourceLocation.parse(soundId);
            var holder = BuiltInRegistries.SOUND_EVENT.getHolder(soundLoc);
            if (holder.isPresent()) {
                player.level().playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        holder.get().value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            } else {
                player.level().playSound(null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        } catch (Exception e) {
            player.level().playSound(null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    /**
     * 通过反射播放 EF 动画
     *
     * @param player      执行者
     * @param attackData  支援攻击数据
     * @param toWeapon    当前手持武器
     * @return true 表示动画播放成功，false 表示播放失败（调用方应回退到原版伤害）
     */
    private static boolean playEpicFightAnimation(Player player, SupportAttackData attackData, ItemStack toWeapon) {
        try {
            Class<?> epicFightCaps = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> playerPatchClass = Class.forName("yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch");
            Class<?> livingEntityPatchClass = Class.forName("yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            Class<?> assetAccessorClass = Class.forName("yesman.epicfight.api.asset.AssetAccessor");

            // 获取玩家的 PlayerPatch 实例
            var getEntityPatchMethod = epicFightCaps.getMethod("getEntityPatch",
                    net.minecraft.world.entity.Entity.class, Class.class);
            Object playerPatch = getEntityPatchMethod.invoke(null, player, playerPatchClass);
            if (playerPatch == null) {
                LOGGER.warn("[EFDC] playEpicFightAnimation: PlayerPatch 为 null, 回退到原版伤害");
                return false;
            }

            // 确定要播放的动画
            Object animationAccessor;
            if (attackData.isAutoAnimation()) {
                // AUTO 模式：从 toWeapon 的 EF 武器能力获取攻击动画
                String weaponName = toWeapon.isEmpty() ? "EMPTY" : toWeapon.getItem().toString();
                LOGGER.debug("[EFDC] playEpicFightAnimation: AUTO模式, toWeapon={}, " +
                                "开始解析自动攻击动画", weaponName);
                animationAccessor = resolveAutoAnimation(toWeapon, playerPatch);
                if (animationAccessor == null) {
                    LOGGER.warn("[EFDC] playEpicFightAnimation: resolveAutoAnimation 返回 null " +
                            "(weapon={}), 回退到原版伤害", weaponName);
                    return false;
                }
                LOGGER.debug("[EFDC] playEpicFightAnimation: 解析到动画 accessor={}",
                        animationAccessor);
            } else {
                // 手动模式：使用 JSON 中配置的固定动画
                ResourceLocation animLoc = attackData.getEfAnimation();
                Class<?> animManagerClass = Class.forName("yesman.epicfight.api.animation.AnimationManager");
                var byKeyMethod = animManagerClass.getMethod("byKey", ResourceLocation.class);
                animationAccessor = byKeyMethod.invoke(null, animLoc);
                LOGGER.debug("[EFDC] playEpicFightAnimation: 手动模式, animLoc={}, accessor={}",
                        animLoc, animationAccessor);
            }

            if (animationAccessor == null) {
                LOGGER.warn("[EFDC] playEpicFightAnimation: animationAccessor 为 null, 回退到原版伤害");
                return false;
            }

            // 检查动画是否已注册并播放
            var isPresentMethod = animationAccessor.getClass().getMethod("isPresent");
            boolean present = (boolean) isPresentMethod.invoke(animationAccessor);
            if (!present) {
                LOGGER.warn("[EFDC] playEpicFightAnimation: 动画未注册 (isPresent=false), " +
                        "accessor={}, 回退到原版伤害", animationAccessor);
                return false;
            }
            var playMethod = livingEntityPatchClass.getMethod("playAnimationInstantly", assetAccessorClass);
            playMethod.invoke(playerPatch, animationAccessor);
            LOGGER.debug("[EFDC] playEpicFightAnimation: 动画播放成功, accessor={}",
                    animationAccessor);
            return true;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            LOGGER.warn("[EFDC] playEpicFightAnimation: EF 类/方法未找到, 回退到原版伤害", e);
            return false;
        } catch (Exception e) {
            LOGGER.warn("[EFDC] playEpicFightAnimation: 动画播放异常, 回退到原版伤害", e);
            return false;
        }
    }

    /**
     * AUTO 模式：从武器 EF 能力获取攻击动画
     *
     * @param weaponStack 目标武器
     * @param playerPatch 玩家的 PlayerPatch
     * @return 攻击动画的 AssetAccessor，失败返回 null
     */
    private static Object resolveAutoAnimation(ItemStack weaponStack, Object playerPatch) {
        try {
            if (weaponStack.isEmpty()) return null;

            Class<?> epicFightCaps = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> playerPatchClass = Class.forName("yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch");
            var getItemCapMethod = epicFightCaps.getMethod("getItemStackCapability", net.minecraft.world.item.ItemStack.class);
            Object capItem = getItemCapMethod.invoke(null, weaponStack);
            if (capItem == null) return null;

            var getAutoAttackMotionMethod = capItem.getClass().getMethod("getAutoAttackMotion", playerPatchClass);
            Object animList = getAutoAttackMotionMethod.invoke(capItem, playerPatch);

            if (animList instanceof java.util.List<?> list && !list.isEmpty()) {
                return list.get(0);
            }
            return null;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

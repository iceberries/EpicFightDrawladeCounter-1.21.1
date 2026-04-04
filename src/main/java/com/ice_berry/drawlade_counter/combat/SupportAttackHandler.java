package com.ice_berry.drawlade_counter.combat;

import com.ice_berry.drawlade_counter.EFDCMod;
import com.ice_berry.drawlade_counter.api.events.SupportAttackEvent;
import com.ice_berry.drawlade_counter.network.NetworkHandler;
import com.ice_berry.drawlade_counter.network.packets.CooldownUpdatePacket;
import com.ice_berry.drawlade_counter.network.packets.TriggerEffectPacket;
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

import java.util.ArrayList;
import java.util.List;

/**
 * 支援攻击处理器
 * 负责执行支援攻击的全部逻辑：目标搜索、伤害计算、
 * 击退、音效/粒子播放、以及 Epic Fight 动画集成。
 *
 * <p>设计原则：
 * <ul>
 *   <li>Epic Fight 为软依赖 —— 若已加载则使用 EF 动画系统，否则回退到原版</li>
 *   <li>所有伤害和效果在服务端执行，客户端仅负责视觉表现</li>
 * </ul>
 */
public final class SupportAttackHandler {

    /**
     * 检测 Epic Fight 是否可用（运行时实时检测，不缓存）
     */
    public static boolean isEpicFightLoaded() {
        return ModList.get().isLoaded("epicfight");
    }

    // 保留 init() 用于启动时日志，但不再缓存结果
    public static void init() {
        if (ModList.get().isLoaded("epicfight")) {
            EFDCMod.LOGGER.info("Epic Fight detected — support attack will use EF animation system");
        } else {
            EFDCMod.LOGGER.info("Epic Fight not detected — support attack will use vanilla combat fallback");
        }
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

        boolean efLoaded = isEpicFightLoaded();
        boolean useEFAttack = efLoaded && attackData.hasEfAnimation();

        // 1. 投递事件（允许第三方取消或修改）
        //    EF 模式下 targets 为空列表（命中由动画 Collider 决定）
        //    原版模式下 targets 延迟搜索（事件取消后无需搜索）
        boolean notCancelled = SupportAttackEvent.fire(
                player, attackData, fromWeapon, toWeapon, List.of(), attackData.getDamageMultiplier());
        if (!notCancelled) return;

        Vec3 playerLook = player.getLookAngle();

        if (useEFAttack) {
            playEpicFightAnimation(player, attackData, toWeapon);

            EFDCMod.LOGGER.info("EF support attack triggered for player {} (weapon: {})",
                    player.getName().getString(),
                    toWeapon.isEmpty() ? "empty" : toWeapon.getItem());
        } else {
            // ========== 原版模式 ==========
            List<LivingEntity> targets = findTargets(player, attackData.getRange());
            if (targets.isEmpty()) {
                EFDCMod.LOGGER.debug("Support attack triggered but no targets in range for player {}", player.getName().getString());
                return;
            }

            // 计算伤害
            float baseDamage = player.getAttackStrengthScale(0.5F);
            float weaponDamage = toWeapon.isEmpty() ? 1.0F
                    : (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float totalDamage = (baseDamage + weaponDamage) * attackData.getDamageMultiplier();

            // 对目标造成伤害
            for (LivingEntity target : targets) {
                if (target == player) continue;
                if (!target.isAlive()) continue;

                Vec3 knockbackDir = target.position().subtract(player.position())
                        .normalize().add(0, 0.2, 0).normalize()
                        .scale(attackData.getKnockback());

                target.hurt(serverLevel.damageSources().playerAttack(player), totalDamage);
                target.knockback(
                        attackData.getKnockback() * 2.0F,
                        -knockbackDir.x,
                        -knockbackDir.z
                );
                target.invulnerableTime = 0;
            }

            EFDCMod.LOGGER.debug("Support attack executed by {} on {} targets, damage={}",
                    player.getName().getString(), targets.size(), totalDamage);
        }

        // ========== 公共逻辑：音效、冷却、粒子 ==========
        playSound(player, attackData.getSound());

        if (player instanceof ServerPlayer serverPlayer) {
            CooldownUpdatePacket packet = new CooldownUpdatePacket(flowSlot, attackData.getCooldownTicks());
            NetworkHandler.sendToClient(serverPlayer, packet);

            TriggerEffectPacket effectPacket = new TriggerEffectPacket(
                    (int) player.getX(), (int) player.getY(), (int) player.getZ(),
                    attackData.getParticle(), (float)playerLook.x, (float)playerLook.y, (float)playerLook.z
            );
            NetworkHandler.sendToClient(serverPlayer, effectPacket);
        }
    }

    /**
     * 搜索范围内的有效目标
     */
    private static List<LivingEntity> findTargets(Player player, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, box,
                entity -> entity != player && entity.isAlive() && entity.isAttackable());

        List<LivingEntity> targets = new ArrayList<>();
        Vec3 lookVec = player.getLookAngle();
        double maxAngle = Math.toRadians(90); // 180 度扇形

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
     * 播放攻击音效
     */
    private static void playSound(Player player, String soundId) {
        try {
            ResourceLocation soundLoc = ResourceLocation.parse(soundId);
            var holder = BuiltInRegistries.SOUND_EVENT.getHolder(soundLoc);
            if (holder.isPresent()) {
                player.level().playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        holder.get().value(),
                        SoundSource.PLAYERS,
                        1.0F, 1.0F
                );
            } else {
                // 回退到默认横扫音效
                player.level().playSound(
                        null,
                        player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_ATTACK_SWEEP,
                        SoundSource.PLAYERS,
                        1.0F, 1.0F
                );
            }
        } catch (Exception e) {
            player.level().playSound(
                    null,
                    player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS,
                    1.0F, 1.0F
            );
        }
    }

    /**
     * Epic Fight 动画播放
     * 通过反射调用 EF API。
     */
    private static void playEpicFightAnimation(Player player, SupportAttackData attackData, ItemStack toWeapon) {
        try {
            Class<?> epicFightCaps = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> playerPatchClass = Class.forName("yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch");
            Class<?> livingEntityPatchClass = Class.forName("yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            Class<?> assetAccessorClass = Class.forName("yesman.epicfight.api.asset.AssetAccessor");

            // 1. 获取玩家的 EntityPatch（使用 PlayerPatch.class 以确保返回 PlayerPatch 实例）
            var getEntityPatchMethod = epicFightCaps.getMethod("getEntityPatch",
                    net.minecraft.world.entity.Entity.class, Class.class);
            Object playerPatch = getEntityPatchMethod.invoke(null, player, playerPatchClass);

            if (playerPatch == null) {
                EFDCMod.LOGGER.warn("EF player patch is null for player {}, skipping animation", player.getName().getString());
                return;
            }

            // 2. 确定要播放的动画 AssetAccessor
            Object animationAccessor;

            if (attackData.isAutoAnimation()) {
                // === AUTO 模式：从 toWeapon 的 EF 武器能力获取地面攻击动画 ===
                animationAccessor = resolveAutoAnimation(toWeapon, playerPatch);
                if (animationAccessor == null) {
                    EFDCMod.LOGGER.warn("EF auto animation resolution failed for weapon {}, falling back to vanilla",
                            toWeapon.isEmpty() ? "empty" : toWeapon.getItem());
                    return;
                }
            } else {
                // === 手动模式：使用 JSON 中配置的固定动画路径 ===
                ResourceLocation animLoc = attackData.getEfAnimation();
                Class<?> animManagerClass = Class.forName("yesman.epicfight.api.animation.AnimationManager");
                var byKeyMethod = animManagerClass.getMethod("byKey", ResourceLocation.class);
                animationAccessor = byKeyMethod.invoke(null, animLoc);
            }

            if (animationAccessor == null) {
                EFDCMod.LOGGER.warn("EF animation accessor is null");
                return;
            }

            // 3. 检查动画是否已注册
            var isPresentMethod = animationAccessor.getClass().getMethod("isPresent");
            boolean present = (boolean) isPresentMethod.invoke(animationAccessor);
            if (!present) {
                EFDCMod.LOGGER.warn("EF animation not present in registry");
                return;
            }
            var playMethod = livingEntityPatchClass.getMethod("playAnimationInstantly", assetAccessorClass);
            playMethod.invoke(playerPatch, animationAccessor);

            EFDCMod.LOGGER.info("EF animation played successfully (mode={}) for player {}",
                    attackData.isAutoAnimation() ? "auto" : "manual",
                    player.getName().getString());

        } catch (ClassNotFoundException e) {
            EFDCMod.LOGGER.warn("Epic Fight classes not found, cannot play animation", e);
        } catch (NoSuchMethodException e) {
            EFDCMod.LOGGER.warn("Epic Fight API method signature mismatch, animation playback failed", e);
        } catch (Exception e) {
            EFDCMod.LOGGER.warn("Failed to play Epic Fight animation for player {}", player.getName().getString(), e);
        }
    }

    /**
     * @param weaponStack 目标武器的 ItemStack
     * @param playerPatch 玩家的 PlayerPatch 对象（由 EF getEntityPatch 获取）
     * @return 攻击动画的 AssetAccessor，失败返回 null
     */
    private static Object resolveAutoAnimation(ItemStack weaponStack, Object playerPatch) {
        try {
            if (weaponStack.isEmpty()) {
                EFDCMod.LOGGER.debug("Auto animation: weapon stack is empty");
                return null;
            }

            // 1. EpicFightCapabilities.getItemStackCapability(ItemStack) → CapabilityItem
            Class<?> epicFightCaps = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> playerPatchClass = Class.forName("yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch");
            var getItemCapMethod = epicFightCaps.getMethod("getItemStackCapability", net.minecraft.world.item.ItemStack.class);
            Object capItem = getItemCapMethod.invoke(null, weaponStack);

            if (capItem == null) {
                EFDCMod.LOGGER.debug("Auto animation: item capability is null for {}", weaponStack.getItem());
                return null;
            }

            var getAutoAttackMotionMethod = capItem.getClass().getMethod("getAutoAttackMotion", playerPatchClass);
            Object animList = getAutoAttackMotionMethod.invoke(capItem, playerPatch);

            if (animList instanceof java.util.List<?> list && !list.isEmpty()) {
                Object animationAccessor = list.get(0);
                if (animationAccessor != null) {
                    EFDCMod.LOGGER.debug("Auto animation resolved: {} for weapon {}",
                            animationAccessor, weaponStack.getItem());
                    return animationAccessor;
                }
            }

            EFDCMod.LOGGER.debug("Auto animation: attack motion list is empty for {}", weaponStack.getItem());
            return null;

        } catch (ClassNotFoundException e) {
            EFDCMod.LOGGER.warn("Epic Fight classes not found for auto animation", e);
        } catch (NoSuchMethodException e) {
            EFDCMod.LOGGER.warn("Epic Fight API method signature mismatch for auto animation", e);
        } catch (Exception e) {
            EFDCMod.LOGGER.warn("Failed to resolve auto animation for weapon {}", weaponStack.getItem(), e);
        }
        return null;
    }
}

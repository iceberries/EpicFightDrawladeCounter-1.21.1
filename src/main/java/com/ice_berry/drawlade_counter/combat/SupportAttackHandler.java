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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.fml.ModList;
import org.jetbrains.annotations.Nullable;

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

    private static boolean epicFightLoaded = false;

    private SupportAttackHandler() {}

    /**
     * 在模组构造阶段调用一次，检测 Epic Fight 是否可用
     */
    public static void init() {
        epicFightLoaded = ModList.get().isLoaded("epicfight");
        if (epicFightLoaded) {
            EFDCMod.LOGGER.info("Epic Fight detected — support attack will use EF animation system");
        } else {
            EFDCMod.LOGGER.info("Epic Fight not detected — support attack will use vanilla combat fallback");
        }
    }

    public static boolean isEpicFightLoaded() {
        return epicFightLoaded;
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

        // 1. 范围搜索目标
        List<LivingEntity> targets = findTargets(player, attackData.getRange());
        if (targets.isEmpty()) {
            EFDCMod.LOGGER.debug("Support attack triggered but no targets in range for player {}", player.getName().getString());
            return;
        }

        // 2. 计算伤害
        float baseDamage = player.getAttackStrengthScale(0.5F);
        float weaponDamage = toWeapon.isEmpty() ? 1.0F
                : (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
        float totalDamage = (baseDamage + weaponDamage) * attackData.getDamageMultiplier();

        // 3. 投递事件（允许第三方修改伤害倍率）
        boolean notCancelled = SupportAttackEvent.fire(
                player, attackData, fromWeapon, toWeapon, targets, attackData.getDamageMultiplier());

        if (!notCancelled) return;

        // 重新获取可能被事件修改的倍率（简单方案：使用数据中的倍率）
        // 注意：如果需要获取事件中修改后的倍率，可以在回调中直接修改 totalDamage

        // 4. 对目标造成伤害
        Vec3 playerLook = player.getLookAngle();
        for (LivingEntity target : targets) {
            // 排除自身和同队
            if (target == player) continue;
            if (!target.isAlive()) continue;

            // 击退方向
            Vec3 knockbackDir = target.position().subtract(player.position())
                    .normalize().add(0, 0.2, 0).normalize()
                    .scale(attackData.getKnockback());

            target.hurt(serverLevel.damageSources().playerAttack(player), totalDamage);
            target.knockback(
                    attackData.getKnockback() * 2.0F,
                    -knockbackDir.x,
                    -knockbackDir.z
            );
            target.invulnerableTime = 0; // 忽略无敌帧
        }

        // 5. 播放音效
        playSound(player, attackData.getSound());

        // 6. Epic Fight 动画集成
        if (epicFightLoaded && attackData.hasEfAnimation()) {
            playEpicFightAnimation(player, attackData);
        }

        // 7. 同步冷却到客户端
        if (player instanceof ServerPlayer serverPlayer) {
            CooldownUpdatePacket packet = new CooldownUpdatePacket(flowSlot, attackData.getCooldownTicks());
            NetworkHandler.sendToClient(serverPlayer, packet);

            // 通知客户端播放粒子特效
            TriggerEffectPacket effectPacket = new TriggerEffectPacket(
                    (int) player.getX(), (int) player.getY(), (int) player.getZ(),
                    attackData.getParticle(), (float)playerLook.x, (float)playerLook.y, (float)playerLook.z
            );
            NetworkHandler.sendToClient(serverPlayer, effectPacket);
        }

        EFDCMod.LOGGER.debug("Support attack executed by {} on {} targets, damage={}",
                player.getName().getString(), targets.size(), totalDamage);
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
     * 通过反射调用 EF API，确保 EF 不存在时不会崩溃。
     * 当 EF 加载时，使用 PlayerPatch 播放配置的攻击动画。
     */
    @SuppressWarnings("unchecked")
    private static void playEpicFightAnimation(Player player, SupportAttackData attackData) {
        try {
            // EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class)
            Class<?> epicFightCaps = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> playerPatchClass = Class.forName("yesman.epicfight.gameasset.Animations");
            Class<?> livingEntityPatchClass = Class.forName("yesman.epicfight.world.entity.LivingEntityPatch");

            var getEntityPatchMethod = epicFightCaps.getMethod("getEntityPatch",
                    net.minecraft.world.entity.LivingEntity.class, Class.class);
            Object entityPatch = getEntityPatchMethod.invoke(null, player, livingEntityPatchClass);

            if (entityPatch == null) {
                EFDCMod.LOGGER.debug("EF entity patch is null for player {}, skipping animation", player.getName().getString());
                return;
            }

            // 通过 AnimationManager.byKey 获取动画
            ResourceLocation animLoc = attackData.getEfAnimation();
            Class<?> animManagerClass = Class.forName("yesman.epicfight.api.animation.AnimationManager");
            var byKeyMethod = animManagerClass.getMethod("byKey", ResourceLocation.class);
            Object animationAccessor = byKeyMethod.invoke(null, animLoc);

            if (animationAccessor == null) {
                EFDCMod.LOGGER.warn("EF animation not found: {}", animLoc);
                return;
            }

            // 检查动画是否可用
            var isPresentMethod = animationAccessor.getClass().getMethod("isPresent");
            boolean present = (boolean) isPresentMethod.invoke(animationAccessor);
            if (!present) {
                EFDCMod.LOGGER.warn("EF animation not present: {}", animLoc);
                return;
            }

            // 获取动画对象
            var getMethod = animationAccessor.getClass().getMethod("get");
            Object animation = getMethod.invoke(animationAccessor);

            // 播放动画：entityPatch.playAnimationInstantly(animation)
            var playMethod = livingEntityPatchClass.getMethod("playAnimationInstantly",
                    Class.forName("yesman.epicfight.api.animation.types.StaticAnimation"));
            playMethod.invoke(entityPatch, animation);

            EFDCMod.LOGGER.debug("EF animation played: {} for player {}",
                    animLoc, player.getName().getString());

        } catch (ClassNotFoundException e) {
            EFDCMod.LOGGER.warn("Epic Fight classes not found, cannot play animation", e);
        } catch (NoSuchMethodException e) {
            EFDCMod.LOGGER.warn("Epic Fight API method not found, animation playback failed", e);
        } catch (Exception e) {
            EFDCMod.LOGGER.warn("Failed to play Epic Fight animation", e);
        }
    }
}

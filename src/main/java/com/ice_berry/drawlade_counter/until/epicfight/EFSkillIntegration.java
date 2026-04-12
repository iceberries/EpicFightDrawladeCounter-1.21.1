package com.ice_berry.drawlade_counter.until.epicfight;

import net.neoforged.fml.ModList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * Epic Fight 技能系统集成工具类
 * 通过反射访问 EF API，提供格挡状态检测、动画锁定判断等功能。
 */
public final class EFSkillIntegration {

    private static final Logger LOGGER = LogUtils.getLogger();

    private EFSkillIntegration() {}

    /** EF LivingMotions.BLOCK 的字符串表示 */
    private static final String LIVING_MOTION_BLOCK = "BLOCK";

    /**
     * 检查玩家当前是否被 EF 动画锁定（不允许切换物品）
     *
     * @param player 要检查的玩家
     * @return true 表示被锁定；EF 未加载时返回 false
     */
    public static boolean isPlayerInAttackAnimation(Object player) {
        if (!ModList.get().isLoaded("epicfight")) return false;

        try {
            Class<?> capsClass = Class.forName("yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> patchClass = Class.forName("yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");

            var getPatch = capsClass.getMethod("getEntityPatch",
                    net.minecraft.world.entity.Entity.class, Class.class);
            Object patch = getPatch.invoke(null, player, patchClass);
            if (patch == null) return false;

            // 获取 EntityState 并检查 canSwitchHoldingItem
            var getEntityState = patchClass.getMethod("getEntityState");
            Object entityState = getEntityState.invoke(patch);
            var canSwitch = entityState.getClass().getMethod("canSwitchHoldingItem");
            return !(boolean) canSwitch.invoke(entityState);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 EF 武器的攻击伤害值
     *
     * @param weaponStack 武器 ItemStack
     * @return EF 计算的攻击伤害值，EF 未暴露该方法时返回 0
     */
    public static float getEFWeaponDamage(Object weaponStack) {
        if (!ModList.get().isLoaded("epicfight")) return 0F;
        return 0F;
    }

    // #region 格挡监听集成

    /**
     * 获取 EF GuardSkill 的 Class 对象
     *
     * @return GuardSkill 的 Class 对象，获取失败返回 null
     */
    public static Class<?> getGuardSkillClass() {
        if (!ModList.get().isLoaded("epicfight")) return null;

        try {
            return Class.forName("yesman.epicfight.skill.guard.GuardSkill");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * 通过反射获取玩家的 EF LivingEntityPatch 实例
     *
     * @param player 玩家实例
     * @return LivingEntityPatch 实例，获取失败返回 null
     */
    private static Object getEntityPatch(Player player) {
        try {
            Class<?> capsClass = Class.forName(
                    "yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> patchClass = Class.forName(
                    "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            var getPatch = capsClass.getMethod("getEntityPatch",
                    net.minecraft.world.entity.Entity.class, Class.class);
            return getPatch.invoke(null, player, patchClass);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查玩家是否处于 EF 格挡状态
     * 通过 Animator.currentMotion() 是否为 BLOCK 判断
     *
     * @param player 玩家
     * @return true 表示正在格挡中，EF 未加载返回 false
     */
    public static boolean isGuarding(Player player) {
        if (!ModList.get().isLoaded("epicfight")) return false;

        try {
            Object patch = getEntityPatch(player);
            if (patch == null) return false;

            // 获取 Animator 并检查 currentMotion 是否为 BLOCK
            Class<?> patchClass = Class.forName(
                    "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            java.lang.reflect.Method getAnimator = patchClass.getMethod("getAnimator");
            Object animator = getAnimator.invoke(patch);
            java.lang.reflect.Method currentMotion = animator.getClass().getMethod("currentMotion");
            Object motion = currentMotion.invoke(animator);

            return LIVING_MOTION_BLOCK.equals(motion.toString());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 EF AttackResult.ResultType 的 Class 对象
     *
     * @return ResultType 的 Class 对象，获取失败返回 null
     */
    public static Class<?> getResultTypeClass() {
        if (!ModList.get().isLoaded("epicfight")) return null;

        try {
            return Class.forName("yesman.epicfight.api.utils.AttackResult$ResultType");
        } catch (ClassNotFoundException e1) {
            try {
                return Class.forName("yesman.epicfight.gameasset.MobAttackAnimations$ContactLevelChange$ResultType");
            } catch (ClassNotFoundException e2) {
                try {
                    return Class.forName("yesman.epicfight.api.animation.types.ContactLevelChangeAnimation$ResultType");
                } catch (ClassNotFoundException e3) {
                    return null;
                }
            }
        }
    }

    /**
     * 判断 EF 伤害结果是否为格挡成功（BLOCKED）
     *
     * @param resultObj EF 伤害结果对象
     * @return true 表示格挡成功，EF 未加载或检查失败返回 false
     */
    public static boolean isBlockedResult(Object resultObj) {
        if (!ModList.get().isLoaded("epicfight")) return false;
        if (resultObj == null) return false;

        try {
            var getResult = resultObj.getClass().getMethod("getResult");
            Object resultType = getResult.invoke(resultObj);
            if (resultType instanceof Enum<?> enumVal) {
                return "BLOCKED".equals(enumVal.name());
            }
            return "BLOCKED".equals(resultType.toString());
        } catch (NoSuchMethodException e) {
            // getResult() 不存在时通过 toString 回退检查
            try {
                return resultObj.toString().contains("BLOCKED");
            } catch (Exception ex) {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    // #endregion

    // #region 完美格挡检测

    /**
     * 检查玩家的 GuardSkill 是否处于激活状态
     * 遍历 PlayerSkills 中的 SkillContainer 查找已激活的 GuardSkill
     *
     * @param player 玩家
     * @return true 表示 GuardSkill 的容器已激活
     */
    public static boolean isGuardSkillActivated(Player player) {
        if (!ModList.get().isLoaded("epicfight")) return false;

        try {
            Object patch = getEntityPatch(player);
            if (patch == null) return false;

            // 获取 PlayerSkills 并遍历 SkillContainer
            java.lang.reflect.Method getPlayerSkills = patch.getClass().getMethod("getPlayerSkills");
            Object playerSkills = getPlayerSkills.invoke(patch);
            java.lang.reflect.Method listContainers = playerSkills.getClass().getMethod("listSkillContainers");

            boolean[] found = {false};
            try (java.util.stream.Stream<?> containers =
                    (java.util.stream.Stream<?>) listContainers.invoke(playerSkills)) {
                containers.forEach(container -> {
                    if (found[0]) return;
                    try {
                        // 检查容器是否激活且技能为 GuardSkill
                        java.lang.reflect.Method isActivated =
                                container.getClass().getMethod("isActivated");
                        if (!(boolean) isActivated.invoke(container)) return;

                        java.lang.reflect.Method getSkill =
                                container.getClass().getMethod("getSkill");
                        Object skill = getSkill.invoke(container);
                        if (skill != null && skill.getClass().getName().contains("GuardSkill")) {
                            found[0] = true;
                        }
                    } catch (Exception ignored) {
                    }
                });
            }
            return found[0];
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取玩家当前格挡状态名称
     * EF 21.12.5 中格挡结果在服务端处理，客户端只能检测是否正在格挡
     *
     * @param player 玩家
     * @return "GUARDING" 表示正在格挡中，否则返回 null
     */
    public static String getGuardResultName(Player player) {
        if (!ModList.get().isLoaded("epicfight")) return null;

        try {
            if (isGuardSkillActivated(player) && isGuarding(player)) {
                return "GUARDING";
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查玩家是否触发了完美格挡（弹反）
     * EF 中完美格挡判定在服务端，客户端无法直接读取
     *
     * @param player 玩家
     * @return 当前始终返回 false，通过 hurtTime 检测 + 窗口机制实现
     */
    public static boolean isParried(Player player) {
        return false;
    }

    // #endregion

    // #region 锁定目标与攻击状态检测

    /**
     * 获取客户端 EF 锁定目标实体
     * 通过 PlayerPatch（客户端为 ClientPlayerPatch）的 getTarget() 方法获取。
     * 仅在客户端有效，服务端调用返回 null。
     *
     * @param player 本地玩家
     * @return 锁定的 LivingEntity，未锁定或 EF 未加载时返回 null
     */
    public static LivingEntity getLockOnTarget(Player player) {
        if (!ModList.get().isLoaded("epicfight")) {
            LOGGER.debug("[EFDC] getLockOnTarget: EF 未加载");
            return null;
        }

        try {
            Object patch = getEntityPatch(player);
            if (patch == null) {
                LOGGER.debug("[EFDC] getLockOnTarget: PlayerPatch 为 null");
                return null;
            }

            // ClientPlayerPatch 继承自 PlayerPatch，具有 getTarget() 方法
            java.lang.reflect.Method getTarget = patch.getClass().getMethod("getTarget");
            Object target = getTarget.invoke(patch);
            LOGGER.debug("[EFDC] getLockOnTarget: target={}, isLiving={}",
                    target, target instanceof LivingEntity);
            if (target instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
            return null;
        } catch (NoSuchMethodException e) {
            LOGGER.debug("[EFDC] getLockOnTarget: getTarget() 方法未找到", e);
            return null;
        } catch (Exception e) {
            LOGGER.debug("[EFDC] getLockOnTarget: 反射调用失败", e);
            return null;
        }
    }

    /**
     * 检查指定实体是否处于 EF 攻击状态
     * 通过反射调用 EntityState.attacking() 判断。
     *
     * @param entity 要检查的实体
     * @return true 表示正在执行攻击动画，EF 未加载或检查失败返回 false
     */
    public static boolean isEntityAttacking(LivingEntity entity) {
        if (!ModList.get().isLoaded("epicfight")) return false;

        try {
            Class<?> capsClass = Class.forName(
                    "yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> patchClass = Class.forName(
                    "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");

            java.lang.reflect.Method getPatch = capsClass.getMethod("getEntityPatch",
                    net.minecraft.world.entity.Entity.class, Class.class);
            Object patch = getPatch.invoke(null, entity, patchClass);
            LOGGER.debug("[EFDC] isEntityAttacking: entity={}, patch={}",
                    entity.getName().getString(), patch != null ? "存在" : "null");
            if (patch == null) return false;

            java.lang.reflect.Method getEntityState = patchClass.getMethod("getEntityState");
            Object entityState = getEntityState.invoke(patch);
            java.lang.reflect.Method attacking = entityState.getClass().getMethod("attacking");
            boolean result = (boolean) attacking.invoke(entityState);
            LOGGER.debug("[EFDC] isEntityAttacking: entity={}, attacking={}",
                    entity.getName().getString(), result);
            return result;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            LOGGER.debug("[EFDC] isEntityAttacking: EF 类/方法未找到 (entity={})",
                    entity.getName().getString(), e);
            return false;
        } catch (Exception e) {
            LOGGER.debug("[EFDC] isEntityAttacking: 反射调用失败 (entity={})",
                    entity.getName().getString(), e);
            return false;
        }
    }

    /**
     * 获取服务端玩家的 EF 锁定攻击目标
     * 通过反射调用 ServerPlayerPatch.getTarget() 实现。
     *
     * @param player 服务端玩家
     * @return 锁定的 LivingEntity，未锁定或 EF 未加载时返回 null
     */
    public static LivingEntity getServerPlayerTarget(ServerPlayer player) {
        if (!ModList.get().isLoaded("epicfight")) return null;

        try {
            Class<?> capsClass = Class.forName(
                    "yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> serverPatchClass = Class.forName(
                    "yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch");

            java.lang.reflect.Method getPatch = capsClass.getMethod("getEntityPatch",
                    net.minecraft.world.entity.Entity.class, Class.class);
            Object patch = getPatch.invoke(null, player, serverPatchClass);
            LOGGER.debug("[EFDC] getServerPlayerTarget: player={}, patch={}",
                    player.getName().getString(), patch != null ? patch.getClass().getSimpleName() : "null");
            if (patch == null) return null;

            java.lang.reflect.Method getTarget = patch.getClass().getMethod("getTarget");
            Object target = getTarget.invoke(patch);
            LOGGER.debug("[EFDC] getServerPlayerTarget: target={}, isLiving={}",
                    target, target instanceof LivingEntity);
            if (target instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
            return null;
        } catch (NoSuchMethodException e) {
            LOGGER.debug("[EFDC] getServerPlayerTarget: getTarget() 方法未找到", e);
            return null;
        } catch (Exception e) {
            LOGGER.debug("[EFDC] getServerPlayerTarget: 反射调用失败", e);
            return null;
        }
    }

    // #endregion

    // #region 弹反格挡激活

    /**
     * 强制激活玩家的 EF GuardSkill（服务端）
     * 通过反射获取 ServerPlayerPatch 和 GUARD 槽位的 SkillContainer，
     * 调用 requestHold 激活格挡。激活后 EF 原版的耐力消耗、格挡动画、
     * 正面检测、音效粒子等机制全部正常工作。
     *
     * @param player 服务端玩家
     * @return true 表示格挡成功激活（耐力等资源已消耗）
     */
    public static boolean forceActivateGuard(ServerPlayer player) {
        if (!ModList.get().isLoaded("epicfight")) {
            LOGGER.debug("[EFDC] forceActivateGuard: EF 未加载");
            return false;
        }

        try {
            Object serverPatch = getServerPlayerPatch(player);
            if (serverPatch == null) {
                LOGGER.debug("[EFDC] forceActivateGuard: ServerPlayerPatch 为 null");
                return false;
            }

            Object container = getGuardSkillContainer(serverPatch);
            if (container == null) {
                LOGGER.debug("[EFDC] forceActivateGuard: GuardSkillContainer 为 null");
                return false;
            }

            // 调用 requestHold(ServerPlayerPatch, CompoundTag) 激活格挡
            for (java.lang.reflect.Method m : container.getClass().getMethods()) {
                if ("requestHold".equals(m.getName()) && m.getParameterCount() == 2) {
                    boolean result = (boolean) m.invoke(container, serverPatch,
                            (net.minecraft.nbt.CompoundTag) null);
                    LOGGER.debug("[EFDC] forceActivateGuard: requestHold 结果={}", result);
                    return result;
                }
            }
            LOGGER.debug("[EFDC] forceActivateGuard: 未找到 requestHold(ServerPlayerPatch, CompoundTag) 方法");
            return false;
        } catch (Exception e) {
            LOGGER.debug("[EFDC] forceActivateGuard: 反射调用失败", e);
            return false;
        }
    }

    /**
     * 取消玩家的 EF GuardSkill（服务端）
     * 弹反触发后调用，防止格挡状态持续存在。
     *
     * @param player 服务端玩家
     */
    public static void cancelGuard(ServerPlayer player) {
        if (!ModList.get().isLoaded("epicfight")) return;

        try {
            Object serverPatch = getServerPlayerPatch(player);
            if (serverPatch == null) return;

            Object container = getGuardSkillContainer(serverPatch);
            if (container == null) return;

            for (java.lang.reflect.Method m : container.getClass().getMethods()) {
                if ("requestCancel".equals(m.getName()) && m.getParameterCount() == 2) {
                    m.invoke(container, serverPatch, (net.minecraft.nbt.CompoundTag) null);
                    return;
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 获取服务端玩家的 ServerPlayerPatch 实例
     *
     * @param player 服务端玩家
     * @return ServerPlayerPatch 实例，获取失败返回 null
     */
    private static Object getServerPlayerPatch(ServerPlayer player) {
        try {
            Class<?> capsClass = Class.forName(
                    "yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> serverPatchClass = Class.forName(
                    "yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch");

            java.lang.reflect.Method getPatch = capsClass.getMethod("getEntityPatch",
                    net.minecraft.world.entity.Entity.class, Class.class);
            return getPatch.invoke(null, player, serverPatchClass);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 GuardSkill 对应的 SkillContainer
     * 通过反射访问 SkillSlots.GUARD 获取槽位索引，
     * 再调用 PlayerPatch.getSkill(int) 获取容器。
     *
     * @param serverPlayerPatch ServerPlayerPatch 实例
     * @return SkillContainer 实例，获取失败返回 null
     */
    private static Object getGuardSkillContainer(Object serverPlayerPatch) {
        try {
            Class<?> skillSlotsClass = Class.forName("yesman.epicfight.skill.SkillSlots");
            Object guardSlot = skillSlotsClass.getField("GUARD").get(null);

            int slotIndex;
            if (guardSlot instanceof Number n) {
                slotIndex = n.intValue();
            } else {
                slotIndex = ((java.lang.Enum<?>) guardSlot).ordinal();
            }

            java.lang.reflect.Method getSkill =
                    serverPlayerPatch.getClass().getMethod("getSkill", int.class);
            return getSkill.invoke(serverPlayerPatch, slotIndex);
        } catch (Exception e) {
            return null;
        }
    }

    // #endregion
}

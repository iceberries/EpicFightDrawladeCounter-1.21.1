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
     * 通过反射获取实体的 EF LivingEntityPatch 实例
     * 接受 LivingEntity 类型，适用于怪物等非玩家实体。
     *
     * @param entity 实体实例
     * @return LivingEntityPatch 实例，获取失败返回 null
     */
    private static Object getEntityPatchForEntity(LivingEntity entity) {
        try {
            Class<?> capsClass = Class.forName(
                    "yesman.epicfight.world.capabilities.EpicFightCapabilities");
            Class<?> patchClass = Class.forName(
                    "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            var getPatch = capsClass.getMethod("getEntityPatch",
                    net.minecraft.world.entity.Entity.class, Class.class);
            return getPatch.invoke(null, entity, patchClass);
        } catch (Exception e) {
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
     * 检查指定实体是否处于 EF 攻击动画中（含抬手前摇）
     * 通过反射调用 EntityState.getLevel() 判断。
     * PHASE_LEVEL: 0=空闲, 1=抬手前摇(anticipation), 2=攻击判定(attacking), 3=恢复(recovery)
     * 仅由 AttackAnimation.bindPhaseState() 设置，不会受受伤等非攻击动画影响。
     *
     * @param entity 要检查的实体
     * @return true 表示正在执行攻击动画（含前摇），EF 未加载或检查失败返回 false
     */
    public static boolean isEntityAttacking(LivingEntity entity) {
        if (!ModList.get().isLoaded("epicfight")) return false;

        try {
            Object patch = getEntityPatchForEntity(entity);
            LOGGER.debug("[EFDC] isEntityAttacking: entity={}, patch={}",
                    entity.getName().getString(), patch != null ? "存在" : "null");
            if (patch == null) return false;

            Class<?> patchClass = Class.forName(
                    "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            java.lang.reflect.Method getEntityState = patchClass.getMethod("getEntityState");
            Object entityState = getEntityState.invoke(patch);
            java.lang.reflect.Method getLevel = entityState.getClass().getMethod("getLevel");
            int level = (int) getLevel.invoke(entityState);
            boolean result = level > 0;
            LOGGER.debug("[EFDC] isEntityAttacking: entity={}, phaseLevel={}, attacking={}",
                    entity.getName().getString(), level, result);
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

    /**
     * 获取指定实体当前攻击动画第一阶段的 preDelay 时间（秒）
     * preDelay 是攻击抬手前摇阶段的结束时刻，
     * 即 EntityState.PHASE_LEVEL 从 1(anticipation) 变为 2(attacking) 的时间点。
     *
     * 通过反射获取 baseLayer 的 animationPlayer，再从当前播放动画（或 LinkAnimation
     * 过渡期的目标动画）中读取 AttackAnimation.phases[0].preDelay。
     * 注意：EF 的 Animator.findFor(Class) 存在 bug，检查的是
     * AssetAccessor.getClass() 而非实际动画对象的 getClass()，导致
     * findFor(AttackAnimation.class) 永远返回 null，因此不能使用该方法。
     *
     * @param entity 要获取 preDelay 的实体
     * @return 第一阶段的 preDelay 值（秒），非攻击动画或反射失败时返回 0
     */
    public static float getAttackPhasePreDelay(LivingEntity entity) {
        if (!ModList.get().isLoaded("epicfight")) return 0;

        try {
            Object patch = getEntityPatchForEntity(entity);
            if (patch == null) return 0;

            Class<?> patchClass = Class.forName(
                    "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            java.lang.reflect.Method getAnimator = patchClass.getMethod("getAnimator");
            Object animator = getAnimator.invoke(patch);

            // 获取 baseLayer.animationPlayer（均为 public 字段）
            java.lang.reflect.Field baseLayerField =
                    animator.getClass().getField("baseLayer");
            Object baseLayer = baseLayerField.get(animator);
            java.lang.reflect.Field animPlayerField =
                    baseLayer.getClass().getField("animationPlayer");
            Object animationPlayer = animPlayerField.get(baseLayer);

            // 先尝试 getRealAnimation()：LinkAnimation 过渡期会返回目标动画的 accessor
            Object animation = null;
            try {
                java.lang.reflect.Method getRealAnim =
                        animationPlayer.getClass().getMethod("getRealAnimation");
                Object realAnimAccessor = getRealAnim.invoke(animationPlayer);
                if (realAnimAccessor != null) {
                    java.lang.reflect.Method get = realAnimAccessor.getClass().getMethod("get");
                    animation = get.invoke(realAnimAccessor);
                }
            } catch (NoSuchMethodException ignored) {
            }

            // 非过渡期时直接获取 animationPlayer 当前播放的动画
            if (animation == null) {
                java.lang.reflect.Method getAnimAccessor =
                        animationPlayer.getClass().getMethod("getAnimation");
                Object animAccessor = getAnimAccessor.invoke(animationPlayer);
                java.lang.reflect.Method get = animAccessor.getClass().getMethod("get");
                animation = get.invoke(animAccessor);
            }

            if (animation == null) {
                LOGGER.debug("[EFDC] getAttackPhasePreDelay: 无法获取动画对象 (entity={})",
                        entity.getName().getString());
                return 0;
            }

            // 检查实际动画对象是否为 AttackAnimation 实例
            Class<?> attackAnimClass = Class.forName(
                    "yesman.epicfight.api.animation.types.AttackAnimation");
            if (!attackAnimClass.isAssignableFrom(animation.getClass())) {
                LOGGER.debug("[EFDC] getAttackPhasePreDelay: 当前动画非 AttackAnimation (entity={}, animClass={})",
                        entity.getName().getString(), animation.getClass().getSimpleName());
                return 0;
            }

            // 读取 public final Phase[] phases 字段，取第一个 phase 的 preDelay
            java.lang.reflect.Field phasesField = attackAnimClass.getField("phases");
            Object phases = phasesField.get(animation);
            if (phases != null && java.lang.reflect.Array.getLength(phases) > 0) {
                Object firstPhase = java.lang.reflect.Array.get(phases, 0);
                java.lang.reflect.Field preDelayField = firstPhase.getClass().getField("preDelay");
                float preDelay = preDelayField.getFloat(firstPhase);
                LOGGER.debug("[EFDC] getAttackPhasePreDelay: entity={}, preDelay={}s",
                        entity.getName().getString(), preDelay);
                return preDelay;
            }
            return 0;
        } catch (Exception e) {
            LOGGER.debug("[EFDC] getAttackPhasePreDelay: 反射调用失败 (entity={})",
                    entity.getName().getString(), e);
            return 0;
        }
    }

    /**
     * 获取指定实体当前攻击动画的总时长（秒）
     * 通过反射依次尝试 Animator 的多种方法名获取当前播放的动画，
     * 再调用其 getTotalTime() 获取动画总时长。
     *
     * @param entity 要获取动画时长的实体
     * @return 动画总时长（秒），EF 未加载或反射失败时返回 0
     */
    public static float getAttackAnimationDuration(LivingEntity entity) {
        if (!ModList.get().isLoaded("epicfight")) return 0;

        try {
            Object patch = getEntityPatchForEntity(entity);
            if (patch == null) return 0;

            Class<?> patchClass = Class.forName(
                    "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch");
            java.lang.reflect.Method getAnimator = patchClass.getMethod("getAnimator");
            Object animator = getAnimator.invoke(patch);

            // 尝试多种方法名获取当前播放的动画对象
            Object animation = null;
            String[] methodNames = {"getCurrentAnimation", "getPlayingAnimation", "currentAnimation"};
            for (String name : methodNames) {
                try {
                    java.lang.reflect.Method m = animator.getClass().getMethod(name);
                    animation = m.invoke(animator);
                    if (animation != null) break;
                } catch (NoSuchMethodException ignored) {
                }
            }
            if (animation == null) {
                LOGGER.debug("[EFDC] getAttackAnimationDuration: 无法获取当前动画 (entity={})",
                        entity.getName().getString());
                return 0;
            }

            // 获取动画总时长（秒）
            java.lang.reflect.Method getTotalTime = animation.getClass().getMethod("getTotalTime");
            float totalTime = (float) getTotalTime.invoke(animation);
            LOGGER.debug("[EFDC] getAttackAnimationDuration: entity={}, duration={}s",
                    entity.getName().getString(), totalTime);
            return totalTime;
        } catch (Exception e) {
            LOGGER.debug("[EFDC] getAttackAnimationDuration: 反射调用失败 (entity={})",
                    entity.getName().getString(), e);
            return 0;
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

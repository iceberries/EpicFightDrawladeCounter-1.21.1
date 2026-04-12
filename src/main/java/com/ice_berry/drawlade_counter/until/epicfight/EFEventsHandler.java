package com.ice_berry.drawlade_counter.until.epicfight;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Epic Fight 事件处理器
 * 提供弹反标记管理、EF 格挡状态查询和完美格挡验证工具方法。
 */
public final class EFEventsHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 待定弹反设置时的玩家 tick 记录 key */
    private static final String TAG_PENDING_PARRY_TICK = "efdc_pending_parry_tick";

    /** 待定弹反目标实体 ID key */
    private static final String TAG_PENDING_PARRY_TARGET = "efdc_pending_parry_target_id";

    /** 待定弹反超时时间（tick），5 秒 */
    private static final int PENDING_PARRY_TIMEOUT_TICKS = 100;

    private EFEventsHandler() {}

    // #region 注册

    /**
     * 验证 EF 事件总线类是否可用
     */
    public static void register() {
        if (!ModList.get().isLoaded("epicfight")) return;

        try {
            Class.forName("yesman.epicfight.api.event.EpicFightEventHooks");
        } catch (ClassNotFoundException e) {
            // EF 不可用，格挡命中检测将不工作
        }
    }

    // #endregion

    // #region 待定弹反管理

    /**
     * 设置待定弹反标记
     * 当玩家在怪物攻击起手阶段按下武器流转键时调用，
     * 标记将在怪物攻击到达时被消费。
     *
     * @param player 设置标记的玩家
     * @param target 锁定的攻击目标实体
     */
    public static void setPendingParry(Player player, LivingEntity target) {
        player.getPersistentData().putInt(TAG_PENDING_PARRY_TICK, player.tickCount);
        player.getPersistentData().putInt(TAG_PENDING_PARRY_TARGET, target.getId());
        LOGGER.debug("[EFDC] setPendingParry: player={}, target={} (id={})",
                player.getName().getString(), target.getName().getString(), target.getId());
    }

    /**
     * 获取待定弹反的目标实体
     * 如果待定弹反标记有效且未超时，返回目标实体。
     * 超时或目标失效时自动清除标记。
     *
     * @param player 持有标记的玩家
     * @return 待定弹反的目标实体，无有效标记时返回 null
     */
    public static LivingEntity getPendingParryTarget(Player player) {
        var data = player.getPersistentData();
        if (!data.contains(TAG_PENDING_PARRY_TICK)) return null;

        int pendingTick = data.getInt(TAG_PENDING_PARRY_TICK);
        int elapsed = player.tickCount - pendingTick;
        if (elapsed > PENDING_PARRY_TIMEOUT_TICKS) {
            LOGGER.debug("[EFDC] getPendingParryTarget: 超时 ({} ticks > {}), 清除标记",
                    elapsed, PENDING_PARRY_TIMEOUT_TICKS);
            clearPendingParry(player);
            return null;
        }

        if (!data.contains(TAG_PENDING_PARRY_TARGET)) return null;
        int targetId = data.getInt(TAG_PENDING_PARRY_TARGET);
        Entity target = player.level().getEntity(targetId);
        if (target instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
            LOGGER.debug("[EFDC] getPendingParryTarget: 有效, target={} (id={})",
                    livingEntity.getName().getString(), targetId);
            return livingEntity;
        }
        LOGGER.debug("[EFDC] getPendingParryTarget: 目标无效(id={})，清除标记", targetId);
        clearPendingParry(player);
        return null;
    }

    /**
     * 清除指定玩家的待定弹反标记
     *
     * @param player 要清除标记的玩家
     */
    public static void clearPendingParry(Player player) {
        player.getPersistentData().remove(TAG_PENDING_PARRY_TICK);
        player.getPersistentData().remove(TAG_PENDING_PARRY_TARGET);
        LOGGER.debug("[EFDC] clearPendingParry: player={}", player.getName().getString());
    }

    // #endregion

    // #region 完美格挡触发验证

    /**
     * 处理客户端发来的完美格挡触发请求。
     * 优先尝试弹反系统：玩家未格挡时，验证锁定目标正在攻击，设置待定弹反；
     * 回退到格挡反击系统：玩家正在格挡时，验证 lastHurtByMob 攻击者记录。
     *
     * @param player 发起请求的服务端玩家
     * @return 攻击者实体（格挡反击模式），null 表示弹反已设置或验证失败
     */
    public static LivingEntity handlePerfectGuardTrigger(ServerPlayer player) {
        if (!ModList.get().isLoaded("epicfight")) return null;

        boolean isGuarding = EFSkillIntegration.isGuarding(player);
        LOGGER.debug("[EFDC] handlePerfectGuardTrigger: player={}, isGuarding={}",
                player.getName().getString(), isGuarding);

        // 优先尝试弹反系统：玩家未格挡 + 锁定目标正在攻击 → 设置待定弹反
        if (!isGuarding) {
            LivingEntity target = EFSkillIntegration.getServerPlayerTarget(player);
            LOGGER.debug("[EFDC] handlePerfectGuardTrigger: serverTarget={}",
                    target != null ? target.getName().getString() : "null");
            if (target != null && target.isAlive()) {
                boolean attacking = EFSkillIntegration.isEntityAttacking(target);
                LOGGER.debug("[EFDC] handlePerfectGuardTrigger: target={}, alive={}, attacking={}",
                        target.getName().getString(), target.isAlive(), attacking);
                if (attacking) {
                    setPendingParry(player, target);
                    return null;
                }
            }
        }

        // 回退到格挡反击系统：玩家正在格挡 + 有攻击者记录 → 立即触发反击
        if (isGuarding) {
            LivingEntity attacker = player.getLastHurtByMob();
            LOGGER.debug("[EFDC] handlePerfectGuardTrigger: 回退格挡反击, lastHurtByMob={}",
                    attacker != null ? attacker.getName().getString() : "null");
            if (attacker != null && attacker.isAlive() && attacker != player) {
                return attacker;
            }
        }

        LOGGER.debug("[EFDC] handlePerfectGuardTrigger: 所有条件均不满足，返回 null");
        return null;
    }

    // #endregion

    // #region 清理

    /**
     * 清理指定玩家的数据
     *
     * @param playerUUID 玩家 UUID
     */
    public static void cleanup(UUID playerUUID) {
    }

    // #endregion
}

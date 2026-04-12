package com.ice_berry.drawlade_counter.combat.state;

/**
 * 战斗状态枚举
 * 追踪玩家在武器流转系统中的战斗状态。
 */
public enum CombatState {
    /** 空闲状态 -- 可以正常切换武器 */
    IDLE,
    /** 正在执行支援攻击动画/效果 */
    SUPPORT_ATTACKING,
    /** 正在执行反击（闪避反击 / 格挡反击） */
    COUNTER_ATTACKING,
    /** 支援攻击冷却中 -- 仍可切换但不再触发支援攻击 */
    COOLDOWN,
    /** 持有强化攻击（完美格挡触发后） */
    PERFECT_GUARD;

    /**
     * 判断当前状态是否允许切换武器
     *
     * @return true 表示允许切换
     */
    public boolean canSwitchWeapon() {
        return this == IDLE || this == COOLDOWN || this == PERFECT_GUARD;
    }

    /**
     * 判断当前状态是否允许触发支援攻击
     *
     * @return true 表示允许触发
     */
    public boolean canTriggerSupportAttack() {
        return this == IDLE;
    }

    /**
     * 判断当前状态是否允许触发完美格挡判定
     *
     * @return true 表示允许判定
     */
    public boolean canPerfectGuard() {
        return this != SUPPORT_ATTACKING;
    }
}

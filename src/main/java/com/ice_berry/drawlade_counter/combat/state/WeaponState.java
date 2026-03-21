package com.ice_berry.drawlade_counter.combat.state;

/**
 * 武器槽位状态
 * 追踪单个武器槽位在流转过程中的状态。
 */
public enum WeaponState {
    /** 就绪 —— 可以被选中并触发支援攻击 */
    READY,
    /** 当前激活 —— 正在使用中 */
    ACTIVE,
    /** 冷却中 —— 切换走后进入冷却 */
    ON_COOLDOWN;
}

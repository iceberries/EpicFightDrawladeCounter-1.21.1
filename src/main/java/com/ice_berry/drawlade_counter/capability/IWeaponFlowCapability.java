package com.ice_berry.drawlade_counter.capability;

import net.minecraft.nbt.CompoundTag;

/**
 * 武器流转 Capability 接口
 * 管理玩家4格武器流转槽位的状态，包括冷却、激活槽位等。
 * 槽位为闭环序列，切换时自动循环。
 */
public interface IWeaponFlowCapability {

    /** 武器流转槽位总数 */
    int WEAPON_SLOT_COUNT = 4;

    /** 默认快捷栏槽位索引 */
    int[] DEFAULT_WEAPON_SLOT_INDICES = {0, 1, 2, 3};

    // ==================== 槽位索引管理 ====================

    /**
     * 获取当前激活的流转槽位索引 (0 ~ WEAPON_SLOT_COUNT-1)
     */
    int getCurrentActiveSlot();

    /**
     * 设置当前激活的流转槽位索引
     *
     * @param slot 流转槽位索引，会自动取模到合法范围
     */
    void setCurrentActiveSlot(int slot);

    /**
     * 获取指定流转槽位对应的快捷栏索引
     *
     * @param flowSlot 流转槽位索引 (0 ~ WEAPON_SLOT_COUNT-1)
     * @return 对应的快捷栏索引
     */
    int getWeaponSlotIndex(int flowSlot);

    /**
     * 获取全部快捷栏槽位索引数组
     *
     * @return 快捷栏索引数组副本
     */
    int[] getWeaponSlotIndices();

    /**
     * 设置全部快捷栏槽位索引
     *
     * @param indices 快捷栏索引数组，长度必须为 WEAPON_SLOT_COUNT
     */
    void setWeaponSlotIndices(int[] indices);

    // ==================== 冷却管理 ====================

    /**
     * 检查指定流转槽位是否处于冷却中
     *
     * @param flowSlot 流转槽位索引
     * @return true 表示正在冷却
     */
    boolean isSlotOnCooldown(int flowSlot);

    /**
     * 获取指定流转槽位的剩余冷却 tick
     *
     * @param flowSlot 流转槽位索引
     * @return 剩余冷却 tick
     */
    int getSlotCooldown(int flowSlot);

    /**
     * 获取指定流转槽位的最大冷却 tick（用于 HUD 进度计算）
     *
     * @param flowSlot 流转槽位索引
     * @return 最大冷却 tick，无冷却记录时返回 0
     */
    int getSlotMaxCooldown(int flowSlot);

    /**
     * 设置指定流转槽位的冷却时间
     *
     * @param flowSlot 流转槽位索引
     * @param ticks    冷却 tick 数
     */
    void setSlotCooldown(int flowSlot, int ticks);

    /**
     * 清除指定流转槽位的冷却
     *
     * @param flowSlot 流转槽位索引
     */
    void clearSlotCooldown(int flowSlot);

    /**
     * 清除所有流转槽位的冷却
     */
    void clearAllCooldowns();

    // ==================== 武器切换（闭环序列） ====================

    /**
     * 切换到下一个武器槽位（闭环，末尾回到开头）
     */
    void switchToNextWeapon();

    /**
     * 切换到上一个武器槽位（闭环，开头回到末尾）
     */
    void switchToPreviousWeapon();

    // ==================== Tick 更新 ====================

    /**
     * 每服务端 tick 调用，递减所有槽位的冷却时间
     */
    void tick();

    // ==================== 序列化 / 反序列化 ====================

    /**
     * 将当前状态序列化到 NBT
     *
     * @param tag 目标 CompoundTag
     */
    void save(CompoundTag tag);

    /**
     * 从 NBT 反序列化恢复状态
     *
     * @param tag 来源 CompoundTag
     */
    void load(CompoundTag tag);
}

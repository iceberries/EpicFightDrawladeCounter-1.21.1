package com.ice_berry.drawlade_counter.capability;

import net.minecraft.nbt.CompoundTag;

/**
 * 武器流转数据实现
 * 存储玩家4格武器流转槽位的完整状态。
 */
public class WeaponFlowData implements IWeaponFlowCapability {

    /** 当前激活的流转槽位索引 */
    private int currentActiveSlot;

    /** 各流转槽位对应的快捷栏索引 */
    private final int[] weaponSlotIndices;

    /** 各流转槽位的剩余冷却时间（tick） */
    private final int[] slotCooldowns;

    public WeaponFlowData() {
        this.currentActiveSlot = 0;
        this.weaponSlotIndices = DEFAULT_WEAPON_SLOT_INDICES.clone();
        this.slotCooldowns = new int[WEAPON_SLOT_COUNT];
    }

    // ==================== 槽位索引管理 ====================

    @Override
    public int getCurrentActiveSlot() {
        return currentActiveSlot;
    }

    @Override
    public void setCurrentActiveSlot(int slot) {
        this.currentActiveSlot = Math.floorMod(slot, WEAPON_SLOT_COUNT);
    }

    @Override
    public int getWeaponSlotIndex(int flowSlot) {
        if (flowSlot < 0 || flowSlot >= WEAPON_SLOT_COUNT) {
            throw new IndexOutOfBoundsException(
                    "Flow slot index out of range: " + flowSlot + ", size: " + WEAPON_SLOT_COUNT);
        }
        return weaponSlotIndices[flowSlot];
    }

    @Override
    public int[] getWeaponSlotIndices() {
        return weaponSlotIndices.clone();
    }

    @Override
    public void setWeaponSlotIndices(int[] indices) {
        if (indices == null || indices.length != WEAPON_SLOT_COUNT) {
            throw new IllegalArgumentException(
                    "Weapon slot indices array must have length " + WEAPON_SLOT_COUNT);
        }
        System.arraycopy(indices, 0, this.weaponSlotIndices, 0, WEAPON_SLOT_COUNT);
    }

    // ==================== 冷却管理 ====================

    @Override
    public boolean isSlotOnCooldown(int flowSlot) {
        if (flowSlot < 0 || flowSlot >= WEAPON_SLOT_COUNT) {
            throw new IndexOutOfBoundsException(
                    "Flow slot index out of range: " + flowSlot + ", size: " + WEAPON_SLOT_COUNT);
        }
        return slotCooldowns[flowSlot] > 0;
    }

    @Override
    public int getSlotCooldown(int flowSlot) {
        if (flowSlot < 0 || flowSlot >= WEAPON_SLOT_COUNT) {
            throw new IndexOutOfBoundsException(
                    "Flow slot index out of range: " + flowSlot + ", size: " + WEAPON_SLOT_COUNT);
        }
        return slotCooldowns[flowSlot];
    }

    @Override
    public void setSlotCooldown(int flowSlot, int ticks) {
        if (flowSlot < 0 || flowSlot >= WEAPON_SLOT_COUNT) {
            throw new IndexOutOfBoundsException(
                    "Flow slot index out of range: " + flowSlot + ", size: " + WEAPON_SLOT_COUNT);
        }
        this.slotCooldowns[flowSlot] = Math.max(0, ticks);
    }

    @Override
    public void clearSlotCooldown(int flowSlot) {
        if (flowSlot < 0 || flowSlot >= WEAPON_SLOT_COUNT) {
            throw new IndexOutOfBoundsException(
                    "Flow slot index out of range: " + flowSlot + ", size: " + WEAPON_SLOT_COUNT);
        }
        this.slotCooldowns[flowSlot] = 0;
    }

    @Override
    public void clearAllCooldowns() {
        for (int i = 0; i < WEAPON_SLOT_COUNT; i++) {
            slotCooldowns[i] = 0;
        }
    }

    // ==================== 武器切换（闭环序列） ====================

    @Override
    public void switchToNextWeapon() {
        currentActiveSlot = (currentActiveSlot + 1) % WEAPON_SLOT_COUNT;
    }

    @Override
    public void switchToPreviousWeapon() {
        currentActiveSlot = (currentActiveSlot - 1 + WEAPON_SLOT_COUNT) % WEAPON_SLOT_COUNT;
    }

    // ==================== Tick 更新 ====================

    @Override
    public void tick() {
        for (int i = 0; i < WEAPON_SLOT_COUNT; i++) {
            if (slotCooldowns[i] > 0) {
                slotCooldowns[i]--;
            }
        }
    }

    // ==================== 序列化 / 反序列化 ====================

    private static final String TAG_ACTIVE_SLOT = "ActiveSlot";
    private static final String TAG_SLOT_INDICES = "SlotIndices";
    private static final String TAG_SLOT_COOLDOWNS = "SlotCooldowns";

    @Override
    public void save(CompoundTag tag) {
        tag.putInt(TAG_ACTIVE_SLOT, currentActiveSlot);
        tag.putIntArray(TAG_SLOT_INDICES, weaponSlotIndices);
        tag.putIntArray(TAG_SLOT_COOLDOWNS, slotCooldowns);
    }

    @Override
    public void load(CompoundTag tag) {
        if (tag.contains(TAG_ACTIVE_SLOT)) {
            this.currentActiveSlot = Math.floorMod(tag.getInt(TAG_ACTIVE_SLOT), WEAPON_SLOT_COUNT);
        }
        if (tag.contains(TAG_SLOT_INDICES, CompoundTag.TAG_INT_ARRAY)) {
            int[] loaded = tag.getIntArray(TAG_SLOT_INDICES);
            if (loaded.length == WEAPON_SLOT_COUNT) {
                System.arraycopy(loaded, 0, this.weaponSlotIndices, 0, WEAPON_SLOT_COUNT);
            }
        }
        if (tag.contains(TAG_SLOT_COOLDOWNS, CompoundTag.TAG_INT_ARRAY)) {
            int[] loaded = tag.getIntArray(TAG_SLOT_COOLDOWNS);
            if (loaded.length == WEAPON_SLOT_COUNT) {
                System.arraycopy(loaded, 0, this.slotCooldowns, 0, WEAPON_SLOT_COUNT);
            }
        }
    }
}

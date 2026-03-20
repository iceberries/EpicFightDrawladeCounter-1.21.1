package com.ice_berry.drawlade_counter.client.input;

import com.ice_berry.drawlade_counter.EFDCMod;
import com.ice_berry.drawlade_counter.capability.IWeaponFlowCapability;
import com.ice_berry.drawlade_counter.capability.WeaponFlowProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * 武器切换输入处理器
 * 监听客户端 tick 事件，处理模组按键输入和物品栏变化检测，
 * 实现闭环武器流转（槽位 0 → 1 → 2 → 3 → 0）。
 */
public class WeaponFlowInputHandler {

    /** 上一 tick 各武器槽位的物品快照，用于检测物品栏变化 */
    private static final ItemStack[] prevWeaponStacks = new ItemStack[IWeaponFlowCapability.WEAPON_SLOT_COUNT];

    static {
        for (int i = 0; i < prevWeaponStacks.length; i++) {
            prevWeaponStacks[i] = ItemStack.EMPTY;
        }
    }

    /**
     * 客户端 tick 事件处理
     * - 处理按键输入（下一武器 / 上一武器）
     * - 检测物品栏变化（玩家手动切换快捷栏时同步流转槽位）
     * - 检测武器槽位物品变化
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        // 处理按键输入
        while (EFDCKeyBindings.NEXT_WEAPON.consumeClick()) {
            switchWeapon(player, true);
        }
        while (EFDCKeyBindings.PREVIOUS_WEAPON.consumeClick()) {
            switchWeapon(player, false);
        }

        // 检测快捷栏切换同步 + 武器物品变化
        detectChanges(player);
    }

    /**
     * 执行闭环武器切换
     * 根据方向（下一/上一）切换流转槽位，并更新快捷栏选中位置。
     */
    private static void switchWeapon(LocalPlayer player, boolean next) {
        IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);
        if (cap == null) return;

        if (next) {
            cap.switchToNextWeapon();
        } else {
            cap.switchToPreviousWeapon();
        }

        int hotbarSlot = cap.getWeaponSlotIndex(cap.getCurrentActiveSlot());
        player.getInventory().selected = hotbarSlot;

        // 更新物品快照
        updateStackSnapshot(player, cap);

        EFDCMod.LOGGER.debug("Switched to flow slot {} (hotbar {})",
                cap.getCurrentActiveSlot(), hotbarSlot);
    }

    /**
     * 检测物品栏变化
     * 1. 玩家手动切换快捷栏格时，同步更新流转激活槽位
     * 2. 武器槽位中物品发生变化时，输出日志
     */
    private static void detectChanges(LocalPlayer player) {
        IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);
        if (cap == null) return;

        // 1) 检测玩家手动切换快捷栏
        int currentHotbar = player.getInventory().selected;
        for (int i = 0; i < IWeaponFlowCapability.WEAPON_SLOT_COUNT; i++) {
            if (cap.getWeaponSlotIndex(i) == currentHotbar && i != cap.getCurrentActiveSlot()) {
                cap.setCurrentActiveSlot(i);
                EFDCMod.LOGGER.debug("Hotbar changed externally, synced to flow slot {}", i);
                break;
            }
        }

        // 2) 检测武器槽位中物品变化
        for (int i = 0; i < IWeaponFlowCapability.WEAPON_SLOT_COUNT; i++) {
            int hotbarSlot = cap.getWeaponSlotIndex(i);
            ItemStack current = player.getInventory().getItem(hotbarSlot);
            ItemStack previous = prevWeaponStacks[i];

            if (!ItemStack.isSameItem(current, previous)) {
                onWeaponSlotItemChanged(player, cap, i, previous, current);
            }
        }

        // 更新物品快照
        updateStackSnapshot(player, cap);
    }

    /**
     * 武器槽位物品发生变化时的回调
     */
    private static void onWeaponSlotItemChanged(LocalPlayer player, IWeaponFlowCapability cap,
                                                 int flowSlot, ItemStack oldStack, ItemStack newStack) {
        EFDCMod.LOGGER.debug("Weapon slot {} item changed: {} -> {}",
                flowSlot,
                oldStack.isEmpty() ? "empty" : oldStack.getItem(),
                newStack.isEmpty() ? "empty" : newStack.getItem());

        // TODO: 可在此处添加物品变化后的额外逻辑（如重置冷却等）
    }

    /**
     * 更新各武器槽位的物品快照
     */
    private static void updateStackSnapshot(LocalPlayer player, IWeaponFlowCapability cap) {
        for (int i = 0; i < IWeaponFlowCapability.WEAPON_SLOT_COUNT; i++) {
            int hotbarSlot = cap.getWeaponSlotIndex(i);
            prevWeaponStacks[i] = player.getInventory().getItem(hotbarSlot).copy();
        }
    }
}

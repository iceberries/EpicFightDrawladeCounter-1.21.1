package com.ice_berry.drawlade_counter.client.input;

import com.ice_berry.drawlade_counter.EFDCMod;
import com.ice_berry.drawlade_counter.capability.IWeaponFlowCapability;
import com.ice_berry.drawlade_counter.capability.WeaponFlowProvider;
import com.ice_berry.drawlade_counter.network.packets.WeaponSwitchPacket;
import com.ice_berry.drawlade_counter.until.epicfight.EFSkillIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 武器切换输入处理器
 * 监听键盘输入和客户端 tick 事件：
 * 1. 处理模组按键输入（下一武器 / 上一武器），实现闭环武器流转
 * 2. 检测快捷栏外部切换（玩家手动切换快捷栏时同步流转槽位）
 * 3. 检测武器槽位物品变化
 */
public class WeaponSwitchHandler {

    /** 上一 tick 各武器槽位的物品快照，用于检测物品栏变化 */
    private static final ItemStack[] prevWeaponStacks = new ItemStack[IWeaponFlowCapability.WEAPON_SLOT_COUNT];

    static {
        for (int i = 0; i < prevWeaponStacks.length; i++) {
            prevWeaponStacks[i] = ItemStack.EMPTY;
        }
    }

    private WeaponSwitchHandler() {}

    // ==================== 键盘输入处理 ====================

    /**
     * 处理键盘输入事件
     * 按下切换键时立即更新本地快捷栏（视觉即时反馈），并发送 C2S 包到服务端。
     */
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 动画播放中禁止使用流转键切换武器
        if (EFSkillIntegration.isPlayerInAttackAnimation(mc.player)) {
            EFDCKeyBindings.PREVIOUS_WEAPON.consumeClick();
            EFDCKeyBindings.NEXT_WEAPON.consumeClick();
            return;
        }

        if (EFDCKeyBindings.PREVIOUS_WEAPON.consumeClick()) {
            switchWeapon(mc.player, false);
        }
        if (EFDCKeyBindings.NEXT_WEAPON.consumeClick()) {
            switchWeapon(mc.player, true);
        }
    }

    // ==================== 客户端 Tick 处理 ====================

    /**
     * 客户端 tick 事件处理
     * - EF 动画播放中强制恢复快捷栏选中位，禁止切换武器
     * - 检测玩家手动切换快捷栏时同步流转激活槽位
     * - 检测武器槽位物品变化
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);

        // 动画播放中强制恢复快捷栏选中位，禁止任何形式的武器切换
        if (EFSkillIntegration.isPlayerInAttackAnimation(player) && cap != null) {
            int expectedHotbar = cap.getWeaponSlotIndex(cap.getCurrentActiveSlot());
            if (player.getInventory().selected != expectedHotbar) {
                player.getInventory().selected = expectedHotbar;
                EFDCMod.LOGGER.debug("Animation playing, forced hotbar back to slot {}", expectedHotbar);
            }
            return;
        }

        if (cap == null) return;

        // 1) 检测玩家手动切换快捷栏（通过数字键/滚轮等）
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
                EFDCMod.LOGGER.debug("Weapon slot {} item changed: {} -> {}",
                        i,
                        previous.isEmpty() ? "empty" : previous.getItem(),
                        current.isEmpty() ? "empty" : current.getItem());
            }
        }

        // 更新物品快照
        updateStackSnapshot(player, cap);
    }

    // ==================== 内部方法 ====================

    /**
     * 执行闭环武器切换
     * 根据方向（下一/上一）切换流转槽位，更新快捷栏选中位置，
     * 并发送 C2S 包通知服务端。
     */
    private static void switchWeapon(LocalPlayer player, boolean next) {
        IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);
        if (cap == null) return;

        // 在切换前记录旧槽位，确保服务端能正确计算新旧槽位
        int oldSlot = cap.getCurrentActiveSlot();
        int direction = next ? 1 : -1;

        if (next) {
            cap.switchToNextWeapon();
        } else {
            cap.switchToPreviousWeapon();
        }

        int hotbarSlot = cap.getWeaponSlotIndex(cap.getCurrentActiveSlot());
        player.getInventory().selected = hotbarSlot;

        // 更新物品快照
        updateStackSnapshot(player, cap);

        // 通知服务端（携带旧槽位，避免集成服务器下 capability 共享导致错位）
        PacketDistributor.sendToServer(new WeaponSwitchPacket(oldSlot, direction));

        EFDCMod.LOGGER.debug("Switched to flow slot {} (hotbar {}), oldSlot={}",
                cap.getCurrentActiveSlot(), hotbarSlot, oldSlot);
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

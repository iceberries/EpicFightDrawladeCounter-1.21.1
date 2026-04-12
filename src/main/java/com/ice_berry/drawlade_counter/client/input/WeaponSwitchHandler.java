package com.ice_berry.drawlade_counter.client.input;

import com.ice_berry.drawlade_counter.capability.IWeaponFlowCapability;
import com.ice_berry.drawlade_counter.capability.WeaponFlowProvider;
import com.ice_berry.drawlade_counter.config.Config;
import com.ice_berry.drawlade_counter.network.packets.PerfectGuardTriggerPacket;
import com.ice_berry.drawlade_counter.network.packets.WeaponSwitchPacket;
import com.ice_berry.drawlade_counter.until.epicfight.EFSkillIntegration;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * 武器切换输入处理器
 * 监听键盘输入和客户端 tick 事件，处理武器流转和完美格挡触发
 */
public class WeaponSwitchHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 上一 tick 各武器槽位的物品快照 */
    private static final ItemStack[] prevWeaponStacks = new ItemStack[IWeaponFlowCapability.WEAPON_SLOT_COUNT];

    /** 最后一次格挡命中时间戳（毫秒），0 表示无记录 */
    private static long lastGuardHitTime = 0;

    static {
        for (int i = 0; i < prevWeaponStacks.length; i++) {
            prevWeaponStacks[i] = ItemStack.EMPTY;
        }
    }

    private WeaponSwitchHandler() {}

    /**
     * 检查当前是否处于完美格挡判定窗口内
     *
     * @return true 表示在窗口内，准星应显示红色指示
     */
    public static boolean isInPerfectGuardWindow() {
        if (lastGuardHitTime <= 0 || !Config.PERFECT_GUARD_ENABLED.get()) return false;
        long elapsed = System.currentTimeMillis() - lastGuardHitTime;
        long windowMs = (long) (Config.PERFECT_GUARD_WINDOW.get() * 1000.0);
        return elapsed <= windowMs;
    }

    /**
     * 服务端通知客户端格挡命中，开启完美格挡判定窗口
     */
    public static void onGuardHit() {
        lastGuardHitTime = System.currentTimeMillis();
    }

    // #region 键盘输入处理

    /**
     * 处理键盘输入事件
     * 在完美格挡判定窗口内按键触发弹反，否则执行武器切换
     */
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean prevConsumed = EFDCKeyBindings.PREVIOUS_WEAPON.consumeClick();
        boolean nextConsumed = EFDCKeyBindings.NEXT_WEAPON.consumeClick();
        if (!prevConsumed && !nextConsumed) return;

        LOGGER.debug("[EFDC] onKeyInput: 流转键被按下, lastGuardHitTime={}, enabled={}",
                lastGuardHitTime, Config.PERFECT_GUARD_ENABLED.get());

        // 检查是否在完美格挡判定窗口内
        if (lastGuardHitTime > 0 && Config.PERFECT_GUARD_ENABLED.get()) {
            long elapsed = System.currentTimeMillis() - lastGuardHitTime;
            long windowMs = (long) (Config.PERFECT_GUARD_WINDOW.get() * 1000.0);

            if (elapsed <= windowMs) {
                LOGGER.debug("[EFDC] 在弹反窗口内({}ms/{}ms)，发送 PerfectGuardTriggerPacket",
                        elapsed, windowMs);
                // 在窗口内 → 触发完美格挡，取消武器切换
                lastGuardHitTime = 0;
                PacketDistributor.sendToServer(new PerfectGuardTriggerPacket());
                return;
            }
            LOGGER.debug("[EFDC] 弹反窗口已超时({}ms > {}ms)，执行武器切换",
                    elapsed, windowMs);
            // 超出窗口，清除记录
            lastGuardHitTime = 0;
        }

        // 不在完美格挡窗口 → 动画锁定时禁止切换
        if (EFSkillIntegration.isPlayerInAttackAnimation(mc.player)) {
            return;
        }

        if (prevConsumed) switchWeapon(mc.player, false);
        if (nextConsumed) switchWeapon(mc.player, true);
    }

    // #endregion

    // #region 客户端 Tick 处理

    /**
     * 客户端 tick 事件处理
     * 检测锁定目标攻击起手、强制恢复快捷栏、同步流转槽位、检测物品变化
     */
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null) return;

        IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);

        // 动画播放中强制恢复快捷栏选中位
        if (EFSkillIntegration.isPlayerInAttackAnimation(player) && cap != null) {
            int expectedHotbar = cap.getWeaponSlotIndex(cap.getCurrentActiveSlot());
            if (player.getInventory().selected != expectedHotbar) {
                player.getInventory().selected = expectedHotbar;
            }
            return;
        }

        if (cap == null) return;

        // 检测锁定目标的攻击起手（弹反窗口）
        if (Config.PERFECT_GUARD_ENABLED.get()) {
            // 清理过期窗口，重置时间戳以便重新检测
            if (lastGuardHitTime > 0) {
                long elapsed = System.currentTimeMillis() - lastGuardHitTime;
                long windowMs = (long) (Config.PERFECT_GUARD_WINDOW.get() * 1000.0);
                if (elapsed > windowMs) {
                    LOGGER.debug("[EFDC] 准星窗口过期，重置 lastGuardHitTime");
                    lastGuardHitTime = 0;
                }
            }

            // 窗口过期后检测锁定目标是否开始攻击
            if (lastGuardHitTime <= 0) {
                LivingEntity target = EFSkillIntegration.getLockOnTarget(player);
                LOGGER.debug("[EFDC] 弹反检测: lockOnTarget={}, isAlive={}",
                        target, target != null && target.isAlive());
                if (target != null && target.isAlive()) {
                    boolean attacking = EFSkillIntegration.isEntityAttacking(target);
                    LOGGER.debug("[EFDC] 弹反检测: target={}, attacking={}",
                            target.getName().getString(), attacking);
                    if (attacking) {
                        lastGuardHitTime = System.currentTimeMillis();
                        LOGGER.debug("[EFDC] 弹反窗口已开启，准星应变红");
                    }
                }
            } else {
                LOGGER.debug("[EFDC] 弹反窗口仍然有效，跳过检测");
            }
        } else {
            LOGGER.debug("[EFDC] PERFECT_GUARD_ENABLED=false，跳过弹反检测");
        }

        // 检测玩家手动切换快捷栏，同步流转激活槽位
        int currentHotbar = player.getInventory().selected;
        for (int i = 0; i < IWeaponFlowCapability.WEAPON_SLOT_COUNT; i++) {
            if (cap.getWeaponSlotIndex(i) == currentHotbar && i != cap.getCurrentActiveSlot()) {
                cap.setCurrentActiveSlot(i);
                break;
            }
        }

        // 更新物品快照
        updateStackSnapshot(player, cap);
    }

    // #endregion

    // #region 内部方法

    /**
     * 执行闭环武器切换并发送 C2S 包通知服务端
     */
    private static void switchWeapon(LocalPlayer player, boolean next) {
        IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);
        if (cap == null) return;

        int oldSlot = cap.getCurrentActiveSlot();
        if (next) {
            cap.switchToNextWeapon();
        } else {
            cap.switchToPreviousWeapon();
        }

        int hotbarSlot = cap.getWeaponSlotIndex(cap.getCurrentActiveSlot());
        player.getInventory().selected = hotbarSlot;
        updateStackSnapshot(player, cap);
        PacketDistributor.sendToServer(new WeaponSwitchPacket(oldSlot, next ? 1 : -1));
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

    // #endregion
}

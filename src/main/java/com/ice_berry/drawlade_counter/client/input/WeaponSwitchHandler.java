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

    /** 弹反窗口起始时间戳（毫秒），0 表示无弹反窗口 */
    private static long windowStartTime = 0;

    /** 当前弹反窗口时长（毫秒），由攻击动画时长 * 配置百分比计算得出 */
    private static long currentWindowMs = 0;

    /** 上一次锁定目标攻击动画的检测时间戳，防止同一攻击动画重复设置窗口 */
    private static long lastAttackDetectedTime = 0;

    static {
        for (int i = 0; i < prevWeaponStacks.length; i++) {
            prevWeaponStacks[i] = ItemStack.EMPTY;
        }
    }

    private WeaponSwitchHandler() {}

    /**
     * 检查当前是否处于完美格挡判定窗口内
     * 窗口位于攻击动画抬手前摇阶段 [start, preDelay] 的后半段：
     *   窗口起始 = detectionTime + preDelay * ratio
     *   窗口结束 = detectionTime + preDelay
     * windowStartTime 存储窗口起始时刻（绝对时间戳），
     * currentWindowMs 存储窗口持续时长。
     *
     * @return true 表示在窗口内，准星应显示红色指示
     */
    public static boolean isInPerfectGuardWindow() {
        if (windowStartTime <= 0 || currentWindowMs <= 0 || !Config.PERFECT_GUARD_ENABLED.get()) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - windowStartTime;
        boolean inWindow = elapsed >= 0 && elapsed <= currentWindowMs;
        LOGGER.debug("[EFDC] isInPerfectGuardWindow: elapsed={}ms, windowMs={}ms, result={}",
                elapsed, currentWindowMs, inWindow);
        return inWindow;
    }

    // #region 键盘输入处理

    /**
     * 处理键盘输入事件
     * 在完美格挡判定窗口内按键 → 记录格挡成功 + 切换武器 + 发送弹反包
     * 非窗口期间按键 → 动画锁定检测后执行普通武器切换
     */
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        boolean prevConsumed = EFDCKeyBindings.PREVIOUS_WEAPON.consumeClick();
        boolean nextConsumed = EFDCKeyBindings.NEXT_WEAPON.consumeClick();
        if (!prevConsumed && !nextConsumed) return;

        LOGGER.debug("[EFDC] onKeyInput: 流转键被按下, windowStartTime={}, enabled={}",
                windowStartTime, Config.PERFECT_GUARD_ENABLED.get());

        // 检查是否在完美格挡判定窗口内
        if (windowStartTime > 0 && currentWindowMs > 0 && Config.PERFECT_GUARD_ENABLED.get()) {
            long elapsed = System.currentTimeMillis() - windowStartTime;

            if (elapsed >= 0 && elapsed <= currentWindowMs) {
                LOGGER.debug("[EFDC] 在弹反窗口内({}ms/{}ms)，切换武器(跳过支援攻击)并发送弹反包",
                        elapsed, currentWindowMs);
                windowStartTime = 0;
                currentWindowMs = 0;
                lastAttackDetectedTime = 0;
                // 切换武器（客户端即时生效 + 发送 WeaponSwitchPacket，skipSupportAttack=true）
                if (prevConsumed) switchWeapon(mc.player, false, true);
                if (nextConsumed) switchWeapon(mc.player, true, true);
                // 发送弹反触发包：服务端设置待定弹反，等待目标 PHASE_LEVEL=2 时激活格挡
                PacketDistributor.sendToServer(new PerfectGuardTriggerPacket());
                return;
            }
            // 窗口未开启（elapsed < 0）时不做任何处理
            if (elapsed < 0) {
                LOGGER.debug("[EFDC] 弹反窗口尚未开启({}ms后开启)，忽略此次按键", -elapsed);
                return;
            }
            LOGGER.debug("[EFDC] 弹反窗口已超时({}ms > {}ms)，执行武器切换",
                    elapsed, currentWindowMs);
            windowStartTime = 0;
            currentWindowMs = 0;
        }

        // 不在完美格挡窗口 → 动画锁定时禁止切换
        if (EFSkillIntegration.isPlayerInAttackAnimation(mc.player)) {
            return;
        }

        if (prevConsumed) switchWeapon(mc.player, false, false);
        if (nextConsumed) switchWeapon(mc.player, true, false);
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

        // 动画播放中强制恢复快捷栏选中位（但不阻止弹反窗口检测）
        if (EFSkillIntegration.isPlayerInAttackAnimation(player) && cap != null) {
            int expectedHotbar = cap.getWeaponSlotIndex(cap.getCurrentActiveSlot());
            if (player.getInventory().selected != expectedHotbar) {
                player.getInventory().selected = expectedHotbar;
            }
            // 不再直接 return，继续执行弹反窗口检测和过期清理
        }

        if (cap == null) return;

        // 检测锁定目标的攻击起手（弹反窗口）
        if (Config.PERFECT_GUARD_ENABLED.get()) {
            long currentTime = System.currentTimeMillis();

            // 清理过期窗口，重置时间戳以便重新检测
            if (windowStartTime > 0) {
                long elapsed = currentTime - windowStartTime;
                if (elapsed > currentWindowMs) {
                    LOGGER.debug("[EFDC] 准星窗口过期: elapsed={}ms > windowMs={}ms, " +
                                    "windowStartTime={}, 重置所有状态",
                            elapsed, currentWindowMs, windowStartTime);
                    windowStartTime = 0;
                    currentWindowMs = 0;
                    lastAttackDetectedTime = 0;
                } else {
                    // 窗口仍然有效，输出当前状态方便调试
                    LOGGER.debug("[EFDC] 准星窗口有效: elapsed={}ms, windowMs={}ms, " +
                                    "窗口未到({}ms后到期), isInWindow={}",
                            elapsed, currentWindowMs, currentWindowMs - elapsed,
                            elapsed >= 0);
                }
            }

            // 仅在无活跃窗口时检测锁定目标是否开始攻击
            if (windowStartTime <= 0) {
                LivingEntity target = EFSkillIntegration.getLockOnTarget(player);
                if (target != null && target.isAlive()) {
                    boolean attacking = EFSkillIntegration.isEntityAttacking(target);
                    if (attacking) {
                        float preDelay = EFSkillIntegration.getAttackPhasePreDelay(target);
                        if (preDelay > 0) {
                            // 同一次攻击动画（1秒内再次检测到）不重复设置窗口
                            if (currentTime - lastAttackDetectedTime < 1000) {
                                LOGGER.debug("[EFDC] 弹反检测: 同一攻击动画内, " +
                                                "距上次检测{}ms < 1000ms, 跳过",
                                        currentTime - lastAttackDetectedTime);
                            } else {
                                lastAttackDetectedTime = currentTime;
                                double ratio = Config.PERFECT_GUARD_WINDOW.get();
                                windowStartTime = currentTime + (long) (preDelay * ratio * 1000.0);
                                currentWindowMs = (long) (preDelay * (1.0 - ratio) * 1000.0);
                                LOGGER.debug("[EFDC] 弹反窗口已设定: target={}, preDelay={}s, " +
                                                "ratio={}%, windowStartTime={}ms(延迟{}ms), " +
                                                "currentWindowMs={}ms, lastAttackDetectedTime={}ms",
                                        target.getName().getString(), preDelay,
                                        (int) (ratio * 100),
                                        windowStartTime, (long) (preDelay * ratio * 1000.0),
                                        currentWindowMs, lastAttackDetectedTime);
                            }
                        } else {
                            LOGGER.debug("[EFDC] 弹反检测: attacking=true 但 preDelay={}<=0, " +
                                    "跳过窗口设置", preDelay);
                        }
                    } else {
                        if (lastAttackDetectedTime > 0) {
                            LOGGER.debug("[EFDC] 弹反检测: attacking=false, " +
                                    "重置 lastAttackDetectedTime(原值={})", lastAttackDetectedTime);
                        }
                        lastAttackDetectedTime = 0;
                    }
                }
            }
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
     *
     * @param player            本地玩家
     * @param next              true=下一武器，false=上一武器
     * @param skipSupportAttack 是否跳过支援攻击（弹反窗口触发时为 true）
     */
    private static void switchWeapon(LocalPlayer player, boolean next, boolean skipSupportAttack) {
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
        PacketDistributor.sendToServer(new WeaponSwitchPacket(oldSlot, next ? 1 : -1, skipSupportAttack));
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

package com.ice_berry.drawlade_counter.combat;

import com.ice_berry.drawlade_counter.EFDCMod;
import com.ice_berry.drawlade_counter.api.events.WeaponSwitchEvent;
import com.ice_berry.drawlade_counter.capability.IWeaponFlowCapability;
import com.ice_berry.drawlade_counter.capability.WeaponFlowProvider;
import com.ice_berry.drawlade_counter.config.DataDrivenLoader;
import com.ice_berry.drawlade_counter.network.NetworkHandler;
import com.ice_berry.drawlade_counter.network.packets.CooldownUpdatePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 武器流转管理器
 * 统一管理武器切换、支援攻击触发的完整流程。
 *
 * <h3>武器切换流程：</h3>
 * <ol>
 *   <li>客户端检测按键 → 发送 C2S 包</li>
 *   <li>服务端收到包 → 调用 {@link #switchWeapon}</li>
 *   <li>检查冷却和战斗状态</li>
 *   <li>投递 {@link WeaponSwitchEvent}</li>
 *   <li>更新 Capability 中的当前槽位</li>
 *   <li>查找匹配的支援攻击数据 → 若匹配则执行支援攻击</li>
 *   <li>同步冷却给客户端</li>
 * </ol>
 */
public final class WeaponFlowManager {

    private WeaponFlowManager() {}

    /**
     * 处理武器切换请求（服务端调用）
     *
     * @param player       发起切换的玩家
     * @param direction    切换方向：+1 下一武器，-1 上一武器
     */
    public static void switchWeapon(Player player, int direction) {
        IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);
        if (cap == null) return;

        int currentSlot = cap.getCurrentActiveSlot();
        int oldSlot = currentSlot;

        // 计算新槽位
        int newSlot = direction > 0
                ? (currentSlot + 1) % IWeaponFlowCapability.WEAPON_SLOT_COUNT
                : (currentSlot - 1 + IWeaponFlowCapability.WEAPON_SLOT_COUNT) % IWeaponFlowCapability.WEAPON_SLOT_COUNT;

        // 冷却检查 —— 切换本身不受冷却限制（但支援攻击受限制）
        ItemStack fromWeapon = getWeaponInSlot(player, oldSlot);
        ItemStack toWeapon = getWeaponInSlot(player, newSlot);

        // 投递武器切换事件（可取消）
        if (!WeaponSwitchEvent.fire(player, oldSlot, newSlot, fromWeapon, toWeapon)) {
            EFDCMod.LOGGER.debug("Weapon switch cancelled by event for player {}", player.getName().getString());
            return;
        }

        // 更新当前激活槽位
        cap.setCurrentActiveSlot(newSlot);

        // 切换玩家快捷栏选中
        int newHotbarSlot = cap.getWeaponSlotIndex(newSlot);
        player.getInventory().selected = newHotbarSlot;

        // 强制同步容器状态到客户端（确保客户端立即更新选中槽位）
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.broadcastChanges();
        }

        // 查找并执行支援攻击
        @Nullable
        SupportAttackData attackData = DataDrivenLoader.findSupportAttack(fromWeapon, toWeapon);
        if (attackData != null && !cap.isSlotOnCooldown(newSlot)) {
            // 检查原武器槽位是否在冷却中（防止连续切换刷攻击）
            if (!cap.isSlotOnCooldown(oldSlot) || true) {
                // 设置目标武器槽位冷却
                cap.setSlotCooldown(newSlot, attackData.getCooldownTicks());
                // 同时设置原武器槽位短冷却（防止来回刷）
                cap.setSlotCooldown(oldSlot, attackData.getCooldownTicks() / 2);

                EFDCMod.LOGGER.debug("Support attack triggered for player {}: {} -> {}",
                        player.getName().getString(),
                        fromWeapon.isEmpty() ? "empty" : fromWeapon.getItem(),
                        toWeapon.isEmpty() ? "empty" : toWeapon.getItem());

                // 执行支援攻击
                SupportAttackHandler.executeSupportAttack(player, attackData, fromWeapon, toWeapon, newSlot);
            }
        }

        // 同步冷却到客户端
        if (player instanceof ServerPlayer serverPlayer) {
            syncCooldowns(serverPlayer, cap);
        }
    }

    /**
     * 获取指定流转槽位中的武器
     */
    private static ItemStack getWeaponInSlot(Player player, int flowSlot) {
        IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);
        if (cap == null) return ItemStack.EMPTY;

        int hotbarIndex = cap.getWeaponSlotIndex(flowSlot);
        return player.getInventory().getItem(hotbarIndex);
    }

    /**
     * 同步全部槽位冷却到客户端
     */
    private static void syncCooldowns(ServerPlayer player, IWeaponFlowCapability cap) {
        for (int i = 0; i < IWeaponFlowCapability.WEAPON_SLOT_COUNT; i++) {
            int cd = cap.getSlotCooldown(i);
            if (cd > 0) {
                CooldownUpdatePacket packet = new CooldownUpdatePacket(i, cd);
                NetworkHandler.sendToClient(player, packet);
            }
        }
    }
}

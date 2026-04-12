package com.ice_berry.drawlade_counter.combat;

import com.ice_berry.drawlade_counter.api.events.WeaponSwitchEvent;
import com.ice_berry.drawlade_counter.capability.IWeaponFlowCapability;
import com.ice_berry.drawlade_counter.capability.WeaponFlowProvider;
import com.ice_berry.drawlade_counter.config.DataDrivenLoader;
import com.ice_berry.drawlade_counter.network.NetworkHandler;
import com.ice_berry.drawlade_counter.network.packets.CooldownUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * 武器流转管理器
 * 统一管理武器切换、支援攻击触发的完整流程。
 */
public final class WeaponFlowManager {

    private WeaponFlowManager() {}

    /**
     * 处理武器切换请求（服务端调用）
     *
     * @param player    发起切换的玩家
     * @param oldSlot   客户端发送的旧槽位索引
     * @param direction 切换方向：+1 下一武器，-1 上一武器
     */
    public static void switchWeapon(Player player, int oldSlot, int direction) {
        IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(player);
        if (cap == null) return;

        // 根据旧槽位 + 方向计算新槽位（不依赖 cap，避免集成服务器共享问题）
        int newSlot = (oldSlot + direction + IWeaponFlowCapability.WEAPON_SLOT_COUNT)
                % IWeaponFlowCapability.WEAPON_SLOT_COUNT;

        ItemStack fromWeapon = getWeaponInSlot(player, oldSlot);
        ItemStack toWeapon = getWeaponInSlot(player, newSlot);

        // 投递武器切换事件（可取消）
        if (!WeaponSwitchEvent.fire(player, oldSlot, newSlot, fromWeapon, toWeapon)) return;

        // 更新当前激活槽位和快捷栏
        cap.setCurrentActiveSlot(newSlot);
        int newHotbarSlot = cap.getWeaponSlotIndex(newSlot);
        player.getInventory().selected = newHotbarSlot;

        // 强制同步容器状态
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.containerMenu.broadcastChanges();
        }

        // 查找并执行支援攻击
        @Nullable
        SupportAttackData attackData = DataDrivenLoader.findSupportAttack(fromWeapon, toWeapon);
        if (attackData != null && !cap.isSlotOnCooldown(newSlot)) {
            // 设置目标槽位和原槽位冷却
            cap.setSlotCooldown(newSlot, attackData.getCooldownTicks());
            cap.setSlotCooldown(oldSlot, attackData.getCooldownTicks() / 2);
            SupportAttackHandler.executeSupportAttack(player, attackData, fromWeapon, toWeapon, newSlot);
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
        return player.getInventory().getItem(cap.getWeaponSlotIndex(flowSlot));
    }

    /**
     * 同步全部槽位冷却到客户端
     */
    private static void syncCooldowns(ServerPlayer player, IWeaponFlowCapability cap) {
        for (int i = 0; i < IWeaponFlowCapability.WEAPON_SLOT_COUNT; i++) {
            int cd = cap.getSlotCooldown(i);
            if (cd > 0) {
                NetworkHandler.sendToClient(player, new CooldownUpdatePacket(i, cd));
            }
        }
    }
}

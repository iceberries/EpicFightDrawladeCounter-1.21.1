package com.ice_berry.drawlade_counter.client.gui;

import com.ice_berry.drawlade_counter.capability.IWeaponFlowCapability;
import com.ice_berry.drawlade_counter.capability.WeaponFlowProvider;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 武器流转 HUD
 * 在快捷栏上方渲染 4 格武器流转状态，显示物品图标、激活高亮和冷却进度。
 *
 * 通过 NeoForge RegisterGuiLayersEvent 注册为 GUI 层，
 * 渲染在原版快捷栏之上。
 *
 * 布局：
 *     [1] [2] [3] [4]    <-- 武器流转 HUD（此类渲染）
 *     [1] [2] [3] ... [9] <-- 原版快捷栏
 */
public final class WeaponFlowHud implements LayeredDraw.Layer {

    public static final ResourceLocation NAME =
            ResourceLocation.fromNamespaceAndPath("epicfightdrawladecounter", "weapon_flow_hud");

    private static final int SLOT_GAP = 2;
    private static final int HOTBAR_GAP = 4;

    private WeaponFlowHud() {}

    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, NAME, new WeaponFlowHud());
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return;

        IWeaponFlowCapability cap = WeaponFlowProvider.getCapability(mc.player);
        if (cap == null) return;

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int slotCount = IWeaponFlowCapability.WEAPON_SLOT_COUNT;
        int slotSize = WeaponSlotWidget.SLOT_SIZE;

        int totalWidth = slotCount * slotSize + (slotCount - 1) * SLOT_GAP;
        int startX = (screenWidth - totalWidth) / 2;

        int hotbarTopY = screenHeight - 27;
        int startY = hotbarTopY - slotSize - HOTBAR_GAP;

        for (int i = 0; i < slotCount; i++) {
            int x = startX + i * (slotSize + SLOT_GAP);
            int hotbarIndex = cap.getWeaponSlotIndex(i);
            var item = mc.player.getInventory().getItem(hotbarIndex);
            boolean active = (i == cap.getCurrentActiveSlot());
            boolean onCooldown = cap.isSlotOnCooldown(i);

            WeaponSlotWidget.renderSlot(graphics, x, startY, item, active, i + 1);

            if (onCooldown) {
                int remaining = cap.getSlotCooldown(i);
                int maxCd = cap.getSlotMaxCooldown(i);
                float progress = maxCd > 0 ? (float) remaining / maxCd : 0;
                CooldownIndicator.render(graphics, x, startY, remaining, progress);
            }
        }
    }
}

package com.ice_berry.drawlade_counter.client.gui;

import com.ice_berry.drawlade_counter.client.input.WeaponSwitchHandler;
import com.mojang.logging.LogUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.slf4j.Logger;

/**
 * 完美格挡十字准星叠加层
 * 在完美格挡判定窗口内将准星渲染为红色，提示玩家可以触发弹反。
 */
public final class PerfectGuardCrosshairOverlay implements LayeredDraw.Layer {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation NAME =
            ResourceLocation.fromNamespaceAndPath("epicfightdrawladecounter", "perfect_guard_crosshair");

    /** 准星线条粗细 */
    private static final int THICKNESS = 2;

    /** 准星臂长 */
    private static final int ARM_LENGTH = 9;

    /** 准星中心间隙 */
    private static final int GAP = 4;

    /** 红色准星颜色 (ABGR) */
    private static final int RED_COLOR = 0xFF0000FF;

    private PerfectGuardCrosshairOverlay() {}

    /**
     * 注册十字准星叠加层，渲染在原版准星之上
     */
    public static void register(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, NAME, new PerfectGuardCrosshairOverlay());
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return;

        boolean inWindow = WeaponSwitchHandler.isInPerfectGuardWindow();
        if (!inWindow) return;

        LOGGER.debug("[EFDC] 渲染红色准星（弹反窗口内）");

        // 获取窗口中心作为准星位置
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        int cx = screenWidth / 2;
        int cy = screenHeight / 2;

        drawCrosshair(graphics, cx, cy);
    }

    /**
     * 绘制红色十字准星（4 条臂 + 中心点）
     *
     * @param graphics GUI 图形上下文
     * @param cx       中心 X 坐标
     * @param cy       中心 Y 坐标
     */
    private static void drawCrosshair(GuiGraphics graphics, int cx, int cy) {
        int halfT = THICKNESS / 2;
        int halfGap = GAP / 2;

        // 上臂
        graphics.fill(cx - halfT, cy - halfGap - ARM_LENGTH, cx + halfT, cy - halfGap, RED_COLOR);
        // 下臂
        graphics.fill(cx - halfT, cy + halfGap, cx + halfT, cy + halfGap + ARM_LENGTH, RED_COLOR);
        // 左臂
        graphics.fill(cx - halfGap - ARM_LENGTH, cy - halfT, cx - halfGap, cy + halfT, RED_COLOR);
        // 右臂
        graphics.fill(cx + halfGap, cy - halfT, cx + halfGap + ARM_LENGTH, cy + halfT, RED_COLOR);
        // 中心点
        graphics.fill(cx - halfT, cy - halfT, cx + halfT, cy + halfT, RED_COLOR);
    }
}

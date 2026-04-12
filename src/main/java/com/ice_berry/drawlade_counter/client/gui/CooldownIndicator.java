package com.ice_berry.drawlade_counter.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 冷却指示器渲染
 * 在武器槽位上叠加冷却遮罩，采用透明度渐变动画。
 * 剩余秒数以白色文字显示在槽位中央。
 */
public final class CooldownIndicator {

    /** 冷却时间文字颜色 */
    private static final int TIME_TEXT_COLOR = 0xFFFFFFFF;

    /** 冷却时间文字阴影 */
    private static final int TIME_SHADOW_COLOR = 0xFF000000;

    private CooldownIndicator() {}

    /**
     * 渲染冷却指示器
     *
     * @param graphics  GuiGraphics 上下文
     * @param x         槽位左上角 X
     * @param y         槽位左上角 Y
     * @param remaining 剩余冷却 tick
     * @param progress  冷却进度 (0.0 ~ 1.0)，1.0 = 刚开始冷却，0.0 = 冷却结束
     */
    public static void render(GuiGraphics graphics, int x, int y,
                               int remaining, float progress) {
        if (progress <= 0.0F || remaining <= 0) return;

        int size = WeaponSlotWidget.SLOT_SIZE;

        // 1) 绘制冷却遮罩（透明度随 progress 线性变化）
        //    progress=1.0 → alpha=160 (深色)，progress=0.0 → alpha=0 (透明)
        int alpha = (int) (progress * 160);
        int overlayColor = (alpha << 24) | 0x000088;
        graphics.fill(x, y, x + size, y + size, overlayColor);

        // 2) 在槽位中央显示剩余秒数（仅当 >= 1秒时显示）
        float seconds = remaining / 20.0F;
        if (seconds >= 1.0F) {
            String timeStr = String.valueOf((int) Math.ceil(seconds));
            Minecraft mc = Minecraft.getInstance();
            int textWidth = mc.font.width(timeStr);
            int textX = x + (size - textWidth) / 2;
            int textY = y + (size - mc.font.lineHeight) / 2 + 1;
            // 阴影
            graphics.drawString(mc.font, timeStr, textX + 1, textY + 1, TIME_SHADOW_COLOR, false);
            // 文字
            graphics.drawString(mc.font, timeStr, textX, textY, TIME_TEXT_COLOR, true);
        }
    }
}

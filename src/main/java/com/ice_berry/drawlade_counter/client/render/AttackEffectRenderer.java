package com.ice_berry.drawlade_counter.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 攻击特效渲染器（阶段二占位）
 * 未来将实现：
 * <ul>
 *   <li>武器切换时的视觉指示器</li>
 *   <li>支援攻击命中的 HUD 反馈</li>
 *   <li>连击计数显示</li>
 * </ul>
 */
public class AttackEffectRenderer {

    private static float attackFlashTimer = 0;
    private static final float FLASH_DURATION = 10.0F; // tick

    private AttackEffectRenderer() {}

    /**
     * 触发攻击闪光效果
     */
    public static void triggerAttackFlash() {
        attackFlashTimer = FLASH_DURATION;
    }

    /**
     * 每帧 tick 更新
     */
    public static void tick() {
        if (attackFlashTimer > 0) {
            attackFlashTimer--;
        }
    }

    /**
     * 渲染攻击闪光覆盖层
     * 在 HUD 层上叠加一个短暂的半透明红色闪光。
     */
    public static void render(GuiGraphics graphics) {
        if (attackFlashTimer <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        float alpha = (attackFlashTimer / FLASH_DURATION) * 0.15F;
        int width = mc.getWindow().getGuiScaledWidth();
        int height = mc.getWindow().getGuiScaledHeight();

        // 绘制半透明红色覆盖层
        graphics.fill(0, 0, width, height, ((int) (alpha * 255) << 24) | 0xFF0000);
    }

    public static boolean isFlashing() {
        return attackFlashTimer > 0;
    }
}

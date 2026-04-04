package com.ice_berry.drawlade_counter.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * 武器槽位渲染组件
 * 负责绘制单个武器流转槽位的视觉效果：背景、边框、物品图标、槽位编号、激活高亮。
 *
 * 尺寸与原版快捷栏槽位对齐（20x20 像素），物品图标居中偏移 2px。
 */
public final class WeaponSlotWidget {

    /** 标准槽位尺寸（像素），与原版 hotbar 一致 */
    public static final int SLOT_SIZE = 20;

    private static final int BG_NORMAL = 0x88000000;
    private static final int BG_ACTIVE = 0x99303030;
    private static final int BORDER_NORMAL = 0xA0606060;
    private static final int BORDER_ACTIVE = 0xFFE8C840;
    private static final int BORDER_ACTIVE_INNER = 0x60E8C840;
    private static final int LABEL_NORMAL = 0xFF909090;
    private static final int LABEL_ACTIVE = 0xFFFFFFFF;

    private WeaponSlotWidget() {}

    /**
     * 渲染单个武器槽位
     *
     * @param graphics    GuiGraphics 上下文
     * @param x           左上角 X 坐标
     * @param y           左上角 Y 坐标
     * @param item        该槽位对应的物品（可为 EMPTY）
     * @param active      是否为当前激活槽位
     * @param slotNumber  槽位编号（1-based，用于标签显示）
     */
    public static void renderSlot(GuiGraphics graphics, int x, int y,
                                   ItemStack item, boolean active, int slotNumber) {
        int size = SLOT_SIZE;

        // 1) 背景
        int bg = active ? BG_ACTIVE : BG_NORMAL;
        graphics.fill(x, y, x + size, y + size, bg);

        // 2) 物品图标（居中偏移 2px，与原版 hotbar 一致）
        if (!item.isEmpty()) {
            graphics.renderItem(item, x + 2, y + 2);
        }

        // 3) 边框
        int borderColor = active ? BORDER_ACTIVE : BORDER_NORMAL;
        graphics.fill(x, y, x + size, y + 1, borderColor);
        graphics.fill(x, y + size - 1, x + size, y + size, borderColor);
        graphics.fill(x, y, x + 1, y + size, borderColor);
        graphics.fill(x + size - 1, y, x + size, y + size, borderColor);

        // 4) 激活槽位额外高亮（内层细边框）
        if (active) {
            graphics.fill(x + 1, y + 1, x + size - 1, y + 2, BORDER_ACTIVE_INNER);
            graphics.fill(x + 1, y + size - 2, x + size - 1, y + size - 1, BORDER_ACTIVE_INNER);
            graphics.fill(x + 1, y + 1, x + 2, y + size - 1, BORDER_ACTIVE_INNER);
            graphics.fill(x + size - 2, y + 1, x + size - 1, y + size - 1, BORDER_ACTIVE_INNER);
        }

        // 5) 槽位编号（右下角，带阴影）
        Minecraft mc = Minecraft.getInstance();
        String label = String.valueOf(slotNumber);
        int labelColor = active ? LABEL_ACTIVE : LABEL_NORMAL;
        int labelX = x + size - mc.font.width(label) - 1;
        int labelY = y + size - mc.font.lineHeight - 1;
        graphics.drawString(mc.font, label, labelX + 1, labelY + 1, 0xFF000000, false);
        graphics.drawString(mc.font, label, labelX, labelY, labelColor, false);
    }
}

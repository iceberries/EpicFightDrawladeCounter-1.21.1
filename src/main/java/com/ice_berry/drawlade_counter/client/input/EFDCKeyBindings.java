package com.ice_berry.drawlade_counter.client.input;

import com.ice_berry.drawlade_counter.EFDCMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * 模组按键绑定定义
 * 提供上一武器 / 下一武器的按键切换，支持按键重绑定。
 */
public class EFDCKeyBindings {

    /** 按键分类（对应语言文件的 key.categories.epicfightdrawladecounter） */
    private static final String CATEGORY = "key.categories." + EFDCMod.MODID;

    /** 切换到上一个武器（闭环序列，向前） */
    public static final KeyMapping PREVIOUS_WEAPON = new KeyMapping(
            "key." + EFDCMod.MODID + ".previous_weapon",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V,
            CATEGORY
    );

    /** 切换到下一个武器（闭环序列，向后） */
    public static final KeyMapping NEXT_WEAPON = new KeyMapping(
            "key." + EFDCMod.MODID + ".next_weapon",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            CATEGORY
    );

    private EFDCKeyBindings() {
        // 工具类，禁止实例化
    }
}

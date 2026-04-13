package com.ice_berry.drawlade_counter.config;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 模组配置类
 * 包含模板默认配置和完美格挡配置。
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // #region 模板默认配置

    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    // #endregion

    // #region 完美格挡配置

    /** 完美格挡判定窗口占攻击动画的百分比（0-1），从动画开始播放算起 */
    public static final ModConfigSpec.DoubleValue PERFECT_GUARD_WINDOW = BUILDER
            .comment("Percentage (0-1) of the attack animation duration as the perfect guard window, "
                    + "starting from the beginning of the animation (default: 0.8 = 80%)")
            .defineInRange("perfectGuardWindow", 0.8, 0.01, 1.0);

    /** 完美格挡反击伤害倍率 */
    public static final ModConfigSpec.DoubleValue PERFECT_GUARD_DAMAGE_MULTIPLIER = BUILDER
            .comment("Damage multiplier for perfect guard counter attack (default: 1.5)")
            .defineInRange("perfectGuardDamageMultiplier", 1.5, 0.1, 10.0);

    /** 完美格挡是否启用 */
    public static final ModConfigSpec.BooleanValue PERFECT_GUARD_ENABLED = BUILDER
            .comment("Enable perfect guard counter attack system (default: true)")
            .define("perfectGuardEnabled", true);

    /** 完美格挡强化攻击持续时间（tick） */
    public static final ModConfigSpec.IntValue PERFECT_GUARD_ENHANCED_DURATION = BUILDER
            .comment("Enhanced attack duration after perfect guard in ticks (default: 60 = 3s)")
            .defineInRange("perfectGuardEnhancedDuration", 60, 10, 600);

    // #endregion

    public static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}

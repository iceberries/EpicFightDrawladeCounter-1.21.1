package com.ice_berry.drawlade_counter.combat;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 完美格挡反击数据定义
 * 对应 data/&lt;ns&gt;/efdc/parry_counters/&lt;id&gt;.json 中的记录。
 */
public class PerfectGuardCounterData {

    private final ResourceLocation id;
    private final String weapon;
    private final float damageMultiplier;
    private final double range;
    private final float knockback;
    private final String sound;
    private final String particle;
    private final List<String> applyEffects;
    private final int effectDuration;
    private final int effectAmplifier;
    private final String efAnimation;
    private final int efAnimationPriority;

    // #region 构造方法

    /**
     * 构造完美格挡反击数据
     *
     * @param id                 数据唯一标识符
     * @param weapon             绑定武器 ID（null 匹配任意）
     * @param damageMultiplier   反击伤害倍率
     * @param range              反击命中范围（格）
     * @param knockback          反击击退强度
     * @param sound              反击音效 ID
     * @param particle           反击粒子类型
     * @param applyEffects       额外施加的 Effect ID 列表
     * @param effectDuration     Effect 持续时间（tick）
     * @param effectAmplifier    Effect 等级
     * @param efAnimation        EF 动画标识（null/"auto"）
     * @param efAnimationPriority EF 动画优先级
     */
    public PerfectGuardCounterData(ResourceLocation id, String weapon,
                                   float damageMultiplier, double range, float knockback,
                                   String sound, String particle,
                                   List<String> applyEffects, int effectDuration, int effectAmplifier,
                                   String efAnimation, int efAnimationPriority) {
        this.id = id;
        this.weapon = weapon;
        this.damageMultiplier = damageMultiplier;
        this.range = range;
        this.knockback = knockback;
        this.sound = sound;
        this.particle = particle;
        this.applyEffects = applyEffects != null
                ? Collections.unmodifiableList(new ArrayList<>(applyEffects))
                : Collections.emptyList();
        this.effectDuration = effectDuration;
        this.effectAmplifier = effectAmplifier;
        this.efAnimation = efAnimation;
        this.efAnimationPriority = efAnimationPriority;
    }

    // #endregion

    // #region Getter 方法

    /** @return 数据唯一标识符 */
    public ResourceLocation getId() { return id; }

    /** @return 绑定武器 ID，null 表示匹配任意 */
    public String getWeapon() { return weapon; }

    /** @return 反击伤害倍率 */
    public float getDamageMultiplier() { return damageMultiplier; }

    /** @return 反击命中范围（格） */
    public double getRange() { return range; }

    /** @return 击退强度 */
    public float getKnockback() { return knockback; }

    /** @return 音效 ID 字符串 */
    public String getSound() { return sound; }

    /** @return 粒子类型字符串 */
    public String getParticle() { return particle; }

    /** @return Effect ID 列表（不可变） */
    public List<String> getApplyEffects() { return applyEffects; }

    /** @return Effect 持续时间（tick） */
    public int getEffectDuration() { return effectDuration; }

    /** @return Effect 等级 */
    public int getEffectAmplifier() { return effectAmplifier; }

    /** @return EF 动画标识，null 表示不使用 */
    public String getEfAnimation() { return efAnimation; }

    /** @return EF 动画优先级 */
    public int getEfAnimationPriority() { return efAnimationPriority; }

    // #endregion

    // #region 状态查询

    /** @return true 表示配置了 EF 动画 */
    public boolean hasEfAnimation() {
        return efAnimation != null && !efAnimation.isEmpty();
    }

    /** @return true 表示使用 "auto" 模式 */
    public boolean isAutoAnimation() {
        return "auto".equalsIgnoreCase(efAnimation);
    }

    // #endregion

    // #region 匹配逻辑

    /**
     * 判断该数据是否匹配指定武器，weapon 为 null 时匹配任意
     *
     * @param weaponItemId 武器物品的 ResourceLocation 字符串
     * @return true 表示匹配
     */
    public boolean matches(String weaponItemId) {
        if (weapon == null || weapon.isEmpty()) return true;
        return weapon.equals(weaponItemId);
    }

    // #endregion

    // #region JSON 反序列化

    /**
     * 从 JSON 对象解析完美格挡反击数据
     *
     * @param json       JSON 对象
     * @param fallbackId 解析失败时的回退 ID
     * @return 解析后的 PerfectGuardCounterData 实例
     */
    public static PerfectGuardCounterData fromJson(com.google.gson.JsonObject json,
                                                    ResourceLocation fallbackId) {
        ResourceLocation id = fallbackId;
        if (json.has("id")) id = ResourceLocation.parse(json.get("id").getAsString());

        String weapon = null;
        if (json.has("weapon") && !json.get("weapon").isJsonNull()) {
            weapon = json.get("weapon").getAsString();
        }

        float damageMultiplier = json.has("damage_multiplier")
                ? json.get("damage_multiplier").getAsFloat() : 1.5F;
        double range = json.has("range")
                ? json.get("range").getAsDouble() : 3.0;
        float knockback = json.has("knockback")
                ? json.get("knockback").getAsFloat() : 0.5F;
        String sound = json.has("sound")
                ? json.get("sound").getAsString() : "minecraft:entity.player.attack.crit";
        String particle = json.has("particle")
                ? json.get("particle").getAsString() : "crit";

        List<String> applyEffects = new ArrayList<>();
        if (json.has("apply_effects") && json.get("apply_effects").isJsonArray()) {
            for (var elem : json.get("apply_effects").getAsJsonArray()) {
                applyEffects.add(elem.getAsString());
            }
        }

        int effectDuration = json.has("effect_duration")
                ? json.get("effect_duration").getAsInt() : 200;
        int effectAmplifier = json.has("effect_amplifier")
                ? json.get("effect_amplifier").getAsInt() : 0;

        String efAnimation = null;
        if (json.has("ef_animation") && !json.get("ef_animation").isJsonNull()) {
            efAnimation = json.get("ef_animation").getAsString();
        }

        int efAnimationPriority = json.has("ef_animation_priority")
                ? json.get("ef_animation_priority").getAsInt() : 10;

        return new PerfectGuardCounterData(id, weapon, damageMultiplier, range, knockback,
                sound, particle, applyEffects, effectDuration, effectAmplifier,
                efAnimation, efAnimationPriority);
    }

    // #endregion
}

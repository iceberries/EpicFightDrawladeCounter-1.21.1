package com.ice_berry.drawlade_counter.combat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * 支援攻击数据定义
 * 对应 data/&lt;ns&gt;/efdc/support_attacks/&lt;name&gt;.json 中的每一条记录。
 */
public class SupportAttackData {

    private final ResourceLocation id;
    @Nullable private final ResourceLocation fromWeapon;
    @Nullable private final ResourceLocation toWeapon;
    private final float damageMultiplier;
    private final int cooldownTicks;
    private final float knockback;
    private final float range;
    private final String sound;
    private final String particle;
    @Nullable private final ResourceLocation efAnimation;
    private final int efAnimationPriority;

    public SupportAttackData(ResourceLocation id,
                             @Nullable ResourceLocation fromWeapon,
                             @Nullable ResourceLocation toWeapon,
                             float damageMultiplier, int cooldownTicks,
                             float knockback, float range,
                             String sound, String particle,
                             @Nullable ResourceLocation efAnimation,
                             int efAnimationPriority) {
        this.id = id;
        this.fromWeapon = fromWeapon;
        this.toWeapon = toWeapon;
        this.damageMultiplier = damageMultiplier;
        this.cooldownTicks = cooldownTicks;
        this.knockback = knockback;
        this.range = range;
        this.sound = sound;
        this.particle = particle;
        this.efAnimation = efAnimation;
        this.efAnimationPriority = efAnimationPriority;
    }

    // #region Getter 方法

    public ResourceLocation getId() { return id; }

    /** 原武器 ID，null 表示匹配任意武器 */
    @Nullable
    public ResourceLocation getFromWeapon() { return fromWeapon; }

    /** 目标武器 ID，null 表示匹配任意武器 */
    @Nullable
    public ResourceLocation getToWeapon() { return toWeapon; }

    public float getDamageMultiplier() { return damageMultiplier; }
    public int getCooldownTicks() { return cooldownTicks; }
    public float getKnockback() { return knockback; }
    public float getRange() { return range; }
    public String getSound() { return sound; }
    public String getParticle() { return particle; }

    /** "auto" 特殊值 — 表示自动使用 toWeapon 的 EF 武器攻击动画 */
    public static final String EF_ANIMATION_AUTO = "auto";

    /** EF 攻击动画 ResourceLocation，null 表示使用原版攻击逻辑 */
    @Nullable
    public ResourceLocation getEfAnimation() { return efAnimation; }

    /** EF 动画优先级 */
    public int getEfAnimationPriority() { return efAnimationPriority; }

    /** 是否配置了 EF 动画（包括 auto 模式） */
    public boolean hasEfAnimation() { return efAnimation != null; }

    /** 是否为 auto 模式（从 toWeapon 的 EF 武器能力自动获取攻击动画） */
    public boolean isAutoAnimation() { return efAnimation != null && EF_ANIMATION_AUTO.equals(efAnimation.getPath()); }

    // #endregion

    // #region 匹配逻辑

    /**
     * 检查此数据是否匹配给定的武器切换
     *
     * @param fromItem 原武器 Item（可为 null）
     * @param toItem   目标武器 Item（可为 null）
     * @return true 表示匹配
     */
    public boolean matches(@Nullable Item fromItem, @Nullable Item toItem) {
        if (fromWeapon != null && fromItem != null) {
            ResourceLocation fromKey = BuiltInRegistries.ITEM.getKey(fromItem);
            if (!fromWeapon.equals(fromKey)) return false;
        } else if (fromWeapon != null && fromItem == null) {
            return false;
        }
        if (toWeapon != null && toItem != null) {
            ResourceLocation toKey = BuiltInRegistries.ITEM.getKey(toItem);
            if (!toWeapon.equals(toKey)) return false;
        } else if (toWeapon != null && toItem == null) {
            return false;
        }
        return true;
    }

    /**
     * ItemStack 版本的匹配
     */
    public boolean matches(@Nullable ItemStack from, @Nullable ItemStack to) {
        return matches(
                from != null && !from.isEmpty() ? from.getItem() : null,
                to != null && !to.isEmpty() ? to.getItem() : null
        );
    }

    // #endregion

    // #region JSON 反序列化

    /**
     * 从 JsonObject 解析支援攻击数据
     *
     * @param json 数据 JSON 对象
     * @param id   数据的 ResourceLocation
     * @return 解析后的 SupportAttackData
     * @throws JsonParseException 格式错误时抛出
     */
    public static SupportAttackData fromJson(JsonObject json, ResourceLocation id) throws JsonParseException {
        // from_weapon / to_weapon 均可选，null 表示通配
        ResourceLocation fromWeapon = json.has("from_weapon") && !json.get("from_weapon").isJsonNull()
                ? ResourceLocation.parse(json.get("from_weapon").getAsString()) : null;

        ResourceLocation toWeapon = json.has("to_weapon") && !json.get("to_weapon").isJsonNull()
                ? ResourceLocation.parse(json.get("to_weapon").getAsString()) : null;

        float damageMultiplier = json.has("damage_multiplier")
                ? json.get("damage_multiplier").getAsFloat() : 1.0F;
        int cooldownTicks = json.has("cooldown_ticks")
                ? json.get("cooldown_ticks").getAsInt() : 40;
        float knockback = json.has("knockback")
                ? json.get("knockback").getAsFloat() : 0.5F;
        float range = json.has("range")
                ? json.get("range").getAsFloat() : 4.0F;
        String sound = json.has("sound")
                ? json.get("sound").getAsString() : "minecraft:entity.player.attack.sweep";
        String particle = json.has("particle")
                ? json.get("particle").getAsString() : "crit";

        ResourceLocation efAnimation = null;
        efAnimation = json.has("ef_animation") && !json.get("ef_animation").isJsonNull()
                ? ResourceLocation.parse(json.get("ef_animation").getAsString()) : null;

        int efAnimationPriority = json.has("ef_animation_priority")
                ? json.get("ef_animation_priority").getAsInt() : 0;

        return new SupportAttackData(
                id, fromWeapon, toWeapon,
                damageMultiplier, cooldownTicks,
                knockback, range,
                sound, particle,
                efAnimation, efAnimationPriority
        );
    }

    // #endregion
}

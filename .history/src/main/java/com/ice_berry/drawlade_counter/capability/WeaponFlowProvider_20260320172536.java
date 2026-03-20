package com.ice_berry.drawlade_counter.capability;

import com.ice_berry.drawlade_counter.EFDCMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 武器流转 Capability 提供者
 * 使用 NeoForge 1.21.1 新的 EntityCapability API，
 * 通过 player.getPersistentData() 进行数据持久化，
 * 内存中维护缓存避免频繁 NBT 读写。
 */
public class WeaponFlowProvider implements ICapabilityProvider<Player, Void, IWeaponFlowCapability> {

    /** 武器流转 Capability 定义 */
    public static final EntityCapability<IWeaponFlowCapability, Void> WEAPON_FLOW_CAPABILITY =
            EntityCapability.createVoid(
                    ResourceLocation.fromNamespaceAndPath(EFDCMod.MODID, "weapon_flow"),
                    IWeaponFlowCapability.class
            );

    private static final String DATA_KEY = "WeaponFlowData";
    private static final Map<UUID, WeaponFlowCapability> CACHE = new ConcurrentHashMap<>();

    @Override
    @Nullable
    public IWeaponFlowCapability getCapability(Player player, Void context) {
        return CACHE.computeIfAbsent(player.getUUID(), id -> {
            CompoundTag tag = player.getPersistentData().getCompound(DATA_KEY);
            WeaponFlowCapability cap = new WeaponFlowCapability();
            if (!tag.isEmpty()) {
                cap.load(tag);
            }
            return cap;
        });
    }

    /**
     * 将缓存中的武器流转数据同步到玩家 persistentData
     */
    public static void saveCapability(Player player) {
        WeaponFlowCapability cap = CACHE.get(player.getUUID());
        if (cap != null) {
            CompoundTag tag = new CompoundTag();
            cap.save(tag);
            player.getPersistentData().put(DATA_KEY, tag);
        }
    }

    /**
     * 玩家登出时清除内存缓存
     */
    public static void removeCapability(UUID playerId) {
        CACHE.remove(playerId);
    }

    /**
     * 在玩家死亡/维度切换时，将旧玩家的数据复制到新玩家
     */
    public static void copyCapability(Player original, Player target) {
        WeaponFlowCapability oldCap = CACHE.remove(original.getUUID());
        if (oldCap != null) {
            WeaponFlowCapability newCap = new WeaponFlowCapability();
            CompoundTag tag = new CompoundTag();
            oldCap.save(tag);
            newCap.load(tag);
            CACHE.put(target.getUUID(), newCap);
        }
    }

    /**
     * 便捷方法：从玩家实体获取武器流转 Capability
     *
     * @return Capability 实例，若未注册则返回 null
     */
    @Nullable
    public static IWeaponFlowCapability getCapability(Player player) {
        return player.getCapability(WEAPON_FLOW_CAPABILITY);
    }
}

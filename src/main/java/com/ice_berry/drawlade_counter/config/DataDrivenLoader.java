package com.ice_berry.drawlade_counter.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.ice_berry.drawlade_counter.combat.PerfectGuardCounterData;
import com.ice_berry.drawlade_counter.combat.SupportAttackData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 数据驱动的配置加载器
 * 从 data/&lt;ns&gt;/efdc/ 目录加载支援攻击和完美格挡反击的定义。
 */
public class DataDrivenLoader implements PreparableReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SUPPORT_ATTACK_DIR = "efdc/support_attacks";
    private static final String PARRY_COUNTER_DIR = "efdc/parry_counters";

    private static final Map<ResourceLocation, SupportAttackData> SUPPORT_ATTACKS = new HashMap<>();
    private static final Map<ResourceLocation, PerfectGuardCounterData> PARRY_COUNTERS = new HashMap<>();

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                                           ProfilerFiller preparationProfiler, ProfilerFiller reloadProfiler,
                                           Executor backgroundExecutor, Executor gameExecutor) {
        // Phase 1: 在后台线程解析 JSON
        CompletableFuture<Map<ResourceLocation, SupportAttackData>> loadSupportAttacks = CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, SupportAttackData> loaded = new HashMap<>();
            Map<ResourceLocation, Resource> resources =
                    manager.listResources(SUPPORT_ATTACK_DIR, key -> key.getPath().endsWith(".json"));

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                ResourceLocation resourcePath = entry.getKey();
                Resource resource = entry.getValue();
                try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonElement element = GSON.fromJson(reader, JsonElement.class);
                    if (!element.isJsonObject()) continue;

                    JsonObject json = element.getAsJsonObject();
                    String path = resourcePath.getPath();
                    String relativePath = path.substring(SUPPORT_ATTACK_DIR.length() + 1);
                    String name = relativePath.substring(0, relativePath.length() - 5);
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(resourcePath.getNamespace(), name);

                    loaded.put(id, SupportAttackData.fromJson(json, id));
                } catch (IOException | JsonParseException e) {
                    // 解析失败，跳过该文件
                }
            }
            return loaded;
        }, backgroundExecutor);

        CompletableFuture<Map<ResourceLocation, PerfectGuardCounterData>> loadParryCounters = CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, PerfectGuardCounterData> loaded = new HashMap<>();
            Map<ResourceLocation, Resource> resources =
                    manager.listResources(PARRY_COUNTER_DIR, key -> key.getPath().endsWith(".json"));

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                ResourceLocation resourcePath = entry.getKey();
                Resource resource = entry.getValue();
                try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonElement element = GSON.fromJson(reader, JsonElement.class);
                    if (!element.isJsonObject()) continue;

                    JsonObject json = element.getAsJsonObject();
                    String path = resourcePath.getPath();
                    String relativePath = path.substring(PARRY_COUNTER_DIR.length() + 1);
                    String name = relativePath.substring(0, relativePath.length() - 5);
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(resourcePath.getNamespace(), name);

                    loaded.put(id, PerfectGuardCounterData.fromJson(json, id));
                } catch (IOException | JsonParseException e) {
                    // 解析失败，跳过该文件
                }
            }
            return loaded;
        }, backgroundExecutor);

        // Phase 2: 等待完成后在主线程替换数据
        return loadSupportAttacks.thenCombine(loadParryCounters, (sa, pc) -> new Object[]{sa, pc})
                .thenCompose(barrier::wait)
                .thenAcceptAsync(result -> {
                    @SuppressWarnings("unchecked")
                    Map<ResourceLocation, SupportAttackData> saData = (Map<ResourceLocation, SupportAttackData>) result[0];
                    @SuppressWarnings("unchecked")
                    Map<ResourceLocation, PerfectGuardCounterData> pcData = (Map<ResourceLocation, PerfectGuardCounterData>) result[1];

                    SUPPORT_ATTACKS.clear();
                    SUPPORT_ATTACKS.putAll(saData);
                    PARRY_COUNTERS.clear();
                    PARRY_COUNTERS.putAll(pcData);
                }, gameExecutor);
    }

    // #region 支援攻击查询

    @Nullable
    public static SupportAttackData getSupportAttack(ResourceLocation id) {
        return SUPPORT_ATTACKS.get(id);
    }

    /**
     * 根据武器对查找匹配的支援攻击数据，精确匹配优先
     */
    @Nullable
    public static SupportAttackData findSupportAttack(@Nullable Item fromItem, @Nullable Item toItem) {
        SupportAttackData fallback = null;
        for (SupportAttackData data : SUPPORT_ATTACKS.values()) {
            if (data.matches(fromItem, toItem)) {
                if (data.getFromWeapon() != null && data.getToWeapon() != null) return data;
                if (fallback == null) fallback = data;
            }
        }
        return fallback;
    }

    @Nullable
    public static SupportAttackData findSupportAttack(@Nullable ItemStack from, @Nullable ItemStack to) {
        return findSupportAttack(
                from != null && !from.isEmpty() ? from.getItem() : null,
                to != null && !to.isEmpty() ? to.getItem() : null);
    }

    public static Collection<SupportAttackData> getAllSupportAttacks() {
        return Collections.unmodifiableCollection(SUPPORT_ATTACKS.values());
    }

    public static int size() {
        return SUPPORT_ATTACKS.size();
    }

    // #endregion

    // #region 完美格挡反击查询

    /**
     * 通过 ID 获取完美格挡反击数据
     *
     * @param id 数据 ID
     * @return 对应数据，不存在返回 null
     */
    @Nullable
    public static PerfectGuardCounterData getParryCounter(ResourceLocation id) {
        return PARRY_COUNTERS.get(id);
    }

    /**
     * 根据武器查找匹配的反击数据，精确匹配优先
     *
     * @param weapon 玩家当前主手武器
     * @return 匹配数据，无匹配返回 null
     */
    @Nullable
    public static PerfectGuardCounterData findParryCounter(ItemStack weapon) {
        if (weapon.isEmpty()) return null;

        String weaponId = BuiltInRegistries.ITEM.getKey(weapon.getItem()).toString();
        PerfectGuardCounterData fallback = null;

        for (PerfectGuardCounterData data : PARRY_COUNTERS.values()) {
            if (data.matches(weaponId)) {
                if (data.getWeapon() != null && !data.getWeapon().isEmpty()) return data;
                if (fallback == null) fallback = data;
            }
        }
        return fallback;
    }

    /**
     * 获取所有已加载的反击数据（只读）
     *
     * @return 数据集合
     */
    public static Collection<PerfectGuardCounterData> getAllParryCounters() {
        return Collections.unmodifiableCollection(PARRY_COUNTERS.values());
    }

    // #endregion
}

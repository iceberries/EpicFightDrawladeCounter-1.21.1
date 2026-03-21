package com.ice_berry.drawlade_counter.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.ice_berry.drawlade_counter.EFDCMod;
import com.ice_berry.drawlade_counter.combat.SupportAttackData;
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
 * 数据驱动的支援攻击配置加载器
 * 从 data/&lt;namespace&gt;/efdc/support_attacks/&lt;name&gt;.json 加载所有支援攻击定义。
 * 支持 /reload 热重载。
 */
public class DataDrivenLoader implements PreparableReloadListener {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String DIRECTORY = "efdc/support_attacks";

    /** 已加载的全部支援攻击数据 (id -> data) */
    private static final Map<ResourceLocation, SupportAttackData> SUPPORT_ATTACKS = new HashMap<>();

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager,
                                           ProfilerFiller preparationProfiler, ProfilerFiller reloadProfiler,
                                           Executor backgroundExecutor, Executor gameExecutor) {
        // Phase 1: 在后台线程解析 JSON
        CompletableFuture<Map<ResourceLocation, SupportAttackData>> loadFuture = CompletableFuture.supplyAsync(() -> {
            Map<ResourceLocation, SupportAttackData> loaded = new HashMap<>();
            Map<ResourceLocation, Resource> resources =
                    manager.listResources(DIRECTORY, key -> key.getPath().endsWith(".json"));

            for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
                ResourceLocation resourcePath = entry.getKey();
                Resource resource = entry.getValue();
                try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
                    JsonElement element = GSON.fromJson(reader, JsonElement.class);
                    if (!element.isJsonObject()) {
                        EFDCMod.LOGGER.warn("Skipping non-JSON support attack file: {}", resourcePath);
                        continue;
                    }
                    JsonObject json = element.getAsJsonObject();
                    String path = resourcePath.getPath();
                    String relativePath = path.substring(DIRECTORY.length() + 1);
                    String name = relativePath.substring(0, relativePath.length() - 5);
                    ResourceLocation id = ResourceLocation.fromNamespaceAndPath(resourcePath.getNamespace(), name);

                    SupportAttackData data = SupportAttackData.fromJson(json, id);
                    loaded.put(id, data);
                } catch (IOException | JsonParseException e) {
                    EFDCMod.LOGGER.error("Failed to load support attack data: {}", resourcePath, e);
                }
            }

            EFDCMod.LOGGER.info("Loaded {} support attack data entries", loaded.size());
            return loaded;
        }, backgroundExecutor);

        // Phase 2: 等待 barrier 后在主线程替换数据
        return loadFuture.thenCompose(barrier::wait).thenAcceptAsync(loaded -> {
            SUPPORT_ATTACKS.clear();
            SUPPORT_ATTACKS.putAll(loaded);
        }, gameExecutor);
    }

    // ==================== 查询方法 ====================

    @Nullable
    public static SupportAttackData getSupportAttack(ResourceLocation id) {
        return SUPPORT_ATTACKS.get(id);
    }

    @Nullable
    public static SupportAttackData findSupportAttack(@Nullable Item fromItem, @Nullable Item toItem) {
        for (SupportAttackData data : SUPPORT_ATTACKS.values()) {
            if (data.matches(fromItem, toItem)) {
                return data;
            }
        }
        return null;
    }

    @Nullable
    public static SupportAttackData findSupportAttack(@Nullable ItemStack from, @Nullable ItemStack to) {
        return findSupportAttack(
                from != null && !from.isEmpty() ? from.getItem() : null,
                to != null && !to.isEmpty() ? to.getItem() : null
        );
    }

    public static Collection<SupportAttackData> getAllSupportAttacks() {
        return Collections.unmodifiableCollection(SUPPORT_ATTACKS.values());
    }

    public static int size() {
        return SUPPORT_ATTACKS.size();
    }
}

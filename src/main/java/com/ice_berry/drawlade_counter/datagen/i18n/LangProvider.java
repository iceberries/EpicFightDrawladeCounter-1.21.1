package com.ice_berry.drawlade_counter.datagen.i18n;

import com.google.gson.JsonObject;
import com.ice_berry.drawlade_counter.EFDCMod;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

/**
 * 语言文件数据生成基类
 * 实现 {@link DataProvider} 接口，将翻译键值对输出为 JSON 语言文件。
 * 子类只需实现 {@link #addTranslations()} 填充翻译数据。
 */
public abstract class LangProvider implements DataProvider {

    private final PackOutput.PathProvider pathProvider;
    private final String locale;

    protected final Map<String, String> data = new TreeMap<>();

    protected LangProvider(PackOutput output, String locale) {
        this.locale = locale;
        this.pathProvider = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "lang");
    }

    /**
     * 添加一条翻译。如果 key 已存在则抛出异常。
     */
    protected void add(String key, String value) {
        String existing = this.data.put(key, value);
        if (existing != null) {
            throw new IllegalStateException("Duplicate i18n key: " + key);
        }
    }

    /**
     * 子类实现此方法，在其中调用 {@link #add} 填充翻译条目。
     */
    protected abstract void addTranslations();

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        addTranslations();
        JsonObject json = new JsonObject();
        data.forEach(json::addProperty);
        return DataProvider.saveStable(cache, json,
                pathProvider.json(ResourceLocation.parse(EFDCMod.MODID + ":" + locale)));
    }

    @Override
    public String getName() {
        return EFDCMod.MODID + " Languages [" + locale + "]";
    }
}

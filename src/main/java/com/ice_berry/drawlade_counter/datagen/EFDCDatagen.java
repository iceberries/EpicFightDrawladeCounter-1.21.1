package com.ice_berry.drawlade_counter.datagen;

import com.ice_berry.drawlade_counter.datagen.i18n.LangProvider;
import com.ice_berry.drawlade_counter.datagen.i18n.en_us;
import com.ice_berry.drawlade_counter.datagen.i18n.zh_cn;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.List;

/**
 * 数据生成工具类
 * 通过 {@link GatherDataEvent} 注册所有客户端/服务端数据生成器。
 * 由 {@link EFDCMod} 构造器注册到 MOD 事件总线。
 */
public final class EFDCDatagen {

    private EFDCDatagen() {
    }

    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();

        // 客户端数据生成 - 语言文件
        initI18n(output).forEach(provider -> addClientProvider(event, generator, provider));
    }

    /**
     * 初始化所有语言文件的 DataProvider
     */
    public static List<LangProvider> initI18n(PackOutput output) {
        return List.of(
                new en_us(output),
                new zh_cn(output)
        );
    }

    private static void addClientProvider(GatherDataEvent event,
                                          DataGenerator generator,
                                          DataProvider provider) {
        generator.addProvider(event.includeClient(), provider);
    }

    private static void addServerProvider(GatherDataEvent event,
                                          DataGenerator generator,
                                          DataProvider provider) {
        generator.addProvider(event.includeServer(), provider);
    }
}

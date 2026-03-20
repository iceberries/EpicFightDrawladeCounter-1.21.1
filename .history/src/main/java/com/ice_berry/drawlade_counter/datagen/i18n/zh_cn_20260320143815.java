package com.ice_berry.drawlade_counter.datagen.i18n;

import com.ice_berry.drawlade_counter.EFDCMod;
import net.minecraft.data.PackOutput;

/**
 * 中文翻译数据生成
 */
public class zh_cn extends LangProvider {

    public zh_cn(PackOutput output) {
        super(output, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        String id = EFDCMod.MODID;

        // 示例物品（模板代码，后续可移除）
        add("itemGroup." + id, "拔刃反击 模组标签");
        add("block." + id + ".example_block", "示例方块");
        add("item." + id + ".example_item", "示例物品");

        // 配置界面
        add(id + ".configuration.title", "EpicFight - 拔刃反击 配置");
        add(id + ".configuration.section." + id + ".common.toml", "EpicFight - 拔刃反击 配置");
        add(id + ".configuration.section." + id + ".common.toml.title", "EpicFight - 拔刃反击 配置");
        add(id + ".configuration.items", "物品列表");
        add(id + ".configuration.logDirtBlock", "记录泥土方块");
        add(id + ".configuration.magicNumberIntroduction", "魔法数字文本");
        add(id + ".configuration.magicNumber", "魔法数字");

        // 按键绑定
        add("key.categories." + id, "拔刃反击");
        add("key." + id + ".previous_weapon", "上一把武器");
        add("key." + id + ".next_weapon", "下一把武器");
    }
}

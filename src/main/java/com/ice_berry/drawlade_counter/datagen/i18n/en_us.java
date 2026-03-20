package com.ice_berry.drawlade_counter.datagen.i18n;

import com.ice_berry.drawlade_counter.EFDCMod;
import net.minecraft.data.PackOutput;

/**
 * 英文翻译数据生成
 */
public class en_us extends LangProvider {

    public en_us(PackOutput output) {
        super(output, "en_us");
    }

    @Override
    protected void addTranslations() {
        String id = EFDCMod.MODID;

        // 示例物品（模板代码，后续可移除）
        add("itemGroup." + id, "EpicFight Drawlade Counter");
        add("block." + id + ".example_block", "Example Block");
        add("item." + id + ".example_item", "Example Item");

        // 配置界面
        add(id + ".configuration.title", "EpicFight - Drawlade_Counter Configs");
        add(id + ".configuration.section." + id + ".common.toml", "EpicFight - Drawlade_Counter Configs");
        add(id + ".configuration.section." + id + ".common.toml.title", "EpicFight - Drawlade_Counter Configs");
        add(id + ".configuration.items", "Item List");
        add(id + ".configuration.logDirtBlock", "Log Dirt Block");
        add(id + ".configuration.magicNumberIntroduction", "Magic Number Text");
        add(id + ".configuration.magicNumber", "Magic Number");

        // 按键绑定
        add("key.categories." + id, "EpicFight Drawlade Counter");
        add("key." + id + ".previous_weapon", "Previous Weapon");
        add("key." + id + ".next_weapon", "Next Weapon");
    }
}

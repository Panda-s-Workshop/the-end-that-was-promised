package com.panda.promisedend.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.SmithingTemplateItem;

/**
 * The Resonite Upgrade, the Hollow Sentinel's drop: on a smithing table it turns a diamond piece into its Resonite one,
 * enchantments kept. Its tooltip is vanilla's netherite upgrade's: what it applies to and what it takes.
 */
public class ResoniteTemplateItem extends SmithingTemplateItem {
    private static final String KEY = "item.promisedend.smithing_template.resonite_upgrade.";

    public ResoniteTemplateItem(Properties properties) {
        super(Component.translatable(KEY + "applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable(KEY + "ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable(KEY + "base_slot_description"),
                Component.translatable(KEY + "additions_slot_description"),
                List.of(slot("helmet"), slot("sword"), slot("chestplate"), slot("pickaxe"), slot("leggings"), slot("axe"), slot("boots"), slot("hoe"), slot("shovel")),
                List.of(slot("ingot")), properties);
    }

    private static Identifier slot(String name) {
        return Identifier.withDefaultNamespace("container/slot/" + name);
    }
}

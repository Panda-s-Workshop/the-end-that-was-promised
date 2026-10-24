package com.panda.promisedend.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** An item whose tooltip says how it works: {@code tooltip.promisedend.<item>.0}, {@code .1}, … in grey. */
public class HintItem extends Item {
    private final String key;
    private final int lines;

    public HintItem(Properties properties, String name, int lines) {
        super(properties);
        this.key = "tooltip.promisedend." + name;
        this.lines = lines;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        for (int i = 0; i < lines; i++) out.accept(Component.translatable(key + "." + i).withStyle(ChatFormatting.GRAY));
    }
}

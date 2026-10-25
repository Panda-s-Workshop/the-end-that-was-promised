package com.panda.promisedend.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;

/** A block's item whose tooltip says how it works, as {@link HintItem}: {@code tooltip.promisedend.<block>.0}, … */
public class HintBlockItem extends BlockItem {
    private final String key;
    private final int lines;

    public HintBlockItem(Block block, Properties properties, String name, int lines) {
        super(block, properties);
        this.key = "tooltip.promisedend." + name;
        this.lines = lines;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        for (int i = 0; i < lines; i++) out.accept(Component.translatable(key + "." + i).withStyle(ChatFormatting.GRAY));
    }
}

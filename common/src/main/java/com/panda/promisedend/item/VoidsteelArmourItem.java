package com.panda.promisedend.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A Voidsteel armour piece: its tooltip tells the Void-touched set bonus and, on the client, how many pieces of the set
 * the player has on, in the rift's light purple once it's all four.
 */
public class VoidsteelArmourItem extends HintItem {
    public VoidsteelArmourItem(Properties properties) {
        super(properties, "voidsteel_armour", 4);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        Player player = ResoniteArmourItem.viewer.get();
        if (player == null) return;
        int worn = VoidTouched.piecesWorn(player);
        out.accept(Component.empty());
        out.accept(Component.translatable("tooltip.promisedend.voidsteel_armour.worn", worn, VoidTouched.PIECES)
                .withStyle(worn == VoidTouched.PIECES ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY));
    }
}

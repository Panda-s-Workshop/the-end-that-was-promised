package com.panda.promisedend.item;

import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A Resonite armour piece: its tooltip tells the set bonus and, on the client, how many pieces of the set the player
 * has on, in teal once it's all four.
 */
public class ResoniteArmourItem extends HintItem {
    /** The player reading the tooltip; the client installs it (common code can't read the client's player). */
    public static Supplier<Player> viewer = () -> null;

    public ResoniteArmourItem(Properties properties) {
        super(properties, "resonite_armour", 4);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        Player player = viewer.get();
        if (player == null) return;
        int worn = ResonanceSet.piecesWorn(player);
        out.accept(Component.empty());
        out.accept(Component.translatable("tooltip.promisedend.resonite_armour.worn", worn, ResonanceSet.PIECES)
                .withStyle(worn == ResonanceSet.PIECES ? ChatFormatting.DARK_AQUA : ChatFormatting.GRAY));
    }
}

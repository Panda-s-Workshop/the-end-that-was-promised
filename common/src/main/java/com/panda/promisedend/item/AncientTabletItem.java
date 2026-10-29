package com.panda.promisedend.item;

import com.panda.promisedend.logic.AncientTablets;
import com.panda.promisedend.logic.CodexBook;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * An Ancient Tablet: read, it gives its reader their next page of the Ancients' story, into the Promise Codex, and is
 * used up. A reader who has every page keeps it.
 */
public class AncientTabletItem extends Item {
    public AncientTabletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer reader)) return InteractionResult.SUCCESS;
        ItemStack stack = player.getItemInHand(hand);
        int n = AncientTablets.read(reader);
        if (n == 0) {
            reader.sendOverlayMessage(Component.translatable("message.promisedend.ancient_tablet.known"));
            return InteractionResult.FAIL;
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.6F, 0.7F);
        reader.sendSystemMessage(Component.translatable("message.promisedend.ancient_tablet.read", AncientTablets.title(reader, n), n, CodexBook.PAGES));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        out.accept(Component.translatable("tooltip.promisedend.ancient_tablet.0").withStyle(ChatFormatting.GRAY));
        out.accept(Component.translatable("tooltip.promisedend.ancient_tablet.1").withStyle(ChatFormatting.GRAY));
    }
}

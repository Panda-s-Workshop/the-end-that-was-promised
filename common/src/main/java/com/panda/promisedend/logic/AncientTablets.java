package com.panda.promisedend.logic;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Reading an Ancient Tablet: the reader's next unread page is theirs, as its advancement, in order wherever the tablet
 * was found, so the story reads from its start. A page a pack has taken out counts as read.
 */
public final class AncientTablets {
    private AncientTablets() {}

    /** Whether {@code player} has read page {@code n}, or it doesn't exist. */
    public static boolean read(ServerPlayer player, int n) {
        AdvancementHolder page = player.level().getServer().getAdvancements().get(CodexBook.page(n));
        return page == null || player.getAdvancements().getOrStartProgress(page).isDone();
    }

    /** Reads a tablet: gives the next page and returns its number, or 0 when the reader has every page. */
    public static int read(ServerPlayer player) {
        int n = CodexBook.nextPage(i -> read(player, i));
        if (n == 0) return 0;
        var advancements = player.level().getServer().getAdvancements();
        AdvancementHolder root = advancements.get(CodexBook.TABLETS);
        if (root != null) player.getAdvancements().award(root, "read");
        player.getAdvancements().award(advancements.get(CodexBook.page(n)), "read");
        AdvancementHolder half = advancements.get(CodexBook.TWELVE_TABLETS);
        if (half != null && n >= CodexBook.HALF) player.getAdvancements().award(half, "read");
        return n;
    }

    /** A page's title, as its advancement shows it. */
    public static Component title(ServerPlayer player, int n) {
        AdvancementHolder page = player.level().getServer().getAdvancements().get(CodexBook.page(n));
        return page == null || page.value().display().isEmpty() ? Component.literal(String.valueOf(n)) : page.value().display().get().title();
    }
}

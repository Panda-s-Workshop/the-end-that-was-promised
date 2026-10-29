package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.logic.CodexBook;
import com.panda.promisedend.logic.ShrineBoons;
import com.panda.promisedend.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

/** The Ancient Tablets, and the Codex's goals for the shrines. */
final class TabletGameTests {
    private TabletGameTests() {}

    private static boolean done(ServerPlayer p, Identifier id) {
        var holder = p.level().getServer().getAdvancements().get(id);
        return holder != null && p.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /**
     * Three tablets read give pages 1, 2 and 3 in order, and the tablets' tab, and are used up; the twelfth page gives
     * the Codex's goal for half the story. A reader with every page keeps the tablet. A shrine's boon ticks the Codex's
     * goal for it, and all six its goal for every boon (five don't).
     */
    static void ancientTabletsReadInOrder(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        List<String> bad = new ArrayList<>();
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ANCIENT_TABLET.get(), 3));
        List<InteractionResult> results = new ArrayList<>();
        for (int i = 0; i < 3; i++) results.add(p.getMainHandItem().use(level, p, InteractionHand.MAIN_HAND));
        List<Integer> read = new ArrayList<>();
        for (int n = 1; n <= CodexBook.PAGES; n++) if (done(p, CodexBook.page(n))) read.add(n);
        if (!read.equals(List.of(1, 2, 3))) bad.add("three tablets read gave pages " + read);
        if (!done(p, CodexBook.TABLETS)) bad.add("the tablets' tab wasn't opened");
        if (!p.getMainHandItem().isEmpty()) bad.add("the tablets weren't used up: " + p.getMainHandItem());
        if (done(p, CodexBook.TWELVE_TABLETS)) bad.add("half the story after three pages");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ANCIENT_TABLET.get(), 9));
        for (int i = 0; i < 9; i++) p.getMainHandItem().use(level, p, InteractionHand.MAIN_HAND);
        if (!done(p, CodexBook.TWELVE_TABLETS)) bad.add("twelve pages read didn't give half the story");
        // every page read: the next tablet is kept
        var advancements = level.getServer().getAdvancements();
        for (int n = 13; n <= CodexBook.PAGES; n++) p.getAdvancements().award(advancements.get(CodexBook.page(n)), "read");
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ANCIENT_TABLET.get()));
        var known = p.getMainHandItem().use(level, p, InteractionHand.MAIN_HAND);
        if (known != InteractionResult.FAIL || p.getMainHandItem().getCount() != 1) bad.add("a reader with every page: " + known + ", " + p.getMainHandItem());
        // the shrine's goal
        ShrineBoons.grant(p, 0);
        if (!done(p, CodexBook.TAKE_BOON)) bad.add("a boon didn't tick the Codex's shrine goal");
        for (int i = 1; i < 5; i++) ShrineBoons.grant(p, i);
        if (done(p, CodexBook.ALL_BOONS)) bad.add("every boon's goal after five of six");
        ShrineBoons.grant(p, 5);
        if (!done(p, CodexBook.ALL_BOONS)) bad.add("every boon's goal not kept after all six");
        PromisedEnd.LOGGER.info("[ancient_tablets_read_in_order] {} then {}; pages {}; with all read {}", results, p.getMainHandItem(), read, known);
        level.getServer().getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("tablets: " + bad);
        helper.succeed();
    }

    /** How many tablets each chest holds, over 40 rolls of each: the Waystation's none (the arrival gives three), an Observatory's 1, a Foundry's 2, a Rim Outpost's 2. */
    static void ancientTabletsInChests(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        for (var c : List.of(new Object[] {"waystation", 0}, new Object[] {"fallen_observatory", 1}, new Object[] {"shulker_foundry", 2}, new Object[] {"rim_outpost", 2})) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, PromisedEnd.id("chests/" + c[0]));
            var table = level.getServer().reloadableRegistries().getLootTable(key);
            java.util.Set<Integer> counts = new java.util.TreeSet<>();
            for (int i = 0; i < 40; i++) {
                var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
                counts.add(table.getRandomItems(params, net.minecraft.util.Mth.murmurHash3Mixer(9100 + i)).stream()
                        .filter(st -> st.is(ModItems.ANCIENT_TABLET.get())).mapToInt(ItemStack::getCount).sum());
            }
            if (!counts.equals(java.util.Set.of((Integer) c[1]))) bad.add(c[0] + " gave " + counts + " tablets, not " + c[1]);
        }
        if (!bad.isEmpty()) helper.fail("tablets in chests: " + bad);
        helper.succeed();
    }
}

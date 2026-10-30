package com.panda.promisedend.logic;

import com.panda.promisedend.PromisedEnd;
import java.util.List;
import java.util.function.IntPredicate;
import net.minecraft.resources.Identifier;

/**
 * What the Promise Codex holds: five acts, each a list of goals backed by advancements, in the order a player meets
 * them: the arrival, the outer End, the Void, the Edge, and the heart of the End where the dragon waits once the
 * Ancients' Ward is down. The screen marks a goal done from the client's own advancement progress, so the book needs
 * no packet. An act is sealed until the advancement in {@code opensWith} is done; Act I is always open. Act IV's
 * goals come with the Void's gear and the Edge.
 *
 * <p>The book also holds the Ancient Tablets' pages: {@link #PAGES} of them, read in order, one a tablet, each an
 * advancement of its own (hidden until read), so the screen shows the read ones the same way. And it has a page for
 * each of the Ancients' constructs, written in when it is first met: its name and its one rule.
 */
public final class CodexBook {
    public record Goal(Identifier advancement, String key) {
        public String title() {
            return "codex.promisedend.goal." + key;
        }

        public String hint() {
            return "codex.promisedend.goal." + key + ".hint";
        }
    }

    public record Act(String key, Identifier opensWith, List<Goal> goals) {
        public String title() {
            return "codex.promisedend.act." + key;
        }

        public String sealedHint() {
            return "codex.promisedend.act." + key + ".sealed";
        }
    }

    public static final Identifier KILL_DRAGON = Identifier.withDefaultNamespace("end/kill_dragon");

    /** The advancement a player in the End earns while the Ward is down: it opens the last act. */
    public static final Identifier WARD_DOWN = PromisedEnd.id("rim/ward_down");
    public static final Identifier FORGE_ANCHOR = PromisedEnd.id("rim/forge_anchor");
    /** The Hollow Sentinel's: awarded to everyone who fought it; it opens Act III. */
    public static final Identifier DEFEAT_SENTINEL = PromisedEnd.id("outer/defeat_sentinel");
    /** The Void Leviathan's: awarded to everyone who fought it; it opens Act IV. */
    public static final Identifier DEFEAT_LEVIATHAN = PromisedEnd.id("void/defeat_leviathan");
    /** All four Voidsteel pieces had: the Leviathan's template put to use. */
    public static final Identifier VOIDSTEEL_SET = PromisedEnd.id("void/voidsteel_set");
    /** Awarded by a shrine's boon and a Rift Gate's journey. */
    public static final Identifier TAKE_BOON = PromisedEnd.id("outer/take_boon");
    public static final Identifier GATE_TRAVEL = PromisedEnd.id("outer/gate_travel");
    /** Every boon taken, a criterion each (named as {@link Offerings#BOONS}); twelve pages read. */
    public static final Identifier ALL_BOONS = PromisedEnd.id("outer/all_boons");
    public static final Identifier TWELVE_TABLETS = PromisedEnd.id("outer/twelve_tablets");
    public static final int HALF = 12;

    /** The Architect's Promise used: the closing page, written in under the last act's goals (block.ArchitectsPromiseBlock). */
    public static final Identifier PROMISE_KEPT = PromisedEnd.id("void/promise_kept");

    /** The tablets' pages, and the advancement for having read any (the root of their tab). */
    public static final int PAGES = 24;
    public static final Identifier TABLETS = PromisedEnd.id("lore/root");

    public static Identifier page(int n) {
        return PromisedEnd.id("lore/page_" + n);
    }

    /** A page's text, its own key: the advancement's description is a short line. */
    public static String text(int n) {
        return "codex.promisedend.page." + n;
    }

    /** The first page, 1 to {@link #PAGES}, that {@code read} says isn't read, or 0 when every page is. */
    public static int nextPage(IntPredicate read) {
        for (int n = 1; n <= PAGES; n++) if (!read.test(n)) return n;
        return 0;
    }

    public static final List<Act> ACTS = List.of(
            new Act("arrival", null, List.of(
                    new Goal(PromisedEnd.id("outer/enter_outer"), "enter_outer"),
                    new Goal(PromisedEnd.id("rim/mine_voidstone"), "mine_voidstone"),
                    new Goal(FORGE_ANCHOR, "forge_anchor"),
                    new Goal(PromisedEnd.id("rim/saved_by_anchor"), "saved_by_anchor"),
                    new Goal(PromisedEnd.id("outer/ride_drift"), "ride_drift"))),
            new Act("outer", FORGE_ANCHOR, List.of(
                    new Goal(TABLETS, "read_tablet"),
                    new Goal(GATE_TRAVEL, "gate_travel"),
                    new Goal(TAKE_BOON, "take_boon"),
                    new Goal(PromisedEnd.id("outer/all_seven"), "all_seven"),
                    new Goal(PromisedEnd.id("outer/find_foundry"), "find_foundry"),
                    new Goal(DEFEAT_SENTINEL, "defeat_sentinel"))),
            new Act("void", DEFEAT_SENTINEL, List.of(
                    new Goal(PromisedEnd.id("outer/find_observatory"), "find_observatory"),
                    new Goal(ALL_BOONS, "all_boons"),
                    new Goal(TWELVE_TABLETS, "twelve_tablets"),
                    new Goal(PromisedEnd.id("outer/find_maw"), "find_maw"),
                    new Goal(DEFEAT_LEVIATHAN, "defeat_leviathan"))),
            new Act("edge", DEFEAT_LEVIATHAN, List.of(
                    new Goal(VOIDSTEEL_SET, "voidsteel_set"),
                    new Goal(PromisedEnd.id("void/augment_elytra"), "augment_elytra"),
                    new Goal(PromisedEnd.id("void/starfall_core"), "starfall_core"),
                    new Goal(PromisedEnd.id("void/reach_edge"), "reach_edge"),
                    new Goal(PromisedEnd.id("void/defeat_architect"), "defeat_architect"))),
            new Act("heart", WARD_DOWN, List.of(
                    new Goal(PromisedEnd.id("rim/enter_rim"), "enter_rim"),
                    new Goal(KILL_DRAGON, "kill_dragon"))));

    /**
     * A construct's page: written in when it is first met (its advancement, hidden until then, entity.Constructs#meet),
     * its name the entity's and its one rule a line.
     */
    public record Construct(String key, Identifier met) {
        public String name() {
            return "entity.promisedend." + key;
        }

        public String rule() {
            return "codex.promisedend.construct." + key;
        }
    }

    /** In the order a player meets them: the dragon's, the outer lands', the Edge's. */
    public static final List<Construct> CONSTRUCTS = java.util.stream.Stream.of("crystal_warden", "purpur_sentry", "rift_stalker",
            "chorus_weaver", "lumen_wisp", "lattice_crawler").map(k -> new Construct(k, PromisedEnd.id("outer/met/" + k))).toList();

    /** Each construct's rule, learned by doing it: a stare holds a Stalker, a blow round a raised shield lands, a Weaver wakes, a Wisp lights the way. */
    public static final Identifier FREEZE_STALKER = PromisedEnd.id("outer/freeze_stalker");
    public static final Identifier FLANK_SENTRY = PromisedEnd.id("outer/flank_sentry");
    public static final Identifier WAKE_WEAVER = PromisedEnd.id("outer/wake_weaver");
    public static final Identifier WISP_LIGHT = PromisedEnd.id("outer/wisp_light");

    /** The advancement the Void Anchor awards when it pulls its owner back. */
    public static final Identifier SAVED_BY_ANCHOR = PromisedEnd.id("rim/saved_by_anchor");

    private CodexBook() {}
}

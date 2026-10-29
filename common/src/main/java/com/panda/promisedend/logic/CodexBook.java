package com.panda.promisedend.logic;

import com.panda.promisedend.PromisedEnd;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * What the Promise Codex holds: five acts, each a list of goals backed by advancements, in the order a player meets
 * them: the arrival, the outer End, the Void, the Edge, and the heart of the End where the dragon waits once the
 * Ancients' Ward is down. The screen marks a goal done from the client's own advancement progress, so the book needs
 * no packet. An act is sealed until the advancement in {@code opensWith} is done; Act I is always open. Acts III and
 * IV get their goals with their guardians.
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

    public static final List<Act> ACTS = List.of(
            new Act("arrival", null, List.of(
                    new Goal(PromisedEnd.id("outer/enter_outer"), "enter_outer"),
                    new Goal(PromisedEnd.id("rim/mine_voidstone"), "mine_voidstone"),
                    new Goal(FORGE_ANCHOR, "forge_anchor"),
                    new Goal(PromisedEnd.id("rim/saved_by_anchor"), "saved_by_anchor"),
                    new Goal(PromisedEnd.id("outer/ride_drift"), "ride_drift"))),
            new Act("outer", FORGE_ANCHOR, List.of(
                    new Goal(PromisedEnd.id("outer/find_foundry"), "find_foundry"),
                    new Goal(DEFEAT_SENTINEL, "defeat_sentinel"),
                    new Goal(PromisedEnd.id("outer/all_seven"), "all_seven"))),
            new Act("void", DEFEAT_SENTINEL, List.of()),
            new Act("edge", PromisedEnd.id("void/defeat_leviathan"), List.of()),
            new Act("heart", WARD_DOWN, List.of(
                    new Goal(PromisedEnd.id("rim/enter_rim"), "enter_rim"),
                    new Goal(KILL_DRAGON, "kill_dragon"))));

    /** The advancement the Void Anchor awards when it pulls its owner back. */
    public static final Identifier SAVED_BY_ANCHOR = PromisedEnd.id("rim/saved_by_anchor");

    private CodexBook() {}
}

package com.panda.promisedend.logic;

import com.panda.promisedend.PromisedEnd;
import java.util.List;
import net.minecraft.resources.Identifier;

/**
 * What the Promise Codex holds: four acts, each a list of goals backed by advancements. The screen marks a
 * goal done from the client's own advancement progress, so the book needs no packet. An act is sealed until the
 * advancement in {@code opensWith} is done; Act I is always open. Acts II–IV get their goals in later milestones.
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

    public static final List<Act> ACTS = List.of(
            new Act("rim", null, List.of(
                    new Goal(PromisedEnd.id("rim/enter_rim"), "enter_rim"),
                    new Goal(PromisedEnd.id("rim/mine_voidstone"), "mine_voidstone"),
                    new Goal(PromisedEnd.id("rim/forge_anchor"), "forge_anchor"),
                    new Goal(PromisedEnd.id("rim/saved_by_anchor"), "saved_by_anchor"),
                    new Goal(KILL_DRAGON, "kill_dragon"))),
            new Act("outer", KILL_DRAGON, List.of()),
            new Act("void", PromisedEnd.id("outer/defeat_sentinel"), List.of()),
            new Act("edge", PromisedEnd.id("void/defeat_leviathan"), List.of()));

    /** The advancement the Void Anchor awards when it pulls its owner back. */
    public static final Identifier SAVED_BY_ANCHOR = PromisedEnd.id("rim/saved_by_anchor");

    private CodexBook() {}
}

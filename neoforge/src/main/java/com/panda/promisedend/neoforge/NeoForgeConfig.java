package com.panda.promisedend.neoforge;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The mod's settings as a NeoForge config (synced from server to clients): {@code [terrain] enabled, edge_distance},
 * {@code [anchor] trigger_depth} and {@code [dragon] phase3, wrath, max_wrath}. Until the file is loaded the defaults apply, and the first read says so.
 */
public final class NeoForgeConfig implements EndConfig.Source {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue TERRAIN;
    private static final ModConfigSpec.IntValue EDGE_DISTANCE;
    private static final ModConfigSpec.IntValue ANCHOR_TRIGGER_DEPTH;
    private static final ModConfigSpec.BooleanValue DRAGON_PHASE3;
    private static final ModConfigSpec.BooleanValue DRAGON_WRATH;
    private static final ModConfigSpec.IntValue MAX_WRATH;
    private static final ModConfigSpec.BooleanValue ARRIVAL;
    private static final ModConfigSpec.IntValue WARD_RADIUS;
    private static final ModConfigSpec.IntValue SHRINE_COOLDOWN;
    private static final ModConfigSpec.BooleanValue STARFALL;
    private static final ModConfigSpec.IntValue STARFALL_INTERVAL;
    private static final ModConfigSpec.IntValue STARFALL_MAX_PER_DAY;

    static {
        B.push("terrain");
        TERRAIN = B.comment("Our End biomes replace the vanilla outer-End biomes. Turn off for packs with another End biome overhaul.")
                .define("enabled", true);
        EDGE_DISTANCE = B.comment("Where the Unfinished Edge begins, in blocks from the End's origin. Takes effect in newly generated chunks.")
                .defineInRange("edge_distance", 8000, 2000, 1000000);
        B.pop();
        B.push("anchor");
        ANCHOR_TRIGGER_DEPTH = B.comment("How far below the dimension's lowest block a falling player with a Void Anchor is pulled back (the End: Y -24).")
                .defineInRange("trigger_depth", 24, 0, 52);
        B.pop();
        B.push("dragon");
        DRAGON_PHASE3 = B.comment("The dragon's third phase: below a quarter of its health it calls Crystal Wardens to its pillars (3 to 5 by Wrath) and dives with void breath.")
                .define("phase3", true);
        DRAGON_WRATH = B.comment("Each dragon killed raises the world's Wrath by 1: the next dragon has more health and calls more Wardens, and its hoard is bigger. Off: every fight is the first.")
                .define("wrath", true);
        MAX_WRATH = B.comment("The highest Wrath a world reaches.")
                .defineInRange("max_wrath", 5, 0, 5);
        B.pop();
        B.push("arrival");
        ARRIVAL = B.comment("An End portal sets you down in the outer End in the portal's direction, at an Ancient Waystation with a way home, and the Ancients' Ward keeps everyone from the dragon until three guardians' seals break. Off: vanilla's platform and the dragon at once.")
                .define("by_coordinates", true);
        WARD_RADIUS = B.comment("The Ancients' Ward's radius round the End's centre, in blocks.")
                .defineInRange("ward_radius", 640, 300, 800);
        B.pop();
        B.push("shrine");
        SHRINE_COOLDOWN = B.comment("How long a player waits for another boon at the same Void Shrine, in ticks (24000: 20 minutes).")
                .defineInRange("cooldown_ticks", 24000, 0, 1728000);
        B.pop();
        B.push("starfall");
        STARFALL = B.comment("Stars fall on the outer End's islands while a player is out there, leaving a crater with Starmetal Ore and a Starfall Core. A crater takes only natural End blocks and never lands within 8 blocks of anything built.")
                .define("enabled", true);
        STARFALL_INTERVAL = B.comment("The ticks between falls, give or take a quarter (48000: two Minecraft days).")
                .defineInRange("interval_ticks", 48000, 1200, 1728000);
        STARFALL_MAX_PER_DAY = B.comment("The most stars that fall in a Minecraft day.")
                .defineInRange("max_per_day", 2, 0, 24);
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();

    private static boolean warned;

    private static boolean loaded() {
        if (SPEC.isLoaded()) return true;
        if (!warned) {
            warned = true;
            PromisedEnd.LOGGER.warn("The End That Was Promised: a setting was read before its config file loaded; using the defaults");
        }
        return false;
    }

    @Override
    public boolean terrainEnabled() {
        return loaded() ? TERRAIN.get() : EndConfig.DEFAULTS.terrainEnabled();
    }

    @Override
    public int edgeDistance() {
        return loaded() ? EDGE_DISTANCE.get() : EndConfig.DEFAULTS.edgeDistance();
    }

    @Override
    public int anchorTriggerDepth() {
        return loaded() ? ANCHOR_TRIGGER_DEPTH.get() : EndConfig.DEFAULTS.anchorTriggerDepth();
    }

    @Override
    public boolean dragonPhase3() {
        return loaded() ? DRAGON_PHASE3.get() : EndConfig.DEFAULTS.dragonPhase3();
    }

    @Override
    public boolean arrivalByCoordinates() {
        return loaded() ? ARRIVAL.get() : EndConfig.DEFAULTS.arrivalByCoordinates();
    }

    @Override
    public int wardRadius() {
        return loaded() ? WARD_RADIUS.get() : EndConfig.DEFAULTS.wardRadius();
    }

    @Override
    public int shrineCooldownTicks() {
        return loaded() ? SHRINE_COOLDOWN.get() : EndConfig.DEFAULTS.shrineCooldownTicks();
    }

    @Override
    public boolean starfallEnabled() {
        return loaded() ? STARFALL.get() : EndConfig.DEFAULTS.starfallEnabled();
    }

    @Override
    public int starfallInterval() {
        return loaded() ? STARFALL_INTERVAL.get() : EndConfig.DEFAULTS.starfallInterval();
    }

    @Override
    public int starfallMaxPerDay() {
        return loaded() ? STARFALL_MAX_PER_DAY.get() : EndConfig.DEFAULTS.starfallMaxPerDay();
    }

    @Override
    public boolean dragonWrath() {
        return loaded() ? DRAGON_WRATH.get() : EndConfig.DEFAULTS.dragonWrath();
    }

    @Override
    public int maxWrath() {
        return loaded() ? MAX_WRATH.get() : EndConfig.DEFAULTS.maxWrath();
    }
}

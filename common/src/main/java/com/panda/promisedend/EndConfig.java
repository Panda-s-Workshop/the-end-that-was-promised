package com.panda.promisedend;

/** The mod's settings. Each loader keeps them in its own config system and plugs it in with {@link #use}. */
public final class EndConfig {
    /** Where the values come from. The defaults apply until the loader's config is loaded. */
    public interface Source {
        /** Our End biomes replace the vanilla outer-End biomes (off for packs with another End biome overhaul). */
        boolean terrainEnabled();

        /** Where the Unfinished Edge begins, in blocks from the End's origin. */
        int edgeDistance();

        /** How far below the dimension's lowest block a falling player with a Void Anchor is pulled back. */
        int anchorTriggerDepth();

        /** The dragon's third phase: below a quarter of its health it calls Crystal Wardens and dives with void breath. */
        boolean dragonPhase3();

        /** Dragon rematches raise the world's Wrath: a stronger dragon, more Wardens, a bigger hoard. */
        boolean dragonWrath();

        /** The highest Wrath a world reaches (0 to 5). */
        int maxWrath();

        /**
         * An End portal sets a player down in the outer End by the portal's direction, at an Ancient Waystation, and the
         * Ancients' Ward keeps the dragon until three seals break. Off: vanilla's platform and fight, no Ward.
         */
        default boolean arrivalByCoordinates() {
            return true;
        }

        /** The Ancients' Ward's radius round the End's centre, in blocks. */
        default int wardRadius() {
            return 640;
        }

        /** How long a player waits for another boon at the same Void Shrine, in ticks (20 minutes). */
        default int shrineCooldownTicks() {
            return 24000;
        }
    }

    public static final Source DEFAULTS = new Source() {
        @Override public boolean terrainEnabled() { return true; }
        @Override public int edgeDistance() { return 8000; }
        @Override public int anchorTriggerDepth() { return 24; }
        @Override public boolean dragonPhase3() { return true; }
        @Override public boolean dragonWrath() { return true; }
        @Override public int maxWrath() { return 5; }
    };

    private static Source source = DEFAULTS;

    private EndConfig() {}

    public static void use(Source loaderConfig) {
        source = loaderConfig;
    }

    /** The source in use (the GameTests swap it for a moment and put it back). */
    public static Source current() {
        return source;
    }

    public static boolean terrainEnabled() {
        return source.terrainEnabled();
    }

    public static int edgeDistance() {
        return source.edgeDistance();
    }

    public static int anchorTriggerDepth() {
        return source.anchorTriggerDepth();
    }

    public static boolean dragonPhase3() {
        return source.dragonPhase3();
    }

    public static boolean dragonWrath() {
        return source.dragonWrath();
    }

    public static int maxWrath() {
        return source.maxWrath();
    }

    public static boolean arrivalByCoordinates() {
        return source.arrivalByCoordinates();
    }

    public static int wardRadius() {
        return source.wardRadius();
    }

    public static int shrineCooldownTicks() {
        return source.shrineCooldownTicks();
    }
}

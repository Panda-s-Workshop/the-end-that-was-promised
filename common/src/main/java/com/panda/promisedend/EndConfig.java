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
    }

    public static final Source DEFAULTS = new Source() {
        @Override public boolean terrainEnabled() { return true; }
        @Override public int edgeDistance() { return 8000; }
        @Override public int anchorTriggerDepth() { return 24; }
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
}

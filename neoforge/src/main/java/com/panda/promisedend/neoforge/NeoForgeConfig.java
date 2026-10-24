package com.panda.promisedend.neoforge;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The mod's settings as a NeoForge config (synced from server to clients): {@code [terrain] enabled, edge_distance}
 * and {@code [anchor] trigger_depth}. Until the file is loaded the defaults apply, and the first read says so.
 */
public final class NeoForgeConfig implements EndConfig.Source {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue TERRAIN;
    private static final ModConfigSpec.IntValue EDGE_DISTANCE;
    private static final ModConfigSpec.IntValue ANCHOR_TRIGGER_DEPTH;

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
}

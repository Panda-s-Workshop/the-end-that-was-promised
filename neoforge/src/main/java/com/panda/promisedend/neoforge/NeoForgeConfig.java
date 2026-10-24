package com.panda.promisedend.neoforge;

import com.panda.promisedend.EndConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** The mod's settings as a NeoForge config (synced from server to clients). */
public final class NeoForgeConfig implements EndConfig.Source {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    private static final ModConfigSpec.BooleanValue TERRAIN = B
            .comment("Our End biomes replace the vanilla outer-End biomes. Turn off for packs with another End biome overhaul.")
            .define("terrainEnabled", true);
    private static final ModConfigSpec.IntValue EDGE_DISTANCE = B
            .comment("Where the Unfinished Edge begins, in blocks from the End's origin. Takes effect in newly generated chunks.")
            .defineInRange("edgeDistance", 8000, 2000, 1000000);
    private static final ModConfigSpec.IntValue ANCHOR_TRIGGER_DEPTH = B
            .comment("How far below the dimension's lowest block a falling player with a Void Anchor is pulled back (the End: Y -24).")
            .defineInRange("anchorTriggerDepth", 24, 0, 60);

    public static final ModConfigSpec SPEC = B.build();

    @Override
    public boolean terrainEnabled() {
        return SPEC.isLoaded() ? TERRAIN.get() : EndConfig.DEFAULTS.terrainEnabled();
    }

    @Override
    public int edgeDistance() {
        return SPEC.isLoaded() ? EDGE_DISTANCE.get() : EndConfig.DEFAULTS.edgeDistance();
    }

    @Override
    public int anchorTriggerDepth() {
        return SPEC.isLoaded() ? ANCHOR_TRIGGER_DEPTH.get() : EndConfig.DEFAULTS.anchorTriggerDepth();
    }
}

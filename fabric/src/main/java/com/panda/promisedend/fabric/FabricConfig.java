package com.panda.promisedend.fabric;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The mod's settings on Fabric: config/promisedend.properties ({@code terrain.enabled}, {@code terrain.edge_distance},
 * {@code anchor.trigger_depth}, {@code dragon.phase3}, {@code dragon.wrath}, {@code dragon.max_wrath}), written with the defaults on first start; settings added in a later version are appended to an existing file, which is otherwise left as it is. Unlike NeoForge's synced config it is read
 * once at startup, on each side from its own file.
 */
final class FabricConfig implements EndConfig.Source {
    private final boolean terrainEnabled;
    private final int edgeDistance;
    private final int anchorTriggerDepth;
    private final boolean dragonPhase3;
    private final boolean dragonWrath;
    private final int maxWrath;

    private FabricConfig(Properties p) {
        EndConfig.Source d = EndConfig.DEFAULTS;
        terrainEnabled = Boolean.parseBoolean(p.getProperty("terrain.enabled", String.valueOf(d.terrainEnabled())).trim());
        edgeDistance = clamp(p.getProperty("terrain.edge_distance"), d.edgeDistance(), 2000, 1000000);
        anchorTriggerDepth = clamp(p.getProperty("anchor.trigger_depth"), d.anchorTriggerDepth(), 0, 52);
        dragonPhase3 = Boolean.parseBoolean(p.getProperty("dragon.phase3", String.valueOf(d.dragonPhase3())).trim());
        dragonWrath = Boolean.parseBoolean(p.getProperty("dragon.wrath", String.valueOf(d.dragonWrath())).trim());
        maxWrath = clamp(p.getProperty("dragon.max_wrath"), d.maxWrath(), 0, 5);
    }

    static FabricConfig load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("promisedend.properties");
        Properties p = new Properties();
        boolean exists = Files.exists(file);
        if (exists) {
            try (Reader in = Files.newBufferedReader(file)) {
                p.load(in);
            } catch (IOException e) {
                PromisedEnd.LOGGER.warn("Couldn't read {}, using the defaults", file, e);
                return new FabricConfig(p);
            }
        }
        // the settings the file lacks (all of them for a new file; for one from an older version, those added since) are
        // appended with their comments at their defaults, so a pack maker can find them; nothing already there is touched
        StringBuilder missing = new StringBuilder(exists ? "" : "# The End That Was Promised\n");
        for (String[] key : keys()) {
            if (p.containsKey(key[0])) continue;
            p.setProperty(key[0], key[1]);
            missing.append("\n# ").append(key[2]).append('\n').append(key[0]).append('=').append(key[1]).append('\n');
        }
        if (missing.length() > (exists ? 0 : "# The End That Was Promised\n".length())) {
            try {
                Files.writeString(file, missing.toString(), java.nio.charset.StandardCharsets.ISO_8859_1,
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                PromisedEnd.LOGGER.warn("Couldn't write {}", file, e);
            }
        }
        return new FabricConfig(p);
    }

    /** Each setting: its key, its default and its comment. */
    private static String[][] keys() {
        EndConfig.Source d = EndConfig.DEFAULTS;
        return new String[][] {
            {"terrain.enabled", String.valueOf(d.terrainEnabled()), "our End biomes replace the vanilla outer-End biomes; false for packs with another End biome overhaul (true/false)"},
            {"terrain.edge_distance", String.valueOf(d.edgeDistance()), "where the Unfinished Edge begins, in blocks from the End's origin (2000-1000000)"},
            {"anchor.trigger_depth", String.valueOf(d.anchorTriggerDepth()), "how far below the dimension's lowest block a falling player with a Void Anchor is pulled back (0-52)"},
            {"dragon.phase3", String.valueOf(d.dragonPhase3()), "below a quarter of its health the dragon calls Crystal Wardens to its pillars (3 to 5 by Wrath) and dives with void breath (true/false)"},
            {"dragon.wrath", String.valueOf(d.dragonWrath()), "each dragon killed raises the world's Wrath by 1 for a stronger dragon, more Wardens and a bigger hoard; false: every fight is the first (true/false)"},
            {"dragon.max_wrath", String.valueOf(d.maxWrath()), "the highest Wrath a world reaches (0-5)"},
        };
    }

    private static int clamp(String value, int fallback, int min, int max) {
        if (value == null) return fallback;
        try {
            return Math.clamp(Integer.parseInt(value.trim()), min, max);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public boolean terrainEnabled() {
        return terrainEnabled;
    }

    @Override
    public int edgeDistance() {
        return edgeDistance;
    }

    @Override
    public int anchorTriggerDepth() {
        return anchorTriggerDepth;
    }

    @Override
    public boolean dragonPhase3() {
        return dragonPhase3;
    }

    @Override
    public boolean dragonWrath() {
        return dragonWrath;
    }

    @Override
    public int maxWrath() {
        return maxWrath;
    }
}

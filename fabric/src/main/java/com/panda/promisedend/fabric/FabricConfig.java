package com.panda.promisedend.fabric;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

/**
 * The mod's settings on Fabric: config/promisedend.properties ({@code terrain.enabled}, {@code terrain.edge_distance},
 * {@code anchor.trigger_depth}), written with the defaults on first start. Unlike NeoForge's synced config it is read
 * once at startup, on each side from its own file.
 */
final class FabricConfig implements EndConfig.Source {
    private final boolean terrainEnabled;
    private final int edgeDistance;
    private final int anchorTriggerDepth;

    private FabricConfig(Properties p) {
        EndConfig.Source d = EndConfig.DEFAULTS;
        terrainEnabled = Boolean.parseBoolean(p.getProperty("terrain.enabled", String.valueOf(d.terrainEnabled())).trim());
        edgeDistance = clamp(p.getProperty("terrain.edge_distance"), d.edgeDistance(), 2000, 1000000);
        anchorTriggerDepth = clamp(p.getProperty("anchor.trigger_depth"), d.anchorTriggerDepth(), 0, 60);
    }

    static FabricConfig load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("promisedend.properties");
        Properties p = new Properties();
        if (Files.exists(file)) {
            try (Reader in = Files.newBufferedReader(file)) {
                p.load(in);
            } catch (IOException e) {
                PromisedEnd.LOGGER.warn("Couldn't read {}, using the defaults", file, e);
            }
        } else {
            EndConfig.Source d = EndConfig.DEFAULTS;
            p.setProperty("terrain.enabled", String.valueOf(d.terrainEnabled()));
            p.setProperty("terrain.edge_distance", String.valueOf(d.edgeDistance()));
            p.setProperty("anchor.trigger_depth", String.valueOf(d.anchorTriggerDepth()));
            try (Writer out = Files.newBufferedWriter(file)) {
                p.store(out, """
                         The End That Was Promised
                         terrain.enabled: our End biomes replace the vanilla outer-End biomes; false for packs with another End biome overhaul (true/false)
                         terrain.edge_distance: where the Unfinished Edge begins, in blocks from the End's origin (2000-1000000)
                         anchor.trigger_depth: how far below the dimension's lowest block a falling player with a Void Anchor is pulled back (0-60)""");
            } catch (IOException e) {
                PromisedEnd.LOGGER.warn("Couldn't write {}", file, e);
            }
        }
        return new FabricConfig(p);
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
}

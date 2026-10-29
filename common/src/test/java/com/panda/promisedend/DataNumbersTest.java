package com.panda.promisedend;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.panda.promisedend.logic.VoidAnchor;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The Rim's numbers and the anchor's, as the generated data and constants carry them. */
class DataNumbersTest {
    private static final Path DATA = Path.of("src", "main", "resources", "data", "promisedend");

    private static JsonObject read(String path) throws IOException {
        return JsonParser.parseString(Files.readString(DATA.resolve(path))).getAsJsonObject();
    }

    private static JsonObject modifier(JsonObject placed, String type) {
        for (var m : placed.getAsJsonArray("placement")) {
            if (m.getAsJsonObject().get("type").getAsString().equals(type)) return m.getAsJsonObject();
        }
        return fail("no " + type + " in " + placed);
    }

    @Test
    void debrisIslandsAreOnePerChunkAtTwentyPercentAtY50To80() throws IOException {
        for (String name : new String[] {"rim_debris", "rim_debris_fallback"}) {
            JsonObject placed = read("worldgen/placed_feature/" + name + ".json");
            assertEquals(5, modifier(placed, "promisedend:chunk_chance").get("chance").getAsInt(), name + ": 1 in 5 chunks, 20 %");
            JsonObject height = modifier(placed, "minecraft:height_range").getAsJsonObject("height");
            assertEquals(50, height.getAsJsonObject("min_inclusive").get("absolute").getAsInt(), name);
            assertEquals(80, height.getAsJsonObject("max_inclusive").get("absolute").getAsInt(), name);
        }
    }

    /**
     * Every terrain-off copy (a {@code *_fallback} placed feature) builds only while our terrain is off: its feature
     * checks that itself ({@code "fallback": true}, ours), or its placement starts with {@code promisedend:terrain_off}
     * (a vanilla feature, or one of ours without the flag). With terrain on, vanilla's biomes stay vanilla's.
     */
    @Test
    void everyFallbackWaitsForTerrainOff() throws IOException {
        int checked = 0;
        try (var files = Files.list(DATA.resolve("worldgen/placed_feature"))) {
            for (Path file : files.filter(f -> f.getFileName().toString().endsWith("_fallback.json")).toList()) {
                JsonObject placed = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                String feature = placed.get("feature").getAsString();
                boolean flagged = false;
                if (feature.startsWith("promisedend:")) {
                    JsonObject config = read("worldgen/feature/" + feature.substring("promisedend:".length()) + ".json");
                    flagged = config.has("fallback") && config.get("fallback").getAsBoolean();
                }
                // the first step that places: the random's reseeding goes before everything
                String first = null;
                for (var m : placed.getAsJsonArray("placement")) {
                    first = m.getAsJsonObject().get("type").getAsString();
                    if (!first.equals("promisedend:stable_seed")) break;
                }
                assertTrue(flagged || first.equals("promisedend:terrain_off"), file.getFileName() + " builds with terrain on: " + feature + ", placed first by " + first);
                checked++;
            }
        }
        assertTrue(checked >= 16, "only " + checked + " terrain-off copies found");
    }

    @Test
    void outpostsSpacedTwelveSeparatedFiveInTheRing() throws IOException {
        JsonObject placement = read("worldgen/structure_set/rim_outpost.json").getAsJsonObject("placement");
        assertEquals(12, placement.get("spacing").getAsInt());
        assertEquals(5, placement.get("separation").getAsInt());
        assertEquals(170, placement.get("min_distance").getAsInt());
        assertEquals(430, placement.get("max_distance").getAsInt());
    }

    @Test
    void anchorTakesAPearlFourPolishedVoidstoneAndFourIronNuggets() throws IOException {
        JsonObject recipe = read("recipe/void_anchor.json");
        Map<String, Integer> counts = new HashMap<>();
        JsonObject key = recipe.getAsJsonObject("key");
        for (var row : recipe.getAsJsonArray("pattern")) {
            for (char c : row.getAsString().toCharArray()) {
                if (c != ' ') counts.merge(key.get(String.valueOf(c)).getAsString(), 1, Integer::sum);
            }
        }
        assertEquals(Map.of("minecraft:ender_pearl", 1, "promisedend:polished_voidstone", 4, "minecraft:iron_nugget", 4), counts);
        assertEquals("promisedend:void_anchor", recipe.getAsJsonObject("result").get("id").getAsString());
    }

    @Test
    void rescueGivesFiveSecondsOfSlowFalling() {
        assertEquals(100, VoidAnchor.SLOW_FALLING_TICKS);
    }

    @Test
    void outpostChestHoldsNoAnchor() throws IOException {
        // the outposts are reached last, inside the Ward: an anchor there would be a first kit at the end
        String table = Files.readString(DATA.resolve("loot_table/chests/rim_outpost.json"));
        assertFalse(table.contains("\"promisedend:void_anchor\""), "a charged anchor in the chest ticks Make an anchor");
        assertFalse(table.contains("\"promisedend:cracked_void_anchor\""), "a starter's cracked anchor at the journey's end");
    }
}

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
    void outpostChestNeverHoldsAChargedAnchor() throws IOException {
        String table = Files.readString(DATA.resolve("loot_table/chests/rim_outpost.json"));
        assertFalse(table.contains("\"promisedend:void_anchor\""), "a charged anchor in the chest ticks Make an anchor");
        assertTrue(table.contains("\"promisedend:cracked_void_anchor\""));
    }
}

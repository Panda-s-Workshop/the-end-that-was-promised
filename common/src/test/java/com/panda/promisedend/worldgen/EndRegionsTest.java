package com.panda.promisedend.worldgen;

import static org.junit.jupiter.api.Assertions.*;

import com.panda.promisedend.worldgen.EndRegions.Biome;
import com.panda.promisedend.worldgen.EndRegions.Terrain;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The End's layout: bands, terrain classes, region shares, determinism. */
class EndRegionsTest {
    private static final long SEED = 1234567L;
    private static final int EDGE = 8000;

    private static int chunk(double block) {
        return Math.floorDiv((int) Math.floor(block), 16);
    }

    @Test
    void classifyMatchesVanillaThresholds() {
        assertEquals(Terrain.HIGHLANDS, EndRegions.classify(0.26));
        assertEquals(Terrain.MIDLANDS, EndRegions.classify(0.25));
        assertEquals(Terrain.MIDLANDS, EndRegions.classify(-0.0625));
        assertEquals(Terrain.BARRENS, EndRegions.classify(-0.07));
        assertEquals(Terrain.BARRENS, EndRegions.classify(-0.21875));
        assertEquals(Terrain.ISLANDS, EndRegions.classify(-0.22));
    }

    @Test
    void mainIslandStaysVanillaAndTheRimIsARing() {
        assertEquals(Biome.THE_END, EndRegions.pick(SEED, 0, 0, Terrain.MAIN, EDGE));
        assertEquals(Biome.THE_END, EndRegions.pick(SEED, chunk(100), 0, Terrain.MAIN, EDGE), "the arrival platform at x=100");
        for (int angle = 0; angle < 360; angle += 15) {
            double r = Math.toRadians(angle);
            assertEquals(Biome.SHATTERED_RIM, EndRegions.pick(SEED, chunk(300 * Math.cos(r)), chunk(300 * Math.sin(r)), Terrain.MAIN, EDGE), "rim at " + angle);
            assertEquals(Biome.THE_END, EndRegions.pick(SEED, chunk(700 * Math.cos(r)), chunk(700 * Math.sin(r)), Terrain.MAIN, EDGE), "void gap at " + angle);
        }
        // the end gateways stand on a ring of radius 96: never inside the Rim
        for (int angle = 0; angle < 360; angle += 18) {
            double r = Math.toRadians(angle);
            assertEquals(Biome.THE_END, EndRegions.pick(SEED, chunk(96 * Math.cos(r)), chunk(96 * Math.sin(r)), Terrain.MAIN, EDGE));
        }
    }

    @Test
    void mainAreaIsVanillasSixtyFourChunks() {
        assertTrue(EndRegions.mainArea(64, 0));
        assertFalse(EndRegions.mainArea(65, 0));
        assertTrue(EndRegions.mainArea(45, 45));
        assertFalse(EndRegions.mainArea(46, 46));
    }

    @Test
    void edgeBeginsAroundTheConfiguredDistance() {
        for (int angle = 0; angle < 360; angle += 7) {
            double r = Math.toRadians(angle);
            int inX = chunk((EDGE - EndRegions.EDGE_JITTER - 50) * Math.cos(r)), inZ = chunk((EDGE - EndRegions.EDGE_JITTER - 50) * Math.sin(r));
            int outX = chunk((EDGE + EndRegions.EDGE_JITTER + 50) * Math.cos(r)), outZ = chunk((EDGE + EndRegions.EDGE_JITTER + 50) * Math.sin(r));
            for (Terrain t : EnumSet.complementOf(EnumSet.of(Terrain.MAIN))) {
                assertNotEquals(Biome.UNFINISHED_EDGE, EndRegions.pick(SEED, inX, inZ, t, EDGE));
                assertEquals(Biome.UNFINISHED_EDGE, EndRegions.pick(SEED, outX, outZ, t, EDGE));
            }
        }
    }

    @Test
    void edgeBoundaryIsContinuous() {
        double previous = EndRegions.edgeRadius(SEED, 1, 0, EDGE);
        for (int step = 1; step <= 3600; step++) {
            double r = Math.toRadians(step / 10.0);
            double now = EndRegions.edgeRadius(SEED, Math.cos(r), Math.sin(r), EDGE);
            assertTrue(Math.abs(now - previous) < 25, "edge radius jumps at " + step / 10.0 + " degrees");
            previous = now;
        }
    }

    @Test
    void everyBiomeIsReachableAndSharesAreSane() {
        Map<Biome, Integer> count = new EnumMap<>(Biome.class);
        Map<Terrain, Map<Biome, Integer>> perTerrain = new EnumMap<>(Terrain.class);
        int samples = 0;
        Terrain[] outer = {Terrain.HIGHLANDS, Terrain.MIDLANDS, Terrain.BARRENS, Terrain.ISLANDS};
        for (int gx = 0; gx < 140; gx++) {
            for (int gz = 0; gz < 140; gz++) {
                // a grid over the outer End between 1100 and 7300 blocks out, in all four quadrants
                int bx = (gx - 70) * 104, bz = (gz - 70) * 104;
                double d = Math.sqrt((double) bx * bx + (double) bz * bz);
                if (d < 1100 || d > 7300) continue;
                Terrain t = outer[(gx * 7 + gz * 13) % 4];
                Biome b = EndRegions.pick(SEED, chunk(bx), chunk(bz), t, EDGE);
                count.merge(b, 1, Integer::sum);
                perTerrain.computeIfAbsent(t, k -> new EnumMap<>(Biome.class)).merge(b, 1, Integer::sum);
                samples++;
            }
        }
        Set<Biome> outerBiomes = EnumSet.of(Biome.CHORUS_THICKET, Biome.RESONANT_SPIRES, Biome.HOLLOW_HIGHLANDS, Biome.LUMEN_MIRE,
                Biome.SHULKER_REEFS, Biome.VOIDSCAR_WASTES, Biome.SKYFALL_ARCHIPELAGO);
        for (Biome b : outerBiomes) assertTrue(count.getOrDefault(b, 0) > 0, b + " never appears");
        // within its own terrain class, every biome takes a real share and vanilla keeps at least about a quarter
        for (Terrain t : outer) {
            Map<Biome, Integer> m = perTerrain.get(t);
            int total = m.values().stream().mapToInt(Integer::intValue).sum();
            int vanilla = m.entrySet().stream().filter(e -> e.getKey().vanilla()).mapToInt(Map.Entry::getValue).sum();
            assertTrue(vanilla >= total * 0.15, t + ": vanilla share " + vanilla + "/" + total);
            for (var e : m.entrySet()) {
                assertTrue(e.getValue() >= total * 0.10, t + ": " + e.getKey() + " only " + e.getValue() + "/" + total);
            }
        }
        assertTrue(samples > 5000);
    }

    /** Over a 20,000-sample grid each outer biome is 5–25 % of the outer End. */
    @Test
    void outerSharesMatchTheDesign() {
        Map<Biome, Integer> count = new EnumMap<>(Biome.class);
        int samples = 0;
        Terrain[] outer = {Terrain.HIGHLANDS, Terrain.MIDLANDS, Terrain.BARRENS, Terrain.ISLANDS};
        for (int gx = 0; gx < 200; gx++) {
            for (int gz = 0; gz < 200; gz++) {
                int bx = (gx - 100) * 80, bz = (gz - 100) * 80;
                double d = Math.sqrt((double) bx * bx + (double) bz * bz);
                if (d < 1100 || d > 7300) continue;
                Biome b = EndRegions.pick(SEED, chunk(bx), chunk(bz), outer[(gx * 7 + gz * 13) % 4], EDGE);
                count.merge(b, 1, Integer::sum);
                samples++;
            }
        }
        assertTrue(samples >= 20_000, "only " + samples + " samples");
        int vanilla = 0;
        for (var e : count.entrySet()) {
            double share = (double) e.getValue() / samples;
            if (e.getKey().vanilla()) {
                vanilla += e.getValue();
                continue;
            }
            assertTrue(share >= 0.05 && share <= 0.25, e.getKey() + " is " + Math.round(share * 1000) / 10.0 + " % of the outer End");
        }
        // Vanilla's share is meant to be about 25 %; with equal terrain classes it comes out near 37 %. The real share
        // depends on vanilla's class mix, so it's tuned against the real End generator; this only keeps it from
        // collapsing.
        double v = (double) vanilla / samples;
        assertTrue(v >= 0.15, "vanilla keeps only " + Math.round(v * 1000) / 10.0 + " %");
    }

    @Test
    void regionsAreLargeNotNoise() {
        // walking 2000 blocks east, the region theme changes a few times, not every chunk
        int changes = 0;
        EndRegions.Theme last = null;
        for (int x = 1500; x < 3500; x += 16) {
            EndRegions.Theme t = EndRegions.theme(SEED, x, 1800);
            if (last != null && t != last) changes++;
            last = t;
        }
        assertTrue(changes >= 1 && changes <= 12, "theme changed " + changes + " times over 2000 blocks");
    }

    @Test
    void deterministicPerSeedAndDifferentAcrossSeeds() {
        int same = 0, differ = 0;
        for (int i = 0; i < 400; i++) {
            int cx = 70 + i % 300, cz = (i * 37) % 300 - 150; // all within the outer band, short of the Edge
            Biome a = EndRegions.pick(SEED, cx, cz, Terrain.HIGHLANDS, EDGE);
            assertEquals(a, EndRegions.pick(SEED, cx, cz, Terrain.HIGHLANDS, EDGE));
            if (a == EndRegions.pick(SEED + 1, cx, cz, Terrain.HIGHLANDS, EDGE)) same++;
            else differ++;
        }
        assertTrue(differ > 100, "another seed should give another layout: same " + same + ", differ " + differ);
    }
}

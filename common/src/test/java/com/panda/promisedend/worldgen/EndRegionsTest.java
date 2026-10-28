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
    void eachOuterBiomeStandsForOneTerrainClass() {
        for (EndRegions.Theme theme : EndRegions.Theme.values()) {
            for (Terrain terrain : new Terrain[] {Terrain.HIGHLANDS, Terrain.MIDLANDS, Terrain.BARRENS, Terrain.ISLANDS}) {
                assertEquals(terrain, EndRegions.terrainOf(theme.biome(terrain)), theme + " " + terrain);
            }
        }
        assertNull(EndRegions.terrainOf(Biome.THE_END));
        assertNull(EndRegions.terrainOf(Biome.SHATTERED_RIM));
        assertNull(EndRegions.terrainOf(Biome.UNFINISHED_EDGE));
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

    /**
     * Where the twenty end gateways throw a player (vanilla puts them 96 out at every 18 degrees and looks for land
     * along each from 1024 blocks, up to 256 either way), no region is vanilla's own theme, on five seeds; further out
     * vanilla's theme still turns up.
     */
    @Test
    void gatewaysLandInOurs() {
        int far = 0;
        for (long seed : new long[] {SEED, 42L, -7L, 987654321L, 20261025L}) {
            for (int i = 0; i < 20; i++) {
                double a = 2.0 * (-Math.PI + 0.15707963267948966 * i);
                double ux = Math.cos(a), uz = Math.sin(a);
                for (int d = 768; d <= 1300; d += 16) {
                    assertNotEquals(EndRegions.Theme.VANILLA, EndRegions.theme(seed, ux * d, uz * d), "seed " + seed + ", gateway " + i + ", " + d + " out");
                }
                for (int d = 2600; d <= 7000; d += 64) if (EndRegions.theme(seed, ux * d, uz * d) == EndRegions.Theme.VANILLA) far++;
            }
        }
        assertTrue(far > 0, "vanilla's theme never turns up past the gateways' ring");
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
                Biome.SHULKER_REEFS, Biome.VOIDSCAR_WASTES, Biome.SKYFALL_ARCHIPELAGO, Biome.BLOOM_MEADOWS);
        for (Biome b : outerBiomes) assertTrue(count.getOrDefault(b, 0) > 0, b + " never appears");
        // within its own terrain class, every biome takes a real share and vanilla's own theme still shows (a tenth of
        // the regions; most of vanilla's quarter of the outer End is the small islands of the Crystal theme)
        for (Terrain t : outer) {
            Map<Biome, Integer> m = perTerrain.get(t);
            int total = m.values().stream().mapToInt(Integer::intValue).sum();
            int vanilla = m.entrySet().stream().filter(e -> e.getKey().vanilla()).mapToInt(Map.Entry::getValue).sum();
            assertTrue(vanilla >= total * 0.05, t + ": vanilla share " + vanilla + "/" + total);
            for (var e : m.entrySet()) {
                if (e.getKey().vanilla()) continue;
                assertTrue(e.getValue() >= total * 0.10, t + ": " + e.getKey() + " only " + e.getValue() + "/" + total);
            }
        }
        assertTrue(samples > 5000);
    }

    /**
     * Over a 20,000-sample grid, with vanilla's terrain classes in the real End's mix (GameTest {@code outer_end_shares}:
     * highlands 17.9 %, midlands 25.9 %, barrens 11.0 %, small islands 45.2 %), each outer biome of ours is 2-30 % of the
     * outer End and within a point and a half of what the theme weights give it (the weight times its class's share,
     * summed over the themes it is in), and vanilla's own keep 20-30 %. The smallest is a barrens biome with a theme to
     * itself, about 2 %. Over five seeds: one seed's 600 or so regions leave each theme's share a couple of points
     * either way.
     */
    @Test
    void outerSharesMatchTheDesign() {
        Map<Biome, Integer> count = new EnumMap<>(Biome.class);
        int samples = 0;
        for (long seed : new long[] {SEED, 42L, -7L, 987654321L, 20261025L}) {
            for (int gx = 0; gx < 200; gx++) {
                for (int gz = 0; gz < 200; gz++) {
                    int bx = (gx - 100) * 80, bz = (gz - 100) * 80;
                    double d = Math.sqrt((double) bx * bx + (double) bz * bz);
                    if (d < 1100 || d > 7300) continue;
                    // hashed, not a stripe pattern: stripes of one class are thousands of blocks apart and alias with the regions
                    int roll = (int) Math.floorMod(EndRegions.hash(77L, gx, gz), 1000L);
                    Terrain terrain = roll < 179 ? Terrain.HIGHLANDS : roll < 438 ? Terrain.MIDLANDS : roll < 548 ? Terrain.BARRENS : Terrain.ISLANDS;
                    count.merge(EndRegions.pick(seed, chunk(bx), chunk(bz), terrain, EDGE), 1, Integer::sum);
                    samples++;
                }
            }
        }
        assertTrue(samples >= 100_000, "only " + samples + " samples");
        Map<Terrain, Double> classShare = Map.of(Terrain.HIGHLANDS, 0.179, Terrain.MIDLANDS, 0.259, Terrain.BARRENS, 0.110, Terrain.ISLANDS, 0.452);
        Map<Biome, Double> expected = new EnumMap<>(Biome.class);
        for (EndRegions.Theme theme : EndRegions.Theme.values()) {
            for (var c : classShare.entrySet()) expected.merge(theme.biome(c.getKey()), theme.weight / 100.0 * c.getValue(), Double::sum);
        }
        int vanilla = 0;
        for (var e : count.entrySet()) if (e.getKey().vanilla()) vanilla += e.getValue();
        // every outer biome of ours, so one left out of every theme fails here; the Rim and the Edge are the inner
        // and the far End, not the outer
        for (Biome biome : Biome.values()) {
            if (biome.vanilla() || biome == Biome.SHATTERED_RIM || biome == Biome.UNFINISHED_EDGE) {
                assertTrue(biome.vanilla() || !count.containsKey(biome), biome + " turned up in the outer End");
                continue;
            }
            double share = (double) count.getOrDefault(biome, 0) / samples;
            String said = biome + " is " + Math.round(share * 1000) / 10.0 + " % of the outer End";
            assertTrue(share >= 0.02 && share <= 0.30, said);
            double want = expected.getOrDefault(biome, 0.0);
            assertTrue(Math.abs(share - want) <= 0.015, said + ", the weights give " + Math.round(want * 1000) / 10.0 + " %");
        }
        double v = (double) vanilla / samples;
        assertTrue(v >= 0.20 && v <= 0.30, "vanilla keeps " + Math.round(v * 1000) / 10.0 + " %");
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

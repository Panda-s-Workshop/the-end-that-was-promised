package com.panda.promisedend.worldgen;

/**
 * Where each End biome goes. Pure maths, no Minecraft classes, so JUnit pins the layout.
 *
 * <p>The vanilla End decides biomes per chunk: inside 64 chunks of the origin it is the main island's biome, outside it
 * picks highlands, midlands, barrens or small islands from the terrain's erosion. We keep those decisions (so terrain
 * and biome agree) and swap the biome by distance band and by a seeded region: cells about {@link #CELL} blocks
 * across, each with a theme that says which biome each vanilla terrain class becomes.
 */
public final class EndRegions {
    /** The vanilla End's terrain classes outside the main island, from {@code TheEndBiomeSource#getNoiseBiome}. */
    public enum Terrain { MAIN, HIGHLANDS, MIDLANDS, BARRENS, ISLANDS }

    /** Every biome the End can have once we are installed: the vanilla five and ours. */
    public enum Biome {
        THE_END("minecraft:the_end"),
        END_HIGHLANDS("minecraft:end_highlands"),
        END_MIDLANDS("minecraft:end_midlands"),
        END_BARRENS("minecraft:end_barrens"),
        SMALL_END_ISLANDS("minecraft:small_end_islands"),
        SHATTERED_RIM("promisedend:shattered_rim"),
        CHORUS_THICKET("promisedend:chorus_thicket"),
        RESONANT_SPIRES("promisedend:resonant_spires"),
        HOLLOW_HIGHLANDS("promisedend:hollow_highlands"),
        LUMEN_MIRE("promisedend:lumen_mire"),
        SHULKER_REEFS("promisedend:shulker_reefs"),
        VOIDSCAR_WASTES("promisedend:voidscar_wastes"),
        SKYFALL_ARCHIPELAGO("promisedend:skyfall_archipelago"),
        BLOOM_MEADOWS("promisedend:bloom_meadows"),
        CHORUS_CANOPY("promisedend:chorus_canopy"),
        VOID_FUNGUS_BARRENS("promisedend:void_fungus_barrens"),
        HANGING_GARDENS("promisedend:hanging_gardens"),
        UNFINISHED_EDGE("promisedend:unfinished_edge");

        public final String id;

        Biome(String id) {
            this.id = id;
        }

        public boolean vanilla() {
            return id.startsWith("minecraft:");
        }
    }

    /**
     * A region's theme: the biome each terrain class becomes inside it. Weights sum to 100. The real End is about 18 %
     * highlands, 26 % midlands, 11 % barrens and 45 % small islands (GameTest {@code outer_end_shares}), so a highland
     * biome of ours with a theme to itself is about a fifth of its weight: with five themes, about 3.5-4 % of the outer End.
     * Vanilla's own theme stays small: its small islands in the Crystal theme already give vanilla most of its quarter,
     * which is why Crystal is the heaviest. The Wilds are the living End, a biome of their own for each terrain class;
     * their barrens are the smallest share of ours, about 2 %, since the barrens are the rarest class.
     */
    enum Theme {
        GROVE(21, Biome.CHORUS_THICKET, Biome.LUMEN_MIRE, Biome.VOIDSCAR_WASTES, Biome.SKYFALL_ARCHIPELAGO),
        CRYSTAL(28, Biome.RESONANT_SPIRES, Biome.SHULKER_REEFS, Biome.VOIDSCAR_WASTES, Biome.SMALL_END_ISLANDS),
        ANCIENT(20, Biome.HOLLOW_HIGHLANDS, Biome.SHULKER_REEFS, Biome.VOIDSCAR_WASTES, Biome.SKYFALL_ARCHIPELAGO),
        WILDS(21, Biome.CHORUS_CANOPY, Biome.BLOOM_MEADOWS, Biome.VOID_FUNGUS_BARRENS, Biome.HANGING_GARDENS),
        VANILLA(10, Biome.END_HIGHLANDS, Biome.END_MIDLANDS, Biome.END_BARRENS, Biome.SMALL_END_ISLANDS);

        final int weight;
        final Biome highlands, midlands, barrens, islands;

        Theme(int weight, Biome highlands, Biome midlands, Biome barrens, Biome islands) {
            this.weight = weight;
            this.highlands = highlands;
            this.midlands = midlands;
            this.barrens = barrens;
            this.islands = islands;
        }

        Biome biome(Terrain terrain) {
            return switch (terrain) {
                case HIGHLANDS -> highlands;
                case MIDLANDS -> midlands;
                case BARRENS -> barrens;
                case ISLANDS -> islands;
                case MAIN -> throw new IllegalArgumentException("the main island has no theme");
            };
        }
    }

    /** The Shattered Rim: a ring of debris islands around the main island, reachable before the dragon. */
    public static final int RIM_INNER = 150, RIM_OUTER = 450;
    /** Region cell size in blocks (proposed). */
    public static final int CELL = 512;
    /**
     * Where the gateways land is ours: no region centred nearer than this is vanilla's theme (the gateways throw a player
     * out to about 1000-1500 blocks, and every point there has its region's centre within about 615), and nearer than
     * this the Crystal theme's small islands are the Skyfall Archipelago. The gateway's own search (its mixin) does the
     * rest, past an island's edge still in the main island's biome.
     */
    public static final int GATEWAY_LANDING_CLEAR = 2000;
    /** How far the Unfinished Edge's boundary wanders in and out, in blocks (proposed). */
    public static final int EDGE_JITTER = 600;

    private EndRegions() {}

    /**
     * The terrain class a biome takes the place of: each outer biome, vanilla's four included, stands for one class in
     * every theme, so a biome sampled from the world gives back the class vanilla chose there. Null for the main
     * island's, the Rim's and the Edge's.
     */
    public static Terrain terrainOf(Biome biome) {
        for (Theme theme : Theme.values()) {
            for (Terrain terrain : Terrain.values()) {
                if (terrain != Terrain.MAIN && theme.biome(terrain) == biome) return terrain;
            }
        }
        return null;
    }

    /** The vanilla terrain class for an erosion value, with vanilla's thresholds. */
    public static Terrain classify(double erosion) {
        if (erosion > 0.25) return Terrain.HIGHLANDS;
        if (erosion >= -0.0625) return Terrain.MIDLANDS;
        if (erosion < -0.21875) return Terrain.ISLANDS;
        return Terrain.BARRENS;
    }

    /** Whether a chunk is in vanilla's main-island area (64 chunks around the origin). */
    public static boolean mainArea(int chunkX, int chunkZ) {
        return (long) chunkX * chunkX + (long) chunkZ * chunkZ <= 4096L;
    }

    /**
     * The biome for a chunk.
     *
     * @param terrain      vanilla's class for this chunk ({@link Terrain#MAIN} inside the main-island area)
     * @param edgeDistance where the Unfinished Edge begins, in blocks from the origin
     */
    public static Biome pick(long seed, int chunkX, int chunkZ, Terrain terrain, int edgeDistance) {
        double x = chunkX * 16 + 8, z = chunkZ * 16 + 8;
        double distance = Math.sqrt(x * x + z * z);
        if (terrain == Terrain.MAIN) {
            return distance >= RIM_INNER && distance <= RIM_OUTER ? Biome.SHATTERED_RIM : Biome.THE_END;
        }
        if (distance > edgeRadius(seed, x, z, edgeDistance)) return Biome.UNFINISHED_EDGE;
        Biome picked = theme(seed, x, z).biome(terrain);
        return picked == Biome.SMALL_END_ISLANDS && distance < GATEWAY_LANDING_CLEAR ? Biome.SKYFALL_ARCHIPELAGO : picked;
    }

    /** The Edge's boundary in the direction of (x, z): the base distance plus smooth seeded wobble. */
    static double edgeRadius(long seed, double x, double z, int edgeDistance) {
        double turn = (Math.atan2(z, x) / (2 * Math.PI) + 0.5) * 24; // 24 control points around the circle
        int i = (int) Math.floor(turn);
        double t = turn - i;
        double a = unit(hash(seed ^ 0x5EED_ED6EL, i % 24, 0)), b = unit(hash(seed ^ 0x5EED_ED6EL, (i + 1) % 24, 0));
        double s = t * t * (3 - 2 * t);
        return edgeDistance + EDGE_JITTER * ((a + (b - a) * s) * 2 - 1);
    }

    /** The theme of the region whose jittered centre is nearest to (x, z): a Voronoi diagram over {@link #CELL} cells. */
    static Theme theme(long seed, double x, double z) {
        int cx = Math.floorDiv((int) Math.floor(x), CELL), cz = Math.floorDiv((int) Math.floor(z), CELL);
        double best = Double.MAX_VALUE, bestX = 0, bestZ = 0;
        long bestHash = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                int gx = cx + dx, gz = cz + dz;
                long h = hash(seed, gx, gz);
                double px = (gx + 0.15 + 0.7 * unit(h)) * CELL, pz = (gz + 0.15 + 0.7 * unit(Long.rotateLeft(h, 21))) * CELL;
                double d = (px - x) * (px - x) + (pz - z) * (pz - z);
                if (d < best) {
                    best = d;
                    bestHash = h;
                    bestX = px;
                    bestZ = pz;
                }
            }
        }
        int roll = (int) Math.floorMod(Long.rotateLeft(bestHash, 42) ^ (bestHash >>> 7), 100L);
        Theme picked = Theme.VANILLA;
        for (Theme theme : Theme.values()) {
            if (roll < theme.weight) {
                picked = theme;
                break;
            }
            roll -= theme.weight;
        }
        if (picked != Theme.VANILLA || bestX * bestX + bestZ * bestZ >= (double) GATEWAY_LANDING_CLEAR * GATEWAY_LANDING_CLEAR) return picked;
        // where the gateways land: one of ours instead, by the same weights among them, from other bits of the hash
        int ours = 100 - Theme.VANILLA.weight;
        int again = (int) Math.floorMod(Long.rotateLeft(bestHash, 13) ^ (bestHash >>> 29), (long) ours);
        for (Theme theme : Theme.values()) {
            if (theme == Theme.VANILLA) continue;
            if (again < theme.weight) return theme;
            again -= theme.weight;
        }
        return Theme.GROVE;
    }

    static long hash(long seed, int a, int b) {
        long h = seed * 0x9E3779B97F4A7C15L + a * 0xC2B2AE3D27D4EB4FL + b * 0x165667B19E3779F9L;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    /** A hash's top 53 bits as a double in [0, 1). */
    static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }
}

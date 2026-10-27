package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;

/**
 * One chunk in {@code chance}, as vanilla's {@code rarity_filter}, but drawn from the world's seed, the chunk and the
 * feature's own {@code salt}, not from the decoration random. Vanilla seeds that random by the feature's place among
 * every feature of its step, so adding any feature anywhere moved which chunks had a grove or a scar, and its first
 * rolls ran together across neighbouring chunks (a whole region of a 1-in-6 feature came up empty). Here each chunk
 * is its own hash, the same however many features there are.
 */
public record ChunkChanceFilter(int chance, String salt) implements PlacementFilter {
    public static final MapCodec<ChunkChanceFilter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(1, 1024).fieldOf("chance").forGetter(ChunkChanceFilter::chance),
            Codec.STRING.fieldOf("salt").forGetter(ChunkChanceFilter::salt)
    ).apply(i, ChunkChanceFilter::new));
    /** The seed a GameTest stands in for, one world per test seed; null for the world's own. */
    private static final ThreadLocal<Long> SEED_FOR_TESTS = new ThreadLocal<>();

    /** Runs {@code body} as if the world's seed were {@code seed} (GameTests: a placed feature in many worlds). */
    public static void asIfSeed(long seed, Runnable body) {
        SEED_FOR_TESTS.set(seed);
        try {
            body.run();
        } finally {
            SEED_FOR_TESTS.remove();
        }
    }

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return pick(worldSeed(context), salt, pos.getX() >> 4, pos.getZ() >> 4, chance);
    }

    /** The world's seed, or the one a GameTest stands in for. */
    static long worldSeed(PlacementContext context) {
        Long test = SEED_FOR_TESTS.get();
        return test != null ? test : context.getLevel().getSeed();
    }

    /** Whether the chunk at {@code cx}, {@code cz} is one of the one in {@code chance}: a SplitMix64 hash of the three. */
    public static boolean pick(long seed, String salt, int cx, int cz, int chance) {
        return Long.remainderUnsigned(hash(seed, salt, cx, cz), chance) == 0;
    }

    /** A SplitMix64 hash of the seed, the salt and the chunk. */
    static long hash(long seed, String salt, int cx, int cz) {
        long h = seed ^ salt.hashCode() * 0x9E3779B97F4A7C15L ^ ((cx & 0xFFFFFFFFL) | ((long) cz << 32)) * 0xC2B2AE3D27D4EB4FL;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    @Override
    public MapCodec<ChunkChanceFilter> codec() {
        return CODEC;
    }
}

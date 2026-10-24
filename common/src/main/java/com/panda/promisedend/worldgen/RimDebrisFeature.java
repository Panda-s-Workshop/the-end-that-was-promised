package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A Shattered Rim debris island: a flat-topped lens of Voidstone and end stone that tapers to a point below,
 * like a broken-off piece of the main island, with a few loose shards hanging beside it. On 26.3 a feature is a record
 * whose fields are its configuration ({@code worldgen/feature/rim_debris.json}).
 */
public record RimDebrisFeature(int minRadius, int maxRadius, float voidstone) implements Feature {
    public static final MapCodec<RimDebrisFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(2, 12).fieldOf("min_radius").forGetter(RimDebrisFeature::minRadius),
            Codec.intRange(2, 12).fieldOf("max_radius").forGetter(RimDebrisFeature::maxRadius),
            Codec.floatRange(0, 1).fieldOf("voidstone").forGetter(RimDebrisFeature::voidstone)
    ).apply(i, RimDebrisFeature::new));

    @Override
    public MapCodec<RimDebrisFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // only in the Rim's ring: the feature is also on vanilla the_end (a biome modifier) for packs that turn our
        // terrain off, and there it must not build on the main island or in the void gap
        double distance = Math.hypot(origin.getX(), origin.getZ());
        if (distance < EndRegions.RIM_INNER || distance > EndRegions.RIM_OUTER) return false;
        // the copy on vanilla the_end is for our own End: another mod's dimension that borrows that biome gets none
        if (level.getLevel().dimension() != Level.END && level.getBiome(origin).is(Biomes.THE_END)) return false;
        int radius = Mth.randomBetweenInclusive(random, minRadius, Math.max(minRadius, maxRadius));
        island(level, random, origin, radius);
        int shards = random.nextInt(4);
        for (int i = 0; i < shards; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int d = radius + 2 + random.nextInt(4);
            BlockPos at = origin.offset((int) Math.round(Math.cos(angle) * d), random.nextInt(9) - 5, (int) Math.round(Math.sin(angle) * d));
            island(level, random, at, 1 + random.nextInt(2));
        }
        return true;
    }

    /** A lens: a slightly domed top a block or two thick, then rings shrinking with depth into a jagged point. */
    private void island(WorldGenLevel level, RandomSource random, BlockPos centre, int radius) {
        int depth = Math.max(2, (int) Math.round(radius * 1.3));
        long salt = random.nextLong();
        // the outline wobbles smoothly with angle (two harmonics), so edges are broken but never full of holes
        double p1 = random.nextDouble() * Math.PI * 2, p2 = random.nextDouble() * Math.PI * 2;
        int k1 = 2 + random.nextInt(2), k2 = 4 + random.nextInt(3);
        // a seam where the deep stone breaks through the crust, so a Rim island reads as one from above;
        // bigger islands sometimes get a second, crossing one
        double s1 = random.nextDouble() * Math.PI, o1 = (random.nextDouble() - 0.5) * radius * 0.6;
        double s2 = s1 + Math.PI / 2 + (random.nextDouble() - 0.5) * 0.8;
        boolean second = radius >= 5 && random.nextBoolean();
        for (int dy = 1; dy >= -depth; dy--) {
            double shrink = dy >= 0 ? (dy == 1 ? 0.55 : 1.0) : Math.pow(1.0 + (double) dy / (depth + 1), 0.85);
            double r = radius * shrink;
            int ri = Mth.ceil(r + 2);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double a = Math.atan2(dz, dx);
                    double edge = r * (1 + 0.16 * Math.sin(k1 * a + p1) + 0.08 * Math.sin(k2 * a + p2 + dy * 0.7));
                    if (dx * dx + dz * dz > edge * edge) continue;
                    BlockPos pos = centre.offset(dx, dy, dz);
                    if (!level.getBlockState(pos).isAir()) continue;
                    boolean seam = dy >= 0 && radius >= 3 && (Math.abs(dx * Math.sin(s1) - dz * Math.cos(s1) - o1) < 0.75
                            || second && Math.abs(dx * Math.sin(s2) - dz * Math.cos(s2)) < 0.6 && dx * dx + dz * dz < radius * radius * 0.5);
                    setBlock(level, pos, seam ? ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState() : material(salt, pos, dy));
                }
            }
        }
    }

    /**
     * The top is weathered end stone with a few dark patches; the underbelly, where the piece broke away from the deep
     * stone, is Voidstone with a few pale ones. Patches follow a coarse 4-block cell, so they read as strata, not speckle.
     */
    private BlockState material(long salt, BlockPos pos, int dy) {
        double v = (jitter(salt ^ 0x51DEL, Math.floorDiv(pos.getX(), 4), Math.floorDiv(pos.getZ(), 4) + Math.floorDiv(pos.getY(), 2) * 31) + 1) / 2;
        double dark = dy >= 0 ? voidstone * 0.3 : Math.min(1, voidstone + 0.1 * -dy);
        return v < dark ? ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState() : Blocks.END_STONE.defaultBlockState();
    }

    /** A hash of (salt, a, b) in [-1, 1). */
    private static double jitter(long salt, int a, int b) {
        return EndRegions.unit(EndRegions.hash(salt, a, b)) * 2 - 1;
    }
}

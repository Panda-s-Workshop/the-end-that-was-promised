package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A Shattered Rim debris island: a flat-topped lens of Voidstone and end stone that tapers to a point below,
 * like a broken-off piece of the main island, with a few loose shards hanging beside it. On 26.3 a feature is a record
 * whose fields are its configuration ({@code worldgen/feature/rim_debris.json}).
 *
 * <p>{@code fallback} marks the copy that packs with our terrain off get ({@code rim_debris_fallback}, on every biome in
 * {@code #promisedend:rim_fallback}: vanilla's and other mods' End biomes). It builds only in the End, and never in
 * one of our own biomes, where the Rim's own copy already builds.
 *
 * <p>{@code ring_inner} and {@code ring_outer} narrow the band it builds in ({@code rim_debris_inner}: small stepping
 * stones close to the main island, so the first crossing is short and the Rim shows through the dragon's fog).
 *
 * <p>An island 8 or more across takes one of four {@link Shape}s, so the Rim isn't a field of the same lens: a level
 * cap, a cap tilted as it settled, one split in two by a crack through it, or one with Voidstone shards thrust up
 * from its top. About one in six of them also carries a piece of the Ancients' building that fell with it
 * ({@link Wreck}): a toppled purpur pillar, a broken corner of Voidstone brick, or a chiseled block with its slab.
 */
public record RimDebrisFeature(int minRadius, int maxRadius, float voidstone, boolean fallback, int ringInner, int ringOuter) implements Feature {
    public static final MapCodec<RimDebrisFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            // at most 8: loose shards hang up to 8 past the radius, and a feature may build no further than the chunks
            // beside its own (16 from an origin on a chunk's edge)
            Codec.intRange(2, 8).fieldOf("min_radius").forGetter(RimDebrisFeature::minRadius),
            Codec.intRange(2, 8).fieldOf("max_radius").forGetter(RimDebrisFeature::maxRadius),
            Codec.floatRange(0, 1).fieldOf("voidstone").forGetter(RimDebrisFeature::voidstone),
            Codec.BOOL.optionalFieldOf("fallback", false).forGetter(RimDebrisFeature::fallback),
            Codec.intRange(EndRegions.RIM_INNER, EndRegions.RIM_OUTER).optionalFieldOf("ring_inner", EndRegions.RIM_INNER).forGetter(RimDebrisFeature::ringInner),
            Codec.intRange(EndRegions.RIM_INNER, EndRegions.RIM_OUTER).optionalFieldOf("ring_outer", EndRegions.RIM_OUTER).forGetter(RimDebrisFeature::ringOuter)
    ).apply(i, RimDebrisFeature::new));

    @Override
    public MapCodec<RimDebrisFeature> codec() {
        return CODEC;
    }

    /** The island's form. Stepping stones (under 8 across) are always caps. */
    public enum Shape { CAP, TILTED, SPLIT, SHARDS }

    /** What fell with an island, if anything. */
    public enum Wreck { NONE, PILLAR, CORNER, CHISELED }

    /**
     * Shapes by weight: caps 4 in 11, tilted 3, split 2, shards 2. An island under 13 across isn't split (4 in 9 caps,
     * 3 tilted, 2 shards): one of its halves would be a sliver.
     */
    public static Shape shape(RandomSource random, int radius) {
        if (radius < 4) return Shape.CAP;
        if (radius < 6) {
            int roll = random.nextInt(9);
            return roll < 4 ? Shape.CAP : roll < 7 ? Shape.TILTED : Shape.SHARDS;
        }
        int roll = random.nextInt(11);
        return roll < 4 ? Shape.CAP : roll < 7 ? Shape.TILTED : roll < 9 ? Shape.SPLIT : Shape.SHARDS;
    }

    /** What an island will be: drawn first from the feature's random, so a test can know it from the seed. */
    public record Plan(int radius, Shape shape, Wreck wreck) {}

    public Plan plan(RandomSource random) {
        int radius = Mth.randomBetweenInclusive(random, minRadius, Math.max(minRadius, maxRadius));
        return new Plan(radius, shape(random, radius), wreck(random, radius));
    }

    /** One island in six of 8 or more across carries a wreck, of the three kinds alike. */
    public static Wreck wreck(RandomSource random, int radius) {
        if (radius < 4 || random.nextInt(6) != 0) return Wreck.NONE;
        return Wreck.values()[1 + random.nextInt(3)];
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // only in the Rim's ring: the fallback copy sits on whole End biomes, and must not build on the main island or
        // in the void gap
        double distance = Math.hypot(origin.getX(), origin.getZ());
        if (distance < ringInner || distance > ringOuter) return false;
        if (fallback && (level.getLevel().dimension() != Level.END
                || level.getBiome(origin).unwrapKey().map(k -> k.identifier().getNamespace().equals(PromisedEnd.MODID)).orElse(false))) {
            return false;
        }
        Plan plan = plan(random);
        int radius = plan.radius();
        Shape shape = plan.shape();
        Wreck wreck = plan.wreck();
        double tilt = 0, tiltAngle = random.nextDouble() * Math.PI * 2;
        if (shape == Shape.TILTED) tilt = 0.28 + random.nextDouble() * 0.17;
        java.util.Set<BlockPos> own = new java.util.HashSet<>();
        island(level, random, origin, radius, Math.cos(tiltAngle) * tilt, Math.sin(tiltAngle) * tilt, own);
        if (shape == Shape.SPLIT) split(level, random, origin, radius, own);
        // the wreck before the shards, so it lies on the island's own top and never on a shard
        if (wreck != Wreck.NONE) wreck(level, random, origin, radius, wreck, own);
        if (shape == Shape.SHARDS) shards(level, random, origin, radius, own);
        int shards = random.nextInt(4);
        for (int i = 0; i < shards; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            int d = radius + 2 + random.nextInt(4);
            // a loose shard hangs beside the island, its top no higher than the island's: what broke off sank
            BlockPos at = origin.offset((int) Math.round(Math.cos(angle) * d), -random.nextInt(6), (int) Math.round(Math.sin(angle) * d));
            island(level, random, at, 2 + random.nextInt(2), 0, 0, null);
        }
        return true;
    }

    /**
     * A lens: a slightly domed top a block or two thick, then rings shrinking with depth into a jagged point. A tilt of
     * ({@code tx}, {@code tz}) lifts each column by that many blocks per block out from the middle, so the whole lens
     * leans as one piece; its crust is then a block thicker (drawn as the stone under it, so the island keeps its
     * share of Voidstone), so where two columns step apart they still share a face and no rim block hangs on by an
     * edge. What it places goes into {@code own}, when given.
     */
    private void island(WorldGenLevel level, RandomSource random, BlockPos centre, int radius, double tx, double tz, java.util.Set<BlockPos> own) {
        boolean tilted = tx != 0 || tz != 0;
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
                    BlockPos pos = centre.offset(dx, dy + (int) Math.round(dx * tx + dz * tz), dz);
                    if (!level.getBlockState(pos).isAir()) continue;
                    boolean seam = dy >= 0 && radius >= 3 && (Math.abs(dx * Math.sin(s1) - dz * Math.cos(s1) - o1) < 0.75
                            || second && Math.abs(dx * Math.sin(s2) - dz * Math.cos(s2)) < 0.6 && dx * dx + dz * dz < radius * radius * 0.5);
                    setBlock(level, pos, seam ? ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState() : material(salt, pos, dy));
                    if (own != null) own.add(pos);
                    if (tilted && dy >= 0 && level.getBlockState(pos.below()).isAir()) {
                        setBlock(level, pos.below(), material(salt, pos.below(), dy - 1));
                        if (own != null) own.add(pos.below());
                    }
                }
            }
        }
    }

    /**
     * A crack two blocks wide through the island, near its middle, top to point: the two halves hang apart. The crack
     * takes only what the island itself placed ({@code own}), never a neighbour's stone or an outpost's; and what it cuts
     * off at the rim, a crumb of a few blocks, falls away with it, so no piece under 8 blocks is left.
     */
    private static void split(WorldGenLevel level, RandomSource random, BlockPos centre, int radius, java.util.Set<BlockPos> own) {
        double a = random.nextDouble() * Math.PI, off = (random.nextDouble() - 0.5) * radius * 0.4;
        double nx = Math.sin(a), nz = -Math.cos(a);
        own.removeIf(pos -> {
            double d = (pos.getX() - centre.getX()) * nx + (pos.getZ() - centre.getZ()) * nz - off;
            if (Math.abs(d) >= 1.0) return false;
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            return true;
        });
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        for (BlockPos start : java.util.List.copyOf(own)) {
            if (!seen.add(start)) continue;
            java.util.List<BlockPos> piece = new java.util.ArrayList<>();
            java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>(java.util.List.of(start));
            while (!queue.isEmpty()) {
                BlockPos q = queue.poll();
                piece.add(q);
                for (var d : net.minecraft.core.Direction.values()) {
                    BlockPos r = q.relative(d);
                    if (own.contains(r) && seen.add(r)) queue.add(r);
                }
            }
            if (piece.size() >= 8) continue;
            for (BlockPos p : piece) {
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                own.remove(p);
            }
        }
    }

    /**
     * One to three Voidstone shards thrust up through the top, 3-6 tall, the first two blocks wide at its foot where
     * the top is there to stand on.
     */
    private static void shards(WorldGenLevel level, RandomSource random, BlockPos centre, int radius, java.util.Set<BlockPos> own) {
        var voidstone = ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState();
        int count = 1 + random.nextInt(3);
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2, d = random.nextDouble() * radius * 0.5;
            int x = centre.getX() + (int) Math.round(Math.cos(a) * d), z = centre.getZ() + (int) Math.round(Math.sin(a) * d);
            BlockPos foot = top(level, own, x, z, centre.getY());
            if (foot == null) continue;
            int tall = 3 + random.nextInt(4);
            // the first is two wide, on each of the three columns beside its foot that has the top at the foot's height
            java.util.List<BlockPos> wide = new java.util.ArrayList<>();
            if (i == 0) {
                for (BlockPos side : java.util.List.of(foot.offset(1, 0, 0), foot.offset(0, 0, 1), foot.offset(1, 0, 1))) {
                    if (!level.getBlockState(side).isAir() && level.getBlockState(side.above()).isAir()) wide.add(side);
                }
            }
            for (int dy = 1; dy <= tall; dy++) {
                setIfAir(level, foot.above(dy), voidstone);
                if (dy <= tall / 2) for (BlockPos side : wide) setIfAir(level, side.above(dy), voidstone);
            }
        }
    }

    /**
     * A piece of the Ancients' building lying on the top near the middle: a purpur pillar toppled on its side, 3-5
     * long, with its broken stump upright at one end; an L of Voidstone brick two high whose upper course has
     * crumbled to stairs and slabs; or a chiseled Voidstone block with a brick slab fallen beside it. Each block rests
     * on its own column's top, the island's own stone ({@code own}), so none hangs over a drop or lands on a
     * neighbour; where a column has no top, that block is gone. The pillar lies level, broken off where the top drops.
     */
    private static void wreck(WorldGenLevel level, RandomSource random, BlockPos centre, int radius, Wreck kind, java.util.Set<BlockPos> own) {
        // on pale end stone near the middle where there is some, so the dark brick and Voidstone read against it, not
        // lost on a seam: up to six tries, else the last top found
        BlockPos ground = null;
        for (int tries = 0; tries < 6; tries++) {
            double a = random.nextDouble() * Math.PI * 2, d = random.nextDouble() * radius * 0.35;
            BlockPos found = top(level, own, centre.getX() + (int) Math.round(Math.cos(a) * d), centre.getZ() + (int) Math.round(Math.sin(a) * d), centre.getY());
            if (found == null) continue;
            ground = found;
            if (level.getBlockState(found).is(Blocks.END_STONE)) break;
        }
        if (ground == null) return;
        BlockPos at = ground.above();
        boolean alongX = random.nextBoolean();
        var bricks = ModBlocks.VOIDSTONE_BRICK_SET;
        switch (kind) {
            case PILLAR -> {
                // it fell whichever of the four ways the top runs level the furthest (the drawn one first, on a tie)
                int length = 3 + random.nextInt(3);
                var first = alongX ? net.minecraft.core.Direction.EAST : net.minecraft.core.Direction.SOUTH;
                var fell = first;
                int best = -1;
                for (var d : java.util.List.of(first, first.getOpposite(), first.getClockWise(), first.getCounterClockWise())) {
                    int run = 0;
                    while (run < length) {
                        BlockPos under = top(level, own, at.getX() + d.getStepX() * (run + 1), at.getZ() + d.getStepZ() * (run + 1), centre.getY());
                        if (under == null || under.getY() != ground.getY()) break;
                        run++;
                    }
                    if (run > best) {
                        best = run;
                        fell = d;
                    }
                }
                for (int i = 1; i <= best; i++) {
                    setIfAir(level, at.relative(fell, i), Blocks.PURPUR_PILLAR.defaultBlockState().setValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS, fell.getAxis()));
                }
                setIfAir(level, at, Blocks.PURPUR_PILLAR.defaultBlockState());
            }
            case CORNER -> {
                var brick = bricks.base().get().defaultBlockState();
                var stairs = bricks.stairs().get().defaultBlockState();
                for (int i = 0; i < 3; i++) {
                    BlockPos p = seat(level, own, alongX ? at.offset(i, 0, 0) : at.offset(0, 0, i), centre.getY());
                    // the upper course only over a lower one
                    if (p != null && setIfAir(level, p, brick) && i < 2) setIfAir(level, p.above(), i == 0 ? brick : stairs);
                }
                for (int i = 1; i < 3; i++) {
                    BlockPos p = seat(level, own, alongX ? at.offset(0, 0, i) : at.offset(i, 0, 0), centre.getY());
                    if (p != null) setIfAir(level, p, i == 2 ? bricks.slab().get().defaultBlockState() : brick);
                }
            }
            case CHISELED -> {
                setIfAir(level, at, ModBlocks.CHISELED_VOIDSTONE.get().defaultBlockState());
                BlockPos beside = alongX ? at.offset(1, 0, 0) : at.offset(0, 0, 1);
                BlockPos slabGround = top(level, own, beside.getX(), beside.getZ(), centre.getY());
                if (slabGround != null) setIfAir(level, slabGround.above(), bricks.slab().get().defaultBlockState());
            }
            default -> { }
        }
    }

    /**
     * The island's top block in a column, looking down from a little over the island: the first block there, if it is
     * the island's own stone ({@code own}); null over the void, or where the first is something else (a wreck, another
     * island's stone).
     */
    private static BlockPos top(WorldGenLevel level, java.util.Set<BlockPos> own, int x, int z, int y) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, y + 12, z);
        for (int i = 0; i < 40; i++, at.move(0, -1, 0)) {
            if (level.getBlockState(at).isAir()) continue;
            return own.contains(at) ? at.immutable() : null;
        }
        return null;
    }

    /** The place over the island's top in {@code pos}'s column, or null over the void. */
    private static BlockPos seat(WorldGenLevel level, java.util.Set<BlockPos> own, BlockPos pos, int y) {
        BlockPos top = top(level, own, pos.getX(), pos.getZ(), y);
        return top == null ? null : top.above();
    }

    /** Sets the block where there is air; true if it did. */
    private static boolean setIfAir(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!level.getBlockState(pos).isAir()) return false;
        level.setBlock(pos, state, 2);
        return true;
    }

    /**
     * The top is weathered end stone with a few dark patches; the underbelly, where the piece broke away from the deep
     * stone, is Voidstone with a few pale ones. Patches follow a coarse 4-block cell, so they read as strata, not speckle;
     * each island's cells are offset its own way, so their edges don't line up across islands into long straight seams.
     */
    private BlockState material(long salt, BlockPos pos, int dy) {
        int ox = (int) (salt & 3), oz = (int) ((salt >>> 2) & 3);
        double v = (jitter(salt ^ 0x51DEL, Math.floorDiv(pos.getX() + ox, 4), Math.floorDiv(pos.getZ() + oz, 4) + Math.floorDiv(pos.getY(), 2) * 31) + 1) / 2;
        double dark = dy >= 0 ? voidstone * 0.3 : Math.min(1, voidstone + 0.1 * -dy);
        return v < dark ? ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState() : Blocks.END_STONE.defaultBlockState();
    }

    /** A hash of (salt, a, b) in [-1, 1). */
    private static double jitter(long salt, int a, int b) {
        return EndRegions.unit(EndRegions.hash(salt, a, b)) * 2 - 1;
    }
}

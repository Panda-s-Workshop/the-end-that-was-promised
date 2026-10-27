package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.IntProviders;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * A Hollow Highlands cavern: a hollow in a big island, {@code radius} blocks round (with a wobble, so no two are the
 * same round) and {@code height} tall, its floor flatter than its vault, under at least {@link #ROOF} blocks of the
 * island and over at least {@link #FLOOR}, checked in every column it has. A sinkhole {@link #MOUTH} blocks across
 * opens from its vault to the surface, so every cavern can be found from above (a way in; the way out takes blocks, or
 * a pearl). Its walls are lined with {@code lining} (Voidstone, the islands' core) in soft blobs three or four blocks
 * across, about {@code liningChance} of the wall, and {@code cluster} (Echo Amber) grows on {@code clusterChance} of
 * the open faces round it, pointing out of the wall it grows on.
 *
 * <p>It carves only {@code #promisedend:hollow_cavern_ground} (end stone, Voidstone), and the plants standing over its
 * sinkhole. A cavern that would meet anything else (a structure, another feature's block), that would come within
 * {@link #ROOF} of the island's top or {@link #FLOOR} of its bottom in any of its columns (so it never opens to the
 * void), whose sinkhole wouldn't reach the surface in every column, or any of whose blocks lies in a chunk a structure
 * has claimed, isn't made. It stays within {@link #REACH} blocks of its origin sideways. Its placement has no heightmap
 * step (the origin is the chunk's spot at the bottom of the world), since it finds the island's top and bottom itself.
 * {@code fallback} marks the copy on vanilla's highlands ({@code #promisedend:hollow_fallback}), which builds only in
 * the End with our terrain off.
 */
public record HollowCavernFeature(BlockStateProvider lining, BlockStateProvider cluster, IntProvider radius, IntProvider height,
                                  float liningChance, float clusterChance, boolean fallback) implements Feature {
    public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("hollow_cavern_ground"));
    /** The least island over the vault and under the floor. */
    public static final int ROOF = 4, FLOOR = 3;
    /** The sinkhole's width. */
    public static final int MOUTH = 3;
    /** The farthest it writes sideways: the codec's widest radius (10) with its wobble (1.3), and a wall beyond it. */
    public static final int REACH = 14;
    public static final MapCodec<HollowCavernFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("lining").forGetter(HollowCavernFeature::lining),
            BlockStateProvider.DIRECT_CODEC.fieldOf("cluster").forGetter(HollowCavernFeature::cluster),
            IntProviders.codec(4, 10).fieldOf("radius").forGetter(HollowCavernFeature::radius),
            IntProviders.codec(4, 10).fieldOf("height").forGetter(HollowCavernFeature::height),
            Codec.floatRange(0, 1).fieldOf("lining_chance").forGetter(HollowCavernFeature::liningChance),
            Codec.floatRange(0, 1).fieldOf("cluster_chance").forGetter(HollowCavernFeature::clusterChance),
            Codec.BOOL.optionalFieldOf("fallback", false).forGetter(HollowCavernFeature::fallback)
    ).apply(i, HollowCavernFeature::new));

    @Override
    public MapCodec<HollowCavernFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        // the copy on vanilla's highlands is for packs with our terrain off, where the Hollow Highlands never generate
        if (fallback && (EndConfig.terrainEnabled() || level.getLevel().dimension() != Level.END)) return false;
        int r = radius.sample(random), h = height.sample(random);
        float half = h / 2.0F;
        // the wobble: the radius by bearing, two slow waves with their own phase
        float p1 = random.nextFloat() * Mth.TWO_PI, p2 = random.nextFloat() * Mth.TWO_PI;
        // the island under it: its top and bottom at the middle and round the rim, so the cavern sits inside all of them
        int roof = Integer.MAX_VALUE, floor = Integer.MIN_VALUE;
        List<BlockPos> rim = new ArrayList<>();
        rim.add(origin);
        for (int k = 0; k < 8; k++) {
            float a = k * Mth.TWO_PI / 8;
            rim.add(origin.offset(Math.round(Mth.cos(a) * r * 1.3F), 0, Math.round(Mth.sin(a) * r * 1.3F)));
        }
        for (BlockPos column : rim) {
            int top = top(level, column.getX(), column.getZ());
            if (top == Integer.MIN_VALUE) return false;
            int bottom = top;
            while (bottom > level.getMinY() && level.getBlockState(new BlockPos(column.getX(), bottom - 1, column.getZ())).is(GROUND)) bottom--;
            roof = Math.min(roof, top);
            floor = Math.max(floor, bottom);
        }
        int centre = roof - ROOF - Mth.ceil(half);
        if (centre - Mth.ceil(half * 0.6F) - FLOOR < floor) return false;
        BlockPos mid = new BlockPos(origin.getX(), centre, origin.getZ());

        // the hollow, and the sinkhole up from its vault, off to one side
        Set<BlockPos> hollow = new LinkedHashSet<>();
        int span = Mth.ceil(r * 1.3F);
        for (int dx = -span; dx <= span; dx++) {
            for (int dz = -span; dz <= span; dz++) {
                float bearing = (float) Mth.atan2(dz, dx);
                float reach = r * (1 + 0.18F * Mth.sin(2 * bearing + p1) + 0.12F * Mth.sin(3 * bearing + p2));
                float across = (dx * dx + dz * dz) / (reach * reach);
                for (int dy = -Mth.ceil(half); dy <= Mth.ceil(half); dy++) {
                    // the floor is flatter than the vault
                    float up = dy < 0 ? dy / (half * 0.6F) : dy / half;
                    if (across + up * up <= 1) hollow.add(mid.offset(dx, dy, dz));
                }
            }
        }
        // every column of the hollow inside the island, ROOF under its top and FLOOR over its bottom
        Map<Long, int[]> columns = new HashMap<>();
        for (BlockPos p : hollow) {
            int[] ys = columns.computeIfAbsent(BlockPos.asLong(p.getX(), 0, p.getZ()), k -> new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE});
            ys[0] = Math.min(ys[0], p.getY());
            ys[1] = Math.max(ys[1], p.getY());
        }
        for (var e : columns.entrySet()) {
            int x = BlockPos.getX(e.getKey()), z = BlockPos.getZ(e.getKey()), top = top(level, x, z);
            if (top == Integer.MIN_VALUE || top - ROOF < e.getValue()[1]) return false;
            int bottom = e.getValue()[0];
            for (int d = 1; d <= FLOOR; d++) if (!level.getBlockState(new BlockPos(x, bottom - d, z)).is(GROUND)) return false;
        }
        float a = random.nextFloat() * Mth.TWO_PI;
        int mx = Math.round(Mth.cos(a) * r * 0.5F), mz = Math.round(Mth.sin(a) * r * 0.5F);
        Set<BlockPos> shaft = new LinkedHashSet<>();
        for (int dx = 0; dx < MOUTH; dx++) {
            for (int dz = 0; dz < MOUTH; dz++) {
                // a round-cornered shaft: the 3 x 3 without its corners
                if ((dx == 0 || dx == MOUTH - 1) && (dz == 0 || dz == MOUTH - 1)) continue;
                int x = mid.getX() + mx + dx - 1, z = mid.getZ() + mz + dz - 1, top = top(level, x, z);
                if (top == Integer.MIN_VALUE || top < centre) return false;
                for (int y = centre; y <= top; y++) shaft.add(new BlockPos(x, y, z));
                // and what stands on it, so nothing is left hanging over the hole
                int surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).getY();
                for (int y = top + 1; y < surface; y++) shaft.add(new BlockPos(x, y, z));
            }
        }
        hollow.addAll(shaft);
        // keep out: only its ground and plants over the shaft are carved, and the air already there is left as it is
        for (BlockPos p : hollow) {
            BlockState state = level.getBlockState(p);
            boolean plant = shaft.contains(p) && (state.canBeReplaced() || state.is(Blocks.CHORUS_PLANT) || state.is(Blocks.CHORUS_FLOWER));
            if (!state.isAir() && !state.is(GROUND) && !plant) return false;
        }
        if (StructureClaims.claimed(level, hollow)) return false;
        for (BlockPos p : hollow) level.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        // the walls: lined, then the amber on their open faces, one cluster a block of air at most
        Set<BlockPos> walls = new LinkedHashSet<>();
        for (BlockPos p : hollow) {
            for (Direction d : Direction.values()) {
                BlockPos w = p.relative(d);
                if (!hollow.contains(w) && level.getBlockState(w).is(GROUND)) walls.add(w);
            }
        }
        // in soft blobs, from a smooth noise over the walls, not block by block, which would checker them
        long salt = random.nextLong();
        for (BlockPos w : walls) {
            if (blob(w, salt) < liningChance) level.setBlock(w, lining.getState(level, random, w), Block.UPDATE_CLIENTS);
        }
        for (BlockPos p : hollow) {
            if (random.nextFloat() >= clusterChance) continue;
            List<Direction> on = new ArrayList<>();
            for (Direction d : Direction.values()) {
                BlockPos w = p.relative(d);
                if (walls.contains(w) && level.getBlockState(w).isFaceSturdy(level, w, d.getOpposite())) on.add(d);
            }
            if (on.isEmpty()) continue;
            Direction wall = on.get(random.nextInt(on.size()));
            // pointing out of its wall, for a cluster that has a facing (a pack's block may not)
            level.setBlock(p, cluster.getState(level, random, p).trySetValue(AmethystClusterBlock.FACING, wall.getOpposite()), Block.UPDATE_CLIENTS);
        }
        return true;
    }

    /**
     * Smooth value noise in 0-1 over a 3-block lattice, blended between its corners: neighbouring blocks get near
     * values, so a threshold on it draws blobs. Spread evenly enough that a threshold of t covers about t of the wall.
     */
    static float blob(BlockPos p, long salt) {
        float fx = p.getX() / 3.0F, fy = p.getY() / 3.0F, fz = p.getZ() / 3.0F;
        int x0 = Mth.floor(fx), y0 = Mth.floor(fy), z0 = Mth.floor(fz);
        float tx = smooth(fx - x0), ty = smooth(fy - y0), tz = smooth(fz - z0), sum = 0;
        for (int c = 0; c < 8; c++) {
            int dx = c & 1, dy = (c >> 1) & 1, dz = (c >> 2) & 1;
            float w = (dx == 1 ? tx : 1 - tx) * (dy == 1 ? ty : 1 - ty) * (dz == 1 ? tz : 1 - tz);
            sum += w * corner(x0 + dx, y0 + dy, z0 + dz, salt);
        }
        // the blend of uniform corners bunches round 0.5: stretch it back towards an even spread
        return Mth.clamp((sum - 0.5F) * 1.8F + 0.5F, 0, 1);
    }

    private static float smooth(float t) {
        return t * t * (3 - 2 * t);
    }

    private static float corner(int x, int y, int z, long salt) {
        long h = Mth.getSeed(x, y, z) ^ salt;
        h = (h ^ (h >>> 33)) * 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        return (h >>> 40) / (float) (1L << 24);
    }

    /** The top of the ground in a column, or {@link Integer#MIN_VALUE} over the void: down from the surface past any plant. */
    private static int top(WorldGenLevel level, int x, int z) {
        BlockPos p = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).below();
        while (p.getY() > level.getMinY() && !level.getBlockState(p).is(GROUND)) {
            if (!level.getBlockState(p).isAir() && !level.getBlockState(p).canBeReplaced() && !level.getBlockState(p).is(Blocks.CHORUS_PLANT)
                    && !level.getBlockState(p).is(Blocks.CHORUS_FLOWER)) return Integer.MIN_VALUE;
            p = p.below();
        }
        return p.getY() > level.getMinY() ? p.getY() : Integer.MIN_VALUE;
    }
}

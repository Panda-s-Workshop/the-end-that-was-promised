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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * A Lumen Mire pool: one block of void-water sunk into the ground, in a shape of one to three overlapping discs of
 * radius up to {@code radius}, with Mire Lilies on some of it and a rim of the cover round it. Fields are its
 * configuration ({@code worldgen/feature/mire_pool.json}).
 *
 * <p>The pool lies at the commonest surface height under it. A pool cell is ground from
 * {@code #promisedend:mire_pool_ground} at that height, on something solid, with air over it, or a column one block
 * higher, whose top block is cut away, so a pool settles into a gentle slope (never a block that walls a higher pool
 * already there). Cells whose side would face the open air
 * are dropped, so every cell is walled in by ground, by other cells or by a pool already there (which joins this one),
 * and a pool never spills over an island's edge (a dropped cell stays ground, so one pass does it). Everything stays
 * within {@code 1.5 radius + 3} blocks of the origin, inside the chunks a feature may write to.
 *
 * <p>{@code fallback} marks the sparser copy on {@code #promisedend:mire_fallback} (vanilla's midlands), which builds
 * only in the End with our terrain off: there the Mire never generates, and its rim is the only Lumen Moss.
 */
public record MirePoolFeature(BlockStateProvider water, IntProvider radius, float lilyChance, BlockStateProvider lily,
                              BlockStateProvider rim, float rimChance, boolean fallback) implements Feature {
    public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("mire_pool_ground"));
    /** The smallest pool worth leaving: fewer cells than this and the ground stays as it was. */
    public static final int MIN_CELLS = 4;
    public static final MapCodec<MirePoolFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("water").forGetter(MirePoolFeature::water),
            IntProviders.codec(1, 6).fieldOf("radius").forGetter(MirePoolFeature::radius),
            Codec.floatRange(0, 1).fieldOf("lily_chance").forGetter(MirePoolFeature::lilyChance),
            BlockStateProvider.DIRECT_CODEC.fieldOf("lily").forGetter(MirePoolFeature::lily),
            BlockStateProvider.DIRECT_CODEC.fieldOf("rim").forGetter(MirePoolFeature::rim),
            Codec.floatRange(0, 1).fieldOf("rim_chance").forGetter(MirePoolFeature::rimChance),
            Codec.BOOL.optionalFieldOf("fallback", false).forGetter(MirePoolFeature::fallback)
    ).apply(i, MirePoolFeature::new));

    @Override
    public MapCodec<MirePoolFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (fallback && (EndConfig.terrainEnabled() || level.getLevel().dimension() != Level.END)) return false;
        Block pool = water.getState(level, random, origin).getBlock();
        int r = radius.sample(random);
        // lobes: the first on the origin, the others off it by up to half the radius, each 60-100 % of it across
        int lobes = 1 + random.nextInt(3);
        float[][] lobe = new float[lobes][];
        for (int k = 0; k < lobes; k++) {
            float ox = k == 0 ? 0 : Mth.nextFloat(random, -r / 2f, r / 2f);
            float oz = k == 0 ? 0 : Mth.nextFloat(random, -r / 2f, r / 2f);
            lobe[k] = new float[] {ox, oz, r * Mth.nextFloat(random, 0.6f, 1.0f)};
        }
        int reach = r + r / 2 + 1;
        List<BlockPos> inside = new ArrayList<>();
        Map<Integer, Integer> heights = new HashMap<>();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                boolean in = false;
                for (float[] l : lobe) in |= Mth.square(dx - l[0]) + Mth.square(dz - l[1]) <= l[2] * l[2];
                if (!in) continue;
                BlockPos column = new BlockPos(origin.getX() + dx, 0, origin.getZ() + dz);
                inside.add(column);
                // a column over the void has no surface, and doesn't vote; a pool already there votes its water's own
                // height (void-water doesn't block motion, so the heightmap stands on its floor), so a pool over one joins it
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, column);
                int surface = level.getBlockState(top).is(pool) ? top.getY() : top.getY() - 1;
                if (surface >= level.getMinY()) heights.merge(surface, 1, Integer::sum);
            }
        }
        if (heights.isEmpty()) return false;
        // the commonest surface height, the lower on a tie
        int y = heights.entrySet().stream().max(Map.Entry.<Integer, Integer>comparingByValue()
                .thenComparing(Map.Entry.<Integer, Integer>comparingByKey().reversed())).orElseThrow().getKey();
        Set<BlockPos> cells = new LinkedHashSet<>();
        for (BlockPos column : inside) {
            BlockPos cell = column.atY(y);
            if (poolable(level, cell, y, pool)) cells.add(cell);
        }
        // a single block of ground between this pool and water beyond it goes under too, so pools that meet join
        for (BlockPos cell : List.copyOf(cells)) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos gap = cell.relative(d), beyond = gap.relative(d);
                if (!cells.contains(gap) && (cells.contains(beyond) || level.getBlockState(beyond).is(pool)) && poolable(level, gap, y, pool)) cells.add(gap);
            }
        }
        // walled in on every side, by ground, by this pool or by one already there
        cells.removeIf(cell -> Direction.Plane.HORIZONTAL.stream().anyMatch(d -> {
            BlockPos n = cell.relative(d);
            var side = level.getBlockState(n);
            return !cells.contains(n) && !side.is(pool) && !side.isFaceSturdy(level, n, d.getOpposite());
        }));
        if (cells.size() < MIN_CELLS) return false;
        // not in a chunk a structure has claimed (an End City next door whose pieces aren't placed yet)
        if (StructureClaims.claimed(level, cells)) return false;
        for (BlockPos cell : cells) {
            level.setBlock(cell, water.getState(level, random, cell), Block.UPDATE_CLIENTS);
            // a column a block higher: its top is cut away over the water
            if (!level.getBlockState(cell.above()).isAir()) Plants.clear(level, cell.above());
            // and the grass on a top cut away, so none is left hanging
            BlockState over = level.getBlockState(cell.above(2));
            if (!over.isAir() && Plants.open(over)) Plants.clear(level, cell.above(2));
        }
        for (BlockPos cell : cells) {
            if (random.nextFloat() < lilyChance) level.setBlock(cell.above(), lily.getState(level, random, cell.above()), Block.UPDATE_CLIENTS);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                BlockPos n = cell.relative(d);
                // only ground open to the sky: never moss buried in a bank
                if (!cells.contains(n) && level.getBlockState(n).is(GROUND) && Plants.open(level.getBlockState(n.above())) && random.nextFloat() < rimChance) {
                    level.setBlock(n, rim.getState(level, random, n), Block.UPDATE_CLIENTS);
                }
            }
        }
        return true;
    }

    /**
     * Ground a pool may take at height {@code y}, on something solid: pool ground at the surface with air over it, or
     * under one more block of pool ground (cut away when the pool is made) with air over that.
     */
    private static boolean poolable(WorldGenLevel level, BlockPos cell, int y, Block pool) {
        int surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, cell).getY() - 1;
        if (surface != y && surface != y + 1) return false;
        if (!level.getBlockState(cell).is(GROUND) || !level.getBlockState(cell.below()).isFaceSturdy(level, cell.below(), Direction.UP)) return false;
        // air or ground cover over it (Void Grass, cleared when the pool is made)
        if (surface == y) return Plants.open(level.getBlockState(cell.above()));
        // the block cut away must not be the wall of a higher pool already there, or that pool would face the air
        BlockPos top = cell.above();
        if (Direction.Plane.HORIZONTAL.stream().anyMatch(d -> level.getBlockState(top.relative(d)).is(pool))) return false;
        return level.getBlockState(top).is(GROUND) && Plants.open(level.getBlockState(cell.above(2)));
    }
}

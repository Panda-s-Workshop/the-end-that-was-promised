package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * The Ancients' scaffolding at the Edge, left standing where the building stopped. Two kinds:
 *
 * <ul>
 *   <li><b>A gantry</b> ({@code floating} false), on the ground at the origin: four {@code post} columns at the corners
 *       of a 4 by 4 square, {@code height} high, each run down to the ground under it (6 at most, so one over a drop
 *       hangs); a {@code deck} floor over them with about a third of it never laid; and from the deck a beam a block
 *       wide running {@code reach} out one way, posts hanging under it every fourth block, its last blocks gaps.</li>
 *   <li><b>A lattice islet</b> ({@code floating} true), in the air at the origin: a {@code deck} disc of radius 3-5,
 *       ribbed under with {@code rib} in a cross two deep, and a post standing on it.</li>
 * </ul>
 *
 * It sets only air, so it never cuts into the land or another feature, and it keeps within 15 of its origin's chunk,
 * inside what a feature may write. Nothing where a structure has claimed the ground.
 */
public record EdgeScaffoldFeature(BlockStateProvider post, BlockStateProvider deck, BlockStateProvider rib, boolean floating,
                                  int minHeight, int maxHeight, int maxReach) implements Feature {
    public static final MapCodec<EdgeScaffoldFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("post").forGetter(EdgeScaffoldFeature::post),
            BlockStateProvider.DIRECT_CODEC.fieldOf("deck").forGetter(EdgeScaffoldFeature::deck),
            BlockStateProvider.DIRECT_CODEC.fieldOf("rib").forGetter(EdgeScaffoldFeature::rib),
            Codec.BOOL.fieldOf("floating").forGetter(EdgeScaffoldFeature::floating),
            Codec.intRange(1, 16).fieldOf("min_height").forGetter(EdgeScaffoldFeature::minHeight),
            Codec.intRange(1, 16).fieldOf("max_height").forGetter(EdgeScaffoldFeature::maxHeight),
            Codec.intRange(0, 11).fieldOf("max_reach").forGetter(EdgeScaffoldFeature::maxReach)
    ).apply(i, EdgeScaffoldFeature::new));

    @Override
    public MapCodec<EdgeScaffoldFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        if (StructureClaims.claimed(level, List.of(origin))) return false;
        return floating ? islet(level, random, origin) : gantry(level, random, origin);
    }

    private boolean gantry(WorldGenLevel level, RandomSource random, BlockPos origin) {
        if (!level.getBlockState(origin).isAir() || level.getBlockState(origin.below()).isAir()) return false;
        int height = minHeight + random.nextInt(maxHeight - minHeight + 1);
        int deckY = origin.getY() + height;
        List<BlockPos> posts = new ArrayList<>();
        for (int dx : new int[] {0, 3}) {
            for (int dz : new int[] {0, 3}) posts.add(origin.offset(dx, 0, dz));
        }
        for (BlockPos foot : posts) {
            // up to the deck, and down to the ground under it (a corner over a drop hangs, 6 at most)
            for (int y = foot.getY(); y < deckY; y++) set(level, new BlockPos(foot.getX(), y, foot.getZ()), post, random);
            for (int d = 1; d <= 6 && level.getBlockState(foot.below(d)).isAir(); d++) set(level, foot.below(d), post, random);
        }
        // the deck, a third of it never laid (never the corners, which stand on the posts)
        for (int dx = 0; dx <= 3; dx++) {
            for (int dz = 0; dz <= 3; dz++) {
                boolean corner = (dx == 0 || dx == 3) && (dz == 0 || dz == 3);
                if (corner || random.nextFloat() >= 0.34F) set(level, new BlockPos(origin.getX() + dx, deckY, origin.getZ() + dz), deck, random);
            }
        }
        // the beam, from the middle of one side out
        if (maxReach > 0) {
            Direction way = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            int reach = 4 + random.nextInt(Math.max(1, maxReach - 3));
            BlockPos start = origin.offset(1 + (way.getStepX() > 0 ? 2 : 0), 0, 1 + (way.getStepZ() > 0 ? 2 : 0));
            if (way.getStepX() < 0) start = start.offset(-1, 0, 0);
            if (way.getStepZ() < 0) start = start.offset(0, 0, -1);
            int unfinished = 1 + random.nextInt(3);
            for (int k = 1; k <= reach; k++) {
                BlockPos at = new BlockPos(start.getX() + way.getStepX() * k, deckY, start.getZ() + way.getStepZ() * k);
                // the end of the beam: laid now and then, the rest gaps
                if (k > reach - unfinished && random.nextFloat() < 0.6F) continue;
                set(level, at, deck, random);
                if (k % 4 == 0) {
                    int hang = 1 + random.nextInt(3);
                    for (int d = 1; d <= hang; d++) set(level, at.below(d), post, random);
                }
            }
        }
        return true;
    }

    private boolean islet(WorldGenLevel level, RandomSource random, BlockPos origin) {
        int radius = 3 + random.nextInt(3);
        // in clear air: nothing within the disc's reach, so it doesn't merge into an island
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(-radius - 1, -3, -radius - 1), origin.offset(radius + 1, 3, radius + 1))) {
            if (!level.getBlockState(p).isAir()) return false;
        }
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz) + random.nextFloat() * 0.6;
                if (r > radius + 0.3) continue;
                BlockPos at = origin.offset(dx, 0, dz);
                set(level, at, deck, random);
                // ribs under it, a cross two deep
                if (dx == 0 || dz == 0) {
                    set(level, at.below(), rib, random);
                    if (Math.abs(dx) + Math.abs(dz) < radius - 1) set(level, at.below(2), rib, random);
                }
            }
        }
        int px = random.nextInt(3) - 1, pz = random.nextInt(3) - 1;
        int height = minHeight + random.nextInt(maxHeight - minHeight + 1);
        for (int y = 1; y <= height; y++) set(level, origin.offset(px * 2, y, pz * 2), post, random);
        return true;
    }

    private static void set(WorldGenLevel level, BlockPos pos, BlockStateProvider provider, RandomSource random) {
        if (!level.getBlockState(pos).isAir()) return;
        BlockState state = provider.getState(level, random, pos);
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
    }
}

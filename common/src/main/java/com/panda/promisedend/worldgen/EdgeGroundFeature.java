package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.PromisedEnd;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * The Unfinished Edge's ground: the land laid out on the Ancients' grid and never finished. Over its whole chunk the
 * tops of {@code #promisedend:edge_ground} on the lines of a {@code cell}-block grid are {@code lattice} (the
 * construction lines), and {@code open} of the cells were never filled in: their ground is gone {@code 1-depth} blocks
 * down between ribs of lattice as deep, so the land is a waffle there. A cell is only opened where the ground under it
 * is at least two blocks thicker than its pit, so a pit never opens onto the void. The grid and which cells are open
 * are fixed by the world's seed and the cell, not the chunk order. Nothing in a chunk a structure has claimed.
 */
public record EdgeGroundFeature(BlockStateProvider lattice, int cell, float open, int depth) implements Feature {
    public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("edge_ground"));
    public static final MapCodec<EdgeGroundFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("lattice").forGetter(EdgeGroundFeature::lattice),
            Codec.intRange(4, 16).fieldOf("cell").forGetter(EdgeGroundFeature::cell),
            Codec.floatRange(0, 1).fieldOf("open").forGetter(EdgeGroundFeature::open),
            Codec.intRange(1, 6).fieldOf("depth").forGetter(EdgeGroundFeature::depth)
    ).apply(i, EdgeGroundFeature::new));

    @Override
    public MapCodec<EdgeGroundFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int x0 = Math.floorDiv(origin.getX(), 16) * 16, z0 = Math.floorDiv(origin.getZ(), 16) * 16;
        if (StructureClaims.claimed(level, List.of(new BlockPos(x0 + 8, 0, z0 + 8)))) return false;
        long seed = level.getSeed();
        boolean placed = false;
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - 1;
                if (y <= level.getMinY()) continue;
                BlockPos top = new BlockPos(x, y, z);
                if (!level.getBlockState(top).is(GROUND)) continue;
                boolean line = Math.floorMod(x, cell) == 0 || Math.floorMod(z, cell) == 0;
                int pit = pit(seed, Math.floorDiv(x, cell), Math.floorDiv(z, cell));
                if (pit > 0 && thickness(level, top, pit + 2) < pit + 2) pit = 0;
                if (line) {
                    for (int d = 0; d < Math.max(1, pit); d++) level.setBlock(top.below(d), lattice.getState(level, random, top.below(d)), Block.UPDATE_CLIENTS);
                    placed = true;
                } else if (pit > 0) {
                    for (int d = 0; d < pit; d++) level.setBlock(top.below(d), Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
                    placed = true;
                }
            }
        }
        return placed;
    }

    /** How deep a cell's pit is: 0 for a cell that was filled in, else 1 to {@code depth}; the same for every chunk. */
    int pit(long seed, int cellX, int cellZ) {
        long h = Mth.getSeed(cellX, 0x6c617474, cellZ) ^ seed;
        h = h * 0x9E3779B97F4A7C15L + 0x632BE59BD9B4E019L;
        h ^= h >>> 29;
        double u = (h >>> 11) * 0x1.0p-53;
        if (u >= open) return 0;
        return 1 + (int) Math.floorMod(h >>> 3, (long) depth);
    }

    /** How many blocks of ground run down from {@code top}, counted up to {@code most}. */
    private static int thickness(WorldGenLevel level, BlockPos top, int most) {
        for (int d = 0; d < most; d++) {
            if (!level.getBlockState(top.below(d)).is(GROUND)) return d;
        }
        return most;
    }
}

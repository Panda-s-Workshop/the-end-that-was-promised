package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.PromisedEnd;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * The Voidscar's worn edges: over its whole chunk, each column's top that is {@code #promisedend:crumbling_edge_ground}
 * and has a fall within {@code edge} blocks of it (a column there whose top is more than {@code drop} blocks lower),
 * and whose ground under it runs out over air within CrumblingVoidstoneBlock.COLUMN blocks (a thin rim, not a thick
 * cliff), becomes {@code crumbling} (Crumbling Voidstone) {@code chance} of the time, down to the air, so an island's
 * rim gives way under a careless step and its middle doesn't. Our biomes are whole chunks, so the chunk is the biome's. Nothing in a chunk
 * a structure has claimed. It reads {@code edge} blocks past its chunk, inside what a feature may read.
 */
public record CrumblingEdgeFeature(BlockStateProvider crumbling, float chance, int edge, int drop) implements Feature {
    public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, PromisedEnd.id("crumbling_edge_ground"));
    public static final MapCodec<CrumblingEdgeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockStateProvider.DIRECT_CODEC.fieldOf("crumbling").forGetter(CrumblingEdgeFeature::crumbling),
            Codec.floatRange(0, 1).fieldOf("chance").forGetter(CrumblingEdgeFeature::chance),
            Codec.intRange(1, 4).fieldOf("edge").forGetter(CrumblingEdgeFeature::edge),
            Codec.intRange(2, 32).fieldOf("drop").forGetter(CrumblingEdgeFeature::drop)
    ).apply(i, CrumblingEdgeFeature::new));

    @Override
    public MapCodec<CrumblingEdgeFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int x0 = Math.floorDiv(origin.getX(), 16) * 16, z0 = Math.floorDiv(origin.getZ(), 16) * 16;
        if (StructureClaims.claimed(level, List.of(new BlockPos(x0 + 8, 0, z0 + 8)))) return false;
        boolean placed = false;
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                if (y <= level.getMinY()) continue;
                BlockPos top = new BlockPos(x, y, z);
                if (!level.getBlockState(top).is(GROUND) || !nearVoid(level, x, y, z) || random.nextFloat() >= chance) continue;
                int depth = thin(level, top);
                if (depth == 0) continue;
                for (int d = 0; d < depth; d++) level.setBlock(top.below(d), crumbling.getState(level, random, top.below(d)), Block.UPDATE_CLIENTS);
                placed = true;
            }
        }
        return placed;
    }

    /** How deep the ground under {@code top} runs before air, if it does within CrumblingVoidstoneBlock.COLUMN; else 0. */
    private static int thin(WorldGenLevel level, BlockPos top) {
        for (int d = 0; d < com.panda.promisedend.block.CrumblingVoidstoneBlock.COLUMN; d++) {
            var state = level.getBlockState(top.below(d));
            if (state.isAir()) return d;
            if (!state.is(GROUND)) return 0;
        }
        return level.getBlockState(top.below(com.panda.promisedend.block.CrumblingVoidstoneBlock.COLUMN)).isAir()
                ? com.panda.promisedend.block.CrumblingVoidstoneBlock.COLUMN : 0;
    }

    /** Whether a column within {@code edge} blocks falls away more than {@code drop} below this top. */
    private boolean nearVoid(WorldGenLevel level, int x, int y, int z) {
        for (int dx = -edge; dx <= edge; dx++) {
            for (int dz = -edge; dz <= edge; dz++) {
                if ((dx != 0 || dz != 0) && level.getHeight(Heightmap.Types.MOTION_BLOCKING, x + dx, z + dz) - 1 < y - drop) return true;
            }
        }
        return false;
    }
}

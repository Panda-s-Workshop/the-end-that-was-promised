package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.block.UpdraftVentBlock;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * An ancient Updraft Vent on an island's edge, where it can carry a player off towards the next island. From the origin
 * it looks over the tops within {@code search} blocks, nearest first (in a random order within each ring), for ground
 * ({@code #promisedend:updraft_vent_ground}) with the void beside it within {@code edge} blocks and ground on at least
 * two of its four sides within a block of its height (an edge, sloped or not, but not a lone spike), and sinks the vent
 * into the top there, unless a vent is already within {@link #SPACING} blocks (the next chunk's search reaches the same
 * edges). {@code search} and {@code edge}, or {@code search} and the spacing, together stay within 16, so it never reads
 * past the chunks next to its own (worldgen can't). The void beside it means nothing within {@link #DROP} blocks
 * below the top. Nothing is built in a chunk a structure has claimed.
 */
public record UpdraftVentFeature(int search, int edge) implements Feature {
    /** How far a column beside the top must fall away, at least, to count as the void. */
    public static final int DROP = 16;
    /** No two vents within this many blocks of each other, across and up or down. */
    public static final int SPACING = 4;

    public static final MapCodec<UpdraftVentFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0, 12).fieldOf("search").forGetter(UpdraftVentFeature::search),
            Codec.intRange(1, 4).fieldOf("edge").forGetter(UpdraftVentFeature::edge)
    ).apply(i, UpdraftVentFeature::new));

    @Override
    public MapCodec<UpdraftVentFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        for (int r = 0; r <= search; r++) {
            List<int[]> ring = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) if (Math.max(Math.abs(dx), Math.abs(dz)) == r) ring.add(new int[] {dx, dz});
            }
            Util.shuffle(ring, random);
            for (int[] d : ring) {
                BlockPos top = top(level, origin.getX() + d[0], origin.getZ() + d[1]);
                if (top == null || !edge(level, top) || spaced(level, top) || StructureClaims.claimed(level, List.of(top))) continue;
                level.setBlock(top, ModBlocks.UPDRAFT_VENT.get().defaultBlockState().setValue(UpdraftVentBlock.ANCIENT, true), 2);
                return true;
            }
        }
        return false;
    }

    /** Whether a vent is already within {@link #SPACING} blocks of {@code top}. */
    private static boolean spaced(WorldGenLevel level, BlockPos top) {
        for (BlockPos p : BlockPos.betweenClosed(top.offset(-SPACING, -SPACING, -SPACING), top.offset(SPACING, SPACING, SPACING))) {
            if (level.getBlockState(p).is(ModBlocks.UPDRAFT_VENT.get())) return true;
        }
        return false;
    }

    /** The column's top block if it is vent ground, else null. */
    private static BlockPos top(WorldGenLevel level, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
        if (y < level.getMinY()) return null;
        BlockPos at = new BlockPos(x, y, z);
        return level.getBlockState(at).is(ModTags.UPDRAFT_VENT_GROUND) ? at : null;
    }

    /** Ground within a block of the top's height on two of its four sides at least, and the void within {@code edge} of it. */
    private boolean edge(WorldGenLevel level, BlockPos top) {
        int ground = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockPos side = top.relative(d);
            if (Math.abs(level.getHeight(Heightmap.Types.MOTION_BLOCKING, side.getX(), side.getZ()) - 1 - top.getY()) <= 1) ground++;
        }
        if (ground < 2) return false;
        for (int dx = -edge; dx <= edge; dx++) {
            for (int dz = -edge; dz <= edge; dz++) {
                if (dx == 0 && dz == 0) continue;
                if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, top.getX() + dx, top.getZ() + dz) - 1 < top.getY() - DROP) return true;
            }
        }
        return false;
    }
}

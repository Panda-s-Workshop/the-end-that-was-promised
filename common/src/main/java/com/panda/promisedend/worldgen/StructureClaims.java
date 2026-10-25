package com.panda.promisedend.worldgen;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;

/**
 * Whether a feature would write into a chunk a structure has claimed. A structure's pieces go down when their own chunk
 * is decorated, which can be after a feature next door has reached in, so the blocks can't tell; every chunk a start's
 * box covers holds its reference from the start, long before features.
 */
public final class StructureClaims {
    private StructureClaims() {}

    /** True if any chunk holding one of {@code positions} (or a block beside one) holds a structure reference. */
    public static boolean claimed(WorldGenLevel level, Iterable<BlockPos> positions) {
        Set<Long> chunks = new HashSet<>();
        for (BlockPos p : positions) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) chunks.add(ChunkPos.pack((p.getX() + dx) >> 4, (p.getZ() + dz) >> 4));
            }
        }
        for (long c : chunks) {
            if (level.getChunk(ChunkPos.getX(c), ChunkPos.getZ(c)).getAllReferences().values().stream().anyMatch(r -> !r.isEmpty())) return true;
        }
        return false;
    }
}

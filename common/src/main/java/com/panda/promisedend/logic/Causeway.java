package com.panda.promisedend.logic;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

/**
 * Where the Ancients' causeway's stones stand (worldgen.CausewayFeature builds them): every {@code every} blocks along
 * the four compass lines through the centre, from {@code from} out to {@code to}, at {@code height} swelling by
 * {@link #SWELL} over {@link #SWELL_LENGTH}. Only the chunk holding a stone's middle lists it.
 */
public record Causeway(int every, int from, int to, int height) {
    /** How far the road rises and falls about its height, and over how long. */
    public static final int SWELL = 3;
    public static final double SWELL_LENGTH = 64;
    private static final int[][] WAYS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};

    /** The middles of the stones whose middle is in chunk ({@code chunkX}, {@code chunkZ}), at the road's height there. */
    public List<BlockPos> stones(int chunkX, int chunkZ) {
        List<BlockPos> out = new ArrayList<>();
        for (int[] way : WAYS) {
            for (int d = from; d <= to; d += every) {
                int x = way[0] * d, z = way[1] * d;
                if (x >> 4 == chunkX && z >> 4 == chunkZ) out.add(new BlockPos(x, height + (int) Math.round(SWELL * Math.sin(d / SWELL_LENGTH)), z));
            }
        }
        return out;
    }
}

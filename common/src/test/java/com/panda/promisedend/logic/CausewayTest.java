package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class CausewayTest {
    private static final Causeway ROAD = new Causeway(12, 160, 1024, 62);

    /** Every chunk the road crosses, each stone once: four lines of (1024 - 160) / 12 + 1 = 73. */
    @Test
    void everyStoneIsBuiltByOneChunkOnly() {
        Set<BlockPos> all = new HashSet<>();
        int count = 0;
        for (int cx = -65; cx <= 64; cx++) {
            for (int cz = -65; cz <= 64; cz++) {
                for (BlockPos p : ROAD.stones(cx, cz)) {
                    count++;
                    all.add(p);
                    assertEquals(cx, p.getX() >> 4);
                    assertEquals(cz, p.getZ() >> 4);
                }
            }
        }
        assertEquals(4 * 73, count);
        assertEquals(count, all.size());
    }

    /** On the compass lines, 12 apart, from 160 to 1024 out, within the swell of 62. */
    @Test
    void stonesKeepToTheLinesAndTheRim() {
        for (int cx = -65; cx <= 64; cx++) {
            for (int cz = -65; cz <= 64; cz++) {
                for (BlockPos p : ROAD.stones(cx, cz)) {
                    assertTrue(p.getX() == 0 || p.getZ() == 0, p.toString());
                    int d = Math.abs(p.getX()) + Math.abs(p.getZ());
                    assertTrue(d >= 160 && d <= 1024 && (d - 160) % 12 == 0, p.toString());
                    assertTrue(Math.abs(p.getY() - 62) <= Causeway.SWELL, p.toString());
                }
            }
        }
    }

    /** Next stones differ in height by a block at most, so the road reads as one line. */
    @Test
    void theRoadSwellsGently() {
        BlockPos last = null;
        for (int cz = 10; cz <= 64; cz++) {
            for (BlockPos p : ROAD.stones(0, cz)) {
                if (last != null) assertTrue(Math.abs(p.getY() - last.getY()) <= 1, last + " to " + p);
                last = p;
            }
        }
    }
}

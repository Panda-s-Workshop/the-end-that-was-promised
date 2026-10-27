package com.panda.promisedend.worldgen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The chunk chance: its share, no empty regions where it should have some, and features apart from each other. */
class ChunkChanceTest {
    private static final long SEED = 1234567L;

    @Test
    void sharesMatchTheChance() {
        for (int chance : new int[] {2, 3, 4, 6}) {
            int hits = 0, n = 0;
            for (int x = -150; x < 150; x++) for (int z = -150; z < 150; z++, n++) if (ChunkChanceFilter.pick(SEED, "f", x, z, chance)) hits++;
            double share = (double) hits / n;
            assertTrue(Math.abs(share - 1.0 / chance) < 0.01, chance + ": " + share);
        }
    }

    /**
     * The failure vanilla's rarity had: regions of 4 by 4 chunks with none of a one-in-two feature. Independent chunks
     * leave one region in 65,536 so, none of 5,625 expected; for one in six (5/6)^16, 5.4 %, about 304 (sd 17).
     */
    @Test
    void noEmptyRegions() {
        int empty2 = 0, empty6 = 0, regions = 0;
        for (int rx = -150; rx < 150; rx += 4) {
            for (int rz = -150; rz < 150; rz += 4, regions++) {
                boolean any2 = false, any6 = false;
                for (int x = rx; x < rx + 4; x++) for (int z = rz; z < rz + 4; z++) {
                    any2 |= ChunkChanceFilter.pick(SEED, "f", x, z, 2);
                    any6 |= ChunkChanceFilter.pick(SEED, "f", x, z, 6);
                }
                if (!any2) empty2++;
                if (!any6) empty6++;
            }
        }
        assertTrue(empty2 <= 1, empty2 + " of " + regions + " regions with none of a one-in-two");
        assertTrue(empty6 >= 236 && empty6 <= 372, empty6 + " of " + regions + " regions with none of a one-in-six (304 expected)");
    }

    @Test
    void featuresAndSeedsDrawApart() {
        int same = 0, sameSeed = 0, n = 0;
        for (int x = 0; x < 100; x++) for (int z = 0; z < 100; z++, n++) {
            boolean a = ChunkChanceFilter.pick(SEED, "grove", x, z, 2);
            if (a == ChunkChanceFilter.pick(SEED, "scar", x, z, 2)) same++;
            if (a == ChunkChanceFilter.pick(SEED + 1, "grove", x, z, 2)) sameSeed++;
        }
        // agreeing about half the time is drawing apart; the same salt and seed always agree
        assertTrue(Math.abs((double) same / n - 0.5) < 0.03, "two features agree in " + same + " of " + n);
        assertTrue(Math.abs((double) sameSeed / n - 0.5) < 0.03, "two seeds agree in " + sameSeed + " of " + n);
        assertEquals(ChunkChanceFilter.pick(SEED, "grove", 7, -3, 3), ChunkChanceFilter.pick(SEED, "grove", 7, -3, 3));
    }
}

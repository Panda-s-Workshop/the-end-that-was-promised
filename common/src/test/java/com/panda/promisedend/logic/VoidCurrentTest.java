package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VoidCurrentTest {
    private static final long[] SEEDS = {0L, 1L, -6022411586323069291L, 8523497328896690331L, 42L};

    @Test
    void theSameSeedGivesTheSameCurrents() {
        for (long seed : SEEDS) {
            for (int i = 0; i < 200; i++) {
                double x = i * 37.3 - 4000, z = i * -91.7 + 2500;
                assertEquals(VoidCurrent.field(seed, x, z), VoidCurrent.field(seed, x, z));
                assertEquals(VoidCurrent.inCurrent(seed, x, z), VoidCurrent.inCurrent(seed, x, z));
            }
        }
        // and another seed gives others: over 2000 columns the two differ about as often as two unrelated maps of that
        // coverage would (a in one and not the other: a(1-b) + b(1-a), some 18 % at a tenth each), at least 3/4 of it
        int differ = 0, one = 0, two = 0;
        for (int i = 0; i < 2000; i++) {
            boolean p = VoidCurrent.inCurrent(1L, i * 13.0, i * 7.0), q = VoidCurrent.inCurrent(2L, i * 13.0, i * 7.0);
            if (p) one++;
            if (q) two++;
            if (p != q) differ++;
        }
        double a = one / 2000.0, b = two / 2000.0, unrelated = 2000 * (a * (1 - b) + b * (1 - a));
        assertTrue(differ >= 0.75 * unrelated && differ >= 200, "two seeds differ in only " + differ + " of 2000 columns, unrelated maps " + (int) unrelated);
    }

    @Test
    void theFieldIsBoundedAndContinuous() {
        for (long seed : SEEDS) {
            double most = 0, step = 0;
            for (int i = 0; i < 20000; i++) {
                double x = (i % 200) * 11.0 - 1100, z = (i / 200) * 11.0 - 1100;
                double n = VoidCurrent.field(seed, x, z);
                most = Math.max(most, Math.abs(n));
                step = Math.max(step, Math.abs(VoidCurrent.field(seed, x + 0.25, z) - n));
            }
            assertTrue(most <= 1.0, "the noise reached " + most);
            assertTrue(step < 0.01, "a quarter block moved the noise by " + step);
        }
    }

    @Test
    void aboutATenthOfTheGroundHasACurrentOverIt() {
        for (long seed : SEEDS) {
            int in = 0, n = 0;
            for (int x = -3000; x < 3000; x += 6) {
                for (int z = -3000; z < 3000; z += 6) {
                    n++;
                    if (VoidCurrent.inCurrent(seed, x, z)) in++;
                }
            }
            double share = in / (double) n;
            System.out.printf("seed %d: currents over %.1f %% of the ground%n", seed, share * 100);
            assertTrue(share >= 0.08 && share <= 0.15, "seed " + seed + ": currents over " + share);
        }
    }

    @Test
    void aCurrentFlowsOneWayAlongItself() {
        for (long seed : SEEDS) {
            // a step of a block along the flow: the way it runs turns a little, a few degrees more only at a rare bend,
            // never sharply (where the noise flattens, at a saddle, it would turn back on itself: that is slack air)
            int pairs = 0, sharp = 0, ends = 0;
            double worst = 0;
            for (int i = 0; i < 200000 && pairs < 2000; i++) {
                double x = (i % 400) * 7.0, z = (i / 400) * 7.0;
                if (!VoidCurrent.inCurrent(seed, x, z)) continue;
                double[] f = VoidCurrent.flow(seed, x, z);
                assertNotNull(f);
                assertEquals(1.0, Math.hypot(f[0], f[1]), 1e-9, "a unit vector");
                double[] g = VoidCurrent.flow(seed, x + f[0], z + f[1]);
                if (g == null) {
                    ends++; // the river's end, at slack air
                    continue;
                }
                double turn = Math.toDegrees(Math.acos(Math.min(1, f[0] * g[0] + f[1] * g[1])));
                worst = Math.max(worst, turn);
                if (turn > 10) sharp++;
                pairs++;
            }
            System.out.printf("seed %d: %d steps, %d turning over 10 degrees, the most %.1f; %d ending%n", seed, pairs, sharp, worst, ends);
            // and a river runs on: a step ends one (at a saddle or a stretch too flat) a few times in a hundred at most
            assertTrue(ends <= pairs / 20, "seed " + seed + ": " + ends + " of " + pairs + " steps ended the river");
            assertTrue(pairs >= 2000, "seed " + seed + ": only " + pairs + " columns in a current");
            assertTrue(sharp <= pairs / 50, "seed " + seed + ": " + sharp + " of " + pairs + " steps turned over 10 degrees");
            assertTrue(worst < 30, "seed " + seed + ": a step turned " + worst + " degrees");
        }
    }

    /** An island 13 across at x 20, its top at 40 and its underside at 30, in otherwise open void. */
    private static int island(int x, int z) {
        return Math.hypot(x - 20, z) <= 6.5 ? 40 : Integer.MIN_VALUE;
    }

    @Test
    void aCurrentWashesAshoreOntoTheNearestEdge() {
        int[] s = VoidCurrent.shore(0, 10, 0, VoidCurrentTest::island);
        assertNotNull(s, "no shore found 14 from an island");
        assertTrue(island(s[2], s[3]) == 40 && s[4] == 40, "the shore isn't the island's top: " + java.util.Arrays.toString(s));
        assertTrue(island(s[0], s[1]) < 10, "the way up isn't open: " + java.util.Arrays.toString(s));
        assertEquals(1, Math.abs(s[0] - s[2]) + Math.abs(s[1] - s[3]), "the open column isn't beside the shore");
        assertTrue(s[2] <= 15, "not the nearest edge: " + java.util.Arrays.toString(s));
        // followed tick by tick from the current: across under nothing, up the open column, onto the top; never inside
        double x = 0.5, y = 10, z = 0.5;
        int ticks = 0;
        for (double[] v; (v = VoidCurrent.ashore(x, y, z, s)) != null; ticks++) {
            x += v[0];
            y += v[1];
            z += v[2];
            boolean inside = island((int) Math.floor(x), (int) Math.floor(z)) == 40 && y >= 30 && y < 40;
            assertFalse(inside, "washed through the island at " + x + ", " + y + ", " + z);
            assertTrue(ticks < VoidCurrent.ASHORE_TICKS, "not ashore in " + VoidCurrent.ASHORE_TICKS + " ticks");
        }
        assertTrue(island((int) Math.floor(x), (int) Math.floor(z)) == 40 && y >= 40, "left at " + x + ", " + y + ", " + z + ", not over the shore");
        // far from any land, none
        assertEquals(null, VoidCurrent.shore(200, 10, 0, VoidCurrentTest::island), "a shore 180 away");
    }

    @Test
    void theBand() {
        assertTrue(VoidCurrent.inBand(0));
        assertTrue(VoidCurrent.inBand(19.9));
        assertFalse(VoidCurrent.inBand(20));
        assertFalse(VoidCurrent.inBand(-0.1));
    }

    @Test
    void aCaughtThingIsCarriedAndHeld() {
        // a fall at 3 a tick into the band's top is slowed each tick, never by more than half its speed at once
        double vy = -3, y = 19;
        for (int t = 0; t < 30; t++) {
            double[] v = VoidCurrent.carry(0, vy, 0, y, 1, 0, VoidCurrent.HOLD_MID);
            assertTrue(Math.abs(v[1] - vy) <= Math.abs(vy) / 2 + VoidCurrent.HOLD_PULL, "the catch jumped from " + vy + " to " + v[1]);
            vy = v[1] - 0.08;
            y += v[1];
        }
        assertTrue(y > VoidCurrent.HOLD_LOW - 4 && y < VoidCurrent.HOLD_HIGH, "after 30 ticks the fall ended at " + y);
        // held: from anywhere in the band, after 60 ticks of a mob's gravity and drag it is between 6 and 14
        for (double start = 1; start < 20; start += 2) {
            double h = start, v = 0, across = 0, moved = 0;
            for (int t = 0; t < 60; t++) {
                double[] next = VoidCurrent.carry(across, v, 0, h, 1, 0, VoidCurrent.HOLD_MID);
                h += next[1];
                moved = next[0];
                v = (next[1] - 0.08) * 0.98;
                across = next[0] * 0.91;
            }
            assertTrue(h >= VoidCurrent.HOLD_LOW && h <= VoidCurrent.HOLD_HIGH, "from " + start + " it ended at " + h);
            assertEquals(VoidCurrent.SPEED, moved, 0.04, "carried at " + moved + " a tick");
        }
    }

    @Test
    void aGliderAlongACurrentGoesFaster() {
        assertArrayEquals(new double[] {0.36, 0}, VoidCurrent.glide(1.2, 0, 1, 0), 1e-9, "along it: 30 %");
        assertArrayEquals(new double[] {0, 0}, VoidCurrent.glide(0, 1.2, 1, 0), 1e-9, "across it: nothing");
        assertArrayEquals(new double[] {0, 0}, VoidCurrent.glide(-1.2, 0, 1, 0), 1e-9, "against it: nothing");
        double[] half = VoidCurrent.glide(1.2 * Math.cos(Math.PI / 3), 1.2 * Math.sin(Math.PI / 3), 1, 0);
        assertEquals(0.18, half[0], 1e-9, "60 degrees off: half of it");
        assertArrayEquals(new double[] {0, 0}, VoidCurrent.glide(0, 0, 1, 0), 1e-9, "standing still: nothing");
    }
}

package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * The Starfall's numbers: the time between falls is the interval ± 25 %, a fall lands 48-160 from its
 * player in any direction, only while a player is past 1,000 from the origin; a crater is 4-6 across its radius, two
 * thirds as deep, holds 3-6 ore.
 */
class StarfallScheduleTest {
    private static final int RUNS = 20000;

    @Test
    void theIntervalIsGiveOrTakeAQuarter() {
        Random random = new Random(1);
        long lo = Long.MAX_VALUE, hi = 0;
        for (int i = 0; i < RUNS; i++) {
            long next = StarfallSchedule.nextIn(random, 48000);
            lo = Math.min(lo, next);
            hi = Math.max(hi, next);
        }
        assertTrue(lo >= 36000 && hi <= 60000, "between 36000 and 60000 ticks: " + lo + " to " + hi);
        assertTrue(lo < 36500 && hi > 59500, "and the whole of that range: " + lo + " to " + hi);
    }

    @Test
    void aFallLandsNearItsPlayerInAnyDirection() {
        Random random = new Random(2);
        boolean[] quarter = new boolean[4];
        for (int i = 0; i < RUNS; i++) {
            double[] at = StarfallSchedule.around(random, 1500, -200);
            double dx = at[0] - 1500, dz = at[1] + 200, d = Math.hypot(dx, dz);
            assertTrue(d >= StarfallSchedule.NEAR - 1e-9 && d <= StarfallSchedule.FAR + 1e-9, "48-160 from the player: " + d);
            quarter[(dx >= 0 ? 0 : 1) + (dz >= 0 ? 0 : 2)] = true;
        }
        for (boolean q : quarter) assertTrue(q, "in every quarter round the player");
    }

    @Test
    void onlyInTheOuterEnd() {
        assertFalse(StarfallSchedule.outer(0, 0));
        assertFalse(StarfallSchedule.outer(700, 700), "990 out: the main island's reach");
        assertTrue(StarfallSchedule.outer(-1001, 0));
        assertTrue(StarfallSchedule.outer(720, 720), "1018 out");
    }

    @Test
    void cratersAndTheirOre() {
        Random random = new Random(3);
        int[] radii = new int[7], ores = new int[7];
        for (int i = 0; i < RUNS; i++) {
            radii[StarfallSchedule.radius(random)]++;
            ores[StarfallSchedule.ores(random)]++;
        }
        for (int r = 4; r <= 6; r++) assertTrue(radii[r] > 0, "a radius of " + r);
        for (int n = 3; n <= 6; n++) assertTrue(ores[n] > 0, n + " ore");
        assertEquals(RUNS, radii[4] + radii[5] + radii[6], "the radius only 4 to 6");
        assertEquals(RUNS, ores[3] + ores[4] + ores[5] + ores[6], "the ore only 3 to 6");
    }

    @Test
    void theSchedulersSteps() {
        assertEquals(StarfallSchedule.Step.IDLE, StarfallSchedule.step(false, true, 100, 500, 0, 2), "turned off: nothing, due or not");
        assertEquals(StarfallSchedule.Step.PLAN, StarfallSchedule.step(true, false, 0, 500, 0, 2), "the first tick plans the first fall, with no one out");
        assertEquals(StarfallSchedule.Step.IDLE, StarfallSchedule.step(true, true, 900, 500, 0, 2), "not yet due");
        assertEquals(StarfallSchedule.Step.IDLE, StarfallSchedule.step(true, false, 100, 500, 0, 2), "due, but no one out there");
        assertEquals(StarfallSchedule.Step.LOOK, StarfallSchedule.step(true, true, 100, 500, 1, 2), "due with someone out: look");
        assertEquals(StarfallSchedule.Step.TOMORROW, StarfallSchedule.step(true, true, 100, 500, 2, 2), "the day's two spent");
        assertEquals(StarfallSchedule.Step.TOMORROW, StarfallSchedule.step(true, true, 100, 500, 0, 0), "a cap of 0: never");
        assertEquals(2, StarfallSchedule.today(30000, 1, 2), "the same day keeps its count");
        assertEquals(0, StarfallSchedule.today(50000, 1, 2), "a new day starts at none");
    }

    @Test
    void theWayToAFall() {
        assertEquals("north", StarfallSchedule.compass(0, -100));
        assertEquals("south", StarfallSchedule.compass(0, 100));
        assertEquals("east", StarfallSchedule.compass(100, 0));
        assertEquals("west", StarfallSchedule.compass(-100, 0));
        assertEquals("northeast", StarfallSchedule.compass(70, -70));
        assertEquals("southwest", StarfallSchedule.compass(-70, 70));
    }

    @Test
    void theBowlIsAsWideAsItsRadiusAndTwoThirdsAsDeep() {
        int r = 6;
        assertTrue(StarfallSchedule.inCrater(0, 0, 0, r), "its middle");
        assertTrue(StarfallSchedule.inCrater(r, 0, 0, r), "its rim, at the surface");
        assertFalse(StarfallSchedule.inCrater(r + 1, 0, 0, r), "past the rim");
        assertTrue(StarfallSchedule.inCrater(0, -4, 0, r), "4 deep at the middle");
        assertFalse(StarfallSchedule.inCrater(0, -5, 0, r), "not 5");
        assertFalse(StarfallSchedule.inCrater(4, -3, 0, r), "the bowl narrows as it goes down");
        assertTrue(StarfallSchedule.inCrater(3, 2, 3, r), "the air over it is cleared, 2 up");
        assertFalse(StarfallSchedule.inCrater(0, 3, 0, r), "not 3");
    }
}

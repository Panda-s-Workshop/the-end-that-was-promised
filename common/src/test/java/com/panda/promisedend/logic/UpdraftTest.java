package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The column lifts 0.12 a tick to 0.8, a glider 0.25 to 1.2, within 1.5 of the centre and under the column's top. The
 * game's order is simulated: an entity moves, takes its gravity and its drag (a mob's 0.08 and 0.98), and then the
 * vent sets its speed for the next tick.
 */
class UpdraftTest {
    private static final double GRAVITY = 0.08, DRAG = 0.98;

    /** One tick for a mob in the column: its own tick, then the vent's. */
    private static double tick(double vy, boolean gliding) {
        return Updraft.lift((vy - GRAVITY) * DRAG, gliding, GRAVITY);
    }

    @Test
    void anEntityRisesToTheCap() {
        double vy = 0;
        int ticks = 0;
        while (vy < Updraft.CAP) {
            double next = tick(vy, false);
            assertTrue(next - vy > Updraft.LIFT - 0.02, "tick " + ticks + " rose by about 0.12, its gravity given back: " + (next - vy));
            assertTrue(next - vy <= Updraft.LIFT + 0.002, "tick " + ticks + " rose by at most 0.12 (and the drag on the gravity given back)");
            vy = next;
            ticks++;
        }
        assertEquals(0.8, vy, 1e-9);
        assertEquals(7, ticks, "from rest, the cap in 7 ticks (0.12 a tick less the drag, the last step short)");
        assertEquals(0.8, tick(vy, false), 1e-9, "held at the cap");
    }

    @Test
    void withoutGravityGivenBackItWouldTakeFarLonger() {
        double vy = 0;
        int ticks = 0;
        while (vy < Updraft.CAP - 1e-9 && ticks < 100) {
            vy = Updraft.lift((vy - GRAVITY) * DRAG, false, 0);
            ticks++;
        }
        assertTrue(ticks > 15, "0.12 less gravity a tick reaches 0.8 only in " + ticks);
    }

    @Test
    void aFallIsTurnedRound() {
        double vy = -1.0;
        int ticks = 0;
        while (vy <= 0) {
            vy = tick(vy, false);
            ticks++;
        }
        assertEquals(8, ticks, "a fall at 1 a tick turns upward in 8 ticks");
    }

    @Test
    void aGliderRisesFasterToAHigherCap() {
        assertEquals(0.25, Updraft.lift(0, true, 0), 1e-9);
        assertEquals(0.33, Updraft.lift(0, true, 0.08), 1e-9);
        assertEquals(1.2, Updraft.lift(1.1, true, 0.08), 1e-9);
        assertEquals(1.2, Updraft.lift(1.2, true, 0.08), 1e-9);
        assertEquals(0.8, Updraft.lift(0.75, false, 0.08), 1e-9, "not gliding: 0.8");
    }

    @Test
    void aFasterRiseIsntSlowed() {
        assertEquals(1.5, Updraft.lift(1.5, false, 0.08), 1e-9);
        assertEquals(2.0, Updraft.lift(2.0, true, 0.08), 1e-9);
    }

    @Test
    void theGravityGivenBackIsWhatTheTickTook() {
        assertEquals(0.08, Updraft.taken(0.08, false, false, false, false, 0), 1e-9, "a mob rising");
        assertEquals(0.08, Updraft.taken(0.08, false, true, false, false, 0), 1e-9, "Slow Falling while rising takes the whole gravity");
        assertEquals(0.01, Updraft.taken(0.08, true, true, false, false, 0), 1e-9, "Slow Falling while falling takes 0.01");
        assertEquals(0, Updraft.taken(0.08, true, false, true, false, 0), 1e-9, "in water, none given back");
        assertEquals(0.02, Updraft.taken(0.08, false, false, false, true, 0), 1e-9, "a level glider takes a quarter");
        assertEquals(0.08, Updraft.taken(0.08, false, false, false, true, 90), 1e-9, "a glider diving straight down takes all of it");
        // under Slow Falling, coming down into a column: each tick turns by 0.12, not 0.19
        double vy = -0.2, next = Updraft.lift((vy - 0.01) * DRAG, false, Updraft.taken(GRAVITY, true, true, false, false, 0));
        assertTrue(next - vy <= Updraft.LIFT + 0.01, "slow falling rose by " + (next - vy));
    }

    @Test
    void particlesGrowWithTheColumn() {
        assertEquals(1, Updraft.puffs(6));
        assertEquals(1, Updraft.puffs(Updraft.HEIGHT_CRAFTED));
        assertEquals(3, Updraft.puffs(Updraft.HEIGHT_ANCIENT));
        assertEquals(3, Updraft.wisps(6));
        assertEquals(8, Updraft.wisps(Updraft.HEIGHT_CRAFTED));
        assertEquals(16, Updraft.wisps(Updraft.HEIGHT_ANCIENT));
    }

    @Test
    void aGlidersColumnIsWider() {
        assertFalse(Updraft.inColumn(2.5, 3, 0, Updraft.HEIGHT_ANCIENT), "2.5 off, outside for a walker");
        assertTrue(Updraft.inColumn(2.5, 3, 0, Updraft.HEIGHT_ANCIENT, true), "2.5 off, inside for a glider");
        assertTrue(Updraft.inColumn(2.1, 3, 2.1, Updraft.HEIGHT_ANCIENT, true), "2.97 off, inside for a glider");
        assertFalse(Updraft.inColumn(2.2, 3, 2.2, Updraft.HEIGHT_ANCIENT, true), "3.11 off, outside for a glider");
        assertFalse(Updraft.inColumn(0, 32, 0, Updraft.HEIGHT_ANCIENT, true), "at the top, outside for a glider too");
    }

    @Test
    void theColumn() {
        assertTrue(Updraft.inColumn(0, 0, 0, Updraft.HEIGHT_CRAFTED), "on the vent");
        assertTrue(Updraft.inColumn(1.5, 3, 0, Updraft.HEIGHT_CRAFTED), "at the radius");
        assertTrue(Updraft.inColumn(1.0, 3, 1.0, Updraft.HEIGHT_CRAFTED), "1.41 off, inside");
        assertFalse(Updraft.inColumn(1.1, 3, 1.1, Updraft.HEIGHT_CRAFTED), "1.56 off, outside");
        assertTrue(Updraft.inColumn(0, 15.9, 0, Updraft.HEIGHT_CRAFTED), "under a crafted vent's top");
        assertFalse(Updraft.inColumn(0, 16, 0, Updraft.HEIGHT_CRAFTED), "at a crafted vent's top");
        assertTrue(Updraft.inColumn(0, 31.9, 0, Updraft.HEIGHT_ANCIENT), "under an ancient vent's top");
        assertFalse(Updraft.inColumn(0, -0.1, 0, Updraft.HEIGHT_ANCIENT), "below the vent's face");
    }
}

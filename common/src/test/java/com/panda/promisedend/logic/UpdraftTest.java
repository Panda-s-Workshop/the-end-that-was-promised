package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The column lifts 0.12 a tick to 0.8, a glider 0.25 to 1.2, within 1.5 of the centre and under the column's top. */
class UpdraftTest {
    @Test
    void anEntityRisesToTheCap() {
        double vy = 0;
        int ticks = 0;
        while (vy < Updraft.CAP) {
            double next = Updraft.lift(vy, false);
            assertTrue(next > vy, "tick " + ticks + " rose");
            assertTrue(next - vy <= Updraft.LIFT + 1e-9, "tick " + ticks + " rose by at most 0.12");
            vy = next;
            ticks++;
        }
        assertEquals(0.8, vy, 1e-9);
        assertEquals(7, ticks, "from rest, the cap in 7 ticks (0.12 a tick, the last step short)");
        assertEquals(0.8, Updraft.lift(vy, false), 1e-9, "held at the cap");
    }

    @Test
    void aFallIsTurnedRound() {
        assertEquals(-0.88, Updraft.lift(-1.0, false), 1e-9);
        double vy = -1.0;
        int ticks = 0;
        while (vy <= 0) {
            vy = Updraft.lift(vy, false);
            ticks++;
        }
        assertEquals(9, ticks, "a fall at 1 a tick turns upward in 9 ticks");
    }

    @Test
    void aGliderRisesFasterToAHigherCap() {
        assertEquals(0.25, Updraft.lift(0, true), 1e-9);
        assertEquals(1.2, Updraft.lift(1.1, true), 1e-9);
        assertEquals(1.2, Updraft.lift(1.2, true), 1e-9);
        assertEquals(0.8, Updraft.lift(0.75, false), 1e-9, "not gliding: 0.8");
    }

    @Test
    void aFasterRiseIsntSlowed() {
        assertEquals(1.5, Updraft.lift(1.5, false), 1e-9);
        assertEquals(2.0, Updraft.lift(2.0, true), 1e-9);
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

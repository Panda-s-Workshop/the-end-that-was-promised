package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LatticeBoostTest {
    @Test
    void firstBoostIsReady() {
        assertTrue(LatticeBoost.ready(LatticeBoost.NEVER, 0));
        assertTrue(LatticeBoost.ready(LatticeBoost.NEVER, 123456));
    }

    @Test
    void waitsThirtySeconds() {
        assertEquals(600, LatticeBoost.COOLDOWN);
        long at = 1000;
        assertFalse(LatticeBoost.ready(at, at));
        assertFalse(LatticeBoost.ready(at, at + 599));
        assertTrue(LatticeBoost.ready(at, at + 600));
        assertEquals(1, LatticeBoost.left(at, at + 599));
        assertEquals(0, LatticeBoost.left(at, at + 5000));
    }

    @Test
    void secondsRoundUp() {
        assertEquals(30, LatticeBoost.seconds(LatticeBoost.left(0, 0)));
        assertEquals(1, LatticeBoost.seconds(1));
        assertEquals(0, LatticeBoost.seconds(0));
    }

    @Test
    void aClockGoneBackIsReady() {
        // a new world's clock behind the last boost (the map is kept per server, but a dimension's time can be set)
        assertTrue(LatticeBoost.ready(5000, 100));
    }
}

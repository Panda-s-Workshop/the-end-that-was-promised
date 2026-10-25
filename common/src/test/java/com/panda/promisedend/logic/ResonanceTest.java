package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Every fourth hit on one target within 3 s of the last rings; anything else starts the count again. */
class ResonanceTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2);

    @Test
    void theFourthHitRingsAndTheCountStartsAgain() {
        Resonance r = new Resonance();
        for (int round = 0; round < 3; round++) {
            for (int k = 1; k <= 3; k++) assertFalse(r.hit(A, round * 40L + k * 10), "hit " + k + " of round " + round);
            assertTrue(r.hit(A, round * 40L + 40), "the fourth hit of round " + round);
            assertEquals(0, r.count());
        }
    }

    @Test
    void anotherTargetStartsTheCount() {
        Resonance r = new Resonance();
        r.hit(A, 0);
        r.hit(A, 10);
        r.hit(A, 20);
        assertFalse(r.hit(B, 30), "the fourth hit, on another target");
        assertEquals(1, r.count());
        r.hit(B, 40);
        r.hit(B, 50);
        assertTrue(r.hit(B, 60), "four on the new target");
    }

    @Test
    void aGapLongerThanTheWindowStartsTheCount() {
        Resonance r = new Resonance();
        r.hit(A, 0);
        r.hit(A, 10);
        r.hit(A, 20);
        assertFalse(r.hit(A, 20 + Resonance.WINDOW + 1), "61 ticks after the third");
        assertEquals(1, r.count());
        // exactly the window still counts
        Resonance s = new Resonance();
        s.hit(A, 0);
        s.hit(A, Resonance.WINDOW);
        s.hit(A, 2L * Resonance.WINDOW);
        assertTrue(s.hit(A, 3L * Resonance.WINDOW), "each hit 60 ticks after the last");
    }

    @Test
    void wouldRingAgreesWithHitAndCountsNothing() {
        Resonance r = new Resonance();
        long t = 0;
        for (int k = 1; k <= 9; k++) {
            t += 15;
            boolean peek = r.wouldRing(A, t);
            assertEquals(peek, r.wouldRing(A, t), "a peek changes nothing");
            assertEquals(peek, r.hit(A, t), "hit " + k);
        }
        assertFalse(r.wouldRing(B, t + 1), "another target never rings at once");
        r.hit(A, 200);
        r.hit(A, 210);
        r.hit(A, 220);
        assertFalse(r.wouldRing(A, 220 + Resonance.WINDOW + 1), "after a long gap");
        assertTrue(r.wouldRing(A, 230), "the fourth within the window");
    }

    @Test
    void theDesignNumbers() {
        assertEquals(4, Resonance.EVERY);
        assertEquals(60, Resonance.WINDOW);
        assertEquals(2.0F, Resonance.BONUS_DAMAGE, 1e-6);
        assertEquals(1, Resonance.SLOWNESS_AMPLIFIER);
        assertEquals(20, Resonance.SLOWNESS_TICKS);
    }
}

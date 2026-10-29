package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** A shrine's favour, its three choices of six, and its wait. */
class OfferingTest {
    @Test
    void favourAddsUpToTheAsk() {
        int f = 0;
        f = Offerings.add(f, 1);
        f = Offerings.add(f, 1);
        assertFalse(Offerings.ready(f), "two favour");
        f = Offerings.add(f, 1);
        assertTrue(Offerings.ready(f), "three favour");
        assertEquals(3, f);
        // one Ancient Core is enough by itself, and what is over stays, up to a cap
        assertTrue(Offerings.ready(Offerings.add(0, 5)));
        assertEquals(Offerings.ASK + Offerings.MOST - 1, Offerings.add(6, 5));
        assertEquals(2, Offerings.add(2, -4), "nothing is taken away");
    }

    @Test
    void threeDistinctChoicesOfSix() {
        int[] seen = new int[Offerings.BOONS.length];
        for (long seed = 0; seed < 2000; seed++) {
            int[] c = Offerings.choices(seed);
            assertEquals(Offerings.CHOICES, c.length);
            Set<Integer> distinct = new HashSet<>();
            for (int i : c) {
                assertTrue(i >= 0 && i < Offerings.BOONS.length, "index " + i);
                distinct.add(i);
                seen[i]++;
            }
            assertEquals(Offerings.CHOICES, distinct.size(), "seed " + seed + " repeats a boon");
        }
        // every boon comes up about half the time (3 of 6): 1000 of 2000, give or take
        for (int i = 0; i < seen.length; i++) assertTrue(seen[i] > 850 && seen[i] < 1150, Offerings.BOONS[i] + " offered " + seen[i] + " times");
    }

    @Test
    void theSameSeedTheSameChoices() {
        assertArrayEquals(Offerings.choices(42), Offerings.choices(42));
        long a = Offerings.seed(1, 2, 3), b = Offerings.seed(1, 2, 3);
        assertEquals(a, b);
        // another altar, player or moment, another draw (most of the time)
        int differ = 0;
        for (long t = 0; t < 100; t++) {
            if (!java.util.Arrays.equals(Offerings.choices(Offerings.seed(1, 2, t)), Offerings.choices(Offerings.seed(1, 2, t + 1)))) differ++;
        }
        assertTrue(differ > 80, differ + " of 100 changed");
    }

    @Test
    void theWait() {
        assertEquals(0, Offerings.waitLeft(-1, 100, 24000), "never taken one");
        assertEquals(23900, Offerings.waitLeft(100, 200, 24000));
        assertEquals(0, Offerings.waitLeft(100, 24100, 24000));
        assertEquals(0, Offerings.waitLeft(100, 99999, 24000));
    }
}

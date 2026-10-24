package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Dragon Wrath's numbers: health 200 x (1 + 0.25 x Wrath), 3 + Wrath scales, one more a kill up to the cap. */
class WrathTest {
    @Test
    void healthByWrath() {
        assertEquals(200.0F, DragonWrath.maxHealth(0), "the first fight is vanilla's 200");
        assertEquals(250.0F, DragonWrath.maxHealth(1));
        assertEquals(350.0F, DragonWrath.maxHealth(3));
        assertEquals(450.0F, DragonWrath.maxHealth(5));
        assertEquals(450.0F, DragonWrath.maxHealth(9), "clamped at 5");
        assertEquals(2.25F, DragonWrath.maxHealth(5) / DragonWrath.maxHealth(0), 1e-6, "Wrath 5 has 2.25 times the health");
    }

    @Test
    void scalesByWrath() {
        assertEquals(3, DragonWrath.scales(0));
        assertEquals(4, DragonWrath.scales(1));
        assertEquals(6, DragonWrath.scales(3));
        assertEquals(8, DragonWrath.scales(5));
    }

    @Test
    void aKillRaisesItUpToTheCap() {
        assertEquals(1, DragonWrath.next(0, 5));
        assertEquals(5, DragonWrath.next(4, 5));
        assertEquals(5, DragonWrath.next(5, 5), "never past 5");
        assertEquals(2, DragonWrath.next(2, 2), "held under dragon.max_wrath");
        assertEquals(0, DragonWrath.next(0, 0), "a cap of 0 keeps every fight the first");
        assertEquals(3, DragonWrath.next(7, 3), "a stored value over a lowered cap comes back under it");
    }

    @Test
    void theFightIsAtTheEffectiveWrath() {
        assertEquals(3, DragonWrath.effective(3, true, 5));
        assertEquals(2, DragonWrath.effective(4, true, 2), "the cap applies to a value stored before it was lowered");
        assertEquals(0, DragonWrath.effective(4, false, 5), "with dragon.wrath off every fight is Wrath 0");
    }

    @Test
    void aHoardTablePerLevel() {
        for (int w = 0; w <= DragonWrath.MAX; w++)
            assertEquals("promisedend:chests/dragon_hoard/wrath_" + w, DragonWrath.hoard(w).identifier().toString());
        assertEquals("promisedend:chests/dragon_hoard/wrath_5", DragonWrath.hoard(8).identifier().toString(), "clamped");
    }
}

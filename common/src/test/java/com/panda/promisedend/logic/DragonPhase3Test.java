package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** When the dragon enters phase 3: below a quarter of its health, once, and only with the setting on. */
class DragonPhase3Test {
    @Test
    void entersBelowAQuarterOnce() {
        assertFalse(DragonPhase3.shouldEnter(200, 200, false, true), "full health");
        assertFalse(DragonPhase3.shouldEnter(50, 200, false, true), "exactly a quarter");
        assertTrue(DragonPhase3.shouldEnter(49.5F, 200, false, true), "just under a quarter");
        assertTrue(DragonPhase3.shouldEnter(1, 200, false, true), "nearly dead");
        assertFalse(DragonPhase3.shouldEnter(10, 200, true, true), "already in phase 3");
        assertFalse(DragonPhase3.shouldEnter(0, 200, false, true), "dying");
    }

    @Test
    void theSettingTurnsItOff() {
        assertFalse(DragonPhase3.shouldEnter(10, 200, false, false));
    }

    @Test
    void theMddsNumbers() {
        assertEquals(400, DragonPhase3.DIVE_INTERVAL, "a dive every 20 s");
        assertEquals(30, DragonPhase3.TELEGRAPH_TICKS, "a 1.5 s telegraph");
        assertEquals(120, DragonPhase3.BREATH_TICKS, "the breath lasts 6 s");
        assertEquals(2.0F, DragonPhase3.BREATH_DAMAGE, "2 a second");
    }

    @Test
    void wardensByWrath() {
        // 3 at the first fight and Wrath 1, 4 at 3, 5 at 5
        assertEquals(3, DragonPhase3.wardenCount(0));
        assertEquals(3, DragonPhase3.wardenCount(1));
        assertEquals(4, DragonPhase3.wardenCount(3));
        assertEquals(5, DragonPhase3.wardenCount(5));
        for (int w = 0; w < 5; w++) assertTrue(DragonPhase3.wardenCount(w) <= DragonPhase3.wardenCount(w + 1), "never fewer with more Wrath");
        assertEquals(3, DragonPhase3.wardenCount(-1), "clamped below");
        assertEquals(5, DragonPhase3.wardenCount(9), "clamped above");
    }
}

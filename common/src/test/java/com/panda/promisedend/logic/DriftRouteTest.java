package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class DriftRouteTest {
    private static final DriftRoute ROUTE = new DriftRoute(0, 64, 0, 30, 64, 0);

    @Test
    void waitsThenCrossesAtThreeBlocksASecond() {
        var state = DriftRoute.State.start();
        for (int t = 0; t < DriftRoute.WAIT; t++) {
            ROUTE.step(state);
            assertEquals(0, state.s, "still at the first mooring on tick " + t);
        }
        // 30 blocks at 0.15 a tick: 200 ticks, 10 s
        assertEquals(200, ROUTE.crossing());
        for (int t = 0; t < 199; t++) ROUTE.step(state);
        assertTrue(state.s < 1 && state.s > 0.99, "one step short of the far end: " + state.s);
        ROUTE.step(state);
        assertEquals(1, state.s);
        assertEquals(-1, state.dir);
        assertEquals(DriftRoute.WAIT, state.wait);
    }

    @Test
    void roundTripIsTwoCrossingsAndTwoWaits() {
        var state = DriftRoute.State.start();
        int ticks = 0;
        do {
            ROUTE.step(state);
            ticks++;
        } while (!(state.s == 0 && state.dir == 1 && state.wait == DriftRoute.WAIT));
        assertEquals(2 * DriftRoute.WAIT + 2 * ROUTE.crossing(), ticks);
    }

    @Test
    void blockedTurnsBackAtOnce() {
        var state = new DriftRoute.State(0.5, 1, 0);
        DriftRoute.turn(state);
        ROUTE.step(state);
        assertEquals(0.5 - DriftRoute.SPEED / 30, state.s, 1e-9);
        assertEquals(0, state.wait, "a turn in the void doesn't wait");
    }

    @Test
    void pointsAlongTheLine() {
        assertArrayEquals(new double[] {15, 64, 0}, ROUTE.at(0.5), 1e-9);
        assertArrayEquals(new double[] {30, 64, 0}, ROUTE.at(1), 1e-9);
    }
}

package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ArrivalTest {
    @Test
    void directionKeptDistanceBanded() {
        // a first-ring stronghold 1600 out to the north-east: the same direction, the same distance
        double[] t = Arrival.target(1131.4, -1131.4);
        assertEquals(1600, Math.hypot(t[0], t[1]), 0.5);
        assertEquals(Math.atan2(-1131.4, 1131.4), Math.atan2(t[1], t[0]), 1e-9);
        // near the centre: out to the band's near edge; far out: in to its far edge, never at the Edge
        assertEquals(Arrival.NEAREST, Math.hypot(Arrival.target(30, 40)[0], Arrival.target(30, 40)[1]), 1e-6);
        double[] far = Arrival.target(-20000, 5000);
        assertEquals(Arrival.FARTHEST, Math.hypot(far[0], far[1]), 1e-6);
        assertEquals(Math.atan2(5000, -20000), Math.atan2(far[1], far[0]), 1e-9);
        // the centre itself: due east
        assertArrayEquals(new double[] {Arrival.NEAREST, 0}, Arrival.target(0, 0), 1e-9);
    }

    @Test
    void samePortalSamePoint() {
        assertArrayEquals(Arrival.target(1503.5, -287.5), Arrival.target(1503.5, -287.5));
    }

    @Test
    void searchNearestFirstWithinReach() {
        int last = -1;
        for (int[] o : Arrival.ORDER) {
            int d = o[0] * o[0] + o[1] * o[1];
            assertTrue(d >= last, "the search order goes out");
            assertTrue(d <= Arrival.SEARCH * Arrival.SEARCH);
            last = d;
        }
        assertArrayEquals(new int[] {0, 0}, Arrival.ORDER.getFirst());
    }

    @Test
    void facesTheCentre() {
        // yaw 0 looks south (+z), 90 west (-x), 180 north, -90 east
        assertEquals(180, Math.abs(Arrival.facingCentre(0, 1500)), 1e-4);
        assertEquals(0, Arrival.facingCentre(0, -1500), 1e-4);
        assertEquals(90, Arrival.facingCentre(1500, 0), 1e-4);
        assertEquals(-90, Arrival.facingCentre(-1500, 0), 1e-4);
    }

    @Test
    void compassNamesTheWayIn() {
        // from a Waystation, the way to the centre is minus its position: north of the centre (-z) looks south
        assertEquals("south", Arrival.compass(0, 1500));
        assertEquals("north", Arrival.compass(0, -1500));
        assertEquals("east", Arrival.compass(1500, 0));
        assertEquals("west", Arrival.compass(-1500, 0));
        assertEquals("northwest", Arrival.compass(-1000, -1000));
        assertEquals("southeast", Arrival.compass(1000, 1100));
    }

    @Test
    void wardIsACylinder() {
        assertTrue(Arrival.insideWard(639, 0, 640));
        assertFalse(Arrival.insideWard(640, 0, 640));
        assertTrue(Arrival.insideWard(450, 450, 640));
        assertFalse(Arrival.insideWard(460, 460, 640));
    }
}

package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The Purpur Sentry's shield: the 60 degree half angle's edges, for blows and for shots, and the steep shot over it. */
class ShieldArcTest {
    private static final double HALF = 60.0;

    /** (dx, dz) at {@code degrees} off the front of a body facing south (+z). */
    private static double[] off(double degrees) {
        double r = Math.toRadians(degrees);
        return new double[] {Math.sin(r), Math.cos(r)};
    }

    @Test
    void blowsWithinSixtyDegreesAreStopped() {
        for (double side : new double[] {1, -1}) {
            double[] in = off(59 * side), out = off(61 * side);
            assertTrue(ShieldArc.inFront(0, HALF, in[0], in[1]), "59 degrees off");
            assertFalse(ShieldArc.inFront(0, HALF, out[0], out[1]), "61 degrees off");
        }
        assertFalse(ShieldArc.inFront(0, HALF, 0, -3), "from behind");
        // facing east (-90), a blow from the east is in front and one from the south isn't
        assertTrue(ShieldArc.inFront(-90, HALF, 3, 0));
        assertFalse(ShieldArc.inFront(-90, HALF, 0, 3));
        // the wrap round 180
        assertTrue(ShieldArc.inFront(179, HALF, 0, -3));
        assertTrue(ShieldArc.inFront(-179, HALF, 0, -3));
    }

    @Test
    void shotsCountByWhereTheyCameFrom() {
        // facing south: a shot from the south travels north (-z)
        assertTrue(ShieldArc.stopsShot(0, HALF, 0, 0, -1.5));
        assertFalse(ShieldArc.stopsShot(0, HALF, 0, 0, 1.5), "from behind");
        double[] in = off(59), out = off(61);
        assertTrue(ShieldArc.stopsShot(0, HALF, -in[0], 0, -in[1]), "59 degrees off");
        assertFalse(ShieldArc.stopsShot(0, HALF, -out[0], 0, -out[1]), "61 degrees off");
    }

    @Test
    void aSteepShotComesOverTheRim() {
        double flat = 1.0;
        double at59 = Math.tan(Math.toRadians(59)) * flat, at61 = Math.tan(Math.toRadians(61)) * flat;
        assertTrue(ShieldArc.stopsShot(0, HALF, 0, -at59, -flat), "59 degrees down");
        assertFalse(ShieldArc.stopsShot(0, HALF, 0, -at61, -flat), "61 degrees down");
        assertFalse(ShieldArc.stopsShot(0, HALF, 0, -2, 0), "straight down");
    }
}

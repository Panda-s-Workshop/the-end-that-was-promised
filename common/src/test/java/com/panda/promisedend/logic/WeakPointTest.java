package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WeakPointTest {
    private static final double E = 1.0E-9;

    @Test
    void forwardFollowsYaw() {
        // yaw 0 faces +z, 90 faces -x, 180 faces -z
        double[] south = WeakPoint.bodyPoint(0, 0, 0, 0.0F, 0, 2.5, 0.625);
        assertEquals(0.0, south[0], E);
        assertEquals(2.5, south[1], E);
        assertEquals(0.625, south[2], E);
        double[] west = WeakPoint.bodyPoint(10, 64, 10, 90.0F, 0, 0, 1);
        assertEquals(9.0, west[0], E);
        assertEquals(10.0, west[2], E);
        double[] north = WeakPoint.bodyPoint(0, 0, 0, 180.0F, 0, 0, 1);
        assertEquals(-1.0, north[2], E);
    }

    @Test
    void rightIsAQuarterClockwise() {
        // facing +z (south), the body's right is -x (west)
        double[] right = WeakPoint.bodyPoint(0, 0, 0, 0.0F, 1, 0, 0);
        assertEquals(-1.0, right[0], E);
        assertEquals(0.0, right[2], E);
    }

    @Test
    void segmentDistance() {
        assertEquals(1.0, WeakPoint.toSegment(0, 1, 0, -5, 0, 0, 5, 0, 0), E);
        // past the end: to the end point
        assertEquals(5.0, WeakPoint.toSegment(8, 4, 0, 0, 0, 0, 5, 0, 0), E);
        // a point segment
        assertEquals(Math.sqrt(3), WeakPoint.toSegment(1, 1, 1, 0, 0, 0, 0, 0, 0), E);
    }

    @Test
    void aBlowAimedAtTheKeystoneLandsAndOneBesideItDoesnt() {
        // the Architect at the origin facing +z; its keystone 2.5 up and 0.625 out (TheArchitect.KEYSTONE_*)
        double[] key = WeakPoint.bodyPoint(0, 0, 0, 0.0F, 0, 2.5, 0.625);
        // a player 3 out in front, eye 1.62 up, looking at the keystone
        double ex = 0, ey = 1.62, ez = 3.5;
        double dx = key[0] - ex, dy = key[1] - ey, dz = key[2] - ez, len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double reach = 8.0;
        assertTrue(WeakPoint.toSegment(key[0], key[1], key[2], ex, ey, ez, ex + dx / len * reach, ey + dy / len * reach, ez + dz / len * reach) < 0.01);
        // the same player looking at its mast, a block and a half lower
        double ly = 1.0 - ey, ll = Math.sqrt(dz * dz + ly * ly);
        assertTrue(WeakPoint.toSegment(key[0], key[1], key[2], ex, ey, ez, ex, ey + ly / ll * reach, ez + dz / ll * reach) > 0.7);
    }
}

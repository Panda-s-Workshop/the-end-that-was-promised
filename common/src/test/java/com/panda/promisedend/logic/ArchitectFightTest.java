package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.panda.promisedend.entity.LatticeShard;
import com.panda.promisedend.entity.TheArchitect;
import org.junit.jupiter.api.Test;

class ArchitectFightTest {
    @Test
    void phasesGoForwardOnly() {
        assertEquals(0, ArchitectFight.phaseFor(0.61F, 0));
        assertEquals(1, ArchitectFight.phaseFor(0.6F, 0));
        assertEquals(1, ArchitectFight.phaseFor(0.26F, 1));
        assertEquals(2, ArchitectFight.phaseFor(0.25F, 1));
        // one blow past both thresholds starts the last
        assertEquals(2, ArchitectFight.phaseFor(0.1F, 0));
        // healed (or read early), it never goes back
        assertEquals(2, ArchitectFight.phaseFor(0.9F, 2));
    }

    @Test
    void healthSharesAddUp() {
        assertEquals(0.4, ArchitectFight.healthShare(0), 1e-6);
        assertEquals(0.35, ArchitectFight.healthShare(1), 1e-6);
        assertEquals(0.25, ArchitectFight.healthShare(2), 1e-6);
    }

    @Test
    void cageOpensAfterThrowsOnly() {
        // phase 1: a wall and two throws, each followed by 50; open for the 8 after each throw and its rest
        assertEquals(116.0 / 210.0, ArchitectFight.openShare(0), 1e-9);
        assertEquals(116.0 / 230.0, ArchitectFight.openShare(1), 1e-9);
        // the last: one throw in three works, with the shorter rest
        assertEquals(43.0 / 185.0, ArchitectFight.openShare(2), 1e-9);
    }

    /**
     * A throw at a target 3 to 24 out goes over the wall it raised before them: from its keystone ({@code HOVER} and
     * {@code KEYSTONE_UP} over the tiles, leaning toward them) to their body (0.72 up), it is over the wall's top (and
     * the shard's half) all the way across the wall's block, {@code WALL_OUT} short of them (half the way, when nearer).
     */
    @Test
    void throwGoesOverItsWalls() {
        for (double out = 3; out <= 24; out += 1) {
            double from = Math.min(out * TheArchitect.LEAN, TheArchitect.LEASH) + TheArchitect.KEYSTONE_FORWARD;
            double up = TheArchitect.HOVER + TheArchitect.KEYSTONE_UP;
            double wall = out - Math.min(TheArchitect.WALL_OUT, out * 0.5);
            double[] v = ArchitectFight.lob(out - from, 0.72 - up, 0, LatticeShard.SPEED, LatticeShard.GRAVITY);
            double x = from, y = up, vx = v[0], vy = v[1], low = Double.MAX_VALUE;
            for (int tick = 0; tick < 200 && x < out; tick++) {
                vy -= LatticeShard.GRAVITY;
                vx *= ArchitectFight.DRAG;
                vy *= ArchitectFight.DRAG;
                x += vx;
                y += vy;
                if (Math.abs(x - wall) <= 0.5 + 0.125) low = Math.min(low, y);
            }
            assertTrue(low > TheArchitect.WALL_HEIGHT + 0.125, "a throw at " + out + " out is " + low + " over the tiles at its wall");
        }
    }

    /** A throw flown as vanilla flies a thrown item (fall, slow, move) comes down where it was aimed. */
    @Test
    void lobLandsOnItsMark() {
        double gravity = LatticeShard.GRAVITY, speed = LatticeShard.SPEED;
        for (double out : new double[] {4, 10, 16, 24}) {
            for (double[] to : new double[][] {{out, -2.8, 0}, {out * 0.6, 0.5, out * 0.8}}) {
                double[] v = ArchitectFight.lob(to[0], to[1], to[2], speed, gravity);
                double x = 0, y = 0, z = 0, vx = v[0], vy = v[1], vz = v[2], miss = Double.NaN;
                for (int tick = 0; tick < 200; tick++) {
                    vy -= gravity;
                    vx *= ArchitectFight.DRAG;
                    vy *= ArchitectFight.DRAG;
                    vz *= ArchitectFight.DRAG;
                    double px = x, py = y, pz = z;
                    x += vx;
                    y += vy;
                    z += vz;
                    if (vy < 0 && y <= to[1]) {
                        double f = (py - to[1]) / (py - y);
                        miss = Math.hypot(px + (x - px) * f - to[0], pz + (z - pz) * f - to[2]);
                        break;
                    }
                }
                assertTrue(miss < 0.05, "a throw " + out + " out missed by " + miss);
            }
        }
    }
}

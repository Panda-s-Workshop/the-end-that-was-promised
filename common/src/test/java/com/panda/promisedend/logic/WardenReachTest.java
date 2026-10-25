package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.panda.promisedend.entity.CrystalWarden;
import org.junit.jupiter.api.Test;

/** How far a called warden leaves its pillar for a target out of its beam. */
class WardenReachTest {
    @Test
    void staysHomeWithTheTargetInItsBeam() {
        assertEquals(0, WardenReach.outFromHome(5));
        assertEquals(0, WardenReach.outFromHome(WardenReach.STAND_OFF));
    }

    @Test
    void closesInToTheStandOffWithinItsLeash() {
        assertEquals(8, WardenReach.outFromHome(WardenReach.STAND_OFF + 8), 1e-9);
        assertEquals(CrystalWarden.CALLED_LEASH, WardenReach.outFromHome(CrystalWarden.CALLED_LEASH + WardenReach.STAND_OFF + 1), 1e-9,
                "never past its leash");
    }

    @Test
    void doesNotChaseWhatItCouldNotBeamFromItsLeash() {
        assertEquals(-1, WardenReach.outFromHome(CrystalWarden.CALLED_LEASH + WardenReach.REACH + 0.5));
        assertTrue(WardenReach.REACH < CrystalWarden.BEAM_RANGE, "from the end of its leash the target is in its beam");
    }

    @Test
    void everyHomeReachesThePodiumsRim() {
        // homes are 4 out from a pillar's side, pillars 42 from the portal with a radius of 2 to 5: 33 to 36 out;
        // a player on the podium's rim is up to 6 further, anywhere round it
        for (double home = 33; home <= 36; home += 0.5) {
            for (double angle = 0; angle < 360; angle += 15) {
                double a = Math.toRadians(angle);
                double distance = Math.hypot(home - 6 * Math.cos(a), 6 * Math.sin(a));
                double out = WardenReach.outFromHome(distance);
                assertTrue(out > 0, "a home " + home + " out doesn't go for a player " + distance + " away");
                assertTrue(distance - out <= WardenReach.REACH, "a home " + home + " out stops " + (distance - out) + " short");
                assertTrue(distance <= CrystalWarden.CALLED_FOLLOW_RANGE, "a home " + home + " out doesn't see a player " + distance + " away");
            }
        }
    }
}

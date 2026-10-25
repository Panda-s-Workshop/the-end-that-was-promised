package com.panda.promisedend.logic;

import com.panda.promisedend.entity.CrystalWarden;

/**
 * How far a called Crystal Warden leaves its pillar for a target out of its beam. It goes along the flat line from its
 * home towards the target until the target is {@link #STAND_OFF} away, no further than its leash; a target it couldn't
 * beam even from the end of its leash isn't chased at all, so it stays by its pillar instead of waiting at the leash.
 * Plain numbers, so it is tested without a world.
 */
public final class WardenReach {
    /** How near it comes: a little inside its beam, so a step back by the target doesn't end the beam. */
    public static final double STAND_OFF = CrystalWarden.BEAM_RANGE - 4.0;
    /** How far past the stand-off the target may be at the end of the leash and still be beamed from there. */
    public static final double REACH = CrystalWarden.BEAM_RANGE - 2.0;

    private WardenReach() {}

    /**
     * How far out from its home, along the flat line to a target that far away, the warden should be: 0 when the
     * target is already in its beam from home, -1 when even the leash wouldn't bring it in.
     */
    public static double outFromHome(double distance) {
        if (distance - CrystalWarden.CALLED_LEASH > REACH) return -1;
        double want = distance - STAND_OFF;
        return want <= 0.5 ? 0 : Math.min(CrystalWarden.CALLED_LEASH, want);
    }
}

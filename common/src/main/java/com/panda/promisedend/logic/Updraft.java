package com.panda.promisedend.logic;

/**
 * The Updraft Vent's column: who is in it, and how it lifts them. Each tick an entity in the column rises 0.12 faster,
 * up to 0.8 a tick; a gliding player 0.25 faster, up to 1.2, keeping their horizontal speed. Something already rising
 * faster (a rocket, a jump pad) isn't slowed.
 */
public final class Updraft {
    /** How far from the vent's centre the column reaches, in blocks. */
    public static final double RADIUS = 1.5;
    /** The column over a vent found in the world, and over one a player made. */
    public static final int HEIGHT_ANCIENT = 32, HEIGHT_CRAFTED = 16;
    public static final double LIFT = 0.12, CAP = 0.8;
    public static final double GLIDE_LIFT = 0.25, GLIDE_CAP = 1.2;
    /** Particles go up every this many ticks. */
    public static final int PARTICLES_EVERY = 4;

    private Updraft() {}

    /** The upward speed after a tick in the column, from {@code vy}. */
    public static double lift(double vy, boolean gliding) {
        double cap = gliding ? GLIDE_CAP : CAP;
        if (vy >= cap) return vy;
        return Math.min(vy + (gliding ? GLIDE_LIFT : LIFT), cap);
    }

    /**
     * Whether a point {@code dx}, {@code dz} from the vent's centre and {@code dy} over its top face is in a column
     * {@code height} tall.
     */
    public static boolean inColumn(double dx, double dy, double dz, int height) {
        return dy >= 0 && dy < height && dx * dx + dz * dz <= RADIUS * RADIUS;
    }
}

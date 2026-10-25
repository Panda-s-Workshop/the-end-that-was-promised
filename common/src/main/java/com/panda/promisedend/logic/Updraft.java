package com.panda.promisedend.logic;

/**
 * The Updraft Vent's column: who is in it, and how it lifts them. Each tick an entity in the column rises 0.12 faster,
 * up to 0.8 a tick; a gliding player 0.25 faster, up to 1.2, keeping their horizontal speed. Something already rising
 * faster (a rocket, a jump pad) isn't slowed. A player sneaking in the column sinks slowly instead, the way down.
 *
 * <p>The vent works after the entity has moved and taken its gravity for the tick, so the lift gives that gravity back
 * as well: what the entity rises by grows 0.12 a tick in game, not 0.12 less its gravity.
 */
public final class Updraft {
    /** How far from the vent's centre the column reaches, in blocks. */
    public static final double RADIUS = 1.5;
    /** The column over a vent found in the world, and over one a player made. */
    public static final int HEIGHT_ANCIENT = 32, HEIGHT_CRAFTED = 16;
    public static final double LIFT = 0.12, CAP = 0.8;
    public static final double GLIDE_LIFT = 0.25, GLIDE_CAP = 1.2;
    /** A sneaking player's speed down the column, a tick, whether they were rising or falling faster. */
    public static final double SINK = -0.15;
    /** Particles go up every this many ticks. */
    public static final int PARTICLES_EVERY = 4;
    /** One puff a burst for every this many blocks of column, at least two. */
    public static final int BLOCKS_A_PUFF = 6;

    private Updraft() {}

    /**
     * The speed up for the next tick, from {@code vy}, the entity's after its own tick, and {@code gravity}, what that
     * tick took off it.
     */
    public static double lift(double vy, boolean gliding, double gravity) {
        double cap = gliding ? GLIDE_CAP : CAP;
        if (vy >= cap) return vy;
        return Math.min(vy + gravity + (gliding ? GLIDE_LIFT : LIFT), cap);
    }

    /** Puffs a burst for a column {@code height} tall. */
    public static int puffs(int height) {
        return Math.max(2, height / BLOCKS_A_PUFF);
    }

    /**
     * Whether a point {@code dx}, {@code dz} from the vent's centre and {@code dy} over its top face is in a column
     * {@code height} tall.
     */
    public static boolean inColumn(double dx, double dy, double dz, int height) {
        return dy >= 0 && dy < height && dx * dx + dz * dz <= RADIUS * RADIUS;
    }
}

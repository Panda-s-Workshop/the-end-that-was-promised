package com.panda.promisedend.logic;

/**
 * The Updraft Vent's column: who is in it, and how it lifts them. Each tick an entity in the column rises 0.12 faster,
 * up to 0.8 a tick; a gliding player 0.25 faster, up to 1.2, keeping their horizontal speed, in a column twice as wide. Something already rising
 * faster (a rocket, a jump pad) isn't slowed. A player sneaking in the column sinks slowly instead, the way down.
 *
 * <p>The vent works after the entity has moved and taken its gravity for the tick, so the lift gives that gravity back
 * as well: what the entity rises by grows 0.12 a tick in game, not 0.12 less its gravity.
 */
public final class Updraft {
    /** How far from the vent's centre the column reaches, in blocks. */
    public static final double RADIUS = 1.5;
    /**
     * How far it reaches for a glider: twice as far, so a glider crossing at cruise speed is in it for some 5 ticks,
     * not 2-3, and leaves it climbing.
     */
    public static final double GLIDE_RADIUS = 3.0;
    /** The column over a vent found in the world, and over one a player made. */
    public static final int HEIGHT_ANCIENT = 32, HEIGHT_CRAFTED = 16;
    public static final double LIFT = 0.12, CAP = 0.8;
    public static final double GLIDE_LIFT = 0.25, GLIDE_CAP = 1.2;
    /** A sneaking player's speed down the column, a tick, whether they were rising or falling faster. */
    public static final double SINK = -0.15;
    /** Particles go up every this many ticks. */
    public static final int PARTICLES_EVERY = 4;
    /** One puff a burst for every this many blocks of column, at least one: the puffs are the core's accents. */
    public static final int BLOCKS_A_PUFF = 10;
    /** One wisp of the column's core a burst for every this many blocks of column, at least two. */
    public static final int BLOCKS_A_WISP = 2;
    /** How far from the centre the core's wisps rise. */
    public static final double CORE = 0.45;
    /**
     * How far off an ancient column's particles are drawn; a crafted one's, as most particles, to 32. Past 32 an
     * ancient column sends up a quarter of its core's wisps, one puff and its spark a burst, which is enough to see it
     * by as a line across the void and keeps a field of them light.
     */
    public static final double SEEN_ANCIENT = 96, SEEN = 32;
    /** No puff goes up within this of the camera, so a rider sees the column round them, not a cloud in their face. */
    public static final double CLEAR_OF_CAMERA = 2;
    /**
     * With the camera in the column, puffs go up only further than this under it: the puffs rise slower than a rider,
     * so any over them would be overtaken, and those this far under rise a few blocks in their life and never reach them.
     */
    public static final double CLEAR_OF_RIDER = 5;
    /**
     * The same for the core's wisps, which rise faster and further (some 6-10 blocks in their life): none start within
     * this under a rider's eye, so none rise through it while the rider hovers at the top or sinks.
     */
    public static final double WISP_CLEAR_OF_RIDER = 11;
    /**
     * About how far a wisp rises in its life: none start within this of the column's top, so the drawn air ends about
     * where the lift does.
     */
    public static final int WISP_RISE = 8;

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

    /**
     * The gravity an entity's own tick took off its rise, which the lift gives back: its gravity, but at most 0.01
     * falling under Slow Falling; none in water or lava, whose own pull differs (the lift there is 0.12 less it); and a
     * glider's share at {@code pitch} degrees, 1 - 0.75 cos² of it, as an elytra takes.
     */
    public static double taken(double gravity, boolean falling, boolean slowFalling, boolean inFluid, boolean gliding, float pitch) {
        if (inFluid) return 0;
        double g = falling && slowFalling ? Math.min(gravity, 0.01) : gravity;
        if (!gliding) return g;
        double cos = Math.cos(Math.toRadians(pitch));
        return g * (1 - 0.75 * cos * cos);
    }

    /** Puffs a burst for a column {@code height} tall. */
    public static int puffs(int height) {
        return Math.max(1, height / BLOCKS_A_PUFF);
    }

    /** The core's wisps a burst for a column {@code height} tall. */
    public static int wisps(int height) {
        return Math.max(2, height / BLOCKS_A_WISP);
    }

    /**
     * Whether a point {@code dx}, {@code dz} from the vent's centre and {@code dy} over its top face is in a column
     * {@code height} tall.
     */
    public static boolean inColumn(double dx, double dy, double dz, int height) {
        return inColumn(dx, dy, dz, height, false);
    }

    /** The same for a glider, whose column reaches {@link #GLIDE_RADIUS}. */
    public static boolean inColumn(double dx, double dy, double dz, int height, boolean gliding) {
        double r = gliding ? GLIDE_RADIUS : RADIUS;
        return dy >= 0 && dy < height && dx * dx + dz * dz <= r * r;
    }
}

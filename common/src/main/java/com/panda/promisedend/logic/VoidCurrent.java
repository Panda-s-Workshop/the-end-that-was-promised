package com.panda.promisedend.logic;

/**
 * The void currents' flow field: slow rivers of air at Y 0-20 under the islands, drawn from a 2-D noise of the world's
 * seed hash, with no blocks. A current runs along the noise's zero line, about 6-19 blocks wide (9 or 10 at the middle of the range), and flows along it
 * (across the noise's slope, so one way the whole length of a river); some 8-15 % of the ground has one over it.
 * Something in a current is carried along it at about 0.4 a tick and held between Y 6 and 14 for up to 20 s, then
 * washed ashore onto the nearest island edge in reach ({@link #shore}), or with none let go; a glider along one goes
 * 30 % faster. The server and the client work it out alike from the same seed hash (the
 * one vanilla sends every client for its biome blending), so no packet carries it.
 */
public final class VoidCurrent {
    /** The band the currents run in, and where in it something caught is held. */
    public static final int BAND_BOTTOM = 0, BAND_TOP = 20;
    public static final double HOLD_LOW = 6, HOLD_HIGH = 14, HOLD_MID = 10;
    /** How long something is held before the current lets it go, in ticks. */
    public static final int HOLD_TICKS = 400;
    /** The speed a current carries something at, and how much of the way to it each tick takes. */
    public static final double SPEED = 0.4, CATCH = 0.6;
    /** How fast a caught thing is brought towards the band's middle, at most, a tick. */
    public static final double HOLD_PULL = 0.25;
    /** A glider along a current goes this much faster. */
    public static final double GLIDE_BONUS = 0.3;
    /** How near the noise's zero line a current reaches (the share of the ground with one over it follows from it). */
    public static final double WIDTH = 0.034;
    /**
     * The least slope a current runs on: where the noise flattens (a saddle, where two rivers' lines cross) the way
     * along turns sharply, even back on itself, so there is slack air instead, no current.
     */
    public static final double MIN_SLOPE = 0.0025;
    /** The noise's two scales, in blocks, and the smaller's share. */
    static final double SCALE = 160, DETAIL_SCALE = 56, DETAIL = 0.3;
    /** Mixed into the seed hash, so the currents don't line up with anything else drawn from it. */
    private static final long SALT = 0x5EEDC0DE1EAFL;

    private VoidCurrent() {}

    /** The noise at a point: about -0.9 to 0.9, continuous. */
    public static double field(long seed, double x, double z) {
        long s = seed ^ SALT;
        return perlin(s, x / SCALE, z / SCALE) + DETAIL * perlin(s + 1, x / DETAIL_SCALE, z / DETAIL_SCALE);
    }

    /** Whether a current runs over this column. */
    public static boolean inCurrent(long seed, double x, double z) {
        return Math.abs(field(seed, x, z)) < WIDTH && flow(seed, x, z) != null;
    }

    /**
     * Which way the current runs at this column, a unit vector {x, z} along the noise's zero line: across its slope,
     * turned a quarter to the left. Null where the noise is flat (no way to tell along from across).
     */
    public static double[] flow(long seed, double x, double z) {
        double[] g = slope(seed, x, z);
        double m = Math.hypot(g[0], g[1]);
        if (m < MIN_SLOPE) return null;
        return new double[] {-g[1] / m, g[0] / m};
    }

    /** The noise's slope at a point, {d/dx, d/dz}, a block's worth. */
    public static double[] slope(long seed, double x, double z) {
        double h = 0.5;
        return new double[] {(field(seed, x + h, z) - field(seed, x - h, z)) / (2 * h), (field(seed, x, z + h) - field(seed, x, z - h)) / (2 * h)};
    }

    /** Whether a height is in the band the currents run in. */
    public static boolean inBand(double y) {
        return y >= BAND_BOTTOM && y < BAND_TOP;
    }

    /**
     * The speed for the next tick of something caught in a current flowing {@code fx}, {@code fz}, from its speed
     * after its own tick: across, most of the way to the current's own; up or down, towards {@code mid} (the band's
     * middle in the world), at most {@link #HOLD_PULL} a tick, half the way each tick, so a fall into it is caught over
     * a few ticks and doesn't stop dead.
     */
    public static double[] carry(double vx, double vy, double vz, double y, double fx, double fz, double mid) {
        double nx = vx + (fx * SPEED - vx) * CATCH, nz = vz + (fz * SPEED - vz) * CATCH;
        double toward = Math.max(-HOLD_PULL, Math.min(HOLD_PULL, (mid - y) * 0.1));
        double ny = vy + (toward - vy) * 0.5;
        return new double[] {nx, ny, nz};
    }

    /**
     * What a current adds to a glider's move this tick, {x, z}: along the current, 30 % of the glider's own speed
     * across, times how much of its heading lies along the current (nothing flying across or against it).
     */
    public static double[] glide(double vx, double vz, double fx, double fz) {
        return glide(vx, vz, fx, fz, GLIDE_BONUS);
    }

    /** As {@link #glide(double, double, double, double)}, with {@code bonus} of the glider's speed (the Fin's more). */
    public static double[] glide(double vx, double vz, double fx, double fz, double bonus) {
        double speed = Math.hypot(vx, vz);
        if (speed < 1e-6) return new double[] {0, 0};
        double along = (vx * fx + vz * fz) / speed;
        if (along <= 0) return new double[] {0, 0};
        double push = speed * bonus * along;
        return new double[] {fx * push, fz * push};
    }

    // ---- washed ashore: where and how a current lets go of what it held --------------------------------------------

    /** How far across a current looks for a shore to wash what it lets go onto. */
    public static final int SHORE_REACH = 32;
    /** How fast it is washed across to the shore's foot, up beside it, and onto it, a tick. */
    public static final double ASHORE_ACROSS = 0.5, ASHORE_UP = 0.5, ASHORE_ONTO = 0.3;
    /** The longest a wash ashore takes before the current lets go anyway (something in the way), in ticks. */
    public static final int ASHORE_TICKS = 240;

    /**
     * Where a current washes up what it lets go at ({@code x}, {@code y}, {@code z}): of the island tops within
     * {@link #SHORE_REACH} across and over the band, the nearest (across and up) with an open column beside it, one with
     * nothing in it over {@code y}, so the way up it is clear. {@code top} is the world's heightmap: the first air over
     * a column's highest block. Returns {open x, open z, shore x, shore z, the height to stand at}, or null with no shore
     * in reach.
     */
    public static int[] shore(int x, int y, int z, java.util.function.IntBinaryOperator top) {
        return shore(x, y, z, SHORE_REACH, top);
    }

    /** {@link #shore(int, int, int, java.util.function.IntBinaryOperator)} within {@code reach} across. */
    public static int[] shore(int x, int y, int z, int reach, java.util.function.IntBinaryOperator top) {
        int[] best = null;
        long bestScore = Long.MAX_VALUE;
        int[][] beside = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                int ex = x + dx, ez = z + dz, h = top.applyAsInt(ex, ez);
                if (h < BAND_TOP + 2) continue;
                long score = (long) dx * dx + (long) dz * dz + (long) (h - y) * (h - y);
                if (score >= bestScore) continue;
                for (int[] b : beside) {
                    int cx = ex + b[0], cz = ez + b[1];
                    if (top.applyAsInt(cx, cz) > y) continue;
                    best = new int[] {cx, cz, ex, ez, h};
                    bestScore = score;
                    break;
                }
            }
        }
        return best;
    }

    /**
     * The speed for the next tick of something being washed ashore at ({@code x}, {@code y}, {@code z}) to
     * {@code shore} (from {@link #shore}): across, at its height, to the open column's middle; up it to just over the
     * shore's height; then onto the shore. Null once it is over the shore, to drop onto it.
     */
    public static double[] ashore(double x, double y, double z, int[] shore) {
        double cx = shore[0] + 0.5, cz = shore[1] + 0.5, ex = shore[2] + 0.5, ez = shore[3] + 0.5, h = shore[4];
        if (y < h + 0.2) {
            double d = Math.hypot(cx - x, cz - z);
            double step = Math.min(ASHORE_ACROSS, d);
            double sx = d < 1e-6 ? 0 : (cx - x) / d * step, sz = d < 1e-6 ? 0 : (cz - z) / d * step;
            // under an island the way is across first; once at the column's foot, up it
            return d > 0.25 ? new double[] {sx, 0, sz} : new double[] {sx, ASHORE_UP, sz};
        }
        double d = Math.hypot(ex - x, ez - z);
        if (d < 0.3) return null;
        double step = Math.min(ASHORE_ONTO, d);
        return new double[] {(ex - x) / d * step, 0.05, (ez - z) / d * step};
    }

    // ---- a plain gradient noise, the same on every side and loader ----------------------------------------------

    private static double perlin(long seed, double x, double z) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double dx = x - x0, dz = z - z0;
        double u = fade(dx), v = fade(dz);
        double a = lerp(u, grad(seed, x0, z0, dx, dz), grad(seed, x0 + 1, z0, dx - 1, dz));
        double b = lerp(u, grad(seed, x0, z0 + 1, dx, dz - 1), grad(seed, x0 + 1, z0 + 1, dx - 1, dz - 1));
        return lerp(v, a, b);
    }

    private static double grad(long seed, int ix, int iz, double dx, double dz) {
        long h = mix(seed ^ (ix * 0x9E3779B97F4A7C15L) ^ (iz * 0xC2B2AE3D27D4EB4FL));
        double angle = (h >>> 11) * 0x1.0p-53 * Math.PI * 2;
        return Math.cos(angle) * dx + Math.sin(angle) * dz;
    }

    private static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }
}

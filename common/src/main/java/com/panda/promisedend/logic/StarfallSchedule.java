package com.panda.promisedend.logic;

import java.util.random.RandomGenerator;

/**
 * The Starfall's numbers, kept apart from the game so a plain JUnit test can pin them: how long between falls, where a
 * fall lands round a player, how big its crater is and what it holds.
 */
public final class StarfallSchedule {
    /** The outer End starts this far from the origin: a fall only comes while a player is out there. */
    public static final double OUTER = 1000;
    /** A fall lands this far from the player it is chosen near. */
    public static final double NEAR = 48, FAR = 160;
    /** The time between falls is the configured interval, give or take this share of it. */
    public static final double SPREAD = 0.25;
    /** The warning before the impact: 30 s. */
    public static final int WARNING_TICKS = 600;
    /** Players this near hear and are told of a fall. */
    public static final double TOLD = 256;
    /** The crater's radius, and the Starmetal Ore in it. */
    public static final int RADIUS_MIN = 4, RADIUS_MAX = 6, ORE_MIN = 3, ORE_MAX = 6;
    /** No fall within this many blocks of a block entity or anything a player may have built. */
    public static final int CLEAR = 8;
    /** A Minecraft day, for the cap on falls a day. */
    public static final long DAY = 24000;

    private StarfallSchedule() {}

    /** What the scheduler does on a tick with no fall on its way. */
    public enum Step {
        /** Nothing: turned off, the Maw's seal not yet broken, not yet due, or no player out in the outer End. */
        IDLE,
        /** The first tick: set when the first fall is due. */
        PLAN,
        /** The day's falls are spent: wait for the next day. */
        TOMORROW,
        /** Due: find a target near an outer player. */
        LOOK
    }

    /**
     * The step at {@code now}, given whether the Starfall is on, whether its clock has started (the Maw's seal broken:
     * stars fall from Act IV on), whether a player is out in the outer End, when the next fall is due (0 before the
     * first plan), and the falls today against the day's cap. A fall that came due while no one was out comes as soon
     * as someone is.
     */
    public static Step step(boolean enabled, boolean started, boolean outerPlayer, long nextAt, long now, int today, int cap) {
        if (!enabled || !started) return Step.IDLE;
        if (nextAt == 0) return Step.PLAN;
        if (now < nextAt || !outerPlayer) return Step.IDLE;
        return today >= cap ? Step.TOMORROW : Step.LOOK;
    }

    /** The way from a listener to a fall {@code dx}, {@code dz} off, of eight: "north" ... "northwest" (north is -z). */
    public static String compass(double dx, double dz) {
        String[] ways = {"south", "southwest", "west", "northwest", "north", "northeast", "east", "southeast"};
        double a = Math.toDegrees(Math.atan2(-dx, dz));
        return ways[(int) Math.floorMod(Math.round(a / 45.0), 8)];
    }

    /** The falls today at {@code now}, of {@code today} counted on day {@code storedDay}: none on a new day. */
    public static int today(long now, long storedDay, int today) {
        return now / DAY == storedDay ? today : 0;
    }

    /** The ticks to the next fall: {@code interval} ± {@link #SPREAD}. */
    public static long nextIn(RandomGenerator random, int interval) {
        return Math.round(interval * (1 - SPREAD + 2 * SPREAD * random.nextDouble()));
    }

    /** Whether a player at {@code x}, {@code z} is in the outer End. */
    public static boolean outer(double x, double z) {
        return x * x + z * z > OUTER * OUTER;
    }

    /** A point {@link #NEAR} to {@link #FAR} from {@code x}, {@code z}, in any direction: {x, z}. */
    public static double[] around(RandomGenerator random, double x, double z) {
        double a = random.nextDouble() * Math.PI * 2, d = NEAR + (FAR - NEAR) * random.nextDouble();
        return new double[] {x + Math.cos(a) * d, z + Math.sin(a) * d};
    }

    public static int radius(RandomGenerator random) {
        return RADIUS_MIN + random.nextInt(RADIUS_MAX - RADIUS_MIN + 1);
    }

    public static int ores(RandomGenerator random) {
        return ORE_MIN + random.nextInt(ORE_MAX - ORE_MIN + 1);
    }

    /**
     * Whether the block at {@code dx}, {@code dy}, {@code dz} from the impact point is inside a crater of {@code radius}:
     * a bowl, as wide as the radius and two thirds as deep, open above the surface.
     */
    public static boolean inCrater(int dx, int dy, int dz, int radius) {
        if (dy > 2) return false;
        double depth = radius * 2.0 / 3.0;
        double y = dy > 0 ? 0 : dy / depth;
        return (dx * dx + dz * dz) / (double) (radius * radius) + y * y <= 1.0;
    }
}

package com.panda.promisedend.logic;

import java.util.List;

/**
 * The Architect's fight numbers, kept apart from the entity so a plain JUnit test can reach them: its health and
 * armour, its keystone, its phases, the turns of its works and how long each takes, the window its cage stands open
 * after each throw, and the lob of its throw. Its fight's length, the time it is open times a sword's damage a second, is
 * {@link #seconds}.
 */
public final class ArchitectFight {
    /** Its health (vanilla's most), armour and toughness: toughness keeps a big blow, as a critical one, from going through. */
    public static final float HEALTH = 1000.0F, ARMOUR = 16.0F, TOUGHNESS = 8.0F;
    /**
     * A blow on the keystone deals this many times its damage. It is taken before armour, which takes less of a bigger
     * blow, so a Voidsteel sword's blow lands about 1.6 times as hard there.
     */
    public static final float KEYSTONE_DAMAGE = 1.45F;
    /** The phases begin at these shares of its health. */
    public static final float[] PHASES = {0.6F, 0.25F};
    /** The pause between its works, and the shorter one in its last phase. */
    public static final int REST_BETWEEN = 50, REST_BETWEEN_LAST = 35;
    /** Its clips' lengths, and the tick in each the work lands on (the clips' own: gen_constructs architect). */
    public static final int RAISE_TICKS = 20, RAISE_AT = 12, THROW_TICKS = 20, THROW_WINDUP = 12, UNMAKE_TELL = 40;

    public enum Work { RAISE, THROW, UNMAKE }

    /** What it does in turn, in each phase. */
    public static final List<List<Work>> WORKS = List.of(
            List.of(Work.RAISE, Work.THROW, Work.THROW),
            List.of(Work.UNMAKE, Work.THROW, Work.THROW),
            List.of(Work.RAISE, Work.UNMAKE, Work.THROW));

    /** A shot's air drag a tick, vanilla's for a thrown item. */
    public static final double DRAG = 0.99;

    private ArchitectFight() {}

    /** The pause after a work in {@code phase} (0 to 2). */
    public static int rest(int phase) {
        return phase >= 2 ? REST_BETWEEN_LAST : REST_BETWEEN;
    }

    /** How long {@code work} takes, to the tick it ends. */
    public static int length(Work work) {
        return switch (work) {
            case RAISE -> RAISE_TICKS;
            case THROW -> THROW_TICKS;
            case UNMAKE -> UNMAKE_TELL;
        };
    }

    /** How long its cage stands open after a throw leaves it: the throw's end and the rest after it. */
    public static int openAfterThrow(int phase) {
        return THROW_TICKS - THROW_WINDUP + rest(phase);
    }

    /** The share of a round of {@code phase}'s works that its cage stands open. */
    public static double openShare(int phase) {
        int open = 0, round = 0;
        for (Work work : WORKS.get(phase)) {
            round += length(work) + rest(phase);
            if (work == Work.THROW) open += openAfterThrow(phase);
        }
        return (double) open / round;
    }

    /** The share of its health each phase takes off: the first to {@link #PHASES}[0], and so on. */
    public static double healthShare(int phase) {
        float from = phase == 0 ? 1.0F : PHASES[phase - 1], to = phase < PHASES.length ? PHASES[phase] : 0.0F;
        return from - to;
    }

    /** The phase its health has reached, forward only: one big blow past both thresholds starts the last. */
    public static int phaseFor(float share, int current) {
        int phase = current;
        for (int i = current; i < PHASES.length; i++) if (share <= PHASES[i]) phase = i + 1;
        return phase;
    }

    /**
     * Seconds of ideal play to bring it down with blows of {@code blow} (before its armour), {@code perSecond} of them a
     * second, each landed while its cage is open: the time it is open times the damage a second, phase by phase.
     */
    public static double seconds(float blow, float perSecond) {
        double each = Balance.afterArmour(blow, ARMOUR, TOUGHNESS) * perSecond, total = 0;
        for (int phase = 0; phase < WORKS.size(); phase++) total += HEALTH * healthShare(phase) / (each * openShare(phase));
        return total;
    }

    /** A throw is in the air this long at least, so even a near one goes up over a wall before it comes down. */
    public static final double FLIGHT_LEAST = 22.0;

    /** The ticks a throw takes to come down {@code across} away, at {@code speed} across the ground. */
    public static double flight(double across, double speed) {
        return Math.max(FLIGHT_LEAST, across / speed);
    }

    /**
     * The throw that comes down on a point {@code (dx, dy, dz)} from where it leaves: out at {@code speed} across the
     * ground, falling {@code gravity} a tick a tick. Vanilla's thrown items fall, then slow by {@link #DRAG}, then
     * move, each tick, so after {@code t} ticks a throw of {@code v} has gone {@code v * s} with
     * {@code s = DRAG (1 - DRAG^t) / (1 - DRAG)}, and has fallen {@code gravity * DRAG / (1 - DRAG) * (t - s)}.
     */
    public static double[] lob(double dx, double dy, double dz, double speed, double gravity) {
        double t = flight(Math.sqrt(dx * dx + dz * dz), speed);
        double s = DRAG * (1.0 - Math.pow(DRAG, t)) / (1.0 - DRAG);
        double fall = gravity * DRAG / (1.0 - DRAG) * (t - s);
        return new double[] {dx / s, (dy + fall) / s, dz / s};
    }
}

package com.panda.promisedend.logic;

/**
 * The Hollow Sentinel's fight numbers, kept apart from the entity so a plain JUnit test can reach them: its health and
 * armour, its volley, the window it stands open and its phases. Its fight's length with ideal play, each volley sent
 * back and every blow landed while it is open, is {@link #seconds}.
 */
public final class SentinelFight {
    public static final double HEALTH = 600.0, ARMOUR = 10.0;
    /** Its volley: the count to it, the shots in it and the ticks between them. */
    public static final int VOLLEY_TICKS = 80, VOLLEY_SHOTS = 3, SHOT_GAP = 5;
    /** How long it stands open once three bullets come back, and what it takes of a blow then. */
    public static final int OPEN_TICKS = 120;
    public static final float OPEN_DAMAGE = 1.5F;
    /** The phases begin at these shares of its health; each one's bullets fly this much faster than the last's. */
    public static final float[] PHASES = {2.0F / 3.0F, 1.0F / 3.0F};
    public static final double PHASE_SPEEDUP = 1.25;
    /** Its bullets' speed out (before the phase's speed-up) and back, in blocks a tick. */
    public static final double BULLET_SPEED = 0.3, RETURN_SPEED = 0.6;
    /** How high over its rest it hovers while closed, and how fast it drops to its dais when it cracks open. */
    public static final double HOVER = 5.0, SINK = 0.5;
    /** About how far a bullet flies from its core to a fighter close enough to send it back (it hovers 5 up). */
    public static final double REACH = 8.0;

    private SentinelFight() {}

    /** Its bullets' speed in {@code phase} (0 to 2). */
    public static double bulletSpeed(int phase) {
        return BULLET_SPEED * Math.pow(PHASE_SPEEDUP, phase);
    }

    /**
     * The ticks it stays shut with ideal play: the count to its volley, the volley, its last bullet out to the fighter
     * and back, and its drop to the dais.
     */
    public static double closedTicks(int phase) {
        return VOLLEY_TICKS + 1 + (VOLLEY_SHOTS - 1) * SHOT_GAP + REACH / bulletSpeed(phase) + REACH / RETURN_SPEED + HOVER / SINK;
    }

    /** The share of its health each phase takes off: the first to {@link #PHASES}[0], and so on. */
    public static double healthShare(int phase) {
        float from = phase == 0 ? 1.0F : PHASES[phase - 1], to = phase < PHASES.length ? PHASES[phase] : 0.0F;
        return from - to;
    }

    /**
     * Seconds of ideal play to bring it down with blows of {@code blow} (before its armour), {@code perSecond} of them a
     * second: each phase as many shut spells as it takes windows to take that phase's health (a phase's break shuts it
     * at once), and the time open that the health takes at that damage a second.
     */
    public static double seconds(float blow, float perSecond) {
        double each = Balance.afterArmour(blow * OPEN_DAMAGE, (float) ARMOUR, 0.0F) * perSecond, total = 0;
        for (int phase = 0; phase <= PHASES.length; phase++) {
            double health = HEALTH * healthShare(phase), window = each * OPEN_TICKS / 20.0;
            total += Math.ceil(health / window - 1e-9) * closedTicks(phase) / 20.0 + health / each;
        }
        return total;
    }
}

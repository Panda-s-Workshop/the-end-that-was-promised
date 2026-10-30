package com.panda.promisedend.logic;

/**
 * The Void Leviathan's fight numbers, kept apart from the entity so a plain JUnit test can reach them: its health and
 * armour, the turns of its breach, and its pairs below half its health. Its fight's length with ideal play, every blow
 * landed while it rears, is {@link #seconds}.
 */
public final class LeviathanFight {
    public static final double HEALTH = 850.0, ARMOUR = 8.0;
    /** Under the surface between breaches, then the ticks it marks its ring, bursts up, rears exposed and dives. */
    public static final int GAP_MIN = 160, GAP_MAX = 240;
    public static final int TELEGRAPH_TICKS = 30, BREACH_TICKS = 10, EXPOSED_TICKS = 80, DIVE_TICKS = 15;
    /** Under the surface between the two breaches of a pair. */
    public static final int PAIR_GAP = 20;
    /** Below this share of its health it breaches twice in a row. */
    public static final float TWICE = 0.5F;

    private LeviathanFight() {}

    /**
     * Seconds of ideal play to bring it down with blows of {@code blow} (before its armour), {@code perSecond} of them a
     * second, each landed while it rears: breach by breach, as it fights, its gaps at their mean. Woken, it comes up
     * after {@link #TELEGRAPH_TICKS} under; once below {@link #TWICE} each breach brings a second after
     * {@link #PAIR_GAP}.
     */
    public static double seconds(float blow, float perSecond) {
        double each = Balance.afterArmour(blow, (float) ARMOUR, 0.0F) * perSecond, window = each * EXPOSED_TICKS / 20.0;
        double health = HEALTH, ticks = TELEGRAPH_TICKS + TELEGRAPH_TICKS + BREACH_TICKS;
        boolean second = false;
        while (window < health) {
            health -= window;
            boolean pair = health / HEALTH <= TWICE && !second;
            second = pair;
            ticks += EXPOSED_TICKS + DIVE_TICKS + (pair ? PAIR_GAP : (GAP_MIN + GAP_MAX) / 2.0) + TELEGRAPH_TICKS + BREACH_TICKS;
        }
        return ticks / 20.0 + health / each;
    }
}

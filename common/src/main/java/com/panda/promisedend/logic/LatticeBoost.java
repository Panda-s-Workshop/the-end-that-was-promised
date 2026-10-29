package com.panda.promisedend.logic;

/**
 * The Architect's Lattice's boost: one a glide at most every {@link #COOLDOWN} ticks (30 s), on the jump key. Pure,
 * for the JUnit test; the server keeps each player's last boost (item.Augments).
 */
public final class LatticeBoost {
    public static final int COOLDOWN = 600;
    /** A player who hasn't boosted yet. */
    public static final long NEVER = Long.MIN_VALUE;

    private LatticeBoost() {}

    /** Ticks until the next boost, for a player who last boosted at {@code last}, now {@code now}; 0 when ready. */
    public static long left(long last, long now) {
        if (last == NEVER || now < last) return 0;
        return Math.max(0, last + COOLDOWN - now);
    }

    public static boolean ready(long last, long now) {
        return left(last, now) == 0;
    }

    /** The whole seconds a player waits, rounded up, as the message says it. */
    public static int seconds(long left) {
        return (int) ((left + 19) / 20);
    }
}

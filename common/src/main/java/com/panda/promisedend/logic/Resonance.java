package com.panda.promisedend.logic;

import java.util.UUID;

/**
 * The Resonance set bonus's count, one per player: every {@link #EVERY}th melee hit on the same target, each within
 * {@link #WINDOW} ticks of the last, rings. Another target, or a longer gap, starts the count again from that hit.
 * Kept apart from the game so a plain JUnit test can pin it.
 */
public final class Resonance {
    /** Which hit rings. */
    public static final int EVERY = 4;
    /** The most ticks between two hits of one count: 3 s. */
    public static final int WINDOW = 60;
    /** What the ringing hit adds, and the Slowness it gives (amplifier 1 is Slowness II) for how long. */
    public static final float BONUS_DAMAGE = 2.0F;
    public static final int SLOWNESS_AMPLIFIER = 1, SLOWNESS_TICKS = 20;

    private UUID target;
    private long lastTick;
    private int count;

    /** Whether a hit on {@code hit} at {@code tick} would ring, without counting it: the bonus goes on before the hit lands. */
    public boolean wouldRing(UUID hit, long tick) {
        return continues(hit, tick) && count + 1 >= EVERY;
    }

    /** Counts a hit on {@code hit} at {@code tick} (one that landed), and says whether it rings. */
    public boolean hit(UUID hit, long tick) {
        if (!continues(hit, tick)) count = 0;
        target = hit;
        lastTick = tick;
        count++;
        if (count < EVERY) return false;
        count = 0;
        return true;
    }

    private boolean continues(UUID hit, long tick) {
        return hit.equals(target) && tick - lastTick <= WINDOW;
    }

    /** How many hits the current count holds (0 after a ring). */
    public int count() {
        return count;
    }
}

package com.panda.promisedend.logic;

import java.util.SplittableRandom;

/**
 * The Void Shrine's offerings, without the game: favour gathers per player at an altar, and at {@link #ASK} the altar
 * offers {@link #CHOICES} of the {@link #BOONS} boons, distinct, drawn from a seed. One boon a player each
 * {@code shrine.cooldown_ticks} at each altar.
 */
public final class Offerings {
    /** The boons, by the index the screen and the payloads use; each is a mob effect of the same name. */
    public static final String[] BOONS = {"gliding_grace", "pearl_sense", "void_sight", "rift_step", "resonant_edge", "shulker_ward"};
    /** Favour an altar needs before it offers; what is left over stays for the next boon. */
    public static final int ASK = 3;
    public static final int CHOICES = 3;
    /** The most favour one item is worth (the Ancient Core), and so the most an altar holds for a player. */
    public static final int MOST = 5;

    private Offerings() {}

    /** {@link #CHOICES} distinct boon indices from {@code seed}, in the order the screen shows them. */
    public static int[] choices(long seed) {
        int[] order = new int[BOONS.length];
        for (int i = 0; i < order.length; i++) order[i] = i;
        SplittableRandom random = new SplittableRandom(seed);
        // the first CHOICES of a Fisher-Yates shuffle
        for (int i = 0; i < CHOICES; i++) {
            int j = i + random.nextInt(order.length - i);
            int t = order[i];
            order[i] = order[j];
            order[j] = t;
        }
        int[] out = new int[CHOICES];
        System.arraycopy(order, 0, out, 0, CHOICES);
        return out;
    }

    /** The favour after an offering worth {@code worth}, capped so one altar can't bank boons. */
    public static int add(int favour, int worth) {
        return Math.min(favour + Math.max(0, worth), ASK + MOST - 1);
    }

    /** Whether {@code favour} is enough to be offered a choice. */
    public static boolean ready(int favour) {
        return favour >= ASK;
    }

    /** Ticks until a player may take another boon at an altar where they last took one at {@code last} (none: -1). */
    public static long waitLeft(long last, long now, long cooldown) {
        if (last < 0) return 0;
        return Math.max(0, last + cooldown - now);
    }

    /** The seed of a player's choice at an altar: the same offering at the same time draws the same three. */
    public static long seed(long altar, long player, long gameTime) {
        long h = altar * 0x9E3779B97F4A7C15L ^ player * 0xC2B2AE3D27D4EB4FL ^ gameTime;
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        return h ^ h >>> 33;
    }
}

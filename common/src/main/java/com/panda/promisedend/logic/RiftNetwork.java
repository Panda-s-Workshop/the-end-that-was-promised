package com.panda.promisedend.logic;

import java.util.List;

/**
 * A player's Rift Gate network, as plain data: the gates they have attuned, each by its level and packed position,
 * with the name it shows and the biome it stands in. Pure, so the rules are tested without a game.
 */
public final class RiftNetwork {
    /** The most gates a network holds. */
    public static final int MOST = 32;
    /** The wait between two journeys, in ticks (30 s). */
    public static final long COOLDOWN = 600;
    /** The longest name a gate takes. */
    public static final int NAME_MOST = 32;

    private RiftNetwork() {}

    public record Gate(String level, long pos, String name, String biome) {
        public boolean at(String level, long pos) {
            return this.pos == pos && this.level.equals(level);
        }
    }

    public enum Added { ADDED, KNOWN, FULL }

    /** Adds {@code gate} unless its place is known already or the network is full. */
    public static Added add(List<Gate> gates, Gate gate) {
        if (find(gates, gate.level(), gate.pos()) != null) return Added.KNOWN;
        if (gates.size() >= MOST) return Added.FULL;
        gates.add(gate);
        return Added.ADDED;
    }

    /** Takes out the gate at that place; whether there was one. */
    public static boolean remove(List<Gate> gates, String level, long pos) {
        return gates.removeIf(g -> g.at(level, pos));
    }

    /** Renames the gate at that place (trimmed, at most {@link #NAME_MOST}); a blank name changes nothing. */
    public static boolean rename(List<Gate> gates, String level, long pos, String name) {
        String trimmed = name.strip();
        if (trimmed.isEmpty()) return false;
        if (trimmed.length() > NAME_MOST) trimmed = trimmed.substring(0, NAME_MOST);
        for (int i = 0; i < gates.size(); i++) {
            Gate g = gates.get(i);
            if (g.at(level, pos)) {
                gates.set(i, new Gate(g.level(), g.pos(), trimmed, g.biome()));
                return true;
            }
        }
        return false;
    }

    public static Gate find(List<Gate> gates, String level, long pos) {
        for (Gate g : gates) if (g.at(level, pos)) return g;
        return null;
    }

    /** Ticks until the next journey, after one at {@code last} ({@code -1}: never); 0 when it may go now. */
    public static long waitLeft(long last, long now) {
        return last < 0 ? 0 : Math.max(0, last + COOLDOWN - now);
    }
}

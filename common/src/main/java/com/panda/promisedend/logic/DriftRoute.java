package com.panda.promisedend.logic;

/**
 * A Drift Islet's route: a straight line between two moorings, travelled at {@link #SPEED} blocks a tick,
 * with a wait of {@link #WAIT} ticks at each end. The islet's place on it is a {@link State}, stepped once a tick the
 * same way on the server and the client, so both see it in the same place without the server sending it each tick.
 */
public record DriftRoute(double ax, double ay, double az, double bx, double by, double bz) {
    /** 3 blocks a second. */
    public static final double SPEED = 0.15;
    /** 3 seconds at each end. */
    public static final int WAIT = 60;
    /** The void a route crosses, edge to edge. */
    public static final int SHORTEST = 12, LONGEST = 96;

    public double length() {
        return Math.sqrt((bx - ax) * (bx - ax) + (by - ay) * (by - ay) + (bz - az) * (bz - az));
    }

    /** The point a fraction {@code s} of the way from the first mooring to the second. */
    public double[] at(double s) {
        return new double[] {ax + (bx - ax) * s, ay + (by - ay) * s, az + (bz - az) * s};
    }

    /** Ticks to cross once. */
    public int crossing() {
        return (int) Math.ceil(length() / SPEED);
    }

    /** Where an islet is on its route: the fraction of the way to the second mooring, which way it goes, its wait. */
    public static final class State {
        public double s;
        public int dir;
        public int wait;

        public State(double s, int dir, int wait) {
            this.s = s;
            this.dir = dir;
            this.wait = wait;
        }

        /** Waiting at the first mooring, about to leave for the second. */
        public static State start() {
            return new State(0, 1, WAIT);
        }
    }

    /** One tick: a tick of the wait, or a step along, turning round with a wait at either end. */
    public void step(State state) {
        if (state.wait > 0) {
            state.wait--;
            return;
        }
        double length = length();
        state.s += state.dir * (length < 1e-6 ? 1 : SPEED / length);
        if (state.s >= 1) {
            state.s = 1;
            state.dir = -1;
            state.wait = WAIT;
        } else if (state.s <= 0) {
            state.s = 0;
            state.dir = 1;
            state.wait = WAIT;
        }
    }

    /** Something in the way: back the way it came, at once. */
    public static void turn(State state) {
        state.dir = -state.dir;
    }
}

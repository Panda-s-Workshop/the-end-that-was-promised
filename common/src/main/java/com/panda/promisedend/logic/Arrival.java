package com.panda.promisedend.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Where an End portal sets a player down: in the same direction from the End's centre as the portal is from the
 * Overworld's, at its distance held between {@link #NEAREST} and {@link #FARTHEST}, so a first-ring stronghold lands in
 * the outer lands and none lands at the Edge. Pure maths; {@link Arrivals} finds the ground and builds the Waystation.
 */
public final class Arrival {
    public static final int NEAREST = 1100, FARTHEST = 2500;
    /** How far round the point the ground is looked for, and every how many blocks. */
    public static final int SEARCH = 256, STEP = 4;
    /** A landing already made within this of the point is used again: one Waystation for one portal's arrivals. */
    public static final int REUSE = 96;

    /** The columns to try round the point, nearest first, every {@link #STEP} blocks out to {@link #SEARCH}. */
    public static final List<int[]> ORDER;

    static {
        List<int[]> order = new ArrayList<>();
        for (int dx = -SEARCH; dx <= SEARCH; dx += STEP) {
            for (int dz = -SEARCH; dz <= SEARCH; dz += STEP) {
                if (dx * dx + dz * dz <= SEARCH * SEARCH) order.add(new int[] {dx, dz});
            }
        }
        order.sort(Comparator.comparingInt((int[] o) -> o[0] * o[0] + o[1] * o[1]));
        ORDER = List.copyOf(order);
    }

    private Arrival() {}

    /** The point in the End for a portal at ({@code x}, {@code z}): {x, z}. At the centre itself, due east. */
    public static double[] target(double x, double z) {
        double r = Math.sqrt(x * x + z * z);
        double ux = r < 1e-6 ? 1 : x / r, uz = r < 1e-6 ? 0 : z / r;
        double d = Math.clamp(r, NEAREST, FARTHEST);
        return new double[] {ux * d, uz * d};
    }

    /** Whether ({@code x}, {@code z}) is inside the Ancients' Ward of {@code radius}: a cylinder round the centre. */
    /** The eight-point compass name of the way ({@code dx}, {@code dz}) points: north is -z, east +x. */
    public static String compass(double dx, double dz) {
        String[] names = {"east", "southeast", "south", "southwest", "west", "northwest", "north", "northeast"};
        int i = (int) Math.round(Math.atan2(dz, dx) / (Math.PI / 4));
        return names[Math.floorMod(i, 8)];
    }

    public static boolean insideWard(double x, double z, int radius) {
        return x * x + z * z < (double) radius * radius;
    }

    /** The yaw that faces the End's centre from ({@code x}, {@code z}), for the arrival's first look: at the Ward. */
    public static float facingCentre(double x, double z) {
        return (float) Math.toDegrees(Math.atan2(x, -z));
    }
}

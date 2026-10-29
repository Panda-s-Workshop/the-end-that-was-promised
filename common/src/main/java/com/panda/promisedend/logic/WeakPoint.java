package com.panda.promisedend.logic;

/**
 * A boss's weak point without a hitbox of its own: vanilla doesn't say where on a hitbox a blow landed, so the
 * attacker's aim is tested instead. A point on the body is found from the entity's place and facing; a blow counts when
 * the attacker's eye ray passes near it, a shot when its flight does. Pure, for the JUnit test.
 */
public final class WeakPoint {
    private WeakPoint() {}

    /**
     * The point {@code right}, {@code up} and {@code forward} of ({@code x}, {@code y}, {@code z}) for a body turned
     * {@code yawDeg} (Minecraft's yaw: 0 faces +z). In a rig's pixels: right = -x/16, up = y/16, forward = -z/16, times
     * the model's scale.
     */
    public static double[] bodyPoint(double x, double y, double z, float yawDeg, double right, double up, double forward) {
        double yaw = Math.toRadians(yawDeg);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        // the body's right: its forward turned a quarter clockwise, seen from above
        double rx = -fz, rz = fx;
        return new double[] {x + fx * forward + rx * right, y + up, z + fz * forward + rz * right};
    }

    /** How far the point ({@code px}, {@code py}, {@code pz}) is from the segment from a to b. */
    public static double toSegment(double px, double py, double pz, double ax, double ay, double az, double bx, double by, double bz) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double len2 = dx * dx + dy * dy + dz * dz;
        double t = len2 == 0 ? 0 : ((px - ax) * dx + (py - ay) * dy + (pz - az) * dz) / len2;
        t = Math.max(0, Math.min(1, t));
        double cx = ax + dx * t - px, cy = ay + dy * t - py, cz = az + dz * t - pz;
        return Math.sqrt(cx * cx + cy * cy + cz * cz);
    }
}

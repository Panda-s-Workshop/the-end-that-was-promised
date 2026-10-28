package com.panda.promisedend.logic;

/**
 * Which blows and shots a front shield stops, as plain arithmetic on a body's turn: a blow from within a half angle of
 * where the body faces, and a shot travelling at it from there, but not one dropping on it steeper than
 * {@link #STEEP_DEGREES} (it comes over the rim).
 */
public final class ShieldArc {
    /** A shot coming down steeper than this, from the level, passes over the shield. */
    public static final double STEEP_DEGREES = 60.0;

    private ShieldArc() {}

    /**
     * Whether something {@code (dx, dz)} away from a body facing {@code bodyYaw} (Minecraft's degrees: 0 faces south,
     * +z, and -90 east, +x) is within {@code halfAngle} degrees of its front. Right on top of it counts as in front.
     */
    public static boolean inFront(float bodyYaw, double halfAngle, double dx, double dz) {
        double length = Math.hypot(dx, dz);
        if (length < 1e-6) return true;
        double yaw = Math.toRadians(bodyYaw);
        double facing = (-Math.sin(yaw) * dx + Math.cos(yaw) * dz) / length;
        return facing >= Math.cos(Math.toRadians(halfAngle)) - 1e-9;
    }

    /**
     * Whether a shot moving {@code (mx, my, mz)} a tick meets the shield of a body facing {@code bodyYaw}: it came from
     * in front (the way it travels, reversed) and isn't dropping steeper than {@link #STEEP_DEGREES}.
     */
    public static boolean stopsShot(float bodyYaw, double halfAngle, double mx, double my, double mz) {
        double flat = Math.hypot(mx, mz);
        if (flat < 1e-6 || Math.atan2(Math.abs(my), flat) > Math.toRadians(STEEP_DEGREES)) return false;
        return inFront(bodyYaw, halfAngle, -mx, -mz);
    }
}

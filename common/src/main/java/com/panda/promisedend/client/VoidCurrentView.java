package com.panda.promisedend.client;

import com.panda.promisedend.logic.VoidCurrent;
import com.panda.promisedend.logic.VoidCurrents;
import com.panda.promisedend.registry.ModParticles;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The void currents on the client, once a tick after the world's: the local player (or what they drive) on a ride in a
 * current, as the server moves everything else (logic/VoidCurrents), and the currents drawn as lit motes drifting
 * along them, sampled from the same field round the camera, so a player on an island sees the rivers under it.
 */
public final class VoidCurrentView {
    /**
     * Tries a tick for a mote within {@link #REACH} of the camera, by the particle setting; most miss (a current is a
     * tenth of the ground).
     */
    static final int SAMPLES_ALL = 96, SAMPLES_DECREASED = 32;
    /**
     * More tries a tick in the far ring only, past {@link #FULL_SIZE_WITHIN}, with longer trails: at an even spread a
     * river 60-90 blocks off, seen from an island, has too few motes to read as one.
     */
    static final int FAR_SAMPLES_ALL = 64, FAR_SAMPLES_DECREASED = 24;
    /** Motes a hit sends, one behind another along the current, so it reads as a stream and not a scatter; and far. */
    static final int TRAIL = 2, FAR_TRAIL = 3;
    /** How far round the camera, across, motes are tried; and how high over the band the camera still sees them. */
    static final double REACH = 96, SEEN_ABOVE = 110;
    /** Past this far from the camera a mote is drawn larger, in step with the distance, so a river reads from an island. */
    static final double FULL_SIZE_WITHIN = 32;
    /** No mote starts within this of the camera, so a rider sees the river round them, not motes in their face. */
    static final double CLEAR_OF_CAMERA = 3;
    /** The local player's ride (by UUID: one logged out held comes back on the same clock, as on the server). */
    private static final Map<UUID, VoidCurrents.Ride> RIDES = new HashMap<>();

    private VoidCurrentView() {}

    public static void tick(Minecraft mc) {
        var level = mc.level;
        var player = mc.player;
        if (level == null || player == null || mc.isPaused() || level.dimension() != Level.END) return;
        Entity mover = player.getVehicle() != null && player.getVehicle().getControllingPassenger() == player ? player.getVehicle() : player;
        VoidCurrents.ride(RIDES, level, mover, player, true, true, VoidCurrent.HOLD_MID);
        motes(mc);
    }

    private static void motes(Minecraft mc) {
        var status = mc.options.particles().get();
        if (status == ParticleStatus.MINIMAL) return;
        Vec3 eye = mc.gameRenderer.mainCamera().position();
        if (eye.y > VoidCurrent.BAND_TOP + SEEN_ABOVE) return;
        var level = mc.level;
        var random = level.getRandom();
        boolean all = status == ParticleStatus.ALL;
        int near = all ? SAMPLES_ALL : SAMPLES_DECREASED, samples = near + (all ? FAR_SAMPLES_ALL : FAR_SAMPLES_DECREASED);
        double inner = FULL_SIZE_WITHIN / REACH;
        for (int i = 0; i < samples; i++) {
            // the first over the whole disc, evenly by area; the rest in the far ring only
            double u = i < near ? random.nextDouble() : inner * inner + random.nextDouble() * (1 - inner * inner);
            double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(u) * REACH;
            double x = eye.x + Math.cos(a) * r, z = eye.z + Math.sin(a) * r;
            // in the middle of the band, where what it carries is held, so a current reads as one flat river
            double y = VoidCurrent.HOLD_MID - 2 + random.nextDouble() * 4;
            double away = eye.distanceTo(new Vec3(x, y, z));
            if (away < CLEAR_OF_CAMERA) continue;
            double[] f = VoidCurrents.flowAt(level, x, y, z);
            if (f == null) continue;
            // forced: from an island the currents are 40-100 blocks down, past the game's own 32; the size rides on
            // the up speed (a mote neither rises nor falls)
            double size = Math.max(1, away / FULL_SIZE_WITHIN) - 1;
            for (int k = 0, trail = i < near ? TRAIL : FAR_TRAIL; k < trail; k++) {
                level.addParticle(ModParticles.CURRENT_MOTE.get(), true, false, x - f[0] * k * 0.7, y, z - f[1] * k * 0.7,
                        f[0] * VoidCurrent.SPEED, size, f[1] * VoidCurrent.SPEED);
            }
        }
    }
}

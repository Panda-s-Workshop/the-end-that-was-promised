package com.panda.promisedend.logic;

import com.panda.promisedend.EndConfig;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ender Pearls show the way in: in the End, out past the main island, a player holding one sees a stream of
 * pearl-green motes rise before them and drift toward the heart of the End, where the dragon waits. Only they see it.
 */
public final class PearlGuide {
    /** Nearer the centre than this, the main island is in sight and the pearl shows nothing. */
    public static final int FROM = 160;
    /** How often a mote leaves, in ticks. */
    public static final int EVERY = 3;
    /** How far ahead of the eyes a mote starts. */
    public static final double AHEAD = 2.5;
    /** How far inward a mote drifts, in blocks, and in how many ticks. */
    public static final double REACH = 6;
    public static final int DRIFT = 30;
    /**
     * An ender pearl's green: the trail motes that move to a point (vanilla's creaking trail) in a colour nothing else
     * of the End's uses, where white end-rod sparks were already the Waystation's posts and the Ward's wall.
     */
    public static final int COLOUR = 0xFF2BD9A2;

    private PearlGuide() {}

    /** The way in from ({@code x}, {@code z}): a unit vector toward the centre on the ground's plane. */
    public static double[] way(double x, double z) {
        double d = Math.hypot(x, z);
        return d < 1e-6 ? new double[] {0, 0} : new double[] {-x / d, -z / d};
    }

    /**
     * Whether the player is shown the way: in the End, past {@link #FROM}, an Ender Pearl in either hand, with arrivals
     * by coordinates on (off, vanilla's End lands everyone at the centre).
     */
    public static boolean guides(ServerPlayer player) {
        if (!EndConfig.arrivalByCoordinates() || player.isSpectator() || !(player.level() instanceof ServerLevel level) || level.dimension() != Level.END) return false;
        if (Math.hypot(player.getX(), player.getZ()) < FROM) return false;
        return player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.ENDER_PEARL) || player.getItemInHand(InteractionHand.OFF_HAND).is(Items.ENDER_PEARL);
    }

    /** Each player's tick: every {@link #EVERY} ticks a mote before them, sent to them alone, that drifts inward. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % EVERY != 0 || !guides(player)) return;
        ServerLevel level = (ServerLevel) player.level();
        double[] w = way(player.getX(), player.getZ());
        // a few steps ahead on the ground's plane (looking straight up or down, toward the way in), a little below the
        // eyes: seen whichever way they look, and never a blot over the view
        Vec3 look = player.getLookAngle();
        double lx = look.x, lz = look.z, flat = Math.hypot(lx, lz);
        if (flat < 0.3) {
            lx = w[0];
            lz = w[1];
        } else {
            lx /= flat;
            lz /= flat;
        }
        double x = player.getX() + lx * AHEAD + (level.getRandom().nextDouble() - 0.5) * 0.6;
        double y = player.getEyeY() - 0.6 + (level.getRandom().nextDouble() - 0.5) * 0.4;
        double z = player.getZ() + lz * AHEAD + (level.getRandom().nextDouble() - 0.5) * 0.6;
        Vec3 to = new Vec3(x + w[0] * REACH, y + 0.3, z + w[1] * REACH);
        level.sendParticles(player, new TrailParticleOption(to, COLOUR, DRIFT), true, false, x, y, z, 1, 0, 0, 0, 0);
    }
}

package com.panda.promisedend.logic;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.worldgen.EndRegions;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * The last act's first goal, {@code rim/enter_rim}: reaching the inner Rim, 150-450 blocks from the centre and all
 * inside the Ancients' Ward. It is by distance alone, since the Rim's biome reaches out to the outer lands and with
 * terrain off there is no Rim biome at all.
 */
public final class RimProgress {
    public static final Identifier ENTER_RIM = PromisedEnd.id("rim/enter_rim");
    public static final int CHECK_INTERVAL = 20;

    private RimProgress() {}

    /** Whether a spot in the End is in the inner Rim (150-450 blocks from the centre). */
    public static boolean inRing(double x, double z) {
        double d = Math.hypot(x, z);
        return d >= EndRegions.RIM_INNER && d <= EndRegions.RIM_OUTER;
    }

    public static void tick(ServerPlayer player) {
        if (player.tickCount % CHECK_INTERVAL != 0 || player.level().dimension() != Level.END) return;
        if (!inRing(player.getX(), player.getZ())) return;
        var advancement = player.level().getServer().getAdvancements().get(ENTER_RIM);
        if (advancement != null) player.getAdvancements().award(advancement, "in_ring");
    }
}

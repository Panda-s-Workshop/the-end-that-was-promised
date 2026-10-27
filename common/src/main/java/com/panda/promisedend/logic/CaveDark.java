package com.panda.promisedend.logic;

import com.panda.promisedend.PromisedEnd;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * The Hollow Highlands' dark caves. The End has no darkness of its own (it is lit the same everywhere), so the dark is
 * a hazard of the biome's caverns: a survival player in the Hollow Highlands with the ground over their head and less
 * than {@link #HELD_OFF_BY} block light at their eyes is given Darkness, renewed every {@link #CHECK_EVERY} ticks while
 * it holds. A dim Echo Amber Cluster gives 5, too little; a flared one (11) holds the dark off for a few blocks round
 * it, and a torch does too. Under a sinkhole, with nothing over their head, there is no dark.
 */
public final class CaveDark {
    public static final ResourceKey<Biome> BIOME = ResourceKey.create(Registries.BIOME, PromisedEnd.id("hollow_highlands"));
    /** How often it is checked, and so how long each dose lasts past the last dark check. */
    public static final int CHECK_EVERY = 20, DOSE_TICKS = 60;
    /** The block light at a player's eyes that keeps the dark away. */
    public static final int HELD_OFF_BY = 6;

    private CaveDark() {}

    public static void tick(ServerPlayer player) {
        if (player.tickCount % CHECK_EVERY != 0 || !affects(player)) return;
        if (dark(player.level(), BlockPos.containing(player.getEyePosition()))) {
            player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, DOSE_TICKS, 0, true, false, true));
        }
    }

    /** Whether the dark is for them: survival and adventure players, not creative or spectating ones. */
    public static boolean affects(Player player) {
        return !player.isCreative() && !player.isSpectator();
    }

    /** Whether {@code eyes} is in the caves' dark: in the biome in the End, and shrouded. */
    public static boolean dark(Level level, BlockPos eyes) {
        return level.dimension() == Level.END && level.getBiome(eyes).is(BIOME) && shrouded(level, eyes);
    }

    /** Under the ground with too little block light: the surface is higher than the block over their eyes. */
    public static boolean shrouded(Level level, BlockPos eyes) {
        if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, eyes.getX(), eyes.getZ()) <= eyes.getY() + 1) return false;
        return level.getBrightness(LightLayer.BLOCK, eyes) < HELD_OFF_BY;
    }
}

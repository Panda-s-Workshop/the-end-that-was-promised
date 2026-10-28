package com.panda.promisedend.entity;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What every construct of the Ancients shares, whatever it is built on (a monster, or the passive Lumen Wisp): it never
 * works against the dragon or another construct, and it shatters into its materials instead of tipping over.
 */
public final class Constructs {
    private Constructs() {}

    /** Whether a construct may attack {@code target}: never the dragon or another construct ({@code #promisedend:constructs}). */
    public static boolean mayAttack(LivingEntity target) {
        return !(target instanceof EnderDragon) && !target.is(ModTags.CONSTRUCTS);
    }

    /**
     * Whether a construct may spawn at {@code pos} as far as our terrain goes: on one of vanilla's biomes in
     * {@code fallback} (where it is added so it still exists with our terrain off) only while the terrain is off, so
     * with it on the construct keeps to its own biome and the vanilla regions stay vanilla.
     */
    public static boolean terrainAllows(ServerLevelAccessor level, BlockPos pos, TagKey<Biome> fallback) {
        return !EndConfig.terrainEnabled() || !level.getBiome(pos).is(fallback);
    }

    /**
     * Breaks {@code construct} into shards of its materials, an end-rod glint among them, and removes it at once (its
     * loot and xp have dropped by then). Call from {@code tickDeath}.
     */
    public static void shatter(LivingEntity construct, BlockState... materials) {
        if (!(construct.level() instanceof ServerLevel server) || construct.isRemoved()) return;
        double y = construct.getY() + construct.getBbHeight() * 0.6;
        double w = construct.getBbWidth() * 0.35, h = construct.getBbHeight() * 0.25;
        int count = 40;
        for (BlockState material : materials) {
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, material), construct.getX(), y, construct.getZ(), count, w, h, w, 0.15);
            count = Math.max(12, count / 2);
        }
        server.sendParticles(ParticleTypes.END_ROD, construct.getX(), y, construct.getZ(), 8, w, h, w, 0.06);
        construct.remove(Entity.RemovalReason.KILLED);
    }
}

package com.panda.promisedend.neoforge;

import com.mojang.serialization.MapCodec;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.entity.Constructs;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * {@code promisedend:construct_fallback_spawns}: adds {@link Constructs#FALLBACK_SPAWNS} to vanilla's End biomes, only
 * while our terrain is off (the server's config is read before the biome modifiers run). Fabric adds the same list in
 * EndFabric.
 */
public record ConstructFallbackSpawns() implements BiomeModifier {
    public static final MapCodec<ConstructFallbackSpawns> CODEC = MapCodec.unit(ConstructFallbackSpawns::new);

    @Override
    public void modify(RegistryAccess registries, Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || EndConfig.terrainEnabled()) return;
        for (var spawn : Constructs.FALLBACK_SPAWNS) {
            if (biome.is(spawn.biomes())) builder.getMobSpawnSettings().addSpawn(spawn.type().get(), spawn.weight(), spawn.min(), spawn.max());
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}

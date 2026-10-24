package com.panda.promisedend.worldgen;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Our End biomes as holders, for one vanilla End biome source. The holders come from the biome lookup that owns the
 * source's own main-island holder, so they belong to the same registry the world was loaded with.
 */
public final class EndBiomes {
    /** The world seed; set when a server starts (both loaders), read by the region picker. */
    private static volatile long seed;
    private static boolean warned;

    private final Map<EndRegions.Biome, Holder<Biome>> holders = new EnumMap<>(EndRegions.Biome.class);

    private EndBiomes(HolderGetter<Biome> lookup) {
        for (EndRegions.Biome b : EndRegions.Biome.values()) {
            if (b.vanilla()) continue;
            lookup.get(ResourceKey.create(Registries.BIOME, Identifier.parse(b.id))).ifPresent(h -> holders.put(b, h));
        }
    }

    public static void setSeed(long worldSeed) {
        seed = worldSeed;
    }

    public static long seed() {
        return seed;
    }

    /** The resolver for a biome source whose main-island holder is {@code end}, or an empty one if it has no lookup. */
    @SuppressWarnings("unchecked")
    public static EndBiomes forSource(Holder<Biome> end) {
        if (end instanceof Holder.Reference<Biome> ref) {
            HolderOwner<Biome> owner = ref.owner;
            if (owner instanceof HolderGetter<?> getter) return new EndBiomes((HolderGetter<Biome>) getter);
        }
        if (!warned) {
            warned = true;
            PromisedEnd.LOGGER.warn("The End's biome source has no biome lookup ({}); the outer End stays vanilla", end);
        }
        return new EndBiomes(new HolderGetter<>() {
            @Override
            public java.util.Optional<Holder.Reference<Biome>> get(ResourceKey<Biome> key) {
                return java.util.Optional.empty();
            }

            @Override
            public java.util.Optional<net.minecraft.core.HolderSet.Named<Biome>> get(net.minecraft.tags.TagKey<Biome> tag) {
                return java.util.Optional.empty();
            }
        });
    }

    /** Every one of our biomes the registry has: added to the source's possible biomes so their features are sorted. */
    public Stream<Holder<Biome>> all() {
        return holders.values().stream();
    }

    /**
     * Our replacement for the biome vanilla chose, or null to keep vanilla's.
     *
     * @param quartX quart coordinates, as {@code getNoiseBiome} receives them
     */
    public Holder<Biome> pick(int quartX, int quartZ, EndRegions.Terrain terrain) {
        if (!EndConfig.terrainEnabled()) return null;
        int chunkX = quartX >> 2, chunkZ = quartZ >> 2;
        EndRegions.Biome b = EndRegions.pick(seed, chunkX, chunkZ, terrain, EndConfig.edgeDistance());
        return b.vanilla() ? null : holders.get(b);
    }
}

package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;

/**
 * Vanilla's random spread, kept to a ring around the origin ({@code promisedend:ring_spread}). The Rim Outpost uses it so
 * it only appears 170–430 blocks out whatever the biome there is: the Shattered Rim normally, vanilla {@code the_end}
 * when {@code terrain.enabled} is off. It stays a {@link RandomSpreadStructurePlacement}, so {@code /locate}
 * and Star Charts find it.
 */
public class RingSpreadPlacement extends RandomSpreadStructurePlacement {
    public static final MapCodec<RingSpreadPlacement> CODEC = RecordCodecBuilder.<RingSpreadPlacement>mapCodec(i -> placementCodec(i).and(i.group(
            Codec.intRange(0, 4096).fieldOf("spacing").forGetter(RandomSpreadStructurePlacement::spacing),
            Codec.intRange(0, 4096).fieldOf("separation").forGetter(RandomSpreadStructurePlacement::separation),
            RandomSpreadType.CODEC.optionalFieldOf("spread_type", RandomSpreadType.LINEAR).forGetter(RandomSpreadStructurePlacement::spreadType),
            Codec.intRange(0, 30_000_000).fieldOf("min_distance").forGetter(RingSpreadPlacement::minDistance),
            Codec.intRange(0, 30_000_000).fieldOf("max_distance").forGetter(RingSpreadPlacement::maxDistance)
    )).apply(i, RingSpreadPlacement::new)).validate(p -> p.spacing() <= p.separation() ? DataResult.error(() -> "Spacing has to be larger than separation")
            : p.maxDistance() <= p.minDistance() ? DataResult.error(() -> "max_distance has to be larger than min_distance") : DataResult.success(p));

    private final int minDistance, maxDistance;

    public RingSpreadPlacement(Vec3i locateOffset, FrequencyReductionMethod reduction, float frequency, int salt, Optional<ExclusionZone> exclusion,
            int spacing, int separation, RandomSpreadType spreadType, int minDistance, int maxDistance) {
        super(locateOffset, reduction, frequency, salt, exclusion, spacing, separation, spreadType);
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
    }

    public int minDistance() {
        return minDistance;
    }

    public int maxDistance() {
        return maxDistance;
    }

    /** Whether a chunk's centre lies in the ring. */
    public boolean inRing(int chunkX, int chunkZ) {
        double d = Math.hypot(chunkX * 16 + 8, chunkZ * 16 + 8);
        return d >= minDistance && d <= maxDistance;
    }

    @Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        return inRing(chunkX, chunkZ) && super.isPlacementChunk(state, chunkX, chunkZ);
    }

    @Override
    public MapCodec<RandomSpreadStructurePlacement> codec() {
        @SuppressWarnings("unchecked")
        MapCodec<RandomSpreadStructurePlacement> codec = (MapCodec<RandomSpreadStructurePlacement>) (MapCodec<?>) CODEC;
        return codec;
    }
}

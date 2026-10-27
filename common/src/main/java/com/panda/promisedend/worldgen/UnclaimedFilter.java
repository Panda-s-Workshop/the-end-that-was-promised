package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;

/**
 * Places only where no structure has claimed a chunk the feature could reach: any chunk within {@code reach} blocks of
 * the spot, and the chunks beside those ({@link StructureClaims}). For vanilla's features on our biomes (a tree's crown
 * grows into an End City's chunk next door otherwise); ours check the same in the feature.
 */
public record UnclaimedFilter(int reach) implements PlacementFilter {
    public static final MapCodec<UnclaimedFilter> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(0, 32).fieldOf("reach").forGetter(UnclaimedFilter::reach)
    ).apply(i, UnclaimedFilter::new));

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return !StructureClaims.claimed(context.getLevel(), List.of(pos.offset(-reach, 0, -reach), pos.offset(reach, 0, -reach),
                pos.offset(-reach, 0, reach), pos.offset(reach, 0, reach)));
    }

    @Override
    public MapCodec<UnclaimedFilter> codec() {
        return CODEC;
    }
}

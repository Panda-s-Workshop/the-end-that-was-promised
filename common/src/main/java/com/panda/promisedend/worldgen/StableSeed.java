package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementModifier;

/**
 * Reseeds a placed feature's random from the world's seed, the chunk and the feature's own {@code salt}, first of its
 * placement. Vanilla seeds that random by the feature's place among every feature of its step, so adding a feature
 * anywhere changed how many of every later feature a chunk got, where they stood and how they grew (a seed's Resonant
 * Spires came out a third the size when the Wilds' features were added). After this the random each of ours draws
 * from is its own, whatever else is in the world. {@link ChunkChanceFilter} keeps its own hash, so a feature's chunks
 * stay where they were.
 */
public record StableSeed(String salt) implements PlacementModifier {
    public static final MapCodec<StableSeed> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("salt").forGetter(StableSeed::salt)
    ).apply(i, StableSeed::new));

    @Override
    public void modify(PlacementContext context, RandomSource random, BlockPos pos, Consumer<BlockPos> out) {
        random.setSeed(ChunkChanceFilter.hash(ChunkChanceFilter.worldSeed(context), "seed/" + salt, pos.getX() >> 4, pos.getZ() >> 4));
        out.accept(pos);
    }

    @Override
    public MapCodec<StableSeed> codec() {
        return CODEC;
    }
}

package com.panda.promisedend.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * The Ancients' causeway across the Rim: broken purpur stepping stones every {@code every} blocks along the four compass
 * lines through the centre, from {@code to} out at the Rim's far edge in to {@code from} by the main island, a road the
 * outer lands can see leading in to the dragon. Each stone is a cross of purpur round a pillar, its corners and arms
 * broken off by a chance of its own, at a height that swells gently along the road; it only fills air, so an island
 * in the way keeps its blocks. A stone's shape comes from its place and the world's seed alone, so it is the same
 * whichever chunk is generated first; only the chunk holding its middle builds it (logic.Causeway).
 */
public record CausewayFeature(int every, int from, int to, int height) implements Feature {
    public static final MapCodec<CausewayFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.intRange(4, 64).fieldOf("every").forGetter(CausewayFeature::every),
            Codec.intRange(EndRegions.RIM_INNER, EndRegions.RIM_FAR).fieldOf("from").forGetter(CausewayFeature::from),
            Codec.intRange(EndRegions.RIM_INNER, EndRegions.RIM_FAR).fieldOf("to").forGetter(CausewayFeature::to),
            Codec.intRange(0, 255).fieldOf("height").forGetter(CausewayFeature::height)
    ).apply(i, CausewayFeature::new));

    @Override
    public MapCodec<CausewayFeature> codec() {
        return CODEC;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        List<BlockPos> stones = new com.panda.promisedend.logic.Causeway(every, from, to, height).stones(origin.getX() >> 4, origin.getZ() >> 4);
        for (BlockPos middle : stones) stone(level, middle, RandomSource.create(level.getSeed() ^ Mth.getSeed(middle.getX(), 0, middle.getZ())));
        return !stones.isEmpty();
    }

    /**
     * One stone: a purpur pillar in the middle, its four arms there five times in six and its corners two in five, each
     * a purpur block or, one in four, a slab sunk into it; one stone in eight has only its pillar and an arm at most.
     */
    private static void stone(WorldGenLevel level, BlockPos middle, RandomSource random) {
        boolean ruined = random.nextInt(8) == 0;
        put(level, middle, Blocks.PURPUR_PILLAR.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y));
        int arms = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                boolean arm = dx == 0 || dz == 0;
                boolean stays = ruined ? arm && arms == 0 && random.nextBoolean() : random.nextFloat() < (arm ? 5 / 6F : 0.4F);
                if (!stays) continue;
                if (arm) arms++;
                put(level, middle.offset(dx, 0, dz), random.nextInt(4) == 0
                        ? Blocks.PURPUR_SLAB.defaultBlockState().setValue(SlabBlock.TYPE, SlabType.TOP) : Blocks.PURPUR_BLOCK.defaultBlockState());
            }
        }
    }

    private static void put(WorldGenLevel level, BlockPos at, BlockState state) {
        if (level.isEmptyBlock(at)) level.setBlock(at, state, 2);
    }
}

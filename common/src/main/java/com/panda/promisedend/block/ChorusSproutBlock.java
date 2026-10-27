package com.panda.promisedend.block;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * The Chorus Sprout, which the Chorus Canopy's leaves drop: a sapling that grows the canopy's tree ({@link #TREE}) on
 * end stone or the End's mosses ({@code #promisedend:void_grass_ground}). Vanilla's sapling needs light 9 to grow and
 * the End has no sky, so it grows here in any light, one random tick in {@link #TICK_CHANCE_FOR_SAPLING_GROWTH}.
 */
public class ChorusSproutBlock extends SaplingBlock {
    public static final ResourceKey<Feature> TREE = ResourceKey.create(Registries.FEATURE, PromisedEnd.id("chorus_canopy_tree"));
    public static final TreeGrower GROWER = new TreeGrower(PromisedEnd.MODID + "_chorus_canopy",
            WeightedList.of(TREE), WeightedList.of(), WeightedList.of(), TREE);

    public ChorusSproutBlock(Properties properties) {
        super(GROWER, properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(TICK_CHANCE_FOR_SAPLING_GROWTH) == 0) advanceTree(level, pos, state, random);
    }

    @Override
    protected boolean mayPlaceOn(BlockState below, BlockGetter level, BlockPos pos) {
        return below.is(ModTags.VOID_GRASS_GROUND);
    }
}

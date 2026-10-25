package com.panda.promisedend.worldgen;

import com.mojang.serialization.MapCodec;
import com.panda.promisedend.EndConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import net.minecraft.world.level.levelgen.placement.PlacementFilter;

/**
 * Places only while our terrain is off ({@code terrain.enabled=false}), and only in the End: for the copies of our
 * biomes' content that go on vanilla's biomes then, when the feature itself is vanilla's and has no fallback guard of
 * its own (ours check the same two things in the feature).
 */
public record TerrainOffFilter() implements PlacementFilter {
    public static final TerrainOffFilter INSTANCE = new TerrainOffFilter();
    public static final MapCodec<TerrainOffFilter> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public boolean shouldPlace(PlacementContext context, RandomSource random, BlockPos pos) {
        return !EndConfig.terrainEnabled() && context.getLevel().getLevel().dimension() == Level.END;
    }

    @Override
    public MapCodec<TerrainOffFilter> codec() {
        return CODEC;
    }
}

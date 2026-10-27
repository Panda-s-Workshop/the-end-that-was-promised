package com.panda.promisedend.block;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * The Void Mushroom: vanilla's mushroom (it spreads slowly, and bone meal grows it into a huge one, the feature
 * {@link #HUGE}), but on Void Mycelium only ({@code #promisedend:void_mushroom_ground}) and in any light, since the
 * End is all one dim light.
 */
public class VoidMushroomBlock extends MushroomBlock {
    public static final ResourceKey<Feature> HUGE = ResourceKey.create(Registries.FEATURE, PromisedEnd.id("huge_void_mushroom"));

    public VoidMushroomBlock(Properties properties) {
        super(HUGE, properties);
    }

    @Override
    protected boolean mayPlaceOn(BlockState below, BlockGetter level, BlockPos pos) {
        return below.is(ModTags.VOID_MUSHROOM_GROUND);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(ModTags.VOID_MUSHROOM_GROUND);
    }
}

package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Void Grass: the End's own grass, lilac tufts on end stone and the End's mosses ({@code #promisedend:void_grass_ground}).
 * As vanilla's short grass: a block placed into it replaces it, it drops nothing but to shears (its loot table), and
 * bone meal grows it into {@link ModBlocks#TALL_VOID_GRASS} where there is room.
 */
public class VoidGrassBlock extends VegetationBlock implements BonemealableBlock {
    private static final VoxelShape SHAPE = Block.column(12, 0, 13);

    public VoidGrassBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.move(state.getOffset(pos));
    }

    @Override
    protected boolean mayPlaceOn(BlockState below, BlockGetter level, BlockPos pos) {
        return below.is(ModTags.VOID_GRASS_GROUND);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return level.getBlockState(pos.above()).isAir();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        DoublePlantBlock.placeAt(level, ModBlocks.TALL_VOID_GRASS.get().defaultBlockState(), pos, Block.UPDATE_CLIENTS);
    }

    /** Tall Void Grass: two blocks of it, on the same ground. */
    public static class Tall extends DoublePlantBlock {
        public Tall(Properties properties) {
            super(properties);
        }

        @Override
        protected boolean mayPlaceOn(BlockState below, BlockGetter level, BlockPos pos) {
            return below.is(ModTags.VOID_GRASS_GROUND);
        }
    }
}

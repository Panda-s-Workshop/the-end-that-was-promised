package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModBlocks;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Mire Lily: a pad on void-water with a pale gold bloom that gives off light. It grows on void-water
 * only. Bone meal on it puts another lily on open void-water within {@link #SPREAD} blocks, so a pool keeps
 * giving lilies once you've found one.
 */
public class MireLilyBlock extends VegetationBlock implements BonemealableBlock {
    /** The bloom and the middle of the pad, for the cursor; the pad itself lies on the water two pixels down. */
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 5, 13);
    public static final int SPREAD = 2;

    public MireLilyBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean mayPlaceOn(BlockState below, BlockGetter level, BlockPos pos) {
        return below.is(ModBlocks.VOID_WATER.get());
    }

    /** Open void-water within {@link #SPREAD} blocks of {@code pos}: a pool cell with air over it. */
    private List<BlockPos> openWater(LevelReader level, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-SPREAD, 0, -SPREAD), pos.offset(SPREAD, 0, SPREAD))) {
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).is(ModBlocks.VOID_WATER.get())) out.add(p.immutable());
        }
        return out;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return !openWater(level, pos).isEmpty();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        List<BlockPos> open = openWater(level, pos);
        if (!open.isEmpty()) level.setBlock(open.get(random.nextInt(open.size())), defaultBlockState(), Block.UPDATE_ALL);
    }
}

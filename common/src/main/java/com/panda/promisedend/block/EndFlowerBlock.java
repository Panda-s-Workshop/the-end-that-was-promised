package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModTags;
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
 * A flower of Bloom Meadows (the Ender Bloom, the Starflower): it glows a little, stands where Void Grass stands
 * ({@code #promisedend:void_grass_ground}: end stone and the End's mosses), drops itself and crafts into a dye. Bone
 * meal puts another of its kind on open ground within {@link #SPREAD} blocks, so one flower found is a supply of dye,
 * as vanilla's flowers are from a grass block.
 */
public class EndFlowerBlock extends VegetationBlock implements BonemealableBlock {
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 13, 13);
    public static final int SPREAD = 2;

    public EndFlowerBlock(Properties properties) {
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

    /** Open ground within {@link #SPREAD} blocks of {@code pos}, a block either way up or down: air over a ground block. */
    private List<BlockPos> openGround(LevelReader level, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-SPREAD, -1, -SPREAD), pos.offset(SPREAD, 1, SPREAD))) {
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).is(ModTags.VOID_GRASS_GROUND)) out.add(p.immutable());
        }
        return out;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return !openGround(level, pos).isEmpty();
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        List<BlockPos> open = openGround(level, pos);
        if (!open.isEmpty()) level.setBlock(open.get(random.nextInt(open.size())), defaultBlockState(), Block.UPDATE_ALL);
    }
}

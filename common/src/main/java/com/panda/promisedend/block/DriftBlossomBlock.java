package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModParticles;
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
 * The Drift Blossom: a pale flower of the Skyfall Archipelago's islets, on end stone ({@code
 * #promisedend:drift_blossom_ground}). Broken by hand it gives Drift Silk (its loot table); shears or Silk Touch keep
 * the blossom. Bone meal puts another on open ground within {@link #SPREAD} blocks, so one blossom found is a supply.
 * Now and then a petal comes loose and drifts off on the air.
 */
public class DriftBlossomBlock extends VegetationBlock implements BonemealableBlock {
    /** The puff's own extent in the art, 15 pixels up (the outline is what a player aims at). */
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 15, 13);
    public static final int SPREAD = 2;
    /** One tick in this many, a petal comes loose (client only). */
    public static final int PETAL_CHANCE = 12;

    public DriftBlossomBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.move(state.getOffset(pos));
    }

    @Override
    protected boolean mayPlaceOn(BlockState below, BlockGetter level, BlockPos pos) {
        return below.is(ModTags.DRIFT_BLOSSOM_GROUND);
    }

    /** Open ground within {@link #SPREAD} blocks of {@code pos}, a block either way up or down: air over a ground block. */
    private List<BlockPos> openGround(LevelReader level, BlockPos pos) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-SPREAD, -1, -SPREAD), pos.offset(SPREAD, 1, SPREAD))) {
            if (level.getBlockState(p).isAir() && level.getBlockState(p.below()).is(ModTags.DRIFT_BLOSSOM_GROUND)) out.add(p.immutable());
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

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(PETAL_CHANCE) != 0) return;
        level.addParticle(ModParticles.DRIFT_PETAL.get(), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.6 + random.nextDouble() * 0.2,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0, 0);
    }
}

package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.AmethystBlock;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Budding Resonite: as budding amethyst grows its buds, it grows a Resonite Cluster on one of its open faces, one random
 * tick in {@link #GROWTH_CHANCE}. There are no bud stages: the cluster comes whole. It has no item and drops nothing, so
 * a player farms it where it grew.
 */
public class BuddingResoniteBlock extends AmethystBlock {
    /** Vanilla budding amethyst's chance, so a farm grows at the pace players know. */
    public static final int GROWTH_CHANCE = 5;

    public BuddingResoniteBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(GROWTH_CHANCE) != 0) return;
        Direction face = Direction.getRandom(random);
        BlockPos out = pos.relative(face);
        if (!level.getBlockState(out).isAir()) return;
        level.setBlockAndUpdate(out, ModBlocks.RESONITE_CLUSTER.get().defaultBlockState().setValue(AmethystClusterBlock.FACING, face));
    }
}

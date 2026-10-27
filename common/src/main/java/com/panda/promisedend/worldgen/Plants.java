package com.panda.promisedend.worldgen;

import com.panda.promisedend.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

/**
 * Ground cover a feature may build over, as vanilla's trees build over grass: the Void Grass a neighbouring chunk laid
 * (its patches reach four blocks past their own chunk, and a chunk's features run after its neighbour's) must not keep
 * a grove or a pool out.
 */
public final class Plants {
    private Plants() {}

    /** Air, or a plant a block placed into it would replace: not a fluid, and not void-water. */
    public static boolean open(BlockState state) {
        return state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty() && !state.is(ModBlocks.VOID_WATER.get()));
    }

    /** Clears {@code pos}, and the other half of a two-block plant there, so no half is left standing alone. */
    public static void clear(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return;
        if (state.hasProperty(DoublePlantBlock.HALF)) {
            BlockPos other = state.getValue(DoublePlantBlock.HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
            if (level.getBlockState(other).is(state.getBlock())) level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
    }
}

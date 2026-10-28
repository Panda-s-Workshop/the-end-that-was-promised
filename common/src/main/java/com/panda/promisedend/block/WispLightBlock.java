package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The light a Lumen Wisp leaves: unseen, no body, nothing to break or pick, gone by itself when its tick comes (each one
 * placed is given its own time, {@link #place}). A block keeps its scheduled tick through a save, so none is left behind.
 */
public class WispLightBlock extends Block {
    public WispLightBlock(Properties properties) {
        super(properties);
    }

    /**
     * Lights {@code pos} for {@code ticks} from now, if it is open air or already a wisp's light (whose old time is
     * dropped first: a light can't be scheduled twice, and the earlier tick would put this one out).
     */
    public static boolean place(Level level, BlockPos pos, int ticks) {
        BlockState here = level.getBlockState(pos);
        if (here.is(ModBlocks.WISP_LIGHT.get())) {
            if (level instanceof ServerLevel server) server.getBlockTicks().clearArea(new net.minecraft.world.level.levelgen.structure.BoundingBox(pos));
        } else if (here.isAir()) {
            level.setBlock(pos, ModBlocks.WISP_LIGHT.get().defaultBlockState(), Block.UPDATE_ALL);
        } else {
            return false;
        }
        level.scheduleTick(pos, ModBlocks.WISP_LIGHT.get(), ticks);
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }
}

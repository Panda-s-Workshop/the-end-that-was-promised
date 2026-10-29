package com.panda.promisedend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Architect's Promise: the trophy each of its fighters is given, its keystone on a short mast of lattice over a
 * plinth of tile. Only something to set on a shelf; its shape is the model's.
 */
public class ArchitectsPromiseBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(6, 3, 6, 10, 8, 10), Block.box(4.5, 8, 4.5, 11.5, 15, 11.5));

    public ArchitectsPromiseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}

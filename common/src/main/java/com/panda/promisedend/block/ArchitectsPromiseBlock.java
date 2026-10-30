package com.panda.promisedend.block;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.logic.CodexBook;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Architect's Promise: the trophy each of its fighters is given, its keystone on a short mast of lattice over a
 * plinth of tile; its shape is the model's. Used, it writes the Codex's closing page in (its advancement) and opens
 * the Codex there, on the last act.
 */
public class ArchitectsPromiseBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.or(Block.box(2, 0, 2, 14, 3, 14), Block.box(6, 3, 6, 10, 8, 10), Block.box(4.5, 8, 4.5, 11.5, 15, 11.5));

    public ArchitectsPromiseBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            PromisedEnd.openCodex.accept(CodexBook.ACTS.size() - 1);
        } else if (player instanceof ServerPlayer p) {
            var kept = p.level().getServer().getAdvancements().get(CodexBook.PROMISE_KEPT);
            if (kept != null) p.getAdvancements().award(kept, "read");
            level.playSound(null, pos, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
}

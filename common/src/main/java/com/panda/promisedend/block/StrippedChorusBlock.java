package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Stripped chorus timber: put in place of a chorus log or wood (an axe's strip, any mod's), it sheds Chorus Resin
 * half the time, popped out of its top. Placed any other way (a player, worldgen, a command over air) it sheds none,
 * so the Thicket's groves pay resin to the work of stripping them.
 */
public class StrippedChorusBlock extends RotatedPillarBlock {
    /** One strip in this many sheds resin: half. */
    public static final int SHEDS_ONE_IN = 2;

    public StrippedChorusBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (!(level instanceof ServerLevel server) || moved) return;
        if (!oldState.is(ModBlocks.CHORUS_LOG.get()) && !oldState.is(ModBlocks.CHORUS_WOOD.get())) return;
        if (server.getRandom().nextInt(SHEDS_ONE_IN) != 0) return;
        Block.popResourceFromFace(server, pos, Direction.UP, new ItemStack(ModItems.CHORUS_RESIN.get()));
        server.playSound(null, pos, SoundEvents.RESIN_BREAK, SoundSource.BLOCKS, 0.8F, 1.1F);
    }
}

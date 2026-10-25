package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crumbling Voidstone, the Voidscar Wastes' worn island edges: a player who steps onto it cracks it (a crack and a
 * puff of grit), and {@link #DELAY} ticks later it falls, out from under them, with the Crumbling Voidstone under it
 * down to the air (the worn rim is laid as columns that end over the void). A column that doesn't end in air within
 * {@link #COLUMN} blocks stays: a falling block that lands at once is put back where it was. A player sneaking
 * (stepping carefully, as on magma) leaves it whole, and so do players in creative or spectator mode and everything
 * that isn't a player, so endermen don't wear the edges away. What falls drops nothing where it lands (it crumbles),
 * so it is no source of the block; broken, it gives Voidstone, and Silk Touch keeps it (its loot table).
 */
public class CrumblingVoidstoneBlock extends Block {
    /** Ticks from the crack to the fall. */
    public static final int DELAY = 20;
    /** The deepest column of it that falls together (the edge feature lays none deeper). */
    public static final int COLUMN = 4;

    public CrumblingVoidstoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel server && cracks(entity) && !server.getBlockTicks().hasScheduledTick(pos, this)) {
            server.scheduleTick(pos, this, DELAY);
            server.playSound(null, pos, ModSounds.CRUMBLING_VOIDSTONE_CRACK.get(), SoundSource.BLOCKS, 0.8F, 0.9F + server.getRandom().nextFloat() * 0.2F);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    10, 0.3, 0.02, 0.3, 0.05);
        }
        super.stepOn(level, pos, state, entity);
    }

    /** Whether this entity's step cracks the block: a player in survival or adventure, not sneaking. */
    public static boolean cracks(Entity entity) {
        return entity instanceof Player player && !player.isCreative() && !player.isSpectator() && !entity.isSteppingCarefully();
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int depth = column(level, pos);
        if (depth == 0) return;
        // from the bottom up, so each block has the air under it as it goes
        for (int d = depth - 1; d >= 0; d--) FallingBlockEntity.fall(level, pos.below(d), level.getBlockState(pos.below(d))).disableDrop();
    }

    /**
     * How many blocks of it, from {@code top} down, end over air (a falling block's free space), or 0 when the column
     * meets anything else, or runs deeper than {@link #COLUMN}.
     */
    public static int column(net.minecraft.world.level.LevelReader level, BlockPos top) {
        for (int d = 0; d < COLUMN; d++) {
            var state = level.getBlockState(top.below(d));
            if (!(state.getBlock() instanceof CrumblingVoidstoneBlock)) return d > 0 && net.minecraft.world.level.block.FallingBlock.isFree(state) ? d : 0;
        }
        return net.minecraft.world.level.block.FallingBlock.isFree(level.getBlockState(top.below(COLUMN))) ? COLUMN : 0;
    }
}

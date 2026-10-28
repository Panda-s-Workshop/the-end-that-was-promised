package com.panda.promisedend.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Ancient Waystation's Return Gate: a pool of light in the Waystation's floor. A player who steps in is
 * sent to their respawn point, as vanilla's exit portal sends them; nothing else goes through.
 */
public class ReturnGateBlock extends Block implements Portal {
    public static final int TRANSITION = 40;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public ReturnGateBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean intersects) {
        if (entity instanceof ServerPlayer && entity.canUsePortal(false)) entity.setAsInsidePortal(this, pos);
    }

    @Override
    public TeleportTransition getPortalDestination(ServerLevel level, Entity entity, BlockPos pos) {
        if (!(entity instanceof ServerPlayer player)) return null;
        return com.panda.promisedend.logic.AncientWard.home(player, TeleportTransition.PLAY_PORTAL_SOUND);
    }

    /** Two seconds standing in it, so a first step off the arrival's floor doesn't send a new player home. */
    @Override
    public int getPortalTransitionTime(ServerLevel level, Entity entity) {
        return entity instanceof ServerPlayer player && player.isCreative() ? 1 : TRANSITION;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // a block is only asked a few times a second, so each time is a handful; an end-rod spark now and then, seen from afar
        for (int i = 0; i < 4; i++) {
            level.addParticle(ParticleTypes.REVERSE_PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 0.8, pos.getZ() + random.nextDouble(), 0, 0.04, 0);
        }
        if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0, 0.06, 0);
    }
}

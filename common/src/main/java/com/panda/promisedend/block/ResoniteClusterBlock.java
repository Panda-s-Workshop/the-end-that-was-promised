package com.panda.promisedend.block;

import com.panda.promisedend.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.monster.Enderman;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * A Resonite Cluster: amethyst's cluster in Resonite, which hums while you're near (now and then, at the
 * cluster, so the sound's falloff is the distance) and chimes when a player breaks it, which turns every Enderman
 * within {@link #CHIME_RADIUS} blocks on that player: the soft aggro radius.
 */
public class ResoniteClusterBlock extends AmethystClusterBlock {
    /** How far a broken cluster's chime carries to Endermen. */
    public static final double CHIME_RADIUS = 12.0;
    /** One tick in this many a cluster hums (animateTick runs for blocks within 32 of the player). */
    public static final int HUM_ONE_IN = 60;

    public ResoniteClusterBlock(Properties properties) {
        super(7.0F, 3.0F, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(HUM_ONE_IN) != 0) return;
        level.playLocalSound(pos, ModSounds.RESONITE_HUM.get(), SoundSource.BLOCKS, 0.6F, 0.9F + random.nextFloat() * 0.2F, false);
        level.addParticle(ParticleTypes.GLOW, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0.02, 0);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel server && !player.isCreative()) chime(server, pos, player);
        return super.playerWillDestroy(level, pos, state, player);
    }

    /** The chime: its sound, and every Enderman within the radius takes the player as its target. */
    public static int chime(ServerLevel level, BlockPos pos, Player player) {
        level.playSound(null, pos, ModSounds.RESONITE_CHIME.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        int drawn = 0;
        for (Enderman e : level.getEntitiesOfClass(Enderman.class, new AABB(pos).inflate(CHIME_RADIUS),
                e -> e.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos)) <= CHIME_RADIUS * CHIME_RADIUS)) {
            e.setTarget(player);
            drawn++;
        }
        return drawn;
    }
}

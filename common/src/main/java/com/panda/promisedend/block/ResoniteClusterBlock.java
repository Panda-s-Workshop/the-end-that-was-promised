package com.panda.promisedend.block;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModSounds;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A Resonite Cluster: amethyst's cluster in Resonite, which hums while you're near (now and then, at the
 * cluster, so the sound's falloff is the distance) and chimes when it breaks, however it breaks: by hand, by losing the
 * block it grew on, a piston or an explosion. The chime turns the nearest {@link #DRAWN} mobs from
 * {@code #promisedend:hears_resonite} (Endermen) within {@link #CHIME_RADIUS} blocks on the player whose break set it off
 * (the cluster itself, or the block it stood on), if they are in survival or adventure (a creative builder's break
 * rings and draws none), and on the nearest such player within the radius when no player broke anything (a piston, an
 * explosion) or a machine's fake player did: the soft aggro radius.
 */
public class ResoniteClusterBlock extends AmethystClusterBlock {
    public static final TagKey<EntityType<?>> HEARS = TagKey.create(Registries.ENTITY_TYPE, PromisedEnd.id("hears_resonite"));
    /** How far a broken cluster's chime carries. */
    public static final double CHIME_RADIUS = 12.0;
    /** The most mobs one chime draws, the nearest: a break in a crowd is a fight, not a death. */
    public static final int DRAWN = 3;
    /**
     * One call in this many a cluster hums. The client calls animateTick for blocks round the player 667 times a tick
     * at range 16 and 667 at 32, each offset {@code nextInt(r) - nextInt(r)} on each axis, so near blocks are asked far
     * more often: about 0.16 a tick 2 blocks off, 0.12 at 6, 0.05 at 12. At one in 300 a cluster hums about every 95 s
     * 2 blocks off, two minutes at 6 and four and a half at 12; beside a spire's score of clusters, one every several
     * seconds.
     */
    public static final int HUM_ONE_IN = 300;
    /** The player whose break is under way on this thread: every cluster that goes in it (its own, or one it stood on) is theirs. */
    private static final ThreadLocal<Player> BREAKER = new ThreadLocal<>();

    public ResoniteClusterBlock(Properties properties) {
        super(7.0F, 3.0F, properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(HUM_ONE_IN) != 0) return;
        level.playLocalSound(pos, ModSounds.RESONITE_HUM.get(), SoundSource.BLOCKS, 0.6F, 0.9F + random.nextFloat() * 0.2F, false);
        level.addParticle(ParticleTypes.GLOW, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble(), pos.getZ() + random.nextDouble(), 0, 0.02, 0);
    }

    /** The player whose break is under way, or null: see ServerPlayerGameModeMixin. */
    public static Player breaker() {
        return BREAKER.get();
    }

    /** The server's block break starts ({@code player}) and ends (the one before it, or {@code null}): see ServerPlayerGameModeMixin. */
    public static void breaking(Player player) {
        if (player == null) BREAKER.remove();
        else BREAKER.set(player);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        chime(level, pos, chimeTarget(level, pos));
    }

    /**
     * Whom a cluster going at {@code pos} now draws onto: the player whose break is under way, if they are a person in
     * survival or adventure (none for a creative or spectating one); with no break under way, or a machine's fake
     * player's, the nearest player in survival or adventure within the radius.
     */
    public static Player chimeTarget(ServerLevel level, BlockPos pos) {
        Player breaker = BREAKER.get();
        if (breaker != null && !com.panda.promisedend.platform.Services.PLATFORM.isFakePlayer(breaker)) {
            return EntitySelector.NO_CREATIVE_OR_SPECTATOR.test(breaker) ? breaker : null;
        }
        Vec3 at = Vec3.atCenterOf(pos);
        return level.getNearestPlayer(at.x, at.y, at.z, CHIME_RADIUS, EntitySelector.NO_CREATIVE_OR_SPECTATOR);
    }

    /** The chime: its sound and a ring of glints, and the nearest hearers within the radius take {@code target} (if any). */
    public static int chime(ServerLevel level, BlockPos pos, Player target) {
        Vec3 at = Vec3.atCenterOf(pos);
        level.playSound(null, pos, ModSounds.RESONITE_CHIME.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        for (int i = 0; i < 16; i++) {
            double a = i * Math.PI / 8;
            level.sendParticles(ParticleTypes.GLOW, at.x + Math.cos(a) * 0.8, at.y, at.z + Math.sin(a) * 0.8, 1, 0, 0, 0, 0.05);
        }
        if (target == null) return 0;
        List<Mob> hearers = level.getEntitiesOfClass(Mob.class, new AABB(pos).inflate(CHIME_RADIUS),
                e -> e.getType().builtInRegistryHolder().is(HEARS) && e.distanceToSqr(at) <= CHIME_RADIUS * CHIME_RADIUS);
        hearers.sort(Comparator.comparingDouble(e -> e.distanceToSqr(at)));
        int drawn = 0;
        for (Mob e : hearers) {
            if (drawn == DRAWN) break;
            e.setTarget(target);
            // angry, as when hurt: an Enderman's own goals keep a target it is angry at, and drop one merely set
            if (e instanceof NeutralMob neutral) {
                neutral.setPersistentAngerTarget(EntityReference.of(target));
                neutral.startPersistentAngerTimer();
            }
            drawn++;
        }
        return drawn;
    }
}

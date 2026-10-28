package com.panda.promisedend.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.panda.promisedend.block.WispLightBlock;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.registry.ModSounds;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Lumen Wisp: the Ancients' lamplighter, a small passive construct drifting low over the Lumen Mire. A player
 * holding Lumen within {@link #NOTICE_RANGE} draws it: it flies at their shoulder and lights the way behind it, a
 * {@link ModBlocks#WISP_LIGHT} every {@link #TRAIL_EVERY} ticks that goes out {@link #TRAIL_TICKS} later. It chimes and
 * glints as it takes up with someone, and no more than {@link #MAX_FOLLOWERS} follow one player. When it breaks it
 * leaves its light where it was for {@link #DEATH_LIGHT_TICKS} (30 s). It never fights.
 */
public class LumenWisp extends PathfinderMob implements GeoEntity {
    /** A player holding Lumen this near draws it. */
    public static final double NOTICE_RANGE = 12.0;
    /** It keeps this near the player it follows (it stops closing in once within). */
    public static final double FOLLOW_NEAR = 2.5;
    /** It flies this far over the player's feet: a lamp at the shoulder. */
    public static final double FOLLOW_HEIGHT = 2.2;
    /** A light on its path every this many ticks while it follows, each lit this long. */
    public static final int TRAIL_EVERY = 10, TRAIL_TICKS = 100;
    /** What it leaves where it breaks. */
    public static final int DEATH_LIGHT_TICKS = 600;
    /** How far from where it first drifted it wanders when it follows nobody, and how high over the ground. */
    public static final double WANDER_RADIUS = 8.0, WANDER_LOW = 1.0, WANDER_HIGH = 3.5;
    /** At most this many wisps within 32 blocks for one more to spawn. */
    public static final int LOCAL_CAP = 3;
    /** At most this many wisps follow one player: a lamp or two, not a swarm. */
    public static final int MAX_FOLLOWERS = 2;
    public static final float MODEL_SCALE = 1.0F;

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    /** Every clip the class plays: the GameTest checks the shipped animation file has exactly these. */
    public static final List<String> CLIPS = List.of("idle", "walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable BlockPos home;
    private @Nullable Player following;
    /** Lights it has left since it loaded (not saved): GameTests count them. */
    private int lit;

    public LumenWisp(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
        setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0)
                .add(Attributes.FLYING_SPEED, 0.45)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.FOLLOW_RANGE, NOTICE_RANGE);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanOpenDoors(false);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FollowLumenGoal());
        this.goalSelector.addGoal(2, new DriftGoal());
    }

    /** Whether {@code player} holds Lumen in either hand. */
    public static boolean holdsLumen(Player player) {
        return player.getMainHandItem().is(ModItems.LUMEN.get()) || player.getOffhandItem().is(ModItems.LUMEN.get());
    }

    public @Nullable Player following() {
        return following;
    }

    public int lit() {
        return lit;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (home == null) home = blockPosition();
        if (following != null && this.tickCount % TRAIL_EVERY == 0) {
            // a light it hangs where one already burns only burns longer: count the new ones
            boolean fresh = level.getBlockState(blockPosition()).isAir();
            if (WispLightBlock.place(level, blockPosition(), TRAIL_TICKS) && fresh) lit++;
        }
    }

    /** It never fights: not a player, not another construct. */
    @Override
    public boolean canAttack(net.minecraft.world.entity.LivingEntity target) {
        return false;
    }

    // ---- spawning: drifting over the Lumen Mire's moss, in any light ----------------------------------------------

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return true;
    }

    public static boolean canSpawn(EntityType<LumenWisp> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (reason != EntitySpawnReason.NATURAL) return true;
        if (!level.getBlockState(pos.below()).isSolid()) return false;
        if (!Constructs.terrainAllows(level, pos, com.panda.promisedend.registry.ModTags.MIRE_FALLBACK)) return false;
        return level.getEntitiesOfClass(LumenWisp.class, new AABB(pos).inflate(32)).size() < LOCAL_CAP;
    }

    // ---- a construct, not a creature ------------------------------------------------------------

    /** It breaks into amber glass and lamp shards, and its light stays where it was for 30 s. */
    @Override
    protected void tickDeath() {
        if (level() instanceof ServerLevel server && !isRemoved()) WispLightBlock.place(server, blockPosition(), DEATH_LIGHT_TICKS);
        Constructs.shatter(this, Blocks.STAINED_GLASS.orange().defaultBlockState(), ModBlocks.LUMEN_LAMP.get().defaultBlockState());
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.LUMEN_WISP_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.LUMEN_WISP_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LUMEN_WISP_DEATH.get();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("home", BlockPos.CODEC, home);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.home = input.read("home", BlockPos.CODEC).orElse(null);
    }

    // ---- GeckoLib -------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<LumenWisp>("motion", 4, test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** Flies at the shoulder of the nearest player holding Lumen, while they hold it and stay within reach. */
    final class FollowLumenGoal extends Goal {
        FollowLumenGoal() {
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        private @Nullable Player find() {
            return level().getNearestPlayer(getX(), getY(), getZ(), NOTICE_RANGE,
                    e -> e instanceof Player p && !p.isSpectator() && holdsLumen(p) && followers(p) < MAX_FOLLOWERS);
        }

        /** How many other wisps follow {@code player}. */
        private int followers(Player player) {
            return level().getEntitiesOfClass(LumenWisp.class, player.getBoundingBox().inflate(NOTICE_RANGE * 1.5),
                    w -> w != LumenWisp.this && w.following() == player).size();
        }

        @Override
        public boolean canUse() {
            following = find();
            return following != null;
        }

        @Override
        public boolean canContinueToUse() {
            return following != null && following.isAlive() && holdsLumen(following) && distanceTo(following) <= NOTICE_RANGE * 1.5;
        }

        @Override
        public void tick() {
            if (following == null) return;
            getLookControl().setLookAt(following, 30.0F, 30.0F);
            // a little behind and to the side of them, over their shoulder, so it lights where they've walked
            Vec3 look = following.getLookAngle().multiply(1, 0, 1);
            if (look.lengthSqr() < 1e-4) look = new Vec3(0, 0, 1);
            look = look.normalize();
            Vec3 spot = following.position().add(-look.x * 1.2 + look.z * 1.0, FOLLOW_HEIGHT, -look.z * 1.2 - look.x * 1.0);
            if (position().distanceTo(spot) > 0.6) getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, distanceTo(following) > FOLLOW_NEAR * 2 ? 1.4 : 0.8);
        }

        @Override
        public void start() {
            // it takes up with them: a chime and a glint, so the Lumen's pull is seen the first time
            playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.6F);
            if (level() instanceof ServerLevel server)
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, getX(), getY() + getBbHeight() * 0.5, getZ(), 6, 0.2, 0.2, 0.2, 0.02);
        }

        @Override
        public void stop() {
            following = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    /** Drifts low over the ground round where it first drifted: a new spot every few seconds, steered to every tick. */
    final class DriftGoal extends Goal {
        private int wait;
        private @Nullable Vec3 spot;

        DriftGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return home != null && following == null;
        }

        @Override
        public void tick() {
            if (--wait <= 0 || spot == null) {
                wait = 50 + getRandom().nextInt(70);
                double a = getRandom().nextDouble() * Math.PI * 2, r = getRandom().nextDouble() * WANDER_RADIUS;
                int x = (int) Math.floor(home.getX() + 0.5 + Math.cos(a) * r), z = (int) Math.floor(home.getZ() + 0.5 + Math.sin(a) * r);
                int ground = level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                if (Math.abs(ground - home.getY()) > 8) ground = home.getY();
                spot = new Vec3(x + 0.5, ground + WANDER_LOW + getRandom().nextDouble() * (WANDER_HIGH - WANDER_LOW), z + 0.5);
            }
            if (position().distanceToSqr(spot) > 0.25) getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, 0.5);
        }

        @Override
        public void stop() {
            spot = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }
}

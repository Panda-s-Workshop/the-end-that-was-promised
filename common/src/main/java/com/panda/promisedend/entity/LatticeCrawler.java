package com.panda.promisedend.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.panda.promisedend.block.PhantomLatticeBlock;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModSounds;
import com.panda.promisedend.registry.ModTags;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The Lattice Crawler: a low frame of raw lattice on six scaffold legs, the Ancients' builder left working at the
 * Unfinished Edge. It climbs lattice as a spider climbs anything (only lattice: a wall of anything else stops it), and
 * bites. Every {@link #TEAR_EVERY} ticks, a target in sight within {@link #TEAR_RANGE} that stands on lattice has the
 * block under them torn out: for {@link #TEAR_TICKS} the block groans and sheds grit, so a step off it is in time, then
 * it becomes Phantom Lattice and re-forms {@link PhantomLatticeBlock#REFORM} ticks later. Only lattice tears; a target
 * on anything else is only bitten. So it is fought off the lattice, or by keeping moving on it.
 *
 * <p>Its state is synced ({@link #state()}, {@link #isClimbing()}), so clients play the clips.
 */
public class LatticeCrawler extends Monster implements GeoEntity {
    /** Ticks between one tear and the next, counted while it hunts. */
    public static final int TEAR_EVERY = 100;
    /** It tears under a target this near and in sight. */
    public static final double TEAR_RANGE = 12.0;
    /** The tear clip is 0.8 s: the block under the target groans through it and goes on its last tick. */
    public static final int TEAR_TICKS = 16;
    /** Its bite. */
    public static final double DAMAGE = 5.0;
    /** The renderer draws the rig at this scale; the hitbox is measured at it. */
    public static final float MODEL_SCALE = 1.0F;
    /** At most this many crawlers within 32 blocks for one more to spawn. */
    public static final int LOCAL_CAP = 2;
    /** The tag a player gets once told what the groan means (the hint is said once). */
    public static final String HINTED = "promisedend.lattice_crawler_hinted";

    public enum State { HUNT, TEAR }

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(LatticeCrawler.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_CLIMBING = SynchedEntityData.defineId(LatticeCrawler.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation CLIMB = RawAnimation.begin().thenLoop("climb");
    private static final RawAnimation TEAR = RawAnimation.begin().thenPlay("tear");
    /** Every clip the class plays: the GameTest checks the shipped animation file has exactly these. */
    public static final List<String> CLIPS = List.of("idle", "walk", "climb", "tear");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int stateTicks;
    private int tearIn = TEAR_EVERY;
    /** The block it is tearing, while it tears. */
    private @Nullable BlockPos tearing;
    /** Tears since it loaded (not saved): GameTests and the harness count them. */
    private int tears;

    public LatticeCrawler(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 36.0)
                .add(Attributes.ARMOR, 6.0)
                .add(Attributes.ATTACK_DAMAGE, DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.4)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, (byte) State.HUNT.ordinal());
        builder.define(DATA_CLIMBING, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new Hunting<>(new MeleeAttackGoal(this, 1.0, false)));
        this.goalSelector.addGoal(6, new Hunting<>(new WaterAvoidingRandomStrollGoal(this, 0.7)));
        this.goalSelector.addGoal(7, new Hunting<>(new LookAtPlayerGoal(this, Player.class, 10.0F)));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new WallClimberNavigation(this, level);
    }

    // ---- climbing: lattice faces only -------------------------------------------------------------

    public boolean isClimbing() {
        return this.entityData.get(DATA_CLIMBING);
    }

    @Override
    public boolean onClimbable() {
        return isClimbing();
    }

    /** Whether a lattice block touches its sides: what it can climb. */
    public boolean againstLattice() {
        AABB box = getBoundingBox().inflate(0.15, 0, 0.15);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY, box.minZ), BlockPos.containing(box.maxX, box.maxY - 1e-3, box.maxZ))) {
            if (level().getBlockState(pos).is(ModTags.LATTICE_CLIMBABLE)) return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide()) this.entityData.set(DATA_CLIMBING, horizontalCollision && againstLattice());
    }

    // ---- the mechanic ---------------------------------------------------------------------------

    public State state() {
        return State.values()[Mth.clamp(this.entityData.get(DATA_STATE), 0, State.values().length - 1)];
    }

    public int tearIn() {
        return tearIn;
    }

    public int tears() {
        return tears;
    }

    public @Nullable BlockPos tearing() {
        return tearing;
    }

    private void setState(State state) {
        this.entityData.set(DATA_STATE, (byte) state.ordinal());
        this.stateTicks = 0;
    }

    /** The lattice block {@code target} stands on, or null when they aren't on lattice (or aren't on the ground). */
    public static @Nullable BlockPos latticeUnder(LivingEntity target) {
        if (!target.onGround()) return null;
        BlockPos under = target.getOnPos();
        return target.level().getBlockState(under).is(ModTags.LATTICE_TEARABLE) ? under : null;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = getTarget();
        switch (state()) {
            case HUNT -> {
                if (target == null || !target.isAlive()) {
                    this.tearIn = Math.max(tearIn, 20);
                    return;
                }
                if (--tearIn > 0) return;
                BlockPos under = canTearAt(target) ? latticeUnder(target) : null;
                if (under != null) startTear(under);
                else this.tearIn = 10; // off the lattice or out of sight: look again in half a second
            }
            case TEAR -> {
                getNavigation().stop();
                if (target != null) getLookControl().setLookAt(target, 30.0F, 30.0F);
                BlockPos at = tearing;
                if (at == null || !level.getBlockState(at).is(ModTags.LATTICE_TEARABLE)) {
                    endTear();
                    return;
                }
                // the block groans and sheds its grit while it goes: a step off it is in time
                BlockState state = level.getBlockState(at);
                if (stateTicks % 3 == 0) {
                    level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5,
                            6, 0.35, 0.02, 0.35, 0.05);
                }
                if (++stateTicks < TEAR_TICKS) return;
                if (PhantomLatticeBlock.tear(level, at, PhantomLatticeBlock.REFORM)) {
                    tears++;
                    hint(level, at);
                }
                endTear();
            }
        }
    }

    /** Whether it may tear under {@code target}: alive, attackable, within {@link #TEAR_RANGE} and in sight. */
    public boolean canTearAt(LivingEntity target) {
        return target.isAlive() && canAttack(target) && distanceTo(target) <= TEAR_RANGE && hasLineOfSight(target);
    }

    /** Starts tearing out the lattice block at {@code at} (public for GameTests): it goes {@link #TEAR_TICKS} later. */
    public void startTear(BlockPos at) {
        this.tearing = at.immutable();
        setState(State.TEAR);
        getNavigation().stop();
        playSound(ModSounds.LATTICE_CRAWLER_TEAR.get(), 1.0F, 1.0F);
        level().playSound(null, at, ModSounds.LATTICE_GROAN.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    private void endTear() {
        this.tearing = null;
        this.tearIn = TEAR_EVERY;
        setState(State.HUNT);
    }

    /** The first time one tears the lattice out from under a player, what the groan meant, once. */
    private void hint(ServerLevel level, BlockPos at) {
        for (var player : level.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class, new AABB(at).inflate(2))) {
            if (player.entityTags().contains(HINTED)) continue;
            player.addTag(HINTED);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("hint.promisedend.lattice_crawler.tear"));
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && Constructs.mayAttack(target);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    // ---- spawning: at the Edge, in any light ------------------------------------------------------

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return true;
    }

    public static boolean canSpawn(EntityType<LatticeCrawler> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) return false;
        if (reason != EntitySpawnReason.NATURAL) return true;
        return Constructs.roomFor(level, pos, LatticeCrawler.class, LOCAL_CAP);
    }

    // ---- a construct, not a creature ------------------------------------------------------------

    @Override
    protected void tickDeath() {
        Constructs.shatter(this, ModBlocks.RAW_LATTICE.get().defaultBlockState(), ModBlocks.LATTICE_SCAFFOLD.get().defaultBlockState());
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return state() == State.HUNT ? ModSounds.LATTICE_CRAWLER_AMBIENT.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.LATTICE_CRAWLER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.LATTICE_CRAWLER_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(ModSounds.LATTICE_CRAWLER_STEP.get(), 0.4F, 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("tear_in", tearIn);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // a reload mid-tear ends it, the block whole
        this.tearIn = input.getIntOr("tear_in", TEAR_EVERY);
        this.tearing = null;
        setState(State.HUNT);
    }

    // ---- GeckoLib -------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<LatticeCrawler>("motion", 2, test -> {
            LatticeCrawler c = test.animatable();
            if (c.state() == State.TEAR) return test.setAndContinue(TEAR);
            if (c.isClimbing()) return test.setAndContinue(CLIMB);
            return test.setAndContinue(test.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** A goal that runs only while it hunts (tearing, it does nothing else). */
    final class Hunting<G extends Goal> extends Goal {
        private final G goal;

        Hunting(G goal) {
            this.goal = goal;
            setFlags(goal.getFlags());
        }

        private boolean hunting() {
            return state() == State.HUNT;
        }

        @Override
        public boolean canUse() {
            return hunting() && goal.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return hunting() && goal.canContinueToUse();
        }

        @Override
        public void start() {
            goal.start();
        }

        @Override
        public void stop() {
            goal.stop();
        }

        @Override
        public void tick() {
            goal.tick();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return goal.requiresUpdateEveryTick();
        }

        @Override
        public boolean isInterruptable() {
            return goal.isInterruptable();
        }
    }
}

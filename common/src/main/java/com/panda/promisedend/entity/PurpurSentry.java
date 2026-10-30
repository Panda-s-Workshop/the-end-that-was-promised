package com.panda.promisedend.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.panda.promisedend.logic.Balance;
import com.panda.promisedend.logic.ShieldArc;
import com.panda.promisedend.registry.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Purpur Sentry: the Ancients' guard, a squat purpur construct with a tower shield and a lance. At a target
 * within {@link #SHIELD_RANGE} it plants itself and raises its shield for {@link #SHIELD_TICKS} (3 s), turning to keep
 * it on the target no faster than {@link #TURN_RATE} a tick; a blow from within {@link #FRONT_HALF_ANGLE} of its front
 * meanwhile rings off it and a shot from there glances back ({@link ShieldArc}), so it is fought from the side or the
 * back. The shield's last {@link #SHIELD_WARN} ticks it grates and its lance glints; then it lunges along a line it
 * chose at the start ({@link #LUNGE_WINDUP} ticks crouched, the lance lit, then {@link #LUNGE_DASH} ticks of dash,
 * stopping short of a drop), striking the first time it meets the target ({@link #DAMAGE}), and stands open for
 * {@link #RECOVER_TICKS} before it guards again. It walks up to a target but strikes only by the lunge.
 *
 * <p>Its state is synced ({@link #state()}), so clients play the clips.
 */
public class PurpurSentry extends Monster implements GeoEntity {
    /** It raises its shield at a target this near and in sight. */
    public static final double SHIELD_RANGE = 8.0;
    /** The shield stays up 3 s, then it lunges; it warns the last half second of it. */
    public static final int SHIELD_TICKS = 60, SHIELD_WARN = 10;
    /** A target further than this gets away: the shield comes down. */
    public static final double GIVE_UP_RANGE = SHIELD_RANGE * 1.5;
    /** While its shield is up it turns at most this many degrees a tick: a player circling it gets round. */
    public static final float TURN_RATE = 4.0F;
    /** A blow from within this many degrees of its front is stopped by the shield. */
    public static final double FRONT_HALF_ANGLE = 60.0;
    /** The lunge: crouched this long (0.4 s, the tell), then dashing this long at this speed along the line it chose. */
    public static final int LUNGE_WINDUP = 8, LUNGE_DASH = 8, LUNGE_TICKS = LUNGE_WINDUP + LUNGE_DASH;
    public static final double LUNGE_SPEED = 0.8;
    /** Its dash vaults a step with a jump's lift, meets a target this much nearer than touching, and brakes to this. */
    public static final double VAULT = 0.42, LUNGE_REACH = 0.4, BRAKE = 0.2;
    /** No ground this many blocks under the next step, the dash stops at the edge. */
    public static final int EDGE_DROP = 3;
    /** The lunge's damage (its attack), its health and armour (no toughness): the balance targets are pinned on these. */
    public static final double DAMAGE = Balance.SENTRY_LUNGE, HEALTH = Balance.SENTRY_HEALTH, ARMOUR = Balance.SENTRY_ARMOUR;
    /** After a lunge it stands open this long; then this long more before its shield comes up again. */
    public static final int RECOVER_TICKS = 30, GUARD_COOLDOWN = 20;
    /** The renderer draws the rig at this scale; the hitbox is measured at it. */
    public static final float MODEL_SCALE = 1.25F;

    public enum State { GUARD, SHIELD, LUNGE, RECOVER }

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(PurpurSentry.class, EntityDataSerializers.BYTE);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SHIELD = RawAnimation.begin().thenLoop("shield");
    private static final RawAnimation RECOVER = RawAnimation.begin().thenLoop("recover");
    private static final RawAnimation LUNGE = RawAnimation.begin().thenPlay("lunge");
    /** Every clip the class plays: the GameTest checks the shipped animation file has exactly these. */
    public static final List<String> CLIPS = List.of("idle", "walk", "shield", "recover", "lunge");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int stateTicks;
    private int cooldown;
    private boolean struck;
    private @Nullable Vec3 lungeLine;
    /** Blows its shield stopped, and lunges that landed, since it loaded (not saved): GameTests count them. */
    private int blocked, lunges;

    public PurpurSentry(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ARMOR, ARMOUR)
                .add(Attributes.ATTACK_DAMAGE, DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.22)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, (byte) State.GUARD.ordinal());
    }

    /** How far a placed Sentry strays from where it stood when it's not fighting. */
    public static final int HOME_RADIUS = 3;

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new Guarding<>(new ApproachGoal(this, 1.0, 3.0)));
        this.goalSelector.addGoal(5, new Guarding<>(new net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal(this, 0.8)));
        this.goalSelector.addGoal(6, new Guarding<>(new WaterAvoidingRandomStrollGoal(this, 0.6)));
        this.goalSelector.addGoal(7, new Guarding<>(new LookAtPlayerGoal(this, Player.class, 8.0F)));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- the mechanic ---------------------------------------------------------------------------

    public State state() {
        return State.values()[Mth.clamp(this.entityData.get(DATA_STATE), 0, State.values().length - 1)];
    }

    public int blocked() {
        return blocked;
    }

    public int lunges() {
        return lunges;
    }

    private void setState(State state) {
        this.entityData.set(DATA_STATE, (byte) state.ordinal());
        this.stateTicks = 0;
    }

    /** Whether a blow from {@code from} comes within {@link #FRONT_HALF_ANGLE} of where its body faces. */
    public boolean inFront(Vec3 from) {
        return ShieldArc.inFront(this.yBodyRot, FRONT_HALF_ANGLE, from.x - getX(), from.z - getZ());
    }

    /** A player told its rule: once a player. */
    public static final String HINTED = "promisedend.purpur_sentry_hinted";

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        // the shield stops a blow at its front, as a player's shield does; a shot it met is in deflection
        Vec3 from = source.getSourcePosition();
        if (state() == State.SHIELD && from != null && !(source.getDirectEntity() instanceof Projectile)
                && !source.is(DamageTypeTags.BYPASSES_SHIELD) && inFront(from)) {
            ring(level, from);
            // the first blow a player's has turned, its rule, once
            if (source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player && !player.entityTags().contains(HINTED)) {
                player.addTag(HINTED);
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("hint.promisedend.purpur_sentry.flank"));
            }
            return false;
        }
        boolean shielded = state() == State.SHIELD && from != null;
        boolean hurt = super.hurtServer(level, source, amount);
        // round its raised shield: the blow that teaches its rule
        if (hurt && shielded) Constructs.award(source.getEntity(), com.panda.promisedend.logic.CodexBook.FLANK_SENTRY, "flanked");
        return hurt;
    }

    /** A shot coming at its raised shield from in front, not dropping over it, glances back the way it came. */
    @Override
    public ProjectileDeflection deflection(Projectile projectile) {
        Vec3 m = projectile.getDeltaMovement();
        if (state() == State.SHIELD && ShieldArc.stopsShot(this.yBodyRot, FRONT_HALF_ANGLE, m.x, m.y, m.z)) return this::glance;
        return super.deflection(projectile);
    }

    private void glance(Projectile projectile, @Nullable Entity hit, RandomSource random, Vec3 at) {
        ProjectileDeflection.REVERSE.deflect(projectile, hit, random, at);
        if (level() instanceof ServerLevel level) ring(level, projectile.position());
    }

    /** The shield took one: it rings, and chips of purpur fly off its face towards {@code from}. */
    private void ring(ServerLevel level, Vec3 from) {
        blocked++;
        playSound(ModSounds.PURPUR_SENTRY_BLOCK.get(), 1.0F, 0.9F + getRandom().nextFloat() * 0.2F);
        var chip = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PURPUR_BLOCK.defaultBlockState());
        Vec3 out = from.subtract(position()).multiply(1, 0, 1);
        out = out.lengthSqr() < 1e-6 ? facing() : out.normalize();
        Vec3 at = position().add(out.scale(0.7)).add(0, getBbHeight() * 0.55, 0);
        level.sendParticles(chip, at.x, at.y, at.z, 8, 0.15, 0.2, 0.15, 0.1);
    }

    /** Where its body faces, flat. */
    private Vec3 facing() {
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    /** A few lit glints along its lance (lit, so End light doesn't swallow them): the tell before a lunge. */
    private void glint(ServerLevel level, int count) {
        Vec3 tip = position().add(facing().scale(1.1)).add(0, getBbHeight() * 0.45, 0);
        level.sendParticles(ParticleTypes.END_ROD, tip.x, tip.y, tip.z, count, 0.25, 0.1, 0.25, 0.01);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        Constructs.meet(this, "purpur_sentry");
        // one a structure placed (or a Sentinel called) keeps near where it stood, and goes back after a fight
        if (!hasHome() && isPersistenceRequired()) setHomeTo(blockPosition(), HOME_RADIUS);
        LivingEntity target = getTarget();
        boolean live = target != null && target.isAlive() && canAttack(target);
        switch (state()) {
            case GUARD -> {
                if (cooldown > 0) cooldown--;
                if (cooldown <= 0 && live && distanceTo(target) <= SHIELD_RANGE && hasLineOfSight(target)) raiseShield();
            }
            case SHIELD -> {
                getNavigation().stop();
                setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
                if (!live || distanceTo(target) > GIVE_UP_RANGE) {
                    setState(State.GUARD);
                    return;
                }
                turnTowards(target, TURN_RATE);
                ++stateTicks;
                if (stateTicks == SHIELD_TICKS - SHIELD_WARN) playSound(ModSounds.PURPUR_SENTRY_SHIELD.get(), 1.0F, 0.6F);
                if (stateTicks > SHIELD_TICKS - SHIELD_WARN && stateTicks % 3 == 0) glint(level, 2);
                if (stateTicks >= SHIELD_TICKS) startLunge(target);
            }
            case LUNGE -> {
                getNavigation().stop();
                stateTicks++;
                if (stateTicks <= LUNGE_WINDUP || lungeLine == null) {
                    setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
                    glint(level, 3);
                } else if (!groundAhead(level)) {
                    // the line runs off an edge: it pulls up there rather than throw itself after a dodger
                    setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
                    stateTicks = LUNGE_TICKS;
                } else {
                    // a step in its way it vaults, as a charging guard would
                    double up = horizontalCollision && onGround() ? VAULT : getDeltaMovement().y;
                    setDeltaMovement(lungeLine.x * LUNGE_SPEED, up, lungeLine.z * LUNGE_SPEED);
                    if (!struck && live && getBoundingBox().inflate(LUNGE_REACH).intersects(target.getBoundingBox())) {
                        struck = true;
                        if (doHurtTarget(level, target)) lunges++;
                        setDeltaMovement(getDeltaMovement().multiply(BRAKE, 1, BRAKE));
                    }
                }
                if (stateTicks >= LUNGE_TICKS) {
                    setState(State.RECOVER);
                    setDeltaMovement(getDeltaMovement().multiply(BRAKE, 1, BRAKE));
                }
            }
            case RECOVER -> {
                getNavigation().stop();
                if (++stateTicks >= RECOVER_TICKS) {
                    setState(State.GUARD);
                    this.cooldown = GUARD_COOLDOWN;
                }
            }
        }
    }

    /** Whether its next dash step lands on something: a solid block within {@link #EDGE_DROP} under it. */
    private boolean groundAhead(ServerLevel level) {
        if (lungeLine == null) return false;
        double ahead = getBbWidth() * 0.5 + LUNGE_SPEED;
        BlockPos step = BlockPos.containing(getX() + lungeLine.x * ahead, getY() + 0.5, getZ() + lungeLine.z * ahead);
        for (int down = 0; down <= EDGE_DROP; down++) {
            BlockPos below = step.below(down);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) return true;
        }
        return false;
    }

    /** Turns its body and head towards {@code target}, at most {@code rate} degrees. */
    private void turnTowards(LivingEntity target, float rate) {
        float want = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
        float yaw = Mth.approachDegrees(this.yBodyRot, want, rate);
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    /** Plants itself and raises the shield (public for GameTests). */
    public void raiseShield() {
        setState(State.SHIELD);
        getNavigation().stop();
        playSound(ModSounds.PURPUR_SENTRY_SHIELD.get(), 1.0F, 1.0F);
    }

    /** Crouches for a lunge at {@code target}: the line is set now, so a step aside is a dodge. */
    private void startLunge(LivingEntity target) {
        Vec3 to = target.position().subtract(position()).multiply(1, 0, 1);
        this.lungeLine = to.lengthSqr() < 1e-6 ? null : to.normalize();
        this.struck = false;
        setState(State.LUNGE);
        playSound(ModSounds.PURPUR_SENTRY_LUNGE.get(), 1.0F, 1.0F);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && Constructs.mayAttack(target);
    }

    /** Planted behind its shield, it isn't shoved aside. */
    @Override
    public boolean isPushable() {
        return state() != State.SHIELD && super.isPushable();
    }

    // ---- a construct, not a creature ------------------------------------------------------------

    @Override
    protected void tickDeath() {
        Constructs.shatter(this, Blocks.PURPUR_BLOCK.defaultBlockState(), Blocks.PURPUR_PILLAR.defaultBlockState());
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return state() == State.GUARD ? ModSounds.PURPUR_SENTRY_AMBIENT.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.PURPUR_SENTRY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.PURPUR_SENTRY_DEATH.get();
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // a reload mid-lunge ends it
        setState(State.GUARD);
    }

    // ---- GeckoLib -------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<PurpurSentry>("motion", 3, test -> switch (test.animatable().state()) {
            case SHIELD -> test.setAndContinue(SHIELD);
            case LUNGE -> test.setAndContinue(LUNGE);
            case RECOVER -> test.setAndContinue(RECOVER);
            case GUARD -> test.setAndContinue(test.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** A goal that runs only while it guards (shield up, lunging or recovering, it does nothing else). */
    final class Guarding<G extends Goal> extends Goal {
        private final G goal;

        Guarding(G goal) {
            this.goal = goal;
            setFlags(goal.getFlags());
        }

        private boolean guarding() {
            return state() == State.GUARD;
        }

        @Override
        public boolean canUse() {
            return guarding() && goal.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return guarding() && goal.canContinueToUse();
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

package com.panda.promisedend.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.panda.promisedend.registry.ModParticles;
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
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Chorus Weaver: a construct of chorus wood grown among the Thicket's plants and standing as one of them, still
 * and silent, until a player comes within {@link #REVEAL_RANGE} (or it is hurt). Then it unfurls, and every
 * {@link #LASH_COOLDOWN} ticks, at a target in sight within {@link #LASH_RANGE}, it throws a root vine: half a second
 * drawn back (chorus motes gathering at it), then the vine lands, slowing the target (Slowness IV, 2 s) and pulling it
 * towards the weaver over the ground between them, where it strikes. With no target near for {@link #DISGUISE_AFTER}
 * ticks it roots again, as a plant, square to the grid and in the middle of its block, as the Thicket's plants stand.
 * A player wakes it only from where it can see them: not through a wall.
 *
 * <p>Its state is synced ({@link #state()}, {@link #lashTargetId()}), so clients play the clips; the vine is drawn as
 * chorus particles along its line when it lands.
 */
public class ChorusWeaver extends Monster implements GeoEntity {
    /** How near a player comes before it unfurls. */
    public static final double REVEAL_RANGE = 6.0;
    /** The reveal clip is 1.0 s: it does nothing else meanwhile. */
    public static final int REVEAL_TICKS = 20;
    /** The vine: thrown at a target this near, in sight, never closer than its reach. */
    public static final double LASH_RANGE = 12.0, LASH_MIN = 2.5;
    /** The lash clip is 0.75 s: drawn back for the first {@link #LASH_WINDUP} ticks, then the vine lands. */
    public static final int LASH_TICKS = 15, LASH_WINDUP = 10;
    /** Ticks between one vine and the next. */
    public static final int LASH_COOLDOWN = 80;
    /** Slowness IV (amplifier 3) for 2 s. */
    public static final int SLOW_TICKS = 40, SLOW_AMPLIFIER = 3;
    /** The pull: this fast towards the weaver, and up a little so it drags over the ground's lips. */
    public static final double PULL = 1.1, PULL_UP = 0.3;
    /** It pulls a target no nearer than this, and only over ground: no solid block this far under the line, no pull. */
    public static final double PULL_STOP = 1.2, PULL_EASE = 0.35;
    public static final int PULL_DROP = 4;
    /** Ticks with no target before it roots again as a plant. */
    public static final int DISGUISE_AFTER = 200;
    /** The renderer draws the rig at this scale; the hitbox is measured at it. */
    public static final float MODEL_SCALE = 1.25F;

    public enum State { DISGUISED, REVEAL, ACTIVE, LASH }

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(ChorusWeaver.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(ChorusWeaver.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation DISGUISED = RawAnimation.begin().thenLoop("disguised");
    private static final RawAnimation REVEAL = RawAnimation.begin().thenPlay("reveal");
    private static final RawAnimation LASH = RawAnimation.begin().thenPlay("lash");
    /** Every clip the class plays: the GameTest checks the shipped animation file has exactly these. */
    public static final List<String> CLIPS = List.of("idle", "walk", "disguised", "reveal", "lash");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int stateTicks;
    private int cooldown;
    private int alone;
    /** Vines landed since it loaded (not saved): GameTests and the harness count them. */
    private int lashes;

    public ChorusWeaver(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 34.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 5.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, (byte) State.DISGUISED.ordinal());
        builder.define(DATA_TARGET, -1);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new Awake<>(new MeleeAttackGoal(this, 1.0, false)));
        this.goalSelector.addGoal(6, new Awake<>(new WaterAvoidingRandomStrollGoal(this, 0.6)));
        this.goalSelector.addGoal(7, new Awake<>(new LookAtPlayerGoal(this, Player.class, 8.0F)));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- the mechanic ---------------------------------------------------------------------------

    public State state() {
        return State.values()[Mth.clamp(this.entityData.get(DATA_STATE), 0, State.values().length - 1)];
    }

    public boolean disguised() {
        return state() == State.DISGUISED;
    }

    public int lashTargetId() {
        return this.entityData.get(DATA_TARGET);
    }

    public int lashes() {
        return lashes;
    }

    private void setState(State state, @Nullable LivingEntity target) {
        this.entityData.set(DATA_STATE, (byte) state.ordinal());
        this.entityData.set(DATA_TARGET, target == null ? -1 : target.getId());
        this.stateTicks = 0;
    }

    /** Unfurls from its disguise (public for GameTests). */
    public void reveal() {
        if (!disguised()) return;
        setState(State.REVEAL, null);
        this.cooldown = LASH_WINDUP; // the first vine comes soon after it wakes
        playSound(ModSounds.CHORUS_WEAVER_REVEAL.get(), 1.2F, 1.0F);
    }

    /** Roots again as a plant: still, facing as it stands. */
    public void disguise() {
        setState(State.DISGUISED, null);
        setTarget(null);
        getNavigation().stop();
        this.alone = 0;
        // a plant grows square in its block: turned to the nearest quarter, set in the middle if there is room
        float yaw = Math.round(getYRot() / 90.0F) * 90.0F;
        double x = Mth.floor(getX()) + 0.5, z = Mth.floor(getZ()) + 0.5;
        if (!level().noCollision(this, getDimensions(getPose()).makeBoundingBox(x, getY(), z))) {
            x = getX();
            z = getZ();
        }
        snapTo(x, getY(), z, yaw, 0);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        if (hurt && disguised()) reveal();
        return hurt;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = getTarget();
        switch (state()) {
            case DISGUISED -> {
                // still as a plant: no turning to whoever walks by
                getNavigation().stop();
                setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
                Player near = level.getNearestPlayer(getX(), getY(), getZ(), REVEAL_RANGE,
                        p -> p instanceof Player pl && !pl.isCreative() && !pl.isSpectator() && hasLineOfSight(pl));
                if (near != null) {
                    setTarget(near);
                    reveal();
                }
            }
            case REVEAL -> {
                getNavigation().stop();
                if (++stateTicks >= REVEAL_TICKS) setState(State.ACTIVE, null);
            }
            case ACTIVE -> {
                if (cooldown > 0) cooldown--;
                if (target == null || !target.isAlive()) {
                    if (++alone >= DISGUISE_AFTER) disguise();
                    return;
                }
                alone = 0;
                if (cooldown <= 0 && canLash(target)) startLash(target);
            }
            case LASH -> {
                getNavigation().stop();
                LivingEntity lashed = level.getEntity(lashTargetId()) instanceof LivingEntity l ? l : null;
                if (lashed != null) getLookControl().setLookAt(lashed, 360.0F, 90.0F);
                ++stateTicks;
                // drawn back, the vine gathers at its arm: lit chorus motes, so the windup reads in the End's dark
                if (stateTicks < LASH_WINDUP && stateTicks % 2 == 0)
                    level.sendParticles(ModParticles.CHORUS_VINE.get(), getX(), getY() + getBbHeight() * 0.7, getZ(), 3, 0.35, 0.3, 0.35, 0.0);
                if (stateTicks == LASH_WINDUP && lashed != null && canLash(lashed)) land(level, lashed);
                if (stateTicks >= LASH_TICKS) {
                    setState(State.ACTIVE, null);
                    this.cooldown = LASH_COOLDOWN;
                }
            }
        }
    }

    /** Whether a vine can reach {@code target}: alive, attackable, within range but past its reach, and in sight. */
    public boolean canLash(LivingEntity target) {
        if (!target.isAlive() || !canAttack(target)) return false;
        double d = distanceTo(target);
        if (d > LASH_RANGE || d < LASH_MIN) return false;
        return level().clip(new ClipContext(getEyePosition(), target.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    /** Draws a vine back at {@code target} (public for GameTests): it lands {@link #LASH_WINDUP} ticks later. */
    public void startLash(LivingEntity target) {
        setState(State.LASH, target);
        getNavigation().stop();
        playSound(ModSounds.CHORUS_WEAVER_LASH.get(), 1.2F, 1.0F);
    }

    /** The vine lands: the target slowed and pulled in, the vine drawn as chorus along its line. */
    private void land(ServerLevel level, LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, SLOW_AMPLIFIER), this);
        Vec3 to = position().subtract(target.position());
        Vec3 flat = new Vec3(to.x, 0, to.z);
        double d = flat.length();
        if (d > 1e-4 && groundBetween(level, target.position(), position())) {
            // no further than to its reach: pulled to the weaver, not past it
            double speed = Math.min(PULL, (d - PULL_STOP) * PULL_EASE);
            Vec3 pull = flat.scale(Math.max(0, speed) / d).add(0, PULL_UP, 0);
            target.setDeltaMovement(pull);
            // to the trackers, and (syncVelocity) to a pulled player's own client too
            target.needsSync = true;
            target.syncVelocity = true;
        }
        lashes++;
        playSound(ModSounds.CHORUS_WEAVER_VINE_HIT.get(), 1.2F, 1.0F);
        Vec3 from = getEyePosition().subtract(0, 0.4, 0), at = target.position().add(0, target.getBbHeight() * 0.5, 0);
        int steps = (int) Math.ceil(from.distanceTo(at) * 3);
        // the vine is lit motes along its line (a block's chips take the End's dim light and vanish), chips of chorus
        // among them for its stuff
        var chips = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.CHORUS_PLANT.defaultBlockState());
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(at, i / (double) steps);
            level.sendParticles(ModParticles.CHORUS_VINE.get(), p.x, p.y, p.z, 1, 0.03, 0.03, 0.03, 0.0);
            if (i % 3 == 0) level.sendParticles(chips, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /** Whether there is ground all along the flat line from {@code from} to {@code to}: a solid block within {@link #PULL_DROP} under each step. */
    private boolean groundBetween(ServerLevel level, Vec3 from, Vec3 to) {
        double d = Math.hypot(to.x - from.x, to.z - from.z);
        int steps = Math.max(1, (int) Math.ceil(d));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = from.lerp(to, i / (double) steps);
            BlockPos column = BlockPos.containing(p.x, Math.max(from.y, to.y) + 0.5, p.z);
            boolean ground = false;
            for (int down = 0; down <= PULL_DROP && !ground; down++) {
                BlockPos below = column.below(down);
                ground = !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
            }
            if (!ground) return false;
        }
        return true;
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && Constructs.mayAttack(target);
    }

    /** Disguised it is a plant: nothing pushes it. */
    @Override
    public boolean isPushable() {
        return !disguised() && super.isPushable();
    }

    // ---- spawning: among the Thicket's plants, in any light (the End has sky light, which a monster shuns) -------

    /** A weaver near another is too many: at most this many within 32 blocks. */
    public static final int LOCAL_CAP = 2;

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return true;
    }

    public static boolean canSpawn(EntityType<ChorusWeaver> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) return false;
        if (reason != EntitySpawnReason.NATURAL) return true;
        if (!Constructs.terrainAllows(level, pos, com.panda.promisedend.registry.ModTags.GROVE_FALLBACK)) return false;
        return level.getEntitiesOfClass(ChorusWeaver.class, new AABB(pos).inflate(32)).size() < LOCAL_CAP;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        // it grows facing any way, as a plant does
        float yaw = level.getRandom().nextInt(4) * 90.0F;
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // ---- a construct, not a creature ------------------------------------------------------------

    @Override
    protected void tickDeath() {
        Constructs.shatter(this, Blocks.CHORUS_PLANT.defaultBlockState(), Blocks.CHORUS_FLOWER.defaultBlockState());
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return state() == State.ACTIVE ? ModSounds.CHORUS_WEAVER_AMBIENT.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CHORUS_WEAVER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CHORUS_WEAVER_DEATH.get();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("disguised", disguised());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // a reload mid-lash ends it; a weaver that was awake stays awake
        setState(input.getBooleanOr("disguised", true) ? State.DISGUISED : State.ACTIVE, null);
    }

    // ---- GeckoLib -------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<ChorusWeaver>("motion", 3, test -> switch (test.animatable().state()) {
            case DISGUISED -> test.setAndContinue(DISGUISED);
            case REVEAL -> test.setAndContinue(REVEAL);
            case LASH -> test.setAndContinue(LASH);
            case ACTIVE -> test.setAndContinue(test.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** A goal that runs only while the weaver is awake and not lashing (disguised, it is a plant). */
    final class Awake<G extends net.minecraft.world.entity.ai.goal.Goal> extends net.minecraft.world.entity.ai.goal.Goal {
        private final G goal;

        Awake(G goal) {
            this.goal = goal;
            setFlags(goal.getFlags());
        }

        private boolean awake() {
            return state() == State.ACTIVE;
        }

        @Override
        public boolean canUse() {
            return awake() && goal.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return awake() && goal.canContinueToUse();
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

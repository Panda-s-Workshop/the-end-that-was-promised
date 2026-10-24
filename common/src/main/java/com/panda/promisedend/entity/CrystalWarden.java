package com.panda.promisedend.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModSounds;
import com.panda.promisedend.registry.ModTags;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Crystal Warden: a construct that hovers by its pillar. When it sees a player it locks on for a second
 * (its shutters open, a thin thread of light runs to the target, it resonates), then holds a beam on them at 2 damage a
 * second for up to 4 seconds. Breaking line of sight ends either step; then it rests 2 seconds before the next.
 *
 * <p>The beam does its own damage type, {@code promisedend:crystal_beam} (data/promisedend/damage_type), which the
 * vanilla tags send through armour and the hurt cooldown without knockback, so it holds 2 a second whatever else hurts
 * the target. It never works on the dragon or another construct ({@code #promisedend:constructs}). Every step is synced
 * ({@link #phase()}, {@link #beamTargetId()}) so clients play the clips and draw the thread and the beam.
 */
public class CrystalWarden extends Monster implements GeoEntity {
    /** The lock-on tell: the lock_on clip is 1.0 s. */
    public static final int LOCK_ON_TICKS = 20;
    /** The longest a beam is held. */
    public static final int BEAM_TICKS = 80;
    /** The rest after a beam, or after the target slipped out of sight. */
    public static final int REST_TICKS = 40;
    /** One point of damage every 10 ticks: 2 a second. */
    public static final int DAMAGE_INTERVAL = 10;
    public static final float BEAM_DAMAGE = 1.0F;
    public static final double BEAM_RANGE = 16.0;
    /** How far from its pillar it drifts while it hovers. */
    public static final double HOVER_RADIUS = 3.0;
    /** The beam's sound is about 1.4 s long: it is played again this often while the beam lasts. */
    public static final int BEAM_SOUND_INTERVAL = 30;
    /** The renderer draws the rig at this scale; the hitbox and the lens below are measured at it. */
    public static final float MODEL_SCALE = 1.5F;
    /**
     * The lens's face, in blocks, with the front shutter open as it is for the beam: 13.23 px up and 7.18 px forward on
     * the model, drawn at {@link #MODEL_SCALE}. The GameTest {@code construct_assets_match} recomputes it from the files.
     */
    public static final float LENS_HEIGHT = 1.24F;
    public static final double LENS_FORWARD = 0.67;
    public static final ResourceKey<DamageType> BEAM_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, PromisedEnd.id("crystal_beam"));

    public enum Phase { IDLE, LOCK, BEAM }

    private static final EntityDataAccessor<Byte> DATA_PHASE = SynchedEntityData.defineId(CrystalWarden.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_TARGET = SynchedEntityData.defineId(CrystalWarden.class, EntityDataSerializers.INT);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation LOCK_ON = RawAnimation.begin().thenPlay("lock_on");
    private static final RawAnimation BEAMING = RawAnimation.begin().thenLoop("beam");
    /** Every clip the class plays: the GameTest checks the shipped animation file has exactly these. */
    public static final List<String> CLIPS = List.of("idle", "walk", "lock_on", "beam");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private @Nullable BlockPos home;
    private int phaseTicks;
    private int rest;
    /** Clients count the ticks since the phase last changed themselves, for the lock-on thread's charge. */
    private Phase clientPhase = Phase.IDLE;
    private int clientPhaseTicks;

    public CrystalWarden(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
        // a flying move control only lifts gravity once it first moves: without this it dropped to the floor
        setNoGravity(true);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.FLYING_SPEED, 0.35)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PHASE, (byte) Phase.IDLE.ordinal());
        builder.define(DATA_TARGET, -1);
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
        this.goalSelector.addGoal(1, new HoverGoal());
        this.goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, (float) BEAM_RANGE));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- the mechanic ---------------------------------------------------------------------------

    public Phase phase() {
        return Phase.values()[Mth.clamp(this.entityData.get(DATA_PHASE), 0, Phase.values().length - 1)];
    }

    public int beamTargetId() {
        return this.entityData.get(DATA_TARGET);
    }

    public int phaseTicks() {
        return phaseTicks;
    }

    /** The pillar it guards; it hovers within {@link #HOVER_RADIUS} of this. Unset, it is where it first ticked. */
    public void setHome(BlockPos home) {
        this.home = home.immutable();
    }

    public @Nullable BlockPos home() {
        return home;
    }

    private void setPhase(Phase phase, @Nullable LivingEntity target) {
        this.entityData.set(DATA_PHASE, (byte) phase.ordinal());
        this.entityData.set(DATA_TARGET, target == null ? -1 : target.getId());
        this.phaseTicks = 0;
    }

    /** Whether it can work on this target now: alive, attackable, within range, and nothing solid between lens and aim. */
    private boolean canBeam(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || !canAttack(target) || distanceToSqr(target) > BEAM_RANGE * BEAM_RANGE) return false;
        Vec3 from = lensPosition(1.0F);
        return level().clip(new ClipContext(from, aimPoint(target, 1.0F), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this))
                .getType() == HitResult.Type.MISS;
    }

    /** It guards the dragon's fight: never the dragon, never another construct, even when one of them hits it. */
    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && !(target instanceof EnderDragon) && !target.is(ModTags.CONSTRUCTS);
    }

    /** Starts the lock-on at a target (public for GameTests). */
    public void startLockOn(LivingEntity target) {
        setPhase(Phase.LOCK, target);
        playSound(ModSounds.CRYSTAL_WARDEN_LOCK_ON.get(), 1.5F, 1.0F);
    }

    /** Ends a lock-on or a beam, with a falling tone so the target hears it end, and rests. */
    private void stopBeam() {
        setPhase(Phase.IDLE, null);
        this.rest = REST_TICKS;
        playSound(ModSounds.CRYSTAL_WARDEN_POWER_DOWN.get(), 1.2F, 1.0F);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (home == null) home = blockPosition();
        LivingEntity target = getTarget();
        switch (phase()) {
            case IDLE -> {
                if (rest > 0) rest--;
                else if (canBeam(target)) startLockOn(target);
            }
            case LOCK -> {
                LivingEntity locked = lockedTarget(level);
                if (!canBeam(locked)) {
                    stopBeam();
                } else {
                    face(locked);
                    if (++phaseTicks >= LOCK_ON_TICKS) {
                        setPhase(Phase.BEAM, locked);
                        playSound(ModSounds.CRYSTAL_WARDEN_BEAM.get(), 1.5F, 1.0F);
                    }
                }
            }
            case BEAM -> {
                LivingEntity locked = lockedTarget(level);
                if (!canBeam(locked) || phaseTicks >= BEAM_TICKS) {
                    stopBeam();
                } else {
                    face(locked);
                    if (phaseTicks % DAMAGE_INTERVAL == 0) {
                        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(BEAM_DAMAGE_TYPE), this, this);
                        locked.hurtServer(level, source, BEAM_DAMAGE);
                    }
                    if (phaseTicks > 0 && phaseTicks % BEAM_SOUND_INTERVAL == 0) playSound(ModSounds.CRYSTAL_WARDEN_BEAM.get(), 1.5F, 1.0F);
                    phaseTicks++;
                }
            }
        }
    }

    private @Nullable LivingEntity lockedTarget(ServerLevel level) {
        return level.getEntity(beamTargetId()) instanceof LivingEntity living ? living : null;
    }

    /** Turns the whole body (the lens is on its front) to the target. */
    private void face(LivingEntity target) {
        float yaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        getLookControl().setLookAt(target, 360.0F, 90.0F);
        // hold still: a flying move control turns the body to where it is going, and the drift it had keeps carrying it
        getMoveControl().setWantedPosition(getX(), getY(), getZ(), 0.0);
        setDeltaMovement(getDeltaMovement().scale(0.6));
    }

    /** Ticks since the phase last changed, as this client saw it. */
    public int clientPhaseTicks() {
        return clientPhaseTicks;
    }

    /** Where the beam lands on a target: the chest, a little below the eyes. */
    public static Vec3 aimPoint(Entity target, float partialTick) {
        return target.getPosition(partialTick).add(0, target.getEyeHeight() * 0.8, 0);
    }

    /** Where the beam leaves from: the lens on its front shutter. */
    public Vec3 lensPosition(float partialTick) {
        double yaw = Math.toRadians(Mth.lerp(partialTick, this.yBodyRotO, this.yBodyRot));
        Vec3 at = getPosition(partialTick);
        return new Vec3(at.x - Math.sin(yaw) * LENS_FORWARD, at.y + LENS_HEIGHT, at.z + Math.cos(yaw) * LENS_FORWARD);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            Phase phase = phase();
            if (phase != clientPhase) {
                clientPhase = phase;
                clientPhaseTicks = 0;
            } else {
                clientPhaseTicks++;
            }
            sparks(phase);
        }
    }

    /** The beam itself is drawn by the renderer; these are its sparks, at the lens and where it lands. */
    private void sparks(Phase phase) {
        if (phase != Phase.BEAM || this.tickCount % 3 != 0 || !(level().getEntity(beamTargetId()) instanceof LivingEntity target)) return;
        Vec3 from = lensPosition(1.0F);
        level().addParticle(ParticleTypes.ELECTRIC_SPARK, from.x, from.y, from.z, 0, 0.02, 0);
        // not on the target's own camera: a particle at the eye fills the screen
        if (target instanceof Player p && p.isLocalPlayer()) return;
        Vec3 at = aimPoint(target, 1.0F);
        level().addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, (this.random.nextDouble() - 0.5) * 0.1, 0.05, (this.random.nextDouble() - 0.5) * 0.1);
    }

    // ---- a construct, not a creature ------------------------------------------------------------

    // no fall damage: it is in #minecraft:fall_damage_immune

    /** It shatters at once instead of tipping over: shards of its crystal, then gone (its loot and xp drop on death). */
    @Override
    protected void tickDeath() {
        if (level() instanceof ServerLevel server && !isRemoved()) {
            double y = getY() + getBbHeight() * 0.6;
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STAINED_GLASS.pink().defaultBlockState()), getX(), y, getZ(), 40, 0.35, 0.45, 0.35, 0.15);
            server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PURPUR_BLOCK.defaultBlockState()), getX(), y, getZ(), 20, 0.4, 0.5, 0.4, 0.1);
            server.sendParticles(ParticleTypes.END_ROD, getX(), y, getZ(), 8, 0.3, 0.4, 0.3, 0.06);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return phase() == Phase.IDLE ? ModSounds.CRYSTAL_WARDEN_AMBIENT.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CRYSTAL_WARDEN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CRYSTAL_WARDEN_DEATH.get();
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
        // a reload mid-beam ends it
        setPhase(Phase.IDLE, null);
    }

    // ---- GeckoLib -------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<CrystalWarden>("motion", 4, test -> switch (test.animatable().phase()) {
            case LOCK -> test.setAndContinue(LOCK_ON);
            case BEAM -> test.setAndContinue(BEAMING);
            case IDLE -> test.setAndContinue(test.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /**
     * Drifts round its pillar: a new spot within {@link #HOVER_RADIUS}, up to 2 above it, every few seconds, steered to
     * every tick (a move control moves only on the ticks it is given a target).
     */
    final class HoverGoal extends Goal {
        private int wait;
        private @Nullable Vec3 spot;

        HoverGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return home != null && phase() == Phase.IDLE;
        }

        @Override
        public void tick() {
            if (--wait <= 0 || spot == null) {
                wait = 60 + getRandom().nextInt(60);
                double angle = getRandom().nextDouble() * Math.PI * 2;
                double r = getRandom().nextDouble() * HOVER_RADIUS;
                spot = new Vec3(home.getX() + 0.5 + Math.cos(angle) * r, home.getY() + getRandom().nextDouble() * 2.0, home.getZ() + 0.5 + Math.sin(angle) * r);
            }
            if (position().distanceToSqr(spot) > 0.25) getMoveControl().setWantedPosition(spot.x, spot.y, spot.z, 0.6);
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

    /** Whether an entity is the one this warden is beaming at now (GameTests). */
    public boolean isBeaming(Entity entity) {
        return phase() == Phase.BEAM && beamTargetId() == entity.getId();
    }
}

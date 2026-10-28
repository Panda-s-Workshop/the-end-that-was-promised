package com.panda.promisedend.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Rift Stalker: a lanky voidstone construct with a rift for a heart, hunting the Voidscar Wastes. Every
 * {@link #BLINK_EVERY} ticks it gathers itself into the rift for half a second, blinks to just behind its target and
 * strikes there {@link #STRIKE_HIT} ticks later; a blow from someone near makes it blink at once, and so does standing
 * next to it (it gathers {@link #UP_CLOSE_BLINK} ticks later). That blink and strike is its only attack: it walks up
 * but never paws. A player looking straight at it freezes it where it stands (the opposite of an enderman): it neither
 * moves nor strikes, and its blink waits. So a stalker is fought by turning to face it, and a blink is answered by
 * turning round before it strikes.
 *
 * <p>Its state is synced ({@link #state()}), so clients play the clips.
 */
public class RiftStalker extends Monster implements GeoEntity {
    /** Ticks between one blink and the next, counted only while it hunts unwatched. */
    public static final int BLINK_EVERY = 120;
    /** It blinks only at a target this near and in sight. */
    public static final double BLINK_RANGE = 16.0;
    /** How far behind its target it comes out. */
    public static final double BLINK_BEHIND = 2.0;
    /** The gather clip is 0.5 s: it draws into the rift, then blinks. */
    public static final int GATHER_TICKS = 10;
    /**
     * The strike clip is 0.7 s; the blow lands on its 10th tick (blades raised from the 2nd), at a target within
     * {@link #STRIKE_REACH}: half a second to turn round.
     */
    public static final int STRIKE_TICKS = 14, STRIKE_HIT = 10;
    public static final double STRIKE_REACH = 3.0;
    /** The strike's damage: its attack, 6. */
    public static final double DAMAGE = 6.0;
    /** A player this near, looking at it this straight (twice as forgiving as the enderman's 0.025), freezes it. */
    public static final double STARE_RANGE = 32.0, STARE_TOLERANCE = 0.05;
    /** Frozen, it stays held this many ticks after the stare leaves it, so an aim that wavers doesn't flicker it. */
    public static final int FREEZE_HOLD = 4;
    /** Its target within this, it doesn't wait out its blink: it gathers in {@link #UP_CLOSE_BLINK} ticks at most. */
    public static final double UP_CLOSE = 4.0;
    public static final int UP_CLOSE_BLINK = 30;
    /** The renderer draws the rig at this scale; the hitbox is measured at it. */
    public static final float MODEL_SCALE = 1.25F;
    /** At most this many stalkers within 32 blocks for one more to spawn. */
    public static final int LOCAL_CAP = 2;
    /** The tag a player gets once told what to do when one comes out behind them (the hint is said once). */
    public static final String HINTED = "promisedend.rift_stalker_hinted";

    public enum State { HUNT, GATHER, STRIKE, FROZEN }

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(RiftStalker.class, EntityDataSerializers.BYTE);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation FROZEN = RawAnimation.begin().thenLoop("frozen");
    private static final RawAnimation GATHER = RawAnimation.begin().thenPlay("gather");
    private static final RawAnimation STRIKE = RawAnimation.begin().thenPlay("strike");
    /** Every clip the class plays: the GameTest checks the shipped animation file has exactly these. */
    public static final List<String> CLIPS = List.of("idle", "walk", "frozen", "gather", "strike");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int stateTicks;
    private int blinkIn = BLINK_EVERY;
    /** While above 0, a stare doesn't hold it: it was hit and is already on its way. */
    private int shaken;
    /** Ticks since a stare last held it, while it is frozen. */
    private int unseen;
    /** Blinks and blows landed since it loaded (not saved): GameTests and the harness count them. */
    private int blinks, strikes;

    public RiftStalker(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 28.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.ATTACK_DAMAGE, DAMAGE)
                .add(Attributes.MOVEMENT_SPEED, 0.28)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, (byte) State.HUNT.ordinal());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new Hunting<>(new ApproachGoal(this, 1.0, BLINK_BEHIND)));
        this.goalSelector.addGoal(6, new Hunting<>(new WaterAvoidingRandomStrollGoal(this, 0.7)));
        this.goalSelector.addGoal(7, new Hunting<>(new LookAtPlayerGoal(this, Player.class, 10.0F)));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ---- the mechanic ---------------------------------------------------------------------------

    public State state() {
        return State.values()[Mth.clamp(this.entityData.get(DATA_STATE), 0, State.values().length - 1)];
    }

    public int blinkIn() {
        return blinkIn;
    }

    public int blinks() {
        return blinks;
    }

    public int strikes() {
        return strikes;
    }

    private void setState(State state) {
        this.entityData.set(DATA_STATE, (byte) state.ordinal());
        this.stateTicks = 0;
    }

    /** Whether {@code player} looks straight at it: within {@link #STARE_RANGE}, in sight, its middle in their aim. */
    public boolean staredAtBy(Player player) {
        if (player.isSpectator() || !player.isAlive() || distanceTo(player) > STARE_RANGE) return false;
        Vec3 view = player.getViewVector(1.0F).normalize();
        Vec3 to = new Vec3(getX() - player.getX(), getY(0.6) - player.getEyeY(), getZ() - player.getZ());
        double d = to.length();
        if (d < 1e-4) return false;
        return view.dot(to.scale(1 / d)) > 1.0 - STARE_TOLERANCE / d && player.hasLineOfSight(this);
    }

    private boolean stared(ServerLevel level) {
        for (Player p : level.players()) {
            if (staredAtBy(p)) return true;
        }
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean hurt = super.hurtServer(level, source, amount);
        // hit by someone near, it slips into the rift at once, and a stare can't hold it until it is out the other
        // side; fire, a fall or a shot from afar doesn't send it
        if (hurt && isAlive() && (state() == State.HUNT || state() == State.FROZEN)
                && source.getEntity() instanceof LivingEntity attacker && distanceTo(attacker) <= BLINK_RANGE) {
            if (getTarget() == null && canAttack(attacker)) setTarget(attacker);
            if (getTarget() == null) return hurt;
            this.shaken = GATHER_TICKS + 1; // through the tick it blinks on
            startGather();
        }
        return hurt;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (shaken > 0) shaken--;
        boolean stared = shaken <= 0 && stared(level);
        if (stared) this.unseen = 0;
        if (stared || state() == State.FROZEN && ++unseen < FREEZE_HOLD) {
            if (state() != State.FROZEN) {
                // caught mid-blink or mid-blow, it loses it: the next blink waits its whole time
                if (state() != State.HUNT) this.blinkIn = BLINK_EVERY;
                setState(State.FROZEN);
                playSound(ModSounds.RIFT_STALKER_FREEZE.get(), 1.0F, 1.0F);
            }
            getNavigation().stop();
            setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
            return;
        }
        if (state() == State.FROZEN) setState(State.HUNT);
        LivingEntity target = getTarget();
        switch (state()) {
            case HUNT -> {
                if (target == null || !target.isAlive()) {
                    this.blinkIn = Math.max(blinkIn, GATHER_TICKS * 2);
                    return;
                }
                if (distanceTo(target) <= UP_CLOSE) this.blinkIn = Math.min(blinkIn, UP_CLOSE_BLINK);
                if (--blinkIn <= 0) {
                    if (canBlinkAt(target)) startGather();
                    else this.blinkIn = 20; // out of sight or too far: try again in a second
                }
            }
            case GATHER -> {
                getNavigation().stop();
                setDeltaMovement(getDeltaMovement().multiply(0, 1, 0));
                if (++stateTicks < GATHER_TICKS) return;
                Vec3 to = target == null || !target.isAlive() ? null : behind(level, target);
                if (to == null) {
                    setState(State.HUNT);
                    this.blinkIn = 20;
                    return;
                }
                blink(level, to, target);
                setState(State.STRIKE);
            }
            case STRIKE -> {
                getNavigation().stop();
                if (target != null) getLookControl().setLookAt(target, 360.0F, 90.0F);
                if (++stateTicks == STRIKE_HIT && target != null && target.isAlive() && distanceTo(target) <= STRIKE_REACH) {
                    if (doHurtTarget(level, target)) strikes++;
                    playSound(ModSounds.RIFT_STALKER_STRIKE.get(), 1.0F, 1.0F);
                }
                if (stateTicks >= STRIKE_TICKS) {
                    setState(State.HUNT);
                    this.blinkIn = BLINK_EVERY;
                }
            }
            case FROZEN -> { }
        }
    }

    /** Whether it may blink at {@code target}: alive, attackable, within {@link #BLINK_RANGE} and in sight. */
    public boolean canBlinkAt(LivingEntity target) {
        return target.isAlive() && canAttack(target) && distanceTo(target) <= BLINK_RANGE && hasLineOfSight(target);
    }

    /** Draws into the rift (public for GameTests): it blinks {@link #GATHER_TICKS} later. */
    public void startGather() {
        setState(State.GATHER);
        getNavigation().stop();
        playSound(ModSounds.RIFT_STALKER_GATHER.get(), 1.0F, 1.0F);
    }

    /**
     * Where it comes out: {@link #BLINK_BEHIND} behind {@code target} (opposite where they look), else a little nearer,
     * further or to a side; on a solid top, with room for its body and no liquid. Null if there is no such spot.
     */
    public @Nullable Vec3 behind(ServerLevel level, LivingEntity target) {
        Vec3 look = target.getViewVector(1.0F).multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-4) look = new Vec3(0, 0, 1);
        look = look.normalize();
        for (double back : new double[] {BLINK_BEHIND, 1.6, 2.6, 3.2}) {
            for (int turn : new int[] {0, 30, -30, 60, -60}) {
                Vec3 spot = target.position().subtract(look.yRot((float) Math.toRadians(turn)).scale(back));
                for (int dy : new int[] {0, 1, -1, 2, -2}) {
                    BlockPos feet = BlockPos.containing(spot.x, target.getY() + dy, spot.z);
                    BlockPos under = feet.below();
                    if (!level.getBlockState(under).isFaceSturdy(level, under, Direction.UP)) continue;
                    AABB box = getDimensions(getPose()).makeBoundingBox(spot.x, feet.getY(), spot.z);
                    if (level.noCollision(this, box) && !level.containsAnyLiquid(box)) return new Vec3(spot.x, feet.getY(), spot.z);
                }
            }
        }
        return null;
    }

    private void blink(ServerLevel level, Vec3 to, LivingEntity target) {
        rift(level, position());
        teleportTo(to.x, to.y, to.z);
        double dx = target.getX() - to.x, dz = target.getZ() - to.z;
        float yaw = (float) (Mth.atan2(dz, dx) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        rift(level, to);
        playSound(ModSounds.RIFT_STALKER_BLINK.get(), 1.0F, 1.0F);
        blinks++;
        // the first time one comes out behind a player, its rule, once
        if (target instanceof net.minecraft.server.level.ServerPlayer player && !player.entityTags().contains(HINTED)) {
            player.addTag(HINTED);
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("hint.promisedend.rift_stalker.turn"));
        }
    }

    /** The rift it goes through: a burst of the Voidscar's own motes, and the portal's reversed. */
    private void rift(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + getBbHeight() * 0.5, at.z, 30, 0.3, getBbHeight() * 0.4, 0.3, 0.05);
        level.sendParticles(com.panda.promisedend.registry.ModParticles.RIFT_MOTE.get(), at.x, at.y + getBbHeight() * 0.5, at.z, 12, 0.3, getBbHeight() * 0.4, 0.3, 0.02);
    }

    @Override
    public void tick() {
        super.tick();
        // gathering, it bleeds into the rift: the motes a player sees before it goes
        if (level().isClientSide() && state() == State.GATHER) {
            for (int i = 0; i < 2; i++) {
                level().addParticle(ParticleTypes.REVERSE_PORTAL, getRandomX(0.5), getRandomY(), getRandomZ(0.5), 0, 0.05, 0);
            }
        }
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && Constructs.mayAttack(target);
    }

    // ---- spawning: on the Voidscar's islands, in any light --------------------------------------

    @Override
    public boolean checkSpawnRules(LevelAccessor level, EntitySpawnReason reason) {
        return true;
    }

    public static boolean canSpawn(EntityType<RiftStalker> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (!Monster.checkAnyLightMonsterSpawnRules(type, level, reason, pos, random)) return false;
        if (reason != EntitySpawnReason.NATURAL) return true;
        if (!Constructs.terrainAllows(level, pos, com.panda.promisedend.registry.ModTags.VOIDSCAR_FALLBACK)) return false;
        return Constructs.roomFor(level, pos, RiftStalker.class, LOCAL_CAP);
    }

    // ---- a construct, not a creature ------------------------------------------------------------

    @Override
    protected void tickDeath() {
        Constructs.shatter(this, ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState(), net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN.defaultBlockState());
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return state() == State.HUNT ? ModSounds.RIFT_STALKER_AMBIENT.get() : null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.RIFT_STALKER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RIFT_STALKER_DEATH.get();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("blink_in", blinkIn);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        // a reload mid-blink ends it
        this.blinkIn = input.getIntOr("blink_in", BLINK_EVERY);
        setState(State.HUNT);
    }

    // ---- GeckoLib -------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RiftStalker>("motion", 2, test -> switch (test.animatable().state()) {
            case FROZEN -> test.setAndContinue(FROZEN);
            case GATHER -> test.setAndContinue(GATHER);
            case STRIKE -> test.setAndContinue(STRIKE);
            case HUNT -> test.setAndContinue(test.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    /** A goal that runs only while it hunts (frozen, gathering or striking, it does nothing else). */
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

package com.panda.promisedend.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.logic.AncientWard;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModEntities;
import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.registry.ModSounds;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Hollow Sentinel, the Shulker Foundry's guardian and the keeper of the Ancients' first seal. The Foundry's
 * template places it on its dais, dormant: closed and still, it can't be hurt, until a player in survival or adventure
 * comes within {@link #WAKE_RANGE}. Then it rises with a boss bar and fights within {@link #LEASH} of its dais:
 *
 * <ul>
 *   <li><b>Closed</b>, it can't be hurt. Every {@link #VOLLEY_TICKS} it charges for {@link #VOLLEY_WINDUP} (its plates
 *       part, its core flares, it rings) and fires {@link #VOLLEY_SHOTS} {@link ResonantBullet}s at its target. A
 *       player's blow sends a bullet back; {@link #RETURNS_TO_OPEN} returned bullets crack it open.</li>
 *   <li><b>Open</b> for {@link #OPEN_TICKS} (6 s), it sinks to its dais, bare, and takes {@link #OPEN_DAMAGE} times the
 *       damage. Then it closes and rises again.</li>
 *   <li>At two thirds and one third of its health it closes, calls two Purpur Sentries to the Foundry's pads and its
 *       bullets fly a quarter faster. After {@link #ENRAGE_TICKS} (4 minutes) in one phase it rages for good: its
 *       bullets hit half again as hard and its volleys come a quarter sooner.</li>
 * </ul>
 *
 * With no player in the arena for {@link #REST_TICKS} it settles back on its dais, dormant, keeping its wounds. On its
 * death the Foundry's seal breaks, and everyone who fought it (struck it, or stood within {@link #WITNESS} at the end)
 * is given an Ancient Core and the advancement that opens the Codex's next act.
 */
public class HollowSentinel extends Monster implements GeoEntity {
    public static final double WAKE_RANGE = 20.0;
    /** Its fight's reach: players this near its dais see the bar and may be its target. */
    public static final double ARENA = 48.0;
    /** How far from its dais it may go, and how high over its rest it hovers while closed (out of a blow's reach). */
    public static final double LEASH = 12.0, HOVER = 5.0;
    public static final int VOLLEY_TICKS = 80, VOLLEY_WINDUP = 10, VOLLEY_SHOTS = 3, SHOT_GAP = 5;
    public static final int RETURNS_TO_OPEN = 3, OPEN_TICKS = 120;
    /** How fast it drops to its dais when it cracks open, in blocks a tick: there in about half a second. */
    public static final double SINK = 0.5;
    public static final float OPEN_DAMAGE = 1.5F;
    /** The phases begin at these shares of its health; each one's bullets fly this much faster than the last's. */
    public static final float[] PHASES = {2.0F / 3.0F, 1.0F / 3.0F};
    public static final double PHASE_SPEEDUP = 1.25;
    /** The Foundry's pads, on the diagonals this far from its rest (gen_structures FOUNDRY_PAD). */
    public static final int PAD = 10;
    public static final int ENRAGE_TICKS = 4800;
    public static final float ENRAGE_DAMAGE = 1.5F;
    public static final double ENRAGE_VOLLEY = 0.75;
    public static final int REST_TICKS = 600;
    /** A player this near when it dies counts as having fought it. */
    public static final double WITNESS = 32.0;
    public static final double HEALTH = 300.0, ARMOUR = 10.0, CONTACT_DAMAGE = 8.0;
    /** The renderer draws the rig at this scale; the hitbox is measured at it. */
    public static final float MODEL_SCALE = 1.5F;
    /** The tag a player gets once told how to fight it (the hint is said once). */
    public static final String HINTED = "promisedend.hollow_sentinel_hinted";
    private static final net.minecraft.resources.Identifier DEFEATED = PromisedEnd.id("outer/defeat_sentinel");

    public enum State { DORMANT, CLOSED, OPEN }

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(HollowSentinel.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING = SynchedEntityData.defineId(HollowSentinel.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_ENRAGED = SynchedEntityData.defineId(HollowSentinel.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation DORMANT = RawAnimation.begin().thenLoop("dormant");
    private static final RawAnimation OPEN = RawAnimation.begin().thenLoop("open");
    private static final RawAnimation VOLLEY = RawAnimation.begin().thenPlay("volley");
    /** Every clip the class plays: the GameTest checks the shipped animation file has exactly these. */
    public static final List<String> CLIPS = List.of("idle", "walk", "dormant", "open", "volley");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.promisedend.hollow_sentinel"),
            BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.NOTCHED_6);
    private @Nullable BlockPos home;
    private int phase;
    private int phaseTicks;
    private int volley = VOLLEY_TICKS;
    private int shots;
    private int returns;
    private int openTicks;
    private int restless;
    /** Its bullets that hit, since it loaded (not saved). */
    private int hits;
    /** Everyone who struck it, for the cores and the advancement. */
    private final Set<UUID> fought = new LinkedHashSet<>();

    public HollowSentinel(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 10, true);
        setNoGravity(true);
        setPersistenceRequired();
        this.xpReward = 50;
        bar.setPlayBossMusic(true);
        bar.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ARMOR, ARMOUR)
                .add(Attributes.ATTACK_DAMAGE, CONTACT_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FLYING_SPEED, 0.3)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, ARENA);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, (byte) State.DORMANT.ordinal());
        builder.define(DATA_CHARGING, false);
        builder.define(DATA_ENRAGED, false);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    // ---- the state, for the renderer, the tests and the harness ---------------------------------------------------

    public State state() {
        return State.values()[Mth.clamp(this.entityData.get(DATA_STATE), 0, State.values().length - 1)];
    }

    public boolean charging() {
        return this.entityData.get(DATA_CHARGING);
    }

    public boolean enraged() {
        return this.entityData.get(DATA_ENRAGED);
    }

    public int phase() {
        return phase;
    }

    public int returns() {
        return returns;
    }

    /** Where it rests on its dais; it fights within {@link #LEASH} of this. */
    public @Nullable BlockPos home() {
        return home;
    }

    public void setHome(BlockPos home) {
        this.home = home.immutable();
    }

    public ServerBossEvent bar() {
        return bar;
    }

    /** Where its bullets leave from: the core, at its middle. */
    public Vec3 corePosition() {
        return position().add(0, getBbHeight() * 0.5, 0);
    }

    /** The speed of its bullets in its phase. */
    public double bulletSpeed() {
        return ResonantBullet.SPEED * Math.pow(PHASE_SPEEDUP, phase);
    }

    private void setState(State state) {
        this.entityData.set(DATA_STATE, (byte) state.ordinal());
        this.entityData.set(DATA_CHARGING, false);
    }

    /** Wakes it (public for tests): it rises with its bar, and tells the arena once how it is fought. */
    public void wake() {
        if (state() != State.DORMANT) return;
        setState(State.CLOSED);
        this.volley = VOLLEY_TICKS;
        this.restless = 0;
        bar.setVisible(true);
        playSound(ModSounds.HOLLOW_SENTINEL_WAKE.get(), 3.0F, 1.0F);
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, getX(), getY() + 1.5, getZ(), 60, 1.0, 1.2, 1.0, 0.08);
            for (ServerPlayer p : near(level, ARENA)) {
                if (p.entityTags().contains(HINTED)) continue;
                p.addTag(HINTED);
                p.sendOverlayMessage(Component.translatable("hint.promisedend.hollow_sentinel.return"));
            }
        }
    }

    /** A returned bullet landed on it: closed, it cracks a step, and at {@link #RETURNS_TO_OPEN} it opens. */
    public void bulletReturned() {
        if (state() != State.CLOSED) return;
        returns++;
        if (level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getY() + 1.5, getZ(), 20, 0.8, 0.8, 0.8, 0.1);
        }
        playSound(ModSounds.HOLLOW_SENTINEL_HURT_CLOSED.get(), 2.0F, 0.8F + 0.2F * returns);
        if (returns >= RETURNS_TO_OPEN) crackOpen();
    }

    /** Cracks it open for {@link #OPEN_TICKS} (public for tests). */
    public void crackOpen() {
        setState(State.OPEN);
        this.returns = 0;
        this.shots = 0;
        this.openTicks = OPEN_TICKS;
        playSound(ModSounds.HOLLOW_SENTINEL_OPEN.get(), 3.0F, 1.0F);
    }

    private void close() {
        setState(State.CLOSED);
        this.volley = volleyTicks();
        playSound(ModSounds.HOLLOW_SENTINEL_CLOSE.get(), 2.0F, 1.0F);
    }

    private int volleyTicks() {
        return enraged() ? (int) Math.round(VOLLEY_TICKS * ENRAGE_VOLLEY) : VOLLEY_TICKS;
    }

    /** One of its bullets hit {@code target} (counted for the tests and the harness). */
    void bulletLanded(LivingEntity target) {
        hits++;
    }

    public int hits() {
        return hits;
    }

    // ---- the fight ------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (home == null) home = blockPosition();
        if (tickCount % 10 == 0) updateBar(level);
        if (state() == State.DORMANT) {
            hold(Vec3.atBottomCenterOf(home));
            if (tickCount % 10 == 0 && !near(level, WAKE_RANGE).isEmpty()) wake();
            return;
        }
        LivingEntity target = target(level);
        if (target != null) {
            restless = 0;
            face(target);
            if (++phaseTicks >= ENRAGE_TICKS && !enraged()) enrage();
        }
        // open: its six seconds run whether or not anyone is left in the arena (a player stepping out to shoot from
        // afar gets no longer)
        if (state() == State.OPEN) {
            sink(Vec3.atBottomCenterOf(home));
            if (--openTicks <= 0) close();
            return;
        }
        if (target == null) {
            if (++restless >= REST_TICKS) rest();
            hold(Vec3.atBottomCenterOf(home).add(0, HOVER, 0));
            return;
        }
        // closed: over the dais, leaning toward the target within its leash, out of a blow's reach
        Vec3 rest = Vec3.atBottomCenterOf(home);
        Vec3 lean = target.position().subtract(rest).multiply(1, 0, 1);
        if (lean.length() > LEASH) lean = lean.normalize().scale(LEASH);
        hold(rest.add(lean.scale(0.5)).add(0, HOVER, 0));
        // the volley: counting down, the tell for its last VOLLEY_WINDUP ticks, then a shot every SHOT_GAP
        if (shots > 0) {
            if (--volley <= 0) {
                fire(level, target);
                shots--;
                volley = shots > 0 ? SHOT_GAP : volleyTicks();
                if (shots == 0) this.entityData.set(DATA_CHARGING, false);
            }
        } else if (--volley <= 0) {
            shots = VOLLEY_SHOTS;
        } else if (volley <= VOLLEY_WINDUP && !charging()) {
            this.entityData.set(DATA_CHARGING, true);
            playSound(ModSounds.HOLLOW_SENTINEL_VOLLEY.get(), 2.0F, 1.0F);
        }
        if (tickCount % 10 == 0) {
            for (Player p : level.getEntitiesOfClass(Player.class, getBoundingBox().inflate(0.25), p -> !p.isSpectator() && !p.isCreative())) {
                doHurtTarget(level, p);
            }
        }
    }

    private void fire(ServerLevel level, LivingEntity target) {
        float damage = enraged() ? ResonantBullet.DAMAGE * ENRAGE_DAMAGE : ResonantBullet.DAMAGE;
        level.addFreshEntity(new ResonantBullet(level, this, target, bulletSpeed(), damage));
        playSound(ModSounds.HOLLOW_SENTINEL_SHOOT.get(), 2.0F, 1.0F + 0.1F * shots);
    }

    /** Settles back on its dais, dormant, with its wounds: no one is left to fight. */
    private void rest() {
        setState(State.DORMANT);
        this.restless = 0;
        this.shots = 0;
        this.returns = 0;
        setTarget(null);
        bar.setVisible(false);
        bar.removeAllPlayers();
    }

    private void enrage() {
        this.entityData.set(DATA_ENRAGED, true);
        playSound(ModSounds.HOLLOW_SENTINEL_ENRAGE.get(), 3.0F, 1.0F);
        bar.setColor(BossEvent.BossBarColor.RED);
    }

    /** Its target: the one it has, while still in the arena, or the nearest player there. */
    private @Nullable LivingEntity target(ServerLevel level) {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && !(target instanceof Player p && (p.isCreative() || p.isSpectator()))
                && target.distanceToSqr(Vec3.atCenterOf(home)) <= ARENA * ARENA) return target;
        List<ServerPlayer> near = near(level, ARENA);
        ServerPlayer nearest = null;
        for (ServerPlayer p : near) if (nearest == null || p.distanceToSqr(this) < nearest.distanceToSqr(this)) nearest = p;
        setTarget(nearest);
        return nearest;
    }

    /** Players in survival or adventure within {@code range} of its dais. */
    private List<ServerPlayer> near(ServerLevel level, double range) {
        Vec3 at = home == null ? position() : Vec3.atCenterOf(home);
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(at) <= range * range) out.add(p);
        }
        return out;
    }

    private void hold(Vec3 at) {
        getMoveControl().setWantedPosition(at.x, at.y, at.z, 1.0);
        if (position().distanceToSqr(at) < 0.04) setDeltaMovement(getDeltaMovement().scale(0.5));
    }

    /**
     * Open: straight down onto its dais at {@link #SINK} a tick and held there. A flyer's drift there would spend much
     * of the open window on the way.
     */
    private void sink(Vec3 at) {
        getMoveControl().setWantedPosition(at.x, at.y, at.z, 1.0);
        Vec3 to = at.subtract(position());
        double d = to.length();
        setDeltaMovement(d < 0.05 ? Vec3.ZERO : to.scale(Math.min(1.0, SINK / d)));
    }

    private void face(LivingEntity target) {
        float yaw = (float) (Mth.atan2(target.getZ() - getZ(), target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0F;
        setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
    }

    private void updateBar(ServerLevel level) {
        bar.setProgress(getHealth() / getMaxHealth());
        if (!bar.isVisible()) return;
        List<ServerPlayer> near = near(level, ARENA);
        for (ServerPlayer p : List.copyOf(bar.getPlayers())) if (!near.contains(p)) bar.removePlayer(p);
        for (ServerPlayer p : near) bar.addPlayer(p);
    }

    // ---- damage: closed it rings, open it takes half again; its phases --------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, amount);
        if (state() != State.OPEN) {
            if (source.getEntity() instanceof Player) {
                playSound(ModSounds.HOLLOW_SENTINEL_HURT_CLOSED.get(), 1.5F, 1.0F);
                if (state() == State.DORMANT && source.getEntity() instanceof ServerPlayer p && !p.isCreative()) wake();
            }
            return false;
        }
        boolean hurt = super.hurtServer(level, source, amount * OPEN_DAMAGE);
        if (hurt && source.getEntity() instanceof ServerPlayer p) fought.add(p.getUUID());
        if (hurt && isAlive()) checkPhase(level);
        return hurt;
    }

    /** The phase its health has reached, forward only: one big blow past both thresholds calls both pairs. */
    public static int phaseFor(float share, int current) {
        int phase = current;
        for (int i = current; i < PHASES.length; i++) if (share <= PHASES[i]) phase = i + 1;
        return phase;
    }

    private void checkPhase(ServerLevel level) {
        int reached = phaseFor(getHealth() / getMaxHealth(), phase);
        if (reached == phase) return;
        while (phase < reached) {
            phase++;
            callSentries(level, phase);
        }
        this.phaseTicks = 0;
        playSound(ModSounds.HOLLOW_SENTINEL_PHASE.get(), 3.0F, 1.0F);
        close();
    }

    /** Two Purpur Sentries to two of the Foundry's pads (the first phase the north-east and south-west ones). */
    private void callSentries(ServerLevel level, int phase) {
        if (home == null) return;
        int[][] pads = phase % 2 == 1 ? new int[][] {{PAD, -PAD}, {-PAD, PAD}} : new int[][] {{PAD, PAD}, {-PAD, -PAD}};
        for (int[] pad : pads) {
            BlockPos at = home.offset(pad[0], 0, pad[1]);
            // a Sentinel away from its Foundry (summoned) has no pads: they come beside it
            if (!level.getBlockState(at.below()).isSolid()) at = home.offset(Integer.signum(pad[0]) * 2, 0, Integer.signum(pad[1]) * 2);
            PurpurSentry sentry = ModEntities.PURPUR_SENTRY.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (sentry == null) continue;
            sentry.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, getYRot(), 0.0F);
            sentry.setPersistenceRequired();
            level.addFreshEntity(sentry);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.getX() + 0.5, at.getY() + 1.0, at.getZ() + 0.5, 30, 0.4, 0.8, 0.4, 0.05);
        }
    }

    // ---- its end: the seal, the cores, the advancement -------------------------------------------------------------

    @Override
    public void die(DamageSource source) {
        boolean already = this.dead;
        super.die(source);
        if (already || !(level() instanceof ServerLevel level)) return;
        bar.removeAllPlayers();
        AncientWard.breakSeal(level.getServer(), 0);
        Set<ServerPlayer> rewarded = new LinkedHashSet<>();
        for (UUID id : fought) if (level.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer p) rewarded.add(p);
        // witnesses who could have fought it; one who struck it is rewarded whatever their mode now
        for (ServerPlayer p : level.players()) if (!p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) <= WITNESS * WITNESS) rewarded.add(p);
        var defeated = level.getServer().getAdvancements().get(DEFEATED);
        for (ServerPlayer p : rewarded) {
            if (defeated != null) p.getAdvancements().award(defeated, "defeated");
            ItemStack core = new ItemStack(ModItems.ANCIENT_CORE.get());
            if (!p.getInventory().add(core)) p.spawnAtLocation(level, core);
        }
    }

    @Override
    protected void tickDeath() {
        Constructs.shatter(this, ModBlocks.ANCIENT_PURPUR_SET.base().get().defaultBlockState(), Blocks.PURPUR_BLOCK.defaultBlockState(), Blocks.OBSIDIAN.defaultBlockState());
    }

    @Override
    public void remove(RemovalReason reason) {
        bar.removeAllPlayers();
        super.remove(reason);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return super.canAttack(target) && Constructs.mayAttack(target);
    }

    /** The Foundry's: it stays on any difficulty, however far the players go. */
    @Override
    public void checkDespawn() {
        setNoActionTime(0);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return state() == State.DORMANT ? null : ModSounds.HOLLOW_SENTINEL_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HOLLOW_SENTINEL_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HOLLOW_SENTINEL_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.0F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("home", BlockPos.CODEC, home);
        output.putBoolean("awake", state() != State.DORMANT);
        output.putInt("phase", phase);
        output.putInt("phase_ticks", phaseTicks);
        output.putBoolean("enraged", enraged());
        output.store("fought", UUIDUtil.CODEC.listOf(), List.copyOf(fought));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.home = input.read("home", BlockPos.CODEC).orElse(null);
        // open or mid-volley when saved, it comes back closed
        setState(input.getBooleanOr("awake", false) ? State.CLOSED : State.DORMANT);
        bar.setVisible(state() != State.DORMANT);
        this.phase = input.getIntOr("phase", 0);
        this.phaseTicks = input.getIntOr("phase_ticks", 0);
        this.entityData.set(DATA_ENRAGED, input.getBooleanOr("enraged", false));
        if (enraged()) bar.setColor(BossEvent.BossBarColor.RED);
        fought.clear();
        fought.addAll(input.read("fought", UUIDUtil.CODEC.listOf()).orElse(List.of()));
    }

    // ---- GeckoLib -------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<HollowSentinel>("motion", 5, test -> switch (test.animatable().state()) {
            case DORMANT -> test.setAndContinue(DORMANT);
            case OPEN -> test.setAndContinue(OPEN);
            case CLOSED -> test.setAndContinue(test.animatable().charging() ? VOLLEY : test.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

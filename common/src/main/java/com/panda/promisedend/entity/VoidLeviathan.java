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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Void Leviathan, the Rift Maw's guardian and the keeper of the Ancients' second seal. The Maw's template sets it
 * down asleep in the pool's throat, under the floor; it wakes when a player in survival or adventure comes within
 * {@link #WAKE_RANGE}, with a boss bar, and fights from the pool:
 *
 * <ul>
 *   <li><b>Submerged</b>, it swims under the pool for 8 to 12 seconds.</li>
 *   <li><b>Telegraph</b>: a ring of the rift's light marks the point of the pool nearest its target for
 *       {@link #TELEGRAPH_TICKS} (1.5 s), the void churning, while it swims under it.</li>
 *   <li><b>Breach</b>: it bursts up through the ring. Everyone within {@link #RING} of its middle takes
 *       {@link #BREACH_DAMAGE}.</li>
 *   <li><b>Exposed</b> for {@link #EXPOSED_TICKS} (4 s), reared over the lip, it takes full damage. Every other time a
 *       blow reaches it only {@link #OUTSIDE} of it lands. Below {@link #DRAGS} of its health it draws the players within
 *       {@link #PULL_RANGE} toward it, {@link #PULL} blocks a tick, while it rears.</li>
 *   <li><b>Dive</b>: it drops back under. Below {@link #TWICE} of its health it breaches twice in a row.</li>
 * </ul>
 *
 * With no player in the arena for {@link #REST_TICKS} it goes back to sleep in the throat, keeping its wounds. On its
 * death the Maw's seal breaks, and everyone who fought it (struck it, or stood within {@link #WITNESS} at the end) is
 * given the advancement that opens the Codex's next act and a Star Chart to the Unfinished Citadel.
 */
public class VoidLeviathan extends Monster implements GeoEntity {
    public static final double WAKE_RANGE = 24.0;
    /** Its fight's reach: players this near its rest see the bar and may be its target. */
    public static final double ARENA = 48.0;
    /**
     * The Maw's pool, in blocks across from its middle (gen_structures MAW_POOL), and how far under the floor's block
     * the template sets it down asleep (MAW_DEPTH): its rest.
     */
    public static final int POOL = 14, SURFACE = 8;
    /** How far the Maw's template reaches under its rest (gen_structures' THROAT less MAW_DEPTH): the pool is clear that deep. */
    static final int THROAT_UNDER = 12;
    /** How far under its rest it swims between breaches, and how far under the floor's top it rears from. */
    public static final double DEEP = 4.0, PEAK = 1.5;
    /**
     * A breach comes up under its target, but no further from the pool's middle than this: its body (half its width)
     * just inside the pool's edge, its ring reaching {@code BREACH_OUT + RING}, past the lip's row of vents.
     */
    public static final double BREACH_OUT = POOL - 1.25;
    public static final int GAP_MIN = 160, GAP_MAX = 240;
    public static final int TELEGRAPH_TICKS = 30, BREACH_TICKS = 10, EXPOSED_TICKS = 80, DIVE_TICKS = 15;
    /** Under the surface between the two breaches of a pair. */
    public static final int PAIR_GAP = 20;
    public static final double RING = 4.5;
    public static final float BREACH_DAMAGE = 14.0F;
    /** The share of a blow it takes while it isn't exposed. */
    public static final float OUTSIDE = 0.2F;
    /** Below these shares of its health it breaches twice in a row, and draws players in while it rears. */
    public static final float TWICE = 0.5F, DRAGS = 0.2F;
    /**
     * The draw: {@link #PULL} blocks a tick, given as a shove every {@link #PULL_EVERY} ticks to a player on the ground
     * (a player's own client moves them, so a shove each tick would hold a falling player in the air). Ground friction
     * keeps about 0.546 of a walker's speed each tick, so a shove of {@code PULL * PULL_EVERY * (1 - 0.546)} carries
     * them about {@code PULL * PULL_EVERY}.
     */
    public static final double PULL = 0.2, PULL_RANGE = 16.0;
    public static final int PULL_EVERY = 5;
    public static final double PULL_SHOVE = PULL * PULL_EVERY * (1 - 0.546);
    /**
     * The draw stops this far from the pool's middle: a player is dragged to the lip, not over it (a shove carries
     * about a block, and the pool's edge is at {@link #POOL}).
     */
    public static final double HOLD_AT = POOL + 2.5;
    public static final int REST_TICKS = 600;
    /** A player this near when it dies counts as having fought it. */
    public static final double WITNESS = 32.0;
    public static final double HEALTH = 450.0, ARMOUR = 8.0;
    /** The renderer draws the rig at this scale; the hitbox is measured at it. */
    public static final float MODEL_SCALE = 2.6F;
    /** The tag a player gets once told how to fight it (the hint is said once). */
    public static final String HINTED = "promisedend.void_leviathan_hinted";
    private static final net.minecraft.resources.Identifier DEFEATED = PromisedEnd.id("void/defeat_leviathan");

    public enum State { DORMANT, SUBMERGED, TELEGRAPH, BREACH, EXPOSED, DIVE }

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(VoidLeviathan.class, EntityDataSerializers.BYTE);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation DORMANT = RawAnimation.begin().thenLoop("dormant");
    private static final RawAnimation EXPOSED = RawAnimation.begin().thenLoop("exposed");
    private static final RawAnimation BREACH = RawAnimation.begin().thenPlay("breach");
    private static final RawAnimation DIVE = RawAnimation.begin().thenPlay("dive");
    /** Every clip the class plays: the GameTest checks the shipped animation file has exactly these. */
    public static final List<String> CLIPS = List.of("idle", "walk", "dormant", "exposed", "breach", "dive");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.promisedend.void_leviathan"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private @Nullable BlockPos home;
    /**
     * Whether the End's land that grew into the pool is gone: the Maw is placed chunk by chunk, so an island a chunk
     * beside it decorated later can reach into its throat. Cleared once, on its first tick.
     */
    private boolean poolCleared;
    /** Where this breach comes up: a point on the floor's plane over the pool. */
    private @Nullable Vec3 breach;
    private int timer;
    private boolean second;
    private int restless;
    /** Its breaches, the players they struck, and its draws, since it loaded (not saved): for the tests and the harness. */
    private int breaches, struck, pulls;
    /** Whether this rear's draw has begun (its warning is said once a rear). */
    private boolean drawing;
    /** Everyone who struck it, for the advancement. */
    private final Set<UUID> fought = new LinkedHashSet<>();

    public VoidLeviathan(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        setNoGravity(true);
        setPersistenceRequired();
        this.xpReward = 80;
        bar.setPlayBossMusic(true);
        bar.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ARMOR, ARMOUR)
                .add(Attributes.ATTACK_DAMAGE, BREACH_DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, ARENA);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, (byte) State.DORMANT.ordinal());
    }

    /** It swims through the stone of the Maw's throat as through the void (as a vex passes walls). */
    @Override
    public void tick() {
        this.noPhysics = true;
        super.tick();
        this.noPhysics = false;
        setNoGravity(true);
    }

    // ---- the state, for the renderer, the tests and the harness ---------------------------------------------------

    public State state() {
        return State.values()[Mth.clamp(this.entityData.get(DATA_STATE), 0, State.values().length - 1)];
    }

    /** Where it sleeps, in the pool's throat; it fights over the pool above this. */
    public @Nullable BlockPos home() {
        return home;
    }

    /**
     * Clears the pool of any land in it, from under its rest to over the floor, inside its edge (the Maw's own teeth and
     * vents stand past it); once its chunks are all loaded. Returns how many blocks went.
     */
    public int clearPool(ServerLevel level) {
        if (home == null) home = blockPosition();
        double edge = POOL - 0.5;
        int r = (int) Math.ceil(edge), cleared = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!level.isLoaded(home.offset(dx, 0, dz))) return cleared;
            }
        }
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > edge * edge) continue;
                for (int y = home.getY() - THROAT_UNDER; y <= home.getY() + SURFACE + 18; y++) {
                    BlockPos b = new BlockPos(home.getX() + dx, y, home.getZ() + dz);
                    if (level.getBlockState(b).isAir()) continue;
                    level.setBlock(b, Blocks.AIR.defaultBlockState(), 2);
                    cleared++;
                }
            }
        }
        poolCleared = true;
        if (cleared > 0) com.panda.promisedend.PromisedEnd.LOGGER.info("The Rift Maw's pool at {} had {} blocks of land in it: cleared", home.toShortString(), cleared);
        return cleared;
    }

    public void setHome(BlockPos home) {
        this.home = home.immutable();
    }

    public ServerBossEvent bar() {
        return bar;
    }

    public @Nullable Vec3 breachPoint() {
        return breach;
    }

    public int timer() {
        return timer;
    }

    public int breaches() {
        return breaches;
    }

    public int struck() {
        return struck;
    }

    /** The ticks it drew at least one player in. */
    public int pulls() {
        return pulls;
    }

    /** Everyone who has struck it. */
    public Set<UUID> fought() {
        return java.util.Collections.unmodifiableSet(fought);
    }

    /** The top of the Maw's floor, where its fighters stand. */
    public double surface() {
        return home().getY() + SURFACE + 1.0;
    }

    /** Whether its health is low enough that it draws players in while it rears. */
    public boolean drags() {
        return getHealth() / getMaxHealth() <= DRAGS;
    }

    private void setState(State state, int ticks) {
        this.entityData.set(DATA_STATE, (byte) state.ordinal());
        this.timer = ticks;
    }

    /** Wakes it (public for tests): it stirs under the pool with its bar, and tells the arena once how it is fought. */
    public void wake() {
        if (state() != State.DORMANT) return;
        setState(State.SUBMERGED, TELEGRAPH_TICKS);
        this.restless = 0;
        bar.setVisible(true);
        playSound(ModSounds.VOID_LEVIATHAN_WAKE.get(), 3.0F, 1.0F);
        if (level() instanceof ServerLevel level) {
            for (ServerPlayer p : near(level, ARENA)) hint(p);
        }
    }

    /** Marks the breach point nearest {@code target} and swims for it (public for tests). */
    public void telegraph(LivingEntity target) {
        if (home == null) home = blockPosition();
        this.breach = breachPoint(Vec3.atBottomCenterOf(home), target.position(), surface());
        setState(State.TELEGRAPH, TELEGRAPH_TICKS);
        // its ring on the surface, drawn lit by clients (End light greys a ring of dust to a haze)
        if (level() instanceof ServerLevel level) {
            DiveMark mark = com.panda.promisedend.registry.ModEntities.DIVE_MARK.get().create(level, net.minecraft.world.entity.EntitySpawnReason.EVENT);
            if (mark != null) {
                mark.setKind(DiveMark.Kind.BREACH);
                mark.snapTo(breach.x, breach.y, breach.z, 0.0F, 0.0F);
                level.addFreshEntity(mark);
            }
        }
        playSound(ModSounds.VOID_LEVIATHAN_TELEGRAPH.get(), 3.0F, 1.0F);
    }

    /** Rears at {@code point} straight away, exposed for its {@link #EXPOSED_TICKS} (public for tests). */
    public void rear(Vec3 point) {
        this.breach = point;
        this.drawing = false;
        setState(State.EXPOSED, EXPOSED_TICKS);
    }

    /**
     * Where a breach comes up: on the floor's plane at {@code surface}, from the pool's middle ({@code rest}) toward
     * {@code target} as far as {@link #BREACH_OUT}, or under the target when they are nearer the middle than that.
     */
    public static Vec3 breachPoint(Vec3 rest, Vec3 target, double surface) {
        double dx = target.x - rest.x, dz = target.z - rest.z, d = Math.hypot(dx, dz);
        double out = Math.min(d, BREACH_OUT);
        return d < 1e-3 ? new Vec3(rest.x, surface, rest.z) : new Vec3(rest.x + dx / d * out, surface, rest.z + dz / d * out);
    }

    /** Whether a player at {@code at} stands in the ring of a breach at {@code point}. */
    public static boolean inRing(Vec3 point, Vec3 at) {
        return Math.hypot(at.x - point.x, at.z - point.z) <= RING && at.y >= point.y - 4 && at.y <= point.y + 6;
    }

    /** The shove its draw gives a player at {@code at} toward it at {@code from}: flat, {@link #PULL_SHOVE} long. */
    public static Vec3 pull(Vec3 from, Vec3 at) {
        Vec3 to = new Vec3(from.x - at.x, 0, from.z - at.z);
        double d = to.length();
        return d < 0.5 ? Vec3.ZERO : to.scale(PULL_SHOVE / d);
    }

    // ---- the fight ------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (home == null) home = blockPosition();
        if (tickCount % 10 == 0) updateBar(level);
        Vec3 rest = Vec3.atBottomCenterOf(home);
        if (state() == State.DORMANT) {
            if (!poolCleared) clearPool(level);
            glide(rest, 0.2);
            if (tickCount % 10 == 0 && !near(level, WAKE_RANGE).isEmpty()) wake();
            return;
        }
        LivingEntity target = target(level);
        if (target != null) {
            restless = 0;
            face(target);
        }
        Vec3 under = breach == null ? rest.add(0, -DEEP, 0) : new Vec3(breach.x, rest.y - DEEP, breach.z);
        Vec3 peak = breach == null ? rest : new Vec3(breach.x, surface() - PEAK, breach.z);
        timer--;
        switch (state()) {
            case SUBMERGED -> {
                glide(under, 0.3);
                if (target == null) {
                    if (++restless >= REST_TICKS) rest();
                } else if (timer <= 0) {
                    // each breach goes for whoever is nearest now, so one fighter doesn't take them all
                    setTarget(null);
                    LivingEntity nearest = target(level);
                    telegraph(nearest == null ? target : nearest);
                }
            }
            case TELEGRAPH -> {
                glide(under, 0.8);
                if (tickCount % 2 == 0) ring(level);
                if (timer <= 0) {
                    setState(State.BREACH, BREACH_TICKS);
                    playSound(ModSounds.VOID_LEVIATHAN_BREACH.get(), 4.0F, 1.0F);
                }
            }
            case BREACH -> {
                glide(peak, position().distanceTo(peak) / Math.max(1, timer));
                if (timer <= 0) surface(level);
            }
            case EXPOSED -> {
                glide(peak, 0.5);
                if (drags() && timer % PULL_EVERY == 0) drawIn(level);
                if (timer <= 0) {
                    setState(State.DIVE, DIVE_TICKS);
                    playSound(ModSounds.VOID_LEVIATHAN_DIVE.get(), 3.0F, 1.0F);
                }
            }
            case DIVE -> {
                glide(under, position().distanceTo(under) / Math.max(1, timer));
                if (timer <= 0) {
                    // below half its health the second of a pair comes straight after the first
                    boolean pair = getHealth() / getMaxHealth() <= TWICE && !second;
                    second = pair;
                    setState(State.SUBMERGED, pair ? PAIR_GAP : GAP_MIN + getRandom().nextInt(GAP_MAX - GAP_MIN + 1));
                }
            }
            default -> {}
        }
    }

    /**
     * The telegraph's motes: the ring itself is a {@link DiveMark} clients draw lit; over it a column at its middle,
     * where it will come up, seen from across the Maw, of lit rift motes rising.
     */
    private void ring(ServerLevel level) {
        if (breach == null) return;
        for (int i = 0; i < 6; i++) level.sendParticles(com.panda.promisedend.registry.ModParticles.RIFT_MOTE.get(), breach.x, breach.y + 0.4 + i * 0.7, breach.z, 1, 0.08, 0.1, 0.08, 0);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, breach.x, breach.y + 0.2, breach.z, 6, RING * 0.4, 0.1, RING * 0.4, 0.04);
    }

    /** It clears the surface: everyone in the ring is struck, and it rears, exposed. */
    private void surface(ServerLevel level) {
        breaches++;
        this.drawing = false;
        setState(State.EXPOSED, EXPOSED_TICKS);
        if (breach == null) return;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, breach.x, breach.y + 0.5, breach.z, 80, RING * 0.5, 1.0, RING * 0.5, 0.25);
        level.sendParticles(com.panda.promisedend.registry.ModParticles.RIFT_MOTE.get(), breach.x, breach.y + 0.5, breach.z, 40, RING * 0.5, 0.6, RING * 0.5, 0.0);
        for (ServerPlayer p : near(level, ARENA)) {
            if (inRing(breach, p.position()) && p.hurtServer(level, damageSources().mobAttack(this), BREACH_DAMAGE)) struck++;
        }
    }

    /** Below {@link #DRAGS}, while it rears: the players within {@link #PULL_RANGE} on the ground are drawn toward it. */
    private void drawIn(ServerLevel level) {
        boolean any = false;
        Vec3 middle = Vec3.atBottomCenterOf(home);
        for (ServerPlayer p : near(level, ARENA)) {
            if (!standing(level, p) || p.distanceToSqr(this) > PULL_RANGE * PULL_RANGE) continue;
            if (Math.hypot(p.getX() - middle.x, p.getZ() - middle.z) <= HOLD_AT) continue;
            if (!drawing) p.sendOverlayMessage(Component.translatable("hint.promisedend.void_leviathan.drag"));
            Vec3 shove = pull(position(), p.position());
            p.setDeltaMovement(shove.x, 0.0, shove.z);
            // to the trackers, and (syncVelocity) to the drawn player's own client too
            p.needsSync = true;
            p.syncVelocity = true;
            any = true;
        }
        if (any && (!drawing || timer % (PULL_EVERY * 4) == 0)) playSound(ModSounds.VOID_LEVIATHAN_PULL.get(), 3.0F, 1.0F);
        if (any) {
            drawing = true;
            pulls++;
        }
    }

    /**
     * Whether a player is on their feet: on the ground by their client's word, or over a block that holds them (a player
     * standing still sends their footing seldom, and not at all until a teleport is settled).
     */
    private static boolean standing(ServerLevel level, ServerPlayer p) {
        BlockPos under = BlockPos.containing(p.getX(), p.getY() - 0.2, p.getZ());
        return p.onGround() || !level.getBlockState(under).getCollisionShape(level, under).isEmpty();
    }

    /** Goes back to sleep in the throat with its wounds: no one is left to fight. */
    private void rest() {
        setState(State.DORMANT, 0);
        this.restless = 0;
        this.second = false;
        this.breach = null;
        setTarget(null);
        bar.setVisible(false);
        bar.removeAllPlayers();
    }

    /** Its target: the one it has, while still in the arena, or the nearest player there. */
    private @Nullable LivingEntity target(ServerLevel level) {
        LivingEntity target = getTarget();
        if (target != null && target.isAlive() && !(target instanceof Player p && (p.isCreative() || p.isSpectator()))
                && target.distanceToSqr(Vec3.atCenterOf(home)) <= ARENA * ARENA) return target;
        ServerPlayer nearest = null;
        for (ServerPlayer p : near(level, ARENA)) if (nearest == null || p.distanceToSqr(this) < nearest.distanceToSqr(this)) nearest = p;
        setTarget(nearest);
        return nearest;
    }

    /** Players in survival or adventure within {@code range} of its rest. */
    private List<ServerPlayer> near(ServerLevel level, double range) {
        Vec3 at = home == null ? position() : Vec3.atCenterOf(home);
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (p.isAlive() && !p.isCreative() && !p.isSpectator() && p.distanceToSqr(at) <= range * range) out.add(p);
        }
        return out;
    }

    /** Straight toward {@code at}, {@code speed} blocks a tick at most (it has no physics: nothing slows it). */
    private void glide(Vec3 at, double speed) {
        Vec3 to = at.subtract(position());
        double d = to.length();
        setDeltaMovement(d < 0.05 ? Vec3.ZERO : to.scale(Math.min(1.0, speed / d)));
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
        for (ServerPlayer p : near) {
            bar.addPlayer(p);
            hint(p);
        }
    }

    /** The rule, said once to each player (whoever came after it woke too). */
    private static void hint(ServerPlayer p) {
        if (p.entityTags().contains(HINTED)) return;
        p.addTag(HINTED);
        p.sendOverlayMessage(Component.translatable("hint.promisedend.void_leviathan.window"));
    }

    // ---- damage: a fifth under the surface, all of it while it rears -----------------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, amount);
        boolean exposed = state() == State.EXPOSED;
        if (source.getEntity() instanceof ServerPlayer p && !p.isCreative()) {
            fought.add(p.getUUID());
            if (state() == State.DORMANT) wake();
        }
        if (!exposed && source.getEntity() instanceof Player) playSound(ModSounds.VOID_LEVIATHAN_HURT_DEEP.get(), 1.5F, 1.0F);
        return super.hurtServer(level, source, exposed ? amount : amount * OUTSIDE);
    }

    // ---- its end: the seal, the advancement -------------------------------------------------------------------------

    @Override
    public void die(DamageSource source) {
        boolean already = this.dead;
        super.die(source);
        if (already || !(level() instanceof ServerLevel level)) return;
        bar.removeAllPlayers();
        landDrops(level, source);
        AncientWard.breakSeal(level.getServer(), 1);
        // who it rewards: each who struck it, and witnesses who could have fought it (one who struck it is rewarded
        // whatever their mode now); given now, or kept for one dead or away (logic/Rewards)
        Set<UUID> rewarded = new LinkedHashSet<>(fought);
        for (ServerPlayer p : level.players()) if (!p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) <= WITNESS * WITNESS) rewarded.add(p.getUUID());
        com.panda.promisedend.logic.Rewards.grant(level.getServer(), rewarded, com.panda.promisedend.logic.Rewards.Guardian.LEVIATHAN);
    }

    /**
     * Its reward to one player: the advancement; the way on, a chart to the nearest Unfinished Citadel, where the third
     * seal is kept; a Leviathan Fin (it can't be copied, so a group's every glider gets one); and with no stars falling,
     * a Starfall Core, so the Codex's goal and the Star Compass can still be had.
     */
    public static void reward(ServerPlayer p) {
        var defeated = p.level().getServer().getAdvancements().get(DEFEATED);
        if (defeated != null) p.getAdvancements().award(defeated, "defeated");
        List<net.minecraft.world.item.ItemStack> rewards = new java.util.ArrayList<>(List.of(
                com.panda.promisedend.item.StarChartItem.of(ModItems.STAR_CHART.get(), PromisedEnd.id("on_citadel_charts")),
                new net.minecraft.world.item.ItemStack(ModItems.LEVIATHAN_FIN.get())));
        if (!com.panda.promisedend.EndConfig.starfallEnabled()) rewards.add(new net.minecraft.world.item.ItemStack(ModItems.STARFALL_CORE.get()));
        for (net.minecraft.world.item.ItemStack reward : rewards) {
            if (!p.getInventory().add(reward)) p.spawnAtLocation(p.level(), reward);
        }
    }

    /**
     * It dies over the pool, so what it dropped (its loot and experience, just made) is carried to the lip, the point of
     * it nearest whoever killed it (or, killed by no one, the nearest player in the arena, its target, the breach),
     * where they can take it.
     */
    private void landDrops(ServerLevel level, DamageSource source) {
        if (home == null) return;
        Vec3 toward = source.getEntity() != null ? source.getEntity().position() : null;
        if (toward == null) {
            ServerPlayer nearest = null;
            for (ServerPlayer p : near(level, ARENA)) if (nearest == null || p.distanceToSqr(this) < nearest.distanceToSqr(this)) nearest = p;
            toward = nearest != null ? nearest.position() : getTarget() != null ? getTarget().position() : breach != null ? breach : position().add(1, 0, 0);
        }
        Vec3 at = lip(Vec3.atBottomCenterOf(home), toward, surface());
        var box = getBoundingBox().inflate(4);
        for (var item : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box, e -> e.tickCount == 0)) {
            item.setPos(at.x, at.y + 0.2, at.z);
            item.setDeltaMovement(Vec3.ZERO);
        }
        for (var orb : level.getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, box, e -> e.tickCount == 0)) {
            orb.setPos(at.x, at.y + 0.2, at.z);
            orb.setDeltaMovement(Vec3.ZERO);
        }
    }

    /** The point on the Maw's floor just past the pool's edge, from its middle toward {@code toward}. */
    public static Vec3 lip(Vec3 rest, Vec3 toward, double surface) {
        double dx = toward.x - rest.x, dz = toward.z - rest.z, d = Math.hypot(dx, dz);
        double r = POOL + 2.0;
        return d < 1e-3 ? new Vec3(rest.x + r, surface, rest.z) : new Vec3(rest.x + dx / d * r, surface, rest.z + dz / d * r);
    }

    @Override
    protected void tickDeath() {
        Constructs.shatter(this, ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState(), Blocks.CRYING_OBSIDIAN.defaultBlockState(), Blocks.OBSIDIAN.defaultBlockState());
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

    /** The Maw's: it stays on any difficulty, however far the players go. */
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
        return state() == State.DORMANT ? null : ModSounds.VOID_LEVIATHAN_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.VOID_LEVIATHAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.VOID_LEVIATHAN_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 3.0F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("home", BlockPos.CODEC, home);
        output.putBoolean("awake", state() != State.DORMANT);
        output.putBoolean("pool_cleared", poolCleared);
        output.store("fought", UUIDUtil.CODEC.listOf(), List.copyOf(fought));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.home = input.read("home", BlockPos.CODEC).orElse(null);
        this.poolCleared = input.getBooleanOr("pool_cleared", false);
        // mid-breach when saved, it comes back under the surface
        setState(input.getBooleanOr("awake", false) ? State.SUBMERGED : State.DORMANT, TELEGRAPH_TICKS);
        bar.setVisible(state() != State.DORMANT);
        fought.clear();
        fought.addAll(input.read("fought", UUIDUtil.CODEC.listOf()).orElse(List.of()));
    }

    // ---- GeckoLib -------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<VoidLeviathan>("motion", 4, test -> switch (test.animatable().state()) {
            case DORMANT -> test.setAndContinue(DORMANT);
            case BREACH -> test.setAndContinue(BREACH);
            case EXPOSED -> test.setAndContinue(EXPOSED);
            case DIVE -> test.setAndContinue(DIVE);
            case SUBMERGED, TELEGRAPH -> test.setAndContinue(test.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

package com.panda.promisedend.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.util.GeckoLibUtil;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.block.PhantomLatticeBlock;
import com.panda.promisedend.logic.AncientWard;
import com.panda.promisedend.logic.ArchitectFight;
import com.panda.promisedend.logic.WeakPoint;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModEntities;
import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.registry.ModSounds;
import com.panda.promisedend.registry.ModTags;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Architect, the Unfinished Citadel's guardian and the keeper of the Ancients' third seal. The Citadel's template
 * places it over the hub of its arena, dormant, bowed and bound: it can't be hurt or woken until its Amber Gate has been
 * opened with a key ({@link #unbind}), and then not until a player in survival or adventure comes within
 * {@link #WAKE_RANGE} of it (or strikes it). Then it rises with a boss bar and builds against them, over its disc of
 * Lattice Tile:
 *
 * <ul>
 *   <li><b>Phase 1</b>, it raises walls of lattice ({@link #WALL_LENGTH} long, {@link #WALL_HEIGHT} high, for
 *       {@link #WALL_TICKS}) between itself and its target, splitting the arena, and lobs {@link LatticeShard}s from its
 *       keystone over them.</li>
 *   <li><b>Phase 2</b>, below {@link #PHASES}[0] of its health, it unmakes the tiles round its target instead: they glow
 *       for {@link #UNMAKE_TELL} (2 s) and go for {@link #UNMAKE_TICKS} (10 s), so a fighter keeps moving.</li>
 *   <li><b>Phase 3</b>, below {@link #PHASES}[1], it does both, and calls {@link #CRAWLERS} Lattice Crawlers.</li>
 * </ul>
 *
 * Its cage is shut while it works: it can be hurt only while it stands open, from a throw's leaving it to its next work
 * ({@link ArchitectFight#openAfterThrow}), and only by a fighter on its disc. A blow or a shot that meets its keystone,
 * the lit stone in front of its chest, deals {@link #KEYSTONE_DAMAGE} times the damage ({@link WeakPoint}). A fighter
 * under it on its hub, where it neither walls nor unmakes, is swept out onto the tiles.
 * Everything it builds or unmakes is Phantom Lattice over the arena's own lattice: it never
 * touches another block. With no player within {@link #RESET_RANGE} for {@link #RESET_TICKS} (a minute), or when it
 * dies, the arena is put back as it was: walls gone, tiles re-formed, its Crawlers gone, and it settles dormant with its
 * wounds. On its death the Citadel's seal breaks, and everyone who fought it is given the Architect's Promise, the
 * Architect's Lattice and the advancement that opens the Codex's last page.
 */
public class TheArchitect extends Monster implements GeoEntity {
    public static final double WAKE_RANGE = 16.0;
    /** Its fight's reach: players this near its rest see the bar and may be its target. */
    public static final double ARENA = 48.0;
    /** Its arena: the disc of tiles this far out from its rest (gen_structures CITADEL_SHAFT, and a half). */
    public static final double ARENA_RADIUS = 16.0;
    /** How far from its rest it leans toward its target, and how high over it it hovers. */
    public static final double LEASH = 5.0, HOVER = 1.0;
    public static final int RESET_RANGE = 64, RESET_TICKS = 1200;
    /** The pause between its works, and the shorter one in its last phase. */
    public static final int REST_BETWEEN = ArchitectFight.REST_BETWEEN, REST_BETWEEN_LAST = ArchitectFight.REST_BETWEEN_LAST;
    /** Its clips' lengths, and the tick in each the work lands on (the clips' own: gen_constructs architect). */
    public static final int RAISE_TICKS = ArchitectFight.RAISE_TICKS, RAISE_AT = ArchitectFight.RAISE_AT,
            THROW_TICKS = ArchitectFight.THROW_TICKS, THROW_WINDUP = ArchitectFight.THROW_WINDUP, UNMAKE_TELL = ArchitectFight.UNMAKE_TELL;
    public static final int WALL_LENGTH = 7, WALL_HEIGHT = 3, WALL_TICKS = 200;
    /** A wall stands this far from its target, on its side. */
    public static final double WALL_OUT = 3.0;
    /** The tiles it unmakes: those within this of its target's feet, gone this long. */
    public static final double UNMAKE_RADIUS = 2.5;
    public static final int UNMAKE_TICKS = 200;
    /** The keystone: in front of its chest (the rig's (0, 20, -5) px at {@link #MODEL_SCALE}), and how near it a blow lands. */
    public static final double KEYSTONE_UP = 2.5, KEYSTONE_FORWARD = 0.625, KEYSTONE_RADIUS = 0.7;
    public static final float KEYSTONE_DAMAGE = ArchitectFight.KEYSTONE_DAMAGE;
    /** The phases begin at these shares of its health. */
    public static final float[] PHASES = ArchitectFight.PHASES;
    /** Its hub: a target within this of its rest stands under it, and is swept out for this, this hard. */
    public static final double HUB_REACH = 3.0, SWEEP_PUSH = 1.4;
    public static final float SWEEP_DAMAGE = 6.0F;
    /** A throw leads its target by their pace over its flight, this far at most. */
    public static final double LEAD_MOST = 6.0;
    /** It leans this share of the way toward its target, so it stays behind the walls it raises between them. */
    public static final double LEAN = 0.3;
    public static final int CRAWLERS = 2;
    /** Where its Crawlers come out: on the arena, this far from its rest. */
    public static final int CRAWLER_OUT = 8;
    /** A player this near when it dies counts as having fought it. */
    public static final double WITNESS = 32.0;
    public static final double HEALTH = ArchitectFight.HEALTH, ARMOUR = ArchitectFight.ARMOUR;
    /** The renderer draws the rig at this scale; the hitbox is measured at it. */
    public static final float MODEL_SCALE = 2.0F;
    /** The tag a player gets once told how to fight it (the hint is said once). */
    public static final String HINTED = "promisedend.the_architect_hinted";
    private static final net.minecraft.resources.Identifier DEFEATED = PromisedEnd.id("void/defeat_architect");
    private static final DustParticleOptions GLOW = new DustParticleOptions(0xFFF4FF, 1.4F);

    public enum State { DORMANT, IDLE, RAISE, THROW, UNMAKE }

    /** What it does in turn, in each phase ({@link ArchitectFight#WORKS}). */
    private static final List<List<State>> WORKS = ArchitectFight.WORKS.stream()
            .map(works -> works.stream().map(w -> State.valueOf(w.name())).toList()).toList();

    private static final EntityDataAccessor<Byte> DATA_STATE = SynchedEntityData.defineId(TheArchitect.class, EntityDataSerializers.BYTE);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation DORMANT = RawAnimation.begin().thenLoop("dormant");
    private static final RawAnimation RAISE = RawAnimation.begin().thenPlay("raise");
    private static final RawAnimation THROW = RawAnimation.begin().thenPlay("throw");
    private static final RawAnimation UNMAKE = RawAnimation.begin().thenPlay("unmake");
    /** Every clip the class plays: the GameTest checks the shipped animation file has exactly these. */
    public static final List<String> CLIPS = List.of("idle", "walk", "dormant", "raise", "throw", "unmake");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bar = new ServerBossEvent(UUID.randomUUID(), Component.translatable("entity.promisedend.the_architect"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private @Nullable BlockPos home;
    private int phase;
    private int stateTicks;
    private int rest = REST_BETWEEN;
    private int next;
    private int alone;
    /** Ticks its cage has left open; shut at 0. */
    private int open;
    /** Bound behind its gate: placed so by the Citadel, until the gate is opened. */
    private boolean bound;
    /** The game time it was saved at, read back on its load, to count the time it was away toward its reset. */
    private long savedAt = -1;
    /** Its target's pace, a tick, from where it stood the tick before; for the throws' lead. */
    private @Nullable UUID seenId;
    private Vec3 seen = Vec3.ZERO, pace = Vec3.ZERO;
    private int toldAt = -100;
    /** Its works as they began, the tick and the work, for the tests (the last 32, not saved). */
    private final List<String> began = new ArrayList<>();
    /** The tiles glowing in an unmaking, to go at its end. */
    private final List<BlockPos> marked = new ArrayList<>();
    /** What it has done since it loaded (not saved): for the tests and the harness. */
    private int walls, unmade, throwsMade, keystoneHits, sweeps;
    /** Everyone who struck it, for the rewards and the advancement. */
    private final Set<UUID> fought = new LinkedHashSet<>();
    /** The Crawlers it called, sent away when the arena resets. */
    private final List<UUID> called = new ArrayList<>();

    public TheArchitect(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 10, true);
        setNoGravity(true);
        setPersistenceRequired();
        this.xpReward = 100;
        bar.setPlayBossMusic(true);
        bar.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, HEALTH)
                .add(Attributes.ARMOR, ARMOUR)
                .add(Attributes.ATTACK_DAMAGE, LatticeShard.DAMAGE)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FLYING_SPEED, 0.2)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.FOLLOW_RANGE, ARENA);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STATE, (byte) State.DORMANT.ordinal());
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

    public int phase() {
        return phase;
    }

    public int walls() {
        return walls;
    }

    public int unmade() {
        return unmade;
    }

    public int throwsMade() {
        return throwsMade;
    }

    public int keystoneHits() {
        return keystoneHits;
    }

    public int sweeps() {
        return sweeps;
    }

    /** Its works as they began, each "tick work" (for tests). */
    public List<String> began() {
        return List.copyOf(began);
    }

    /** Whether its cage stands open, so a blow can land. */
    public boolean isOpen() {
        return open > 0;
    }

    /** Opens its cage for {@code ticks} now (for tests: as a throw does). */
    public void openCage(int ticks) {
        this.open = ticks;
    }

    public boolean bound() {
        return bound;
    }

    public void setBound(boolean bound) {
        this.bound = bound;
    }

    /** Its gate is open: from now on it may wake. */
    public void unbind() {
        if (!bound) return;
        this.bound = false;
        if (level() instanceof ServerLevel level) {
            Vec3 key = keystonePosition();
            level.sendParticles(ParticleTypes.WAX_OFF, key.x, key.y, key.z, 20, 0.6, 0.8, 0.6, 0.02);
        }
    }

    public List<UUID> called() {
        return List.copyOf(called);
    }

    /** Where it rests over its arena's hub. */
    public @Nullable BlockPos home() {
        return home;
    }

    public void setHome(BlockPos home) {
        this.home = home.immutable();
    }

    public ServerBossEvent bar() {
        return bar;
    }

    /** The arena's floor, the tiles' level: a block under its rest. */
    private int floorY() {
        return rest().getY() - 1;
    }

    /** Its rest, taken where it stands if it has none yet. */
    private BlockPos rest() {
        if (home == null) home = blockPosition();
        return home;
    }

    /** Ticks with no player near (for tests: the count run on toward {@link #RESET_TICKS}). */
    public void setAlone(int ticks) {
        this.alone = ticks;
    }

    /** Its keystone's middle, where it faces. */
    public Vec3 keystonePosition() {
        double[] at = WeakPoint.bodyPoint(getX(), getY(), getZ(), this.yBodyRot, 0.0, KEYSTONE_UP, KEYSTONE_FORWARD);
        return new Vec3(at[0], at[1], at[2]);
    }

    /** Whether {@code source} struck its keystone: a blow aimed through it, or a shot whose flight meets it. */
    public boolean onKeystone(DamageSource source) {
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        Vec3 key = keystonePosition();
        if (direct != null && direct != attacker) {
            Vec3 from = direct.position();
            Vec3 to = from.add(direct.getDeltaMovement().scale(1.5));
            return WeakPoint.toSegment(key.x, key.y, key.z, from.x, from.y, from.z, to.x, to.y, to.z) <= KEYSTONE_RADIUS + direct.getBbWidth() * 0.5 + 0.2;
        }
        if (!(attacker instanceof LivingEntity living)) return false;
        Vec3 eye = living.getEyePosition();
        Vec3 reach = eye.add(living.getLookAngle().scale(8.0));
        return WeakPoint.toSegment(key.x, key.y, key.z, eye.x, eye.y, eye.z, reach.x, reach.y, reach.z) <= KEYSTONE_RADIUS;
    }

    private void setState(State state) {
        this.entityData.set(DATA_STATE, (byte) state.ordinal());
        this.stateTicks = 0;
    }

    /** Wakes it (public for tests): it rises with its bar, and tells the arena once where to strike. */
    public void wake() {
        if (state() != State.DORMANT || bound) return;
        setState(State.IDLE);
        this.rest = REST_BETWEEN;
        this.alone = 0;
        bar.setVisible(true);
        playSound(ModSounds.ARCHITECT_WAKE.get(), 3.0F, 1.0F);
        if (level() instanceof ServerLevel level) {
            Vec3 key = keystonePosition();
            level.sendParticles(ParticleTypes.END_ROD, key.x, key.y, key.z, 40, 0.6, 0.6, 0.6, 0.06);
            for (ServerPlayer p : near(level, ARENA)) {
                if (p.entityTags().contains(HINTED)) continue;
                p.addTag(HINTED);
                p.sendOverlayMessage(Component.translatable("hint.promisedend.the_architect.keystone"));
            }
            // woken again in its last phase, after a reset sent them away
            if (phase >= 2 && called.isEmpty()) callCrawlers(level);
        }
    }

    // ---- the fight ------------------------------------------------------------------------------------------------

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (home == null) home = blockPosition();
        if (savedAt >= 0) {
            // loaded again: the time it was away, unloaded, counts toward its reset
            if (state() != State.DORMANT) alone += (int) Math.min(RESET_TICKS, Math.max(0L, level.getGameTime() - savedAt));
            savedAt = -1;
        }
        if (tickCount % 10 == 0) updateBar(level);
        if (state() == State.DORMANT) {
            hold(Vec3.atBottomCenterOf(home));
            if (tickCount % 10 == 0 && !bound && !near(level, WAKE_RANGE).isEmpty()) wake();
            return;
        }
        if (open > 0 && --open % 4 == 0) {
            Vec3 key = keystonePosition();
            level.sendParticles(GLOW, key.x, key.y, key.z, 2, 0.25, 0.25, 0.25, 0.0);
        }
        // the arena put back after a minute with no one near
        if (near(level, RESET_RANGE).isEmpty()) {
            if (++alone >= RESET_TICKS) {
                resetArena(level);
                sleep();
                return;
            }
        } else {
            alone = 0;
        }
        LivingEntity target = target(level);
        Vec3 over = Vec3.atBottomCenterOf(home).add(0, HOVER, 0);
        if (target == null) {
            if (state() == State.UNMAKE) marked.clear();
            if (state() != State.IDLE) setState(State.IDLE);
            hold(over);
            return;
        }
        face(target);
        if (!target.getUUID().equals(seenId)) {
            seenId = target.getUUID();
            pace = Vec3.ZERO;
        } else {
            pace = pace.scale(0.5).add(target.position().subtract(seen).scale(0.5));
        }
        seen = target.position();
        Vec3 lean = target.position().subtract(over).multiply(1, 0, 1).scale(LEAN);
        if (lean.length() > LEASH) lean = lean.normalize().scale(LEASH);
        hold(over.add(lean));
        stateTicks++;
        switch (state()) {
            case IDLE -> {
                if (--rest <= 0) {
                    if (onHub(target)) sweep(level, target);
                    else begin(level, target);
                }
            }
            case RAISE -> {
                if (stateTicks == RAISE_AT) raiseWall(level, target);
                if (stateTicks >= RAISE_TICKS) done();
            }
            case THROW -> {
                if (stateTicks == THROW_WINDUP) throwShard(level, target);
                if (stateTicks >= THROW_TICKS) done();
            }
            case UNMAKE -> {
                if (stateTicks % 4 == 0) glow(level);
                if (stateTicks >= UNMAKE_TELL) {
                    unmake(level);
                    done();
                }
            }
            case DORMANT -> {}
        }
    }

    /** Starts its next work in its phase's turn. */
    private void begin(ServerLevel level, LivingEntity target) {
        List<State> works = WORKS.get(Mth.clamp(phase, 0, WORKS.size() - 1));
        State work = works.get(next % works.size());
        next++;
        setState(work);
        if (open > 0) playSound(ModSounds.ARCHITECT_SHUT.get(), 1.5F, 1.0F);
        open = 0;
        if (began.size() >= 32) began.removeFirst();
        began.add(tickCount + " " + work);
        switch (work) {
            case RAISE -> playSound(ModSounds.ARCHITECT_RAISE.get(), 2.5F, 1.0F);
            case THROW -> playSound(ModSounds.ARCHITECT_THROW.get(), 2.0F, 1.0F);
            case UNMAKE -> startUnmaking(level, target);
            default -> {}
        }
    }

    private void done() {
        setState(State.IDLE);
        this.rest = ArchitectFight.rest(phase);
    }

    /** Whether {@code e} stands under it, on its hub, where its walls and unmaking can't reach. */
    private boolean onHub(Entity e) {
        BlockPos home = rest();
        return Math.hypot(e.getX() - (home.getX() + 0.5), e.getZ() - (home.getZ() + 0.5)) <= HUB_REACH;
    }

    /** Whether {@code e} stands over its disc: only a fighter there finds its cage open. */
    private boolean onDisc(Entity e) {
        BlockPos home = rest();
        return Math.hypot(e.getX() - (home.getX() + 0.5), e.getZ() - (home.getZ() + 0.5)) <= ARENA_RADIUS + 1.0;
    }

    /**
     * Its answer to a fighter under it on the hub, in the place of its next work: a sweep of its frames that deals
     * {@link #SWEEP_DAMAGE} and throws them out onto the tiles (public for tests).
     */
    public void sweep(ServerLevel level, LivingEntity target) {
        Vec3 out = target.position().subtract(Vec3.atBottomCenterOf(rest())).multiply(1, 0, 1);
        if (out.lengthSqr() < 1.0E-4) out = Vec3.directionFromRotation(0.0F, getYRot()).multiply(1, 0, 1);
        out = out.normalize();
        target.hurtServer(level, damageSources().mobAttack(this), SWEEP_DAMAGE);
        target.setDeltaMovement(out.x * SWEEP_PUSH, 0.45, out.z * SWEEP_PUSH);
        // to the trackers, and (syncVelocity) to a swept player's own client too
        target.needsSync = true;
        if (target instanceof ServerPlayer p) p.syncVelocity = true;
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, getX() + Math.cos(a) * 2.0, rest().getY() + 1.0, getZ() + Math.sin(a) * 2.0, 1, 0, 0, 0, 0);
        }
        playSound(ModSounds.ARCHITECT_SWEEP.get(), 2.5F, 1.0F);
        sweeps++;
        if (began.size() >= 32) began.removeFirst();
        began.add(tickCount + " SWEEP");
        this.rest = ArchitectFight.rest(phase);
    }

    /** Whether ({@code x}, {@code z}) is on its arena: within the disc, and off the hub it hangs over. */
    private boolean onArena(double x, double z) {
        BlockPos home = rest();
        double dx = x - (home.getX() + 0.5), dz = z - (home.getZ() + 0.5);
        double d = Math.sqrt(dx * dx + dz * dz);
        return d <= ARENA_RADIUS && d > 2.0;
    }

    /**
     * Raises a wall across its target's way to it: {@link #WALL_LENGTH} columns {@link #WALL_HEIGHT} high,
     * {@link #WALL_OUT} from the target on its side, square to the line between them. Only over the arena's own lattice
     * (a tile, the hub's raw lattice, or a torn one's ghost) and only into air.
     */
    public int raiseWall(ServerLevel level, LivingEntity target) {
        Vec3 me = Vec3.atBottomCenterOf(rest());
        Vec3 to = me.subtract(target.position()).multiply(1, 0, 1);
        if (to.lengthSqr() < 1.0E-4) to = new Vec3(1, 0, 0);
        double gap = to.length();
        Vec3 dir = to.normalize();
        Vec3 centre = target.position().add(dir.scale(Math.min(WALL_OUT, gap * 0.5)));
        Vec3 across = new Vec3(-dir.z, 0, dir.x);
        int floor = floorY(), raised = 0;
        Set<BlockPos> columns = new LinkedHashSet<>();
        for (int i = -WALL_LENGTH / 2; i <= WALL_LENGTH / 2; i++) {
            Vec3 c = centre.add(across.scale(i));
            columns.add(BlockPos.containing(c.x, floor, c.z));
        }
        for (BlockPos column : columns) {
            if (!onArena(column.getX() + 0.5, column.getZ() + 0.5) || !arenaFloor(level.getBlockState(column))) continue;
            for (int y = 1; y <= WALL_HEIGHT; y++) {
                if (PhantomLatticeBlock.raise(level, column.above(y), WALL_TICKS)) raised++;
            }
        }
        if (raised > 0) walls++;
        return raised;
    }

    private static boolean arenaFloor(BlockState state) {
        return state.is(ModTags.LATTICE_TEARABLE) || state.is(ModBlocks.PHANTOM_LATTICE.get());
    }

    /** A throw at where its target will be when it comes down, and its cage opens. */
    private void throwShard(ServerLevel level, LivingEntity target) {
        Vec3 from = keystonePosition(), body = target.position().add(0, target.getBbHeight() * 0.4, 0), aim = body;
        for (int i = 0; i < 2; i++) {
            Vec3 lead = pace.multiply(1, 0, 1).scale(ArchitectFight.flight(Math.hypot(aim.x - from.x, aim.z - from.z), LatticeShard.SPEED));
            if (lead.length() > LEAD_MOST) lead = lead.normalize().scale(LEAD_MOST);
            aim = body.add(lead);
        }
        level.addFreshEntity(new LatticeShard(level, this, aim));
        throwsMade++;
        open = ArchitectFight.openAfterThrow(phase);
        level.sendParticles(ParticleTypes.END_ROD, from.x, from.y, from.z, 12, 0.3, 0.3, 0.3, 0.05);
        playSound(ModSounds.ARCHITECT_OPEN.get(), 2.0F, 1.0F);
    }

    /** Marks the tiles round its target's feet to go at the end of {@link #UNMAKE_TELL} (public for tests). */
    public int startUnmaking(ServerLevel level, LivingEntity target) {
        setState(State.UNMAKE);
        marked.clear();
        int floor = floorY();
        int r = Mth.ceil(UNMAKE_RADIUS);
        BlockPos feet = BlockPos.containing(target.getX(), floor, target.getZ());
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos at = feet.offset(dx, 0, dz);
                double ox = at.getX() + 0.5 - target.getX(), oz = at.getZ() + 0.5 - target.getZ();
                if (ox * ox + oz * oz > UNMAKE_RADIUS * UNMAKE_RADIUS || !onArena(at.getX() + 0.5, at.getZ() + 0.5)) continue;
                if (level.getBlockState(at).is(ModBlocks.LATTICE_TILE.get())) marked.add(at);
            }
        }
        playSound(ModSounds.ARCHITECT_UNMAKE.get(), 2.5F, 1.0F);
        level.playSound(null, feet, ModSounds.LATTICE_GROAN.get(), SoundSource.HOSTILE, 1.5F, 0.8F);
        glow(level);
        return marked.size();
    }

    /** The marked tiles glow while they go, a lit haze over each, so a step off them is in time. */
    private void glow(ServerLevel level) {
        for (BlockPos at : marked) {
            level.sendParticles(GLOW, at.getX() + 0.5, at.getY() + 1.05, at.getZ() + 0.5, 3, 0.3, 0.02, 0.3, 0.0);
            if (stateTicks % 8 == 0) level.sendParticles(ParticleTypes.END_ROD, at.getX() + 0.5, at.getY() + 1.1, at.getZ() + 0.5, 1, 0.3, 0.05, 0.3, 0.02);
        }
    }

    /** The marked tiles go, for {@link #UNMAKE_TICKS}. */
    private void unmake(ServerLevel level) {
        for (BlockPos at : marked) {
            if (level.getBlockState(at).is(ModBlocks.LATTICE_TILE.get()) && PhantomLatticeBlock.tear(level, at, UNMAKE_TICKS)) unmade++;
        }
        marked.clear();
    }

    /** The tiles marked now (for tests). */
    public List<BlockPos> marked() {
        return List.copyOf(marked);
    }

    /** Runs its unmaking to its end now (for tests). */
    public void finishUnmaking(ServerLevel level) {
        unmake(level);
        done();
    }

    /** Settles back over its hub, dormant, with its wounds: no one is left to fight. */
    private void sleep() {
        setState(State.DORMANT);
        this.alone = 0;
        this.next = 0;
        this.open = 0;
        marked.clear();
        setTarget(null);
        bar.setVisible(false);
        bar.removeAllPlayers();
    }

    /**
     * Puts its arena back (public for tests): every Phantom Lattice within its reach re-formed or, raised, gone; its
     * Crawlers sent away. Returns the blocks put back.
     */
    public int resetArena(ServerLevel level) {
        marked.clear();
        int back = 0;
        if (home != null) {
            int r = Mth.ceil(ARENA_RADIUS) + 1, floor = floorY();
            BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    for (int y = floor - 1; y <= floor + WALL_HEIGHT + 1; y++) {
                        at.set(home.getX() + dx, y, home.getZ() + dz);
                        if (PhantomLatticeBlock.restore(level, at)) back++;
                    }
                }
            }
        }
        for (UUID id : called) {
            Entity crawler = level.getEntity(id);
            if (crawler != null && crawler.isAlive()) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, crawler.getX(), crawler.getY() + 0.4, crawler.getZ(), 20, 0.4, 0.3, 0.4, 0.05);
                crawler.discard();
            }
        }
        called.clear();
        return back;
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

    private void hold(Vec3 at) {
        getMoveControl().setWantedPosition(at.x, at.y, at.z, 1.0);
        if (position().distanceToSqr(at) < 0.04) setDeltaMovement(getDeltaMovement().scale(0.5));
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

    // ---- damage: only while its cage is open, the keystone harder; its phases -------------------------------------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, amount);
        if (state() == State.DORMANT) {
            if (source.getEntity() instanceof ServerPlayer p && !p.isCreative() && !p.isSpectator()) {
                if (bound) tell(p, "message.promisedend.the_architect.bound");
                else wake();
            }
            return false;
        }
        Entity attacker = source.getEntity();
        if (open <= 0 || attacker == null || !onDisc(attacker)) {
            if (attacker instanceof ServerPlayer p) {
                playSound(ModSounds.ARCHITECT_SHUT.get(), 1.5F, 1.2F);
                tell(p, onDisc(p) ? "hint.promisedend.the_architect.shut" : "hint.promisedend.the_architect.off_disc");
            }
            return false;
        }
        boolean key = onKeystone(source);
        boolean hurt = super.hurtServer(level, source, key ? amount * KEYSTONE_DAMAGE : amount);
        if (hurt && key) {
            keystoneHits++;
            Vec3 at = keystonePosition();
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 16, 0.3, 0.3, 0.3, 0.15);
            playSound(ModSounds.ARCHITECT_KEYSTONE.get(), 2.0F, 1.0F);
        }
        if (hurt && source.getEntity() instanceof ServerPlayer p) fought.add(p.getUUID());
        if (hurt && isAlive()) checkPhase(level);
        return hurt;
    }

    /** Tells {@code p} why a blow did nothing, at most once in two seconds. */
    private void tell(ServerPlayer p, String key) {
        if (tickCount - toldAt < 40) return;
        toldAt = tickCount;
        p.sendOverlayMessage(Component.translatable(key));
    }

    /** The phase its health has reached ({@link ArchitectFight#phaseFor}). */
    public static int phaseFor(float share, int current) {
        return ArchitectFight.phaseFor(share, current);
    }

    private void checkPhase(ServerLevel level) {
        int reached = phaseFor(getHealth() / getMaxHealth(), phase);
        if (reached == phase) return;
        this.phase = reached;
        this.next = 0;
        if (phase >= 2) callCrawlers(level);
        playSound(ModSounds.ARCHITECT_PHASE.get(), 3.0F, 1.0F);
        Vec3 key = keystonePosition();
        level.sendParticles(ParticleTypes.END_ROD, key.x, key.y, key.z, 30 + 20 * phase, 0.8, 1.2, 0.8, 0.12);
    }

    /** Two Lattice Crawlers onto the arena, either side of its target's way to it (public for tests). */
    public void callCrawlers(ServerLevel level) {
        BlockPos home = rest();
        LivingEntity target = getTarget();
        Vec3 dir = target == null ? new Vec3(1, 0, 0) : target.position().subtract(Vec3.atBottomCenterOf(home)).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(1, 0, 0);
        dir = dir.normalize();
        Vec3 across = new Vec3(-dir.z, 0, dir.x);
        for (int i = 0; i < CRAWLERS; i++) {
            Vec3 c = Vec3.atBottomCenterOf(home).add(across.scale(i % 2 == 0 ? CRAWLER_OUT : -CRAWLER_OUT));
            BlockPos at = BlockPos.containing(c.x, home.getY(), c.z);
            if (!level.getBlockState(at.below()).isSolid()) at = home.offset(i % 2 == 0 ? 2 : -2, 0, 0);
            LatticeCrawler crawler = ModEntities.LATTICE_CRAWLER.get().create(level, EntitySpawnReason.MOB_SUMMONED);
            if (crawler == null) continue;
            crawler.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, getYRot(), 0.0F);
            crawler.setPersistenceRequired();
            if (target != null) crawler.setTarget(target);
            level.addFreshEntity(crawler);
            called.add(crawler.getUUID());
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 30, 0.4, 0.4, 0.4, 0.05);
        }
    }

    // ---- its end: the seal, the rewards, the advancement ----------------------------------------------------------

    @Override
    public void die(DamageSource source) {
        boolean already = this.dead;
        super.die(source);
        if (already || !(level() instanceof ServerLevel level)) return;
        bar.removeAllPlayers();
        resetArena(level);
        AncientWard.breakSeal(level.getServer(), 2);
        Set<ServerPlayer> rewarded = new LinkedHashSet<>();
        for (UUID id : fought) if (level.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer p) rewarded.add(p);
        // witnesses who could have fought it; one who struck it is rewarded whatever their mode now
        for (ServerPlayer p : level.players()) if (!p.isSpectator() && !p.isCreative() && p.distanceToSqr(this) <= WITNESS * WITNESS) rewarded.add(p);
        var defeated = level.getServer().getAdvancements().get(DEFEATED);
        for (ServerPlayer p : rewarded) {
            if (defeated != null) p.getAdvancements().award(defeated, "defeated");
            for (ItemStack reward : List.of(new ItemStack(ModBlocks.ARCHITECTS_PROMISE.get()), new ItemStack(ModItems.ARCHITECTS_LATTICE.get()))) {
                if (!p.getInventory().add(reward)) p.spawnAtLocation(level, reward);
            }
        }
    }

    @Override
    protected void tickDeath() {
        Constructs.shatter(this, ModBlocks.RAW_LATTICE.get().defaultBlockState(), ModBlocks.LATTICE_TILE.get().defaultBlockState(),
                ModBlocks.LATTICE_SCAFFOLD.get().defaultBlockState());
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

    /** The Citadel's: it stays on any difficulty, however far the players go. */
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
        return state() == State.DORMANT ? null : ModSounds.ARCHITECT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ARCHITECT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ARCHITECT_DEATH.get();
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
        output.putBoolean("bound", bound);
        output.putLong("saved_at", level().getGameTime());
        output.putInt("phase", phase);
        output.store("fought", UUIDUtil.CODEC.listOf(), List.copyOf(fought));
        output.store("called", UUIDUtil.CODEC.listOf(), List.copyOf(called));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.home = input.read("home", BlockPos.CODEC).orElse(null);
        // mid-work when saved, it comes back between works (an unmaking's tiles not yet gone stay)
        setState(input.getBooleanOr("awake", false) ? State.IDLE : State.DORMANT);
        bar.setVisible(state() != State.DORMANT);
        this.phase = input.getIntOr("phase", 0);
        this.bound = input.getBooleanOr("bound", false);
        this.savedAt = input.getLongOr("saved_at", -1L);
        fought.clear();
        fought.addAll(input.read("fought", UUIDUtil.CODEC.listOf()).orElse(List.of()));
        called.clear();
        called.addAll(input.read("called", UUIDUtil.CODEC.listOf()).orElse(List.of()));
    }

    // ---- GeckoLib -------------------------------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<TheArchitect>("motion", 4, test -> switch (test.animatable().state()) {
            case DORMANT -> test.setAndContinue(DORMANT);
            case RAISE -> test.setAndContinue(RAISE);
            case THROW -> test.setAndContinue(THROW);
            case UNMAKE -> test.setAndContinue(UNMAKE);
            case IDLE -> test.setAndContinue(test.isMoving() ? WALK : IDLE);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}

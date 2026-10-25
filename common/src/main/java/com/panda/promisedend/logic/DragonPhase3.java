package com.panda.promisedend.logic;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.entity.CrystalWarden;
import com.panda.promisedend.registry.ModEntities;
import com.panda.promisedend.registry.ModSounds;
import com.panda.promisedend.registry.ModTags;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.PowerParticleOption;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jspecify.annotations.Nullable;

/**
 * The dragon's third phase, from a server tick over every level's dragons (vanilla's phase registry has a private
 * factory, so this sits beside it rather than in it). Below a quarter of its health a dragon enters it once: it roars,
 * its boss bar turns purple, and it calls Crystal Wardens to the tallest pillars (3 to 5 by the Wrath it is fought at); each
 * heals it while it beams.
 * Every 20 s it then marks the nearest player's spot with a ring for 1.5 s and charges it, and void breath spreads where
 * the ring was: Slowness II and 2 damage a second for 6 s.
 *
 * <p>The phase is a tag on the dragon, so it survives a restart; the dive's clock is not saved and starts again.
 */
public final class DragonPhase3 {
    /** Below this share of its health the dragon enters phase 3. */
    public static final float THRESHOLD = 0.25F;
    /** On the dragon once it has entered phase 3 (saved with it). */
    public static final String TAG = "promisedend.phase3";
    /** On each cloud of void breath. */
    public static final String BREATH_TAG = "promisedend.void_breath";
    /** Wardens called at Wrath 0 to 5: 3 at 0 and 1, 4 at 3, 5 at 5; Wrath 2 and 4 take the row below. */
    private static final int[] WARDENS_BY_WRATH = {3, 3, 3, 4, 4, 5};
    /**
     * How far out from its pillar's side a warden's home is, towards the island's centre; how far over the ground there
     * it hovers; and how far under the pillar's top it stays at most, for a pillar standing over the void.
     */
    public static final int HOME_OUT = 4;
    public static final int HOME_HOVER = 6;
    public static final int HOME_DOWN = 3;
    /** How far under a player the dive's mark looks for ground; with none there, no dive at that player. */
    public static final int MARK_DROP = 8;
    /** Off the End (a dragon summoned anywhere else has no pillars) the wardens come to a ring this wide round it. */
    public static final int RING_RADIUS = 6;

    public static final int DIVE_INTERVAL = 400;
    public static final int TELEGRAPH_TICKS = 30;
    /**
     * The charge ends when the dragon passes this close to the mark across the ground (and within CHARGE_HEIGHT above
     * it), or after CHARGE_TIMEOUT; the breath lands on the mark either way.
     */
    public static final double CHARGE_ARRIVAL = 8.0;
    public static final int CHARGE_TIMEOUT = 80;
    /** How high above the mark the dragon may pass and still breathe down on it. */
    public static final double CHARGE_HEIGHT = 30.0;
    public static final double DIVE_RANGE = 150.0;
    public static final float BREATH_RADIUS = 3.0F;
    public static final int BREATH_TICKS = 120;
    /** Every 20 ticks: 2 a second. */
    public static final float BREATH_DAMAGE = 2.0F;
    public static final ResourceKey<DamageType> BREATH_DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE, com.panda.promisedend.PromisedEnd.id("void_breath"));

    private static final Map<EnderDragon, Dive> DIVES = new WeakHashMap<>();

    private DragonPhase3() {}

    public static int wardenCount(int wrath) {
        return WARDENS_BY_WRATH[Mth.clamp(wrath, 0, WARDENS_BY_WRATH.length - 1)];
    }

    /** Whether a dragon at this health enters phase 3 now. */
    public static boolean shouldEnter(float health, float maxHealth, boolean entered, boolean enabled) {
        return enabled && !entered && health > 0 && health < maxHealth * THRESHOLD;
    }

    public static boolean entered(EnderDragon dragon) {
        return dragon.entityTags().contains(TAG);
    }

    /** At the end of every server tick (both loaders). */
    public static void tick(MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (EnderDragon dragon : level.getDragons()) tick(level, dragon);
            tickBreath(level);
        }
    }

    private static void tick(ServerLevel level, EnderDragon dragon) {
        boolean enabled = EndConfig.dragonPhase3();
        boolean entered = entered(dragon);
        if (dragon.isAlive() && shouldEnter(dragon.getHealth(), dragon.getMaxHealth(), entered, enabled)) {
            enter(level, dragon);
            entered = true;
        }
        bossBar(dragon, entered);
        if (entered && enabled && dragon.isAlive()) dive(level, dragon);
    }

    // ---- entering ---------------------------------------------------------------------------------

    private static void enter(ServerLevel level, EnderDragon dragon) {
        dragon.addTag(TAG);
        dragon.playSound(ModSounds.DRAGON_CALL_WARDENS.get(), 16.0F, 1.0F);
        // its own call: vanilla's growl well under its routine range (0.8 to 1.1), and the portal's boom, with a burst at its head
        dragon.playSound(SoundEvents.ENDER_DRAGON_GROWL, 6.0F, 0.5F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, true, dragon.head.getX(), dragon.head.getY(), dragon.head.getZ(), 120, 1.5, 1.5, 1.5, 0.3);
        tell(level, dragon, Component.translatable("message.promisedend.dragon.call_wardens"));
        for (BlockPos home : wardenHomes(level, dragon, wardenCount(DragonWrath.foughtAt(dragon, level.getServer())))) {
            CrystalWarden warden = ModEntities.CRYSTAL_WARDEN.get().create(level, EntitySpawnReason.EVENT);
            if (warden == null) continue;
            warden.snapTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
            // a pillar moved by another mod's island could leave the home in terrain: up out of it, a few blocks at most
            for (int up = 0; up < 6 && !level.noCollision(warden, warden.getBoundingBox()); up++) warden.snapTo(warden.getX(), warden.getY() + 1, warden.getZ());
            warden.setHome(warden.blockPosition());
            warden.setDragon(dragon);
            warden.setPersistenceRequired();
            level.addFreshEntity(warden);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, true, warden.getX(), warden.getY() + 1, warden.getZ(), 60, 0.5, 0.9, 0.5, 0.1);
            warden.playSound(ModSounds.CRYSTAL_WARDEN_ARRIVE.get(), 3.0F, 1.0F);
        }
        DIVES.put(dragon, new Dive());
    }

    /**
     * Where the wardens hover: beside the tallest pillars, on the side facing the island's centre, a few blocks over
     * the ground there, where they see and beam a player fighting on the island. Off the End, a ring round the dragon.
     */
    public static List<BlockPos> wardenHomes(ServerLevel level, EnderDragon dragon, int count) {
        List<BlockPos> homes = new ArrayList<>();
        if (level.dimension() == Level.END) {
            List<Pillar> pillars = EndSpikeFeature.getSpikesForLevel(level).stream()
                    .map(s -> new Pillar(s.getCenterX(), s.getCenterZ(), s.getRadius(), s.getHeight())).toList();
            homes.addAll(pillarHomes(pillars, count, (x, z) -> {
                // the column's chunk is loaded first: an unloaded one reads as the void
                level.getChunk(x >> 4, z >> 4);
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                return top <= level.getMinY() ? Integer.MIN_VALUE : top;
            }));
        } else {
            for (int i = 0; i < count; i++) {
                double angle = Math.PI * 2 * i / count;
                homes.add(BlockPos.containing(dragon.getX() + Math.cos(angle) * RING_RADIUS, dragon.getY(), dragon.getZ() + Math.sin(angle) * RING_RADIUS));
            }
        }
        return homes;
    }

    /**
     * Homes by the {@code count} tallest pillars: {@link #HOME_OUT} out from the side facing the origin, {@link #HOME_HOVER}
     * over the first free block there ({@code groundAt}, or {@code Integer.MIN_VALUE} for the void), never higher than
     * {@link #HOME_DOWN} under the pillar's top.
     */
    public static List<BlockPos> pillarHomes(List<Pillar> pillars, int count, java.util.function.IntBinaryOperator groundAt) {
        List<Pillar> spikes = new ArrayList<>(pillars);
        spikes.sort(Comparator.comparingInt(Pillar::height).reversed());
        List<BlockPos> homes = new ArrayList<>();
        for (Pillar spike : spikes.subList(0, Math.min(count, spikes.size()))) {
            double length = Math.sqrt((double) spike.x() * spike.x() + (double) spike.z() * spike.z());
            double ux = length < 1 ? 1 : -spike.x() / length, uz = length < 1 ? 0 : -spike.z() / length;
            int out = spike.radius() + HOME_OUT;
            int x = Mth.floor(spike.x() + ux * out), z = Mth.floor(spike.z() + uz * out);
            int top = spike.height() - HOME_DOWN;
            int ground = groundAt.applyAsInt(x, z);
            homes.add(new BlockPos(x, ground == Integer.MIN_VALUE ? top : Math.min(ground + HOME_HOVER, top), z));
        }
        return homes;
    }

    /** An End pillar: its centre, radius and height (vanilla's spikes, as {@code EndSpikeFeature} lays them out). */
    public record Pillar(int x, int z, int radius, int height) {}

    /** A line on the action bar of each player within the dive's range of the dragon. */
    private static void tell(ServerLevel level, EnderDragon dragon, Component line) {
        for (var player : level.players()) {
            if (player.distanceToSqr(dragon) < DIVE_RANGE * DIVE_RANGE) player.sendSystemMessage(line, true);
        }
    }

    /** The fight's bar is purple while its dragon is in phase 3, and back to vanilla's pink for the next dragon. */
    private static void bossBar(EnderDragon dragon, boolean entered) {
        EnderDragonFight fight = dragon.getDragonFight();
        if (fight == null) return;
        BossEvent.BossBarColor want = entered ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.PINK;
        if (fight.dragonEvent.getColor() != want) fight.dragonEvent.setColor(want);
    }

    // ---- the void-breath dive -----------------------------------------------------------------------

    private enum Stage { WAIT, TELEGRAPH, CHARGE }

    private static final class Dive {
        Stage stage = Stage.WAIT;
        int cooldown = DIVE_INTERVAL;
        int ticks;
        Vec3 mark = Vec3.ZERO;
        /** Why the last breath landed: "arrived", "turned away" or "timed out" (for the harness). */
        String landed = "";
    }

    private static void dive(ServerLevel level, EnderDragon dragon) {
        Dive dive = DIVES.computeIfAbsent(dragon, d -> new Dive());
        switch (dive.stage) {
            case WAIT -> {
                if (dive.cooldown > 0) {
                    dive.cooldown--;
                } else if (canDive(dragon)) {
                    Player target = level.getNearestPlayer(dragon.getX(), dragon.getY(), dragon.getZ(), DIVE_RANGE,
                            e -> e instanceof Player p && e.isAlive() && !p.isSpectator() && !p.isCreative());
                    Vec3 mark = target == null ? null : groundUnder(level, target.position());
                    if (mark != null) startDive(level, dragon, mark);
                    else if (target != null) dive.cooldown = 20; // over the void or high up: try again in a second
                }
            }
            case TELEGRAPH -> {
                if (dive.ticks % 3 == 0) ring(level, dive.mark, dive.ticks);
                if (++dive.ticks >= TELEGRAPH_TICKS) {
                    if (!canDive(dragon)) {
                        // it began to perch or land during the telegraph: the breath lands without the charge, and the
                        // player's melee window stays as vanilla made it
                        dive.landed = "without a charge";
                        land(level, dragon, dive);
                        return;
                    }
                    dive.stage = Stage.CHARGE;
                    dragon.getPhaseManager().setPhase(EnderDragonPhase.CHARGING_PLAYER);
                    dragon.getPhaseManager().getPhase(EnderDragonPhase.CHARGING_PLAYER).setTarget(dive.mark);
                }
            }
            case CHARGE -> {
                // the ring stays until the breath lands, so the danger is never invisible
                if (dive.ticks % 3 == 0) ring(level, dive.mark, dive.ticks);
                // it breathes down as it passes over the mark: vanilla's charge swoops low but seldom touches the ground
                double dx = dragon.getX() - dive.mark.x, dz = dragon.getZ() - dive.mark.z;
                boolean arrived = dx * dx + dz * dz < CHARGE_ARRIVAL * CHARGE_ARRIVAL && dragon.getY() - dive.mark.y < CHARGE_HEIGHT;
                boolean turnedAway = dragon.getPhaseManager().getCurrentPhase().getPhase() != EnderDragonPhase.CHARGING_PLAYER;
                if (arrived || turnedAway || ++dive.ticks >= TELEGRAPH_TICKS + CHARGE_TIMEOUT) {
                    dive.landed = arrived ? "arrived" : turnedAway ? "turned away" : "timed out";
                    land(level, dragon, dive);
                }
            }
        }
    }

    /** The breath on the mark, a stream of it from the dragon's head down to it, and the clock set for the next dive. */
    private static void land(ServerLevel level, EnderDragon dragon, Dive dive) {
        Vec3 from = dragon.head.position();
        PowerParticleOption breath = PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);
        for (int i = 0; i <= 24; i++) {
            Vec3 at = from.lerp(dive.mark, i / 24.0);
            level.sendParticles(breath, true, true, at.x, at.y, at.z, 2, 0.2, 0.2, 0.2, 0.01);
            if (i % 3 == 0) level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, true, at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0.0);
        }
        breathe(level, dragon, dive.mark);
        dive.stage = Stage.WAIT;
        dive.cooldown = DIVE_INTERVAL;
    }

    /** The top of the ground under a spot (up to {@link #MARK_DROP} below it), or null where there is none. */
    public static @Nullable Vec3 groundUnder(ServerLevel level, Vec3 at) {
        BlockHitResult hit = level.clip(new ClipContext(at.add(0, 0.1, 0), at.subtract(0, MARK_DROP, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }

    /** Not while it perches, lands, takes off or dies, nor in the middle of a charge of its own. */
    private static boolean canDive(EnderDragon dragon) {
        DragonPhaseInstance phase = dragon.getPhaseManager().getCurrentPhase();
        EnderDragonPhase<?> type = phase.getPhase();
        return !phase.isSitting() && type != EnderDragonPhase.DYING && type != EnderDragonPhase.LANDING_APPROACH
                && type != EnderDragonPhase.LANDING && type != EnderDragonPhase.TAKEOFF && type != EnderDragonPhase.CHARGING_PLAYER;
    }

    /** The dive's step for this dragon (WAIT, TELEGRAPH or CHARGE), for tests and the harness. */
    public static String diveStage(EnderDragon dragon) {
        Dive dive = DIVES.get(dragon);
        return dive == null ? "NONE" : dive.stage.name();
    }

    /** Why the last breath landed where it did, for the harness. */
    public static String lastLanding(EnderDragon dragon) {
        Dive dive = DIVES.get(dragon);
        return dive == null ? "" : dive.landed;
    }

    /** Where the current dive's mark is, for tests and the harness. */
    public static Vec3 diveMark(EnderDragon dragon) {
        Dive dive = DIVES.get(dragon);
        return dive == null ? Vec3.ZERO : dive.mark;
    }

    /** Sets the time to the next dive (the harness: it waits for one without waiting out the 20 s). */
    public static void setDiveCooldown(EnderDragon dragon, int ticks) {
        DIVES.computeIfAbsent(dragon, d -> new Dive()).cooldown = ticks;
    }

    /** Marks a spot and starts the 1.5 s telegraph (public for GameTests). */
    public static void startDive(ServerLevel level, EnderDragon dragon, Vec3 mark) {
        Dive dive = DIVES.computeIfAbsent(dragon, d -> new Dive());
        dive.stage = Stage.TELEGRAPH;
        dive.ticks = 0;
        dive.mark = mark;
        level.playSound(null, mark.x, mark.y, mark.z, ModSounds.DRAGON_VOID_BREATH_GATHER.get(), SoundSource.HOSTILE, 2.0F, 1.0F);
    }

    /**
     * The ring is the cloud's own edge, drawn on the ground: what is inside it when the breath lands is caught. A dense
     * edge of breath that turns as it is drawn, and portal motes rising inside, more of them as the landing nears, so
     * the player standing in it sees it at their feet as well as from outside.
     */
    private static void ring(ServerLevel level, Vec3 at, int tick) {
        PowerParticleOption breath = PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F);
        double turn = tick * 0.05;
        for (int i = 0; i < 40; i++) {
            double angle = Math.PI * 2 * i / 40 + turn;
            double x = at.x + Math.cos(angle) * BREATH_RADIUS, z = at.z + Math.sin(angle) * BREATH_RADIUS;
            level.sendParticles(breath, true, true, x, at.y + 0.1, z, 1, 0, 0, 0, 0);
        }
        int motes = 4 + tick / 3;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, true, at.x, at.y + 0.2, at.z, motes, BREATH_RADIUS * 0.5, 0.05, BREATH_RADIUS * 0.5, 0.02);
    }

    /** Void breath where the mark was (public for GameTests). */
    public static AreaEffectCloud breathe(ServerLevel level, @Nullable EnderDragon dragon, Vec3 at) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, at.x, at.y, at.z);
        if (dragon != null) cloud.setOwner(dragon);
        cloud.setRadius(BREATH_RADIUS);
        cloud.setRadiusPerTick(0.0F);
        cloud.setDuration(BREATH_TICKS);
        cloud.setWaitTime(0);
        cloud.setCustomParticle(PowerParticleOption.create(ParticleTypes.DRAGON_BREATH, 1.0F));
        cloud.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
        cloud.addTag(BREATH_TAG);
        level.addFreshEntity(cloud);
        level.playSound(null, at.x, at.y, at.z, ModSounds.DRAGON_VOID_BREATH.get(), SoundSource.HOSTILE, 3.0F, 1.0F);
        return cloud;
    }

    /** Each cloud of void breath hurts what stands in it once a second, the dragon and its constructs aside. */
    private static void tickBreath(ServerLevel level) {
        for (AreaEffectCloud cloud : level.getEntities(EntityTypes.AREA_EFFECT_CLOUD, c -> c.entityTags().contains(BREATH_TAG))) {
            // its own look beside vanilla's breath: void motes rising out of it
            if (cloud.tickCount % 4 == 0) level.sendParticles(ParticleTypes.REVERSE_PORTAL, cloud.getX(), cloud.getY() + 0.2, cloud.getZ(), 6,
                    cloud.getRadius() * 0.5, 0.05, cloud.getRadius() * 0.5, 0.02);
            if (cloud.tickCount % 20 != 1) continue;
            DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(BREATH_DAMAGE_TYPE),
                    cloud, cloud.getOwner());
            double r2 = cloud.getRadius() * cloud.getRadius();
            for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, cloud.getBoundingBox().expandTowards(0, 2, 0),
                    e -> e.isAlive() && !(e instanceof EnderDragon) && !e.is(ModTags.CONSTRUCTS)
                            // on Peaceful the dragon's own breath and the Wardens don't hurt players, so neither does this
                            && !(e instanceof Player && level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL))) {
                double dx = victim.getX() - cloud.getX(), dz = victim.getZ() - cloud.getZ();
                if (dx * dx + dz * dz <= r2) victim.hurtServer(level, source, BREATH_DAMAGE);
            }
        }
    }
}

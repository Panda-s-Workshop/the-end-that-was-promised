package com.panda.promisedend.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.registry.ModSounds;
import com.panda.promisedend.registry.ModTags;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Starfall: while a player is in the outer End, every {@code starfall.interval_ticks} or so (StarfallSchedule) a
 * star falls on an island near one of them. Thirty seconds of warning (a beam of light where it will land, a chat line
 * and a far rumble for the players within 256), then a streak, a flash and a crater holding Starmetal Ore and a Starfall
 * Core. A crater only takes blocks in {@code #promisedend:starfall_replaceable} (and what anything may replace, as
 * grass), and no star lands within 8 blocks of a block entity or of a block outside {@code #promisedend:starfall_natural}:
 * what a player built. At most {@code starfall.max_per_day} a Minecraft day. The schedule and a fall on its way are
 * saved with the world.
 */
public final class Starfall {
    /** The beam's particles go up every this many ticks. */
    private static final int BEAM_EVERY = 4;
    /** The beam's height over the target, and its colour: the star's pale gold, the biggest dust there is. */
    private static final double BEAM_HEIGHT = 96;
    private static final net.minecraft.core.particles.DustParticleOptions BEAM = new net.minecraft.core.particles.DustParticleOptions(0xFFE7A0, 4.0F);
    /** Tries at a target round the chosen player, and the wait before the next look when none was safe: 10 s. */
    private static final int TRIES = 24, RETRY = 200;

    private Starfall() {}

    /** Whether the Starfall's clock has started: the Maw's seal broken, so stars fall from Act IV on. */
    public static boolean started(MinecraftServer server) {
        return (AncientWard.broken(server) & 1 << AncientWard.SEALS.indexOf("maw")) != 0;
    }

    /** At the end of every server tick. */
    public static void tick(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) return;
        Stored s = Stored.of(server);
        long now = end.getGameTime();
        // a fall on its way lands even if the Starfall was turned off since: its warning was given
        if (s.target.isPresent()) {
            BlockPos at = s.target.get();
            if (now >= s.impactAt) {
                s.target = Optional.empty();
                s.setDirty();
                if (end.isLoaded(at)) impact(end, at, StarfallSchedule.radius(random(end)));
            } else if (now % BEAM_EVERY == 0) {
                beam(end, at, s.impactAt - now);
            }
            return;
        }
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : end.players()) if (!p.isSpectator() && StarfallSchedule.outer(p.getX(), p.getZ())) out.add(p);
        long day = now / StarfallSchedule.DAY;
        int today = StarfallSchedule.today(now, s.day, s.today);
        switch (StarfallSchedule.step(EndConfig.starfallEnabled(), started(server), !out.isEmpty(), s.nextAt, now, today, EndConfig.starfallMaxPerDay())) {
            case IDLE -> {
                return;
            }
            case PLAN -> {
                s.nextAt = now + StarfallSchedule.nextIn(random(end), EndConfig.starfallInterval());
                s.setDirty();
                return;
            }
            case TOMORROW -> {
                s.nextAt = (day + 1) * StarfallSchedule.DAY;
                s.setDirty();
                return;
            }
            case LOOK -> {
                s.day = day;
                s.today = today;
            }
        }
        BlockPos target = target(end, out.get(end.getRandom().nextInt(out.size())));
        if (target == null) {
            s.nextAt = now + RETRY;
            s.setDirty();
            return;
        }
        s.today++;
        s.nextAt = now + StarfallSchedule.nextIn(random(end), EndConfig.starfallInterval());
        warn(end, s, target);
    }

    private static Random random(ServerLevel level) {
        return new Random(level.getRandom().nextLong());
    }

    /**
     * A fall near {@code player} now (the command's), past the schedule, the outer End and the day's cap; where it will
     * land, or null if there was no safe island in reach or a fall is on its way already.
     */
    public static @Nullable BlockPos now(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.END) return null;
        Stored s = Stored.of(level.getServer());
        if (s.target.isPresent()) return null;
        BlockPos target = target(level, player);
        if (target != null) warn(level, s, target);
        return target;
    }

    /** Where a fall on its way will land, if one is. */
    public static Optional<BlockPos> pending(MinecraftServer server) {
        return Stored.of(server).target;
    }

    /** Brings a fall on its way down in {@code ticks}, not to wait out its warning (the harness's). */
    public static void hurry(MinecraftServer server, int ticks) {
        Stored s = Stored.of(server);
        ServerLevel end = server.getLevel(Level.END);
        if (s.target.isEmpty() || end == null) return;
        s.impactAt = Math.min(s.impactAt, end.getGameTime() + ticks);
        s.setDirty();
    }

    /** A safe surface point on an island 48-160 from the player, in a loaded chunk; null after {@link #TRIES}. */
    static @Nullable BlockPos target(ServerLevel level, ServerPlayer player) {
        return target(level, player, null);
    }

    /** As {@link #target(ServerLevel, ServerPlayer)}, with why each try was turned down added to {@code why} (the harness's). */
    public static @Nullable BlockPos target(ServerLevel level, ServerPlayer player, @Nullable List<String> why) {
        Random random = random(level);
        for (int i = 0; i < TRIES; i++) {
            double[] xz = StarfallSchedule.around(random, player.getX(), player.getZ());
            BlockPos column = BlockPos.containing(xz[0], 0, xz[1]);
            String no = null;
            BlockPos ground = null;
            if (!level.isLoaded(column)) no = "not loaded";
            else {
                int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
                ground = new BlockPos(column.getX(), top - 1, column.getZ());
                if (top <= level.getMinY() + 1) no = "no island";
                else if (!level.getBlockState(ground).is(ModTags.STARFALL_REPLACEABLE)) no = "on " + level.getBlockState(ground).getBlock();
                else no = refusal(level, ground, StarfallSchedule.CLEAR);
            }
            if (no == null) return ground;
            if (why != null) why.add(column.getX() + "," + column.getZ() + ": " + no);
        }
        return null;
    }

    /**
     * Whether a star may land at {@code ground}: within {@code clear} of it, no block entity and nothing outside
     * {@code #promisedend:starfall_natural}, every block loaded; and ground it can take under two thirds of a 7 by 7
     * square round it, so the crater is in an island and not on an edge.
     */
    public static boolean safe(ServerLevel level, BlockPos ground, int clear) {
        return refusal(level, ground, clear) == null;
    }

    /** Why a star may not land at {@code ground} ({@link #safe}), or null if it may. */
    private static @Nullable String refusal(ServerLevel level, BlockPos ground, int clear) {
        for (BlockPos b : BlockPos.betweenClosed(ground.offset(-clear, -clear, -clear), ground.offset(clear, clear, clear))) {
            // under the world's floor or over its roof there is nothing to find
            if (level.isOutsideBuildHeight(b)) continue;
            if (!level.isLoaded(b)) return "its edge not loaded at " + b.toShortString();
            BlockState state = level.getBlockState(b);
            if (state.isAir()) continue;
            if (state.hasBlockEntity()) return "a block entity, " + state.getBlock() + ", at " + b.toShortString();
            if (!state.is(ModTags.STARFALL_NATURAL)) return "built: " + state.getBlock() + " at " + b.toShortString();
        }
        int under = 0, all = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                all++;
                if (level.getBlockState(ground.offset(dx, -1, dz)).is(ModTags.STARFALL_REPLACEABLE)) under++;
            }
        }
        return under * 3 >= all * 2 ? null : "an edge: ground under " + under + " of " + all;
    }

    private static void warn(ServerLevel level, Stored s, BlockPos target) {
        s.target = Optional.of(target);
        s.impactAt = level.getGameTime() + StarfallSchedule.WARNING_TICKS;
        s.setDirty();
        for (ServerPlayer p : near(level, target)) {
            double dx = target.getX() + 0.5 - p.getX(), dz = target.getZ() + 0.5 - p.getZ();
            p.sendSystemMessage(Component.translatable("message.promisedend.starfall.warning", Math.round(Math.sqrt(dx * dx + dz * dz)),
                    Component.translatable("direction.promisedend." + StarfallSchedule.compass(dx, dz))));
            // far off, as thunder is: played at the listener, so it carries the whole 256
            hear(p, ModSounds.STARFALL_WARNING.get());
        }
        PromisedEnd.LOGGER.info("A star will fall at {} in {} ticks", target.toShortString(), StarfallSchedule.WARNING_TICKS);
    }

    private static void hear(ServerPlayer p, SoundEvent sound) {
        p.connection.send(new ClientboundSoundPacket(BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound), SoundSource.WEATHER,
                p.getX(), p.getY(), p.getZ(), 1.0F, 1.0F, p.getRandom().nextLong()));
    }

    private static List<ServerPlayer> near(ServerLevel level, BlockPos at) {
        List<ServerPlayer> out = new ArrayList<>();
        Vec3 c = Vec3.atCenterOf(at);
        for (ServerPlayer p : level.players()) if (p.distanceToSqr(c) <= StarfallSchedule.TOLD * StarfallSchedule.TOLD) out.add(p);
        return out;
    }

    /**
     * The beam over where it will land, unlike an updraft's rising white motes: a pale gold column to the sky with
     * sparks falling down it, denser in its last five seconds, and a glow on the ground each second.
     */
    private static void beam(ServerLevel level, BlockPos at, long left) {
        double x = at.getX() + 0.5, z = at.getZ() + 0.5;
        int steps = left < 100 ? 48 : 24;
        for (ServerPlayer p : near(level, at)) {
            for (int i = 0; i < steps; i++) {
                double y = at.getY() + 1 + (i + level.getRandom().nextDouble()) * BEAM_HEIGHT / steps;
                level.sendParticles(p, BEAM, true, true, x, y, z, 1, 0.12, 0.3, 0.12, 0);
                // a count of 0 sends the offset as the spark's motion: down the column. The sparks are lit of themselves,
                // so they carry far where the dust dims in the End's light
                level.sendParticles(p, ParticleTypes.END_ROD, true, true, x, y, z, 0, 0, -1, 0, 0.35);
            }
            if (level.getGameTime() % 20 == 0) level.sendParticles(p, ParticleTypes.GLOW, true, true, x, at.getY() + 1.2, z, 8, 0.8, 0.1, 0.8, 0);
        }
    }

    /** The impact: a streak down, a flash, the crater, its ore and the core. */
    public static void impact(ServerLevel level, BlockPos at, int radius) {
        Random random = random(level);
        double x = at.getX() + 0.5, z = at.getZ() + 0.5;
        for (ServerPlayer p : near(level, at)) {
            for (int i = 0; i < 40; i++) {
                double t = i / 40.0;
                level.sendParticles(p, ParticleTypes.FIREWORK, true, true, x + 30 * (1 - t), at.getY() + 1 + 90 * (1 - t), z + 12 * (1 - t), 2, 0.1, 0.1, 0.1, 0);
            }
            level.sendParticles(p, ParticleTypes.EXPLOSION_EMITTER, true, true, x, at.getY() + 1, z, 1, 0, 0, 0, 0);
            level.sendParticles(p, ParticleTypes.END_ROD, true, true, x, at.getY() + 1, z, 80, 2, 1, 2, 0.4);
            hear(p, ModSounds.STARFALL_IMPACT.get());
        }
        level.playSound(null, x, at.getY(), z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.WEATHER, 4.0F, 0.6F);
        List<BlockPos> walls = crater(level, at, radius);
        scorch(level, at, radius, random);
        Collections.shuffle(walls, random);
        int ores = Math.min(StarfallSchedule.ores(random), walls.size());
        for (int i = 0; i < ores; i++) level.setBlock(walls.get(i), ModBlocks.STARMETAL_ORE.get().defaultBlockState(), 3);
        // the core lies at the bottom of the bowl, glowing, and stays until it is taken
        BlockPos bottom = at;
        while (bottom.getY() > at.getY() - radius && level.getBlockState(bottom.below()).isAir()) bottom = bottom.below();
        ItemEntity core = new ItemEntity(level, bottom.getX() + 0.5, bottom.getY() + 0.1, bottom.getZ() + 0.5, new ItemStack(ModItems.STARFALL_CORE.get()));
        core.setDeltaMovement(Vec3.ZERO);
        core.setUnlimitedLifetime();
        core.setGlowingTag(true);
        level.addFreshEntity(core);
        // what stood in it is thrown clear (a player's own client moves only with syncVelocity), its fall forgotten
        for (Entity e : level.getEntities((Entity) null, new AABB(at).inflate(radius + 2), e -> !(e instanceof ItemEntity) && !e.isSpectator())) {
            Vec3 away = e.position().subtract(x, at.getY(), z);
            Vec3 push = away.horizontalDistanceSqr() < 1e-4 ? new Vec3(0, 1, 0) : away.normalize().add(0, 0.6, 0);
            e.setDeltaMovement(e.getDeltaMovement().add(push.scale(1.2)));
            e.resetFallDistance();
            e.needsSync = true;
            if (e instanceof ServerPlayer) e.syncVelocity = true;
        }
        PromisedEnd.LOGGER.info("A star fell at {}: a crater of {}, {} Starmetal Ore", at.toShortString(), radius, ores);
    }

    /**
     * Hollows the crater round {@code at}: each block in its bowl that is in {@code #promisedend:starfall_replaceable}
     * or replaceable (grass, flowers) goes; anything else stays. Returns the replaceable blocks of its floor and walls,
     * those that face into it, where its ore goes.
     */
    public static List<BlockPos> crater(ServerLevel level, BlockPos at, int radius) {
        int depth = (int) Math.ceil(radius * 2.0 / 3.0) + 1;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -depth; dy <= 2; dy++) {
                    if (!StarfallSchedule.inCrater(dx, dy, dz, radius)) continue;
                    BlockPos b = at.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(b);
                    if (state.isAir() || !state.is(ModTags.STARFALL_REPLACEABLE) && !state.canBeReplaced()) continue;
                    level.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        List<BlockPos> walls = new ArrayList<>();
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                for (int dy = -depth - 1; dy <= 0; dy++) {
                    if (StarfallSchedule.inCrater(dx, dy, dz, radius)) continue;
                    BlockPos b = at.offset(dx, dy, dz);
                    if (!level.getBlockState(b).is(ModTags.STARFALL_REPLACEABLE)) continue;
                    for (Direction d : Direction.values()) {
                        if (d == Direction.DOWN) continue;
                        if (StarfallSchedule.inCrater(dx + d.getStepX(), dy + d.getStepY(), dz + d.getStepZ(), radius) && level.getBlockState(b.relative(d)).isAir()) {
                            walls.add(b.immutable());
                            break;
                        }
                    }
                }
            }
        }
        return walls;
    }

    /**
     * The blast round the rim: about half the ground's top blocks from just inside the rim to 2 past it, if the crater
     * could take them, burnt to Voidstone, and what grew on them gone; so a crater reads as a strike, not a dig.
     */
    static void scorch(ServerLevel level, BlockPos at, int radius, Random random) {
        int r = radius + 2;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d < radius - 0.5 || d > r + 0.5 || random.nextDouble() > 0.55) continue;
                for (int dy = 2; dy >= -radius; dy--) {
                    BlockPos b = at.offset(dx, dy, dz);
                    BlockState state = level.getBlockState(b);
                    if (state.isAir()) continue;
                    if (state.canBeReplaced() && !state.isSolid()) {
                        level.setBlock(b, Blocks.AIR.defaultBlockState(), 3);
                        continue;
                    }
                    if (state.is(ModTags.STARFALL_REPLACEABLE) && !state.is(ModBlocks.STARMETAL_ORE.get()))
                        level.setBlock(b, ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState(), 3);
                    break;
                }
            }
        }
    }

    /** When the next fall is due, the day's count, and a fall on its way. */
    private static final class Stored extends SavedData {
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.LONG.optionalFieldOf("next_at", 0L).forGetter(s -> s.nextAt),
                Codec.LONG.optionalFieldOf("day", 0L).forGetter(s -> s.day),
                Codec.INT.optionalFieldOf("today", 0).forGetter(s -> s.today),
                BlockPos.CODEC.optionalFieldOf("target").forGetter(s -> s.target),
                Codec.LONG.optionalFieldOf("impact_at", 0L).forGetter(s -> s.impactAt)).apply(i, Stored::new));
        static final SavedDataType<Stored> TYPE = new SavedDataType<>(PromisedEnd.id("starfall"), Stored::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

        long nextAt, day, impactAt;
        int today;
        Optional<BlockPos> target = Optional.empty();

        Stored() {}

        Stored(long nextAt, long day, int today, Optional<BlockPos> target, long impactAt) {
            this.nextAt = nextAt;
            this.day = day;
            this.today = today;
            this.target = target;
            this.impactAt = impactAt;
        }

        static Stored of(MinecraftServer server) {
            return server.getDataStorage().computeIfAbsent(TYPE);
        }
    }
}

package com.panda.promisedend.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

/**
 * The Ancients' Ward: a cylinder round the End's centre that no player in survival or adventure crosses
 * inward while it stands. One who does is set back where they were the tick before, outside, with the hint; one found
 * deep inside (logged in there, or put there) is sent to their respawn point. A player riding is set back with their
 * mount. Pearls thrown at it bounce off. Near it, a player sees it as a wall of motes. While it stands no player in
 * survival reaches the dragon fight's range (192 from the centre), and the fight itself is held (mixin
 * EnderDragonFightMixin), so the dragon doesn't wake for a creative player either.
 *
 * <p>It stands while arrival by coordinates is on, fewer than all three seals are broken, and no dragon has ever been
 * killed in this world (a world from before the change keeps its open End).
 */
public final class AncientWard {
    /** The three seals, by their guardians' places: the Shulker Foundry, the Rift Maw, the Unfinished Citadel. */
    public static final List<String> SEALS = List.of("foundry", "maw", "citadel");
    public static final int ALL = (1 << SEALS.size()) - 1;
    /** How far inside the Ward a player may be found before they are sent home instead of set back. */
    public static final int DEEP = 16;
    /** How near the Ward a player sees it. */
    public static final int SEEN = 24;
    private static final DustParticleOptions MOTE = new DustParticleOptions(0xE2CCFF, 2.0F);

    /** Each player's (or their mount's) last place outside the Ward, in the End; cleared for a new server. */
    private static final Map<UUID, Vec3> OUTSIDE = new HashMap<>();
    private static @org.jspecify.annotations.Nullable MinecraftServer forServer;

    private AncientWard() {}

    /** Whether ({@code x}, {@code z}) is inside a Ward of {@code radius}. */
    public static boolean inside(double x, double z, int radius) {
        return Arrival.insideWard(x, z, radius);
    }

    public static boolean stands(MinecraftServer server) {
        if (!EndConfig.arrivalByCoordinates() || broken(server) == ALL) return false;
        ServerLevel end = server.getLevel(Level.END);
        var fight = end == null ? null : end.getDragonFight();
        return fight == null || !fight.hasPreviouslyKilledDragon();
    }

    /** The broken seals, one bit each in {@link #SEALS}' order. */
    public static int broken(MinecraftServer server) {
        return Stored.of(server).broken;
    }

    /**
     * Breaks seal {@code index} for the world, telling everyone in the End; when it was the last, the Ward falls.
     * Returns false when it was already broken.
     */
    public static boolean breakSeal(MinecraftServer server, int index) {
        Stored stored = Stored.of(server);
        int bit = 1 << index;
        if ((stored.broken & bit) != 0) return false;
        stored.broken |= bit;
        stored.setDirty();
        int count = Integer.bitCount(stored.broken);
        ServerLevel end = server.getLevel(Level.END);
        if (end != null) {
            for (ServerPlayer p : end.players()) {
                p.sendSystemMessage(Component.translatable("ward.promisedend.seal_broken", Component.translatable("ward.promisedend.seal." + SEALS.get(index)), count, SEALS.size()));
                if (stored.broken == ALL) p.sendSystemMessage(Component.translatable("ward.promisedend.falls"));
                end.playSound(null, p.blockPosition(), stored.broken == ALL ? SoundEvents.END_PORTAL_SPAWN : SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.MASTER, 1.0F, 0.8F);
            }
        }
        PromisedEnd.LOGGER.info("[promisedend] the {} seal broke ({} of {})", SEALS.get(index), count, SEALS.size());
        return true;
    }

    /** Mends every seal (the command, for tests). */
    public static void reset(MinecraftServer server) {
        Stored stored = Stored.of(server);
        stored.broken = 0;
        stored.setDirty();
    }

    /** Each player's tick: set back at the Ward, sent home from deep inside, shown the Ward near it. */
    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.END) {
            OUTSIDE.remove(player.getUUID());
            return;
        }
        if (player.isCreative() || player.isSpectator() || !stands(level.getServer())) {
            OUTSIDE.remove(player.getUUID());
            return;
        }
        int radius = EndConfig.wardRadius();
        Entity root = player.getRootVehicle();
        double x = root.getX(), z = root.getZ();
        if (!inside(x, z, radius)) {
            OUTSIDE.put(player.getUUID(), root.position());
            if (player.tickCount % 5 == 0 && !inside(x, z, radius - SEEN) && inside(x, z, radius + SEEN)) show(level, player, radius, 40);
            return;
        }
        Vec3 back = OUTSIDE.get(player.getUUID());
        if (back == null || inside(x, z, radius - DEEP)) {
            // inside with no step from outside to undo: home, as the Return Gate sends
            player.stopRiding();
            player.teleport(home(player, TeleportTransition.DO_NOTHING));
            player.sendSystemMessage(Component.translatable("ward.promisedend.sent_home"));
            OUTSIDE.remove(player.getUUID());
            return;
        }
        double d = Math.sqrt(back.x * back.x + back.z * back.z);
        Vec3 out = d < 1e-6 ? new Vec3(1, 0, 0) : new Vec3(back.x / d, 0, back.z / d);
        // the mount and its riders go back together, so no one is left in the air where the mount stood
        root.teleportTo(level, back.x, back.y, back.z, Set.of(), root.getYRot(), root.getXRot(), true);
        root.setDeltaMovement(out.scale(0.3).add(0, Math.max(0, root.getDeltaMovement().y), 0));
        root.syncVelocity = true;
        root.needsSync = true;
        player.sendOverlayMessage(Component.translatable("ward.promisedend.holds"));
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 0.6F);
        show(level, player, radius, 160);
    }

    /** Pearls thrown into the Ward bounce off it (each server tick, the End's pearls near a player at the Ward). */
    public static void tick(MinecraftServer server) {
        if (forServer != server) {
            OUTSIDE.clear();
            forServer = server;
        }
        if (server.getTickCount() % 200 == 0) OUTSIDE.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        ServerLevel end = server.getLevel(Level.END);
        if (end == null || end.players().isEmpty() || !stands(server)) return;
        int radius = EndConfig.wardRadius();
        for (ServerPlayer p : end.players()) {
            if (inside(p.getX(), p.getZ(), radius - 96) || !inside(p.getX(), p.getZ(), radius + 96)) continue;
            for (ThrownEnderpearl pearl : end.getEntitiesOfClass(ThrownEnderpearl.class, p.getBoundingBox().inflate(96), e -> inside(e.getX(), e.getZ(), radius))) {
                double d = Math.sqrt(pearl.getX() * pearl.getX() + pearl.getZ() * pearl.getZ());
                Vec3 v = pearl.getDeltaMovement();
                double ox = pearl.getX() / d, oz = pearl.getZ() / d;
                double inward = -(v.x * ox + v.z * oz);
                if (inward <= 0) continue;
                // mirror the inward part, and step it back outside
                pearl.setDeltaMovement(v.x + 2 * inward * ox, v.y, v.z + 2 * inward * oz);
                pearl.setPos(ox * (radius + 0.5), pearl.getY(), oz * (radius + 0.5));
                end.sendParticles(MOTE, pearl.getX(), pearl.getY(), pearl.getZ(), 8, 0.3, 0.3, 0.3, 0.0);
            }
        }
    }

    /**
     * Where a player sent home goes: their respawn point, as the exit portal sends, or the world's spawn when that point
     * is in the End inside a standing Ward (a respawn anchor or another mod put it there), which would send them back
     * inside every tick.
     */
    public static TeleportTransition home(ServerPlayer player, TeleportTransition.PostTeleportTransition after) {
        TeleportTransition home = player.findRespawnPositionAndUseSpawnBlock(false, after);
        MinecraftServer server = player.level().getServer();
        if (home.newLevel().dimension() != Level.END || !stands(server) || !inside(home.position().x, home.position().z, EndConfig.wardRadius())) return home;
        var spawn = server.overworld().getRespawnData();
        ServerLevel level = server.getLevel(spawn.dimension());
        if (level == null) level = server.overworld();
        Vec3 to = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.pos()));
        return new TeleportTransition(level, to, Vec3.ZERO, player.getYRot(), player.getXRot(), after);
    }

    /**
     * A patch of the Ward's wall facing the player, as motes only they see: portal motes, which keep their own glow in
     * the void's dark (dust took it and read grey), and one in three an end-rod spark.
     */
    private static void show(ServerLevel level, ServerPlayer player, int radius, int motes) {
        double a = Math.atan2(player.getZ(), player.getX());
        for (int i = 0; i < motes; i++) {
            double t = a + (level.getRandom().nextDouble() - 0.5) * 24.0 / radius;
            double y = player.getY() + (level.getRandom().nextDouble() - 0.4) * 10;
            if (i % 3 == 0) level.sendParticles(player, ParticleTypes.END_ROD, true, false, Math.cos(t) * radius, y, Math.sin(t) * radius, 1, 0, 0, 0, 0);
            else level.sendParticles(player, ParticleTypes.REVERSE_PORTAL, true, false, Math.cos(t) * radius, y, Math.sin(t) * radius, 1, 0, 0, 0, 0);
        }
    }

    /** The broken seals. */
    private static final class Stored extends SavedData {
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("broken", 0).forGetter(s -> s.broken)).apply(i, Stored::new));
        static final SavedDataType<Stored> TYPE = new SavedDataType<>(PromisedEnd.id("seals"), Stored::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

        int broken;

        Stored() {}

        Stored(int broken) {
            this.broken = broken & ALL;
        }

        static Stored of(MinecraftServer server) {
            return server.getDataStorage().computeIfAbsent(TYPE);
        }
    }
}

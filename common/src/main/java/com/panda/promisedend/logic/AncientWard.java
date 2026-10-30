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
 * <p>It stands while arrival by coordinates is on, a seal still binds it, and no dragon has ever been killed in this
 * world (a world from before the change keeps its open End). A seal binds it only once its guardian is in the game.
 */
public final class AncientWard {
    /** The three seals, by their guardians' places: the Shulker Foundry, the Rift Maw, the Unfinished Citadel. */
    public static final List<String> SEALS = List.of("foundry", "maw", "citadel");
    public static final int ALL = (1 << SEALS.size()) - 1;
    /**
     * The seals whose guardian is in the game, one bit each in {@link #SEALS}' order: the Foundry's (the Hollow
     * Sentinel), the Maw's (the Void Leviathan) and the Citadel's (the Architect). A seal with no guardian to fight
     * can't be broken, so it wouldn't bind the Ward.
     */
    public static final int GUARDED = 0b111;
    /** Set (to true) for runs that need the Ward as it will stand, every guardian in: the GameTests, the screenshots. */
    public static final String ALL_GUARDIANS = "promisedend.allGuardians";
    /** How often a player in the End with the Ward down is told so by the advancement that opens the Codex's last act. */
    private static final int OPEN_CHECK = 20;
    private static final net.minecraft.resources.Identifier WARD_DOWN = PromisedEnd.id("rim/ward_down");
    /** How far inside the Ward a player may be found before they are sent home instead of set back. */
    public static final int DEEP = 16;
    /** How near the Ward a player sees it. */
    public static final int SEEN = 24;
    private static final DustParticleOptions MOTE = new DustParticleOptions(0xE2CCFF, 2.0F);

    /** Each player's (or their mount's) last place outside the Ward, in the End; cleared for a new server. */
    private static final Map<UUID, Vec3> OUTSIDE = new HashMap<>();
    private static @org.jspecify.annotations.Nullable MinecraftServer forServer;
    /** The guardians' places, in {@link #SEALS}' order. */
    private static final List<String> PLACES = List.of("shulker_foundry", "rift_maw", "unfinished_citadel");
    /** The seals whose guardian's place can generate in this server's End, and the server it was worked out for. */
    private static int placeable = ALL;
    private static @org.jspecify.annotations.Nullable MinecraftServer placeableFor;
    /** The End's world border's reach {@link #placeable} was worked out for (a border moved by a command redoes it). */
    private static double placeableReach = -1;

    private AncientWard() {}

    /** Whether ({@code x}, {@code z}) is inside a Ward of {@code radius}. */
    public static boolean inside(double x, double z, int radius) {
        return Arrival.insideWard(x, z, radius);
    }

    /** The seals whose guardian is in the game, and whose place can generate in the last server's End looked at. */
    public static int guarded() {
        return Boolean.getBoolean(ALL_GUARDIANS) ? ALL : GUARDED & placeable;
    }

    /** The seals whose guardian is in the game and whose place can generate in {@code server}'s End. */
    public static int guarded(MinecraftServer server) {
        double reach = borderReach(server);
        if (placeableFor != server || reach != placeableReach) {
            int was = placeableFor == server ? placeable : ALL;
            placeable = placeable(server);
            placeableFor = server;
            placeableReach = reach;
            // said once a change, not each tick a moving border is looked at
            for (int i = 0; i < PLACES.size(); i++) {
                if ((was & 1 << i) != 0 && (placeable & 1 << i) == 0)
                    PromisedEnd.LOGGER.warn("[promisedend] no {} can stand in this End (its biomes, or its world border {} out): its seal won't bind the Ancients' Ward", PLACES.get(i), Math.round(reach));
            }
        }
        return guarded();
    }

    /**
     * The seals whose guardian's place lists a biome the End's biome source can give. With the terrain off under another
     * End mod's biomes none may: a seal that can't be broken would hold the Ward, and the dragon, for good.
     */
    static int placeable(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        return end == null ? ALL : placeable(end.getChunkSource().getGenerator().getBiomeSource().possibleBiomes(), server.registryAccess())
                & withinReach(borderReach(server), server.registryAccess());
    }

    /**
     * How far from the End's middle its world border lets a whole ring of places stand: to its nearest edge (a ring
     * reaching only into the corners holds few or none, and a chart or pearl could point past the border).
     */
    static double borderReach(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) return Double.MAX_VALUE;
        var b = end.getWorldBorder();
        return Math.min(Math.min(-b.getMinX(), b.getMaxX()), Math.min(-b.getMinZ(), b.getMaxZ()));
    }

    /**
     * The seals whose place's ring (its structure set's {@code min_distance}, or its fallback's) starts inside
     * {@code reach} of the End's middle, with a spread's spacing of room, so dozens can stand there: a pack's world
     * border inside the Citadels' 7,400 would hold the third seal, and the dragon, for good (public for GameTests). No
     * ring starts inside the outer End: the main island's biome reaches to 1,024.
     */
    public static int withinReach(double reach, net.minecraft.core.RegistryAccess access) {
        var sets = access.lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET);
        int mask = 0;
        for (int i = 0; i < PLACES.size(); i++) {
            long nearest = Long.MAX_VALUE;
            for (String id : List.of(PLACES.get(i), PLACES.get(i) + "_fallback")) {
                var set = sets.getValue(PromisedEnd.id(id));
                if (set == null) continue;
                nearest = Math.min(nearest, set.placement() instanceof com.panda.promisedend.worldgen.RingSpreadPlacement ring
                        ? Math.max(ring.minDistance(), com.panda.promisedend.worldgen.EndRegions.RIM_FAR) + ring.spacing() * 16L : 0);
            }
            if (nearest == Long.MAX_VALUE || nearest <= reach) mask |= 1 << i;
        }
        return mask;
    }

    /** The seals whose guardian's place lists one of {@code possible} (public for GameTests). */
    public static int placeable(java.util.Set<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> possible, net.minecraft.core.RegistryAccess access) {
        var structures = access.lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        // by key: the biome source's holders aren't always the registry's own (ours come through the mixin's set)
        var keys = new java.util.HashSet<net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>>();
        for (var h : possible) h.unwrapKey().ifPresent(keys::add);
        int mask = 0;
        for (int i = 0; i < PLACES.size(); i++) {
            // the place, or its fallback with the terrain off (any End biome)
            boolean can = false, known = false;
            for (String id : List.of(PLACES.get(i), PLACES.get(i) + "_fallback")) {
                var structure = structures.getValue(PromisedEnd.id(id));
                if (structure == null) continue;
                known = true;
                can |= structure.biomes().stream().anyMatch(h -> h.unwrapKey().map(keys::contains).orElse(false));
            }
            if (can || !known) mask |= 1 << i;
            else PromisedEnd.LOGGER.warn("[promisedend] no {} can generate in this End's biomes: its seal won't bind the Ancients' Ward", PLACES.get(i));
        }
        return mask;
    }

    /** Whether a seal still binds the Ward: one whose guardian is in the game and isn't broken. */
    public static boolean binds(int broken, int guarded) {
        return (guarded & ALL & ~broken) != 0;
    }

    /**
     * Whether the Ward has fallen: it had seals, and every one of them is broken. Once fallen it stays down, though a
     * world border grown since brings another seal into reach.
     */
    public static boolean fallen(MinecraftServer server) {
        if (!Arrivals.byCoordinates(server)) return false;
        int guarded = guarded(server);
        return Stored.of(server).fell || guarded != 0 && !binds(broken(server), guarded);
    }

    public static boolean stands(MinecraftServer server) {
        if (!Arrivals.byCoordinates(server) || Stored.of(server).fell || !binds(broken(server), guarded(server))) return false;
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
        // counted among the seals that bind it: while a guardian isn't in the game, its seal isn't one of them
        int guarded = guarded(server);
        int count = Integer.bitCount(stored.broken & guarded), of = Integer.bitCount(guarded);
        // the Ward falls with the last seal that binds it, which is the third only once every guardian is in
        boolean falls = binds(stored.broken & ~bit, guarded) && !binds(stored.broken, guarded);
        if (falls) stored.fell = true;
        ServerLevel end = server.getLevel(Level.END);
        if (end != null) {
            for (ServerPlayer p : end.players()) {
                p.sendSystemMessage(Component.translatable("ward.promisedend.seal_broken", Component.translatable("ward.promisedend.seal." + SEALS.get(index)), count, of));
                if (falls) {
                    p.sendSystemMessage(Component.translatable("ward.promisedend.falls"));
                    // and on the screen: the moment the last act opens
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 70, 20));
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.translatable("ward.promisedend.falls.subtitle")));
                    p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.translatable("ward.promisedend.falls.title")));
                }
                end.playSound(null, p.blockPosition(), falls ? SoundEvents.END_PORTAL_SPAWN : SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.MASTER, 1.0F, 0.8F);
            }
        }
        PromisedEnd.LOGGER.info("[promisedend] the {} seal broke ({} of {} that bind the Ward)", SEALS.get(index), count, of);
        return true;
    }

    /** Mends every seal (the command, for tests). */
    public static void reset(MinecraftServer server) {
        Stored stored = Stored.of(server);
        stored.broken = 0;
        stored.fell = false;
        stored.setDirty();
    }

    /** Each player's tick: set back at the Ward, sent home from deep inside, shown the Ward near it. */
    public static void tick(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.END) {
            OUTSIDE.remove(player.getUUID());
            return;
        }
        boolean stands = stands(level.getServer());
        if (player.tickCount % OPEN_CHECK == 0 && !player.isSpectator()) {
            // the last act opens while the Ward is down, and closes again if it rises (a guardian came to a world
            // where it had been down for want of one)
            var down = level.getServer().getAdvancements().get(WARD_DOWN);
            if (down != null && !stands) player.getAdvancements().award(down, "open");
            else if (down != null && player.getAdvancements().getOrStartProgress(down).isDone()) player.getAdvancements().revoke(down, "open");
        }
        if (player.isCreative() || player.isSpectator() || !stands) {
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
        player.sendOverlayMessage(Component.translatable("ward.promisedend.holds", Integer.bitCount(guarded() & ALL & ~broken(level.getServer()))));
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
                Codec.INT.optionalFieldOf("broken", 0).forGetter(s -> s.broken),
                Codec.BOOL.optionalFieldOf("fell", false).forGetter(s -> s.fell)).apply(i, Stored::new));
        static final SavedDataType<Stored> TYPE = new SavedDataType<>(PromisedEnd.id("seals"), Stored::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

        int broken;
        /** Whether the Ward fell with a seal's breaking: it stays down. */
        boolean fell;

        Stored() {}

        Stored(int broken, boolean fell) {
            this.broken = broken & ALL;
            this.fell = fell;
        }

        static Stored of(MinecraftServer server) {
            return server.getDataStorage().computeIfAbsent(TYPE);
        }
    }
}

package com.panda.promisedend.logic;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.registry.ModTags;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The void currents in the world ({@link VoidCurrent} is the field): which entities a current takes, for how long, and
 * what it does to them. A current runs in the End's band at Y 0-20 under the Skyfall Archipelago and the Voidscar
 * Wastes ({@code #promisedend:void_currents}), and with our terrain off under the vanilla biomes their content falls
 * back to. It carries what falls into it along it and holds it in the band for 20 s (or till the river runs out under
 * it), then washes it ashore onto the nearest island edge in reach (across under it, up beside it, onto it), so a fall
 * into one carries you instead of killing you; with no island in reach it lets go.
 *
 * <p>The server moves everything but players and what players drive; a player's client moves them, as an Updraft
 * Vent's column does, and the server, working out the same ride, keeps them from fall damage and from being kicked for
 * flying meanwhile. A glider isn't held, only sped along it. Left alone as by the updraft: spectators, players flying,
 * the weightless, projectiles, passengers, things hung on blocks, {@code #promisedend:void_current_ignores}, and anything
 * standing on the ground. A ride is kept by UUID, so a player who logs out held comes back on the same clock.
 */
public final class VoidCurrents {
    /**
     * The height the biome over a current is read at: where the islands it runs under are. The End's biome can differ
     * with height (its terrain class is sampled where it is asked), and down in the band, under all the land, it isn't
     * the one a player sees on the islands over it.
     */
    public static final int BIOME_Y = 64;
    /** How long before the current lets go a held player is told so, in ticks. */
    public static final int WARN_TICKS = 60;

    /** A ride in a current: how long it has been held, and once washed ashore, where to and for how long. */
    public static final class Ride {
        public int held;
        public int @Nullable [] shore;
        public int ashore;

        /** Held past the hold, with no shore: let go, until it leaves the band. */
        public boolean released() {
            return held > VoidCurrent.HOLD_TICKS && shore == null;
        }
    }

    /** The server's rides (the local player's are the client's: client/VoidCurrentView). */
    private static final Map<UUID, Ride> RIDES = new HashMap<>();
    /**
     * The server's terrain setting, as a client was sent it ({@link #heardTerrain}); null on a server and before it
     * arrives, when the client's own config is read.
     */
    private static @Nullable Boolean serverTerrain;

    private VoidCurrents() {}

    /** The seed hash the field is drawn from: the same on the server and every client (vanilla sends it at login). */
    public static long seed(Level level) {
        return level.getBiomeManager().biomeZoomSeed;
    }

    /** A client was told the server's terrain setting (null on leaving it). */
    public static void heardTerrain(@Nullable Boolean enabled) {
        serverTerrain = enabled;
    }

    private static boolean terrainEnabled(Level level) {
        return level.isClientSide() && serverTerrain != null ? serverTerrain : EndConfig.terrainEnabled();
    }

    /** The way the current runs at a point, a unit {x, z}, or null where there is none. */
    public static double @Nullable [] flowAt(Level level, double x, double y, double z) {
        if (level.dimension() != Level.END || !VoidCurrent.inBand(y)) return null;
        var biome = level.getBiome(BlockPos.containing(x, BIOME_Y, z));
        if (!biome.is(ModTags.VOID_CURRENTS) && (terrainEnabled(level) || !biome.is(ModTags.VOID_CURRENTS_FALLBACK))) return null;
        long seed = seed(level);
        if (Math.abs(VoidCurrent.field(seed, x, z)) >= VoidCurrent.WIDTH) return null;
        return VoidCurrent.flow(seed, x, z);
    }

    /** Who a current moves: not spectators, the weightless, projectiles, passengers, things hung on blocks, or the ignored. */
    public static boolean moves(Entity e) {
        return !e.isSpectator() && e.getGravity() > 0 && !e.isNoGravity() && !(e instanceof Projectile) && !e.isPassenger()
                && !(e instanceof BlockAttachedEntity) && !e.is(ModTags.VOID_CURRENT_IGNORES);
    }

    /**
     * Moves {@code e} for a tick in a current flowing {@code f}: a glider sped along it, anything else carried and held
     * towards {@code mid}. True if its speed changed (the server then sends it).
     */
    public static boolean carry(Entity e, double[] f, double mid, boolean gliding) {
        Vec3 v = e.getDeltaMovement();
        e.resetFallDistance();
        if (gliding) {
            double bonus = VoidCurrent.GLIDE_BONUS + (com.panda.promisedend.item.Augments.gliding(e, com.panda.promisedend.item.Augments.LEVIATHAN_FIN)
                    ? com.panda.promisedend.item.Augments.FIN_CURRENT : 0);
            double[] d = VoidCurrent.glide(v.x, v.z, f[0], f[1], bonus);
            if (d[0] != 0 || d[1] != 0) e.move(MoverType.SELF, new Vec3(d[0], 0, d[1]));
            return false;
        }
        double[] n = VoidCurrent.carry(v.x, v.y, v.z, e.getY(), f[0], f[1], mid);
        e.setDeltaMovement(n[0], n[1], n[2]);
        return true;
    }

    /** Where the current washes {@code e} up from where it is, by the world's heightmap, or null with none in reach. */
    public static int @Nullable [] shoreFor(Level level, Entity e) {
        return VoidCurrent.shore(e.getBlockX(), e.getBlockY(), e.getBlockZ(), (x, z) -> level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z));
    }

    /**
     * One tick of {@code e}'s ride in {@code rides}, on whichever side moves it ({@code moving}; the other side only
     * keeps a player safe meanwhile), held towards {@code mid}. {@code tell} hints the player along (the client's
     * side). True if its speed changed.
     */
    public static boolean ride(Map<UUID, Ride> rides, Level level, Entity e, @Nullable Player player, boolean moving, boolean tell, double mid) {
        Ride r = rides.get(e.getUUID());
        if (r != null && r.shore != null) {
            // being washed ashore, wherever it is now
            boolean stuck = ++r.ashore > VoidCurrent.ASHORE_TICKS;
            double[] v = stuck || r.ashore > 5 && e.onGround() ? null : VoidCurrent.ashore(e.getX(), e.getY(), e.getZ(), r.shore);
            safe(e, player);
            if (v == null) {
                // over the shore, or stuck on the way: it drops the little way left, kept safe a second for it
                if (stuck || r.ashore > 20 && e.onGround() || r.ashore > VoidCurrent.ASHORE_TICKS / 2) rides.remove(e.getUUID());
                return false;
            }
            if (!moving) return false;
            e.setDeltaMovement(v[0], v[1], v[2]);
            return true;
        }
        boolean taken = player == null ? moves(e) : !player.getAbilities().flying && !player.isSpectator();
        double[] f = taken && VoidCurrent.inBand(e.getY()) && !e.onGround() ? flowAt(level, e.getX(), e.getY(), e.getZ()) : null;
        if (f == null) {
            if (r != null && !r.released() && taken && VoidCurrent.inBand(e.getY()) && !e.onGround()) {
                // the river ran out under it (at slack air, or past the biomes it runs under) before the hold did: the
                // ride ends as at the hold's end, so it is washed ashore and not dropped mid-band
                end(r, level, e, player, tell);
                safe(e, player);
                return false;
            }
            // out of it: forgotten, but one let go stays let go till it leaves the band (no new hold on the way down)
            if (r != null && (!r.released() || !VoidCurrent.inBand(e.getY()))) rides.remove(e.getUUID());
            return false;
        }
        if (player != null && e == player && player.isFallFlying()) {
            safe(e, player);
            if (moving) carry(e, f, mid, true);
            return false;
        }
        if (r == null) {
            r = new Ride();
            rides.put(e.getUUID(), r);
            if (tell && player != null) player.sendOverlayMessage(Component.translatable("hint.promisedend.void_current.caught"));
        }
        if (r.released()) return false;
        if (++r.held > VoidCurrent.HOLD_TICKS) {
            end(r, level, e, player, tell);
            return false;
        }
        if (tell && player != null && r.held == VoidCurrent.HOLD_TICKS - WARN_TICKS) player.sendOverlayMessage(Component.translatable("hint.promisedend.void_current.letting_go"));
        safe(e, player);
        return moving && carry(e, f, mid, false);
    }

    /** The hold is over: washed ashore to the nearest island edge in reach, or with none, let go. */
    private static void end(Ride r, Level level, Entity e, @Nullable Player player, boolean tell) {
        r.held = Math.max(r.held, VoidCurrent.HOLD_TICKS + 1);
        r.shore = shoreFor(level, e);
        if (tell && player != null) player.sendOverlayMessage(Component.translatable(r.shore != null ? "hint.promisedend.void_current.ashore" : "hint.promisedend.void_current.let_go"));
    }

    /** No fall damage, and for a player no kick for flying, while a current has them. */
    private static void safe(Entity e, @Nullable Player player) {
        e.resetFallDistance();
        if (player == null) return;
        player.resetFallDistance();
        if (player instanceof ServerPlayer sp) sp.connection.resetFlyingTicks();
    }

    /** Each server tick: everything in the End a current has or might take, in chunks that tick their entities. */
    public static void tick(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) return;
        for (Entity e : end.getAllEntities()) {
            if ((RIDES.containsKey(e.getUUID()) || VoidCurrent.inBand(e.getY())) && end.isPositionEntityTicking(e.blockPosition())) tick(end, RIDES, e);
        }
    }

    /**
     * The server's pass over one entity in {@code level}, with its rides in {@code rides} (the GameTests hand their
     * own of both: a far chunk's entities aren't in the level's list till it ticks them).
     */
    public static void tick(ServerLevel level, Map<UUID, Ride> rides, Entity e) {
        Player player = e instanceof Player p ? p : e.getControllingPassenger() instanceof Player p ? p : null;
        if (ride(rides, level, e, player, player == null, false, VoidCurrent.HOLD_MID)) e.needsSync = true;
    }
}

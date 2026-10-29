package com.panda.promisedend.logic;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.registry.ModTags;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.TrailParticleOption;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Ender Pearls show the way: in the End, out past the main island, a player holding one sees a stream of pearl-green
 * motes rise before them and drift toward where they go next. While the Ancients' Ward stands that is the place of the
 * first of its seals still binding it, where that seal's guardian keeps it: the nearest Shulker Foundry, then the
 * nearest Rift Maw, then the nearest Unfinished Citadel; after, the heart of the End, where the dragon waits. Only they
 * see it.
 */
public final class PearlGuide {
    /** Nearer the centre than this, the main island is in sight and the pearl shows nothing. */
    public static final int FROM = 160;
    /** How often a mote leaves, in ticks. */
    public static final int EVERY = 3;
    /** How far ahead of the eyes a mote starts. */
    public static final double AHEAD = 2.5;
    /** How far inward a mote drifts, in blocks, and in how many ticks. */
    public static final double REACH = 6;
    public static final int DRIFT = 30;
    /** One mote in seven leads: it drifts this far over this long, so the direction reads at a glance. */
    public static final int LEAD_EVERY = EVERY * 7;
    public static final double LEAD_REACH = 16;
    public static final int LEAD_DRIFT = 50;
    /**
     * An ender pearl's green: the trail motes that move to a point (vanilla's creaking trail) in a colour nothing else
     * of the End's uses, where white end-rod sparks were already the Waystation's posts and the Ward's wall.
     */
    public static final int COLOUR = 0xFF4CFFC0;
    /**
     * The Foundry is searched for this many chunks round (as an explorer map), again once a player is this far on, and
     * not more often than every {@link #RESEARCH_TICKS} (a pearl held through a run of throws).
     */
    public static final int SEARCH = 100, RESEARCH = 256, RESEARCH_TICKS = 200;
    /** This near the place it leads to, the pearl shows nothing: it is in sight. */
    public static final double THERE = 32;
    /** Where each seal is kept, in {@link AncientWard#SEALS}' order, as far as there are places a pearl can lead to. */
    private static final java.util.List<net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure>> KEPT =
            java.util.List.of(ModTags.ON_FOUNDRY_CHARTS, ModTags.ON_RIFT_MAW_CHARTS, ModTags.ON_CITADEL_CHARTS);

    /**
     * Each player's Foundry, and where and when they stood when it was found: a search loads chunks, so it isn't run
     * often. Kept per server, weakly: a world left in single player takes its leads with it.
     */
    private record Lead(int seal, BlockPos from, long when, @org.jspecify.annotations.Nullable BlockPos to) {}

    private static final Map<MinecraftServer, Map<UUID, Lead>> LEADS = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    private PearlGuide() {}

    /**
     * The seal a held pearl leads to, in {@link AncientWard#SEALS}' order: the first that still binds the standing Ward
     * and is kept in a place a pearl can find; -1 when there is none (the Ward is down, or only seals with no such place
     * are left).
     */
    public static int seal(MinecraftServer server) {
        if (!AncientWard.stands(server)) return -1;
        int broken = AncientWard.broken(server), guarded = AncientWard.guarded();
        for (int i = 0; i < KEPT.size(); i++) if ((guarded & 1 << i) != 0 && (broken & 1 << i) == 0) return i;
        return -1;
    }

    /**
     * Where a held pearl leads {@code player}: the nearest place of {@link #seal}'s seal (null when none is within
     * {@link #SEARCH} chunks), the centre (0, 0) when no seal is left to lead to. Public for the tests.
     */
    public static @org.jspecify.annotations.Nullable BlockPos lead(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        MinecraftServer server = level.getServer();
        int seal = seal(server);
        if (seal < 0) return BlockPos.ZERO;
        Map<UUID, Lead> leads = LEADS.computeIfAbsent(server, s -> new HashMap<>());
        Lead known = leads.get(player.getUUID());
        long now = level.getGameTime();
        if (known == null || known.seal() != seal
                || known.from().distSqr(player.blockPosition()) > (double) RESEARCH * RESEARCH && now - known.when() >= RESEARCH_TICKS) {
            BlockPos to = level.findNearestMapStructure(KEPT.get(seal), player.blockPosition(), SEARCH, false);
            known = new Lead(seal, player.blockPosition(), now, to == null ? null : centre(level, to, KEPT.get(seal)));
            leads.put(player.getUUID(), known);
        }
        return known.to();
    }

    /**
     * The middle of the place found at {@code found}: a search gives its chunk's corner, which by its turn can lie 28
     * blocks from a Foundry's dais. Its start is read from the chunk at its structure stage, before any terrain is made.
     */
    private static BlockPos centre(ServerLevel level, BlockPos found, net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> kind) {
        var chunk = level.getChunk(found.getX() >> 4, found.getZ() >> 4, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_STARTS, true);
        var structures = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        for (var start : chunk.getAllStarts().entrySet()) {
            if (start.getValue().isValid() && structures.wrapAsHolder(start.getKey()).is(kind)) {
                BlockPos mid = start.getValue().getBoundingBox().getCenter();
                return new BlockPos(mid.getX(), found.getY(), mid.getZ());
            }
        }
        return found;
    }

    /** The way from ({@code x}, {@code z}) to {@code to}: a unit vector on the ground's plane. */
    public static double[] way(double x, double z, BlockPos to) {
        return way(x - to.getX() - 0.5, z - to.getZ() - 0.5);
    }

    /** The way in from ({@code x}, {@code z}): a unit vector toward the centre on the ground's plane. */
    public static double[] way(double x, double z) {
        double d = Math.hypot(x, z);
        return d < 1e-6 ? new double[] {0, 0} : new double[] {-x / d, -z / d};
    }

    /**
     * Whether the player is shown the way: in the End, past {@link #FROM}, an Ender Pearl in either hand, with arrivals
     * by coordinates on (off, vanilla's End lands everyone at the centre).
     */
    public static boolean guides(ServerPlayer player) {
        if (!EndConfig.arrivalByCoordinates() || player.isSpectator() || !(player.level() instanceof ServerLevel level) || level.dimension() != Level.END) return false;
        if (Math.hypot(player.getX(), player.getZ()) < FROM) return false;
        return player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.ENDER_PEARL) || player.getItemInHand(InteractionHand.OFF_HAND).is(Items.ENDER_PEARL);
    }

    /** Each player's tick: every {@link #EVERY} ticks a mote before them, sent to them alone, that drifts on the way. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % EVERY != 0 || !guides(player)) return;
        ServerLevel level = (ServerLevel) player.level();
        BlockPos lead = lead(player);
        if (lead == null || (lead != BlockPos.ZERO && Math.hypot(player.getX() - lead.getX(), player.getZ() - lead.getZ()) < THERE)) return;
        double[] w = way(player.getX(), player.getZ(), lead);
        // a few steps ahead on the ground's plane (looking straight up or down, toward the way in), a little below the
        // eyes: seen whichever way they look, and never a blot over the view
        Vec3 look = player.getLookAngle();
        double lx = look.x, lz = look.z, flat = Math.hypot(lx, lz);
        if (flat < 0.3) {
            lx = w[0];
            lz = w[1];
        } else {
            lx /= flat;
            lz /= flat;
        }
        double x = player.getX() + lx * AHEAD + (level.getRandom().nextDouble() - 0.5) * 0.6;
        double y = player.getEyeY() - 0.6 + (level.getRandom().nextDouble() - 0.5) * 0.4;
        double z = player.getZ() + lz * AHEAD + (level.getRandom().nextDouble() - 0.5) * 0.6;
        boolean leads = player.tickCount % LEAD_EVERY == 0;
        double reach = leads ? LEAD_REACH : REACH;
        Vec3 to = new Vec3(x + w[0] * reach, y + (leads ? 0.6 : 0.3), z + w[1] * reach);
        level.sendParticles(player, new TrailParticleOption(to, COLOUR, leads ? LEAD_DRIFT : DRIFT), true, false, x, y, z, 1, 0, 0, 0, 0);
    }
}

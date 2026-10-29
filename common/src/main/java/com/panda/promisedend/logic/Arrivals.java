package com.panda.promisedend.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * An End portal's arrival: the ground nearest {@link Arrival#target}, found on the generator's own heights
 * (no chunks generated while looking), in one of our lands and level enough to build on; an Ancient Waystation built
 * there once (template {@code promisedend:waystation}, its floor's top at the ground); the player set down on its floor
 * facing the centre. Where no ground is within reach, an end-stone islet carries the Waystation instead. Each player
 * arriving the first time is given the Promise Codex.
 */
public final class Arrivals {
    public static final net.minecraft.resources.Identifier WAYSTATION = PromisedEnd.id("waystation");
    /** The template's size, its centre column, the floor's layer, and where on the floor the player is set down. */
    public static final int SIZE = 13, CENTRE = 6, FLOOR = 4, STAND = 3;
    /** The islet built where there is no ground: its top's height and its radius. */
    public static final int ISLET_Y = 64, ISLET_RADIUS = 6;

    private Arrivals() {}

    /** Where an entity entering an End portal at {@code portal} in {@code from} goes, or null to leave it to vanilla. */
    public static @Nullable TeleportTransition destination(ServerLevel from, @Nullable Entity entity, BlockPos portal) {
        ServerLevel end = from.getServer().getLevel(Level.END);
        if (end == null || from.dimension() == Level.END || !EndConfig.arrivalByCoordinates()) return null;
        BlockPos centre = waystation(end, Arrival.target(portal.getX() + 0.5, portal.getZ() + 0.5));
        Vec3 at = stand(centre);
        float yaw = Arrival.facingCentre(at.x, at.z);
        return new TeleportTransition(end, at, Vec3.ZERO, yaw, 0.0F, Relative.union(Relative.DELTA, Set.of(Relative.X_ROT)),
                TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET).then(Arrivals::greet));
    }

    /**
     * Where the player is set down at a Waystation whose floor's middle is {@code centre}: on its floor, {@link #STAND}
     * from the gate on its side away from the End's centre (along the nearer axis), so a player set down facing the
     * centre faces the gate and the way in beyond it.
     */
    public static Vec3 stand(BlockPos centre) {
        int x = centre.getX(), z = centre.getZ();
        int dx = Math.abs(x) >= Math.abs(z) ? (x < 0 ? -1 : 1) : 0, dz = dx == 0 ? (z < 0 ? -1 : 1) : 0;
        return Vec3.atBottomCenterOf(centre.offset(dx * STAND, 1, dz * STAND));
    }

    /** The gate of the Waystation whose standing place is {@code at}, or null. */
    public static @Nullable BlockPos gateAt(MinecraftServer server, Vec3 at) {
        for (BlockPos gate : built(server)) {
            if (stand(gate).distanceToSqr(at) < 0.01) return gate;
        }
        return null;
    }

    /**
     * The Waystation for a point: one already built for a point within {@link Arrival#REUSE} of it, or standing that
     * near it, else a new one on the ground nearest the point (or on a new islet). Returns the middle of its floor, the
     * gate's block.
     */
    public static BlockPos waystation(ServerLevel end, double[] point) {
        Stored stored = Stored.of(end.getServer());
        for (Station built : stored.waystations) {
            if (near(built.point(), point) || near(built.gate(), point)) return built.gate();
        }
        long t0 = System.nanoTime();
        BlockPos centre = ground(end, (int) Math.floor(point[0]), (int) Math.floor(point[1]));
        if (centre == null) centre = islet(end, (int) Math.floor(point[0]), (int) Math.floor(point[1]));
        long t1 = System.nanoTime();
        build(end, centre);
        stored.waystations.add(new Station(BlockPos.containing(point[0], 0, point[1]), centre));
        stored.setDirty();
        PromisedEnd.LOGGER.info("[promisedend] built an Ancient Waystation at {} for an arrival at {}, {} (found in {} ms, built in {} ms)", centre,
                Math.round(point[0]), Math.round(point[1]), (t1 - t0) / 1_000_000, (System.nanoTime() - t1) / 1_000_000);
        return centre;
    }

    /**
     * The nearest column round ({@code x}, {@code z}) in one of our biomes (any, with our terrain off) whose ground (the
     * generator's height, no features) has the ground three blocks off each way within two of it: the floor's middle at
     * its top, or null.
     */
    public static @Nullable BlockPos ground(ServerLevel end, int x, int z) {
        var generator = end.getChunkSource().getGenerator();
        var random = end.getChunkSource().randomState();
        int floor = end.getMinY() + 1;
        boolean ours = EndConfig.terrainEnabled();
        for (int[] o : Arrival.ORDER) {
            int cx = x + o[0], cz = z + o[1];
            int h = generator.getBaseHeight(cx, cz, Heightmap.Types.WORLD_SURFACE_WG, end, random);
            if (h <= floor) continue;
            if (ours && !oursAt(end, cx, h - 1, cz)) continue;
            boolean level = true;
            for (int[] d : new int[][] {{3, 0}, {-3, 0}, {0, 3}, {0, -3}}) {
                int n = generator.getBaseHeight(cx + d[0], cz + d[1], Heightmap.Types.WORLD_SURFACE_WG, end, random);
                if (n <= floor || Math.abs(n - h) > 2) {
                    level = false;
                    break;
                }
            }
            if (level && clearOfStructures(end, cx, cz)) return new BlockPos(cx, h - 1, cz);
        }
        return null;
    }

    /**
     * Whether no structure (an End City, a Foundry, a Maw) reaches the chunks round a column, so the Waystation, which
     * clears the ground over its floor, cuts into none. Generates those chunks only to their structure references.
     */
    private static boolean clearOfStructures(ServerLevel end, int x, int z) {
        for (int cx = (x >> 4) - 1; cx <= (x >> 4) + 1; cx++) {
            for (int cz = (z >> 4) - 1; cz <= (z >> 4) + 1; cz++) {
                var chunk = end.getChunk(cx, cz, net.minecraft.world.level.chunk.status.ChunkStatus.STRUCTURE_REFERENCES, true);
                if (chunk.getAllReferences().values().stream().anyMatch(refs -> !refs.isEmpty())) return false;
            }
        }
        return true;
    }

    /**
     * Whether the floor's middle and its four corners are in one of our biomes, as the game reports them (zoomed, so
     * near a border a block's biome isn't its noise cell's); read from the noise where the chunk isn't loaded.
     */
    private static boolean oursAt(ServerLevel end, int x, int y, int z) {
        for (int[] d : new int[][] {{0, 0}, {4, 4}, {4, -4}, {-4, 4}, {-4, -4}}) {
            if (!end.getBiome(new BlockPos(x + d[0], y, z + d[1])).unwrapKey().map(k -> k.identifier().getNamespace().equals(PromisedEnd.MODID)).orElse(false)) return false;
        }
        return true;
    }

    /** An end-stone islet round ({@code x}, {@code z}) where there is no ground: a lens whose top is {@link #ISLET_Y}. */
    private static BlockPos islet(ServerLevel end, int x, int z) {
        for (int dy = 0; dy < 4; dy++) {
            int r = ISLET_RADIUS - dy;
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dz * dz <= r * r) end.setBlock(new BlockPos(x + dx, ISLET_Y - dy, z + dz), Blocks.END_STONE.defaultBlockState(), 2);
                }
            }
        }
        return new BlockPos(x, ISLET_Y, z);
    }

    /** The Waystation's template set with its floor's middle at {@code centre}. */
    private static void build(ServerLevel end, BlockPos centre) {
        var template = end.getServer().getStructureTemplateManager().get(WAYSTATION);
        if (template.isEmpty()) {
            PromisedEnd.LOGGER.error("[promisedend] no template {}: the arrival has no Waystation", WAYSTATION);
            end.setBlock(centre, com.panda.promisedend.registry.ModBlocks.RETURN_GATE.get().defaultBlockState(), 2);
            return;
        }
        BlockPos origin = centre.offset(-CENTRE, -FLOOR, -CENTRE);
        template.get().placeInWorld(end, origin, origin, new StructurePlaceSettings(), end.getRandom(), 2);
    }

    /** A player's first arrival: the Promise Codex, and a line on what the Waystation is. */
    private static void greet(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        Stored stored = Stored.of(player.level().getServer());
        if (!stored.greeted.add(player.getUUID())) return;
        stored.setDirty();
        ItemStack codex = new ItemStack(ModItems.PROMISE_CODEX.get());
        player.getInventory().placeItemBackInInventory(codex, net.minecraft.util.Prediction.SERVER_ONLY);
        greeting(player).forEach(player::sendSystemMessage);
        title(player);
    }

    /**
     * What a first arrival is told: what the Waystation is; which way in and how far (to the Ward while it stands, its
     * wall out of sight hundreds of blocks off; to the heart of the End while it is down); and that a pearl in hand
     * shows the way.
     */
    public static List<Component> greeting(Entity player) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("arrival.promisedend.first"));
        double out = Math.hypot(player.getX(), player.getZ());
        Component way = Component.translatable("direction.promisedend." + Arrival.compass(-player.getX(), -player.getZ()));
        if (AncientWard.stands(player.level().getServer())) {
            lines.add(Component.translatable("arrival.promisedend.ward", (int) Math.round(out - EndConfig.wardRadius()), way));
        } else {
            lines.add(Component.translatable("arrival.promisedend.heart", (int) Math.round(out / 10) * 10, way));
        }
        lines.add(Component.translatable("arrival.promisedend.pearl"));
        return lines;
    }

    /** The first arrival's title, for a player who never reads chat, and a low chime under the portal's sound. */
    private static void title(ServerPlayer player) {
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(10, 70, 20));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(Component.translatable("arrival.promisedend.subtitle")));
        player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.translatable("arrival.promisedend.title")));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE,
                net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    private static boolean near(BlockPos at, double[] point) {
        double dx = at.getX() - point[0], dz = at.getZ() - point[1];
        return dx * dx + dz * dz <= (double) Arrival.REUSE * Arrival.REUSE;
    }

    /** The Waystations built so far, their gates (for tests and the server check). */
    public static List<BlockPos> built(MinecraftServer server) {
        return Stored.of(server).waystations.stream().map(Station::gate).toList();
    }

    /** A Waystation: the point it was asked for (y 0) and its gate, which may stand up to the search's reach off it. */
    private record Station(BlockPos point, BlockPos gate) {
        static final Codec<Station> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("point").forGetter(Station::point), BlockPos.CODEC.fieldOf("gate").forGetter(Station::gate)).apply(i, Station::new));
    }

    /** Forgets that a player has arrived before (tests). */
    public static void forget(MinecraftServer server, UUID player) {
        Stored stored = Stored.of(server);
        if (stored.greeted.remove(player)) stored.setDirty();
    }

    /** The Waystations built and the players already given a Codex. */
    private static final class Stored extends SavedData {
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
                Station.CODEC.listOf().optionalFieldOf("waystations", List.of()).forGetter(s -> s.waystations),
                UUIDUtil.CODEC.listOf().optionalFieldOf("greeted", List.of()).forGetter(s -> List.copyOf(s.greeted))).apply(i, Stored::new));
        static final SavedDataType<Stored> TYPE = new SavedDataType<>(PromisedEnd.id("arrivals"), Stored::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

        final List<Station> waystations = new ArrayList<>();
        final java.util.Set<UUID> greeted = new java.util.HashSet<>();

        Stored() {}

        Stored(List<Station> waystations, List<UUID> greeted) {
            this.waystations.addAll(waystations);
            this.greeted.addAll(greeted);
        }

        static Stored of(MinecraftServer server) {
            return server.getDataStorage().computeIfAbsent(TYPE);
        }
    }
}

package com.panda.promisedend;

import com.panda.promisedend.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;

/**
 * A development check of the End on a dedicated server: with {@code -Dpromisedend.servercheck=true} (Gradle
 * {@code -PendServerCheck}), once the server has started it generates the End around (300, 0), checks the biome there
 * (the Shattered Rim, or with {@code terrain.enabled} off the End's own) and the Voidstone the Rim yields, and logs
 * {@code [promisedend servercheck] PASS} or {@code FAIL}. Booting to "Done" alone never generates an End chunk, so it
 * can't show that the worldgen runs without a client. For the same reason it also makes and ticks a Crystal Warden
 * there, strips chorus timber with an axe, and generates the nearest of each outer biome built so far.
 */
public final class ServerCheck {
    public static final String PROPERTY = "promisedend.servercheck";
    private static final int RADIUS = 64;

    private ServerCheck() {}

    public static void onServerStarted(MinecraftServer server) {
        if (!Boolean.getBoolean(PROPERTY) || !server.isDedicatedServer()) return;
        String failure = null;
        try {
            failure = check(server);
        } catch (RuntimeException e) {
            PromisedEnd.LOGGER.error("[promisedend servercheck] threw", e);
            failure = e.toString();
        }
        if (failure == null) PromisedEnd.LOGGER.info("[promisedend servercheck] PASS");
        else PromisedEnd.LOGGER.error("[promisedend servercheck] FAIL: {}", failure);
    }

    private static String check(MinecraftServer server) {
        var end = server.getLevel(Level.END);
        if (end == null) return "no End level";
        BlockPos centre = new BlockPos(300, 64, 0);
        for (int cx = (centre.getX() - RADIUS) >> 4; cx <= (centre.getX() + RADIUS) >> 4; cx++) {
            for (int cz = (centre.getZ() - RADIUS) >> 4; cz <= (centre.getZ() + RADIUS) >> 4; cz++) end.getChunk(cx, cz);
        }
        var biome = end.getBiome(centre);
        String id = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("?");
        PromisedEnd.LOGGER.info("[promisedend servercheck] biome at {}: {}", centre, id);
        boolean terrain = EndConfig.terrainEnabled();
        PromisedEnd.LOGGER.info("[promisedend servercheck] terrain {}", terrain ? "on" : "off");
        if (terrain && !id.equals("promisedend:shattered_rim")) return "expected the Shattered Rim at x=300, got " + id;
        if (!terrain && id.startsWith(PromisedEnd.MODID + ":")) return "terrain is off, yet x=300 is " + id;
        if (!biome.is(TagKey.create(Registries.BIOME, Identifier.parse("c:is_end")))) return id + " isn't in #c:is_end";
        int voidstone = 0;
        var block = ModBlocks.VOIDSTONE_SET.base().get();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int z = -RADIUS; z <= RADIUS; z++) {
                for (int y = 40; y <= 90; y++) {
                    if (end.getBlockState(new BlockPos(centre.getX() + x, y, centre.getZ() + z)).is(block)) voidstone++;
                }
            }
        }
        PromisedEnd.LOGGER.info("[promisedend servercheck] voidstone within {} of {}: {}", RADIUS, centre, voidstone);
        if (voidstone < 200) return "too little Voidstone around x=300: " + voidstone;
        String failure = construct(end, centre.above(26));
        if (failure == null) failure = strips(end, centre.above(40));
        if (failure == null && terrain) failure = biome(end, "chorus_thicket", "Chorus Thicket", ModBlocks.CHORUS_LOG.get(), 30);
        if (failure == null && terrain) failure = biome(end, "lumen_mire", "Lumen Mire", ModBlocks.VOID_WATER.get(), 20);
        if (failure == null && terrain) failure = biome(end, "resonant_spires", "Resonant Spires", ModBlocks.RESONITE_BLOCK.get(), 100);
        return failure != null ? failure : wrath(server);
    }

    /**
     * The axe's block transformer strips chorus timber on this loader: NeoForge appends it from a data map, Fabric
     * registers it in code. There is no player on a dedicated server, so the axe is used from the off hand, which
     * vanilla's transformer allows without one.
     */
    private static String strips(net.minecraft.server.level.ServerLevel end, BlockPos at) {
        var axis = net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
        var axe = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_AXE);
        var transformer = axe.get(net.minecraft.core.component.DataComponents.BLOCK_TRANSFORMER);
        if (transformer == null) return "an iron axe has no block transformer";
        var pairs = java.util.List.of(java.util.List.of(ModBlocks.CHORUS_LOG.get(), ModBlocks.STRIPPED_CHORUS_LOG.get()),
                java.util.List.of(ModBlocks.CHORUS_WOOD.get(), ModBlocks.STRIPPED_CHORUS_WOOD.get()));
        for (var pair : pairs) {
            end.setBlock(at, pair.get(0).defaultBlockState().setValue(axis, net.minecraft.core.Direction.Axis.Z), 3);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), net.minecraft.core.Direction.UP, at, false);
            var context = new net.minecraft.world.item.context.UseOnContext(end, null, net.minecraft.world.InteractionHand.OFF_HAND, axe, hit) {};
            transformer.value().transformBlock(context);
            var now = end.getBlockState(at);
            end.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            if (!now.is(pair.get(1)) || now.getValue(axis) != net.minecraft.core.Direction.Axis.Z) return "an axe on " + pair.get(0) + " left " + now;
        }
        PromisedEnd.LOGGER.info("[promisedend servercheck] an axe strips the chorus log and wood");
        return null;
    }

    /**
     * The nearest of one of our biomes, generated: its features are built without a client, and at least
     * {@code minimum} of {@code block} (what only that biome's features make) lie within 32 blocks.
     */
    private static String biome(net.minecraft.server.level.ServerLevel end, String id, String name, net.minecraft.world.level.block.Block block, int minimum) {
        var key = net.minecraft.resources.ResourceKey.create(Registries.BIOME, PromisedEnd.id(id));
        var found = end.findClosestBiome3d(h -> h.is(key), new BlockPos(0, 64, 0), 6400, 32, 64);
        if (found == null) return "no " + name + " within 6400 blocks";
        BlockPos at = interior(end, key, found.getFirst());
        for (int cx = (at.getX() - 40) >> 4; cx <= (at.getX() + 40) >> 4; cx++) {
            for (int cz = (at.getZ() - 40) >> 4; cz <= (at.getZ() + 40) >> 4; cz++) end.getChunk(cx, cz);
        }
        int count = 0;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-32, -40, -32), at.offset(32, 40, 32))) {
            if (end.getBlockState(p).is(block)) count++;
        }
        String what = block.getName().getString();
        PromisedEnd.LOGGER.info("[promisedend servercheck] {} at {}: {} {} within 32 blocks", name, at, count, what);
        return count >= minimum ? null : "only " + count + " " + what + " in the " + name + " at " + at;
    }

    /**
     * Where to look at a biome found by its nearest point: that point is on the biome's edge, so the middle of the
     * biome round it instead. The biome is sampled every 8 blocks within 96 of {@code hit} (by the biome source, no
     * chunks generated); the result is the matching sample nearest their centroid, at y 64.
     */
    public static BlockPos interior(net.minecraft.server.level.ServerLevel level, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> key, BlockPos hit) {
        java.util.List<BlockPos> in = new java.util.ArrayList<>();
        for (int dx = -96; dx <= 96; dx += 8) {
            for (int dz = -96; dz <= 96; dz += 8) {
                int x = hit.getX() + dx, z = hit.getZ() + dz;
                if (level.getUncachedNoiseBiome(x >> 2, 16, z >> 2).is(key)) in.add(new BlockPos(x, 64, z));
            }
        }
        if (in.isEmpty()) return hit;
        double cx = in.stream().mapToInt(BlockPos::getX).average().orElseThrow(), cz = in.stream().mapToInt(BlockPos::getZ).average().orElseThrow();
        return in.stream().min(java.util.Comparator.comparingDouble(p -> Math.hypot(p.getX() - cx, p.getZ() - cz))).orElseThrow();
    }

    /**
     * The anvil's mixin applies on this loader's server (it runs when the class loads, and a target it can't find fails
     * here), and the world's Wrath goes through its saved data and back.
     */
    private static String wrath(MinecraftServer server) {
        try {
            Class.forName("net.minecraft.world.inventory.AnvilMenu");
        } catch (ClassNotFoundException | Error e) {
            return "the anvil didn't load: " + e;
        }
        int before = com.panda.promisedend.logic.DragonWrath.stored(server);
        com.panda.promisedend.logic.DragonWrath.setStored(server, 3);
        int read = com.panda.promisedend.logic.DragonWrath.stored(server);
        com.panda.promisedend.logic.DragonWrath.setStored(server, before);
        PromisedEnd.LOGGER.info("[promisedend servercheck] anvil loaded; Wrath stored {}, set to 3 read {}", before, read);
        return read == 3 ? null : "Wrath set to 3 read back " + read;
    }

    /** A Crystal Warden made, added and ticked on the server: its GeckoLib side and AI must load without a client. */
    private static String construct(net.minecraft.server.level.ServerLevel end, BlockPos at) {
        var warden = com.panda.promisedend.registry.ModEntities.CRYSTAL_WARDEN.get().create(end, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (warden == null) return "couldn't create a Crystal Warden";
        warden.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        end.addFreshEntity(warden);
        for (int i = 0; i < 40; i++) warden.tick();
        PromisedEnd.LOGGER.info("[promisedend servercheck] crystal warden ticked 40 times at {}: health {}, phase {}, y {}", at,
                warden.getHealth(), warden.phase(), String.format(java.util.Locale.ROOT, "%.2f", warden.getY()));
        String failure = null;
        if (warden.getMaxHealth() != 30.0F) failure = "the Crystal Warden has " + warden.getMaxHealth() + " max health";
        else if (warden.getY() < at.getY() - 1) failure = "the Crystal Warden fell: y " + warden.getY();
        warden.discard();
        return failure;
    }
}

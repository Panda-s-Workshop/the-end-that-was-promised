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
 * there, strips chorus timber with an axe, lifts an item over an Updraft Vent, generates the nearest of each outer
 * biome built so far, and ticks a vent the Skyfall Archipelago's worldgen made. With {@code -Dpromisedend.rimgap=true} as well (Gradle {@code -PendRimGap}) it then measures the
 * first crossing from the main island into the Rim ({@link #rimGap}), which only generated terrain can show.
 */
public final class ServerCheck {
    public static final String PROPERTY = "promisedend.servercheck", RIM_GAP = "promisedend.rimgap";
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
        if (failure == null && Boolean.getBoolean(RIM_GAP) && EndConfig.terrainEnabled()) {
            try {
                rimGap(server.getLevel(Level.END), server.getLevel(Level.END).getSeed());
            } catch (RuntimeException e) {
                PromisedEnd.LOGGER.error("[promisedend rimgap] threw", e);
                failure = e.toString();
            }
        }
        if (failure == null) PromisedEnd.LOGGER.info("[promisedend servercheck] PASS");
        else PromisedEnd.LOGGER.error("[promisedend servercheck] FAIL: {}", failure);
    }

    /**
     * The constructs our terrain-off fallback adds to vanilla's biomes are in those biomes' spawn lists with the
     * terrain off, and not with it on (vanilla's quarter keeps its own spawns).
     */
    private static String fallbackSpawns(net.minecraft.server.level.ServerLevel end) {
        var biomes = end.registryAccess().lookupOrThrow(Registries.BIOME);
        record Want(String biome, net.minecraft.world.entity.EntityType<?> type, net.minecraft.world.entity.MobCategory category) {}
        for (Want want : java.util.List.of(new Want("end_highlands", com.panda.promisedend.registry.ModEntities.CHORUS_WEAVER.get(), net.minecraft.world.entity.MobCategory.MONSTER),
                new Want("end_barrens", com.panda.promisedend.registry.ModEntities.RIFT_STALKER.get(), net.minecraft.world.entity.MobCategory.MONSTER),
                new Want("end_midlands", com.panda.promisedend.registry.ModEntities.LUMEN_WISP.get(), net.minecraft.world.entity.MobCategory.AMBIENT))) {
            var biome = biomes.getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.BIOME, Identifier.withDefaultNamespace(want.biome())));
            var spawns = biome.value().getAttributes().applyModifier(net.minecraft.world.attribute.EnvironmentAttributes.NATURAL_MOB_SPAWNS,
                    net.minecraft.world.level.biome.MobSpawnSettings.EMPTY);
            // a category with nothing in it is no list at all
            var list = spawns.getMobsInCategory(want.category());
            boolean has = list != null && list.unwrap().stream().anyMatch(w -> w.value().type() == want.type());
            boolean wanted = !EndConfig.terrainEnabled();
            PromisedEnd.LOGGER.info("[promisedend servercheck] {} spawns {}: {} (terrain {})", want.biome(), net.minecraft.world.entity.EntityType.getKey(want.type()), has, wanted ? "off" : "on");
            if (has != wanted) return want.biome() + (has ? " spawns " : " doesn't spawn ") + net.minecraft.world.entity.EntityType.getKey(want.type())
                    + " with the terrain " + (wanted ? "off" : "on");
        }
        return null;
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
        // every check runs, so one run shows all of a world's numbers; the failures are reported together
        java.util.List<String> failures = new java.util.ArrayList<>();
        java.util.function.Consumer<String> add = f -> { if (f != null) failures.add(f); };
        add.accept(construct(end, centre.above(26)));
        add.accept(fallbackSpawns(end));
        add.accept(strips(end, centre.above(40)));
        add.accept(updraft(end, centre.above(50)));
        if (terrain) add.accept(biome(end, "chorus_thicket", "Chorus Thicket", ModBlocks.CHORUS_LOG.get(), 30));
        if (terrain) add.accept(biome(end, "lumen_mire", "Lumen Mire", ModBlocks.VOID_WATER.get(), 60));
        if (terrain) add.accept(biome(end, "resonant_spires", "Resonant Spires", ModBlocks.RESONITE_BLOCK.get(), 100));
        if (terrain) add.accept(biome(end, "skyfall_archipelago", "Skyfall Archipelago", ModBlocks.DRIFT_BLOSSOM.get(), 15));
        if (terrain) add.accept(generatedVent(end));
        if (terrain) add.accept(gatewayLandings(end));
        // the scars are few and the Voidscar's islands scattered: counted within 64 (seeds 1-16: 45-270, a third of the
        // fewest the bar)
        if (terrain) add.accept(biome(end, net.minecraft.resources.ResourceKey.create(Registries.BIOME, PromisedEnd.id("voidscar_wastes")), "Voidscar Wastes",
                ModBlocks.VOIDSTEEL_SCRAP_ORE.get(), 15, 64));
        if (terrain) add.accept(biome(end, net.minecraft.resources.ResourceKey.create(Registries.BIOME, PromisedEnd.id("voidscar_wastes")), "Voidscar Wastes' edges",
                ModBlocks.CRUMBLING_VOIDSTONE.get(), 20, 64));
        // seeds 1-16 on the real heightmap: the Skyfall 100 % on every one, the Voidscar 79-100 % (98 % pooled)
        if (terrain) add.accept(shores(end, "voidscar_wastes", 0.7));
        if (terrain) add.accept(voidscarSpawns(end));
        if (terrain) add.accept(shores(end, "skyfall_archipelago", 0.9));
        if (terrain) add.accept(biome(end, "shulker_reefs", "Shulker Reefs", ModBlocks.ANCIENT_PURPUR_SET.base().get(), 150));
        if (terrain) add.accept(biome(end, "hollow_highlands", "Hollow Highlands", ModBlocks.ECHO_AMBER_CLUSTER.get(), 5));
        if (terrain) add.accept(biome(end, "bloom_meadows", "Bloom Meadows", ModBlocks.ENDER_BLOOM.get(), 15));
        if (terrain) add.accept(biome(end, "chorus_canopy", "Chorus Canopy", ModBlocks.CHORUS_LEAVES.get(), 2000));
        if (terrain) add.accept(biome(end, "void_fungus_barrens", "Void Fungus Barrens", ModBlocks.VOID_MUSHROOM_BLOCK.get(), 50));
        if (terrain) add.accept(biome(end, "hanging_gardens", "Hanging Gardens", ModBlocks.VOID_BERRY_VINE.get(), 250));
        // terrain off: the fallbacks on vanilla's biomes, the Thicket's groves and the Spires' formations on its highlands, the Mire's pools on its midlands
        if (!terrain) add.accept(biome(end, net.minecraft.world.level.biome.Biomes.END_HIGHLANDS, "highlands' fallback groves", ModBlocks.CHORUS_LOG.get(), 10, 64));
        if (!terrain) add.accept(biome(end, net.minecraft.world.level.biome.Biomes.END_MIDLANDS, "midlands' fallback pools", ModBlocks.VOID_WATER.get(), 8, 64));
        if (!terrain) add.accept(biome(end, net.minecraft.world.level.biome.Biomes.END_HIGHLANDS, "highlands' fallback spires", ModBlocks.RESONITE_BLOCK.get(), 50, 64));
        // and the Skyfall's blossoms on its small islands
        if (!terrain) add.accept(biome(end, net.minecraft.world.level.biome.Biomes.SMALL_END_ISLANDS, "small islands' fallback blossoms", ModBlocks.DRIFT_BLOSSOM.get(), 3, 64));
        // and the Voidscar's scars on its barrens
        if (!terrain) add.accept(biome(end, net.minecraft.world.level.biome.Biomes.END_BARRENS, "barrens' fallback scars", ModBlocks.VOIDSTEEL_SCRAP_ORE.get(), 3, 64));
        // and the Reefs' growths on its midlands
        if (!terrain) add.accept(biome(end, net.minecraft.world.level.biome.Biomes.END_MIDLANDS, "midlands' fallback reefs", ModBlocks.ANCIENT_PURPUR_SET.base().get(), 20, 64));
        // and the Hollow Highlands' caverns in its highlands
        if (!terrain) add.accept(biome(end, net.minecraft.world.level.biome.Biomes.END_HIGHLANDS, "highlands' fallback caverns", ModBlocks.ECHO_AMBER_CLUSTER.get(), 3, 64));
        // and Bloom Meadows' flowers on its midlands
        if (!terrain) add.accept(biome(end, net.minecraft.world.level.biome.Biomes.END_MIDLANDS, "midlands' fallback blooms", ModBlocks.ENDER_BLOOM.get(), 3, 64));
        // and the rest of the Wilds: the Canopy's trees on its highlands, the Barrens' mycelium, the Gardens' vines; each at least about a
        // quarter of the fewest seen (on either loader), so a fall in density shows
        if (!terrain) add.accept(biome(end, net.minecraft.world.level.biome.Biomes.END_HIGHLANDS, "highlands' fallback canopy", ModBlocks.CHORUS_LEAVES.get(), 300, 64));
        if (!terrain) add.accept(biome(end, net.minecraft.world.level.biome.Biomes.END_BARRENS, "barrens' fallback mycelium", ModBlocks.VOID_MYCELIUM.get(), 200, 64));
        if (!terrain) add.accept(biome(end, net.minecraft.world.level.biome.Biomes.SMALL_END_ISLANDS, "small islands' fallback vines", ModBlocks.VOID_BERRY_VINE.get(), 130, 64));
        add.accept(wrath(server));
        add.accept(arrivals(server, end, terrain));
        add.accept(foundry(end));
        add.accept(maw(end));
        add.accept(shrine(end));
        add.accept(observatory(end));
        add.accept(driftRoutes(end));
        return failures.isEmpty() ? null : String.join(" | ", failures);
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
        // 40 strips shed resin (one in two: 8 or fewer by chance about 1 run in 11000)
        var box = new net.minecraft.world.phys.AABB(at).inflate(3);
        for (var e : end.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) e.discard();
        for (int i = 0; i < 40; i++) {
            end.setBlock(at, ModBlocks.CHORUS_LOG.get().defaultBlockState(), 3);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), net.minecraft.core.Direction.UP, at, false);
            transformer.value().transformBlock(new net.minecraft.world.item.context.UseOnContext(end, null, net.minecraft.world.InteractionHand.OFF_HAND, axe, hit) {});
        }
        end.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        int resin = 0;
        for (var e : end.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            if (e.getItem().is(com.panda.promisedend.registry.ModItems.CHORUS_RESIN.get())) resin += e.getItem().getCount();
            e.discard();
        }
        PromisedEnd.LOGGER.info("[promisedend servercheck] an axe strips the chorus log and wood; 40 strips shed {} Chorus Resin", resin);
        if (resin <= 8) return "40 strips shed only " + resin + " Chorus Resin";
        return null;
    }

    /**
     * The nearest of one of our biomes, generated: its features are built without a client, and at least
     * {@code minimum} of {@code block} (what only that biome's features make) lie within 32 blocks.
     */
    private static String biome(net.minecraft.server.level.ServerLevel end, String id, String name, net.minecraft.world.level.block.Block block, int minimum) {
        return biome(end, net.minecraft.resources.ResourceKey.create(Registries.BIOME, PromisedEnd.id(id)), name, block, minimum, 32);
    }

    /** As above within {@code radius} blocks: a sparse fallback wants a wider look than a biome of ours. */
    private static String biome(net.minecraft.server.level.ServerLevel end, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> key,
                                String name, net.minecraft.world.level.block.Block block, int minimum, int radius) {
        // the best few places, in turn, but only past one a structure claims: an End City standing in a biome's middle
        // claims its chunks, and its features keep out of them (world seed -8559988421388743695: the Spires' best place
        // had no spire within 32)
        java.util.List<BlockPos> places = spots(end, key, radius);
        if (places.isEmpty()) return "no " + name + " within 6400 blocks";
        String what = block.getName().getString(), failure = null;
        for (BlockPos at : places.subList(0, Math.min(3, places.size()))) {
            int claimed = 0;
            for (int cx = (at.getX() - radius - 8) >> 4; cx <= (at.getX() + radius + 8) >> 4; cx++) {
                for (int cz = (at.getZ() - radius - 8) >> 4; cz <= (at.getZ() + radius + 8) >> 4; cz++) {
                    if (end.getChunk(cx, cz).getAllReferences().values().stream().anyMatch(r -> !r.isEmpty())) claimed++;
                }
            }
            int count = 0;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-radius, -40, -radius), at.offset(radius, 40, radius))) {
                if (end.getBlockState(p).is(block)) count++;
            }
            PromisedEnd.LOGGER.info("[promisedend servercheck] {} at {}: {} {} within {} blocks ({} chunks round it a structure claims)", name, at, count, what, radius, claimed);
            if (count >= minimum) return null;
            if (failure == null) failure = "only " + count + " " + what + " in the " + name + " at " + at;
            // the next place only stands in for one a structure took: too few where nothing is claimed is a failure
            if (claimed == 0) break;
        }
        return failure;
    }

    /**
     * Natural spawning's own draw and checks at the Voidscar's place, for monsters: a column in a chunk and a height up to
     * its surface, as the game picks them, then whether an enderman or a Rift Stalker could stand there (an empty spot,
     * its placement, its spawn rule, dark enough) and what the generator offers there. Logged on each loader, so a loader
     * that spawns fewer there shows which step turns them away.
     */
    private static String voidscarSpawns(net.minecraft.server.level.ServerLevel end) {
        BlockPos at = spot(end, net.minecraft.resources.ResourceKey.create(Registries.BIOME, PromisedEnd.id("voidscar_wastes")), 64);
        if (at == null) return null;
        var random = net.minecraft.util.RandomSource.create(42);
        var types = java.util.List.<net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob>>of(
                net.minecraft.world.entity.EntityTypes.ENDERMAN, com.panda.promisedend.registry.ModEntities.RIFT_STALKER.get());
        int tries = 0, empty = 0, offered = 0;
        int[] placed = new int[types.size()], ruled = new int[types.size()];
        java.util.Map<String, Integer> offers = new java.util.TreeMap<>();
        for (int cx = (at.getX() >> 4) - 4; cx <= (at.getX() >> 4) + 4; cx++) {
            for (int cz = (at.getZ() >> 4) - 4; cz <= (at.getZ() >> 4) + 4; cz++) {
                var chunk = end.getChunk(cx, cz);
                for (int i = 0; i < 40; i++) {
                    int x = (cx << 4) + random.nextInt(16), z = (cz << 4) + random.nextInt(16);
                    int top = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z) + 1;
                    int y = net.minecraft.util.Mth.randomBetweenInclusive(random, end.getMinY(), top);
                    BlockPos pos = new BlockPos(x, y, z);
                    tries++;
                    if (!end.getBlockState(pos).isAir() || !end.getBlockState(pos.below()).isSolid()) continue;
                    empty++;
                    var mobs = end.getChunkSource().getGenerator().getMobsAt(end, end.structureManager(), net.minecraft.world.entity.MobCategory.MONSTER, pos);
                    if (!mobs.isEmpty()) offered++;
                    for (var w : mobs.unwrap()) offers.merge(net.minecraft.world.entity.EntityType.getKey(w.value().type()).getPath(), w.weight(), (o, n) -> o);
                    for (int t = 0; t < types.size(); t++) {
                        if (!net.minecraft.world.entity.SpawnPlacements.isSpawnPositionOk(types.get(t), end, pos)) continue;
                        placed[t]++;
                        if (net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(types.get(t), end, net.minecraft.world.entity.EntitySpawnReason.NATURAL, pos, random)) ruled[t]++;
                    }
                }
            }
        }
        PromisedEnd.LOGGER.info("[promisedend servercheck] monster spawning at the Voidscar's {}: {} tries, {} on ground, {} offered monsters {}; enderman placed {} allowed {}; stalker placed {} allowed {}",
                at, tries, empty, offered, offers, placed[0], ruled[0], placed[1], ruled[1]);
        return null;
    }

    /**
     * A place in the biome to count at: the middle ({@link #interior}) of the biome nearest the centre and of the biome
     * nearest a point 1500 and 3000 out in each of eight directions, whichever holds the most land in the biome in the
     * box round it. The first with land on a quarter of the box was once taken, but on some seeds that was a sliver of
     * the Lumen Mire, too little for its pools to be sure of (world seed 5: 27 of 81 samples, 21 Void-water); and the four
     * axis points alone left the harness's seed with a Voidscar a fifth of the box, too few chunks for its scars to be
     * sure of (25 of 121 samples, no scar). Null when there is no such biome within 6400 blocks.
     */
    public static BlockPos spot(net.minecraft.server.level.ServerLevel end, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> key, int radius) {
        var places = spots(end, key, radius);
        return places.isEmpty() ? null : places.getFirst();
    }

    /** Every place {@link #spot} weighs, the most land first. */
    public static java.util.List<BlockPos> spots(net.minecraft.server.level.ServerLevel end, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> key, int radius) {
        java.util.List<java.util.Map.Entry<BlockPos, Integer>> weighed = new java.util.ArrayList<>();
        java.util.List<BlockPos> froms = new java.util.ArrayList<>(java.util.List.of(new BlockPos(0, 64, 0)));
        for (int r : new int[] {1500, 3000}) {
            for (int deg = 0; deg < 360; deg += 45) {
                froms.add(new BlockPos((int) Math.round(r * Math.cos(Math.toRadians(deg))), 64, (int) Math.round(r * Math.sin(Math.toRadians(deg)))));
            }
        }
        java.util.Set<BlockPos> seen = new java.util.HashSet<>();
        for (BlockPos from : froms) {
            var found = end.findClosestBiome3d(h -> h.is(key), from, 6400, 32, 64);
            if (found == null) continue;
            BlockPos at = interior(end, key, found.getFirst());
            if (!seen.add(at)) continue;
            int land = 0, samples = 0;
            for (int dx = -radius; dx <= radius; dx += 8) {
                for (int dz = -radius; dz <= radius; dz += 8) {
                    samples++;
                    if (land(end, key, at.getX() + dx, at.getZ() + dz)) land++;
                }
            }
            PromisedEnd.LOGGER.info("[promisedend servercheck] {} at {}: land in the biome at {} of {} samples", key.identifier(), at, land, samples);
            weighed.add(java.util.Map.entry(at, land));
        }
        // the most land first; the first found first among equals, as the single best place always was
        weighed.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        return weighed.stream().map(java.util.Map.Entry::getKey).toList();
    }

    /**
     * Of the void-current columns over a biome's middle (every 4 blocks within 128 of each of its best places, from
     * {@link #spots}, until 60 are counted), the share a ride's end there washes ashore from
     * ({@link com.panda.promisedend.logic.VoidCurrent#shore} at the hold's middle height): at least {@code minimum}. Heights are the generated chunks' heightmap, the one a ride reads in game: the Skyfall's
     * islets and the Voidscar's islands are built by features, which the generator's own heights leave out.
     */
    private static String shores(net.minecraft.server.level.ServerLevel end, String id, double minimum) {
        var key = net.minecraft.resources.ResourceKey.create(Registries.BIOME, PromisedEnd.id(id));
        var places = spots(end, key, 40);
        if (places.isEmpty()) return "no " + id + " to count shores in";
        long seed = com.panda.promisedend.logic.VoidCurrents.seed(end);
        java.util.function.IntBinaryOperator top = (x, z) -> end.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z);
        java.util.Set<Long> counted = new java.util.HashSet<>();
        int columns = 0, ashore = 0, used = 0;
        // the best places in turn until 60 columns are counted: one Voidscar strip can hold only a handful
        for (BlockPos at : places) {
            if (columns >= 60 || used == 4) break;
            used++;
            // the chunks generated, so the heights are the ones a ride reads in game, islets and islands from features too
            int reach = 128 + com.panda.promisedend.logic.VoidCurrent.SHORE_REACH + 1;
            for (int cx = (at.getX() - reach) >> 4; cx <= (at.getX() + reach) >> 4; cx++) {
                for (int cz = (at.getZ() - reach) >> 4; cz <= (at.getZ() + reach) >> 4; cz++) end.getChunk(cx, cz);
            }
            int here = 0, hereAshore = 0;
            for (int dx = -128; dx <= 128; dx += 4) {
                for (int dz = -128; dz <= 128; dz += 4) {
                    int x = at.getX() + dx, z = at.getZ() + dz;
                    if (!counted.add(net.minecraft.world.level.ChunkPos.pack(x, z))) continue;
                    if (!end.getUncachedNoiseBiome(x >> 2, com.panda.promisedend.logic.VoidCurrents.BIOME_Y >> 2, z >> 2).is(key)) continue;
                    if (!com.panda.promisedend.logic.VoidCurrent.inCurrent(seed, x, z)) continue;
                    here++;
                    if (com.panda.promisedend.logic.VoidCurrent.shore(x, (int) com.panda.promisedend.logic.VoidCurrent.HOLD_MID, z, top) != null) hereAshore++;
                }
            }
            columns += here;
            ashore += hereAshore;
            PromisedEnd.LOGGER.info("[promisedend servercheck] {} round {}: a shore in reach from {} of {} current columns", id, at, hereAshore, here);
        }
        PromisedEnd.LOGGER.info("[promisedend servercheck] {}, {} places: a shore in reach from {} of {} current columns ({})", id, used, ashore, columns,
                columns == 0 ? "-" : String.format(java.util.Locale.ROOT, "%.0f %%", 100.0 * ashore / columns));
        if (columns < 20) return "too few current columns in the " + id + " to count shores: " + columns;
        if (ashore < minimum * columns) return "a shore in reach from only " + ashore + " of " + columns + " current columns in the " + id;
        return null;
    }

    /** Whether the column at x, z is in the biome with land under it, asked of the biome source and the generator (no chunks). */
    private static boolean land(net.minecraft.server.level.ServerLevel level, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> key, int x, int z) {
        if (!level.getUncachedNoiseBiome(x >> 2, 16, z >> 2).is(key)) return false;
        int ground = level.getChunkSource().getGenerator().getBaseHeight(x, z, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG,
                level, level.getChunkSource().randomState());
        return ground > level.getMinY() + 1;
    }

    /**
     * Where to look at a biome found by its nearest point: that point is on the biome's edge, so the middle of the
     * biome round it instead. The biome is sampled every 8 blocks within 96 of {@code hit} (by the biome source, no
     * chunks generated), keeping the samples with land under them (the generator's base height, also without chunks:
     * a biome of the End runs on over the void, and a middle there has nothing to count); the result is the kept sample
     * nearest their centroid, at y 64, or {@code hit} if none is kept.
     */
    /** How far out the crossing is measured, and the distance past which an island counts as the Rim proper. */
    private static final int GAP_REACH = 240, GAP_OUT = 200;

    /**
     * The first crossing from the main island into the Rim, on this world's generated terrain: every column within 240
     * blocks of the origin that has a block in it, seen from above, grouped into islands (columns touching at a side or
     * a corner). Logged: how far the main island reaches; the shortest gap of open air from it to another island, and
     * the shortest into each eighth of the ring; and the longest single gap on the easiest way out to an island 200 or
     * more from the origin (islands reached one by one, each by its shortest gap from one already reached). Gaps are
     * across, not up: islands sit at Y 50-80.
     */
    private static void rimGap(net.minecraft.server.level.ServerLevel end, long seed) {
        int n = GAP_REACH * 2 + 1;
        for (int cx = -GAP_REACH >> 4; cx <= GAP_REACH >> 4; cx++) {
            for (int cz = -GAP_REACH >> 4; cz <= GAP_REACH >> 4; cz++) end.getChunk(cx, cz);
        }
        boolean[] solid = new boolean[n * n];
        for (int x = -GAP_REACH; x <= GAP_REACH; x++) {
            for (int z = -GAP_REACH; z <= GAP_REACH; z++) {
                if (x * x + z * z > GAP_REACH * GAP_REACH) continue;
                solid[(x + GAP_REACH) * n + z + GAP_REACH] = end.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z) > end.getMinY();
            }
        }
        // islands: flood fill; for each, its edge columns, its farthest reach from the origin and its middle
        int[] island = new int[n * n];
        java.util.Arrays.fill(island, -1);
        java.util.List<int[]> edges = new java.util.ArrayList<>();
        java.util.List<double[]> shape = new java.util.ArrayList<>();
        for (int start = 0; start < n * n; start++) {
            if (!solid[start] || island[start] >= 0) continue;
            int id = edges.size();
            it.unimi.dsi.fastutil.ints.IntArrayList cells = new it.unimi.dsi.fastutil.ints.IntArrayList(), edge = new it.unimi.dsi.fastutil.ints.IntArrayList();
            java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>(java.util.List.of(start));
            island[start] = id;
            double far = 0, sx = 0, sz = 0;
            while (!queue.isEmpty()) {
                int c = queue.poll(), cx = c / n, cz = c % n;
                cells.add(c);
                double x = cx - GAP_REACH, z = cz - GAP_REACH;
                far = Math.max(far, Math.sqrt(x * x + z * z));
                sx += x;
                sz += z;
                boolean open = false;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        int ax = cx + dx, az = cz + dz;
                        if ((dx == 0 && dz == 0) || ax < 0 || az < 0 || ax >= n || az >= n) continue;
                        int a = ax * n + az;
                        if (!solid[a]) {
                            if (dx == 0 || dz == 0) open = true;
                        } else if (island[a] < 0) {
                            island[a] = id;
                            queue.add(a);
                        }
                    }
                }
                if (open) edge.add(c);
            }
            edges.add(edge.toIntArray());
            shape.add(new double[] {far, sx / cells.size(), sz / cells.size(), cells.size()});
        }
        int main = island[GAP_REACH * n + GAP_REACH];
        if (main < 0) {
            PromisedEnd.LOGGER.info("[promisedend rimgap] seed {}: nothing at the origin", seed);
            return;
        }
        int count = edges.size();
        // reached one by one, each by its shortest gap from one already reached; worst: the longest gap on its way
        double[] gap = new double[count], worst = new double[count];
        boolean[] reached = new boolean[count];
        java.util.Arrays.fill(gap, Double.MAX_VALUE);
        gap[main] = 0;
        double[] fromMain = new double[count];
        double easiestOut = Double.MAX_VALUE;
        for (int step = 0; step < count; step++) {
            int next = -1;
            for (int i = 0; i < count; i++) if (!reached[i] && (next < 0 || gap[i] < gap[next])) next = i;
            if (next < 0 || gap[next] == Double.MAX_VALUE) break;
            reached[next] = true;
            if (shape.get(next)[0] >= GAP_OUT) easiestOut = Math.min(easiestOut, worst[next]);
            for (int i = 0; i < count; i++) {
                if (reached[i]) continue;
                double g = between(edges.get(next), edges.get(i), n);
                if (next == main) fromMain[i] = g;
                if (g < gap[i]) {
                    gap[i] = g;
                    worst[i] = Math.max(worst[next], g);
                }
            }
        }
        double first = Double.MAX_VALUE;
        int firstIsland = -1;
        double[] byEighth = new double[8];
        java.util.Arrays.fill(byEighth, Double.MAX_VALUE);
        for (int i = 0; i < count; i++) {
            if (i == main) continue;
            if (fromMain[i] < first) {
                first = fromMain[i];
                firstIsland = i;
            }
            double angle = Math.toDegrees(Math.atan2(shape.get(i)[2], shape.get(i)[1]));
            int eighth = Math.floorMod((int) Math.floor((angle + 360) / 45), 8);
            byEighth[eighth] = Math.min(byEighth[eighth], fromMain[i]);
        }
        java.util.List<String> eighths = new java.util.ArrayList<>();
        for (double g : byEighth) eighths.add(g == Double.MAX_VALUE ? "none" : String.format(java.util.Locale.ROOT, "%.0f", g));
        double[] f = firstIsland < 0 ? new double[] {0, 0, 0, 0} : shape.get(firstIsland);
        PromisedEnd.LOGGER.info(String.format(java.util.Locale.ROOT,
                "[promisedend rimgap] seed %d: the main island reaches %.0f; %d islands within %d; the first crossing %.1f (to an island of %.0f columns, %.0f out); by eighth from east %s; the easiest way %d out jumps at most %s",
                seed, shape.get(main)[0], count - 1, GAP_REACH, first, f[3], Math.hypot(f[1], f[2]), eighths, GAP_OUT,
                easiestOut == Double.MAX_VALUE ? "(no way)" : String.format(java.util.Locale.ROOT, "%.1f", easiestOut)));
    }

    /** The open air between two islands' edges: the distance between the nearest columns' centres, less one. */
    private static double between(int[] a, int[] b, int n) {
        double best = Double.MAX_VALUE;
        for (int p : a) {
            int px = p / n, pz = p % n;
            for (int q : b) {
                int dx = px - q / n, dz = pz - q % n;
                double d = dx * dx + dz * dz;
                if (d < best) best = d;
            }
        }
        return Math.sqrt(best) - 1;
    }

    public static BlockPos interior(net.minecraft.server.level.ServerLevel level, net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome> key, BlockPos hit) {
        java.util.List<BlockPos> in = new java.util.ArrayList<>();
        for (int dx = -96; dx <= 96; dx += 8) {
            for (int dz = -96; dz <= 96; dz += 8) {
                int x = hit.getX() + dx, z = hit.getZ() + dz;
                if (land(level, key, x, z)) in.add(new BlockPos(x, 64, z));
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

    /**
     * An Updraft Vent placed in the air, its block entity found and ticked by the ticker the block hands out, with an
     * item over it ticked alongside: the item must rise. The vent and its block entity must work without a client.
     */
    private static String updraft(net.minecraft.server.level.ServerLevel end, BlockPos at) {
        end.setBlock(at, ModBlocks.UPDRAFT_VENT.get().defaultBlockState(), 3);
        String failure = lift(end, at, "an Updraft Vent");
        end.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        return failure;
    }

    /**
     * Drift Islets on real terrain: every route whose start lies in two places of 20x20 chunks, 1100-1420 out on the
     * east and on the west, generated with the chunks along it; as many as the search itself finds from those chunks. Each has its islet, lying at its first end or on its way; its two edges are ground with
     * 12-96 blocks of void between; its corridor is clear at the islet's height and a rider's; its moorings stand.
     */
    private static String driftRoutes(net.minecraft.server.level.ServerLevel end) {
        var structure = end.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValue(PromisedEnd.id("drift_route"));
        if (structure == null) return "no drift_route structure";
        java.util.List<com.panda.promisedend.logic.DriftGap.Gap> gaps = new java.util.ArrayList<>();
        int chunks = 0, searched = 0;
        long searchNanos = 0;
        var gen = end.getChunkSource().getGenerator();
        var noise = end.getChunkSource().randomState();
        int floor = end.getMinY() + 1;
        // two places, 20x20 chunks each: 1100-1420 out on the east, and on the west
        for (int[] at : new int[][] {{1100 >> 4, -10}, {-(1420 >> 4), -10}}) for (int cx = at[0]; cx < at[0] + 20; cx++) {
            for (int cz = at[1]; cz < at[1] + 20; cz++) {
                chunks++;
                // the search alone, on the generator's heights, from the chunk's middle: every gap it finds must have its route
                long t0 = System.nanoTime();
                searched += com.panda.promisedend.logic.DriftGap.all((gx, gz) -> gen.getBaseHeight(gx, gz, net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, end, noise),
                        floor, cx * 16 + 8, cz * 16 + 8).size();
                searchNanos += System.nanoTime() - t0;
                var start = end.getChunk(cx, cz).getAllStarts().get(structure);
                if (start == null || !start.isValid()) continue;
                for (var piece : start.getPieces()) {
                    if (piece instanceof com.panda.promisedend.worldgen.DriftRouteStructure.Piece route) gaps.add(route.gap());
                }
            }
        }
        java.util.List<String> bad = new java.util.ArrayList<>(), seen = new java.util.ArrayList<>();
        int posts = 0, clearedAll = 0;
        for (var gap : gaps) {
            var route = gap.route();
            // the chunks along it, so its islet and its far end are there
            for (int k = 0; k <= gap.width() + 1; k += 8) end.getChunk(BlockPos.containing(gap.ex() + gap.dx() * k, gap.he(), gap.ez() + gap.dz() * k));
            end.getChunk(BlockPos.containing(gap.fx(), gap.hf(), gap.fz()));
            var box = new net.minecraft.world.phys.AABB(Math.min(route.ax(), route.bx()) - 2, Math.min(route.ay(), route.by()) - 1, Math.min(route.az(), route.bz()) - 2,
                    Math.max(route.ax(), route.bx()) + 2, Math.max(route.ay(), route.by()) + 2, Math.max(route.az(), route.bz()) + 2);
            // its own islet: another route's may lie in the box, beside it at a shared corner
            var found = end.getEntities(com.panda.promisedend.registry.ModEntities.DRIFT_ISLET.get(), box, e -> route.equals(e.route()));
            int islets = found.size();
            // as on its first ticks: it clears what worldgen next door grew into its way
            int cleared = 0;
            for (var islet : found) cleared += Math.max(0, islet.clearWay());
            clearedAll += cleared;
            BlockPos near = new BlockPos(gap.ex(), gap.he() - 1, gap.ez()), far = new BlockPos(gap.fx(), gap.hf() - 1, gap.fz());
            boolean ends = !end.getBlockState(near).isAir() && !end.getBlockState(far).isAir();
            int blocked = 0;
            java.util.Set<String> what = new java.util.TreeSet<>();
            int lx = gap.dz() != 0 ? 1 : 0, lz = gap.dx() != 0 ? 1 : 0;
            for (int j = 1; j <= gap.width(); j++) {
                for (int l = -1; l <= 1; l++) {
                    for (int y = gap.lowAt(j) - 1; y <= gap.highAt(j) + 1; y++) {
                        var state = end.getBlockState(new BlockPos(gap.ex() + gap.dx() * j + lx * l, y, gap.ez() + gap.dz() * j + lz * l));
                        if (!state.getCollisionShape(end, near).isEmpty()) {
                            blocked++;
                            if (what.size() < 4) what.add(state.getBlock().builtInRegistryHolder().getRegisteredName() + "@" + (gap.ex() + gap.dx() * j + lx * l) + "," + y + "," + (gap.ez() + gap.dz() * j + lz * l));
                        }
                    }
                }
            }
            int lit = 0;
            for (int[] e : new int[][] {{gap.ex(), gap.ez(), gap.he()}, {gap.fx(), gap.fz(), gap.hf()}}) {
                for (int side : new int[] {-2, 2}) {
                    if (end.getBlockState(new BlockPos(e[0] + lx * side, e[2] + 2, e[1] + lz * side)).is(net.minecraft.world.level.block.Blocks.END_ROD)) lit++;
                }
            }
            posts += lit;
            if (lit != 4) bad.add("a route at " + near + " has " + lit + " of its 4 mooring posts");
            String id = end.getBiome(near).unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
            seen.add(gap.ex() + "," + gap.ez() + " " + (gap.dx() != 0 ? "x" : "z") + gap.width() + (gap.he() == gap.hf() ? "" : " " + gap.he() + "-" + gap.hf()) + " " + id + (islets == 1 ? "" : " islets " + islets) + (ends ? "" : " no edge") + (blocked == 0 ? "" : " blocked " + blocked));
            if (islets != 1) bad.add(islets + " islets on the route at " + near);
            if (!ends) bad.add("a route at " + near + " has no ground at an end");
            if (blocked > 0) bad.add("a route at " + near + " has " + blocked + " blocks in its corridor, e.g. " + what);
            if (gap.width() < com.panda.promisedend.logic.DriftRoute.SHORTEST || gap.width() > com.panda.promisedend.logic.DriftRoute.LONGEST) bad.add("a route at " + near + " crosses " + gap.width());
        }
        for (int i = 0; i < gaps.size(); i++) {
            for (int j = i + 1; j < gaps.size(); j++) {
                if (com.panda.promisedend.logic.DriftGap.clash(gaps.get(i), gaps.get(j))) bad.add("the routes " + gaps.get(i) + " and " + gaps.get(j) + " meet");
            }
        }
        PromisedEnd.LOGGER.info("[promisedend servercheck] drift routes in {} chunks: {} (the search finds {}; {} mooring posts, {} blocks cleared from their ways): {}", chunks, gaps.size(), searched, posts, clearedAll, seen);
        PromisedEnd.LOGGER.info("[promisedend servercheck] the drift route search: {} ms for {} chunks, {} ms a chunk (on generated terrain, so the heights were the generator's own)",
                searchNanos / 1_000_000, chunks, String.format(java.util.Locale.ROOT, "%.2f", searchNanos / 1e6 / chunks));
        if (gaps.size() != searched) bad.add(gaps.size() + " routes where the search finds " + searched);
        if (searched == 0) PromisedEnd.LOGGER.warn("[promisedend servercheck] no gap for a drift route in {} chunks on this world", chunks);
        return bad.isEmpty() ? null : "drift routes: " + String.join("; ", bad);
    }

    /**
     * Arrivals on real terrain: End portals in eight directions 1600 out (a first-ring stronghold's distance) and one
     * 4000 out (held to the band's far edge). Each finds ground within reach (of ours with our terrain on), and the
     * Waystation built there has its Return Gate, a floor to stand on and room above it; the same portal again comes to
     * the same Waystation. A new world's Ward stands if a guardian is in the game to break a seal, and is down if none is.
     */
    private static String arrivals(MinecraftServer server, net.minecraft.server.level.ServerLevel end, boolean terrain) {
        var overworld = server.overworld();
        java.util.List<String> bad = new java.util.ArrayList<>(), landed = new java.util.ArrayList<>();
        boolean guarded = com.panda.promisedend.logic.AncientWard.guarded() != 0;
        if (com.panda.promisedend.logic.AncientWard.stands(server) != guarded) bad.add("a new world's Ward is " + (guarded ? "down" : "up with no guardian to break a seal"));
        java.util.List<BlockPos> portals = new java.util.ArrayList<>();
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 4 * i + 0.3;
            portals.add(new BlockPos((int) Math.round(1600 * Math.cos(a)), 30, (int) Math.round(1600 * Math.sin(a))));
        }
        portals.add(new BlockPos(-2828, 30, 2828));
        int before = com.panda.promisedend.logic.Arrivals.built(server).size();
        for (BlockPos portal : portals) {
            double[] point = com.panda.promisedend.logic.Arrival.target(portal.getX() + 0.5, portal.getZ() + 0.5);
            BlockPos ground = com.panda.promisedend.logic.Arrivals.ground(end, (int) Math.floor(point[0]), (int) Math.floor(point[1]));
            var arrival = com.panda.promisedend.logic.Arrivals.destination(overworld, null, portal);
            if (arrival == null) {
                bad.add("no arrival for a portal at " + portal);
                continue;
            }
            BlockPos at = BlockPos.containing(arrival.position()), gate = com.panda.promisedend.logic.Arrivals.gateAt(server, arrival.position());
            if (gate == null) {
                bad.add("set down at " + at + ", on no Waystation's floor");
                continue;
            }
            String id = end.getBiome(gate).unwrapKey().map(k -> k.identifier().getPath()).orElse("?");
            landed.add(portal.getX() + "," + portal.getZ() + " -> " + at.getX() + "," + at.getY() + "," + at.getZ() + " " + id + (ground == null ? " (islet)" : ""));
            if (ground == null) bad.add("no ground within reach of " + Math.round(point[0]) + ", " + Math.round(point[1]));
            else if (terrain && !end.getBiome(gate).unwrapKey().map(k -> k.identifier().getNamespace().equals(PromisedEnd.MODID)).orElse(false)) bad.add("a Waystation on " + id);
            if (!end.getBlockState(gate).is(ModBlocks.RETURN_GATE.get())) bad.add("no Return Gate at " + gate + ": " + end.getBlockState(gate));
            boolean rift = false;
            for (BlockPos q : BlockPos.betweenClosed(gate.offset(-5, 0, -5), gate.offset(5, 3, 5))) rift |= end.getBlockState(q).is(ModBlocks.RIFT_GATE.get());
            if (!rift) bad.add("no Rift Gate at the Waystation round " + gate);
            if (!end.getBlockState(at.below()).isFaceSturdy(end, at.below(), net.minecraft.core.Direction.UP)) bad.add("nothing to stand on at " + at);
            if (!end.getBlockState(at).getCollisionShape(end, at).isEmpty() || !end.getBlockState(at.above()).getCollisionShape(end, at.above()).isEmpty()) bad.add("no room to stand at " + at + ": " + end.getBlockState(at) + ", " + end.getBlockState(at.above()));
            var again = com.panda.promisedend.logic.Arrivals.destination(overworld, null, portal);
            if (again == null || !again.position().equals(arrival.position())) bad.add("the portal at " + portal + " again came to " + (again == null ? null : again.position()));
        }
        int built = com.panda.promisedend.logic.Arrivals.built(server).size() - before;
        PromisedEnd.LOGGER.info("[promisedend servercheck] arrivals, {} Waystations built: {}", built, landed);
        if (built != portals.size()) bad.add(built + " Waystations for " + portals.size() + " portals");
        return bad.isEmpty() ? null : "arrivals: " + String.join("; ", bad);
    }

    /**
     * Where the twenty outer gateways drop a player on this world: vanilla's own exit search (from 1024 out along the
     * gateway's line, back past land and on past void) for each, with the chunks it reads generated, then vanilla's
     * spot in the chunk it lands in (or, where it finds none and builds an island, the column). Every landing must be
     * in one of our lands, never a vanilla biome.
     */
    private static String gatewayLandings(net.minecraft.server.level.ServerLevel end) {
        java.lang.reflect.Method tentative, chunkAt, spot;
        try {
            var gateway = net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity.class;
            tentative = gateway.getDeclaredMethod("findExitPortalXZPosTentative", net.minecraft.server.level.ServerLevel.class, BlockPos.class);
            chunkAt = gateway.getDeclaredMethod("getChunk", Level.class, net.minecraft.world.phys.Vec3.class);
            spot = gateway.getDeclaredMethod("findValidSpawnInChunk", net.minecraft.world.level.chunk.LevelChunk.class);
            for (var m : java.util.List.of(tentative, chunkAt, spot)) m.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            return "no gateway exit search: " + e;
        }
        java.util.List<String> landed = new java.util.ArrayList<>(), bad = new java.util.ArrayList<>();
        int nearest = Integer.MAX_VALUE, farthest = 0;
        for (int i = 0; i < 20; i++) {
            // where the dragon fight puts gateway i
            double a = 2.0 * (-Math.PI + 0.15707963267948966 * i);
            BlockPos from = new BlockPos(net.minecraft.util.Mth.floor(96.0 * Math.cos(a)), 75, net.minecraft.util.Mth.floor(96.0 * Math.sin(a)));
            BlockPos at;
            try {
                var xz = (net.minecraft.world.phys.Vec3) tentative.invoke(null, end, from);
                at = (BlockPos) spot.invoke(null, chunkAt.invoke(null, end, xz));
                if (at == null) at = BlockPos.containing(xz.x, 75, xz.z);
            } catch (ReflectiveOperationException e) {
                return "gateway " + i + "'s exit search: " + e;
            }
            var biome = end.getBiome(at);
            String id = biome.unwrapKey().map(k -> k.identifier().toString()).orElse("?");
            int out = (int) Math.sqrt((double) at.getX() * at.getX() + (double) at.getZ() * at.getZ());
            nearest = Math.min(nearest, out);
            farthest = Math.max(farthest, out);
            landed.add(i + " " + id.substring(id.indexOf(':') + 1) + " " + out);
            if (!id.startsWith(PromisedEnd.MODID + ":")) bad.add("gateway " + i + " lands " + out + " out on " + id);
        }
        PromisedEnd.LOGGER.info("[promisedend servercheck] gateway landings, {}-{} out: {}", nearest, farthest, landed);
        return bad.isEmpty() ? null : String.join("; ", bad);
    }

    /**
     * A vent the Skyfall Archipelago's worldgen sank into an islet's edge, in the generated biome (generated by
     * {@link #biome} just before): its block entity came from worldgen and ticks without a client, as the placed one does.
     * With it, how often the vents place: over the chunks within 64 blocks of a Skyfall's and a Voidscar's spot whose
     * middle is that biome, the vents in them against the one chunk in two that tries (a try finds no edge in reach, or
     * stands too near a vent already there, so fewer place).
     */
    private static String generatedVent(net.minecraft.server.level.ServerLevel end) {
        String failure = null;
        for (String id : java.util.List.of("skyfall_archipelago", "voidscar_wastes")) {
            var key = net.minecraft.resources.ResourceKey.create(Registries.BIOME, PromisedEnd.id(id));
            var places = spots(end, key, 32);
            if (places.isEmpty()) return "no " + id + " within 6400 blocks";
            int chunks = 0, vents = 0, used = 0;
            BlockPos first = null;
            java.util.Set<Long> counted = new java.util.HashSet<>();
            // the biome's chunks within 8 of its best places in turn, until 120 are counted
            for (BlockPos at : places) {
                if (chunks >= 120 || used == 4) break;
                used++;
                var origin = net.minecraft.world.level.ChunkPos.containing(at);
                for (int cx = origin.x() - 8; cx <= origin.x() + 8; cx++) {
                    for (int cz = origin.z() - 8; cz <= origin.z() + 8; cz++) {
                        if (!counted.add(net.minecraft.world.level.ChunkPos.pack(cx, cz))) continue;
                        if (!end.getBiome(new BlockPos(cx * 16 + 8, 64, cz * 16 + 8)).is(key)) continue;
                        chunks++;
                        for (BlockPos p : BlockPos.betweenClosed(cx * 16, 24, cz * 16, cx * 16 + 15, 140, cz * 16 + 15)) {
                            if (!end.getBlockState(p).is(ModBlocks.UPDRAFT_VENT.get())) continue;
                            vents++;
                            if (first == null) first = p.immutable();
                        }
                    }
                }
            }
            PromisedEnd.LOGGER.info("[promisedend servercheck] {}, {} places: {} generated Updraft Vents in {} of its chunks (1 in {})", id, used, vents, chunks,
                    vents == 0 ? "none" : String.format(java.util.Locale.ROOT, "%.1f", chunks / (double) vents));
            if (first == null) return "no generated Updraft Vent in the " + id;
            if (chunks < 60) return "only " + chunks + " chunks of the " + id + " to count vents in";
            // each biome's own rate, measured on seeds 1-16 twice (the Skyfall 779 vents in 1673 chunks, the Voidscar 394
            // in 1073), and its spread from place to place, half again a binomial's (vents come by islands): the floor is
            // 2.33 of those spreads under the rate, which a fair world falls below about once in a hundred and a vent at
            // half its rate falls below in most worlds
            double rate = id.equals("skyfall_archipelago") ? 0.47 : 0.36;
            int floor = Math.max(1, (int) Math.floor(chunks * rate - 2.33 * 1.5 * Math.sqrt(chunks * rate * (1 - rate))));
            if (vents < floor) return "only " + vents + " generated Updraft Vents in " + chunks + " chunks of the " + id + " (the floor is " + floor + ")";
            if (failure == null && id.equals("skyfall_archipelago")) failure = lift(end, first, "the generated vent at " + first);
        }
        return failure;
    }

    /** The vent at {@code at}: its block entity found and ticked by its own ticker, an item over it ticked alongside. */
    private static String lift(net.minecraft.server.level.ServerLevel end, BlockPos at, String what) {
        var state = end.getBlockState(at);
        var ticker = state.getTicker(end, com.panda.promisedend.registry.ModBlockEntities.UPDRAFT_VENT.get());
        var vent = end.getBlockEntity(at, com.panda.promisedend.registry.ModBlockEntities.UPDRAFT_VENT.get()).orElse(null);
        var item = new net.minecraft.world.entity.item.ItemEntity(end, at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5,
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK), 0, 0, 0);
        end.addFreshEntity(item);
        double start = item.getY();
        String failure = null;
        if (ticker == null || vent == null) failure = what + " has no " + (ticker == null ? "ticker" : "block entity");
        else {
            for (int i = 0; i < 20; i++) {
                ticker.tick(end, at, state, vent);
                item.tick();
            }
            PromisedEnd.LOGGER.info("[promisedend servercheck] an item over {} rose {} in 20 ticks (the column {})", what,
                    String.format(java.util.Locale.ROOT, "%.2f", item.getY() - start), vent.height());
            if (item.getY() < start + 2) failure = "an item over " + what + " rose only " + (item.getY() - start);
        }
        item.discard();
        return failure;
    }

    /**
     * The Shulker Foundry nearest a point 1500 out, as a held pearl finds it, generated here: its Hollow Sentinel is in
     * place and dormant, holds its rest over 40 ticks, and a shot of its own flies at a pig beside it. The Sentinel and its
     * bullet must load and tick without a client.
     */
    private static String foundry(net.minecraft.server.level.ServerLevel end) {
        BlockPos from = new BlockPos(1500, 64, 0);
        BlockPos found = end.findNearestMapStructure(com.panda.promisedend.registry.ModTags.ON_FOUNDRY_CHARTS, from, com.panda.promisedend.logic.PearlGuide.SEARCH, false);
        if (found == null) return "no Shulker Foundry within " + com.panda.promisedend.logic.PearlGuide.SEARCH + " chunks of " + from;
        for (int cx = (found.getX() >> 4) - 3; cx <= (found.getX() >> 4) + 3; cx++) {
            for (int cz = (found.getZ() >> 4) - 3; cz <= (found.getZ() >> 4) + 3; cz++) end.getChunk(cx, cz);
        }
        var sentinels = end.getEntitiesOfClass(com.panda.promisedend.entity.HollowSentinel.class, new net.minecraft.world.phys.AABB(found).inflate(56, 256, 56));
        PromisedEnd.LOGGER.info("[promisedend servercheck] Shulker Foundry {} from {} at {}: {} Sentinel(s){}", Math.round(Math.sqrt(found.distSqr(from))), from, found,
                sentinels.size(), sentinels.isEmpty() ? "" : " at " + sentinels.getFirst().blockPosition() + ", " + sentinels.getFirst().state());
        if (sentinels.size() != 1) return "the Foundry at " + found + " has " + sentinels.size() + " Sentinels";
        var sentinel = sentinels.getFirst();
        if (sentinel.state() != com.panda.promisedend.entity.HollowSentinel.State.DORMANT) return "the Foundry's Sentinel is " + sentinel.state();
        net.minecraft.world.phys.Vec3 rest = sentinel.position();
        for (int i = 0; i < 40; i++) sentinel.tick();
        var pig = net.minecraft.world.entity.EntityTypes.PIG.create(end, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (pig == null) return "couldn't create a pig";
        pig.snapTo(rest.x + 6, rest.y, rest.z, 0, 0);
        end.addFreshEntity(pig);
        var bullet = new com.panda.promisedend.entity.ResonantBullet(end, sentinel, pig, sentinel.bulletSpeed(), com.panda.promisedend.entity.ResonantBullet.DAMAGE);
        end.addFreshEntity(bullet);
        double before = bullet.distanceTo(pig);
        for (int i = 0; i < 10; i++) bullet.tick();
        double after = bullet.isRemoved() ? 0 : bullet.distanceTo(pig);
        PromisedEnd.LOGGER.info("[promisedend servercheck] the Sentinel held {} from its rest over 40 ticks; its bullet closed {} to {} on a pig in 10 ticks",
                String.format(java.util.Locale.ROOT, "%.2f", sentinel.position().distanceTo(rest)), String.format(java.util.Locale.ROOT, "%.1f", before),
                bullet.isRemoved() ? "a hit" : String.format(java.util.Locale.ROOT, "%.1f", after));
        String failure = null;
        if (sentinel.position().distanceTo(rest) > 1) failure = "the dormant Sentinel moved " + sentinel.position().distanceTo(rest) + " from its rest";
        else if (after >= before) failure = "the Sentinel's bullet didn't close on a pig: " + before + " to " + after;
        bullet.discard();
        pig.discard();
        if (failure != null) return failure;
        // the arrival's sealed Star Chart, unrolled where a portal sets a player down, marks this Foundry
        var items = com.panda.promisedend.logic.Arrivals.kit();
        var sealed = items.stream().filter(i -> i.is(com.panda.promisedend.registry.ModItems.STAR_CHART.get())).findFirst().orElse(null);
        if (sealed == null) return "no Star Chart in the arrival's kit: " + items;
        var kind = sealed.get(com.panda.promisedend.registry.ModComponents.STAR_CHART.get());
        if (kind == null) return "the Waystation's Star Chart is blank";
        var chart = com.panda.promisedend.item.StarChartItem.unroll(end, from, kind);
        if (chart == null) return "the Waystation's Star Chart (" + kind + ") found nothing from " + from;
        var marks = chart.getOrDefault(net.minecraft.core.component.DataComponents.MAP_DECORATIONS, net.minecraft.world.item.component.MapDecorations.EMPTY).decorations().values();
        String name = chart.getHoverName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : chart.getHoverName().getString();
        PromisedEnd.LOGGER.info("[promisedend servercheck] the Waystation's Star Chart: {}, map {}, marks {}", name, chart.get(net.minecraft.core.component.DataComponents.MAP_ID),
                marks.stream().map(d -> d.type().getRegisteredName() + " at " + Math.round(d.x()) + ", " + Math.round(d.z())).toList());
        if (!name.equals("item." + PromisedEnd.MODID + ".star_chart.foundry")) return "the Star Chart is named " + name;
        if (marks.stream().noneMatch(d -> Math.hypot(d.x() - found.getX(), d.z() - found.getZ()) < 16)) return "the Star Chart marks no Foundry at " + found + ": " + marks;
        WAYSTATION_MARK[0] = found;
        return null;
    }

    /**
     * The Rift Maw nearest a point 1500 out, as the Sentinel's chart and a held pearl find it, generated here: its Void
     * Leviathan is in place under the pool, asleep, and holds its rest over 40 ticks; the chart, unrolled there, marks it.
     * The Leviathan must load and tick without a client.
     */
    private static String maw(net.minecraft.server.level.ServerLevel end) {
        BlockPos from = new BlockPos(1500, 64, 0);
        BlockPos found = end.findNearestMapStructure(com.panda.promisedend.registry.ModTags.ON_RIFT_MAW_CHARTS, from, com.panda.promisedend.logic.PearlGuide.SEARCH, false);
        if (found == null) return "no Rift Maw within " + com.panda.promisedend.logic.PearlGuide.SEARCH + " chunks of " + from;
        for (int cx = (found.getX() >> 4) - 4; cx <= (found.getX() >> 4) + 4; cx++) {
            for (int cz = (found.getZ() >> 4) - 4; cz <= (found.getZ() >> 4) + 4; cz++) end.getChunk(cx, cz);
        }
        var leviathans = end.getEntitiesOfClass(com.panda.promisedend.entity.VoidLeviathan.class, new net.minecraft.world.phys.AABB(found).inflate(72, 256, 72));
        PromisedEnd.LOGGER.info("[promisedend servercheck] Rift Maw {} from {} at {}: {} Leviathan(s){}", Math.round(Math.sqrt(found.distSqr(from))), from, found,
                leviathans.size(), leviathans.isEmpty() ? "" : " at " + leviathans.getFirst().blockPosition() + ", " + leviathans.getFirst().state());
        if (leviathans.size() != 1) return "the Maw at " + found + " has " + leviathans.size() + " Leviathans";
        var leviathan = leviathans.getFirst();
        if (leviathan.state() != com.panda.promisedend.entity.VoidLeviathan.State.DORMANT) return "the Maw's Leviathan is " + leviathan.state();
        BlockPos rest = leviathan.blockPosition();
        net.minecraft.world.phys.Vec3 at = leviathan.position();
        for (int i = 0; i < 40; i++) leviathan.tick();
        // the pool over it is open to the sky once it has ticked (its first tick clears the land that grew into it)
        for (int y = rest.getY() + 1; y <= rest.getY() + com.panda.promisedend.entity.VoidLeviathan.SURFACE + 8; y++) {
            if (!end.getBlockState(rest.atY(y)).isAir()) return "the pool over the Leviathan is shut at " + rest.atY(y) + ": " + end.getBlockState(rest.atY(y));
        }
        PromisedEnd.LOGGER.info("[promisedend servercheck] the Leviathan held {} from its rest over 40 ticks",
                String.format(java.util.Locale.ROOT, "%.2f", leviathan.position().distanceTo(at)));
        if (leviathan.position().distanceTo(at) > 1) return "the sleeping Leviathan moved " + leviathan.position().distanceTo(at) + " from its rest";
        var chart = com.panda.promisedend.item.StarChartItem.unroll(end, from, PromisedEnd.id("on_rift_maw_charts"));
        if (chart == null) return "the Sentinel's Star Chart found no Maw from " + from;
        var marks = chart.getOrDefault(net.minecraft.core.component.DataComponents.MAP_DECORATIONS, net.minecraft.world.item.component.MapDecorations.EMPTY).decorations().values();
        if (marks.stream().noneMatch(d -> Math.hypot(d.x() - found.getX(), d.z() - found.getZ()) < 16)) return "the Star Chart marks no Maw at " + found + ": " + marks;
        return null;
    }

    /**
     * The Void Shrine nearest a point 1500 out, generated here: its Shrine Altar stands with its block entity, and its
     * two Purpur Sentries are in place.
     */
    private static String shrine(net.minecraft.server.level.ServerLevel end) {
        BlockPos from = new BlockPos(1500, 64, 0);
        var structures = end.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        var key = net.minecraft.resources.ResourceKey.create(Registries.STRUCTURE, PromisedEnd.id("void_shrine"));
        var holder = structures.get(key);
        if (holder.isEmpty()) return "no Void Shrine structure";
        var found = end.getChunkSource().getGenerator().findNearestMapStructure(end, net.minecraft.core.HolderSet.direct(holder.get()), from, 60, false);
        if (found == null) return "no Void Shrine within 60 chunks of " + from;
        BlockPos at = found.getFirst();
        for (int cx = (at.getX() >> 4) - 2; cx <= (at.getX() >> 4) + 2; cx++) {
            for (int cz = (at.getZ() >> 4) - 2; cz <= (at.getZ() >> 4) + 2; cz++) end.getChunk(cx, cz);
        }
        BlockPos altar = null;
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-24, 40, -24), at.offset(24, 90, 24))) {
            if (end.getBlockState(q).is(ModBlocks.SHRINE_ALTAR.get())) {
                altar = q.immutable();
                break;
            }
        }
        if (altar == null) return "no Shrine Altar in the Void Shrine at " + at;
        BlockPos gate = null;
        for (BlockPos q : BlockPos.betweenClosed(altar.offset(-6, -2, -6), altar.offset(6, 4, 6))) {
            if (end.getBlockState(q).is(ModBlocks.RIFT_GATE.get())) {
                gate = q.immutable();
                break;
            }
        }
        if (gate == null) return "no Rift Gate in the Void Shrine at " + altar;
        // its core faces into the shrine: the step out of it is nearer the altar than the core is
        var facing = end.getBlockState(gate).getValue(com.panda.promisedend.block.RiftGateBlock.FACING);
        if (gate.relative(facing).distSqr(altar) >= gate.distSqr(altar)) return "the shrine's Rift Gate at " + gate + " faces " + facing + ", away from its altar";
        int frame = 0;
        for (BlockPos q : BlockPos.betweenClosed(gate.offset(-1, -1, -1), gate.offset(1, 1, 1))) if (end.getBlockState(q).is(ModBlocks.RIFT_GATE_FRAME.get())) frame++;
        if (frame != 8) return "the shrine's Rift Gate at " + gate + " has " + frame + " frame blocks";
        PromisedEnd.LOGGER.info("[promisedend servercheck] the Void Shrine's Rift Gate at {}, facing {}, in a frame of {}", gate, facing, frame);
        int sentries = end.getEntitiesOfClass(com.panda.promisedend.entity.PurpurSentry.class, new net.minecraft.world.phys.AABB(altar).inflate(8)).size();
        boolean entity = end.getBlockEntity(altar) instanceof com.panda.promisedend.block.ShrineAltarBlockEntity;
        PromisedEnd.LOGGER.info("[promisedend servercheck] Void Shrine {} from {} at {}: its altar at {} (block entity {}), {} Purpur Sentries, on {}",
                Math.round(Math.sqrt(at.distSqr(from))), from, at, altar, entity, sentries, end.getBiome(altar).getRegisteredName());
        if (!entity) return "the Shrine Altar at " + altar + " has no block entity";
        if (sentries < 2) return "the Void Shrine at " + altar + " has " + sentries + " Purpur Sentries";
        return null;
    }

    /**
     * The Fallen Observatory nearest a point 1500 out, generated here: its Star Chart Desk stands with its block entity
     * and draws only kinds this world can chart, its chest and its Sentries are in place, and the chart it gives,
     * unrolled there, marks a Foundry other than the one the Waystation's chart led to (each place is charted once).
     */
    /** Where the Waystation's chart led, for the observatory's check that the next chart leads elsewhere. */
    private static final BlockPos[] WAYSTATION_MARK = {null};

    private static String observatory(net.minecraft.server.level.ServerLevel end) {
        BlockPos from = new BlockPos(1500, 64, 0);
        BlockPos at = end.findNearestMapStructure(com.panda.promisedend.item.StarChartItem.tag(PromisedEnd.id("on_observatory_charts")), from,
                com.panda.promisedend.item.StarChartItem.SEARCH_RINGS, false);
        if (at == null) return "no Fallen Observatory within " + com.panda.promisedend.item.StarChartItem.SEARCH_RINGS + " rings of " + from;
        for (int cx = (at.getX() >> 4) - 2; cx <= (at.getX() >> 4) + 2; cx++) {
            for (int cz = (at.getZ() >> 4) - 2; cz <= (at.getZ() >> 4) + 2; cz++) end.getChunk(cx, cz);
        }
        BlockPos desk = null;
        int chests = 0;
        for (BlockPos q : BlockPos.betweenClosed(at.offset(-24, 40, -24), at.offset(24, 100, 24))) {
            var state = end.getBlockState(q);
            if (state.is(ModBlocks.STAR_CHART_DESK.get()) && desk == null) desk = q.immutable();
            if (state.is(net.minecraft.world.level.block.Blocks.CHEST)) chests++;
        }
        if (desk == null) return "no Star Chart Desk in the Fallen Observatory at " + at;
        if (!(end.getBlockEntity(desk) instanceof com.panda.promisedend.block.StarChartDeskBlockEntity entity)) return "the Star Chart Desk at " + desk + " has no block entity";
        var kinds = new java.util.TreeSet<String>();
        for (int i = 0; i < 20; i++) {
            var drawn = entity.draw();
            kinds.add(drawn.isEmpty() ? "nothing" : drawn.get(com.panda.promisedend.registry.ModComponents.STAR_CHART.get()).getPath());
        }
        int sentries = end.getEntitiesOfClass(com.panda.promisedend.entity.PurpurSentry.class, new net.minecraft.world.phys.AABB(desk).inflate(10)).size();
        var chart = com.panda.promisedend.item.StarChartItem.unroll(end, desk, PromisedEnd.id("on_foundry_charts"));
        var marks = chart == null ? java.util.List.<net.minecraft.world.item.component.MapDecorations.Entry>of()
                : java.util.List.copyOf(chart.getOrDefault(net.minecraft.core.component.DataComponents.MAP_DECORATIONS, net.minecraft.world.item.component.MapDecorations.EMPTY).decorations().values());
        PromisedEnd.LOGGER.info("[promisedend servercheck] Fallen Observatory {} from {} at {}: its desk at {} draws {}; {} chest(s), {} Purpur Sentries, on {}; its Foundry chart marks {}",
                Math.round(Math.sqrt(at.distSqr(from))), from, at, desk, kinds, chests, sentries, end.getBiome(desk).getRegisteredName(),
                marks.stream().map(d -> Math.round(d.x()) + ", " + Math.round(d.z())).toList());
        // the Foundry's and the Maw's (the Citadel's tag is empty till its place comes)
        if (!kinds.equals(new java.util.TreeSet<>(java.util.List.of("on_foundry_charts", "on_rift_maw_charts"))))
            return "the desk draws " + kinds;
        if (chests < 1) return "the Fallen Observatory at " + at + " has no chest";
        if (sentries < 2) return "the Fallen Observatory at " + at + " has " + sentries + " Purpur Sentries";
        if (marks.isEmpty()) return "a Foundry chart unrolled at " + desk + " marks nothing";
        if (WAYSTATION_MARK[0] != null && marks.stream().anyMatch(d -> Math.hypot(d.x() - WAYSTATION_MARK[0].getX(), d.z() - WAYSTATION_MARK[0].getZ()) < 32))
            return "the desk's chart leads back to the Foundry the Waystation's chart marked, " + WAYSTATION_MARK[0];
        return null;
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

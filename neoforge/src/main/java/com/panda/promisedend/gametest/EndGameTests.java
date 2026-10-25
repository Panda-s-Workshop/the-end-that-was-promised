package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.panda.promisedend.worldgen.EndBiomes;
import com.panda.promisedend.worldgen.EndRegions;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Registration, data and behaviour checks that run on a GameTest server. */
public final class EndGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
            DeferredRegister.create(Registries.TEST_FUNCTION, PromisedEnd.MODID);
    private static final Identifier ARENA = PromisedEnd.id("test_arena");

    static {
        FUNCTIONS.register("mod_loaded", () -> EndGameTests::modLoaded);
        FUNCTIONS.register("lang_coverage", () -> EndGameTests::langCoverage);
        FUNCTIONS.register("end_biome_source_places_biomes", () -> EndGameTests::endBiomeSourcePlacesBiomes);
        FUNCTIONS.register("biomes_in_is_end_tag", () -> EndGameTests::biomesInIsEndTag);
        FUNCTIONS.register("data_files_all_load", () -> EndGameTests::dataFilesAllLoad);
        FUNCTIONS.register("every_block_drops", () -> EndGameTests::everyBlockDrops);
        FUNCTIONS.register("rim_islands_generate", () -> EndGameTests::rimIslandsGenerate);
        FUNCTIONS.register("voidstone_tools_work", () -> EndGameTests::voidstoneToolsWork);
        FUNCTIONS.register("void_anchor_rescues", () -> EndGameTests::voidAnchorRescues);
        FUNCTIONS.register("void_anchor_needs_charge", () -> EndGameTests::voidAnchorNeedsCharge);
        FUNCTIONS.register("structures_place", () -> EndGameTests::structuresPlace);
        FUNCTIONS.register("codex_goals_are_advancements", () -> EndGameTests::codexGoalsAreAdvancements);
        FUNCTIONS.register("structure_sets_resolve", () -> EndGameTests::structureSetsResolve);
        FUNCTIONS.register("rim_outpost_near_spawn", () -> EndGameTests::rimOutpostNearSpawn);
        FUNCTIONS.register("every_recipe_unlocks", () -> EndGameTests::everyRecipeUnlocks);
        FUNCTIONS.register("void_anchor_rules", () -> EndGameTests::voidAnchorRules);
        FUNCTIONS.register("terrain_off_falls_back", () -> EndGameTests::terrainOffFallsBack);
        FUNCTIONS.register("resonite_gear", () -> EndGameTests::resoniteGear);
        FUNCTIONS.register("chorus_resin_from_stripping", () -> EndGameTests::chorusResinFromStripping);
        FUNCTIONS.register("set_bonus_resonance", () -> EndGameTests::setBonusResonance);
        FUNCTIONS.register("void_anchor_safe_ground", () -> EndGameTests::voidAnchorSafeGround);
        FUNCTIONS.register("void_anchor_landing", () -> EndGameTests::voidAnchorLanding);
        FUNCTIONS.register("void_anchor_end_fallback", () -> EndGameTests::voidAnchorEndFallback);
        FUNCTIONS.register("rim_progress_by_distance", () -> EndGameTests::rimProgressByDistance);
        FUNCTIONS.register("rim_outpost_footprint", () -> EndGameTests::rimOutpostFootprint);
        FUNCTIONS.register("act_one_goals_by_inventory", () -> EndGameTests::actOneGoalsByInventory);
        FUNCTIONS.register("codex_reset_command", () -> EndGameTests::codexResetCommand);
        FUNCTIONS.register("void_anchor_unloaded_neighbour", () -> EndGameTests::voidAnchorUnloadedNeighbour);
        FUNCTIONS.register("crystal_warden_stats", () -> EndGameTests::crystalWardenStats);
        FUNCTIONS.register("crystal_warden_lock_then_beam", () -> EndGameTests::crystalWardenLockThenBeam);
        FUNCTIONS.register("crystal_warden_cover_breaks_lock", () -> EndGameTests::crystalWardenCoverBreaksLock);
        FUNCTIONS.register("construct_assets_match", () -> EndGameTests::constructAssetsMatch);
        FUNCTIONS.register("crystal_warden_beam_cap_and_rest", () -> EndGameTests::crystalWardenBeamCapAndRest);
        FUNCTIONS.register("crystal_warden_ignores_the_dragon", () -> EndGameTests::crystalWardenIgnoresTheDragon);
        FUNCTIONS.register("crystal_warden_shatters_and_drops", () -> EndGameTests::crystalWardenShattersAndDrops);
        FUNCTIONS.register("dragon_phase3_triggers", () -> EndGameTests::dragonPhase3Triggers);
        FUNCTIONS.register("warden_heals_dragon", () -> EndGameTests::wardenHealsDragon);
        FUNCTIONS.register("dragon_void_breath_dive", () -> EndGameTests::dragonVoidBreathDive);
        FUNCTIONS.register("wrath_increments_on_kill", () -> EndGameTests::wrathIncrementsOnKill);
        FUNCTIONS.register("dragon_drops_scales", () -> EndGameTests::dragonDropsScales);
        FUNCTIONS.register("dragon_scale_repairs_elytra", () -> EndGameTests::dragonScaleRepairsElytra);
        FUNCTIONS.register("dragon_phase3_wardens_by_wrath", () -> EndGameTests::dragonPhase3WardensByWrath);
        FUNCTIONS.register("void_breath_dodged", () -> EndGameTests::voidBreathDodged);
        FUNCTIONS.register("called_wardens_take_turns", () -> EndGameTests::calledWardensTakeTurns);
        FUNCTIONS.register("outer_end_shares", () -> EndGameTests::outerEndShares);
        FUNCTIONS.register("end_cities_in_our_biomes", () -> EndGameTests::endCitiesInOurBiomes);
        FUNCTIONS.register("chorus_timber_and_moss", () -> EndGameTests::chorusTimberAndMoss);
        FUNCTIONS.register("chorus_wood_set", () -> EndGameTests::chorusWoodSet);
        FUNCTIONS.register("chorus_grove_grows", () -> EndGameTests::chorusGroveGrows);
        FUNCTIONS.register("chorus_grove_keeps_out", () -> EndGameTests::chorusGroveKeepsOut);
        FUNCTIONS.register("void_water_slows", () -> EndGameTests::voidWaterSlows);
        FUNCTIONS.register("mire_lily_rules", () -> EndGameTests::mireLilyRules);
        FUNCTIONS.register("lumen_moss_drops", () -> EndGameTests::lumenMossDrops);
        FUNCTIONS.register("mire_pools_walled", () -> EndGameTests::mirePoolsWalled);
        FUNCTIONS.register("resonite_spires_grow", () -> EndGameTests::resoniteSpiresGrow);
        FUNCTIONS.register("resonite_cluster_chimes", () -> EndGameTests::resoniteClusterChimes);
        FUNCTIONS.register("budding_resonite_grows", () -> EndGameTests::buddingResoniteGrows);
        FUNCTIONS.register("resonite_drops", () -> EndGameTests::resoniteDrops);
        FUNCTIONS.register("gateway_exits_beside_spires", () -> EndGameTests::gatewayExitsBesideSpires);
    }

    /** Tests that need longer than the default 100 ticks (a dragon's dying takes 200). */
    private static final java.util.Map<String, Integer> TIMEOUTS = java.util.Map.of("crystal_warden_beam_cap_and_rest", 220,
            "wrath_increments_on_kill", 260, "dragon_drops_scales", 240, "void_water_slows", 160, "dragon_phase3_triggers", 460);
    /**
     * Tests in a batch of their own: the world has one Wrath, which these change, and a batch's tests run side by side
     * (batches run one after another).
     */
    private static final java.util.Map<String, String> OWN_BATCH = java.util.Map.of("wrath_increments_on_kill", "wrath_kill",
            "dragon_drops_scales", "wrath_hoard");
    /**
     * Tests with a live dragon, each in a batch alone: a dragon's box is 16 blocks across, wider than the arena, and a
     * test that succeeds removes every entity in and round its own bounds, so a neighbour finishing first took the
     * dragon with it (seen as a Warden whose heals never landed).
     */
    private static final java.util.Set<String> ALONE = java.util.Set.of("dragon_phase3_triggers", "dragon_phase3_wardens_by_wrath",
            "warden_heals_dragon", "dragon_void_breath_dive", "void_breath_dodged", "called_wardens_take_turns",
            "crystal_warden_ignores_the_dragon", "crystal_warden_shatters_and_drops");

    private EndGameTests() {}

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(EndGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(PromisedEnd.id("default"));
        java.util.Map<String, Holder<TestEnvironmentDefinition<?>>> own = new java.util.HashMap<>();
        OWN_BATCH.values().forEach(batch -> own.put(batch, event.registerEnvironment(PromisedEnd.id(batch))));
        for (var entry : FUNCTIONS.getEntries()) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, entry.getId());
            String path = entry.getId().getPath();
            int timeout = TIMEOUTS.getOrDefault(path, 100);
            var testEnv = OWN_BATCH.containsKey(path) ? own.get(OWN_BATCH.get(path))
                    : ALONE.contains(path) ? event.registerEnvironment(PromisedEnd.id("alone_" + path)) : env;
            event.registerTest(key.identifier(), new FunctionGameTestInstance(key, new TestData<>(testEnv, ARENA, timeout, 0, true)));
        }
    }

    // ---- tests -------------------------------------------------------------------------------------------------

    /** The common side ran and its registers were drained into the game's registries. */
    private static void modLoaded(GameTestHelper helper) {
        if (!PromisedEnd.initialised()) helper.fail("PromisedEnd.init() never ran");
        ModItems.ITEMS.getEntries().forEach(h -> h.get());
        ModBlocks.BLOCKS.getEntries().forEach(h -> h.get());
        helper.succeed();
    }

    /** Every item, block and advancement text of ours has an English name, and every hint line in a tooltip its text. */
    private static void langCoverage(GameTestHelper helper) {
        var lang = net.minecraft.locale.Language.getInstance();
        List<String> missing = new ArrayList<>();
        ModItems.ITEMS.getEntries().forEach(h -> {
            String key = h.get().getDescriptionId();
            if (!lang.has(key)) missing.add(key);
        });
        ModBlocks.BLOCKS.getEntries().forEach(h -> {
            String key = h.get().getDescriptionId();
            if (!lang.has(key)) missing.add(key);
        });
        com.panda.promisedend.registry.ModEntities.ENTITIES.getEntries().forEach(h -> {
            String key = h.get().getDescriptionId();
            if (!lang.has(key)) missing.add(key);
        });
        // the hint lines items and blocks show in their tooltips: each key there, and none left over past the last
        ModItems.ITEMS.getEntries().forEach(h -> {
            List<Component> lines = new ArrayList<>();
            h.get().appendHoverText(new ItemStack(h.get()), net.minecraft.world.item.Item.TooltipContext.EMPTY,
                    net.minecraft.world.item.component.TooltipDisplay.DEFAULT, lines::add, net.minecraft.world.item.TooltipFlag.NORMAL);
            String last = null;
            for (Component c : lines) {
                if (!(c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t)) continue;
                if (!lang.has(t.getKey())) missing.add(t.getKey());
                last = t.getKey();
            }
            if (last != null && last.matches(".*\\.\\d+")) {
                int dot = last.lastIndexOf('.');
                String next = last.substring(0, dot + 1) + (Integer.parseInt(last.substring(dot + 1)) + 1);
                if (lang.has(next)) missing.add(next + " (in the lang file, not in the tooltip)");
            }
        });
        MinecraftServer server = helper.getLevel().getServer();
        server.getAdvancements().getAllAdvancements().forEach(holder -> {
            if (!holder.id().getNamespace().equals(PromisedEnd.MODID)) return;
            holder.value().display().ifPresent(d -> {
                for (Component c : List.of(d.title(), d.description())) {
                    if (c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t && !lang.has(t.getKey())) missing.add(t.getKey());
                }
            });
        });
        if (!missing.isEmpty()) helper.fail("missing lang keys: " + missing);
        helper.succeed();
    }

    /**
     * The real End generator (the GameTest server's own levels are flat) with our mixin: the main island
     * keeps vanilla's biome, the ring 150-450 blocks out is the Shattered Rim, the void gap beyond it stays vanilla.
     */
    private static void endBiomeSourcePlacesBiomes(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        ChunkGenerator generator = access.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).value()
                .createWorldDimensions().get(LevelStem.END).orElseThrow().generator();
        if (!(generator instanceof NoiseBasedChunkGenerator noise)) {
            helper.fail("the End's generator isn't a noise generator: " + generator);
            return;
        }
        long previous = EndBiomes.seed();
        try {
            EndBiomes.setSeed(424242L);
            RandomState random = RandomState.create(access.lookupOrThrow(Registries.NOISE), 424242L, noise.generatorSettings().value());
            BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(random);
            List<String> wrong = new ArrayList<>();
            java.util.function.BiConsumer<double[], String> expect = (xz, id) -> {
                var holder = biomes.getNoiseBiome((int) Math.floor(xz[0]) >> 2, 64 >> 2, (int) Math.floor(xz[1]) >> 2);
                String got = holder.unwrapKey().map(k -> k.identifier().toString()).orElse("?");
                if (!got.equals(id)) wrong.add(String.format("(%.0f, %.0f) %s, wanted %s", xz[0], xz[1], got, id));
            };
            expect.accept(new double[] {0, 0}, "minecraft:the_end");
            for (int angle = 0; angle < 360; angle += 30) {
                double r = Math.toRadians(angle);
                expect.accept(new double[] {300 * Math.cos(r), 300 * Math.sin(r)}, "promisedend:shattered_rim");
                expect.accept(new double[] {700 * Math.cos(r), 700 * Math.sin(r)}, "minecraft:the_end");
            }
            if (generator.getBiomeSource().possibleBiomes().stream().noneMatch(h -> h.is(PromisedEnd.id("shattered_rim"))))
                wrong.add("the Shattered Rim isn't among the End's possible biomes");
            // a biome file the picker has no entry for (a typo, or one not added to EndRegions) would never generate
            for (var ref : access.lookupOrThrow(Registries.BIOME).listElements().toList()) {
                String id = ref.key().identifier().toString();
                if (!ref.key().identifier().getNamespace().equals(PromisedEnd.MODID)) continue;
                if (java.util.Arrays.stream(EndRegions.Biome.values()).noneMatch(b -> b.id.equals(id))) wrong.add(id + " has no place in the region picker");
                if (generator.getBiomeSource().possibleBiomes().stream().noneMatch(h -> h.is(ref.key()))) wrong.add(id + " isn't among the End's possible biomes");
            }
            if (!wrong.isEmpty()) helper.fail("End biomes: " + wrong);
        } finally {
            EndBiomes.setSeed(previous);
        }
        helper.succeed();
    }

    /**
     * The outer End's shares with the real End generator: on three seeds, 20,000 samples 80 blocks apart between 1,100
     * and 7,300 blocks out, each read back as vanilla's terrain class there and given the biome the region picker means
     * for it. That holds before all our biomes exist (one not yet registered leaves vanilla's). Vanilla's own biomes
     * keep 20-30 %, and each of ours 5-30 % (the small islands are 45 % of the outer End, most of it open void, and the
     * Skyfall Archipelago takes over half of them).
     */
    private static void outerEndShares(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        ChunkGenerator generator = access.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).value()
                .createWorldDimensions().get(LevelStem.END).orElseThrow().generator();
        if (!(generator instanceof NoiseBasedChunkGenerator noise)) {
            helper.fail("the End's generator isn't a noise generator: " + generator);
            return;
        }
        var count = new java.util.EnumMap<EndRegions.Biome, Integer>(EndRegions.Biome.class);
        var terrains = new java.util.EnumMap<EndRegions.Terrain, Integer>(EndRegions.Terrain.class);
        int samples = 0;
        List<String> unknown = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        long previous = EndBiomes.seed();
        try {
            for (long seed : new long[] {424242L, 1234567L, -98765431L}) {
                EndBiomes.setSeed(seed);
                RandomState random = RandomState.create(access.lookupOrThrow(Registries.NOISE), seed, noise.generatorSettings().value());
                BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(random);
                for (int gx = 0; gx < 200; gx++) {
                    for (int gz = 0; gz < 200; gz++) {
                        int x = (gx - 100) * 80, z = (gz - 100) * 80;
                        double d = Math.sqrt((double) x * x + (double) z * z);
                        if (d < 1100 || d > 7300) continue;
                        String id = biomes.getNoiseBiome(x >> 2, 64 >> 2, z >> 2).unwrapKey().map(k -> k.identifier().toString()).orElse("?");
                        seen.add(id);
                        EndRegions.Terrain terrain = null;
                        for (EndRegions.Biome b : EndRegions.Biome.values()) {
                            if (b.id.equals(id)) terrain = EndRegions.terrainOf(b);
                        }
                        if (terrain == null) {
                            if (unknown.size() < 5) unknown.add(id + " at (" + x + ", " + z + ")");
                            continue;
                        }
                        terrains.merge(terrain, 1, Integer::sum);
                        count.merge(EndRegions.pick(seed, x >> 4, z >> 4, terrain, 8000), 1, Integer::sum);
                        samples++;
                    }
                }
            }
        } finally {
            EndBiomes.setSeed(previous);
        }
        if (!unknown.isEmpty()) {
            helper.fail("samples with no outer terrain class: " + unknown);
            return;
        }
        int vanilla = 0;
        StringBuilder mix = new StringBuilder(), shares = new StringBuilder();
        for (var e : terrains.entrySet()) mix.append(String.format(java.util.Locale.ROOT, " %s %.1f %%", e.getKey(), 100.0 * e.getValue() / samples));
        List<String> wrong = new ArrayList<>();
        for (var e : count.entrySet()) {
            double share = (double) e.getValue() / samples;
            shares.append(String.format(java.util.Locale.ROOT, " %s %.1f %%", e.getKey().id, 100 * share));
            if (e.getKey().vanilla()) vanilla += e.getValue();
            else if (share < 0.05 || share > 0.30) wrong.add(String.format(java.util.Locale.ROOT, "%s %.1f %%", e.getKey().id, 100 * share));
        }
        double v = (double) vanilla / samples;
        PromisedEnd.LOGGER.info("[outer_end_shares] {} samples; terrain:{}; biomes:{}; vanilla {}", samples, mix, shares,
                String.format(java.util.Locale.ROOT, "%.1f %%", 100 * v));
        if (v < 0.20 || v > 0.30) wrong.add(String.format(java.util.Locale.ROOT, "vanilla keeps %.1f %%", 100 * v));
        if (samples < 60_000) wrong.add("only " + samples + " samples");
        // every outer biome of ours that is registered comes back from the real generator, not only from the picker
        var registry = access.lookupOrThrow(Registries.BIOME);
        for (EndRegions.Biome b : EndRegions.Biome.values()) {
            if (b.vanilla() || EndRegions.terrainOf(b) == null) continue;
            if (registry.get(ResourceKey.create(Registries.BIOME, Identifier.parse(b.id))).isPresent() && !seen.contains(b.id))
                wrong.add(b.id + " is registered but the End never generates it");
        }
        if (!wrong.isEmpty()) helper.fail("outer End shares: " + wrong);
        helper.succeed();
    }

    /**
     * End Cities spawn in our outer biomes too: every biome of ours that the region picker puts in place of vanilla's
     * highlands or midlands (End Cities' own biomes) is in {@code #minecraft:has_structure/end_city}. Without it they'd
     * be left to the vanilla regions, about a quarter of the End.
     */
    private static void endCitiesInOurBiomes(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        var cities = biomes.getOrThrow(net.minecraft.tags.TagKey.create(Registries.BIOME, Identifier.parse("minecraft:has_structure/end_city")));
        List<String> missing = new ArrayList<>();
        int checked = 0;
        for (EndRegions.Biome b : EndRegions.Biome.values()) {
            var terrain = EndRegions.terrainOf(b);
            if (b.vanilla() || (terrain != EndRegions.Terrain.HIGHLANDS && terrain != EndRegions.Terrain.MIDLANDS)) continue;
            var ref = biomes.get(ResourceKey.create(Registries.BIOME, Identifier.parse(b.id)));
            if (ref.isEmpty()) continue; // not made yet
            checked++;
            if (!cities.contains(ref.get())) missing.add(b.id);
        }
        if (checked == 0) helper.fail("no biome of ours takes highlands or midlands yet");
        if (!missing.isEmpty()) helper.fail("End Cities can't spawn in: " + missing);
        helper.succeed();
    }

    /**
     * Chorus timber and Chorus Moss: the timber is no furnace fuel (oak planks are, as a control), chorus plants hold on
     * the log and the moss, a chorus flower only on the moss (not up a player's build), and the moss drops end stone
     * unless taken with Silk Touch.
     */
    private static void chorusTimberAndMoss(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var fuel = net.minecraft.core.component.DataComponents.COOKING_FUEL;
        if (!new ItemStack(net.minecraft.world.item.Items.OAK_PLANKS).has(fuel)) bad.add("oak planks aren't fuel, so this check can't tell");
        for (var item : List.of(ModBlocks.CHORUS_LOG.get().asItem(), ModBlocks.CHORUS_PLANKS.get().asItem())) {
            if (new ItemStack(item).has(fuel)) bad.add(item + " burns in a furnace");
        }
        BlockPos log = helper.absolutePos(new BlockPos(1, 2, 1)), moss = helper.absolutePos(new BlockPos(3, 2, 1));
        level.setBlock(log, ModBlocks.CHORUS_LOG.get().defaultBlockState(), 3);
        level.setBlock(moss, ModBlocks.CHORUS_MOSS.get().defaultBlockState(), 3);
        var plant = net.minecraft.world.level.block.Blocks.CHORUS_PLANT.defaultBlockState();
        var flower = net.minecraft.world.level.block.Blocks.CHORUS_FLOWER.defaultBlockState();
        if (!plant.canSurvive(level, log.above())) bad.add("a chorus plant won't hold on the log");
        if (!plant.canSurvive(level, moss.above())) bad.add("a chorus plant won't hold on the moss");
        if (!flower.canSurvive(level, moss.above())) bad.add("a chorus flower won't grow on the moss");
        if (flower.canSurvive(level, log.above())) bad.add("a chorus flower grows on the log");
        var table = level.getServer().reloadableRegistries().getLootTable(ModBlocks.CHORUS_MOSS.get().getLootTable().orElseThrow());
        var silk = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        for (var tool : List.of(new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE), silk)) {
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.atCenterOf(moss))
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, tool)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, ModBlocks.CHORUS_MOSS.get().defaultBlockState())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
            var drops = table.getRandomItems(params);
            var want = tool == silk ? ModBlocks.CHORUS_MOSS.get().asItem() : net.minecraft.world.item.Items.END_STONE;
            if (drops.size() != 1 || !drops.get(0).is(want)) bad.add((tool == silk ? "with" : "without") + " Silk Touch the moss drops " + drops);
        }
        if (!bad.isEmpty()) helper.fail("chorus timber and moss: " + bad);
        helper.succeed();
    }

    /**
     * The rest of the chorus timber set: an iron axe in a player's hand strips the log and the wood, keeps the axis and
     * wears as on crimson's stem; each sign and the shelf hold vanilla's block entity (so a sign keeps its text and the
     * shelf its items), a wall sign drops
     * as its sign, every form of the log saws into four planks, every other recipe gives vanilla's count, a player opens
     * the door, trapdoor and gate and presses the button, and nothing in the set burns (fuel, fire or lava).
     */
    private static void chorusWoodSet(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var axis = net.minecraft.world.level.block.state.properties.BlockStateProperties.AXIS;
        var x = net.minecraft.core.Direction.Axis.X;
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var player = helper.makeMockServerPlayerInLevel();
        // the in-level mock is creative, where tools don't wear: the axe goes in a survival player's hand
        var survivor = helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        // crimson's stem first, as the control: it must cost the axe 1, and ours what it costs
        int controlDamage = -1;
        for (var pair : List.of(List.of(net.minecraft.world.level.block.Blocks.CRIMSON_STEM, net.minecraft.world.level.block.Blocks.STRIPPED_CRIMSON_STEM),
                List.of(ModBlocks.CHORUS_LOG.get(), ModBlocks.STRIPPED_CHORUS_LOG.get()),
                List.of(ModBlocks.CHORUS_WOOD.get(), ModBlocks.STRIPPED_CHORUS_WOOD.get()))) {
            level.setBlock(at, pair.get(0).defaultBlockState().setValue(axis, x), 3);
            survivor.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.IRON_AXE));
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), net.minecraft.core.Direction.UP, at, false);
            survivor.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(survivor, hand, hit));
            var now = level.getBlockState(at);
            if (!now.is(pair.get(1)) || now.getValue(axis) != x) bad.add("an axe on " + pair.get(0) + " left " + now);
            int damage = survivor.getMainHandItem().getDamageValue();
            if (controlDamage < 0) {
                controlDamage = damage;
                if (damage != 1) bad.add("stripping crimson's stem cost the axe " + damage + ", not 1, so this check can't tell");
            }
            else if (damage != controlDamage) bad.add("stripping " + pair.get(0) + " cost the axe " + damage + ", crimson's stem " + controlDamage);
        }
        BlockPos sign = helper.absolutePos(new BlockPos(3, 2, 3));
        var signType = net.minecraft.world.level.block.entity.BlockEntityTypes.SIGN;
        var hangingType = net.minecraft.world.level.block.entity.BlockEntityTypes.HANGING_SIGN;
        for (var e : List.of(java.util.Map.entry(ModBlocks.CHORUS_SIGN.get(), signType), java.util.Map.entry(ModBlocks.CHORUS_WALL_SIGN.get(), signType),
                java.util.Map.entry(ModBlocks.CHORUS_HANGING_SIGN.get(), hangingType), java.util.Map.entry(ModBlocks.CHORUS_WALL_HANGING_SIGN.get(), hangingType),
                java.util.Map.entry(ModBlocks.CHORUS_SHELF.get(), net.minecraft.world.level.block.entity.BlockEntityTypes.SHELF))) {
            level.setBlock(sign, e.getKey().defaultBlockState(), 2);
            var be = level.getBlockEntity(sign);
            if (be == null || be.getType() != e.getValue()) bad.add(e.getKey() + " holds " + (be == null ? "no block entity" : be.getType()));
            level.setBlock(sign, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        if (!ModBlocks.CHORUS_WALL_SIGN.get().getLootTable().equals(ModBlocks.CHORUS_SIGN.get().getLootTable())) bad.add("the wall sign has its own drops");
        if (!ModBlocks.CHORUS_WALL_HANGING_SIGN.get().getLootTable().equals(ModBlocks.CHORUS_HANGING_SIGN.get().getLootTable()))
            bad.add("the wall hanging sign has its own drops");
        var recipes = level.getServer().getRecipeManager();
        java.util.function.Function<net.minecraft.world.item.crafting.CraftingInput, ItemStack> craft = input -> recipes
                .getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).map(h -> h.value().assemble(input)).orElse(ItemStack.EMPTY);
        for (var log : List.of(ModBlocks.CHORUS_LOG, ModBlocks.CHORUS_WOOD, ModBlocks.STRIPPED_CHORUS_LOG, ModBlocks.STRIPPED_CHORUS_WOOD)) {
            var out = craft.apply(net.minecraft.world.item.crafting.CraftingInput.of(1, 1, List.of(new ItemStack(log.get()))));
            if (!out.is(ModBlocks.CHORUS_PLANKS.get().asItem()) || out.getCount() != 4) bad.add(log.getId().getPath() + " saws into " + out);
        }
        // every crafted form, by vanilla's patterns (P planks, L stripped log, S stick, C chain), and its count
        var planks = new ItemStack(ModBlocks.CHORUS_PLANKS.get());
        var key = java.util.Map.of('P', planks, 'W', new ItemStack(ModBlocks.CHORUS_LOG.get()), 'L', new ItemStack(ModBlocks.STRIPPED_CHORUS_LOG.get()),
                'S', new ItemStack(net.minecraft.world.item.Items.STICK),
                'C', new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("iron_chain"))));
        record Grid(String[] rows, net.minecraft.world.level.block.Block out, int count) {}
        for (var g : List.of(new Grid(new String[] {"WW", "WW"}, ModBlocks.CHORUS_WOOD.get(), 3),
                new Grid(new String[] {"LL", "LL"}, ModBlocks.STRIPPED_CHORUS_WOOD.get(), 3),
                new Grid(new String[] {"P  ", "PP ", "PPP"}, ModBlocks.CHORUS_STAIRS.get(), 4),
                new Grid(new String[] {"PPP"}, ModBlocks.CHORUS_SLAB.get(), 6),
                new Grid(new String[] {"PSP", "PSP"}, ModBlocks.CHORUS_FENCE.get(), 3),
                new Grid(new String[] {"SPS", "SPS"}, ModBlocks.CHORUS_FENCE_GATE.get(), 1),
                new Grid(new String[] {"PP", "PP", "PP"}, ModBlocks.CHORUS_DOOR.get(), 3),
                new Grid(new String[] {"PPP", "PPP"}, ModBlocks.CHORUS_TRAPDOOR.get(), 2),
                new Grid(new String[] {"PP"}, ModBlocks.CHORUS_PRESSURE_PLATE.get(), 1),
                new Grid(new String[] {"P"}, ModBlocks.CHORUS_BUTTON.get(), 1),
                new Grid(new String[] {"PPP", "PPP", " S "}, ModBlocks.CHORUS_SIGN.get(), 3),
                new Grid(new String[] {"C C", "LLL", "LLL"}, ModBlocks.CHORUS_HANGING_SIGN.get(), 6),
                new Grid(new String[] {"LLL", "   ", "LLL"}, ModBlocks.CHORUS_SHELF.get(), 6))) {
            List<ItemStack> cells = new ArrayList<>();
            for (String row : g.rows()) for (char c : row.toCharArray()) cells.add(c == ' ' ? ItemStack.EMPTY : key.get(c).copy());
            var out = craft.apply(net.minecraft.world.item.crafting.CraftingInput.of(g.rows()[0].length(), g.rows().length, cells));
            if (!out.is(g.out().asItem()) || out.getCount() != g.count()) bad.add(String.join("/", g.rows()) + " makes " + out + ", not " + g.count() + " " + g.out());
        }
        // a player opens the door, the trapdoor and the gate by hand, and the button powers
        player.setItemInHand(hand, ItemStack.EMPTY);
        BlockPos use = helper.absolutePos(new BlockPos(5, 2, 1));
        var open = net.minecraft.world.level.block.state.properties.BlockStateProperties.OPEN;
        for (var block : List.of(ModBlocks.CHORUS_DOOR.get(), ModBlocks.CHORUS_TRAPDOOR.get(), ModBlocks.CHORUS_FENCE_GATE.get(), ModBlocks.CHORUS_BUTTON.get())) {
            level.setBlock(use, block.defaultBlockState(), 2);
            if (block == ModBlocks.CHORUS_DOOR.get()) level.setBlock(use.above(), block.defaultBlockState().setValue(
                    net.minecraft.world.level.block.DoorBlock.HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER), 2);
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(use), net.minecraft.core.Direction.NORTH, use, false);
            player.gameMode.useItemOn(player, level, ItemStack.EMPTY, hand, hit);
            var now = level.getBlockState(use);
            var property = block == ModBlocks.CHORUS_BUTTON.get() ? net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED : open;
            if (!now.is(block) || !now.getValue(property)) bad.add("a player's hand on " + block + " left " + now);
            level.setBlock(use, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(use.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        // nothing in the set burns: no fuel, no flammability, lava doesn't light it. Oak planks are the control
        BlockPos burn = helper.absolutePos(new BlockPos(5, 2, 3));
        var oak = net.minecraft.world.level.block.Blocks.OAK_PLANKS.defaultBlockState();
        if (oak.getFlammability(level, burn, net.minecraft.core.Direction.UP) == 0 || !oak.ignitedByLava()) bad.add("oak planks don't burn, so this check can't tell");
        for (var h : ModBlocks.BLOCKS.getEntries()) {
            var block = h.get();
            if (!h.getId().getPath().contains("chorus_") || block == ModBlocks.CHORUS_MOSS.get()) continue;
            var state = block.defaultBlockState();
            if (state.getFlammability(level, burn, net.minecraft.core.Direction.UP) != 0) bad.add(block + " is flammable");
            if (state.ignitedByLava()) bad.add(block + " catches from lava");
            if (block.asItem() != net.minecraft.world.item.Items.AIR && new ItemStack(block).has(net.minecraft.core.component.DataComponents.COOKING_FUEL))
                bad.add(block + " burns in a furnace");
        }
        if (!bad.isEmpty()) helper.fail("chorus wood set: " + bad);
        helper.succeed();
    }

    // ---- the Lumen Mire ----

    /** A one-block pen at {@code at} (relative): {@code floor} under it, glass round it two high. */
    private static void pen(GameTestHelper helper, BlockPos at, net.minecraft.world.level.block.state.BlockState floor,
                            net.minecraft.world.level.block.state.BlockState inside) {
        helper.setBlock(at.below(), floor);
        helper.setBlock(at, inside);
        for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            helper.setBlock(at.relative(d), net.minecraft.world.level.block.Blocks.GLASS);
            helper.setBlock(at.relative(d).above(), net.minecraft.world.level.block.Blocks.GLASS);
        }
    }

    /**
     * Void-water: a pig dropped over a pool falls through its surface to the pool's floor (no collision) and has
     * Slowness I there, still at tick 90 (past the first 40 ticks: it is topped up), and none 50 ticks after the pool
     * is gone; a pig in the same pen on end stone, the control, stands a block higher with no Slowness. A player's block
     * placed on the floor goes into the pool (replacing it), a Mire Lily used on its surface goes on top; the cursor finds
     * the surface only with a lily in either hand. Flowing water and lava don't wash it away (it isn't in
     * #minecraft:washed_away_by_fluids) and pistons don't move it. It has no item.
     */
    private static void voidWaterSlows(GameTestHelper helper) {
        var endStone = net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState();
        var water = ModBlocks.VOID_WATER.get().defaultBlockState();
        BlockPos pool = new BlockPos(1, 2, 1), dry = new BlockPos(4, 3, 1);
        pen(helper, pool, endStone, water);
        pen(helper, dry, endStone, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        var wet = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, pool.above());
        var control = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, dry);
        List<String> bad = new ArrayList<>();
        if (ModBlocks.VOID_WATER.get().asItem() != net.minecraft.world.item.Items.AIR) bad.add("void-water has an item");
        // a player plants a lily on a second pool's surface, and lays end stone into a third
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        BlockPos lilyPool = new BlockPos(7, 2, 1), fillPool = new BlockPos(10, 2, 1);
        pen(helper, lilyPool, endStone, water);
        pen(helper, fillPool, endStone, water);
        var lily = new ItemStack(ModBlocks.MIRE_LILY.get());
        player.setItemInHand(hand, lily);
        BlockPos at = helper.absolutePos(lilyPool);
        player.gameMode.useItemOn(player, level, lily, hand, new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(at).add(0, 0.375, 0), net.minecraft.core.Direction.UP, at, false));
        if (!level.getBlockState(at).is(ModBlocks.VOID_WATER.get())) bad.add("planting a lily took the pool's place: " + level.getBlockState(at));
        if (!level.getBlockState(at.above()).is(ModBlocks.MIRE_LILY.get())) bad.add("no lily on the pool's surface: " + level.getBlockState(at.above()));
        var stone = new ItemStack(net.minecraft.world.item.Items.END_STONE);
        player.setItemInHand(hand, stone);
        BlockPos floor = helper.absolutePos(fillPool.below());
        player.gameMode.useItemOn(player, level, stone, hand, new net.minecraft.world.phys.BlockHitResult(
                net.minecraft.world.phys.Vec3.atCenterOf(floor).add(0, 0.5, 0), net.minecraft.core.Direction.UP, floor, false));
        if (!level.getBlockState(floor.above()).is(net.minecraft.world.level.block.Blocks.END_STONE)) bad.add("end stone laid on the pool's floor left " + level.getBlockState(floor.above()));
        // the cursor: the surface with a lily in either hand, nothing with an empty hand
        var shape = ModBlocks.VOID_WATER.get().defaultBlockState();
        player.setItemInHand(hand, ItemStack.EMPTY);
        if (!shape.getShape(level, at, net.minecraft.world.phys.shapes.CollisionContext.of(player)).isEmpty()) bad.add("an empty hand sees the surface");
        player.setItemInHand(hand, lily.copy());
        if (shape.getShape(level, at, net.minecraft.world.phys.shapes.CollisionContext.of(player)).isEmpty()) bad.add("a lily in the main hand doesn't see the surface");
        player.setItemInHand(hand, ItemStack.EMPTY);
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, lily.copy());
        if (shape.getShape(level, at, net.minecraft.world.phys.shapes.CollisionContext.of(player)).isEmpty()) bad.add("a lily in the off hand doesn't see the surface");
        if (shape.getPistonPushReaction() != net.minecraft.world.level.material.PushReaction.IMMOVEABLE) bad.add("pistons move void-water: " + shape.getPistonPushReaction());
        // unbreakable and blast-proof, as bedrock: a player holding a lily targets the surface and mustn't mine it
        if (shape.getDestroySpeed(level, at) != -1.0F) bad.add("void-water can be mined (hardness " + shape.getDestroySpeed(level, at) + ")");
        if (shape.getBlock().getExplosionResistance() < 3600000.0F) bad.add("explosions break void-water (resistance " + shape.getBlock().getExplosionResistance() + ")");
        if (!shape.is(net.minecraft.tags.BlockTags.WITHER_IMMUNE) || !shape.is(net.minecraft.tags.BlockTags.DRAGON_IMMUNE)) bad.add("the Wither or the dragon can remove void-water");
        // 26.3's fluids wash away only what is in #minecraft:washed_away_by_fluids (FlowingFluid.canHoldAnyFluid); short
        // grass is, the control
        var washed = net.minecraft.tags.BlockTags.WASHED_AWAY_BY_FLUIDS;
        if (!net.minecraft.world.level.block.Blocks.SHORT_GRASS.defaultBlockState().is(washed)) bad.add("short grass isn't washed away, so this can't tell");
        if (shape.is(washed) || shape.getBlock() instanceof net.minecraft.world.level.block.LiquidBlockContainer) bad.add("flowing water and lava wash void-water away");
        helper.runAtTickTime(40, () -> {
            double wetY = wet.getY() - helper.absolutePos(pool).getY(), dryY = control.getY() - helper.absolutePos(dry).getY();
            if (Math.abs(wetY) > 0.01) bad.add("the pig in the pool stands at " + wetY + " (the pool's floor is 0)");
            if (Math.abs(dryY) > 0.01) bad.add("the control pig stands at " + dryY);
        });
        helper.runAtTickTime(90, () -> {
            var slow = wet.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);
            if (slow == null || slow.getAmplifier() != 0) bad.add("at tick 90 the pig in the pool has " + slow);
            if (control.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)) bad.add("the control pig is slowed");
            // the pool goes: nothing tops the Slowness up
            helper.setBlock(pool, net.minecraft.world.level.block.Blocks.AIR);
        });
        helper.runAtTickTime(140, () -> {
            if (wet.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)) bad.add("50 ticks out of the water the pig is still slowed");
            if (!bad.isEmpty()) helper.fail("void-water: " + bad);
            helper.succeed();
        });
    }

    /**
     * The Mire Lily holds on void-water only (not end stone, Lumen Moss or vanilla water), gives light 7, and pops off,
     * dropping itself, when its pool cell is filled. Bone meal on it puts one more lily on open void-water two blocks
     * off (with only that ring open) and never three off, and does nothing once the ring is full.
     */
    private static void mireLilyRules(GameTestHelper helper) {
        var level = helper.getLevel();
        var lily = ModBlocks.MIRE_LILY.get();
        List<String> bad = new ArrayList<>();
        var grounds = java.util.Map.of(ModBlocks.VOID_WATER.get().defaultBlockState(), true,
                net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), false,
                ModBlocks.LUMEN_MOSS.get().defaultBlockState(), false,
                net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(), false);
        int x = 1;
        for (var e : grounds.entrySet()) {
            BlockPos at = helper.absolutePos(new BlockPos(x, 2, 1));
            level.setBlock(at, e.getKey(), 2);
            if (lily.defaultBlockState().canSurvive(level, at.above()) != e.getValue()) bad.add("a lily on " + e.getKey() + (e.getValue() ? " won't hold" : " holds"));
            level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            x += 2;
        }
        if (lily.defaultBlockState().getLightEmission() != 7) bad.add("a lily gives light " + lily.defaultBlockState().getLightEmission());
        // filling a lily's cell pops it, and it drops
        BlockPos cell = helper.absolutePos(new BlockPos(10, 2, 1));
        level.setBlock(cell, ModBlocks.VOID_WATER.get().defaultBlockState(), 2);
        level.setBlock(cell.above(), lily.defaultBlockState(), 2);
        level.setBlock(cell, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 3);
        if (!level.getBlockState(cell.above()).isAir()) bad.add("a lily stays on its filled cell: " + level.getBlockState(cell.above()));
        if (level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(cell.above()).inflate(1.5),
                e -> e.getItem().is(lily.asItem())).isEmpty()) bad.add("a popped lily dropped nothing");
        // a lily on void-water; round it a ring of end stone, then a ring of open void-water two off, then one three
        // off, all walled in glass: bone meal fills the ring two off one lily at a time, then stops
        BlockPos centre = helper.absolutePos(new BlockPos(5, 2, 6));
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-4, 0, -4), centre.offset(4, 0, 4))) {
            int ring = Math.max(Math.abs(p.getX() - centre.getX()), Math.abs(p.getZ() - centre.getZ()));
            var state = ring == 1 ? net.minecraft.world.level.block.Blocks.END_STONE : ring == 4 ? net.minecraft.world.level.block.Blocks.GLASS : ModBlocks.VOID_WATER.get();
            level.setBlock(p, state.defaultBlockState(), 2);
            level.setBlock(p.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        level.setBlock(centre.above(), lily.defaultBlockState(), 2);
        for (int n = 1; n <= 17; n++) {
            boolean grew = net.minecraft.world.item.BoneMealItem.growCrop(new ItemStack(net.minecraft.world.item.Items.BONE_MEAL), level, centre.above());
            int two = 0, three = 0;
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-3, 1, -3), centre.offset(3, 1, 3))) {
                if (!level.getBlockState(p).is(lily)) continue;
                int ring = Math.max(Math.abs(p.getX() - centre.getX()), Math.abs(p.getZ() - centre.getZ()));
                if (ring == 2) two++;
                if (ring == 3) three++;
            }
            int want = Math.min(n, 16);
            if (two != want) bad.add("after " + n + " bone meal " + two + " lilies two off, not " + want);
            if (three != 0) bad.add("after " + n + " bone meal a lily three off");
            if (grew != (n <= 16)) bad.add("bone meal " + n + (grew ? " took" : " didn't take"));
        }
        if (!bad.isEmpty()) helper.fail("Mire Lily: " + bad);
        helper.succeed();
    }

    /**
     * Lumen Moss drops end stone and, one time in five, a Lumen (0.3, 0.45 and 0.7 with Fortune I-III), over 400 seeded rolls
     * each; with Silk Touch the moss alone. Endermen don't pick it up (it walls the pools).
     */
    private static void lumenMossDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        var moss = ModBlocks.LUMEN_MOSS.get();
        List<String> bad = new ArrayList<>();
        var table = level.getServer().reloadableRegistries().getLootTable(moss.getLootTable().orElseThrow());
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var silk = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        silk.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        var fortune = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        fortune.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
        var plain = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        var fortune1 = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        fortune1.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 1);
        var fortune2 = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        fortune2.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 2);
        // tool, the fewest and the most Lumen in 400 rolls (p 0.2: 80 expected; 0.3: 120; 0.45: 180; 0.7: 280)
        record Case(String name, ItemStack tool, int min, int max) {}
        // the bounds are 2.5 standard deviations either side
        for (var c : List.of(new Case("a pickaxe", plain, 60, 100), new Case("Fortune I", fortune1, 97, 143), new Case("Fortune II", fortune2, 155, 205),
                new Case("Fortune III", fortune, 255, 305), new Case("Silk Touch", silk, 0, 0))) {
            int lumen = 0;
            for (int i = 0; i < 400; i++) {
                var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, c.tool())
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, moss.defaultBlockState())
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
                // seeds mixed: consecutive seeds give correlated first rolls
                var drops = table.getRandomItems(params, net.minecraft.util.Mth.murmurHash3Mixer(5000 + i));
                var base = c.tool() == silk ? moss.asItem() : net.minecraft.world.item.Items.END_STONE;
                if (drops.stream().filter(st -> st.is(base)).count() != 1) {
                    bad.add("with " + c.name() + " roll " + i + " gave " + drops);
                    break;
                }
                lumen += (int) drops.stream().filter(st -> st.is(ModItems.LUMEN.get())).mapToInt(ItemStack::getCount).sum();
            }
            PromisedEnd.LOGGER.info("[lumen_moss_drops] {}: {} Lumen in 400", c.name(), lumen);
            if (lumen < c.min() || lumen > c.max()) bad.add("with " + c.name() + " " + lumen + " Lumen in 400 (" + c.min() + "-" + c.max() + ")");
        }
        // blown up by a radius-6 blast, each drop survives one time in six: the end stone in 400 about 67 (48-85), the
        // Lumen 0.2 x 1/6 of them, about 13 (4-22); without the explosion conditions 400 and 80
        int stone = 0, blownLumen = 0;
        for (int i = 0; i < 400; i++) {
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, ItemStack.EMPTY)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, moss.defaultBlockState())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.EXPLOSION_RADIUS, 6.0F)
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
            var drops = table.getRandomItems(params, net.minecraft.util.Mth.murmurHash3Mixer(9000 + i));
            stone += (int) drops.stream().filter(st -> st.is(net.minecraft.world.item.Items.END_STONE)).count();
            blownLumen += (int) drops.stream().filter(st -> st.is(ModItems.LUMEN.get())).mapToInt(ItemStack::getCount).sum();
        }
        PromisedEnd.LOGGER.info("[lumen_moss_drops] a radius-6 blast: {} end stone, {} Lumen in 400", stone, blownLumen);
        if (stone < 48 || stone > 85) bad.add("a radius-6 blast left " + stone + " end stone in 400 (48-85)");
        if (blownLumen < 4 || blownLumen > 22) bad.add("a radius-6 blast left " + blownLumen + " Lumen in 400 (4-22)");
        if (moss.defaultBlockState().is(net.minecraft.tags.BlockTags.ENDERMAN_HOLDABLE)) bad.add("endermen pick it up, out of the pools' walls");
        if (!ModBlocks.CHORUS_MOSS.get().defaultBlockState().is(net.minecraft.tags.BlockTags.ENDERMAN_HOLDABLE)) bad.add("Chorus Moss isn't holdable either, so this can't tell");
        if (!bad.isEmpty()) helper.fail("Lumen Moss: " + bad);
        helper.succeed();
    }

    /**
     * The Mire's pool feature as configured, over ten seeds on a wide floor and ten on a 7 by 7 islet: every void-water
     * cell has solid ground under it and ground or water on all four sides (nothing spills off the islet), a lily only
     * ever on the water, and over the wide floors at least 4 cells a pool, a lily and a Lumen Moss rim. On the wide
     * floors a second pool goes 4 blocks off the first, and the two join: no single block of ground left between water
     * on both sides. On obsidian, which isn't pool ground, nothing. On ten terraces (the east half a block higher) a pool
     * on the upper step and then one at the step: the second settles below and cuts into the upper step, every cell at
     * both heights still walled in, and no rim moss with anything over it. On ten floors four layers deep two pools 4
     * apart join at one level, with no ridge. In a chunk an End City has claimed, no pool.
     */
    private static void mirePoolsWalled(GameTestHelper helper) {
        var level = helper.getLevel();
        var feature = grove(level, "mire_pool");
        var water = ModBlocks.VOID_WATER.get();
        var generator = level.getChunkSource().getGenerator();
        var endStone = net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState();
        List<String> bad = new ArrayList<>();
        int y = level.getMaxY() - 60, lilies = 0, rims = 0, wide = 0, walls = 0;
        for (int n = 0; n < 21; n++) {
            BlockPos centre = new BlockPos(4000 + n * 64, y, 2000);
            int r = n < 10 ? 12 : 3;
            var ground = n == 20 ? net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState() : endStone;
            // two layers, so the pool has a floor under it
            floor(level, centre, r, ground);
            floor(level, centre.below(), r, ground);
            boolean placed = feature.place(level, generator, net.minecraft.util.RandomSource.create(700 + n), centre);
            if (n < 10) feature.place(level, generator, net.minecraft.util.RandomSource.create(800 + n), centre.offset(4, 0, 1));
            if (n == 20) {
                if (placed) bad.add("a pool in obsidian");
                clear(level, centre);
                continue;
            }
            int cells = 0;
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-16, -2, -16), centre.offset(16, 4, 16))) {
                var state = level.getBlockState(p);
                if (state.is(ModBlocks.MIRE_LILY.get())) {
                    lilies++;
                    if (!level.getBlockState(p.below()).is(water)) bad.add("a lily off the water at " + p);
                }
                if (state.is(ModBlocks.LUMEN_MOSS.get())) rims++;
                if (!state.is(water)) continue;
                cells++;
                if (p.getY() != y - 1) bad.add("void-water out of the ground's layer at " + p);
                if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) bad.add("void-water over nothing at " + p);
                for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    BlockPos q = p.relative(d);
                    var side = level.getBlockState(q);
                    if (!side.is(water) && !side.isFaceSturdy(level, q, d.getOpposite())) bad.add("void-water open to the " + d + " at " + p);
                    // a wall one block thick between two pools: they should have joined
                    if (n < 10 && !side.is(water) && level.getBlockState(q.relative(d)).is(water)) walls++;
                }
            }
            if (n < 10) {
                if (placed) wide++;
                if (placed && cells < com.panda.promisedend.worldgen.MirePoolFeature.MIN_CELLS) bad.add(cells + " cells at " + centre);
            }
            PromisedEnd.LOGGER.info("[mire_pools_walled] {} (floor {} across): placed {}, {} cells", centre, 2 * r + 1, placed, cells);
            clear(level, centre);
        }
        // ten terraces, the east half a block higher: a pool on the upper step first, then one at the step, which settles
        // on the lower level and cuts into the upper one, never into the first pool's wall
        int cuts = 0, upper = 0;
        for (int n = 0; n < 10; n++) {
            BlockPos centre = new BlockPos(4000 + n * 64, y, 2400);
            floor(level, centre, 12, endStone);
            floor(level, centre.below(), 12, endStone);
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(1, 0, -12), centre.offset(12, 0, 12))) level.setBlock(p, endStone, 2);
            feature.place(level, generator, net.minecraft.util.RandomSource.create(1100 + n), centre.offset(5, 0, 0));
            feature.place(level, generator, net.minecraft.util.RandomSource.create(1200 + n), centre);
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-16, -2, -16), centre.offset(16, 4, 16))) {
                var state = level.getBlockState(p);
                if (state.is(ModBlocks.LUMEN_MOSS.get()) && !level.getBlockState(p.above()).isAir()) bad.add("moss buried under " + level.getBlockState(p.above()) + " at " + p);
                if (!state.is(water)) continue;
                if (p.getY() == y) upper++;
                if (p.getY() == y - 1 && p.getX() > centre.getX()) cuts++;
                if (!level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)) bad.add("terrace void-water over nothing at " + p);
                for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    BlockPos q = p.relative(d);
                    var side = level.getBlockState(q);
                    if (!side.is(water) && !side.isFaceSturdy(level, q, d.getOpposite())) bad.add("terrace void-water open to the " + d + " at " + p);
                }
            }
            clear(level, centre);
        }
        PromisedEnd.LOGGER.info("[mire_pools_walled] terraces: {} cells on the upper step, {} cut into it from below", upper, cuts);
        // ten floors four layers deep, two pools 4 apart as on the wide floors: the second takes the first's level (the
        // water's own surface, not a block below it), so the pools join at one level with no ridge between
        int deep = 0, deepWalls = 0;
        for (int n = 0; n < 10; n++) {
            BlockPos centre = new BlockPos(4000 + n * 64, y, 2800);
            for (int k = 0; k < 4; k++) floor(level, centre.below(k), 12, endStone);
            feature.place(level, generator, net.minecraft.util.RandomSource.create(800 + n), centre);
            feature.place(level, generator, net.minecraft.util.RandomSource.create(700 + n), centre.offset(4, 0, 1));
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-16, -4, -16), centre.offset(16, 4, 16))) {
                if (!level.getBlockState(p).is(water)) continue;
                deep++;
                if (p.getY() != y - 1) bad.add("deep-floor void-water a level down at " + p);
                for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                    BlockPos q = p.relative(d);
                    if (!level.getBlockState(q).is(water) && level.getBlockState(q.relative(d)).is(water)) deepWalls++;
                }
            }
            clear(level, centre);
            for (int k = 2; k < 4; k++) floor(level, centre.below(k), 12, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        PromisedEnd.LOGGER.info("[mire_pools_walled] deep floors: {} cells, {} one-block walls", deep, deepWalls / 2);
        // an End City's claim on the chunk with nothing yet built: no pool (the same floor and seed make one unclaimed)
        {
            BlockPos centre = new BlockPos(4000, y, 3200);
            floor(level, centre, 12, endStone);
            floor(level, centre.below(), 12, endStone);
            var city = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.END_CITY);
            var chunk = level.getChunk(centre);
            var before = new java.util.HashMap<>(chunk.getAllReferences());
            chunk.addReferenceForStructure(city, net.minecraft.world.level.ChunkPos.pack(centre.getX() >> 4, centre.getZ() >> 4));
            boolean claimed = feature.place(level, generator, net.minecraft.util.RandomSource.create(700), centre);
            chunk.setAllReferences(before);
            int cells = 0;
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-16, -2, -16), centre.offset(16, 4, 16))) if (level.getBlockState(p).is(water)) cells++;
            if (claimed || cells > 0) bad.add(cells + " void-water in an End City's chunk");
            boolean free = feature.place(level, generator, net.minecraft.util.RandomSource.create(700), centre);
            if (!free) bad.add("no pool on the unclaimed control floor, so the claim case can't tell");
            clear(level, centre);
        }
        if (deep == 0) bad.add("no pools on the deep floors, so they can't tell");
        if (deepWalls > 0) bad.add(deepWalls / 2 + " one-block walls between pools on the deep floors");
        if (upper == 0) bad.add("no pool on the upper steps, so the terraces can't tell");
        if (cuts == 0) bad.add("no pool cut into the upper step, so the terraces can't tell");
        if (wide < 9) bad.add("only " + wide + " of 10 pools on the wide floors");
        if (walls > 0) bad.add(walls / 2 + " one-block walls between pools that should have joined");
        if (lilies == 0) bad.add("no lilies in 20 pools");
        if (rims == 0) bad.add("no Lumen Moss rim in 20 pools");
        if (!bad.isEmpty()) helper.fail("Mire pools: " + bad);
        helper.succeed();
    }

    // ---- the Resonant Spires ----

    /**
     * The spire feature as configured, over ten seeds on open end stone: a formation every time, its tallest column
     * 10-30 blocks over the floor, budding Resonite in it, and every cluster standing on crystal (its facing away from a
     * Resonite or budding block), and nothing written more than {@code ResoniteSpireFeature.REACH} blocks off on either
     * axis. On obsidian, which isn't spire ground, nothing; inside a purpur ring round the origin (an End City's wall),
     * nothing, and the ring whole; under a purpur overhang 34 up (above any spire) that starts a block east of
     * the origin, over the first column's side, nothing; in a chunk an End City has claimed but not yet built in,
     * nothing.
     */
    private static void resoniteSpiresGrow(GameTestHelper helper) {
        var level = helper.getLevel();
        var feature = grove(level, "resonite_spire");
        var generator = level.getChunkSource().getGenerator();
        var crystal = ModBlocks.RESONITE_BLOCK.get();
        var budding = ModBlocks.BUDDING_RESONITE.get();
        var cluster = ModBlocks.RESONITE_CLUSTER.get();
        List<String> bad = new ArrayList<>();
        int y = level.getMaxY() - 60;
        for (int n = 0; n < 11; n++) {
            BlockPos centre = new BlockPos(6000 + n * 64, y, 2000);
            var ground = n == 10 ? net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState() : net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState();
            floor(level, centre, 14, ground);
            boolean placed = feature.place(level, generator, net.minecraft.util.RandomSource.create(900 + n), centre);
            if (n == 10) {
                if (placed) bad.add("spires on obsidian");
                clear(level, centre);
                continue;
            }
            int top = -1, crystals = 0, buds = 0, clusters = 0, reach = 0;
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-16, 0, -16), centre.offset(16, 40, 16))) {
                var state = level.getBlockState(p);
                if (state.is(crystal) || state.is(budding) || state.is(cluster)) reach = Math.max(reach, Math.max(Math.abs(p.getX() - centre.getX()), Math.abs(p.getZ() - centre.getZ())));
                if (state.is(crystal) || state.is(budding)) {
                    crystals++;
                    if (state.is(budding)) buds++;
                    top = Math.max(top, p.getY() - y + 1);
                } else if (state.is(cluster)) {
                    clusters++;
                    var facing = state.getValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING);
                    var base = level.getBlockState(p.relative(facing.getOpposite()));
                    if (!base.is(crystal) && !base.is(budding)) bad.add("a cluster on " + base + " at " + p);
                }
            }
            PromisedEnd.LOGGER.info("[resonite_spires_grow] {}: {} tall, {} crystal blocks, {} budding, {} clusters", centre, top, crystals, buds, clusters);
            if (!placed) bad.add("nothing grew at " + centre);
            if (top < 10 || top > 30) bad.add("the tallest column " + top + " tall at " + centre);
            if (buds == 0) bad.add("no budding Resonite at " + centre);
            if (clusters == 0) bad.add("no clusters at " + centre);
            if (reach > com.panda.promisedend.worldgen.ResoniteSpireFeature.REACH) bad.add("crystal " + reach + " blocks off at " + centre);
            clear(level, centre);
        }
        // the codec's height is the tallest column's height: 10, 30 and the codec's 32, each over three seeds
        var spire = (com.panda.promisedend.worldgen.ResoniteSpireFeature) feature;
        for (int h : new int[] {10, 30, 32}) {
            var fixed = new com.panda.promisedend.worldgen.ResoniteSpireFeature(spire.crystal(), spire.budding(), spire.cluster(),
                    net.minecraft.util.valueproviders.ConstantInt.of(h), spire.columns(), spire.buddingChance(), spire.clusterChance(), false);
            for (int k = 0; k < 3; k++) {
                BlockPos centre = new BlockPos(6000 + (16 + k) * 64, y, 2000 + h * 64);
                floor(level, centre, 14, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
                fixed.place(level, generator, net.minecraft.util.RandomSource.create(990 + k), centre);
                int top = -1, reach = 0;
                for (BlockPos p : BlockPos.betweenClosed(centre.offset(-16, 0, -16), centre.offset(16, 40, 16))) {
                    var state = level.getBlockState(p);
                    if (state.is(crystal) || state.is(budding)) top = Math.max(top, p.getY() - y + 1);
                    if (state.is(crystal) || state.is(budding) || state.is(cluster)) reach = Math.max(reach, Math.max(Math.abs(p.getX() - centre.getX()), Math.abs(p.getZ() - centre.getZ())));
                }
                if (top != h) bad.add("height " + h + " grew a spire " + top + " tall (seed " + k + ")");
                if (reach > com.panda.promisedend.worldgen.ResoniteSpireFeature.REACH) bad.add("height " + h + ": crystal " + reach + " blocks off");
                clear(level, centre);
            }
        }
        // keep out: a purpur ring round the origin (the formation meets it), then a purpur overhang 34 up, over the
        // tallest spire, from a block east of the origin (the formation is under it), then an End City's claim on the
        // origin's chunk with nothing yet built (the chunk's structure reference, as a city's own chunks have before
        // their pieces go down)
        var city = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.END_CITY);
        for (int n = 0; n < 3; n++) {
            BlockPos centre = new BlockPos(6000 + (12 + n) * 64, y, 2000);
            floor(level, centre, 14, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            List<BlockPos> obstacle = new ArrayList<>();
            if (n == 0) {
                for (BlockPos p : BlockPos.betweenClosed(centre.offset(-1, 0, -1), centre.offset(1, 5, 1))) {
                    if (Math.max(Math.abs(p.getX() - centre.getX()), Math.abs(p.getZ() - centre.getZ())) == 1) obstacle.add(p.immutable());
                }
            } else if (n == 1) {
                for (BlockPos p : BlockPos.betweenClosed(centre.offset(1, 34, -14), centre.offset(14, 34, 14))) obstacle.add(p.immutable());
            }
            for (BlockPos p : obstacle) level.setBlock(p, net.minecraft.world.level.block.Blocks.PURPUR_BLOCK.defaultBlockState(), 2);
            var chunk = level.getChunk(centre);
            var before = new java.util.HashMap<>(chunk.getAllReferences());
            if (n == 2) chunk.addReferenceForStructure(city, net.minecraft.world.level.ChunkPos.pack(centre.getX() >> 4, centre.getZ() >> 4));
            boolean placed = feature.place(level, generator, net.minecraft.util.RandomSource.create(950 + n), centre);
            if (n == 2) chunk.setAllReferences(before);
            int crystals = 0;
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-16, 0, -16), centre.offset(16, 40, 16))) {
                if (level.getBlockState(p).is(crystal) || level.getBlockState(p).is(budding) || level.getBlockState(p).is(cluster)) crystals++;
            }
            String where = n == 0 ? "inside a purpur ring" : n == 1 ? "under a purpur overhang" : "in an End City's chunk";
            if (placed || crystals > 0) bad.add(crystals + " Resonite " + where);
            if (obstacle.stream().anyMatch(p -> !level.getBlockState(p).is(net.minecraft.world.level.block.Blocks.PURPUR_BLOCK))) bad.add("the purpur " + where + " was broken");
            clear(level, centre);
        }
        if (!bad.isEmpty()) helper.fail("Resonite spires: " + bad);
        helper.succeed();
    }

    /**
     * The chime draws the nearest three Endermen within 12 blocks (4, 6 and 8 off) onto the player, not a fourth 10 off
     * (the cap), not one 15.6 off (the radius), and not a pig 3 off (not in #promisedend:hears_resonite). A survival
     * player's real break (the server's destroyBlock) of a cluster, and of the block a cluster stands on, draws onto that
     * player; a machine's fake player in survival is no breaker (and no real player is near, so none is drawn); a
     * creative player's real break 2 blocks off rings and draws none. A survival player's real break in a world is the
     * `biomes` scenario's `ring` step.
     */
    private static void resoniteClusterChimes(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var cluster = ModBlocks.RESONITE_CLUSTER.get().defaultBlockState();
        BlockPos at = helper.absolutePos(new BlockPos(0, 2, 0)), also = helper.absolutePos(new BlockPos(0, 2, 6));
        level.setBlock(at, cluster, 3);
        // held still (no AI), so the check reads where the chime found them
        java.util.function.Function<BlockPos, net.minecraft.world.entity.monster.Enderman> enderman = rel -> {
            var e = helper.spawn(net.minecraft.world.entity.EntityTypes.ENDERMAN, rel);
            e.setNoAi(true);
            return e;
        };
        List<net.minecraft.world.entity.monster.Enderman> within = new ArrayList<>();
        for (int x : new int[] {4, 6, 8, 10}) within.add(enderman.apply(new BlockPos(x, 2, 0)));
        var far = enderman.apply(new BlockPos(11, 2, 11));
        var pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(0, 2, 3));
        pig.setNoAi(true);
        var survivor = helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        int drawn = com.panda.promisedend.block.ResoniteClusterBlock.chime(level, at, survivor);
        if (drawn != 3) bad.add("the chime drew " + drawn + ", not 3");
        for (int k = 0; k < 4; k++) {
            boolean on = within.get(k).getTarget() == survivor;
            if (on != (k < 3)) bad.add("the Enderman " + (4 + 2 * k) + " blocks off " + (on ? "was" : "wasn't") + " drawn");
        }
        if (far.getTarget() != null) bad.add("the Enderman 15.6 blocks off targets " + far.getTarget());
        if (pig.getTarget() != null) bad.add("the pig targets " + pig.getTarget());
        within.forEach(e -> e.setTarget(null));
        // a survival player's real break (the server's destroyBlock) of a cluster, then of the Resonite Block another
        // stands on: each draws onto them, though they aren't in the level for a nearest-player search to find
        BlockPos support = helper.absolutePos(new BlockPos(2, 2, 2));
        for (String what : List.of("the cluster", "the block under a cluster")) {
            BlockPos broken = what.equals("the cluster") ? also : support;
            if (broken == support) {
                level.setBlock(support, ModBlocks.RESONITE_BLOCK.get().defaultBlockState(), 3);
                level.setBlock(support.above(), cluster, 3);
            } else {
                level.setBlock(also, cluster, 3);
            }
            ((net.minecraft.server.level.ServerPlayer) survivor).gameMode.destroyBlock(broken);
            if (!level.getBlockState(broken == support ? support.above() : also).isAir()) bad.add("breaking " + what + " left the cluster");
            for (int k = 0; k < 4; k++) {
                boolean on = within.get(k).getTarget() == survivor;
                if (on != (k < 3)) bad.add("after the survival player broke " + what + " the Enderman " + (4 + 2 * k) + " blocks off " + (on ? "was" : "wasn't") + " drawn");
            }
            within.forEach(e -> e.setTarget(null));
        }
        // a machine's fake player in survival breaks one: it isn't the breaker, and with no real survival player near, none
        // is drawn (taken as the breaker it would draw three onto itself)
        level.setBlock(also, cluster, 3);
        var machine = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        machine.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        machine.gameMode.destroyBlock(also);
        // the choice itself, inside a break as the server brackets it: the fake player isn't taken (vanilla's targeting
        // refuses one anyway, so the Endermen above can't tell), a survival player is
        com.panda.promisedend.block.ResoniteClusterBlock.breaking(machine);
        var forMachine = com.panda.promisedend.block.ResoniteClusterBlock.chimeTarget(level, also);
        com.panda.promisedend.block.ResoniteClusterBlock.breaking(survivor);
        var forSurvivor = com.panda.promisedend.block.ResoniteClusterBlock.chimeTarget(level, also);
        com.panda.promisedend.block.ResoniteClusterBlock.breaking(null);
        if (forMachine != null) bad.add("a fake player's break draws onto " + forMachine);
        if (forSurvivor != survivor) bad.add("a survival player's break draws onto " + forSurvivor);
        if (!level.getBlockState(also).isAir()) bad.add("the fake player's break left " + level.getBlockState(also));
        for (var e : within) if (e.getTarget() != null) bad.add("a fake player's break drew an Enderman onto " + e.getTarget());
        within.forEach(e -> e.setTarget(null));
        // a creative player 2 blocks off breaks one for real (the server's own destroyBlock)
        level.setBlock(also, cluster, 3);
        var creative = helper.makeMockServerPlayerInLevel();
        creative.snapTo(also.getX() + 2.5, also.getY(), also.getZ() + 0.5);
        creative.gameMode.destroyBlock(also);
        if (!level.getBlockState(also).isAir()) bad.add("the creative break left " + level.getBlockState(also));
        for (var e : within) if (e.getTarget() != null) bad.add("a creative player's break drew an Enderman onto " + e.getTarget());
        if (!bad.isEmpty()) helper.fail("Resonite chime: " + bad);
        helper.succeed();
    }

    /**
     * Budding Resonite grows a cluster on an open face, facing out from it, and on no face that isn't open: with only its
     * top open, 4000 seeded random ticks grow 19-48 clusters (one tick in 20, then one face in 6: 33 expected, 2.5
     * standard deviations either side; one in 5 would give 133, one in 50 give 13), each taken away when it grows. It has
     * no item and no loot table.
     */
    private static void buddingResoniteGrows(GameTestHelper helper) {
        var level = helper.getLevel();
        var budding = ModBlocks.BUDDING_RESONITE.get();
        var cluster = ModBlocks.RESONITE_CLUSTER.get();
        List<String> bad = new ArrayList<>();
        BlockPos at = helper.absolutePos(new BlockPos(3, 3, 3));
        level.setBlock(at, budding.defaultBlockState(), 3);
        // every face shut but the top
        for (var d : net.minecraft.core.Direction.values()) {
            if (d != net.minecraft.core.Direction.UP) level.setBlock(at.relative(d), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        }
        level.setBlock(at.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        var random = net.minecraft.util.RandomSource.create(11);
        int grew = 0;
        for (int i = 0; i < 4000; i++) {
            budding.defaultBlockState().randomTick(level, at, random);
            var grown = level.getBlockState(at.above());
            if (!grown.is(cluster)) continue;
            grew++;
            if (grown.getValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING) != net.minecraft.core.Direction.UP) bad.add("the cluster faces " + grown);
            level.setBlock(at.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        PromisedEnd.LOGGER.info("[budding_resonite_grows] {} clusters in 4000 random ticks", grew);
        if (grew < 19 || grew > 48) bad.add(grew + " clusters in 4000 random ticks (19-48)");
        for (var d : net.minecraft.core.Direction.values()) {
            if (d != net.minecraft.core.Direction.UP && !level.getBlockState(at.relative(d)).is(net.minecraft.world.level.block.Blocks.STONE)) bad.add("it grew into the " + d + " face");
        }
        if (budding.asItem() != net.minecraft.world.item.Items.AIR) bad.add("budding Resonite has an item");
        if (budding.getLootTable().isPresent()) bad.add("budding Resonite has a loot table");
        // the ticks above are called by hand: in a world only a block that takes random ticks gets them
        if (!budding.defaultBlockState().isRandomlyTicking()) bad.add("budding Resonite takes no random ticks, so it never grows in a world");
        if (!bad.isEmpty()) helper.fail("budding Resonite: " + bad);
        helper.succeed();
    }

    /**
     * A Resonite Cluster drops 4 shards to a pickaxe (iron, Voidstone's and Resonite's: our pickaxes are in
     * {@code #minecraft:cluster_max_harvestables}, as are amethyst's to them) and 2 to a bare hand, itself to Silk Touch; four shards make a
     * Resonite Block, which drops itself; Resonite rings as amethyst does (#minecraft:crystal_sound_blocks).
     */
    private static void resoniteDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var cluster = ModBlocks.RESONITE_CLUSTER.get();
        var table = level.getServer().reloadableRegistries().getLootTable(cluster.getLootTable().orElseThrow());
        var silk = new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        record Case(String name, ItemStack tool, net.minecraft.world.item.Item item, int count) {}
        for (var c : List.of(new Case("a pickaxe", new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE), ModItems.RESONITE_SHARD.get(), 4),
                new Case("a bare hand", ItemStack.EMPTY, ModItems.RESONITE_SHARD.get(), 2), new Case("Silk Touch", silk, cluster.asItem(), 1),
                new Case("Voidstone's pickaxe", new ItemStack(ModItems.VOIDSTONE_PICKAXE.get()), ModItems.RESONITE_SHARD.get(), 4),
                new Case("Resonite's pickaxe", new ItemStack(ModItems.RESONITE_PICKAXE.get()), ModItems.RESONITE_SHARD.get(), 4))) {
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, c.tool())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, cluster.defaultBlockState())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
            var drops = table.getRandomItems(params);
            if (drops.size() != 1 || !drops.get(0).is(c.item()) || drops.get(0).getCount() != c.count()) bad.add("to " + c.name() + " a cluster drops " + drops);
        }
        // vanilla's amethyst cluster, to our pickaxes: its full 4
        var amethyst = net.minecraft.world.level.block.Blocks.AMETHYST_CLUSTER;
        var amethystTable = level.getServer().reloadableRegistries().getLootTable(amethyst.getLootTable().orElseThrow());
        for (var pick : List.of(ModItems.VOIDSTONE_PICKAXE.get(), ModItems.RESONITE_PICKAXE.get())) {
            var drops = amethystTable.getRandomItems(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, new ItemStack(pick))
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, amethyst.defaultBlockState())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK));
            if (drops.size() != 1 || drops.get(0).getCount() != 4) bad.add("to " + pick + " an amethyst cluster drops " + drops);
        }
        var block = ModBlocks.RESONITE_BLOCK.get();
        var blockDrops = level.getServer().reloadableRegistries().getLootTable(block.getLootTable().orElseThrow()).getRandomItems(
                new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE))
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, block.defaultBlockState())
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK));
        if (blockDrops.size() != 1 || !blockDrops.get(0).is(block.asItem()) || blockDrops.get(0).getCount() != 1) bad.add("a Resonite Block drops " + blockDrops);
        var recipes = level.getServer().getRecipeManager();
        var shard = new ItemStack(ModItems.RESONITE_SHARD.get());
        var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 2, List.of(shard, shard.copy(), shard.copy(), shard.copy()));
        var out = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).map(h -> h.value().assemble(input)).orElse(ItemStack.EMPTY);
        if (!out.is(ModBlocks.RESONITE_BLOCK.get().asItem()) || out.getCount() != 1) bad.add("four shards make " + out);
        var ringing = net.minecraft.tags.BlockTags.CRYSTAL_SOUND_BLOCKS;
        if (!ModBlocks.RESONITE_BLOCK.get().defaultBlockState().is(ringing) || !ModBlocks.BUDDING_RESONITE.get().defaultBlockState().is(ringing))
            bad.add("Resonite isn't in #minecraft:crystal_sound_blocks");
        if (!bad.isEmpty()) helper.fail("Resonite drops: " + bad);
        helper.succeed();
    }

    /**
     * A gateway's exit search (vanilla's tallest full block within a radius) passes over a Resonite column 20 tall and
     * finds an end-stone pillar 3 tall beside it; with the column made of end stone instead, it finds the column's top.
     * The gateway's own search (bedrock allowed, radius 16) finds the pillar beside the Resonite column too, so the
     * gateway stands 10 over the ground there, in a pearl's reach of the exit; with a cluster in the gateway's frame over
     * the pillar it goes over the next tallest ground with a clear frame (the floor), and with every frame crowded (a 3
     * by 3 floor between two spires, searched 1 out) it keeps vanilla's answer, a spire's top. When the tallest is end
     * stone at the search's edge and a spire just past the edge reaches into the frame over it, it goes over the floor.
     */
    private static void gatewayExitsBesideSpires(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        BlockPos centre = new BlockPos(7000, level.getMaxY() - 60, 2400);
        floor(level, centre, 6, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
        BlockPos pillar = centre.offset(-3, 2, 3), column = centre.offset(2, 19, 0);
        for (int k = 0; k < 3; k++) level.setBlock(centre.offset(-3, k, 3), net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 2);
        try {
            var search = net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity.class.getDeclaredMethod("findTallestBlock",
                    net.minecraft.world.level.BlockGetter.class, BlockPos.class, int.class, boolean.class);
            search.setAccessible(true);
            for (var block : List.of(ModBlocks.RESONITE_BLOCK.get(), net.minecraft.world.level.block.Blocks.END_STONE)) {
                for (int k = 0; k < 20; k++) level.setBlock(centre.offset(2, k, 0), block.defaultBlockState(), 2);
                BlockPos found = (BlockPos) search.invoke(null, level, centre, 5, false);
                BlockPos want = block == net.minecraft.world.level.block.Blocks.END_STONE ? column : pillar;
                if (!want.equals(found)) bad.add("beside a column of " + block + " the exit search found " + found + ", not " + want);
                // the gateway's own search (radius 16, bedrock allowed) comes down beside a spire too, as the frame round
                // its perch 10 over the pillar is clear; over an end-stone column it is vanilla's
                BlockPos perch = (BlockPos) search.invoke(null, level, centre, 16, true);
                if (!want.equals(perch)) bad.add("beside a column of " + block + " the gateway's search found " + perch + ", not " + want);
            }
            // a cluster in the gateway's frame over the pillar: the gateway goes over the next tallest ground whose frame
            // is clear, the floor beside, not over the pillar (it would break the cluster) nor back over the spire
            for (int k = 0; k < 20; k++) level.setBlock(centre.offset(2, k, 0), ModBlocks.RESONITE_BLOCK.get().defaultBlockState(), 2);
            level.setBlock(pillar.offset(1, 10, 0), ModBlocks.RESONITE_CLUSTER.get().defaultBlockState(), 2);
            BlockPos crowded = (BlockPos) search.invoke(null, level, centre, 16, true);
            if (crowded.getY() != centre.getY() - 1 || !level.getBlockState(crowded).is(net.minecraft.world.level.block.Blocks.END_STONE))
                bad.add("with a cluster in the gateway's frame over the pillar the search found " + crowded + ", not the floor beside");
            for (BlockPos q : BlockPos.betweenClosed(crowded.above(8).offset(-1, 0, -1), crowded.above(12).offset(1, 0, 1))) {
                if (!level.getBlockState(q).isAir()) bad.add("the gateway's frame over " + crowded + " holds " + level.getBlockState(q));
            }
            // every frame crowded: a 3 by 3 floor between two Resonite columns, searched 1 out; vanilla's perch stands
            BlockPos small = centre.offset(0, 0, -12);
            for (BlockPos p : BlockPos.betweenClosed(small.offset(-1, -1, -1), small.offset(1, -1, 1))) level.setBlock(p, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 2);
            for (int k = 0; k < 20; k++) {
                level.setBlock(small.offset(-1, k, 0), ModBlocks.RESONITE_BLOCK.get().defaultBlockState(), 2);
                level.setBlock(small.offset(1, k, 0), ModBlocks.RESONITE_BLOCK.get().defaultBlockState(), 2);
            }
            BlockPos hemmed = (BlockPos) search.invoke(null, level, small, 1, true);
            if (!small.offset(-1, 19, 0).equals(hemmed)) bad.add("with every frame crowded the search found " + hemmed + ", not vanilla's, the first spire's top");
            for (BlockPos p : BlockPos.betweenClosed(small.offset(-1, -1, -1), small.offset(1, 20, 1))) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            // the tallest is end stone at the search's edge, and a spire just past the edge reaches into the frame over
            // it: the gateway goes over the floor, whose frame is clear
            BlockPos edge = centre.offset(0, 0, -24);
            for (BlockPos p : BlockPos.betweenClosed(edge.offset(-1, -1, -1), edge.offset(1, -1, 1))) level.setBlock(p, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 2);
            for (int k = 0; k < 3; k++) level.setBlock(edge.offset(1, k, 0), net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 2);
            for (int k = 0; k < 20; k++) level.setBlock(edge.offset(2, k, 0), ModBlocks.RESONITE_BLOCK.get().defaultBlockState(), 2);
            BlockPos beside = (BlockPos) search.invoke(null, level, edge, 1, true);
            if (beside.getY() != edge.getY() - 1) bad.add("with a spire past the edge in the frame over the tallest the search found " + beside + ", not the floor");
            for (BlockPos q : BlockPos.betweenClosed(beside.above(8).offset(-1, 0, -1), beside.above(12).offset(1, 0, 1))) {
                if (!level.getBlockState(q).isAir()) bad.add("the gateway's frame over " + beside + " holds " + level.getBlockState(q));
            }
            for (BlockPos p : BlockPos.betweenClosed(edge.offset(-1, -1, -1), edge.offset(2, 20, 1))) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        } catch (ReflectiveOperationException e) {
            bad.add("no exit search: " + e);
        }
        clear(level, centre);
        if (!bad.isEmpty()) helper.fail("gateway exits: " + bad);
        helper.succeed();
    }

    /**
     * Resonite gear is built from GearTiers' numbers: each tool's durability, its sword's damage, a pickaxe that takes
     * obsidian (diamond's tier) where Voidstone's doesn't; each armour piece's defence and toughness, the worn look
     * (equipment asset promisedend:resonite), the Resonance set tag on all four; the ingot repairs every piece; the
     * ingot's recipe is 4 shards, 2 Chorus Resin and 2 iron ingots.
     */
    private static void resoniteGear(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var tier = com.panda.promisedend.logic.GearTiers.RESONITE;
        var ingot = new ItemStack(ModItems.RESONITE_INGOT.get());
        for (var h : List.of(ModItems.RESONITE_SWORD, ModItems.RESONITE_SHOVEL, ModItems.RESONITE_PICKAXE, ModItems.RESONITE_AXE, ModItems.RESONITE_HOE)) {
            var stack = new ItemStack(h.get());
            if (stack.getMaxDamage() != tier.durability()) bad.add(h.getId() + " lasts " + stack.getMaxDamage() + ", not " + tier.durability());
            if (!stack.isValidRepairItem(ingot)) bad.add("the ingot doesn't repair " + h.getId());
        }
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var pick = new ItemStack(ModItems.RESONITE_PICKAXE.get());
        if (Math.abs(pick.getDestroySpeed(stone) - tier.speed()) > 1e-4) bad.add("the pickaxe mines stone at " + pick.getDestroySpeed(stone) + ", not " + tier.speed());
        java.util.function.BiConsumer<net.minecraft.world.item.Item, Integer> enchantable = (item, want) -> {
            var ench = new ItemStack(item).get(net.minecraft.core.component.DataComponents.ENCHANTABLE);
            if (ench == null || ench.value() != want) bad.add(item + " has enchantability " + ench + ", not " + want);
        };
        for (var h : List.of(ModItems.RESONITE_SWORD, ModItems.RESONITE_SHOVEL, ModItems.RESONITE_PICKAXE, ModItems.RESONITE_AXE, ModItems.RESONITE_HOE))
            enchantable.accept(h.get(), tier.enchantability());
        for (var h : List.of(ModItems.RESONITE_HELMET, ModItems.RESONITE_CHESTPLATE, ModItems.RESONITE_LEGGINGS, ModItems.RESONITE_BOOTS))
            enchantable.accept(h.get(), com.panda.promisedend.logic.GearTiers.RESONITE_ARMOUR.enchantability());
        var sword = new ItemStack(ModItems.RESONITE_SWORD.get());
        double[] damage = {1.0};   // the player's fist
        sword.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) damage[0] += modifier.amount();
        });
        if (Math.abs(damage[0] - tier.swordDamage()) > 1e-4) bad.add("the sword deals " + damage[0] + ", not " + tier.swordDamage());
        var obsidian = net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState();
        if (!new ItemStack(ModItems.RESONITE_PICKAXE.get()).isCorrectToolForDrops(obsidian)) bad.add("the pickaxe doesn't mine obsidian");
        if (new ItemStack(ModItems.VOIDSTONE_PICKAXE.get()).isCorrectToolForDrops(obsidian)) bad.add("Voidstone's pickaxe mines obsidian too, so this can't tell");
        var armour = com.panda.promisedend.logic.GearTiers.RESONITE_ARMOUR;
        record Piece(com.panda.promisedend.platform.registry.DeferredItem<?> item, net.minecraft.world.entity.EquipmentSlot slot, int defence) {}
        for (var p : List.of(new Piece(ModItems.RESONITE_HELMET, net.minecraft.world.entity.EquipmentSlot.HEAD, armour.helmet()),
                new Piece(ModItems.RESONITE_CHESTPLATE, net.minecraft.world.entity.EquipmentSlot.CHEST, armour.chestplate()),
                new Piece(ModItems.RESONITE_LEGGINGS, net.minecraft.world.entity.EquipmentSlot.LEGS, armour.leggings()),
                new Piece(ModItems.RESONITE_BOOTS, net.minecraft.world.entity.EquipmentSlot.FEET, armour.boots()))) {
            var stack = new ItemStack((net.minecraft.world.item.Item) p.item().get());
            double[] got = {0, 0};
            stack.forEachModifier(p.slot(), (attribute, modifier) -> {
                if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR)) got[0] += modifier.amount();
                if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS)) got[1] += modifier.amount();
            });
            if (got[0] != p.defence() || Math.abs(got[1] - armour.toughness()) > 1e-4)
                bad.add(p.item().getId() + " gives " + got[0] + " armour and " + got[1] + " toughness, not " + p.defence() + " and " + armour.toughness());
            var equippable = stack.get(net.minecraft.core.component.DataComponents.EQUIPPABLE);
            if (equippable == null || equippable.slot() != p.slot() || equippable.assetId().filter(ModItems.RESONITE_ASSET::equals).isEmpty())
                bad.add(p.item().getId() + " is worn as " + equippable);
            if (!stack.is(com.panda.promisedend.registry.ModTags.RESONANCE_SET)) bad.add(p.item().getId() + " isn't in #promisedend:resonance_set");
            if (!stack.isValidRepairItem(ingot)) bad.add("the ingot doesn't repair " + p.item().getId());
            int durability = net.minecraft.world.item.equipment.ArmorType.valueOf(p.slot() == net.minecraft.world.entity.EquipmentSlot.HEAD ? "HELMET"
                    : p.slot() == net.minecraft.world.entity.EquipmentSlot.CHEST ? "CHESTPLATE" : p.slot() == net.minecraft.world.entity.EquipmentSlot.LEGS ? "LEGGINGS" : "BOOTS")
                    .getDurability(com.panda.promisedend.logic.GearTiers.RESONITE_ARMOUR_DURABILITY);
            if (stack.getMaxDamage() != durability) bad.add(p.item().getId() + " lasts " + stack.getMaxDamage() + ", not " + durability);
        }
        // the ingot from its recipe: the shapeless grid, in any order
        var recipes = level.getServer().getRecipeManager();
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 4; i++) grid.add(new ItemStack(ModItems.RESONITE_SHARD.get()));
        for (int i = 0; i < 2; i++) grid.add(new ItemStack(ModItems.CHORUS_RESIN.get()));
        for (int i = 0; i < 2; i++) grid.add(new ItemStack(net.minecraft.world.item.Items.IRON_INGOT));
        java.util.Collections.shuffle(grid, new java.util.Random(3));
        grid.add(ItemStack.EMPTY);
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        var made = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                .map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
        if (!made.is(ModItems.RESONITE_INGOT.get()) || made.getCount() != 1) bad.add("4 shards, 2 resin and 2 iron make " + made);
        grid.set(grid.indexOf(grid.stream().filter(st -> st.is(ModItems.CHORUS_RESIN.get())).findFirst().orElseThrow()), ItemStack.EMPTY);
        var short1 = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        if (recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, short1, level).isPresent()) bad.add("one resin short still makes an ingot");
        if (!bad.isEmpty()) helper.fail("Resonite gear: " + bad);
        helper.succeed();
    }

    /**
     * Stripping chorus timber sheds Chorus Resin half the time: a survival player's axe strips 400 chorus logs and wood
     * (in turn), and 160-240 resin come out (4 standard deviations round 200: by chance about 1 in 20000), each popped onto the stripped block.
     * Stripped timber set any other way sheds none: over air, over stripped timber, over end stone (a command or a
     * player's placing). In a standing trunk (a log over the stripped one) the resin comes out of the open side
     * towards the player, never inside a block.
     */
    private static void chorusResinFromStripping(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var survivor = helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        var box = new net.minecraft.world.phys.AABB(at).inflate(3);
        var resin = ModItems.CHORUS_RESIN.get();
        int shed = 0;
        for (int i = 0; i < 400; i++) {
            var log = (i % 2 == 0 ? ModBlocks.CHORUS_LOG : ModBlocks.CHORUS_WOOD).get();
            level.setBlock(at, log.defaultBlockState(), 3);
            survivor.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.IRON_AXE));
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at), net.minecraft.core.Direction.UP, at, false);
            survivor.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(survivor, hand, hit));
            if (!level.getBlockState(at).is(i % 2 == 0 ? ModBlocks.STRIPPED_CHORUS_LOG.get() : ModBlocks.STRIPPED_CHORUS_WOOD.get())) {
                bad.add("the axe left " + level.getBlockState(at));
                break;
            }
            for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
                if (e.getItem().is(resin)) shed += e.getItem().getCount();
                e.discard();
            }
        }
        PromisedEnd.LOGGER.info("[chorus_resin_from_stripping] {} resin from 400 strips", shed);
        if (shed < 160 || shed > 240) bad.add(shed + " resin from 400 strips (160-240)");
        // in a standing trunk (timber over it) the resin comes out of a side that is open, towards the player; 40
        // strips, so none at all is a 1 in 2^40 chance
        BlockPos mid = at.offset(4, 1, 0);
        // the side chosen is towards the nearest player in the level (the survivor striking isn't in it): one stands south
        var watcher = helper.makeMockServerPlayerInLevel();
        watcher.snapTo(mid.getX() + 0.5, mid.getY(), mid.getZ() + 3.5);
        var trunkBox = new net.minecraft.world.phys.AABB(mid).inflate(3);
        int out = 0;
        for (int i = 0; i < 40; i++) {
            level.setBlock(mid.below(), ModBlocks.CHORUS_LOG.get().defaultBlockState(), 3);
            level.setBlock(mid.above(), ModBlocks.CHORUS_LOG.get().defaultBlockState(), 3);
            level.setBlock(mid, ModBlocks.CHORUS_LOG.get().defaultBlockState(), 3);
            survivor.setItemInHand(hand, new ItemStack(net.minecraft.world.item.Items.IRON_AXE));
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(mid), net.minecraft.core.Direction.NORTH, mid, false);
            survivor.getMainHandItem().useOn(new net.minecraft.world.item.context.UseOnContext(survivor, hand, hit));
            for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, trunkBox)) {
                if (!e.getItem().is(resin)) continue;
                out++;
                BlockPos in = BlockPos.containing(e.position());
                if (!level.getBlockState(in).getCollisionShape(level, in).isEmpty()) bad.add("resin from a trunk came out inside " + level.getBlockState(in));
                else if (e.getZ() < mid.getZ() + 0.5) bad.add("resin from a trunk came out away from the player, at " + e.position());
                e.discard();
            }
        }
        if (out == 0) bad.add("40 strips in a trunk shed no resin");
        for (BlockPos p : List.of(mid.below(), mid, mid.above())) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        watcher.discard();
        // set any other way: nothing
        int other = 0;
        // over stripped wood, not the stripped log itself: setting a block over its own state is no change, and no onPlace
        for (var under : List.of(net.minecraft.world.level.block.Blocks.AIR, ModBlocks.STRIPPED_CHORUS_WOOD.get(), net.minecraft.world.level.block.Blocks.END_STONE)) {
            for (int i = 0; i < 40; i++) {
                level.setBlock(at, under.defaultBlockState(), 3);
                level.setBlock(at, ModBlocks.STRIPPED_CHORUS_LOG.get().defaultBlockState(), 3);
            }
        }
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) {
            if (e.getItem().is(resin)) other += e.getItem().getCount();
            e.discard();
        }
        if (other > 0) bad.add(other + " resin from stripped timber set over air, stripped timber or end stone");
        level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        if (!bad.isEmpty()) helper.fail("Chorus Resin: " + bad);
        helper.succeed();
    }

    /**
     * The Resonance set bonus: a survival player in the four Resonite pieces punches an iron golem (no AI,
     * no armour, knockback-proof), its hurt cooldown cleared each time (the swings are all equally weak). Hits 1-3 take the same, the 4th that and 2 more, with Slowness II on the golem for
     * at most a second; the count starts again after it. A hit on another target restarts the count (A, A, A, B, A:
     * no ring), three pieces ring nothing in eight hits, and a gap over 3 s (61 ticks) restarts it (A, A, A, wait, A:
     * no ring). Every hit lands: the golem's hurt cooldown (26.3's damageCooldownTime) is cleared between hits. A
     * swing that doesn't land (the cooldown left on) isn't counted: A, A, miss, A rings nothing, the next A rings. A
     * spear's stabs count the same: four on one golem, the 4th 2 more with Slowness II.
     */
    private static void setBonusResonance(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var player = helper.makeMockServerPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var set = java.util.Map.of(net.minecraft.world.entity.EquipmentSlot.HEAD, ModItems.RESONITE_HELMET.get(),
                net.minecraft.world.entity.EquipmentSlot.CHEST, ModItems.RESONITE_CHESTPLATE.get(),
                net.minecraft.world.entity.EquipmentSlot.LEGS, ModItems.RESONITE_LEGGINGS.get(),
                net.minecraft.world.entity.EquipmentSlot.FEET, ModItems.RESONITE_BOOTS.get());
        set.forEach((slot, item) -> player.setItemSlot(slot, new ItemStack(item)));
        java.util.function.Supplier<net.minecraft.world.entity.animal.golem.IronGolem> golem = () -> {
            var g = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityTypes.IRON_GOLEM, new BlockPos(2, 2, 2));
            g.setHealth(g.getMaxHealth());
            return g;
        };
        var a = golem.get();
        var b = golem.get();
        java.util.function.ToDoubleFunction<net.minecraft.world.entity.LivingEntity> hit = target -> {
            // 26.3's hurt cooldown is damageCooldownTime, 20 after a hit: while it is over 10, a second hit lands only
            // if it is harder
            target.damageCooldownTime = 0;
            target.setInvulnerableTime(0);
            target.hurtTime = 0;
            float before = target.getHealth();
            player.attack(target);
            return before - target.getHealth();
        };
        double[] d = new double[5];
        for (int k = 0; k < 4; k++) d[k] = hit.applyAsDouble(a);
        PromisedEnd.LOGGER.info("[set_bonus_resonance] hits 1-4 took {} {} {} {}", d[0], d[1], d[2], d[3]);
        if (d[0] <= 0) bad.add("the first hit took nothing, so this can't tell");
        if (Math.abs(d[1] - d[0]) > 1e-3 || Math.abs(d[2] - d[0]) > 1e-3) bad.add("hits 1-3 took " + d[0] + ", " + d[1] + ", " + d[2]);
        if (Math.abs(d[3] - d[0] - com.panda.promisedend.logic.Resonance.BONUS_DAMAGE) > 1e-3) bad.add("the 4th hit took " + d[3] + ", not " + d[0] + " + 2");
        var slow = a.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);
        if (slow == null || slow.getAmplifier() != 1 || slow.getDuration() > 20) bad.add("after the 4th hit the golem has " + slow);
        d[4] = hit.applyAsDouble(a);
        if (Math.abs(d[4] - d[0]) > 1e-3) bad.add("the 5th hit (the count started again) took " + d[4]);
        // another target between: A, A, A, B, A rings nothing (B first, so A's count starts from none: the 5th hit began one)
        a.removeAllEffects();
        hit.applyAsDouble(b);
        for (int k = 0; k < 3; k++) hit.applyAsDouble(a);
        hit.applyAsDouble(b);
        double after = hit.applyAsDouble(a);
        if (Math.abs(after - d[0]) > 1e-3 || a.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)) bad.add("a hit on another target didn't restart the count (" + after + ")");
        // three pieces: nothing in eight hits
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, ItemStack.EMPTY);
        for (int k = 0; k < 8; k++) {
            double took = hit.applyAsDouble(a);
            if (Math.abs(took - d[0]) > 1e-3) {
                bad.add("with three pieces hit " + (k + 1) + " took " + took);
                break;
            }
        }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.FEET, new ItemStack(ModItems.RESONITE_BOOTS.get()));
        // a swing that doesn't land isn't counted: A, A, then one into the hurt cooldown (nothing taken), then A, A
        var e = golem.get();
        hit.applyAsDouble(e);
        hit.applyAsDouble(e);
        float health = e.getHealth();
        player.attack(e);
        if (e.getHealth() != health) bad.add("the swing into the hurt cooldown landed, so this can't tell");
        double third = hit.applyAsDouble(e);
        if (Math.abs(third - d[0]) > 1e-3 || e.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)) bad.add("a swing that didn't land was counted (" + third + ")");
        double fourth = hit.applyAsDouble(e);
        if (Math.abs(fourth - d[0] - com.panda.promisedend.logic.Resonance.BONUS_DAMAGE) > 1e-3) bad.add("the 4th landed hit after a miss took " + fourth);
        // a spear's stabs: the same count through stabAttack
        var f = golem.get();
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, new ItemStack(net.minecraft.world.item.Items.IRON_SPEAR));
        double[] stab = new double[4];
        for (int k = 0; k < 4; k++) {
            f.damageCooldownTime = 0;
            f.setInvulnerableTime(0);
            f.hurtTime = 0;
            float was = f.getHealth();
            player.stabAttack(net.minecraft.world.entity.EquipmentSlot.MAINHAND, f, 3.0F, true, false, false);
            stab[k] = was - f.getHealth();
        }
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        PromisedEnd.LOGGER.info("[set_bonus_resonance] spear stabs 1-4 took {} {} {} {}", stab[0], stab[1], stab[2], stab[3]);
        if (stab[0] <= 0 || Math.abs(stab[1] - stab[0]) > 1e-3 || Math.abs(stab[2] - stab[0]) > 1e-3
                || Math.abs(stab[3] - stab[0] - com.panda.promisedend.logic.Resonance.BONUS_DAMAGE) > 1e-3 || !f.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS))
            bad.add("a spear's stabs took " + java.util.Arrays.toString(stab) + (f.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS) ? "" : ", no Slowness"));
        // a gap over the window: A, A, A, 61 ticks, A rings nothing
        var c = golem.get();
        for (int k = 0; k < 3; k++) hit.applyAsDouble(c);
        helper.runAfterDelay(com.panda.promisedend.logic.Resonance.WINDOW + 1, () -> {
            double late = hit.applyAsDouble(c);
            if (Math.abs(late - d[0]) > 1e-3 || c.hasEffect(net.minecraft.world.effect.MobEffects.SLOWNESS)) bad.add("a hit 61 ticks on still rang (" + late + ")");
            if (!bad.isEmpty()) helper.fail("Resonance: " + bad);
            helper.succeed();
        });
    }

    /** The Thicket's grove feature, run as it is configured, on a floor high in the test level's sky. */
    private static net.minecraft.world.level.levelgen.feature.Feature grove(net.minecraft.server.level.ServerLevel level, String name) {
        return level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                .getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id(name))).value().feature().value();
    }

    private static void floor(net.minecraft.server.level.ServerLevel level, BlockPos centre, int r, net.minecraft.world.level.block.state.BlockState state) {
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-r, -1, -r), centre.offset(r, -1, r))) level.setBlock(p, state, 2);
    }

    private static void clear(net.minecraft.server.level.ServerLevel level, BlockPos centre) {
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-16, -2, -16), centre.offset(16, 40, 16))) {
            if (!level.getBlockState(p).isAir()) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
    }

    /**
     * A grove on open end stone: 3-6 trunks 6-11 tall, each standing on end stone, at least two of them with limbs
     * (lying timber above their foot), and chorus plants grown from the timber well above the ground (the canopy).
     * Over ten seeds.
     */
    private static void chorusGroveGrows(GameTestHelper helper) {
        var level = helper.getLevel();
        var feature = grove(level, "chorus_grove");
        var log = ModBlocks.CHORUS_LOG.get();
        List<String> bad = new ArrayList<>();
        int y = level.getMaxY() - 60;
        for (int n = 0; n < 10; n++) {
            BlockPos centre = new BlockPos(2000 + n * 64, y, 2000);
            floor(level, centre, 12, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            if (!feature.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(300 + n), centre)) {
                bad.add("nothing grew at " + centre);
                clear(level, centre);
                continue;
            }
            int trunks = 0, limbs = 0, canopy = 0;
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-16, 0, -16), centre.offset(16, 40, 16))) {
                var state = level.getBlockState(p);
                var axis = state.is(log) ? state.getValue(net.minecraft.world.level.block.RotatedPillarBlock.AXIS) : null;
                if (axis == net.minecraft.core.Direction.Axis.Y && p.getY() == y) {
                    trunks++;
                    if (!level.getBlockState(p.below()).is(net.minecraft.world.level.block.Blocks.END_STONE)) bad.add("a trunk off the ground at " + p);
                    int h = 0;
                    while (level.getBlockState(p.above(h)).is(log)) h++;
                    if (h < 6 || h > 11) bad.add("a trunk " + h + " tall at " + p);
                } else if (axis != null && axis != net.minecraft.core.Direction.Axis.Y && p.getY() > y + 1) {
                    limbs++;
                } else if ((state.is(net.minecraft.world.level.block.Blocks.CHORUS_PLANT) || state.is(net.minecraft.world.level.block.Blocks.CHORUS_FLOWER)) && p.getY() >= y + 6) {
                    canopy++;
                }
            }
            // upright timber at the foot is a trunk (a lying log there is a root)
            if (trunks < 3 || trunks > 6) bad.add(trunks + " trunks at " + centre);
            if (limbs < 2) bad.add("only " + limbs + " limb blocks at " + centre);
            if (canopy < 12) bad.add("only " + canopy + " canopy chorus at " + centre);
            PromisedEnd.LOGGER.info("[chorus_grove_grows] {}: {} trunks, {} limb blocks, {} canopy chorus", centre, trunks, limbs, canopy);
            clear(level, centre);
        }
        if (!bad.isEmpty()) helper.fail("groves: " + bad);
        helper.succeed();
    }

    /**
     * Where a grove doesn't belong it builds nothing: on ground outside {@code #promisedend:chorus_grove_ground}
     * (obsidian here; chorus timber too, so no tree stands on a root), under a roof 4 blocks up, and the terrain-off
     * copy while our terrain is on.
     */
    private static void chorusGroveKeepsOut(GameTestHelper helper) {
        var level = helper.getLevel();
        var feature = grove(level, "chorus_grove");
        var log = ModBlocks.CHORUS_LOG.get();
        var generator = level.getChunkSource().getGenerator();
        List<String> bad = new ArrayList<>();
        int y = level.getMaxY() - 60;
        BlockPos obsidian = new BlockPos(3000, y, 2000), timber = new BlockPos(3064, y, 2000), roofed = new BlockPos(3128, y, 2000), off = new BlockPos(3192, y, 2000);
        floor(level, obsidian, 12, net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState());
        floor(level, timber, 12, log.defaultBlockState());
        floor(level, roofed, 12, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
        // a glass roof 4 up over the whole floor: the surface is the roof, which isn't grove ground, and nothing goes under it
        floor(level, roofed.above(5), 12, net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
        floor(level, off, 12, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
        for (BlockPos at : List.of(obsidian, timber, roofed)) {
            if (feature.place(level, generator, net.minecraft.util.RandomSource.create(9), at)) bad.add("grew at " + at);
        }
        for (BlockPos p : BlockPos.betweenClosed(roofed.offset(-12, 0, -12), roofed.offset(12, 3, 12))) {
            if (level.getBlockState(p).is(log)) bad.add("timber under the roof at " + p);
        }
        if (com.panda.promisedend.EndConfig.terrainEnabled() && grove(level, "chorus_grove_fallback").place(level, generator, net.minecraft.util.RandomSource.create(9), off))
            bad.add("the terrain-off copy grew with terrain on");
        for (BlockPos at : List.of(obsidian, timber, roofed, off)) clear(level, at);
        if (!bad.isEmpty()) helper.fail("groves where they don't belong: " + bad);
        helper.succeed();
    }

    /** Every biome of ours is an End biome by tag, so other mods' End spawns and features reach it. */
    private static void biomesInIsEndTag(GameTestHelper helper) {
        var biomes = helper.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);
        var isEnd = biomes.getOrThrow(BiomeTags.IS_END);
        // the convention tag other mods use for "spawns in the End" (both loaders share the c namespace)
        var conventionEnd = biomes.get(net.minecraft.tags.TagKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("c", "is_end")));
        List<String> missing = new ArrayList<>();
        int ours = 0;
        for (var ref : biomes.listElements().toList()) {
            if (!ref.key().identifier().getNamespace().equals(PromisedEnd.MODID)) continue;
            ours++;
            if (!isEnd.contains(ref)) missing.add(ref.key().identifier() + " (minecraft:is_end)");
            if (conventionEnd.isEmpty() || !conventionEnd.get().contains(ref)) missing.add(ref.key().identifier() + " (c:is_end)");
        }
        if (ours == 0) helper.fail("no biomes of ours are registered");
        if (!missing.isEmpty()) helper.fail("not tagged: " + missing);
        helper.succeed();
    }

    /**
     * Every recipe and loot table file we ship made it into the game. 26.3 drops a malformed recipe with one log line
     * and ignores old loot keys silently, so compare the files on disk with what loaded.
     */
    private static void dataFilesAllLoad(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        var resources = server.getResourceManager();
        List<String> missing = new ArrayList<>();
        var recipeFiles = resources.listResources("recipe", id -> id.getNamespace().equals(PromisedEnd.MODID) && id.getPath().endsWith(".json"));
        for (Identifier file : recipeFiles.keySet()) {
            String path = file.getPath().substring("recipe/".length(), file.getPath().length() - ".json".length());
            if (server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, PromisedEnd.id(path))).isEmpty()) missing.add("recipe " + path);
        }
        var lootFiles = resources.listResources("loot_table", id -> id.getNamespace().equals(PromisedEnd.MODID) && id.getPath().endsWith(".json"));
        for (Identifier file : lootFiles.keySet()) {
            String path = file.getPath().substring("loot_table/".length(), file.getPath().length() - ".json".length());
            var table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, PromisedEnd.id(path)));
            if (table == net.minecraft.world.level.storage.loot.LootTable.EMPTY) missing.add("loot table " + path);
        }
        if (recipeFiles.isEmpty() || lootFiles.isEmpty()) missing.add("found no recipe or loot files at all (" + recipeFiles.size() + ", " + lootFiles.size() + ")");
        if (!missing.isEmpty()) helper.fail("data files that didn't load: " + missing);
        helper.succeed();
    }

    /** Every block of ours drops something when mined with the right tool (its loot table rolls a stack). */
    private static void everyBlockDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        for (var h : ModBlocks.BLOCKS.getEntries()) {
            var block = h.get();
            var key = block.getLootTable();
            if (key.isEmpty()) continue; // noLootTable blocks (boss arena pieces) drop nothing on purpose
            var table = level.getServer().reloadableRegistries().getLootTable(key.get());
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.NETHERITE_PICKAXE))
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, block.defaultBlockState())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
            if (table.getRandomItems(params).isEmpty()) bad.add(h.getId().getPath());
        }
        if (!bad.isEmpty()) helper.fail("blocks that drop nothing: " + bad);
        helper.succeed();
    }

    /**
     * Voidstone tools: the pickaxe mines what iron mines and no more, the numbers match GearTiers, the sword's
     * tooltip damage is 5.5, the axe strips logs (its block-transformer component), and each is in its vanilla tool tag.
     */
    private static void voidstoneToolsWork(GameTestHelper helper) {
        var t = com.panda.promisedend.logic.GearTiers.VOIDSTONE;
        var pick = new net.minecraft.world.item.ItemStack(ModItems.VOIDSTONE_PICKAXE.get());
        if (!pick.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.DIAMOND_ORE.defaultBlockState())) helper.fail("can't mine diamond ore");
        if (pick.isCorrectToolForDrops(net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState())) helper.fail("mines obsidian (diamond tier)");
        if (pick.getMaxDamage() != t.durability()) helper.fail("durability " + pick.getMaxDamage());
        float speed = pick.getDestroySpeed(net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
        if (Math.abs(speed - t.speed()) > 1e-4) helper.fail("speed on end stone " + speed);
        var sword = new net.minecraft.world.item.ItemStack(ModItems.VOIDSTONE_SWORD.get());
        double[] damage = {1.0}; // the player's own base attack damage
        sword.forEachModifier(net.minecraft.world.entity.EquipmentSlot.MAINHAND, (attr, mod) -> {
            if (attr.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) damage[0] += mod.amount();
        });
        if (Math.abs(damage[0] - t.swordDamage()) > 1e-4) helper.fail("sword damage " + damage[0]);
        if (!new net.minecraft.world.item.ItemStack(ModItems.VOIDSTONE_AXE.get()).has(net.minecraft.core.component.DataComponents.BLOCK_TRANSFORMER))
            helper.fail("the axe has no block transformer (won't strip logs)");
        var tags = java.util.Map.of(ModItems.VOIDSTONE_SWORD, net.minecraft.tags.ItemTags.SWORDS, ModItems.VOIDSTONE_SHOVEL, net.minecraft.tags.ItemTags.SHOVELS,
                ModItems.VOIDSTONE_PICKAXE, net.minecraft.tags.ItemTags.PICKAXES, ModItems.VOIDSTONE_AXE, net.minecraft.tags.ItemTags.AXES,
                ModItems.VOIDSTONE_HOE, net.minecraft.tags.ItemTags.HOES);
        tags.forEach((item, tag) -> {
            if (!new net.minecraft.world.item.ItemStack(item.get()).is(tag)) helper.fail(item.getId() + " not in " + tag.location());
        });
        if (!new net.minecraft.world.item.ItemStack(ModBlocks.VOIDSTONE_SET.base().get()).is(com.panda.promisedend.registry.ModTags.VOIDSTONE_TOOL_MATERIALS))
            helper.fail("voidstone doesn't repair the tools");
        helper.succeed();
    }

    /**
     * A player carrying a Void Anchor who falls below the trigger depth is put back on their last safe footing
     * with Slow Falling, and the anchor cracks.
     */
    private static void voidAnchorRescues(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos footing = standOn(level, helper.absolutePos(new BlockPos(1, 2, 1)));
        com.panda.promisedend.logic.VoidAnchor.setFooting(player, net.minecraft.core.GlobalPos.of(level.dimension(), footing));
        player.getInventory().setItem(4, new net.minecraft.world.item.ItemStack(ModItems.VOID_ANCHOR.get()));
        int below = com.panda.promisedend.logic.VoidAnchor.triggerY(level) - 6;
        player.teleportTo(footing.getX() + 0.5, below, footing.getZ() + 0.5);
        player.fallDistance = 40;
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        if (!player.blockPosition().equals(footing)) helper.fail("not pulled back: at " + player.blockPosition() + ", footing " + footing);
        if (!player.getInventory().getItem(4).is(ModItems.CRACKED_VOID_ANCHOR.get())) helper.fail("the anchor didn't crack: " + player.getInventory().getItem(4));
        if (!player.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING)) helper.fail("no Slow Falling");
        if (player.fallDistance > 0) helper.fail("fall distance kept: " + player.fallDistance);
        var saved = level.getServer().getAdvancements().get(com.panda.promisedend.logic.CodexBook.SAVED_BY_ANCHOR);
        if (saved == null || !player.getAdvancements().getOrStartProgress(saved).isDone()) helper.fail("the rescue didn't award rim/saved_by_anchor");
        // standing still above the trigger does nothing more
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        if (!player.getInventory().getItem(4).is(ModItems.CRACKED_VOID_ANCHOR.get())) helper.fail("changed again above the trigger");
        com.panda.promisedend.logic.VoidAnchor.forget(player);
        helper.succeed();
    }

    /**
     * A cracked anchor (or none) doesn't pull anyone back, and a cracked anchor plus an ender pearl crafts an
     * intact one.
     */
    private static void voidAnchorNeedsCharge(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos footing = standOn(level, helper.absolutePos(new BlockPos(1, 2, 1)));
        com.panda.promisedend.logic.VoidAnchor.setFooting(player, net.minecraft.core.GlobalPos.of(level.dimension(), footing));
        int below = com.panda.promisedend.logic.VoidAnchor.triggerY(level) - 6;
        player.teleportTo(footing.getX() + 0.5, below, footing.getZ() + 0.5);
        if (com.panda.promisedend.logic.VoidAnchor.rescue(player)) helper.fail("rescued with no anchor");
        player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(ModItems.CRACKED_VOID_ANCHOR.get()));
        if (com.panda.promisedend.logic.VoidAnchor.rescue(player)) helper.fail("rescued by a cracked anchor");
        if (player.blockPosition().getY() > below) helper.fail("moved without an intact anchor");
        var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, List.of(
                new net.minecraft.world.item.ItemStack(ModItems.CRACKED_VOID_ANCHOR.get()), new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL)));
        var recipe = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level);
        if (recipe.isEmpty()) helper.fail("no recipe recharges a cracked anchor with an ender pearl");
        var result = recipe.get().value().assemble(input);
        if (!result.is(ModItems.VOID_ANCHOR.get())) helper.fail("recharging makes " + result);
        com.panda.promisedend.logic.VoidAnchor.forget(player);

        helper.succeed();
    }

    /**
     * Falling past the floor with an anchor plays a cue; a flier below the trigger isn't caught
     * until they stop flying; footing in another dimension is ignored, so the anchor never carries anyone across worlds.
     */
    private static void voidAnchorRules(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        BlockPos footing = standOn(level, helper.absolutePos(new BlockPos(1, 2, 1)));
        com.panda.promisedend.logic.VoidAnchor.setFooting(player, net.minecraft.core.GlobalPos.of(level.dimension(), footing));
        player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(ModItems.VOID_ANCHOR.get()));
        int below = com.panda.promisedend.logic.VoidAnchor.triggerY(level) - 6;
        player.teleportTo(footing.getX() + 0.5, below, footing.getZ() + 0.5);
        // between the floor and the trigger, falling with an intact anchor: the cue plays once, nothing else happens
        player.teleportTo(footing.getX() + 0.5, level.getMinY() - 4, footing.getZ() + 0.5);
        player.setDeltaMovement(0, -1.5, 0);
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        if (!com.panda.promisedend.logic.VoidAnchor.cued(player)) helper.fail("no falling cue under the floor with an anchor");
        if (!player.getInventory().getItem(0).is(ModItems.VOID_ANCHOR.get())) helper.fail("rescued above the trigger");
        player.teleportTo(footing.getX() + 0.5, below, footing.getZ() + 0.5);
        player.getAbilities().flying = true;
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        if (!player.getInventory().getItem(0).is(ModItems.VOID_ANCHOR.get())) helper.fail("a flying player was caught");
        player.getAbilities().flying = false;
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        if (!player.blockPosition().equals(footing)) helper.fail("not caught once flying stopped: at " + player.blockPosition());
        // footing recorded in the End doesn't pull a player out of this dimension
        player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(ModItems.VOID_ANCHOR.get()));
        com.panda.promisedend.logic.VoidAnchor.setFooting(player, net.minecraft.core.GlobalPos.of(net.minecraft.world.level.Level.END, new BlockPos(0, 64, 0)));
        player.teleportTo(footing.getX() + 0.5, below, footing.getZ() + 0.5);
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        if (player.level() != level) helper.fail("the anchor moved the player to " + player.level().dimension().identifier());
        if (player.getY() <= below) helper.fail("not rescued when the footing was in another dimension");
        com.panda.promisedend.logic.VoidAnchor.forget(player);
        helper.succeed();
    }

    /**
     * Safe footing, in the real End's void gap (r = 700): the middle of a 9x9 platform counts and so does one block in
     * from its edge, the edge itself doesn't; the middle of a 3-wide bridge counts, a 1-wide bridge doesn't; a floor of
     * bottom slabs counts through {@code #promisedend:void_safe_ground}.
     */
    private static void voidAnchorSafeGround(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) {
            helper.fail("no End level on the test server");
            return;
        }
        int y = 64;
        BlockPos stone = new BlockPos(700, y, 0), slabs = new BlockPos(700, y, 32), bridges = new BlockPos(700, y, -32);
        // the test server's End isn't the real one: it has a floor a few blocks thick, so carve a true void first
        for (BlockPos c : List.of(stone, slabs, bridges)) {
            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 24; dz++) {
                    for (int dy = end.getMinY(); dy < end.getMinY() + 12; dy++) {
                        end.setBlockAndUpdate(new BlockPos(c.getX() + dx, dy, c.getZ() + dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                end.setBlockAndUpdate(stone.offset(dx, 0, dz), ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState());
                end.setBlockAndUpdate(slabs.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.STONE_SLAB.defaultBlockState());
            }
        }
        // a 3-wide bridge along x at bridges, a 1-wide one 6 blocks south of it, and 12 south a 1-wide one with carpet
        // lanes either side: carpet stands for a block outside the motion-blocking tags, as most other mods' are
        var carpet = net.minecraft.world.level.block.Blocks.MOSS_CARPET.defaultBlockState();
        for (int dx = -6; dx <= 6; dx++) {
            end.setBlockAndUpdate(bridges.offset(dx, 0, 12), ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState());
            // no updates, or the unsupported carpet drops
            end.setBlock(bridges.offset(dx, 0, 11), carpet, net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
            end.setBlock(bridges.offset(dx, 0, 13), carpet, net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE);
            // 18 south, a 1-wide bridge under a 5-wide roof: the roof is over the void columns, not under them
            end.setBlockAndUpdate(bridges.offset(dx, 0, 18), ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState());
            for (int dz = 16; dz <= 20; dz++) end.setBlockAndUpdate(bridges.offset(dx, 4, dz), ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState());
            for (int dz = -1; dz <= 1; dz++) end.setBlockAndUpdate(bridges.offset(dx, 0, dz), ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState());
            end.setBlockAndUpdate(bridges.offset(dx, 0, 6), ModBlocks.VOIDSTONE_SET.base().get().defaultBlockState());
        }
        List<String> bad = new ArrayList<>();
        if (!com.panda.promisedend.logic.VoidAnchor.isSafeGround(end, stone.above())) bad.add("the platform's middle isn't safe");
        if (!com.panda.promisedend.logic.VoidAnchor.isSafeGround(end, stone.offset(3, 1, 0))) bad.add("one block in from the platform's edge isn't safe");
        if (com.panda.promisedend.logic.VoidAnchor.isSafeGround(end, stone.offset(4, 1, 0))) bad.add("the platform's edge block counts as safe");
        int carpetHeight = end.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, bridges.getX(), bridges.getZ() + 11);
        if (carpetHeight > end.getMinY()) bad.add("carpet is motion-blocking (height " + carpetHeight + "): the carpet bridge proves nothing");
        if (end.getBlockState(bridges.offset(0, 0, 11)).isAir()) bad.add("the carpet dropped");
        if (!com.panda.promisedend.logic.VoidAnchor.isSafeGround(end, bridges.offset(0, 1, 12))) bad.add("a bridge flanked by blocks outside the motion-blocking tags isn't safe");
        if (com.panda.promisedend.logic.VoidAnchor.isSafeGround(end, bridges.offset(0, 1, 18))) bad.add("a 1-wide bridge under a roof counts as safe");
        if (!com.panda.promisedend.logic.VoidAnchor.isSafeGround(end, bridges.above())) bad.add("the middle of a 3-wide bridge isn't safe");
        if (com.panda.promisedend.logic.VoidAnchor.isSafeGround(end, bridges.offset(0, 1, 6))) bad.add("a 1-wide bridge counts as safe");
        if (com.panda.promisedend.logic.VoidAnchor.isSafeGround(end, stone.offset(0, 1, 9))) bad.add("the void itself counts as safe");
        // feet stand inside a bottom slab's block
        if (!com.panda.promisedend.logic.VoidAnchor.isSafeGround(end, slabs)) bad.add("a floor of bottom slabs isn't safe");
        if (!bad.isEmpty()) helper.fail("safe footing: " + bad);
        helper.succeed();
    }

    /** Sets the biome of the 17-block cube round {@code at}, as {@code /fillbiome} does (which needs the chunks loaded). */
    private static void fillBiome(net.minecraft.server.level.ServerLevel level, BlockPos at, String biome) {
        for (int cx = (at.getX() - 8) >> 4; cx <= (at.getX() + 8) >> 4; cx++) {
            for (int cz = (at.getZ() - 8) >> 4; cz <= (at.getZ() + 8) >> 4; cz++) level.getChunk(cx, cz);
        }
        var server = level.getServer();
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withLevel(level).withSuppressedOutput(),
                "fillbiome " + (at.getX() - 8) + " " + (at.getY() - 8) + " " + (at.getZ() - 8) + " " + (at.getX() + 8) + " "
                        + (at.getY() + 8) + " " + (at.getZ() + 8) + " " + biome);
    }

    /** Stone under {@code feet}, room for a player at it: footing the rescue accepts. */
    private static BlockPos standOn(net.minecraft.server.level.ServerLevel level, BlockPos feet) {
        level.setBlockAndUpdate(feet.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        level.setBlockAndUpdate(feet, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(feet.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        return feet;
    }

    /**
     * Footing mined away since it was recorded lands the player on the nearest safe spot instead; feet in a bottom
     * slab land on top of it, not inside; a glider is caught only once it's {@code GLIDER_DEPTH} under the floor and
     * still descending; the rescue's Slow Falling lasts {@code SLOW_FALLING_TICKS}.
     */
    private static void voidAnchorLanding(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        List<String> bad = new ArrayList<>();
        // a row of two, the recorded block mined out
        BlockPos footing = standOn(level, helper.absolutePos(new BlockPos(2, 2, 2)));
        standOn(level, footing.east());
        level.setBlockAndUpdate(footing.below(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        com.panda.promisedend.logic.VoidAnchor.setFooting(player, net.minecraft.core.GlobalPos.of(level.dimension(), footing));
        player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(ModItems.VOID_ANCHOR.get()));
        player.teleportTo(footing.getX() + 0.5, com.panda.promisedend.logic.VoidAnchor.triggerY(level) - 6, footing.getZ() + 0.5);
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        BlockPos at = player.blockPosition();
        if (at.equals(footing)) bad.add("landed on the mined-out footing");
        if (!level.getBlockState(at.below()).isFaceSturdy(level, at.below(), net.minecraft.core.Direction.UP)) bad.add("landed over " + level.getBlockState(at.below()) + " at " + at);
        if (at.distManhattan(footing) > com.panda.promisedend.logic.VoidAnchor.SEARCH_RADIUS * 3) bad.add("landed far from the footing: " + at);
        var slow = player.getEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING);
        if (slow == null || slow.getDuration() != com.panda.promisedend.logic.VoidAnchor.SLOW_FALLING_TICKS) bad.add("Slow Falling after the rescue isn't 5 s: " + slow);
        // standing in lava on good ground records nothing
        BlockPos lava = standOn(level, helper.absolutePos(new BlockPos(8, 2, 2)));
        for (BlockPos n : List.of(lava.east(), lava.west(), lava.north(), lava.south(), lava.east().north(), lava.east().south(), lava.west().north(), lava.west().south())) standOn(level, n);
        level.setBlockAndUpdate(lava, net.minecraft.world.level.block.Blocks.LAVA.defaultBlockState());
        player.teleportTo(lava.getX() + 0.5, lava.getY(), lava.getZ() + 0.5);
        player.setOnGround(true);
        player.baseTick();
        player.baseTick();
        player.setOnGround(true);
        if (!player.isInLava()) bad.add("the mock player isn't in lava: the lava check proves nothing");
        else if (com.panda.promisedend.logic.VoidAnchor.isSafeFooting(player)) bad.add("footing in lava counts as safe");
        level.setBlockAndUpdate(lava, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        player.clearFire();
        player.setHealth(player.getMaxHealth());
        // feet inside a bottom slab
        BlockPos slab = standOn(level, helper.absolutePos(new BlockPos(5, 2, 5)));
        level.setBlockAndUpdate(slab, net.minecraft.world.level.block.Blocks.STONE_SLAB.defaultBlockState());
        com.panda.promisedend.logic.VoidAnchor.setFooting(player, net.minecraft.core.GlobalPos.of(level.dimension(), slab));
        player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(ModItems.VOID_ANCHOR.get()));
        player.teleportTo(slab.getX() + 0.5, com.panda.promisedend.logic.VoidAnchor.triggerY(level) - 6, slab.getZ() + 0.5);
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        if (Math.abs(player.getY() - (slab.getY() + 0.5)) > 1e-6) bad.add("on a bottom slab at y " + slab.getY() + " landed at " + player.getY() + ", not on top");
        // a glider at the walkers' trigger is left to pull up; deeper and still descending, it's caught
        player.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(ModItems.VOID_ANCHOR.get()));
        player.startFallFlying();
        player.teleportTo(slab.getX() + 0.5, com.panda.promisedend.logic.VoidAnchor.triggerY(level) - 6, slab.getZ() + 0.5);
        player.setDeltaMovement(0.8, -0.4, 0);
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        if (!player.isFallFlying()) bad.add("the mock player can't glide: the test proves nothing");
        if (!player.getInventory().getItem(0).is(ModItems.VOID_ANCHOR.get())) bad.add("a glider was caught at the walkers' trigger");
        int deep = level.getMinY() - com.panda.promisedend.logic.VoidAnchor.GLIDER_DEPTH - 2;
        player.teleportTo(slab.getX() + 0.5, deep, slab.getZ() + 0.5);
        player.setDeltaMovement(0.8, 0.3, 0);
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        if (!player.getInventory().getItem(0).is(ModItems.VOID_ANCHOR.get())) bad.add("a climbing glider was caught");
        player.setDeltaMovement(0.8, -0.4, 0);
        com.panda.promisedend.logic.VoidAnchor.tick(player);
        if (player.getY() <= deep) bad.add("a descending glider " + com.panda.promisedend.logic.VoidAnchor.GLIDER_DEPTH + " under the floor wasn't caught");
        player.stopFallFlying();
        com.panda.promisedend.logic.VoidAnchor.forget(player);
        if (!bad.isEmpty()) helper.fail("anchor landing: " + bad);
        helper.succeed();
    }

    /**
     * With no footing in the End, the rescue lands on the arrival platform; with the platform gone, on
     * the main island's top; with neither, it rebuilds vanilla's obsidian platform and lands there.
     */
    private static void voidAnchorEndFallback(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) {
            helper.fail("no End level on the test server");
            return;
        }
        BlockPos spawn = net.minecraft.server.level.ServerLevel.END_SPAWN_POINT;
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        var obsidian = net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState();
        List<String> bad = new ArrayList<>();
        // the platform stands
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            end.setBlockAndUpdate(spawn.offset(dx, -1, dz), obsidian);
            for (int dy = 0; dy < 3; dy++) end.setBlockAndUpdate(spawn.offset(dx, dy, dz), air);
        }
        BlockPos first = com.panda.promisedend.logic.VoidAnchor.endFallback(end);
        if (!first.equals(spawn)) bad.add("with the platform standing, landed at " + first);
        // the platform gone: the island's top (on the test server, its floor) walking in from the platform
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) end.setBlockAndUpdate(spawn.offset(dx, -1, dz), air);
        BlockPos second = com.panda.promisedend.logic.VoidAnchor.endFallback(end);
        if (!com.panda.promisedend.logic.VoidAnchor.isSafeGround(end, second)) bad.add("with no platform, landed on unsafe ground at " + second);
        // nothing at all from x 10 to 106 along z = 0: the platform is rebuilt
        for (int x = 10; x <= 106; x++) for (int z = -4; z <= 4; z++) for (int y = end.getMinY(); y < end.getMinY() + 12; y++) {
            end.setBlockAndUpdate(new BlockPos(x, y, z), air);
        }
        BlockPos third = com.panda.promisedend.logic.VoidAnchor.endFallback(end);
        if (!third.equals(spawn) || !end.getBlockState(spawn.below()).is(net.minecraft.world.level.block.Blocks.OBSIDIAN)) {
            bad.add("with nothing to stand on, landed at " + third + " over " + end.getBlockState(spawn.below()));
        }
        if (!bad.isEmpty()) helper.fail("End fallback: " + bad);
        helper.succeed();
    }

    /**
     * The Rim Outpost is 11×11: its purpur corner pillars stand 10 blocks apart on both axes, on an island
     * of its own.
     */
    private static void rimOutpostFootprint(GameTestHelper helper) {
        var template = helper.getLevel().getServer().getStructureTemplateManager().get(PromisedEnd.id("rim_outpost"));
        if (template.isEmpty()) {
            helper.fail("no rim_outpost template");
            return;
        }
        var pillars = template.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                net.minecraft.world.level.block.Blocks.PURPUR_PILLAR);
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (var p : pillars) {
            minX = Math.min(minX, p.pos().getX());
            maxX = Math.max(maxX, p.pos().getX());
            minZ = Math.min(minZ, p.pos().getZ());
            maxZ = Math.max(maxZ, p.pos().getZ());
            minY = Math.min(minY, p.pos().getY());
            maxY = Math.max(maxY, p.pos().getY());
        }
        PromisedEnd.LOGGER.info("rim outpost: {} pillar blocks, {}x{} across, {} tall with the spire, template {}", pillars.size(),
                maxX - minX + 1, maxZ - minZ + 1, maxY - minY + 1, template.get().getSize());
        // each corner pillar's span (the roof layer crosses it, so count from its lowest to its highest block): three
        // stand floor to parapet (7), the south-west one carries the spire (10)
        java.util.Map<Long, int[]> columns = new java.util.HashMap<>();
        for (var p : pillars) {
            int[] span = columns.computeIfAbsent(((long) p.pos().getX() << 32) | (p.pos().getZ() & 0xffffffffL), k -> new int[] {Integer.MAX_VALUE, Integer.MIN_VALUE});
            span[0] = Math.min(span[0], p.pos().getY());
            span[1] = Math.max(span[1], p.pos().getY());
        }
        List<Integer> heights = columns.values().stream().map(span -> span[1] - span[0] + 1).sorted().toList();
        PromisedEnd.LOGGER.info("rim outpost corner pillars: {}", heights);
        List<String> bad = new ArrayList<>();
        if (maxX - minX + 1 != 11 || maxZ - minZ + 1 != 11) bad.add("the outpost isn't 11×11: " + (maxX - minX + 1) + "×" + (maxZ - minZ + 1));
        if (!heights.equals(List.of(7, 7, 7, 10))) bad.add("corner pillars aren't 7, 7, 7 and the spire's 10: " + heights);
        if (!bad.isEmpty()) helper.fail("outpost: " + bad);
        helper.succeed();
    }

    /**
     * Act I's goals as a player meets them: Voidstone in the inventory ticks "Take the deep stone"; a Cracked Void
     * Anchor (the outpost chest's) doesn't tick "Make an anchor", the anchor recharged from it does.
     */
    private static void actOneGoalsByInventory(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var player = helper.makeMockServerPlayerInLevel();
        List<String> bad = new ArrayList<>();
        var mine = server.getAdvancements().get(PromisedEnd.id("rim/mine_voidstone"));
        var forge = server.getAdvancements().get(PromisedEnd.id("rim/forge_anchor"));
        if (mine == null || forge == null) {
            helper.fail("Act I's advancements are missing");
            return;
        }
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModBlocks.VOIDSTONE_SET.base().get()));
        player.inventoryMenu.broadcastChanges();
        if (!player.getAdvancements().getOrStartProgress(mine).isDone()) bad.add("Voidstone in the inventory didn't tick mine_voidstone");
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.CRACKED_VOID_ANCHOR.get()));
        player.inventoryMenu.broadcastChanges();
        if (player.getAdvancements().getOrStartProgress(forge).isDone()) bad.add("a cracked anchor ticked forge_anchor");
        player.getInventory().add(new net.minecraft.world.item.ItemStack(ModItems.VOID_ANCHOR.get()));
        player.inventoryMenu.broadcastChanges();
        if (!player.getAdvancements().getOrStartProgress(forge).isDone()) bad.add("an anchor in the inventory didn't tick forge_anchor");
        // the outpost chest gives a cracked anchor, never a charged one
        var table = server.reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, PromisedEnd.id("chests/rim_outpost")));
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(helper.getLevel())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, player.position())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        int cracked = 0;
        for (int i = 0; i < 60; i++) {
            for (var stack : table.getRandomItems(params, 1000 + i)) {
                if (stack.is(ModItems.VOID_ANCHOR.get())) bad.add("an outpost chest rolled a charged Void Anchor");
                if (stack.is(ModItems.CRACKED_VOID_ANCHOR.get())) cracked++;
            }
        }
        if (cracked == 0) bad.add("no outpost chest in 60 rolled a cracked anchor");
        if (!bad.isEmpty()) helper.fail("Act I goals: " + bad);
        helper.succeed();
    }

    /**
     * Footing on a chunk's edge whose neighbour chunk isn't loaded: the neighbour is loaded and read, not taken for
     * void (the test End's floor is ground there).
     */
    private static void voidAnchorUnloadedNeighbour(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) {
            helper.fail("no End level on the test server");
            return;
        }
        int cx = 3000 + (int) (end.getGameTime() % 500) * 3;           // a fresh chunk each run of the test server
        BlockPos feet = new BlockPos(cx * 16 + 15, 64, 8);
        end.getChunk(cx, 0);
        if (end.getChunkSource().hasChunk(cx + 1, 0)) {
            helper.fail("the neighbour chunk is already loaded: the test proves nothing");
            return;
        }
        end.setBlockAndUpdate(feet.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        if (!com.panda.promisedend.logic.VoidAnchor.isSafeGround(end, feet)) helper.fail("footing beside an unloaded chunk was taken for the void's edge");
        end.setBlockAndUpdate(feet.below(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    /** {@code /promisedend codex reset <player>} forgets our goals and leaves vanilla's (as @s: every mock player shares one name). */
    private static void codexResetCommand(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var player = helper.makeMockServerPlayerInLevel();
        var forge = server.getAdvancements().get(PromisedEnd.id("rim/forge_anchor"));
        var vanilla = server.getAdvancements().get(com.panda.promisedend.logic.CodexBook.KILL_DRAGON);
        if (forge == null || vanilla == null) {
            helper.fail("advancements missing");
            return;
        }
        forge.value().criteria().keySet().forEach(c -> player.getAdvancements().award(forge, c));
        vanilla.value().criteria().keySet().forEach(c -> player.getAdvancements().award(vanilla, c));
        List<String> bad = new ArrayList<>();
        try {
            int n = server.getCommands().getDispatcher().execute("promisedend codex reset @s",
                    server.createCommandSourceStack().withEntity(player).withSuppressedOutput());
            if (n != 1) bad.add("the command reset " + n + " players, not 1");
        } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
            bad.add("the command failed: " + e.getMessage());
        }
        if (player.getAdvancements().getOrStartProgress(forge).isDone()) bad.add("forge_anchor still done after the reset");
        if (!player.getAdvancements().getOrStartProgress(vanilla).isDone()) bad.add("the reset took vanilla's kill_dragon too");
        if (!bad.isEmpty()) helper.fail("codex reset: " + bad);
        helper.succeed();
    }

    /** Act I's "Cross to the Rim" by distance, so it can be done with the terrain off (RimProgress). */
    // ---- the Crystal Warden ---------------------------------------------------------------------------------------

    private static com.panda.promisedend.entity.CrystalWarden warden(GameTestHelper helper, int x, int y, int z) {
        return helper.spawn(com.panda.promisedend.registry.ModEntities.CRYSTAL_WARDEN.get(), new BlockPos(x, y, z));
    }

    /** A husk with no AI stands in for the target: it never moves, and magic damage counts on it as on a player. */
    private static net.minecraft.world.entity.Mob dummy(GameTestHelper helper, int x, int y, int z) {
        var husk = helper.spawn(net.minecraft.world.entity.EntityTypes.HUSK, new BlockPos(x, y, z));
        husk.setNoAi(true);
        return husk;
    }

    /**
     * 30 health and 4 armour; it flies, takes no fall damage (by the vanilla tag), has its loot table, its sounds exist,
     * and its beam's damage type is loaded and tagged to go through armour and the hurt cooldown without knockback.
     */
    private static void crystalWardenStats(GameTestHelper helper) {
        var warden = warden(helper, 3, 3, 3);
        List<String> bad = new ArrayList<>();
        if (warden.getMaxHealth() != 30.0F) bad.add("max health " + warden.getMaxHealth());
        if (warden.getArmorValue() != 4) bad.add("armour " + warden.getArmorValue());
        if (!(warden.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.FlyingPathNavigation)) bad.add("doesn't fly: " + warden.getNavigation());
        if (!warden.is(net.minecraft.tags.EntityTypeTags.FALL_DAMAGE_IMMUNE)) bad.add("not in #minecraft:fall_damage_immune");
        if (warden.causeFallDamage(20, 1, helper.getLevel().damageSources().fall())) bad.add("takes fall damage");
        if (!warden.is(com.panda.promisedend.registry.ModTags.CONSTRUCTS)) bad.add("not in #promisedend:constructs");
        var beam = helper.getLevel().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).get(com.panda.promisedend.entity.CrystalWarden.BEAM_DAMAGE_TYPE);
        if (beam.isEmpty()) {
            bad.add("no damage type promisedend:crystal_beam");
        } else {
            for (var tag : List.of(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR, net.minecraft.tags.DamageTypeTags.BYPASSES_COOLDOWN,
                    net.minecraft.tags.DamageTypeTags.NO_KNOCKBACK)) {
                if (!beam.get().is(tag)) bad.add("crystal_beam not in #" + tag.location());
            }
            if (beam.get().value().scaling() != net.minecraft.world.damagesource.DamageScaling.NEVER) bad.add("crystal_beam scales with difficulty");
        }
        String loot = warden.getLootTable().map(k -> k.identifier().toString()).orElse("none");
        if (!loot.equals("promisedend:entities/crystal_warden")) bad.add("loot table " + loot);
        else if (helper.getLevel().getServer().reloadableRegistries().getLootTable(warden.getLootTable().orElseThrow())
                == net.minecraft.world.level.storage.loot.LootTable.EMPTY) bad.add("the loot table doesn't load");
        for (var sound : List.of(com.panda.promisedend.registry.ModSounds.CRYSTAL_WARDEN_AMBIENT, com.panda.promisedend.registry.ModSounds.CRYSTAL_WARDEN_HURT,
                com.panda.promisedend.registry.ModSounds.CRYSTAL_WARDEN_DEATH, com.panda.promisedend.registry.ModSounds.CRYSTAL_WARDEN_LOCK_ON,
                com.panda.promisedend.registry.ModSounds.CRYSTAL_WARDEN_BEAM, com.panda.promisedend.registry.ModSounds.CRYSTAL_WARDEN_POWER_DOWN)) {
            if (net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getKey(sound.get()) == null) bad.add("unregistered sound " + sound.get().location());
        }
        if (!bad.isEmpty()) helper.fail("Crystal Warden: " + bad);
        helper.succeed();
    }

    /**
     * The lock-on is a full second of tell with no damage (still locking at 18 ticks, beaming by 23), then the beam deals
     * 2 a second: a point on its first tick and one every 10 ticks after, through armour.
     */
    private static void crystalWardenLockThenBeam(GameTestHelper helper) {
        var warden = warden(helper, 2, 3, 6);
        var target = dummy(helper, 9, 1, 6);
        float full = target.getHealth();
        warden.startLockOn(target);
        helper.runAtTickTime(18, () -> {
            if (warden.phase() != com.panda.promisedend.entity.CrystalWarden.Phase.LOCK) helper.fail("at 18 ticks it should still be locking on, is " + warden.phase());
            if (target.getHealth() != full) helper.fail("damage during the lock-on: " + target.getHealth());
        });
        helper.runAtTickTime(23, () -> {
            if (!warden.isBeaming(target)) helper.fail("at 23 ticks it should be beaming, is " + warden.phase());
            if (target.getHealth() != full - 1) helper.fail("the beam's first tick should deal 1, health " + target.getHealth());
        });
        helper.runAtTickTime(66, () -> {
            // beam ticks 0, 10, 20, 30 and 40 have landed by now: 5 points in a little over 2 s
            float dealt = full - target.getHealth();
            if (dealt != 5) helper.fail("after about 2 s of beam it dealt " + dealt + ", wanted 5 (2 a second)");
            helper.succeed();
        });
    }

    /**
     * Breaking line of sight ends the lock-on: no beam, no damage. The dummy stays its target, so once its rest is over it
     * would lock on again by itself; it doesn't while the wall stands, and does within half a second of the wall going.
     */
    private static void crystalWardenCoverBreaksLock(GameTestHelper helper) {
        var warden = warden(helper, 2, 3, 6);
        var target = dummy(helper, 9, 1, 6);
        float full = target.getHealth();
        warden.setTarget(target);
        warden.startLockOn(target);
        helper.runAtTickTime(8, () -> wall(helper, net.minecraft.world.level.block.Blocks.STONE));
        helper.runAtTickTime(12, () -> {
            if (warden.phase() != com.panda.promisedend.entity.CrystalWarden.Phase.IDLE) helper.fail("behind cover it still is " + warden.phase());
        });
        // its rest (40 ticks) ended about tick 49: from then on only the wall keeps it idle
        helper.runAtTickTime(68, () -> {
            if (target.getHealth() != full) helper.fail("the target took damage behind cover: " + target.getHealth());
            if (warden.phase() != com.panda.promisedend.entity.CrystalWarden.Phase.IDLE) helper.fail("it locked on through the wall: " + warden.phase());
            if (warden.getTarget() != target) helper.fail("it lost its target, so the wall proved nothing: " + warden.getTarget());
            wall(helper, net.minecraft.world.level.block.Blocks.AIR);
        });
        helper.runAtTickTime(78, () -> {
            if (warden.phase() == com.panda.promisedend.entity.CrystalWarden.Phase.IDLE) helper.fail("with the wall gone it didn't lock on again");
            helper.succeed();
        });
    }

    private static void wall(GameTestHelper helper, net.minecraft.world.level.block.Block block) {
        for (int y = 1; y <= 8; y++) for (int z = 2; z <= 10; z++) helper.setBlock(new BlockPos(6, y, z), block);
    }

    /**
     * A beam in plain sight is held for BEAM_TICKS (4 s, 8 points) and no longer; then it rests REST_TICKS (2 s) and
     * locks on again by itself.
     */
    private static void crystalWardenBeamCapAndRest(GameTestHelper helper) {
        var warden = warden(helper, 2, 3, 6);
        var target = dummy(helper, 9, 1, 6);
        float full = target.getHealth();
        warden.setTarget(target);
        warden.startLockOn(target);
        // lock-on ends about tick 20, the beam about tick 101, the rest about tick 141
        helper.runAtTickTime(97, () -> {
            if (!warden.isBeaming(target)) helper.fail("at 97 ticks it should still be beaming, is " + warden.phase());
        });
        helper.runAtTickTime(106, () -> {
            if (warden.phase() != com.panda.promisedend.entity.CrystalWarden.Phase.IDLE) helper.fail("the beam ran past 4 s: " + warden.phase());
            if (full - target.getHealth() != 8) helper.fail("a 4 s beam should deal 8, dealt " + (full - target.getHealth()));
        });
        helper.runAtTickTime(136, () -> {
            if (warden.phase() != com.panda.promisedend.entity.CrystalWarden.Phase.IDLE) helper.fail("it should be resting, is " + warden.phase());
            if (full - target.getHealth() != 8) helper.fail("damage while resting: dealt " + (full - target.getHealth()));
        });
        helper.runAtTickTime(150, () -> {
            if (warden.phase() == com.panda.promisedend.entity.CrystalWarden.Phase.IDLE) helper.fail("after its rest it didn't lock on again");
            helper.succeed();
        });
    }

    /** It never works on the dragon or on another construct, even locked on to one; it does on anything else. */
    private static void crystalWardenIgnoresTheDragon(GameTestHelper helper) {
        var warden = warden(helper, 2, 3, 6);
        var other = warden(helper, 9, 3, 6);
        var dragon = net.minecraft.world.entity.EntityTypes.ENDER_DRAGON.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        var husk = dummy(helper, 5, 1, 2);
        List<String> bad = new ArrayList<>();
        if (dragon == null || warden.canAttack(dragon)) bad.add("it can attack the dragon");
        if (warden.canAttack(other)) bad.add("it can attack another construct");
        if (!warden.canAttack(husk)) bad.add("it can't attack a husk, so the test proves nothing");
        if (!bad.isEmpty()) {
            helper.fail("Crystal Warden targets: " + bad);
            return;
        }
        float otherHealth = other.getHealth();
        warden.startLockOn(other);
        helper.runAtTickTime(3, () -> {
            if (warden.phase() != com.panda.promisedend.entity.CrystalWarden.Phase.IDLE) helper.fail("it kept a lock on another construct: " + warden.phase());
        });
        helper.runAtTickTime(40, () -> {
            if (other.getHealth() != otherHealth) helper.fail("it hurt another construct: " + other.getHealth());
            helper.succeed();
        });
    }

    /** Killed by a player, it shatters at once (gone within a couple of ticks, no corpse) and drops a Crystal Fragment. */
    private static void crystalWardenShattersAndDrops(GameTestHelper helper) {
        var warden = warden(helper, 4, 3, 4);
        var player = helper.makeMockServerPlayerInLevel();
        warden.hurtServer(helper.getLevel(), helper.getLevel().damageSources().playerAttack(player), 1000.0F);
        helper.runAtTickTime(3, () -> {
            if (!warden.isRemoved()) helper.fail("3 ticks after dying it is still there (death time " + warden.deathTime + ")");
            var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, warden.getBoundingBox().inflate(4),
                    e -> e.getItem().is(ModItems.CRYSTAL_FRAGMENT.get()));
            if (drops.isEmpty()) helper.fail("a player's kill dropped no Crystal Fragment");
            helper.succeed();
        });
    }

    // ---- the dragon's phase 3 -----------------------------------------------------------------------------------

    /** A dragon that holds still (its AI would fly it off round the level). */
    /**
     * A dragon that stays put, at vanilla's 200 health whatever the world's Wrath (marked as already scaled, so a Wrath
     * test running at the same moment can't change it under another test).
     */
    private static net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon(GameTestHelper helper, int x, int y, int z) {
        var dragon = helper.spawn(net.minecraft.world.entity.EntityTypes.ENDER_DRAGON, new BlockPos(x, y, z));
        dragon.setNoAi(true);
        dragon.addTag(com.panda.promisedend.logic.DragonWrath.SCALED_TAG);
        return dragon;
    }

    // ---- Dragon Wrath, the hoard and the scale ------------------------------------------------------------------

    /**
     * A dragon that dies raises the world's Wrath by one once it has vanished (not while dying), the next dragon's health
     * is scaled by the new Wrath and starts full, and {@code /promisedend wrath set|get} sets and reads it. The world's
     * Wrath is put back as it was.
     */
    private static void wrathIncrementsOnKill(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        int before = com.panda.promisedend.logic.DragonWrath.stored(server);
        com.panda.promisedend.logic.DragonWrath.setStored(server, 1);
        List<String> bad = new ArrayList<>();
        var dragon = dragon(helper, 6, 2, 6);
        net.minecraft.world.entity.boss.enderdragon.EnderDragon[] next = new net.minecraft.world.entity.boss.enderdragon.EnderDragon[1];
        helper.runAtTickTime(2, () -> dragon.setHealth(0.0F));
        helper.runAtTickTime(100, () -> {
            if (com.panda.promisedend.logic.DragonWrath.dyingAt(dragon) != 1) bad.add("the dying dragon wasn't noted at Wrath 1");
            if (com.panda.promisedend.logic.DragonWrath.stored(server) != 1) bad.add("Wrath rose while the dragon was still dying");
        });
        helper.runAtTickTime(212, () -> {
            if (!dragon.isRemoved()) bad.add("the dragon hadn't vanished 210 ticks after it died");
            if (com.panda.promisedend.logic.DragonWrath.stored(server) != 2) bad.add("after the kill Wrath is " + com.panda.promisedend.logic.DragonWrath.stored(server) + ", wanted 2");
            next[0] = helper.spawn(net.minecraft.world.entity.EntityTypes.ENDER_DRAGON, new BlockPos(6, 2, 6));
            next[0].setNoAi(true);
        });
        helper.runAtTickTime(216, () -> {
            try {
                float want = com.panda.promisedend.logic.DragonWrath.maxHealth(2);
                if (want != 300.0F) bad.add("Wrath 2 is " + want + " health, wanted 300");
                if (next[0].getMaxHealth() != want) bad.add("the next dragon's max health is " + next[0].getMaxHealth() + ", wanted " + want);
                if (next[0].getHealth() != next[0].getMaxHealth()) bad.add("the next dragon starts at " + next[0].getHealth() + " of " + next[0].getMaxHealth());
                if (!next[0].entityTags().contains(com.panda.promisedend.logic.DragonWrath.FOUGHT_AT_TAG + 2)) bad.add("the next dragon doesn't keep the Wrath it is fought at: " + next[0].entityTags());
                // its boss bar shows its name: "Ender Dragon · Wrath 2"
                if (!(next[0].getCustomName() != null && next[0].getCustomName().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents named
                        && named.getKey().equals("entity.promisedend.ender_dragon.wrath") && java.util.Arrays.asList(named.getArgs()).contains(2)))
                    bad.add("the next dragon isn't named for its Wrath: " + next[0].getCustomName());
                next[0].discard();
                var source = server.createCommandSourceStack().withSuppressedOutput();
                int set = server.getCommands().getDispatcher().execute("promisedend wrath set 4", source);
                int got = server.getCommands().getDispatcher().execute("promisedend wrath get", source);
                if (set != 4 || got != 4 || com.panda.promisedend.logic.DragonWrath.stored(server) != 4) bad.add("wrath set 4 / get gave " + set + " / " + got);
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException e) {
                bad.add("the wrath command failed: " + e.getMessage());
            } finally {
                com.panda.promisedend.logic.DragonWrath.setStored(server, before);
            }
            if (!bad.isEmpty()) helper.fail("wrath: " + bad);
            helper.succeed();
        });
    }

    /**
     * Once a dragon has vanished its hoard is set in a chest (off the End: on the ground under where it vanished), from
     * the table of the Wrath it was fought at: 3 + Wrath Dragon Scales, 5 at Wrath 2. The world's Wrath is put back.
     */
    private static void dragonDropsScales(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        int before = com.panda.promisedend.logic.DragonWrath.stored(server);
        com.panda.promisedend.logic.DragonWrath.setStored(server, 2);
        var dragon = dragon(helper, 6, 2, 6);
        helper.runAtTickTime(2, () -> dragon.setHealth(0.0F));
        helper.runAtTickTime(212, () -> {
            List<String> bad = new ArrayList<>();
            try {
                BlockPos at = com.panda.promisedend.logic.DragonWrath.lastHoard();
                if (at == null || !(helper.getLevel().getBlockEntity(at) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest)) {
                    bad.add("no hoard chest, last hoard at " + at);
                } else {
                    // the dragon rises as it dies, past the barrier roof over a test arena, so the chest stands on that roof
                    BlockPos rel = helper.relativePos(at);
                    if (rel.getX() != 6 || rel.getZ() != 6) bad.add("the chest isn't under where the dragon vanished: " + rel);
                    if (!helper.getLevel().getBlockState(at.below()).isFaceSturdy(helper.getLevel(), at.below(), net.minecraft.core.Direction.UP))
                        bad.add("the chest isn't on solid ground: " + helper.getLevel().getBlockState(at.below()));
                    chest.unpackLootTable(null);
                    int scales = 0;
                    for (int i = 0; i < chest.getContainerSize(); i++) {
                        var stack = chest.getItem(i);
                        if (stack.is(com.panda.promisedend.registry.ModItems.DRAGON_SCALE.get())) scales += stack.getCount();
                        else if (!stack.isEmpty()) bad.add("the hoard holds " + stack);
                    }
                    if (scales != com.panda.promisedend.logic.DragonWrath.scales(2) || scales != 5) bad.add("the Wrath-2 hoard holds " + scales + " scales, wanted 5");
                    chest.clearContent();
                    helper.getLevel().removeBlock(at, false); // above the arena, so the test's own clean-up misses it
                }
            } finally {
                com.panda.promisedend.logic.DragonWrath.setStored(server, before);
            }
            if (!bad.isEmpty()) helper.fail("hoard: " + bad);
            helper.succeed();
        });
    }

    /**
     * In an anvil one Dragon Scale repairs half an elytra's durability and two repair the rest; phantom membrane still
     * repairs a quarter (vanilla), and a scale repairs nothing outside its tag (a damaged diamond sword).
     */
    private static void dragonScaleRepairsElytra(GameTestHelper helper) {
        var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var scale = com.panda.promisedend.registry.ModItems.DRAGON_SCALE.get();
        List<String> bad = new ArrayList<>();
        int max = new ItemStack(net.minecraft.world.item.Items.ELYTRA).getMaxDamage();
        java.util.function.BiFunction<ItemStack, ItemStack, ItemStack> anvil = (item, material) -> {
            var menu = new net.minecraft.world.inventory.AnvilMenu(0, player.getInventory());
            menu.getSlot(0).set(item);
            menu.getSlot(1).set(material);
            menu.createResult();
            return menu.getSlot(2).getItem();
        };
        ItemStack elytra = new ItemStack(net.minecraft.world.item.Items.ELYTRA);
        elytra.setDamageValue(400);
        ItemStack one = anvil.apply(elytra.copy(), new ItemStack(scale, 1));
        if (one.isEmpty() || one.getDamageValue() != 400 - max / 2) bad.add("one scale left " + (one.isEmpty() ? "no result" : one.getDamageValue()) + " damage, wanted " + (400 - max / 2));
        ItemStack two = anvil.apply(elytra.copy(), new ItemStack(scale, 2));
        if (two.isEmpty() || two.getDamageValue() != 0) bad.add("two scales left " + (two.isEmpty() ? "no result" : two.getDamageValue()) + " damage, wanted 0");
        ItemStack membrane = anvil.apply(elytra.copy(), new ItemStack(net.minecraft.world.item.Items.PHANTOM_MEMBRANE, 1));
        if (membrane.isEmpty() || membrane.getDamageValue() != 400 - max / 4) bad.add("phantom membrane left " + (membrane.isEmpty() ? "no result" : membrane.getDamageValue()) + ", wanted " + (400 - max / 4));
        ItemStack sword = new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
        sword.setDamageValue(400);
        if (!anvil.apply(sword, new ItemStack(scale, 1)).isEmpty()) bad.add("a scale repairs a diamond sword");
        if (!bad.isEmpty()) helper.fail("scale repair: " + bad);
        helper.succeed();
    }

    private static List<com.panda.promisedend.entity.CrystalWarden> wardensOf(GameTestHelper helper, net.minecraft.world.entity.boss.enderdragon.EnderDragon dragon) {
        return helper.getLevel().getEntitiesOfClass(com.panda.promisedend.entity.CrystalWarden.class, dragon.getBoundingBox().inflate(24),
                w -> dragon.getUUID().equals(w.dragonId()));
    }

    /**
     * The server's own tick puts a dragon into phase 3 once, below a quarter of its health and not before: it calls the
     * Wrath-0 count of wardens (3; a ring round it off the End), each linked to it, persistent, and none more on later ticks; when the
     * dragon is killed (here by the kill command, which removes it without the dying) its wardens shatter.
     */
    private static void dragonPhase3Triggers(GameTestHelper helper) {
        var dragon = dragon(helper, 6, 2, 6);
        // the ring of homes reaches the arena's edge and a warden hovers 3 past its home, into chunks the test doesn't
        // keep ticking: there it would never see its dragon die, as it would in a real fight with a player at it
        Runnable release = forceAround(helper, dragon.blockPosition(), com.panda.promisedend.logic.DragonPhase3.RING_RADIUS + (int) com.panda.promisedend.entity.CrystalWarden.HOVER_RADIUS + 4);
        helper.runAtTickTime(5, () -> {
            if (com.panda.promisedend.logic.DragonPhase3.entered(dragon)) fail(helper, release, "in phase 3 at full health");
            if (!wardensOf(helper, dragon).isEmpty()) fail(helper, release, "wardens at full health");
            dragon.setHealth(dragon.getMaxHealth() * 0.2F);
        });
        helper.runAtTickTime(10, () -> {
            if (!com.panda.promisedend.logic.DragonPhase3.entered(dragon)) fail(helper, release, "at a fifth of its health it isn't in phase 3");
            var wardens = wardensOf(helper, dragon);
            if (wardens.size() != com.panda.promisedend.logic.DragonPhase3.wardenCount(0)) fail(helper, release, "phase 3 called " + wardens.size() + " wardens, wanted 3");
            for (var w : wardens) {
                if (!w.isPersistenceRequired()) fail(helper, release, "a called warden can despawn");
                if (w.home() == null || Math.abs(Math.sqrt(w.home().distToCenterSqr(dragon.getX(), w.home().getY() + 0.5, dragon.getZ()))
                        - com.panda.promisedend.logic.DragonPhase3.RING_RADIUS) > 1.5) fail(helper, release, "a warden's home is off the ring: " + w.home());
            }
        });
        // the kill waits until every warden has ticked in an entity-ticking chunk: a forced chunk's entities load in the
        // background, and on a busy test server a warden called at tick 10 sat 26 ticks in a chunk that wasn't ticking
        // yet, so it never saw its dragon die (the shattering itself is the warden's own tick). Once, under load,
        // one waited past tick 140 too, so the wait runs to tick 400
        long[] killed = {-1};
        for (int t = 30; t <= 420; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (killed[0] < 0) {
                    var wardens = wardensOf(helper, dragon);
                    boolean ready = wardens.stream().allMatch(w -> w.tickCount > 0 && helper.getLevel().isPositionEntityTicking(w.blockPosition()));
                    if (!ready && tick < 400) return;
                    if (wardens.size() != com.panda.promisedend.logic.DragonPhase3.wardenCount(0)) fail(helper, release, "it called wardens again");
                    PromisedEnd.LOGGER.info("[dragon_phase3_triggers] wardens ready {}, the dragon killed at tick {}", ready, tick);
                    dragon.kill(helper.getLevel());
                    killed[0] = tick;
                } else if (tick == killed[0] + 6) {
                    var left = wardensOf(helper, dragon);
                    release.run();
                    if (!left.isEmpty()) helper.fail(left.size() + " wardens outlived their dragon (killed at tick " + killed[0] + "): " + left.stream().map(w -> String.format(java.util.Locale.ROOT,
                            "[alive %s, removed %s, health %.1f, ticks %d, no AI %s, entity-ticking %s, at %.1f %.1f %.1f, %.1f from the dragon]",
                            w.isAlive(), w.getRemovalReason(), w.getHealth(), w.tickCount, w.isNoAi(),
                            helper.getLevel().isPositionEntityTicking(w.blockPosition()), w.getX(), w.getY(), w.getZ(), w.distanceTo(dragon))).toList());
                    helper.succeed();
                }
            });
        }
    }

    /** Keeps the chunks within {@code reach} of a spot loaded and ticking until the returned clean-up runs. */
    /** Fails the test after letting go of the chunks it forced, which would otherwise stay forced for the run. */
    private static void fail(GameTestHelper helper, Runnable release, String message) {
        release.run();
        helper.fail(message);
    }

    private static Runnable forceAround(GameTestHelper helper, BlockPos at, int reach) {
        var level = helper.getLevel();
        List<net.minecraft.world.level.ChunkPos> forced = new ArrayList<>();
        for (int cx = (at.getX() - reach) >> 4; cx <= (at.getX() + reach) >> 4; cx++) {
            for (int cz = (at.getZ() - reach) >> 4; cz <= (at.getZ() + reach) >> 4; cz++) {
                if (level.setChunkForced(cx, cz, true)) forced.add(new net.minecraft.world.level.ChunkPos(cx, cz));
            }
        }
        return () -> forced.forEach(c -> level.setChunkForced(c.x(), c.z(), false));
    }

    /**
     * Phase 3 calls as many Wardens as the Wrath the dragon is fought at (saved on it): 4 at Wrath 3, 5 at Wrath 5, read
     * from each dragon, not from the world's Wrath.
     */
    private static void dragonPhase3WardensByWrath(GameTestHelper helper) {
        var three = dragon(helper, 3, 2, 3);
        var five = dragon(helper, 9, 2, 9);
        three.addTag(com.panda.promisedend.logic.DragonWrath.FOUGHT_AT_TAG + 3);
        five.addTag(com.panda.promisedend.logic.DragonWrath.FOUGHT_AT_TAG + 5);
        helper.runAtTickTime(2, () -> {
            three.setHealth(three.getMaxHealth() * 0.2F);
            five.setHealth(five.getMaxHealth() * 0.2F);
        });
        helper.runAtTickTime(8, () -> {
            int a = wardensOf(helper, three).size(), b = wardensOf(helper, five).size();
            three.kill(helper.getLevel());
            five.kill(helper.getLevel());
            if (a != 4 || b != 5) helper.fail("Wrath 3 called " + a + " Wardens (wanted 4), Wrath 5 called " + b + " (wanted 5)");
        });
        helper.runAtTickTime(14, helper::succeed);
    }

    /**
     * Stepping out of the ring before the breath lands is the counter: no damage and no slowness. The dragon here
     * hovers (as if perched) when the telegraph ends, so the breath lands without the charge and it is left perched.
     */
    private static void voidBreathDodged(GameTestHelper helper) {
        var dragon = dragon(helper, 6, 7, 6);
        dragon.addTag(com.panda.promisedend.logic.DragonPhase3.TAG);
        var victim = dummy(helper, 4, 1, 6);
        float full = victim.getHealth();
        com.panda.promisedend.logic.DragonPhase3.startDive(helper.getLevel(), dragon, victim.position());
        // during the telegraph it steps 5 blocks away, out of the 3-block ring
        helper.runAtTickTime(15, () -> victim.teleportTo(victim.getX() + 5, victim.getY(), victim.getZ() + 0.0));
        helper.runAtTickTime(80, () -> {
            float dealt = full - victim.getHealth();
            var slowness = victim.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);
            dragon.discard();
            if (dealt != 0.0F) helper.fail("a victim out of the ring took " + dealt);
            if (slowness != null) helper.fail("a victim out of the ring was slowed");
            if (!com.panda.promisedend.logic.DragonPhase3.lastLanding(dragon).equals("without a charge")
                    || dragon.getPhaseManager().getCurrentPhase().getPhase() != net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOVERING)
                helper.fail("a perched dragon was charged off its perch: " + com.panda.promisedend.logic.DragonPhase3.lastLanding(dragon) + ", "
                        + dragon.getPhaseManager().getCurrentPhase().getPhase());
            helper.succeed();
        });
    }

    /** A linked warden heals its dragon 1 a second while it beams, and not while it locks on. */
    private static void wardenHealsDragon(GameTestHelper helper) {
        var dragon = dragon(helper, 6, 7, 10);
        dragon.setHealth(100.0F);
        var warden = warden(helper, 2, 3, 6);
        var target = dummy(helper, 9, 1, 6);
        warden.setDragon(dragon);
        warden.setTarget(target);
        warden.startLockOn(target);
        helper.runAtTickTime(15, () -> {
            if (dragon.getHealth() != 100.0F) helper.fail("the dragon healed during the lock-on: " + dragon.getHealth());
        });
        helper.runAtTickTime(66, () -> {
            // beam ticks 0, 20 and 40 have landed: 3 health in a little over 2 s
            float healed = dragon.getHealth() - 100.0F;
            String state = "dragon removed " + dragon.getRemovalReason() + ", max " + dragon.getMaxHealth() + ", beams " + warden.beams()
                    + ", warden's dragon " + warden.dragonId() + " (this one " + dragon.getUUID() + ")";
            dragon.discard();
            if (!warden.isBeaming(target)) helper.fail("it should still be beaming, is " + warden.phase() + "; " + state);
            if (healed != 3.0F) helper.fail("after about 2 s of beam the dragon healed " + healed + ", wanted 3 (1 a second); " + state);
            helper.succeed();
        });
    }

    /**
     * Three of a dragon's wardens, one target: no more than two lock on to or beam it at once, and the third waits its
     * turn (the target is kept at full health so the beams go on).
     */
    private static void calledWardensTakeTurns(GameTestHelper helper) {
        var dragon = dragon(helper, 6, 7, 10);
        var target = dummy(helper, 9, 1, 6);
        List<com.panda.promisedend.entity.CrystalWarden> wardens = List.of(warden(helper, 2, 3, 3), warden(helper, 2, 3, 6), warden(helper, 2, 3, 9));
        for (var w : wardens) {
            w.setDragon(dragon);
            w.setTarget(target);
        }
        int[] most = {0};
        for (int t = 1; t <= 90; t++) {
            helper.runAtTickTime(t, () -> {
                target.setHealth(target.getMaxHealth());
                int working = (int) wardens.stream().filter(w -> w.phase() != com.panda.promisedend.entity.CrystalWarden.Phase.IDLE && w.beamTargetId() == target.getId()).count();
                most[0] = Math.max(most[0], working);
                if (working > com.panda.promisedend.entity.CrystalWarden.CALLED_ON_ONE_TARGET) helper.fail(working + " wardens on one target at once");
            });
        }
        helper.runAtTickTime(91, () -> {
            dragon.discard();
            if (most[0] != com.panda.promisedend.entity.CrystalWarden.CALLED_ON_ONE_TARGET) helper.fail("at most " + most[0] + " worked on the target at once, wanted 2");
            if (wardens.stream().mapToInt(com.panda.promisedend.entity.CrystalWarden::beams).sum() < 2) helper.fail("fewer than 2 beams in 4.5 s");
            helper.succeed();
        });
    }

    /**
     * The dive: a 1.5 s telegraph with no breath yet, then the charge, then void breath on the mark (radius 3, 6 s,
     * Slowness II, 2 damage a second) that leaves the dragon's constructs alone.
     */
    private static void dragonVoidBreathDive(GameTestHelper helper) {
        var dragon = dragon(helper, 6, 7, 6);
        dragon.addTag(com.panda.promisedend.logic.DragonPhase3.TAG);
        // in flight (a fresh dragon starts hovering, which counts as perched)
        dragon.getPhaseManager().setPhase(net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.HOLDING_PATTERN);
        var victim = dummy(helper, 6, 1, 6);
        var construct = warden(helper, 7, 2, 6);
        construct.setNoAi(true);
        float full = victim.getHealth();
        net.minecraft.world.phys.Vec3 mark = victim.position();
        com.panda.promisedend.logic.DragonPhase3.startDive(helper.getLevel(), dragon, mark);
        java.util.function.Supplier<List<net.minecraft.world.entity.AreaEffectCloud>> clouds = () -> helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.AreaEffectCloud.class, new net.minecraft.world.phys.AABB(mark, mark).inflate(2),
                c -> c.entityTags().contains(com.panda.promisedend.logic.DragonPhase3.BREATH_TAG));
        // the ring clients draw: a mark on the spot, telegraph, then charge, then the breath's edge
        java.util.function.Supplier<List<com.panda.promisedend.entity.DiveMark>> marks = () -> helper.getLevel().getEntitiesOfClass(
                com.panda.promisedend.entity.DiveMark.class, new net.minecraft.world.phys.AABB(mark, mark).inflate(0.5));
        helper.runAtTickTime(20, () -> {
            if (!clouds.get().isEmpty()) helper.fail("the breath landed during the telegraph");
            if (victim.getHealth() != full) helper.fail("damage during the telegraph");
            var shown = marks.get();
            if (shown.size() != 1 || shown.get(0).stage() != com.panda.promisedend.entity.DiveMark.Stage.TELEGRAPH)
                helper.fail("wanted one telegraph mark on the spot, found " + shown.stream().map(m -> m.stage().name()).toList());
        });
        helper.runAtTickTime(40, () -> {
            var shown = marks.get();
            if (shown.size() != 1 || shown.get(0).stage() == com.panda.promisedend.entity.DiveMark.Stage.TELEGRAPH)
                helper.fail("after the telegraph the mark should be charging or breathing, found " + shown.stream().map(m -> m.stage().name()).toList());
            var found = clouds.get();
            if (found.size() != 1) {
                helper.fail("wanted one cloud of void breath on the mark, found " + found.size());
                return;
            }
            var cloud = found.get(0);
            if (cloud.getRadius() != com.panda.promisedend.logic.DragonPhase3.BREATH_RADIUS) helper.fail("the cloud's radius is " + cloud.getRadius());
            if (cloud.getDuration() != com.panda.promisedend.logic.DragonPhase3.BREATH_TICKS) helper.fail("the cloud lasts " + cloud.getDuration());
            if (dragon.getPhaseManager().getCurrentPhase().getPhase() != net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase.CHARGING_PLAYER)
                helper.fail("the dragon didn't charge the mark");
        });
        helper.runAtTickTime(80, () -> {
            // about 49 ticks of breath: hits at 1, 21 and 41
            float dealt = full - victim.getHealth();
            var slowness = victim.getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);
            dragon.discard();
            if (dealt != 6.0F) helper.fail("about 2.5 s in the breath dealt " + dealt + ", wanted 6 (2 a second)");
            if (slowness == null || slowness.getAmplifier() != 1) helper.fail("no Slowness II in the breath: " + slowness);
            if (construct.getHealth() != construct.getMaxHealth()) helper.fail("the breath hurt a construct: " + construct.getHealth());
            var shown = marks.get();
            if (shown.size() != 1 || shown.get(0).stage() != com.panda.promisedend.entity.DiveMark.Stage.BREATH)
                helper.fail("while the breath lasts its edge should show, found " + shown.stream().map(m -> m.stage().name()).toList());
            shown.forEach(net.minecraft.world.entity.Entity::discard);
            helper.succeed();
        });
    }

    /**
     * The shipped rig matches the class (the design and Java contract): the animation file has exactly the clips the class
     * plays, every clip moves only bones the model has, the texture and its glowmask exist, and every sound event of ours
     * has a sounds.json entry that points at a vanilla event that exists.
     */
    private static void constructAssetsMatch(GameTestHelper helper) {
        List<String> bad = new ArrayList<>();
        String base = "/assets/promisedend/";
        var anim = readJson(base + "geckolib/animations/entity/crystal_warden.animation.json");
        var geo = readJson(base + "geckolib/models/entity/crystal_warden.geo.json");
        if (anim == null || geo == null) {
            helper.fail("the warden's animation or model file is missing from the jar");
            return;
        }
        var clips = new java.util.TreeSet<>(anim.getAsJsonObject("animations").keySet());
        if (!clips.equals(new java.util.TreeSet<>(com.panda.promisedend.entity.CrystalWarden.CLIPS)))
            bad.add("clips " + clips + " but the class plays " + com.panda.promisedend.entity.CrystalWarden.CLIPS);
        var bones = new java.util.HashSet<String>();
        geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")
                .forEach(b -> bones.add(b.getAsJsonObject().get("name").getAsString()));
        anim.getAsJsonObject("animations").entrySet().forEach(e -> e.getValue().getAsJsonObject().getAsJsonObject("bones").keySet().forEach(b -> {
            if (!bones.contains(b)) bad.add(e.getKey() + " moves a bone the model lacks: " + b);
        }));
        var lockOn = anim.getAsJsonObject("animations").getAsJsonObject("lock_on");
        if (lockOn == null || Math.round(lockOn.get("animation_length").getAsDouble() * 20) != com.panda.promisedend.entity.CrystalWarden.LOCK_ON_TICKS)
            bad.add("the lock_on clip isn't LOCK_ON_TICKS long");
        String lens = openLens(geo, anim);
        if (lens != null) bad.add(lens);
        for (String tex : List.of("textures/entity/crystal_warden.png", "textures/entity/crystal_warden_glowmask.png")) {
            if (EndGameTests.class.getResource(base + tex) == null) bad.add("no " + tex);
        }
        var sounds = readJson(base + "sounds.json");
        if (sounds == null) {
            bad.add("no sounds.json");
        } else {
            for (var id : net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.keySet()) {
                if (id.getNamespace().equals(PromisedEnd.MODID) && !sounds.has(id.getPath())) bad.add("no sounds.json entry for " + id);
            }
            var lang = net.minecraft.locale.Language.getInstance();
            sounds.entrySet().forEach(e -> {
                var subtitle = e.getValue().getAsJsonObject().get("subtitle");
                if (subtitle == null || !lang.has(subtitle.getAsString())) bad.add(e.getKey() + " has no subtitle in en_us");
            });
            for (String key : List.of("death.attack.promisedend.crystal_beam", "death.attack.promisedend.crystal_beam.player")) {
                if (!lang.has(key)) bad.add("no death message " + key);
            }
            sounds.entrySet().forEach(e -> e.getValue().getAsJsonObject().getAsJsonArray("sounds").forEach(v -> {
                var o = v.getAsJsonObject();
                if (o.has("type") && o.get("type").getAsString().equals("event")
                        && !net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(Identifier.parse(o.get("name").getAsString())))
                    bad.add(e.getKey() + " plays a sound event that doesn't exist: " + o.get("name").getAsString());
            }));
        }
        if (!bad.isEmpty()) helper.fail("construct assets: " + bad);
        helper.succeed();
    }

    /**
     * Where the lens's face is with the front shutter open (the beam clip's first frame), from the shipped files: the lens
     * cube's north face centre turned about the shutter's hinge, in blocks at the model's scale. Null when it matches
     * CrystalWarden.LENS_HEIGHT and LENS_FORWARD to a hundredth of a block.
     */
    private static String openLens(com.google.gson.JsonObject geo, com.google.gson.JsonObject anim) {
        com.google.gson.JsonObject lens = null, plate = null;
        for (var b : geo.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")) {
            var o = b.getAsJsonObject();
            if (o.get("name").getAsString().equals("lens")) lens = o;
            if (o.get("name").getAsString().equals("plate_front")) plate = o;
        }
        if (lens == null || plate == null || !"plate_front".equals(lens.get("parent").getAsString())) return "no lens bone on plate_front";
        var cube = lens.getAsJsonArray("cubes").get(0).getAsJsonObject();
        var origin = cube.getAsJsonArray("origin");
        var size = cube.getAsJsonArray("size");
        var pivot = plate.getAsJsonArray("pivot");
        double dy = origin.get(1).getAsDouble() + size.get(1).getAsDouble() / 2 - pivot.get(1).getAsDouble();
        double dz = origin.get(2).getAsDouble() - pivot.get(2).getAsDouble();
        double angle = -Math.toRadians(anim.getAsJsonObject("animations").getAsJsonObject("beam").getAsJsonObject("bones")
                .getAsJsonObject("plate_front").getAsJsonObject("rotation").getAsJsonObject("0.0").getAsJsonArray("vector").get(0).getAsDouble());
        double y = pivot.get(1).getAsDouble() + dy * Math.cos(angle) - dz * Math.sin(angle);
        double z = pivot.get(2).getAsDouble() + dy * Math.sin(angle) + dz * Math.cos(angle);
        double scale = com.panda.promisedend.entity.CrystalWarden.MODEL_SCALE / 16.0;
        double height = y * scale, forward = -z * scale;
        if (Math.abs(height - com.panda.promisedend.entity.CrystalWarden.LENS_HEIGHT) > 0.01
                || Math.abs(forward - com.panda.promisedend.entity.CrystalWarden.LENS_FORWARD) > 0.01)
            return String.format(java.util.Locale.ROOT, "the open lens is %.3f up, %.3f forward; the class says %.3f, %.3f", height, forward,
                    com.panda.promisedend.entity.CrystalWarden.LENS_HEIGHT, com.panda.promisedend.entity.CrystalWarden.LENS_FORWARD);
        return null;
    }

    private static com.google.gson.JsonObject readJson(String resource) {
        try (var in = EndGameTests.class.getResourceAsStream(resource)) {
            if (in == null) return null;
            return com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    private static void rimProgressByDistance(GameTestHelper helper) {
        List<String> bad = new ArrayList<>();
        for (double[] p : new double[][] {{0, 0}, {149, 0}, {0, -460}, {320, 330}}) {
            if (com.panda.promisedend.logic.RimProgress.inRing(p[0], p[1])) bad.add("in the ring at " + p[0] + ", " + p[1]);
        }
        for (double[] p : new double[][] {{150, 0}, {0, -300}, {300, 300}, {-450, 0}}) {
            if (!com.panda.promisedend.logic.RimProgress.inRing(p[0], p[1])) bad.add("not in the ring at " + p[0] + ", " + p[1]);
        }
        var server = helper.getLevel().getServer();
        var adv = server.getAdvancements().get(com.panda.promisedend.logic.RimProgress.ENTER_RIM);
        if (adv == null || !adv.value().criteria().containsKey("in_ring")) bad.add("rim/enter_rim has no in_ring criterion");
        else if (adv.value().requirements().requirements().size() != 1) bad.add("rim/enter_rim's criteria aren't alternatives: " + adv.value().requirements());
        var end = server.getLevel(net.minecraft.world.level.Level.END);
        if (end != null && adv != null) {
            var player = helper.makeMockServerPlayerInLevel();
            player.teleport(new net.minecraft.world.level.portal.TeleportTransition(end, new net.minecraft.world.phys.Vec3(300, 80, 0),
                    net.minecraft.world.phys.Vec3.ZERO, 0, 0, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
            player.tickCount = com.panda.promisedend.logic.RimProgress.CHECK_INTERVAL * 7;
            com.panda.promisedend.logic.RimProgress.tick(player);
            if (!player.getAdvancements().getOrStartProgress(adv).isDone()) bad.add("standing at (300, 0) in the End didn't award rim/enter_rim");
        }
        if (!bad.isEmpty()) helper.fail("rim progress: " + bad);
        helper.succeed();
    }

    /** Every recipe of ours is unlocked by an advancement, or it never shows in the recipe book. */
    private static void everyRecipeUnlocks(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        java.util.Set<ResourceKey<net.minecraft.world.item.crafting.Recipe<?>>> unlocked = new java.util.HashSet<>();
        for (var adv : server.getAdvancements().getAllAdvancements()) unlocked.addAll(adv.value().rewards().recipes());
        List<String> missing = new ArrayList<>();
        int ours = 0;
        for (var recipe : server.getRecipeManager().getRecipes()) {
            if (!recipe.id().identifier().getNamespace().equals(PromisedEnd.MODID)) continue;
            ours++;
            if (!unlocked.contains(recipe.id())) missing.add(recipe.id().identifier().toString());
        }
        if (ours == 0) helper.fail("no recipes of ours loaded");
        if (!missing.isEmpty()) helper.fail("recipes no advancement unlocks: " + missing);
        helper.succeed();
    }

    /**
     * Every Codex goal is a real advancement with a display (the client is only sent advancements that have
     * one), and every act with goals opens on a real advancement.
     */
    private static void codexGoalsAreAdvancements(GameTestHelper helper) {
        var advancements = helper.getLevel().getServer().getAdvancements();
        List<String> bad = new ArrayList<>();
        for (var act : com.panda.promisedend.logic.CodexBook.ACTS) {
            if (act.goals().isEmpty()) continue; // a later milestone writes this act
            if (act.opensWith() != null && advancements.get(act.opensWith()) == null) bad.add(act.key() + " opens with a missing " + act.opensWith());
            for (var goal : act.goals()) {
                var holder = advancements.get(goal.advancement());
                if (holder == null) bad.add("missing " + goal.advancement());
                else if (holder.value().display().isEmpty()) bad.add(goal.advancement() + " has no display, so the client never sees it");
            }
        }
        if (!bad.isEmpty()) helper.fail("Codex goals: " + bad);
        helper.succeed();
    }

    /** Our structures. */
    private static final List<String> STRUCTURES = List.of("rim_outpost");

    /** Every template loads, and each chest in it names a loot table that exists. */
    private static void structuresPlace(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        List<String> bad = new ArrayList<>();
        for (String name : STRUCTURES) {
            var template = server.getStructureTemplateManager().get(PromisedEnd.id(name));
            if (template.isEmpty()) {
                bad.add(name + ": no template");
                continue;
            }
            var chests = template.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                    net.minecraft.world.level.block.Blocks.CHEST);
            if (chests.isEmpty()) bad.add(name + ": no chest");
            for (var chest : chests) {
                String table = chest.nbt() == null ? "" : chest.nbt().getStringOr("LootTable", "");
                var key = ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(table.isEmpty() ? "minecraft:empty" : table));
                if (table.isEmpty() || server.reloadableRegistries().getLootTable(key) == net.minecraft.world.level.storage.loot.LootTable.EMPTY)
                    bad.add(name + ": chest at " + chest.pos() + " has no loot table (" + table + ")");
            }
        }
        if (!bad.isEmpty()) helper.fail("structures: " + bad);
        helper.succeed();
    }

    /** Every structure is in a random-spread set and allowed in the biomes it's meant for. */
    private static void structureSetsResolve(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        var sets = access.lookupOrThrow(Registries.STRUCTURE_SET);
        List<String> bad = new ArrayList<>();
        java.util.Map<String, String> home = java.util.Map.of("rim_outpost", "shattered_rim");
        for (String name : STRUCTURES) {
            var set = sets.get(ResourceKey.create(Registries.STRUCTURE_SET, PromisedEnd.id(name)));
            if (set.isEmpty()) {
                bad.add(name + ": no structure set");
                continue;
            }
            if (!(set.get().value().placement() instanceof net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement))
                bad.add(name + ": not a random spread");
            var structure = set.get().value().structures().getFirst().structure().value();
            if (structure.biomes().stream().noneMatch(b -> b.is(PromisedEnd.id(home.get(name))))) bad.add(name + ": not allowed in " + home.get(name));
        }
        if (!bad.isEmpty()) helper.fail("structure sets: " + bad);
        helper.succeed();
    }

    /**
     * Rim Outposts are easy to find. A survey that generates no chunks: for each placement cell
     * near the origin, take its candidate chunk, keep it if it's in the placement's ring and the real End biome source
     * (NORMAL preset) gives a biome the outpost allows. Over 16 seeds, every seed has at least two within 300 blocks and
     * four within 450.
     */
    private static void rimOutpostNearSpawn(GameTestHelper helper) {
        List<String> bad = outpostSurvey(helper, "terrain on");
        if (!bad.isEmpty()) helper.fail("Rim Outposts too rare: " + bad);
        helper.succeed();
    }

    private static List<String> outpostSurvey(GameTestHelper helper, String label) {
        var access = helper.getLevel().registryAccess();
        ChunkGenerator generator = access.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).value()
                .createWorldDimensions().get(LevelStem.END).orElseThrow().generator();
        var noise = (NoiseBasedChunkGenerator) generator;
        var set = access.lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, PromisedEnd.id("rim_outpost")));
        List<String> bad = new ArrayList<>();
        if (!(set.value().placement() instanceof com.panda.promisedend.worldgen.RingSpreadPlacement placement)) {
            bad.add("the Rim Outpost's placement isn't a ring spread: " + set.value().placement());
            return bad;
        }
        var allowed = set.value().structures().getFirst().structure().value().biomes();
        int spacing = placement.spacing();
        long previous = EndBiomes.seed();
        List<String> report = new ArrayList<>();
        try {
            for (long seed = 1; seed <= 16; seed++) {
                EndBiomes.setSeed(seed);
                RandomState random = RandomState.create(access.lookupOrThrow(Registries.NOISE), seed, noise.generatorSettings().value());
                BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(random);
                int within300 = 0, within450 = 0;
                for (int gx = -3; gx <= 3; gx++) {
                    for (int gz = -3; gz <= 3; gz++) {
                        var chunk = placement.getPotentialStructureChunk(seed, gx * spacing, gz * spacing);
                        if (!placement.inRing(chunk.x(), chunk.z())) continue;
                        int x = chunk.getMinBlockX(), z = chunk.getMinBlockZ();
                        if (!allowed.contains(biomes.getNoiseBiome(x >> 2, 56 >> 2, z >> 2))) continue;
                        double d = Math.hypot(x, z);
                        if (d <= 300) within300++;
                        if (d <= 450) within450++;
                    }
                }
                report.add(seed + ":" + within300 + "/" + within450);
                if (within300 < 2 || within450 < 4) bad.add("seed " + seed + " has " + within300 + " within 300 and " + within450 + " within 450");
            }
        } finally {
            EndBiomes.setSeed(previous);
        }
        PromisedEnd.LOGGER.info("rim_outpost survey, {} (seed: within 300 / within 450): {}", label, report);
        if (!bad.isEmpty()) bad.add("all: " + report);
        return bad;
    }

    /**
     * {@code terrain.enabled=false} (packs with another End overhaul): the mixin leaves the End's biomes alone, and
     * the Rim's content falls back to whatever End biomes fill the same ring, so Voidstone, outposts, the Codex and the
     * anchor stay obtainable: the outposts are still found near spawn, and the debris copy sits on every biome in
     * {@code #promisedend:rim_fallback}, which takes in each biome of {@code #minecraft:is_end} and {@code #c:is_end}.
     * The Mire's pools go on vanilla's midlands ({@code #promisedend:mire_fallback}) and the Spires' formations on its
     * highlands ({@code #promisedend:spire_fallback}): with terrain off each builds in the End (a pool with moss round it
     * and a Lily on it; Resonite with budding Resonite in it), and neither outside the End; with terrain on nothing.
     */
    private static void terrainOffFallsBack(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        List<String> bad = new ArrayList<>();
        var previous = com.panda.promisedend.EndConfig.current();
        try {
            com.panda.promisedend.EndConfig.use(new com.panda.promisedend.EndConfig.Source() {
                @Override public boolean terrainEnabled() { return false; }
                @Override public int edgeDistance() { return previous.edgeDistance(); }
                @Override public int anchorTriggerDepth() { return previous.anchorTriggerDepth(); }
                @Override public boolean dragonPhase3() { return previous.dragonPhase3(); }
                @Override public boolean dragonWrath() { return previous.dragonWrath(); }
                @Override public int maxWrath() { return previous.maxWrath(); }
            });
            ChunkGenerator generator = access.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).value()
                    .createWorldDimensions().get(LevelStem.END).orElseThrow().generator();
            var noise = (NoiseBasedChunkGenerator) generator;
            RandomState random = RandomState.create(access.lookupOrThrow(Registries.NOISE), 424242L, noise.generatorSettings().value());
            var biome = generator.getBiomeSource().createUncachedResolver(random).getNoiseBiome(300 >> 2, 64 >> 2, 0);
            if (!biome.is(net.minecraft.world.level.biome.Biomes.THE_END)) bad.add("terrain off, yet the biome at x=300 is " + biome.unwrapKey().orElse(null));
            bad.addAll(outpostSurvey(helper, "terrain off"));
            String pools = fallbackPools(helper);
            if (pools != null) bad.add(pools);
            String spires = fallbackSpires(helper);
            if (spires != null) bad.add(spires);
            // outside the End nothing, even with terrain off: the copies are for the End's own biomes
            BlockPos away = helper.absolutePos(new BlockPos(1, 2, 1)).offset(0, 40, 0);
            var test = helper.getLevel();
            floor(test, away, 14, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            floor(test, away.below(), 14, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            for (String name : List.of("mire_pool_fallback", "resonite_spire_fallback")) {
                if (grove(test, name).place(test, test.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(700), away))
                    bad.add(name + " built outside the End");
            }
            clear(test, away);
        } finally {
            com.panda.promisedend.EndConfig.use(previous);
        }
        var placed = access.lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("rim_debris_fallback")));
        var biomes = access.lookupOrThrow(Registries.BIOME);
        var fallbackTag = net.minecraft.tags.TagKey.create(Registries.BIOME, PromisedEnd.id("rim_fallback"));
        var outpostTag = net.minecraft.tags.TagKey.create(Registries.BIOME, PromisedEnd.id("has_structure/rim_outpost"));
        for (var key : List.of(net.minecraft.world.level.biome.Biomes.THE_END, net.minecraft.world.level.biome.Biomes.END_HIGHLANDS,
                net.minecraft.world.level.biome.Biomes.SMALL_END_ISLANDS)) {
            var b = biomes.getOrThrow(key);
            if (!b.is(fallbackTag)) bad.add(key.identifier() + " isn't in #promisedend:rim_fallback");
            if (!b.is(outpostTag)) bad.add(key.identifier() + " can't hold a Rim Outpost");
            if (b.value().getGenerationSettings().features().stream().noneMatch(step -> step.contains(placed))) {
                bad.add("rim_debris_fallback isn't among " + key.identifier() + "'s features (the loader's biome modifier didn't apply)");
            }
        }
        // every End biome by either tag is in the fallback, so another mod's ring biomes are too
        for (var tag : List.of(BiomeTags.IS_END, net.minecraft.tags.TagKey.create(Registries.BIOME, Identifier.parse("c:is_end")))) {
            biomes.getTagOrEmpty(tag).forEach(b -> {
                if (!b.is(fallbackTag)) bad.add(b.unwrapKey().map(k -> k.identifier().toString()).orElse("?") + " is in #" + tag.location() + " but not in the fallback");
            });
        }
        // the Spires' formations on vanilla's highlands, through the same loader hook; with terrain on nothing
        var spires = access.lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("resonite_spire_fallback")));
        var highlands = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.END_HIGHLANDS);
        if (!highlands.is(net.minecraft.tags.TagKey.create(Registries.BIOME, PromisedEnd.id("spire_fallback")))) bad.add("end_highlands isn't in #promisedend:spire_fallback");
        if (highlands.value().getGenerationSettings().features().stream().noneMatch(step -> step.contains(spires)))
            bad.add("resonite_spire_fallback isn't among end_highlands' features (the loader's biome modifier didn't apply)");
        if (com.panda.promisedend.EndConfig.terrainEnabled()) {
            var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
            BlockPos at = new BlockPos(9200, end.getMaxY() - 60, 9000);
            floor(end, at, 14, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            if (spires.value().feature().value().place(end, end.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(990), at))
                bad.add("the terrain-off spires built with terrain on");
            clear(end, at);
        }
        // the Mire's pools on vanilla's midlands, through the same loader hook; with terrain on the copy builds nothing
        var pools = access.lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("mire_pool_fallback")));
        var midlands = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.END_MIDLANDS);
        if (!midlands.is(net.minecraft.tags.TagKey.create(Registries.BIOME, PromisedEnd.id("mire_fallback")))) bad.add("end_midlands isn't in #promisedend:mire_fallback");
        if (midlands.value().getGenerationSettings().features().stream().noneMatch(step -> step.contains(pools)))
            bad.add("mire_pool_fallback isn't among end_midlands' features (the loader's biome modifier didn't apply)");
        if (com.panda.promisedend.EndConfig.terrainEnabled()) {
            var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
            // the same floor, two deep, and seed that make a pool with terrain off
            BlockPos at = new BlockPos(9064, end.getMaxY() - 60, 9000);
            floor(end, at, 12, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            floor(end, at.below(), 12, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            if (pools.value().feature().value().place(end, end.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(700), at))
                bad.add("the terrain-off pools built with terrain on");
            clear(end, at);
        }
        if (!bad.isEmpty()) helper.fail("terrain-off fallback: " + bad);
        helper.succeed();
    }

    /**
     * With terrain off, the Mire's fallback pool in the End: on a floor of end stone two deep, high in the End's sky, a
     * pool of at least MirePoolFeature.MIN_CELLS void-water with Lumen Moss round it (the only moss there is then).
     * The same floor and seed make a pool in mire_pools_walled. Null when it holds.
     */
    private static String fallbackPools(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) return "no End level";
        var pools = end.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                .getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("mire_pool_fallback"))).value().feature().value();
        BlockPos at = new BlockPos(9000, end.getMaxY() - 60, 9000);
        var endStone = net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState();
        floor(end, at, 12, endStone);
        floor(end, at.below(), 12, endStone);
        boolean placed = pools.place(end, end.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(700), at);
        int water = 0, moss = 0, lilies = 0;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, -3, -16), at.offset(16, 3, 16))) {
            var state = end.getBlockState(p);
            if (state.is(ModBlocks.VOID_WATER.get())) water++;
            else if (state.is(ModBlocks.LUMEN_MOSS.get())) moss++;
            else if (state.is(ModBlocks.MIRE_LILY.get())) lilies++;
        }
        clear(end, at);
        PromisedEnd.LOGGER.info("[terrain_off_falls_back] the Mire's fallback pool: {} void-water, {} Lumen Moss, {} Mire Lilies", water, moss, lilies);
        if (!placed || water < com.panda.promisedend.worldgen.MirePoolFeature.MIN_CELLS || moss == 0 || lilies == 0)
            return "the Mire's fallback pool with terrain off: placed " + placed + ", " + water + " void-water, " + moss + " Lumen Moss, " + lilies + " Mire Lilies";
        return null;
    }

    /**
     * With terrain off, the Spires' fallback formation in the End: on open end stone high in the End's sky, a formation
     * of Resonite with budding Resonite in it (the only Resonite there is then). Null when it holds.
     */
    private static String fallbackSpires(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) return "no End level";
        var spires = end.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                .getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("resonite_spire_fallback"))).value().feature().value();
        BlockPos at = new BlockPos(9300, end.getMaxY() - 60, 9000);
        floor(end, at, 14, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
        boolean placed = spires.place(end, end.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(990), at);
        int crystal = 0, budding = 0;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, 0, -16), at.offset(16, 40, 16))) {
            var state = end.getBlockState(p);
            if (state.is(ModBlocks.RESONITE_BLOCK.get())) crystal++;
            else if (state.is(ModBlocks.BUDDING_RESONITE.get())) budding++;
        }
        clear(end, at);
        PromisedEnd.LOGGER.info("[terrain_off_falls_back] the Spires' fallback formation: {} Resonite Block, {} budding", crystal, budding);
        if (!placed || crystal < 50 || budding == 0) return "the Spires' fallback formation with terrain off: placed " + placed + ", " + crystal + " Resonite, " + budding + " budding";
        return null;
    }

    /**
     * The Rim's placed feature builds an island of Voidstone and end stone in mid-air: mostly Voidstone, a
     * tapering underside, 6–18 blocks across. The placement filters (rarity, height, biome) are bypassed: this checks
     * the shape and materials, not where islands appear. The feature keeps to the 150–450 ring itself, so
     * the islands are built 300 blocks out, high in the test level's sky.
     */
    private static void rimIslandsGenerate(GameTestHelper helper) {
        var level = helper.getLevel();
        var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                .getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("rim_debris")));
        var feature = placed.value().feature().value();
        int y = level.getMaxY() - 40;
        int voidstone = 0, endStone = 0, below = 0;
        List<Integer> widths = new ArrayList<>();
        List<String> bad = new ArrayList<>();
        for (int n = 0; n < 12; n++) {
            double a = Math.toRadians(n * 30);
            BlockPos centre = new BlockPos((int) Math.round(300 * Math.cos(a)), y, (int) Math.round(300 * Math.sin(a)));
            if (!feature.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(77 + n), centre)) bad.add("nothing placed at " + centre);
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-16, -16, -16), centre.offset(16, 6, 16))) {
                var state = level.getBlockState(pos);
                if (state.is(ModBlocks.VOIDSTONE_SET.base().get())) voidstone++;
                else if (state.is(net.minecraft.world.level.block.Blocks.END_STONE)) endStone++;
                if (!state.isAir() && pos.getY() < centre.getY() - 2) below++;
            }
            // the lens across: the unbroken run of blocks through the centre along x
            int east = 0, west = 0;
            while (east < 16 && !level.getBlockState(centre.offset(east + 1, 0, 0)).isAir()) east++;
            while (west < 16 && !level.getBlockState(centre.offset(-west - 1, 0, 0)).isAir()) west++;
            widths.add(east + west + 1);
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-16, -16, -16), centre.offset(16, 6, 16))) level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        if (voidstone == 0) bad.add("no Voidstone in the islands");
        double share = voidstone / (double) Math.max(1, voidstone + endStone);
        if (share < 0.62 || share > 0.80) bad.add("islands should be about 70 % Voidstone: " + voidstone + " voidstone, " + endStone + " end stone");
        if (below == 0) bad.add("the islands have no tapering underside");
        if (widths.stream().anyMatch(w -> w < 6 || w > 18)) bad.add("islands outside 6–18 across: " + widths);
        PromisedEnd.LOGGER.info("rim islands: {} voidstone / {} end stone, widths {}", voidstone, endStone, widths);
        // outside the ring the feature builds nothing
        BlockPos outside = new BlockPos(700, y, 0);
        if (feature.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(5), outside)) bad.add("placed outside the ring at " + outside);
        // the terrain-off copy: never in another dimension that borrows an End biome, never on our own biomes (the Rim's
        // own copy builds there), and on any other End biome in the End's ring
        var fallback = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                .getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("rim_debris_fallback"))).value().feature().value();
        var server = level.getServer();
        BlockPos borrowed = new BlockPos(0, y, 300);
        fillBiome(level, borrowed, "minecraft:the_end");
        if (!level.getBiome(borrowed).is(net.minecraft.world.level.biome.Biomes.THE_END)) bad.add("fillbiome didn't set the_end at " + borrowed);
        if (fallback.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(6), borrowed)) bad.add("the fallback built on the_end outside the End at " + borrowed);
        var end = server.getLevel(net.minecraft.world.level.Level.END);
        if (end == null) bad.add("no End level on the test server");
        else {
            int ey = 120;
            BlockPos ours = new BlockPos(0, ey, -300), theirs = new BlockPos(-300, ey, 0);
            fillBiome(end, ours, "promisedend:shattered_rim");
            fillBiome(end, theirs, "minecraft:end_highlands");
            if (!end.getBiome(ours).is(ResourceKey.create(Registries.BIOME, PromisedEnd.id("shattered_rim")))) bad.add("fillbiome didn't set the Shattered Rim at " + ours);
            if (!end.getBiome(theirs).is(net.minecraft.world.level.biome.Biomes.END_HIGHLANDS)) bad.add("fillbiome didn't set end_highlands at " + theirs);
            if (fallback.place(end, end.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(7), ours)) bad.add("the fallback built on the Shattered Rim at " + ours);
            if (!fallback.place(end, end.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(8), theirs)) bad.add("the fallback built nothing on end_highlands in the End's ring at " + theirs);
            for (BlockPos pos : BlockPos.betweenClosed(theirs.offset(-16, -16, -16), theirs.offset(16, 6, 16))) end.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        if (!bad.isEmpty()) helper.fail("rim islands: " + bad);
        helper.succeed();
    }
}

package com.panda.promisedend.gametest;

import com.panda.promisedend.block.DriftBlossomBlock;
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
        FUNCTIONS.register("constructs_stats_and_drops", () -> EndGameTests::constructsStatsAndDrops);
        FUNCTIONS.register("construct_recipes", () -> EndGameTests::constructRecipes);
        FUNCTIONS.register("constructs_edges_walls_and_swarms", () -> EndGameTests::constructsEdgesWallsAndSwarms);
        FUNCTIONS.register("construct_spawns_keep_to_their_lands", () -> EndGameTests::constructSpawnsKeepToTheirLands);
        FUNCTIONS.register("arrival_by_coordinates", () -> EndGameTests::arrivalByCoordinates);
        FUNCTIONS.register("ward_sets_back", () -> EndGameTests::wardSetsBack);
        FUNCTIONS.register("seals_drop_the_ward", () -> EndGameTests::sealsDropTheWard);
        FUNCTIONS.register("ward_bounces_pearls", () -> EndGameTests::wardBouncesPearls);
        FUNCTIONS.register("pearl_shows_the_way", () -> EndGameTests::pearlShowsTheWay);
        FUNCTIONS.register("arrival_off_is_vanilla", () -> EndGameTests::arrivalOffIsVanilla);
        FUNCTIONS.register("drift_islet_carries", () -> EndGameTests::driftIsletCarries);
        FUNCTIONS.register("drift_islet_blocked_turns", () -> EndGameTests::driftIsletBlockedTurns);
        FUNCTIONS.register("drift_islet_saves", () -> EndGameTests::driftIsletSaves);
        FUNCTIONS.register("drift_islet_headroom_turns", () -> EndGameTests::driftIsletHeadroomTurns);
        FUNCTIONS.register("drift_islet_keeps_rider_aloft", () -> EndGameTests::driftIsletKeepsRiderAloft);
        FUNCTIONS.register("drift_islet_crossing", () -> EndGameTests::driftIsletCrossing);
        FUNCTIONS.register("ward_binds_by_guardian", () -> EndGameTests::wardBindsByGuardian);
        FUNCTIONS.register("drift_islet_lifts_the_sunk", () -> EndGameTests::driftIsletLiftsTheSunk);
        FUNCTIONS.register("return_gate_sends_home", () -> EndGameTests::returnGateSendsHome);
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
        FUNCTIONS.register("drift_blossom_rules", () -> EndGameTests::driftBlossomRules);
        FUNCTIONS.register("updraft_vent_recipe", () -> EndGameTests::updraftVentRecipe);
        FUNCTIONS.register("rift_scar_carves", () -> EndGameTests::riftScarCarves);
        FUNCTIONS.register("voidscar_islands", () -> EndGameTests::voidscarIslands);
        FUNCTIONS.register("purpur_reef_grows", () -> EndGameTests::purpurReefGrows);
        FUNCTIONS.register("ancient_purpur_set", () -> EndGameTests::ancientPurpurSet);
        FUNCTIONS.register("hollow_cavern_carves", () -> EndGameTests::hollowCavernCarves);
        FUNCTIONS.register("echo_amber_flares", () -> EndGameTests::echoAmberFlares);
        FUNCTIONS.register("cave_dark_held_off", () -> EndGameTests::caveDarkHeldOff);
        FUNCTIONS.register("void_grass_rules", () -> EndGameTests::voidGrassRules);
        FUNCTIONS.register("end_flowers_rules", () -> EndGameTests::endFlowersRules);
        FUNCTIONS.register("wilds_plants_rules", () -> EndGameTests::wildsPlantsRules);
        FUNCTIONS.register("wilds_features_grow", () -> EndGameTests::wildsFeaturesGrow);
        FUNCTIONS.register("canopy_keeps_out", () -> EndGameTests::canopyKeepsOut);
        FUNCTIONS.register("crumbling_edges_line_islands", () -> EndGameTests::crumblingEdgesLineIslands);
        FUNCTIONS.register("crumbling_voidstone_falls", () -> EndGameTests::crumblingVoidstoneFalls);
        FUNCTIONS.register("voidsteel_ore_drops", () -> EndGameTests::voidsteelOreDrops);
        FUNCTIONS.register("mire_pools_walled", () -> EndGameTests::mirePoolsWalled);
        FUNCTIONS.register("resonite_spires_grow", () -> EndGameTests::resoniteSpiresGrow);
        FUNCTIONS.register("resonite_cluster_chimes", () -> EndGameTests::resoniteClusterChimes);
        FUNCTIONS.register("budding_resonite_grows", () -> EndGameTests::buddingResoniteGrows);
        FUNCTIONS.register("resonite_drops", () -> EndGameTests::resoniteDrops);
        FUNCTIONS.register("gateway_exits_beside_spires", () -> EndGameTests::gatewayExitsBesideSpires);
        FUNCTIONS.register("updraft_lifts_entity", () -> EndGameTests::updraftLiftsEntity);
        FUNCTIONS.register("updraft_vent_on_edges", () -> EndGameTests::updraftVentOnEdges);
        FUNCTIONS.register("updraft_keeps_player_aloft", () -> EndGameTests::updraftKeepsPlayerAloft);
        FUNCTIONS.register("updraft_column_bounds", () -> EndGameTests::updraftColumnBounds);
        FUNCTIONS.register("updraft_landing_safe", () -> EndGameTests::updraftLandingSafe);
        FUNCTIONS.register("current_carries_entity", () -> EndGameTests::currentCarriesEntity);
        FUNCTIONS.register("chorus_weaver_lashes", () -> EndGameTests::chorusWeaverLashes);
        FUNCTIONS.register("lumen_wisp_lights_the_way", () -> EndGameTests::lumenWispLightsTheWay);
        FUNCTIONS.register("rift_stalker_blinks_and_freezes", () -> EndGameTests::riftStalkerBlinksAndFreezes);
        FUNCTIONS.register("purpur_sentry_shields_and_lunges", () -> EndGameTests::purpurSentryShieldsAndLunges);
        FUNCTIONS.register("lumen_lamp_lights", () -> EndGameTests::lumenLampLights);
        FUNCTIONS.register("biome_visits_award", () -> EndGameTests::biomeVisitsAward);
        FUNCTIONS.register("sentinel_closed_is_invulnerable", () -> SentinelGameTests::closedIsInvulnerable);
        FUNCTIONS.register("sentinel_opens_after_three_returns", () -> SentinelGameTests::opensAfterThreeReturns);
        FUNCTIONS.register("sentinel_phases_call_sentries", () -> SentinelGameTests::phasesCallSentries);
        FUNCTIONS.register("sentinel_volleys_never_crack_itself", () -> SentinelGameTests::volleysNeverCrackItself);
        FUNCTIONS.register("sentinel_death_breaks_seal", () -> SentinelGameTests::deathBreaksSeal);
        FUNCTIONS.register("sentinel_wakes_for_survival", () -> SentinelGameTests::wakesForSurvival);
        FUNCTIONS.register("foundry_places_sentinel", () -> SentinelGameTests::foundryPlacesSentinel);
        FUNCTIONS.register("foundries_within_reach", () -> SentinelGameTests::foundriesWithinReach);
        FUNCTIONS.register("sentinel_opens_onto_its_rest", () -> SentinelGameTests::opensOntoItsRest);
        FUNCTIONS.register("waystation_chart_never_blank", () -> SentinelGameTests::waystationChartNeverBlank);
        FUNCTIONS.register("shrine_grants_buff", () -> ShrineGameTests::shrineGrantsBuff);
        FUNCTIONS.register("boons_do_their_work", () -> ShrineGameTests::boonsDoTheirWork);
        FUNCTIONS.register("gliding_grace_halves_wear", () -> ShrineGameTests::glidingGraceHalvesWear);
        FUNCTIONS.register("void_sight_shows_vents", () -> ShrineGameTests::voidSightShowsVents);
        FUNCTIONS.register("altar_keeps_and_refuses", () -> ShrineGameTests::altarKeepsAndRefuses);
        FUNCTIONS.register("star_chart_locates", () -> ChartGameTests::starChartLocates);
        FUNCTIONS.register("desk_gives_one_chart", () -> ChartGameTests::deskGivesOneChart);
    }

    /** Tests that need longer than the default 100 ticks (a dragon's dying takes 200). */
    private static final java.util.Map<String, Integer> TIMEOUTS = java.util.Map.ofEntries(
            java.util.Map.entry("crystal_warden_beam_cap_and_rest", 220), java.util.Map.entry("wrath_increments_on_kill", 260),
            java.util.Map.entry("dragon_drops_scales", 240), java.util.Map.entry("void_water_slows", 160),
            java.util.Map.entry("dragon_phase3_triggers", 460), java.util.Map.entry("updraft_lifts_entity", 360),
            java.util.Map.entry("echo_amber_flares", 180), java.util.Map.entry("cave_dark_held_off", 200), java.util.Map.entry("drift_islet_blocked_turns", 160), java.util.Map.entry("drift_islet_headroom_turns", 160), java.util.Map.entry("current_carries_entity", 560),
            java.util.Map.entry("chorus_weaver_lashes", 380), java.util.Map.entry("lumen_wisp_lights_the_way", 460),
            java.util.Map.entry("rift_stalker_blinks_and_freezes", 300), java.util.Map.entry("constructs_edges_walls_and_swarms", 200), java.util.Map.entry("purpur_sentry_shields_and_lunges", 260),
            java.util.Map.entry("sentinel_opens_after_three_returns", 260), java.util.Map.entry("sentinel_wakes_for_survival", 100),
            java.util.Map.entry("sentinel_volleys_never_crack_itself", 260), java.util.Map.entry("sentinel_opens_onto_its_rest", 260));
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
            "crystal_warden_ignores_the_dragon", "crystal_warden_shatters_and_drops",
            // counts constructs within 32 blocks, which reaches into the arenas beside it
            "construct_spawns_keep_to_their_lands",
            // waits on the light engine, which in a busy batch never lit its cluster's light in 175 ticks (P100)
            "cave_dark_held_off",
            // a Sentinel targets and wakes for any survival player in 48 or 20, and the phases count Sentries in 16
            "sentinel_opens_after_three_returns", "sentinel_phases_call_sentries", "sentinel_wakes_for_survival", "sentinel_death_breaks_seal",
            "sentinel_volleys_never_crack_itself", "sentinel_opens_onto_its_rest");

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
                // the Rim reaches out to the outer lands, so the way to the heart is never a long gap of void
                expect.accept(new double[] {700 * Math.cos(r), 700 * Math.sin(r)}, "promisedend:shattered_rim");
                expect.accept(new double[] {1000 * Math.cos(r), 1000 * Math.sin(r)}, "promisedend:shattered_rim");
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
     * keep 20-30 %, and each of ours 3-30 % (the small islands are 45 % of the outer End, most of it open void, and the
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
            else if (share < 0.02 || share > 0.30) wrong.add(String.format(java.util.Locale.ROOT, "%s %.1f %%", e.getKey().id, 100 * share));
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
     * A Drift Blossom stands on end stone only. By hand it drops one Drift Silk (one to four with Fortune III), with
     * shears or Silk Touch the blossom. Bone meal fills the open end stone within two blocks one blossom at a time, never
     * three off, and stops when it's full.
     */
    private static void driftBlossomRules(GameTestHelper helper) {
        var level = helper.getLevel();
        var blossom = ModBlocks.DRIFT_BLOSSOM.get();
        List<String> bad = new ArrayList<>();
        var grounds = java.util.Map.of(net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), true,
                ModBlocks.CHORUS_MOSS.get().defaultBlockState(), false,
                ModBlocks.VOID_WATER.get().defaultBlockState(), false,
                net.minecraft.world.level.block.Blocks.GRASS_BLOCK.defaultBlockState(), false);
        int x = 1;
        for (var e : grounds.entrySet()) {
            BlockPos at = helper.absolutePos(new BlockPos(x, 2, 1));
            level.setBlock(at, e.getKey(), 2);
            if (blossom.defaultBlockState().canSurvive(level, at.above()) != e.getValue()) bad.add("a blossom on " + e.getKey() + (e.getValue() ? " won't hold" : " holds"));
            level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            x += 2;
        }
        var table = level.getServer().reloadableRegistries().getLootTable(blossom.getLootTable().orElseThrow());
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var silk = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        silk.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        var fortune = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        fortune.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
        // tool, the item it gives, the fewest and the most in one roll
        record Case(String name, ItemStack tool, net.minecraft.world.item.Item item, int min, int max) {}
        int fortuneMost = 0;
        for (var c : List.of(new Case("a hand", ItemStack.EMPTY, ModItems.DRIFT_SILK.get(), 1, 1),
                new Case("shears", new ItemStack(net.minecraft.world.item.Items.SHEARS), blossom.asItem(), 1, 1),
                new Case("Silk Touch", silk, blossom.asItem(), 1, 1),
                new Case("Fortune III", fortune, ModItems.DRIFT_SILK.get(), 1, 4))) {
            for (int i = 0; i < 100; i++) {
                var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, c.tool())
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, blossom.defaultBlockState())
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
                var drops = table.getRandomItems(params, net.minecraft.util.Mth.murmurHash3Mixer(7000 + i));
                int n = drops.stream().filter(st -> st.is(c.item())).mapToInt(ItemStack::getCount).sum();
                if (drops.stream().anyMatch(st -> !st.is(c.item())) || n < c.min() || n > c.max()) {
                    bad.add("with " + c.name() + " roll " + i + " gave " + drops);
                    break;
                }
                if (c.tool() == fortune) fortuneMost = Math.max(fortuneMost, n);
            }
        }
        if (fortuneMost < 3) bad.add("Fortune III never gave more than " + fortuneMost + " Drift Silk in 100");
        // a blossom on a floor of end stone 7 across, one cell of it chorus moss: bone meal fills the 23 open cells
        // within two, then stops; the ring three off stays bare
        BlockPos centre = helper.absolutePos(new BlockPos(5, 2, 6));
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-3, 0, -3), centre.offset(3, 0, 3))) {
            level.setBlock(p, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 2);
            level.setBlock(p.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        BlockPos moss = centre.offset(1, 0, 0);
        level.setBlock(moss, ModBlocks.CHORUS_MOSS.get().defaultBlockState(), 2);
        level.setBlock(centre.above(), blossom.defaultBlockState(), 2);
        for (int n = 1; n <= 25; n++) {
            boolean grew = net.minecraft.world.item.BoneMealItem.growCrop(new ItemStack(net.minecraft.world.item.Items.BONE_MEAL), level, centre.above());
            int near = 0, three = 0;
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-3, 1, -3), centre.offset(3, 1, 3))) {
                if (!level.getBlockState(p).is(blossom) || p.equals(centre.above())) continue;
                int ring = Math.max(Math.abs(p.getX() - centre.getX()), Math.abs(p.getZ() - centre.getZ()));
                if (ring <= DriftBlossomBlock.SPREAD) near++;
                else three++;
            }
            int want = Math.min(n, 23);
            if (near != want) bad.add("after " + n + " bone meal " + near + " blossoms near, not " + want);
            if (three != 0) bad.add("after " + n + " bone meal a blossom three off");
            if (grew != (n <= 23)) bad.add("bone meal " + n + (grew ? " took" : " didn't take"));
        }
        if (level.getBlockState(moss.above()).is(blossom)) bad.add("a blossom on the chorus moss");
        if (!bad.isEmpty()) helper.fail("Drift Blossom: " + bad);
        helper.succeed();
    }

    /** Six Polished Voidstone, two Drift Silk and a Crystal Fragment craft one Updraft Vent. */
    private static void updraftVentRecipe(GameTestHelper helper) {
        var level = helper.getLevel();
        var v = new ItemStack(ModBlocks.POLISHED_VOIDSTONE_SET.base().get());
        var silk = new ItemStack(ModItems.DRIFT_SILK.get());
        var crystal = new ItemStack(ModItems.CRYSTAL_FRAGMENT.get());
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3,
                List.of(v.copy(), silk.copy(), v.copy(), v.copy(), crystal, v.copy(), v.copy(), silk.copy(), v.copy()));
        var out = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                .map(h -> h.value().assemble(input)).orElse(ItemStack.EMPTY);
        if (!out.is(ModBlocks.UPDRAFT_VENT.get().asItem()) || out.getCount() != 1) helper.fail("the vent recipe makes " + out);
        helper.succeed();
    }

    /**
     * The four constructs against their numbers: each one's health, armour and blow; none of them may attack
     * another construct; and each, killed by a player, shatters (gone the tick after) and drops its material.
     */
    private static void constructsStatsAndDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        List<String> bad = new ArrayList<>();
        record Expect(net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type, double health, double armour,
                double attack, net.minecraft.world.item.Item drop) {}
        List<Expect> all = List.of(
                new Expect(com.panda.promisedend.registry.ModEntities.PURPUR_SENTRY.get(), 40, 8, com.panda.promisedend.entity.PurpurSentry.DAMAGE, ModItems.ANCIENT_PURPUR_SCRAP.get()),
                new Expect(com.panda.promisedend.registry.ModEntities.RIFT_STALKER.get(), 28, 2, com.panda.promisedend.entity.RiftStalker.DAMAGE, ModItems.RIFT_DUST.get()),
                new Expect(com.panda.promisedend.registry.ModEntities.CHORUS_WEAVER.get(), 34, 4, -1, ModItems.CHORUS_RESIN.get()),
                new Expect(com.panda.promisedend.registry.ModEntities.LUMEN_WISP.get(), 6, 0, -1, ModItems.LUMEN.get()));
        List<net.minecraft.world.entity.Mob> mobs = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            Expect e = all.get(i);
            var mob = helper.spawn(e.type(), new BlockPos(1 + i * 2, 2, 4));
            mobs.add(mob);
            String name = net.minecraft.world.entity.EntityType.getKey(e.type()).getPath();
            double health = mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            double armour = mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
            if (health != e.health() || armour != e.armour()) bad.add(name + " is " + health + " / " + armour + ", the table says " + e.health() + " / " + e.armour());
            if (e.attack() >= 0 && mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) != e.attack())
                bad.add(name + " hits for " + mob.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        }
        for (var a : mobs) {
            for (var b : mobs) {
                if (a != b && a.canAttack(b)) bad.add(net.minecraft.world.entity.EntityType.getKey(a.getType()).getPath() + " may attack the " + net.minecraft.world.entity.EntityType.getKey(b.getType()).getPath());
            }
            if (!a.canAttack(player) && !(a instanceof com.panda.promisedend.entity.LumenWisp)) bad.add(net.minecraft.world.entity.EntityType.getKey(a.getType()).getPath() + " may not attack a player");
        }
        helper.runAfterDelay(2, () -> {
            for (var mob : mobs) mob.hurtServer(level, level.damageSources().playerAttack(player), 1000.0F);
            helper.runAfterDelay(3, () -> {
                for (int i = 0; i < all.size(); i++) {
                    var mob = mobs.get(i);
                    Expect e = all.get(i);
                    String name = net.minecraft.world.entity.EntityType.getKey(e.type()).getPath();
                    if (!mob.isRemoved()) bad.add(name + " didn't shatter: still there 3 ticks after it died");
                    var box = new net.minecraft.world.phys.AABB(mob.position(), mob.position()).inflate(2.5);
                    boolean dropped = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box, it -> it.getItem().is(e.drop())).size() > 0;
                    if (!dropped) bad.add(name + " dropped no " + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(e.drop()));
                }
                PromisedEnd.LOGGER.info("[constructs_stats_and_drops] {} constructs checked: {}", all.size(), bad.isEmpty() ? "all as the table" : bad);
                if (!bad.isEmpty()) helper.fail("constructs: " + bad);
                else helper.succeed();
            });
        });
    }

    /**
     * The constructs' guards against the void and against walls, in one arena. A Purpur Sentry lunging east at an iron
     * golem across a gap 2 wide and 4 deep pulls up at its edge, on its own side and on its feet. A Chorus Weaver stays
     * asleep with a survival player 3 off behind a wall, and wakes once the wall is gone; rooted again, it stands square
     * to the grid in the middle of its block. Three Lumen Wisps near one player holding Lumen: two follow, the third
     * doesn't.
     */
    private static void constructsEdgesWallsAndSwarms(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 5));
        List<String> bad = new ArrayList<>();
        // the gap: x 4-5, z 3-7, from the floor down 4
        for (int x = 4; x <= 5; x++) {
            for (int z = 3; z <= 7; z++) {
                for (int y = 0; y >= -3; y--) level.setBlock(helper.absolutePos(new BlockPos(x, y, z)), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            }
        }
        var sentry = helper.spawn(com.panda.promisedend.registry.ModEntities.PURPUR_SENTRY.get(), new BlockPos(1, 1, 5));
        var golem = helper.spawn(net.minecraft.world.entity.EntityTypes.IRON_GOLEM, new BlockPos(8, 1, 5));
        golem.setNoAi(true);
        double gapX = helper.absolutePos(new BlockPos(4, 0, 0)).getX();
        // the wall: x 2, z 8-10, three high, between the weaver at (1, 9) and the player at (4, 9)
        var weaver = helper.spawn(com.panda.promisedend.registry.ModEntities.CHORUS_WEAVER.get(), new BlockPos(1, 1, 9));
        for (int z = 8; z <= 10; z++) {
            for (int y = 1; y <= 3; y++) level.setBlock(helper.absolutePos(new BlockPos(2, y, z)), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
        }
        var walker = helper.makeMockServerPlayerInLevel();
        walker.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.world.phys.Vec3 wv = weaver.position();
        walker.snapTo(wv.x + 3, wv.y, wv.z, 90, 0);
        // the wisps' player, far from the weaver
        var holder = helper.makeMockServerPlayerInLevel();
        holder.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        BlockPos h = helper.absolutePos(new BlockPos(8, 1, 1));
        holder.snapTo(h.getX() + 0.5, h.getY(), h.getZ() + 0.5, 0, 0);
        holder.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.LUMEN.get()));
        List<com.panda.promisedend.entity.LumenWisp> wisps = new ArrayList<>();
        for (int x = 7; x <= 9; x++) wisps.add(helper.spawn(com.panda.promisedend.registry.ModEntities.LUMEN_WISP.get(), new BlockPos(x, 3, 2)));
        int[] began = {-1}, woke = {-1}, lungeAt = {-1}, recoverAt = {-1}, wispsDone = {0};
        for (int t = 1; t <= 190; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (began[0] < 0) {
                    if (level.isPositionEntityTicking(at)) {
                        began[0] = tick;
                        sentry.setTarget(golem);
                        sentry.raiseShield();
                    } else if (tick == 60) helper.fail("the arena's chunk wasn't entity-ticking by tick 60");
                    return;
                }
                int in = tick - began[0];
                if (lungeAt[0] < 0) {
                    sentry.setTarget(golem);
                    if (sentry.state() == com.panda.promisedend.entity.PurpurSentry.State.LUNGE) lungeAt[0] = in;
                }
                if (lungeAt[0] > 0 && recoverAt[0] < 0 && sentry.state() == com.panda.promisedend.entity.PurpurSentry.State.RECOVER) recoverAt[0] = in;
                if (recoverAt[0] > 0 && in == recoverAt[0] + 3) {
                    double front = sentry.getX() + sentry.getBbWidth() / 2;
                    PromisedEnd.LOGGER.info("[constructs_edges_walls_and_swarms] the sentry pulled up {} short of the gap, at y {} (the floor's top {})",
                            String.format("%.2f", gapX - front), String.format("%.2f", sentry.getY()), at.getY());
                    if (front > gapX + 0.05 || sentry.getY() < at.getY() - 0.1) bad.add("the sentry's lunge went over the edge: front " + front + ", the gap at " + gapX + ", y " + sentry.getY());
                }
                if (in <= 20 && !weaver.disguised()) {
                    bad.add("the weaver woke for a player behind a wall (" + in + " ticks in)");
                    woke[0] = in;
                }
                if (in == 21) {
                    for (int z = 8; z <= 10; z++) {
                        for (int y = 1; y <= 3; y++) level.setBlock(helper.absolutePos(new BlockPos(2, y, z)), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                    }
                }
                if (in > 21 && woke[0] < 0 && !weaver.disguised()) {
                    woke[0] = in;
                    // rooted again from a skewed stand: square, in the middle of its block
                    weaver.snapTo(weaver.getX() + 0.31, weaver.getY(), weaver.getZ() - 0.22, 37.0F, 0);
                    weaver.disguise();
                    double fx = weaver.getX() - Math.floor(weaver.getX()), fz = weaver.getZ() - Math.floor(weaver.getZ());
                    float yaw = net.minecraft.util.Mth.wrapDegrees(weaver.getYRot());
                    PromisedEnd.LOGGER.info("[constructs_edges_walls_and_swarms] the weaver woke {} ticks after the wall went; rooted again at {} {} in its block, facing {}",
                            in - 21, String.format("%.2f", fx), String.format("%.2f", fz), yaw);
                    if (Math.abs(fx - 0.5) > 1e-6 || Math.abs(fz - 0.5) > 1e-6 || Math.abs(yaw % 90) > 1e-3) bad.add("rooted again off the grid: " + fx + ", " + fz + ", facing " + yaw);
                }
                if (in == 30 && woke[0] < 0) bad.add("the weaver didn't wake 9 ticks after the wall went");
                if (in == 60) {
                    long following = wisps.stream().filter(wisp -> wisp.following() == holder).count();
                    PromisedEnd.LOGGER.info("[constructs_edges_walls_and_swarms] three wisps near one player with Lumen: {} follow", following);
                    if (following != com.panda.promisedend.entity.LumenWisp.MAX_FOLLOWERS) bad.add(following + " wisps followed one player");
                    wispsDone[0] = 1;
                }
                if (recoverAt[0] > 0 && in >= recoverAt[0] + 3 && wispsDone[0] == 1 && woke[0] > 0) {
                    if (!bad.isEmpty()) helper.fail("constructs: " + bad);
                    else helper.succeed();
                    wispsDone[0] = 2;
                }
                if (tick == 190 && wispsDone[0] != 2) helper.fail("the test ran out (lunge " + lungeAt[0] + ", recover " + recoverAt[0] + ", woke " + woke[0] + "): " + bad);
            });
        }
    }

    /** The constructs' recipes: Rift Dust recharges a cracked anchor, and four Ancient Purpur Scrap make four Ancient Purpur. */
    private static void constructRecipes(GameTestHelper helper) {
        var level = helper.getLevel();
        var recipes = level.getServer().getRecipeManager();
        List<String> bad = new ArrayList<>();
        var recharge = net.minecraft.world.item.crafting.CraftingInput.of(2, 1,
                List.of(new ItemStack(ModItems.CRACKED_VOID_ANCHOR.get()), new ItemStack(ModItems.RIFT_DUST.get())));
        var anchor = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, recharge, level).map(h -> h.value().assemble(recharge)).orElse(ItemStack.EMPTY);
        if (!anchor.is(ModItems.VOID_ANCHOR.get())) bad.add("a cracked anchor and Rift Dust make " + anchor);
        var scrap = new ItemStack(ModItems.ANCIENT_PURPUR_SCRAP.get());
        var four = net.minecraft.world.item.crafting.CraftingInput.of(2, 2, List.of(scrap.copy(), scrap.copy(), scrap.copy(), scrap.copy()));
        var purpur = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, four, level).map(h -> h.value().assemble(four)).orElse(ItemStack.EMPTY);
        if (!purpur.is(ModBlocks.ANCIENT_PURPUR_SET.base().get().asItem()) || purpur.getCount() != 4) bad.add("four scrap make " + purpur);
        if (!bad.isEmpty()) helper.fail("construct recipes: " + bad);
        else helper.succeed();
    }

    /**
     * A rift scar, as the Voidscar configures it, on a floor of end stone four deep high in the test level's sky, in four
     * seeds: it is cut (at least its length in air under the old top), lined with Voidstone and, in each, two or more
     * Voidsteel Scrap Ore; nothing changes more than RiftScarFeature.REACH blocks from its origin. On the same floor
     * with a layer of purpur under the top, a scar would cut into it, so none is made and the floor is untouched. On an
     * islet 7 across, too small for 12, a shorter scar is made: it cuts under half the islet (196 blocks).
     */
    private static void riftScarCarves(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var scar = grove(level, "rift_scar");
        var stone = net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState();
        int reach = com.panda.promisedend.worldgen.RiftScarFeature.REACH;
        for (int seed = 0; seed < 6; seed++) {
            BlockPos at = new BlockPos(9600 + 64 * seed, level.getMaxY() - 60, 9000);
            boolean blocked = seed == 4, islet = seed == 5;
            for (int d = 0; d < 4; d++) floor(level, at.below(d), islet ? 3 : 17, blocked && d == 1 ? net.minecraft.world.level.block.Blocks.PURPUR_BLOCK.defaultBlockState() : stone);
            boolean placed = scar.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(net.minecraft.util.Mth.murmurHash3Mixer(500 + seed)), at);
            int cut = 0, ore = 0, wall = 0, far = 0;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-(islet ? 3 : 17), -4, -(islet ? 3 : 17)), at.offset(islet ? 3 : 17, -1, islet ? 3 : 17))) {
                var state = level.getBlockState(p);
                boolean changed = !(state.is(net.minecraft.world.level.block.Blocks.END_STONE) || state.is(net.minecraft.world.level.block.Blocks.PURPUR_BLOCK));
                if (state.isAir()) cut++;
                else if (state.is(ModBlocks.VOIDSTEEL_SCRAP_ORE.get())) ore++;
                else if (state.is(ModBlocks.VOIDSTONE_SET.base().get())) wall++;
                if (changed && Math.max(Math.abs(p.getX() - at.getX()), Math.abs(p.getZ() - at.getZ())) > reach) far++;
            }
            PromisedEnd.LOGGER.info("[rift_scar_carves] seed {}{}: placed {}, {} cut, {} Voidstone, {} Voidsteel Scrap Ore", seed,
                    blocked ? " (purpur under the top)" : islet ? " (an islet 7 across)" : "",
                    placed, cut, wall, ore);
            if (blocked) {
                if (placed || cut + ore + wall != 0) bad.add("over purpur a scar was made: placed " + placed + ", " + cut + " cut");
            } else if (islet) {
                if (!placed || cut < com.panda.promisedend.worldgen.RiftScarFeature.SHORTEST || cut >= 98) bad.add("on an islet 7 across: placed " + placed + ", " + cut + " cut");
            } else {
                if (!placed || cut < 12) bad.add("seed " + seed + ": placed " + placed + ", only " + cut + " cut");
                if (wall < 20) bad.add("seed " + seed + ": only " + wall + " Voidstone in its walls");
                if (ore < 2) bad.add("seed " + seed + ": only " + ore + " Voidsteel Scrap Ore");
                if (far != 0) bad.add("seed " + seed + ": " + far + " blocks changed past its reach of " + reach);
            }
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-17, -5, -17), at.offset(17, 1, 17))) {
                if (!level.getBlockState(p).isAir()) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }
        if (!bad.isEmpty()) helper.fail("rift scar: " + bad);
        helper.succeed();
    }

    /**
     * A Shulker Reef, as the Reefs configure it, on a floor of end stone high in the test level's sky, in 40 seeds:
     * each makes 20 or more Ancient Purpur, standing 4 or more blocks over the floor at its tallest, nothing changed past
     * PurpurReefFeature.REACH blocks from its spot. Over the seeds: a fifth or more of the purpur hangs over air (the
     * branches, fans and caps: a reef of bare trunks has none); 0.8-3.5 end rods (its polyps) a reef; a colony in 10-45 %
     * of the reefs (one in four is asked; a reef with no perch has none), each of 2 or 3 wild shulkers, every one on the
     * purpur with air in its own block and the one over it. On the same floor under a layer of cobweb two blocks up (it
     * stops nothing that moves, so the growths still find the floor), every growth would go into the cobweb: none is
     * made and the cobweb is whole.
     */
    private static void purpurReefGrows(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var reef = grove(level, "purpur_reef");
        var coral = ModBlocks.ANCIENT_PURPUR_SET.base().get();
        var web = net.minecraft.world.level.block.Blocks.COBWEB;
        int reach = com.panda.promisedend.worldgen.PurpurReefFeature.REACH, seeds = 40;
        int rods = 0, colonies = 0, shulkers = 0, purpur = 0, hanging = 0;
        for (int seed = 0; seed <= seeds; seed++) {
            BlockPos at = new BlockPos(9600 + 64 * (seed % 20), level.getMaxY() - 60 - 30 * (seed / 20), 9800);
            boolean roofed = seed == seeds;
            floor(level, at, 14, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            if (roofed) floor(level, at.above(3), 14, web.defaultBlockState());
            boolean placed = reef.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(net.minecraft.util.Mth.murmurHash3Mixer(700 + seed)), at);
            int blocks = 0, tallest = 0, far = 0, here = 0, under = 0, webs = 0;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, 0, -16), at.offset(16, 14, 16))) {
                var state = level.getBlockState(p);
                if (state.isAir()) continue;
                if (state.is(web)) {
                    webs++;
                    continue;
                }
                if (state.is(coral)) {
                    blocks++;
                    tallest = Math.max(tallest, p.getY() - at.getY() + 1);
                    if (p.getY() > at.getY() && level.getBlockState(p.below()).isAir()) under++;
                }
                if (state.is(net.minecraft.world.level.block.Blocks.END_ROD)) here++;
                if (Math.max(Math.abs(p.getX() - at.getX()), Math.abs(p.getZ() - at.getZ())) > reach) far++;
            }
            var box = new net.minecraft.world.phys.AABB(at).inflate(16, 16, 16);
            var found = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Shulker.class, box);
            for (var shulker : found) {
                BlockPos b = shulker.blockPosition();
                if (!level.getBlockState(b.below()).is(coral) || !level.getBlockState(b).isAir() || !level.getBlockState(b.above()).isAir())
                    bad.add("seed " + seed + ": a shulker at " + b + " over " + level.getBlockState(b.below()) + ", in " + level.getBlockState(b) + ", under " + level.getBlockState(b.above()));
                shulker.discard();
            }
            PromisedEnd.LOGGER.info("[purpur_reef_grows] seed {}{}: placed {}, {} Ancient Purpur ({} over air), the tallest {} over the floor, {} end rods, {} shulkers",
                    seed, roofed ? " (under cobweb)" : "", placed, blocks, under, tallest, here, found.size());
            if (roofed) {
                if (placed || blocks != 0 || webs != 29 * 29) bad.add("under cobweb: placed " + placed + ", " + blocks + " purpur, " + webs + " of " + 29 * 29 + " cobwebs left");
            } else {
                if (!placed || blocks < 20 || tallest < 4) bad.add("seed " + seed + ": placed " + placed + ", " + blocks + " purpur, " + tallest + " tall");
                if (far != 0) bad.add("seed " + seed + ": " + far + " blocks past its reach of " + reach);
                if (!found.isEmpty() && (found.size() < 2 || found.size() > 3)) bad.add("seed " + seed + ": a colony of " + found.size());
                rods += here;
                purpur += blocks;
                hanging += under;
                if (!found.isEmpty()) colonies++;
                shulkers += found.size();
            }
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, -1, -16), at.offset(16, 14, 16))) {
                if (!level.getBlockState(p).isAir()) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }
        PromisedEnd.LOGGER.info("[purpur_reef_grows] over {} seeds: {} Ancient Purpur ({} over air), {} end rods, {} colonies, {} shulkers",
                seeds, purpur, hanging, rods, colonies, shulkers);
        if (hanging * 5 < purpur) bad.add("only " + hanging + " of " + purpur + " purpur over air: bare trunks");
        if (rods * 10 < seeds * 8 || rods * 10 > seeds * 35) bad.add(rods + " end rods in " + seeds + " reefs");
        if (colonies * 100 < seeds * 10 || colonies * 100 > seeds * 45) bad.add(colonies + " colonies in " + seeds + " reefs");
        if (!bad.isEmpty()) helper.fail("purpur reef: " + bad);
        helper.succeed();
    }

    /**
     * A Hollow Highlands cavern, as the Highlands configure it, in an island of end stone 41 across and 26 deep high in
     * the test level's sky, in 24 seeds. Each carves 200-1400 blocks of air; the island's top three layers are broken
     * only by its sinkhole (1-5 columns, the shaft's) and its bottom two not at all; nothing changes past
     * HollowCavernFeature.REACH blocks from its spot (its lining round the sinkhole is roof still). Over the seeds, about three in five of the cavern's walls are
     * Voidstone (0.45-0.75) and there are 2-15 Echo Amber Clusters a cavern, each standing on a wall it may stand on.
     * With a purpur block inside where the hollow would be (off the columns it samples), or in an island only 8 deep,
     * nothing is carved. In 4 islands with a notch cut out of their underside, whether or not a cavern is made, no
     * column is broken through at its bottom (it never opens to the void).
     */
    private static void hollowCavernCarves(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var cavern = grove(level, "hollow_cavern");
        var stone = net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState();
        var amber = ModBlocks.ECHO_AMBER_CLUSTER.get();
        var voidstone = ModBlocks.VOIDSTONE_SET.base().get();
        int reach = com.panda.promisedend.worldgen.HollowCavernFeature.REACH, seeds = 24, half = 20;
        int clusters = 0, linedWalls = 0, bareWalls = 0, notchedPlaced = 0;
        for (int seed = 0; seed < seeds + 6; seed++) {
            boolean blocked = seed == seeds, thin = seed == seeds + 1, notched = seed >= seeds + 2;
            int deep = thin ? 8 : 26;
            BlockPos top = new BlockPos(9600 + 64 * seed, level.getMaxY() - 50, 10400), at = top.below(deep);
            // the bottom of each column, as built: a notch 8 high under one side of the notched islands
            java.util.function.BiFunction<Integer, Integer, Integer> bottom = (dx, dz) -> top.getY() - deep + 1
                    + (notched && dx >= 2 && dx <= 9 && Math.abs(dz) <= 4 ? 8 : 0);
            for (BlockPos p : BlockPos.betweenClosed(top.offset(-half, -deep + 1, -half), top.offset(half, 0, half))) {
                if (p.getY() >= bottom.apply(p.getX() - top.getX(), p.getZ() - top.getZ())) level.setBlock(p, stone, 2);
            }
            BlockPos purpur = top.offset(3, -8, 2);
            if (blocked) level.setBlock(purpur, net.minecraft.world.level.block.Blocks.PURPUR_BLOCK.defaultBlockState(), 2);
            boolean placed = cavern.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(net.minecraft.util.Mth.murmurHash3Mixer(800 + seed)), at);
            int air = 0, far = 0, here = 0, lined = 0, bare = 0;
            java.util.Set<Long> roofBroken = new java.util.HashSet<>(), floorBroken = new java.util.HashSet<>();
            for (BlockPos p : BlockPos.betweenClosed(top.offset(-half, -deep + 1, -half), top.offset(half, 0, half))) {
                int dx = p.getX() - top.getX(), dz = p.getZ() - top.getZ(), floor = bottom.apply(dx, dz);
                if (p.getY() < floor) continue;
                var state = level.getBlockState(p);
                if (!state.isAir()) {
                    // a wall: island next to air inside the island's box
                    boolean wall = false;
                    for (var d : net.minecraft.core.Direction.values()) {
                        BlockPos q = p.relative(d);
                        int qx = q.getX() - top.getX(), qz = q.getZ() - top.getZ();
                        if (Math.abs(qx) <= half && Math.abs(qz) <= half && q.getY() <= top.getY() && q.getY() >= bottom.apply(qx, qz)
                                && level.getBlockState(q).isAir()) wall = true;
                    }
                    if (wall && state.is(voidstone)) lined++;
                    if (wall && state.is(net.minecraft.world.level.block.Blocks.END_STONE)) bare++;
                }
                boolean changed = !state.is(net.minecraft.world.level.block.Blocks.END_STONE) && !(blocked && p.equals(purpur));
                if (!changed) continue;
                if (Math.max(Math.abs(dx), Math.abs(dz)) > reach) far++;
                // a hole in the roof: air (its lining round the sinkhole is the roof still)
                if (p.getY() > top.getY() - 3 && state.isAir()) roofBroken.add(BlockPos.asLong(p.getX(), 0, p.getZ()));
                if (p.getY() < floor + 2) floorBroken.add(BlockPos.asLong(p.getX(), 0, p.getZ()));
                if (state.isAir()) air++;
                if (state.is(amber)) {
                    here++;
                    if (!state.canSurvive(level, p)) bad.add("seed " + seed + ": a cluster at " + p + " on " + level.getBlockState(p.relative(state.getValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING).getOpposite())));
                }
            }
            PromisedEnd.LOGGER.info("[hollow_cavern_carves] seed {}{}: placed {}, {} air, {} columns broken at the top, {} at the bottom, {} clusters, {} of {} walls lined, {} past its reach",
                    seed, blocked ? " (purpur inside)" : thin ? " (8 deep)" : notched ? " (notched)" : "", placed, air, roofBroken.size(), floorBroken.size(), here, lined, lined + bare, far);
            if (blocked || thin) {
                if (placed || air != 0) bad.add((blocked ? "with purpur inside" : "8 deep") + ": placed " + placed + ", " + air + " air");
                if (blocked && !level.getBlockState(purpur).is(net.minecraft.world.level.block.Blocks.PURPUR_BLOCK)) bad.add("the purpur was carved");
            } else if (notched) {
                if (placed) notchedPlaced++;
                if (!floorBroken.isEmpty()) bad.add("seed " + seed + " (notched): " + floorBroken.size() + " columns broken at the bottom");
                if (roofBroken.size() > 5) bad.add("seed " + seed + " (notched): " + roofBroken.size() + " columns broken at the top");
            } else {
                if (!placed || air < 200 || air > 1400) bad.add("seed " + seed + ": placed " + placed + ", " + air + " air");
                if (roofBroken.isEmpty() || roofBroken.size() > 5) bad.add("seed " + seed + ": " + roofBroken.size() + " columns broken at the top (the sinkhole is 1-5)");
                if (!floorBroken.isEmpty()) bad.add("seed " + seed + ": " + floorBroken.size() + " columns broken in the island's bottom two layers");
                if (far != 0) bad.add("seed " + seed + ": " + far + " blocks past its reach of " + reach);
                clusters += here;
                linedWalls += lined;
                bareWalls += bare;
            }
            for (BlockPos p : BlockPos.betweenClosed(top.offset(-half, -deep, -half), top.offset(half, 1, half))) {
                if (!level.getBlockState(p).isAir()) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }
        float share = linedWalls / (float) Math.max(1, linedWalls + bareWalls), each = clusters / (float) seeds;
        PromisedEnd.LOGGER.info("[hollow_cavern_carves] over {} seeds: {} Echo Amber Clusters ({} a cavern), {} of the walls lined; {} of 4 notched islands carved",
                seeds, clusters, each, share, notchedPlaced);
        if (each < 2 || each > 15) bad.add(each + " clusters a cavern, outside 2-15");
        if (share < 0.45F || share > 0.75F) bad.add(share + " of the walls lined, outside 0.45-0.75");
        if (!bad.isEmpty()) helper.fail("hollow cavern: " + bad);
        helper.succeed();
    }

    /**
     * The Hollow Highlands' dark. Where: in the End, a point made hollow_highlands under a roof of end stone is in the
     * caves' dark, over the roof isn't, made vanilla's highlands it isn't, and the test level (not the End) isn't. How dark: in the test level
     * (the End's light engine doesn't run with no player there), in a box of end stone with no light the middle is
     * shrouded; under a dim Echo Amber Cluster 3 off (5) still; under a flared one (11) not (over the roof is the End's
     * case: the arena has its own blocks over it). It is
     * for survival and adventure players, not creative or spectating ones.
     */
    private static void caveDarkHeldOff(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) {
            helper.fail("no End level");
            return;
        }
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var stone = net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState();
        // where: in one tick, as the End's chunks are only held for it
        BlockPos centre = new BlockPos(10248, end.getMaxY() - 60, 9224);
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-2, 2, -2), centre.offset(2, 2, 2))) end.setBlock(p, stone, 2);
        fillBiome(end, centre, "minecraft:end_highlands");
        if (com.panda.promisedend.logic.CaveDark.dark(end, centre)) bad.add("dark under a roof in vanilla's highlands");
        fillBiome(end, centre, "promisedend:hollow_highlands");
        if (!com.panda.promisedend.logic.CaveDark.dark(end, centre)) bad.add("no dark in the Hollow Highlands under a roof (biome "
                + end.getBiome(centre).getRegisteredName() + ", surface " + end.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, centre.getX(), centre.getZ()) + ")");
        if (com.panda.promisedend.logic.CaveDark.dark(end, centre.above(3))) bad.add("dark over the roof");
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-2, 2, -2), centre.offset(2, 2, 2))) end.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        // how dark: a box of end stone in the test level, the middle of it its eyes
        for (BlockPos r : BlockPos.betweenClosed(new BlockPos(1, 1, 1), new BlockPos(7, 6, 7))) {
            boolean inside = r.getX() > 1 && r.getX() < 7 && r.getZ() > 1 && r.getZ() < 7 && r.getY() > 1 && r.getY() < 6;
            level.setBlock(helper.absolutePos(r), inside ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : stone, 3);
        }
        BlockPos eyes = helper.absolutePos(new BlockPos(4, 3, 4)), lamp = helper.absolutePos(new BlockPos(4, 5, 5));
        if (com.panda.promisedend.logic.CaveDark.dark(level, eyes)) bad.add("dark outside the End");
        var amber = ModBlocks.ECHO_AMBER_CLUSTER.get().defaultBlockState().setValue(net.minecraft.world.level.block.AmethystClusterBlock.FACING, net.minecraft.core.Direction.DOWN);
        var lit = com.panda.promisedend.block.EchoAmberClusterBlock.LIT;
        var shrouded = new boolean[2];
        helper.runAtTickTime(10, () -> {
            shrouded[0] = com.panda.promisedend.logic.CaveDark.shrouded(level, eyes);
            level.setBlock(lamp, amber, 3);
        });
        helper.runAtTickTime(25, () -> {
            shrouded[1] = com.panda.promisedend.logic.CaveDark.shrouded(level, eyes);
            level.setBlock(lamp, amber.setValue(lit, true), 3);
            if (!shrouded[0]) bad.add("not shrouded in the box with no light");
            if (!shrouded[1]) bad.add("a dim cluster held the dark off");
            for (var mode : List.of(net.minecraft.world.level.GameType.SURVIVAL, net.minecraft.world.level.GameType.ADVENTURE,
                    net.minecraft.world.level.GameType.CREATIVE, net.minecraft.world.level.GameType.SPECTATOR)) {
                boolean affected = com.panda.promisedend.logic.CaveDark.affects(helper.makeMockPlayer(mode));
                if (affected != (mode == net.minecraft.world.level.GameType.SURVIVAL || mode == net.minecraft.world.level.GameType.ADVENTURE)) {
                    bad.add("a " + mode.getName() + " player " + (affected ? "is" : "isn't") + " in the dark");
                }
            }
            if (!bad.isEmpty()) {
                helper.fail("cave dark: " + bad);
                return;
            }
            // the flared cluster's light reaches the eyes when the light engine gets to it (a busy batch is slower): wait
            // for it, up to the test's timeout
            helper.succeedWhen(() -> {
                if (com.panda.promisedend.logic.CaveDark.shrouded(level, eyes)) throw helper.assertionException(
                        "a flared cluster didn't hold the dark off (block light %s)", level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, eyes));
            });
        });
    }

    /**
     * Void Grass: it stands on end stone, Chorus Moss and Lumen Moss, not on stone or grass; bone meal grows it into Tall
     * Void Grass, both halves; it is replaceable. Shears take one, a hand nothing; the tall one's lower half gives two
     * to shears, its upper half nothing (so a tall one is two, not four). A grove and a pool still grow on ground
     * covered in it, short and tall, and leave no half of a tall one alone.
     */
    private static void voidGrassRules(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var grass = ModBlocks.VOID_GRASS.get();
        var tall = ModBlocks.TALL_VOID_GRASS.get();
        var grounds = new java.util.LinkedHashMap<net.minecraft.world.level.block.Block, Boolean>();
        grounds.put(net.minecraft.world.level.block.Blocks.END_STONE, true);
        grounds.put(ModBlocks.CHORUS_MOSS.get(), true);
        grounds.put(ModBlocks.LUMEN_MOSS.get(), true);
        grounds.put(net.minecraft.world.level.block.Blocks.STONE, false);
        grounds.put(net.minecraft.world.level.block.Blocks.GRASS_BLOCK, false);
        int x = 1;
        for (var e : grounds.entrySet()) {
            BlockPos at = helper.absolutePos(new BlockPos(x, 2, 1));
            level.setBlock(at.below(), e.getKey().defaultBlockState(), 3);
            if (grass.defaultBlockState().canSurvive(level, at) != e.getValue()) bad.add("on " + e.getKey().getName().getString() + (e.getValue() ? " it can't stand" : " it stands"));
            x++;
        }
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 3));
        level.setBlock(at.below(), net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 3);
        level.setBlock(at, grass.defaultBlockState(), 3);
        if (!grass.defaultBlockState().canBeReplaced()) bad.add("not replaceable");
        var meal = (net.minecraft.world.level.block.BonemealableBlock) grass;
        if (!meal.isValidBonemealTarget(level, at, level.getBlockState(at), net.minecraft.world.level.block.BonemealSource.INTERACTION)) bad.add("bone meal can't grow it");
        meal.performBonemeal(level, level.getRandom(), at, level.getBlockState(at), net.minecraft.world.level.block.BonemealSource.INTERACTION);
        var half = net.minecraft.world.level.block.DoublePlantBlock.HALF;
        if (!level.getBlockState(at).is(tall) || level.getBlockState(at).getValue(half) != net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER
                || !level.getBlockState(at.above()).is(tall)) bad.add("bone meal gave " + level.getBlockState(at) + " under " + level.getBlockState(at.above()));
        var shears = new ItemStack(net.minecraft.world.item.Items.SHEARS);
        java.util.function.BiFunction<net.minecraft.world.level.block.state.BlockState, ItemStack, Integer> drops = (state, tool) -> level.getServer().reloadableRegistries()
                .getLootTable(state.getBlock().getLootTable().orElseThrow()).getRandomItems(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, tool)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, state)
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK)).stream()
                .filter(st -> st.is(grass.asItem())).mapToInt(ItemStack::getCount).sum();
        var lower = tall.defaultBlockState().setValue(half, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
        var upper = tall.defaultBlockState().setValue(half, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
        int[] got = {drops.apply(grass.defaultBlockState(), shears), drops.apply(grass.defaultBlockState(), ItemStack.EMPTY),
                drops.apply(lower, shears), drops.apply(upper, shears), drops.apply(lower, ItemStack.EMPTY)};
        int[] want = {1, 0, 2, 0, 0};
        if (!java.util.Arrays.equals(got, want)) bad.add("drops (short to shears, to a hand; tall lower, upper to shears; lower to a hand) " + java.util.Arrays.toString(got) + ", not " + java.util.Arrays.toString(want));
        // ground cover keeps nothing out: a grove and a pool on end stone under grass, short and tall, still grow, and
        // no half of a tall one is left alone (the grass a neighbouring chunk laid comes before a chunk's own features)
        BlockPos centre = new BlockPos(2000, level.getMaxY() - 60, 2640);
        var lowerState = tall.defaultBlockState().setValue(half, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
        var upperState = tall.defaultBlockState().setValue(half, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
        String[] kinds = {"chorus_grove", "mire_pool"};
        for (int k = 0; k < kinds.length; k++) {
            BlockPos c = centre.offset(64 * k, 0, 0);
            // the pool wants ground two deep
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-12, -2, -12), c.offset(12, -1, 12))) level.setBlock(p, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 2);
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-12, 0, -12), c.offset(12, 0, 12))) {
                if ((p.getX() + p.getZ()) % 3 == 0) {
                    level.setBlock(p, lowerState, 2);
                    level.setBlock(p.above(), upperState, 2);
                } else level.setBlock(p, grass.defaultBlockState(), 2);
            }
            int made = 0;
            for (int seed = 0; seed < 4; seed++) {
                if (grove(level, kinds[k]).place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(net.minecraft.util.Mth.murmurHash3Mixer(640 + seed)), c)) made++;
            }
            int lone = 0, built = 0;
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-16, -2, -16), c.offset(16, 30, 16))) {
                var st = level.getBlockState(p);
                if (st.is(ModBlocks.CHORUS_LOG.get()) || st.is(ModBlocks.VOID_WATER.get())) built++;
                if (st.is(tall) && !level.getBlockState(st.getValue(half) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER ? p.above() : p.below()).is(tall)) lone++;
            }
            PromisedEnd.LOGGER.info("[void_grass_rules] a {} on grass: {} of 4 made, {} blocks of it, {} lone halves", kinds[k], made, built, lone);
            if (made == 0 || built == 0) bad.add("no " + kinds[k] + " on grass (" + made + " of 4)");
            if (lone != 0) bad.add(lone + " lone halves of tall grass round a " + kinds[k]);
            clear(level, c);
        }
        if (!bad.isEmpty()) helper.fail("void grass: " + bad);
        helper.succeed();
    }

    /**
     * Bloom Meadows' flowers: each stands on end stone, Chorus Moss and Lumen Moss, not on stone or grass; neither gives
     * light (the glow is the texture's, so a meadow stays dark enough for Endermen); each drops itself, is a small
     * flower (#minecraft:small_flowers), and crafts into a magenta or a cyan dye. Bone meal
     * on one puts another of its kind on the open ground within EndFlowerBlock.SPREAD (a 5 by 5 floor with the flower in
     * the middle: the 24 cells round it, one a use, never further), then stops.
     */
    private static void endFlowersRules(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var recipes = level.getServer().getRecipeManager();
        java.util.Map<com.panda.promisedend.block.EndFlowerBlock, Object[]> flowers = new java.util.LinkedHashMap<>();
        flowers.put(ModBlocks.ENDER_BLOOM.get(), new Object[] {0, net.minecraft.world.item.Items.DYE.magenta()});
        flowers.put(ModBlocks.STARFLOWER.get(), new Object[] {0, net.minecraft.world.item.Items.DYE.cyan()});
        int row = 0;
        for (var e : flowers.entrySet()) {
            var flower = e.getKey();
            String name = flower.getName().getString();
            int x = 1;
            for (var ground : List.of(net.minecraft.world.level.block.Blocks.END_STONE, ModBlocks.CHORUS_MOSS.get(), ModBlocks.LUMEN_MOSS.get(),
                    net.minecraft.world.level.block.Blocks.STONE, net.minecraft.world.level.block.Blocks.GRASS_BLOCK)) {
                BlockPos at = helper.absolutePos(new BlockPos(x++, 2, 1 + row));
                level.setBlock(at.below(), ground.defaultBlockState(), 3);
                boolean want = ground != net.minecraft.world.level.block.Blocks.STONE && ground != net.minecraft.world.level.block.Blocks.GRASS_BLOCK;
                if (flower.defaultBlockState().canSurvive(level, at) != want) bad.add(name + (want ? " can't stand on " : " stands on ") + ground.getName().getString());
            }
            if (flower.defaultBlockState().getLightEmission() != (int) e.getValue()[0]) bad.add(name + " gives light " + flower.defaultBlockState().getLightEmission());
            if (!flower.defaultBlockState().is(net.minecraft.tags.BlockTags.SMALL_FLOWERS)) bad.add(name + " isn't in #minecraft:small_flowers");
            var drops = net.minecraft.world.level.block.Block.getDrops(flower.defaultBlockState(), level, BlockPos.ZERO, null);
            if (drops.size() != 1 || !drops.get(0).is(flower.asItem())) bad.add(name + " drops " + drops);
            var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 1, List.of(new ItemStack(flower)));
            var dye = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
            if (!dye.is((net.minecraft.world.item.Item) e.getValue()[1]) || dye.getCount() != 1) bad.add(name + " crafts into " + dye);
            // bone meal: a 7 by 7 floor, the 5 by 5 round the flower open, one cell past it open too (never reached)
            BlockPos centre = new BlockPos(2400 + 32 * row, level.getMaxY() - 60, 2400);
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-3, -1, -3), centre.offset(3, -1, 3))) level.setBlock(p, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 2);
            level.setBlock(centre, flower.defaultBlockState(), 2);
            var meal = (net.minecraft.world.level.block.BonemealableBlock) flower;
            int uses = 0;
            while (meal.isValidBonemealTarget(level, centre, flower.defaultBlockState(), net.minecraft.world.level.block.BonemealSource.INTERACTION) && uses < 40) {
                int before = 0, after = 0;
                for (BlockPos p : BlockPos.betweenClosed(centre.offset(-3, 0, -3), centre.offset(3, 0, 3))) if (level.getBlockState(p).is(flower)) before++;
                meal.performBonemeal(level, level.getRandom(), centre, flower.defaultBlockState(), net.minecraft.world.level.block.BonemealSource.INTERACTION);
                for (BlockPos p : BlockPos.betweenClosed(centre.offset(-3, 0, -3), centre.offset(3, 0, 3))) if (level.getBlockState(p).is(flower)) after++;
                if (after != before + 1) bad.add(name + ": a use of bone meal made " + (after - before));
                uses++;
            }
            int far = 0, near = 0;
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-3, 0, -3), centre.offset(3, 0, 3))) {
                if (!level.getBlockState(p).is(flower)) continue;
                if (Math.max(Math.abs(p.getX() - centre.getX()), Math.abs(p.getZ() - centre.getZ())) > com.panda.promisedend.block.EndFlowerBlock.SPREAD) far++;
                else near++;
            }
            if (uses != 24 || near != 25 || far != 0) bad.add(name + ": bone meal took " + uses + " uses, " + near + " flowers within 2, " + far + " past it");
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-3, -1, -3), centre.offset(3, 1, 3))) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            row++;
        }
        if (!bad.isEmpty()) helper.fail("end flowers: " + bad);
        helper.succeed();
    }

    /**
     * An Echo Amber Cluster on end stone: light 5; a survival player's hit, step or use flares it to 11, a sneaking step
     * doesn't; 100 ticks after the last touch it is dim again. It drops two Echo Amber to a pickaxe (more on average
     * with Fortune III, over 100 rolls), one to a hand, itself to Silk Touch; a pickaxe takes it.
     */
    private static void echoAmberFlares(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = ModBlocks.ECHO_AMBER_CLUSTER.get();
        List<String> bad = new ArrayList<>();
        var walker = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var sneaker = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        sneaker.setShiftKeyDown(true);
        var lit = com.panda.promisedend.block.EchoAmberClusterBlock.LIT;
        List<BlockPos> spots = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            BlockPos at = helper.absolutePos(new BlockPos(1 + 2 * i, 3, 2));
            level.setBlock(at.below(), net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 3);
            level.setBlock(at, block.defaultBlockState(), 3);
            spots.add(at);
        }
        if (level.getBlockState(spots.get(0)).getLightEmission() != 5) bad.add("dim light " + level.getBlockState(spots.get(0)).getLightEmission());
        level.getBlockState(spots.get(0)).attack(level, spots.get(0), walker);
        block.stepOn(level, spots.get(1), level.getBlockState(spots.get(1)), walker);
        block.stepOn(level, spots.get(2), level.getBlockState(spots.get(2)), sneaker);
        level.getBlockState(spots.get(3)).useItemOn(ItemStack.EMPTY, level, walker, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(spots.get(3)), net.minecraft.core.Direction.UP, spots.get(3), false));
        for (int i = 0; i < 4; i++) {
            var state = level.getBlockState(spots.get(i));
            boolean flares = i != 2;
            if (state.getValue(lit) != flares) bad.add(List.of("a hit", "a step", "a sneaking step", "a use").get(i) + (flares ? " didn't flare it" : " flared it"));
            if (flares && state.getLightEmission() != 11) bad.add("flared light " + state.getLightEmission());
        }
        var table = level.getServer().reloadableRegistries().getLootTable(block.getLootTable().orElseThrow());
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var silk = new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
        silk.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        var fortune = new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
        fortune.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
        java.util.function.BiFunction<ItemStack, Integer, List<ItemStack>> roll = (tool, i) -> table.getRandomItems(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, tool)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, block.defaultBlockState())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK), net.minecraft.util.Mth.murmurHash3Mixer(8100 + i));
        int fortuned = 0;
        for (int i = 0; i < 100; i++) {
            var pick = roll.apply(new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE), i);
            var hand = roll.apply(ItemStack.EMPTY, i);
            var kept = roll.apply(silk, i);
            if (pick.size() != 1 || !pick.get(0).is(ModItems.ECHO_AMBER.get()) || pick.get(0).getCount() != 2) bad.add("a pickaxe's roll " + i + " gave " + pick);
            if (hand.size() != 1 || !hand.get(0).is(ModItems.ECHO_AMBER.get()) || hand.get(0).getCount() != 1) bad.add("a hand's roll " + i + " gave " + hand);
            if (kept.size() != 1 || !kept.get(0).is(block.asItem())) bad.add("Silk Touch's roll " + i + " gave " + kept);
            fortuned += roll.apply(fortune, i).stream().mapToInt(ItemStack::getCount).sum();
            if (bad.size() > 3) break;
        }
        PromisedEnd.LOGGER.info("[echo_amber_flares] Fortune III: {} Echo Amber in 100 breaks", fortuned);
        if (fortuned <= 250) bad.add("Fortune III gave only " + fortuned + " in 100 breaks");
        if (!new ItemStack(net.minecraft.world.item.Items.WOODEN_PICKAXE).isCorrectToolForDrops(block.defaultBlockState())
                && block.defaultBlockState().requiresCorrectToolForDrops()) bad.add("a wooden pickaxe can't take it");
        if (!block.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) bad.add("not mineable with a pickaxe");
        // touch the first again at 50: it stays lit past its first 100
        helper.runAtTickTime(50, () -> level.getBlockState(spots.get(0)).attack(level, spots.get(0), walker));
        helper.runAtTickTime(120, () -> {
            if (!level.getBlockState(spots.get(0)).getValue(lit)) bad.add("touched again at 50, it was dim at 120");
            if (level.getBlockState(spots.get(1)).getValue(lit)) bad.add("still lit 120 ticks after a step");
        });
        helper.runAtTickTime(160, () -> {
            if (level.getBlockState(spots.get(0)).getValue(lit)) bad.add("still lit 110 ticks after its last touch");
            if (!bad.isEmpty()) helper.fail("Echo Amber Cluster: " + bad);
            helper.succeed();
        });
    }

    /**
     * The Ancient Purpur set: four of the grown block make four bricks, four bricks four tiles, two of its slabs a
     * pillar; the stonecutter makes each from the grown block. All take a pickaxe and drop themselves; the grown block
     * sounds as coral, the cut ones as stone, with vanilla purpur's strength (1.5).
     */
    private static void ancientPurpurSet(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var recipes = level.getServer().getRecipeManager();
        var grown = new ItemStack(ModBlocks.ANCIENT_PURPUR_SET.base().get());
        var bricks = new ItemStack(ModBlocks.ANCIENT_PURPUR_BRICK_SET.base().get());
        var slab = new ItemStack(ModBlocks.ANCIENT_PURPUR_SET.slab().get());
        record Craft(String name, int w, int h, ItemStack in, net.minecraft.world.item.Item out, int count) {}
        for (var c : List.of(new Craft("bricks", 2, 2, grown, ModBlocks.ANCIENT_PURPUR_BRICK_SET.base().get().asItem(), 4),
                new Craft("tiles", 2, 2, bricks, ModBlocks.ANCIENT_PURPUR_TILES.get().asItem(), 4),
                new Craft("pillar", 1, 2, slab, ModBlocks.ANCIENT_PURPUR_PILLAR.get().asItem(), 1))) {
            List<ItemStack> grid = new ArrayList<>();
            for (int i = 0; i < c.w() * c.h(); i++) grid.add(c.in().copy());
            var input = net.minecraft.world.item.crafting.CraftingInput.of(c.w(), c.h(), grid);
            var out = recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).map(h -> h.value().assemble(input)).orElse(ItemStack.EMPTY);
            if (!out.is(c.out()) || out.getCount() != c.count()) bad.add("the " + c.name() + " recipe makes " + out);
        }
        var cut = new net.minecraft.world.item.crafting.SingleRecipeInput(grown);
        java.util.Set<net.minecraft.world.item.Item> fromStone = new java.util.HashSet<>();
        for (var h : recipes.getRecipes()) {
            if (h.value() instanceof net.minecraft.world.item.crafting.StonecutterRecipe r && r.matches(cut, level)) fromStone.add(r.assemble(cut).getItem());
        }
        for (var want : List.of(ModBlocks.ANCIENT_PURPUR_BRICK_SET.base().get(), ModBlocks.ANCIENT_PURPUR_TILES.get(), ModBlocks.ANCIENT_PURPUR_PILLAR.get(),
                ModBlocks.ANCIENT_PURPUR_SET.stairs().get(), ModBlocks.ANCIENT_PURPUR_BRICK_SET.wall().get())) {
            if (!fromStone.contains(want.asItem())) bad.add("the stonecutter makes no " + want.getName().getString() + " from Ancient Purpur");
        }
        var set = List.of(ModBlocks.ANCIENT_PURPUR_SET.base().get(), ModBlocks.ANCIENT_PURPUR_SET.stairs().get(), ModBlocks.ANCIENT_PURPUR_SET.slab().get(),
                ModBlocks.ANCIENT_PURPUR_SET.wall().get(), ModBlocks.ANCIENT_PURPUR_BRICK_SET.base().get(), ModBlocks.ANCIENT_PURPUR_BRICK_SET.stairs().get(),
                ModBlocks.ANCIENT_PURPUR_BRICK_SET.slab().get(), ModBlocks.ANCIENT_PURPUR_BRICK_SET.wall().get(), ModBlocks.ANCIENT_PURPUR_PILLAR.get(),
                ModBlocks.ANCIENT_PURPUR_TILES.get());
        if (set.size() != 10) bad.add(set.size() + " blocks in the set");
        for (var block : set) {
            var state = block.defaultBlockState();
            if (!state.requiresCorrectToolForDrops() || !state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE)) bad.add(block.getName().getString() + " doesn't take a pickaxe");
            if (Math.abs(block.defaultDestroyTime() - 1.5F) > 0.001F) bad.add(block.getName().getString() + " strength " + block.defaultDestroyTime());
        }
        if (ModBlocks.ANCIENT_PURPUR_SET.base().get().defaultBlockState().getSoundType() != net.minecraft.world.level.block.SoundType.CORAL_BLOCK) bad.add("the grown block doesn't sound as coral");
        if (ModBlocks.ANCIENT_PURPUR_TILES.get().defaultBlockState().getSoundType() != net.minecraft.world.level.block.SoundType.STONE) bad.add("the tiles don't sound as stone");
        if (!bad.isEmpty()) helper.fail("Ancient Purpur set: " + bad);
        helper.succeed();
    }

    /**
     * The Voidscar's islands, the Rim's debris with {@code anywhere}: 9000 blocks out, where the Rim's own copy builds
     * nothing (it keeps to its ring), the Voidscar's builds an island; over 2000 plans, three in five or more of its
     * islands 13 or more across are split, where the Rim's are two in eleven.
     */
    private static void voidscarIslands(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        BlockPos at = new BlockPos(9000, level.getMaxY() - 60, 9400);
        var random = net.minecraft.util.RandomSource.create(77);
        if (grove(level, "rim_debris").place(level, level.getChunkSource().getGenerator(), random, at)) bad.add("the Rim's debris built 9000 out");
        boolean built = grove(level, "voidscar_debris").place(level, level.getChunkSource().getGenerator(), random, at);
        int blocks = 0;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, -20, -16), at.offset(16, 8, 16))) if (!level.getBlockState(p).isAir()) blocks++;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, -20, -16), at.offset(16, 8, 16))) {
            if (!level.getBlockState(p).isAir()) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        if (!built || blocks < 60) bad.add("the Voidscar's debris 9000 out: built " + built + ", " + blocks + " blocks");
        var debris = (com.panda.promisedend.worldgen.RimDebrisFeature) grove(level, "voidscar_debris");
        int big = 0, split = 0;
        for (int i = 0; i < 2000; i++) {
            var plan = debris.plan(net.minecraft.util.RandomSource.create(net.minecraft.util.Mth.murmurHash3Mixer(30000 + i)));
            if (plan.radius() < 6) continue;
            big++;
            if (plan.shape() == com.panda.promisedend.worldgen.RimDebrisFeature.Shape.SPLIT) split++;
        }
        PromisedEnd.LOGGER.info("[voidscar_islands] {} blocks 9000 out; {} of {} islands 13 or more across split", blocks, split, big);
        if (split * 5 < big * 3) bad.add("only " + split + " of " + big + " big islands split");
        if (!bad.isEmpty()) helper.fail("Voidscar islands: " + bad);
        helper.succeed();
    }

    /**
     * The Voidscar's worn edges, as configured, on a domed island two blocks thick high in the test level's sky (its
     * top flat out to ring 4, a block lower a ring after, out to ring 6, the void round it): Crumbling Voidstone only on
     * tops within 2 blocks of the void (ring 3 and out, as the dome is laid), some there in each of three seeds, none in
     * its middle, never on a top that isn't its ground (a purpur block on ring 5), and laid as columns down to the air,
     * so each falls (CrumblingVoidstoneBlock.column). The same dome on a floor five blocks deep has a thick edge: none.
     */
    private static void crumblingEdgesLineIslands(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var edge = grove(level, "crumbling_edge");
        var crumbling = ModBlocks.CRUMBLING_VOIDSTONE.get();
        for (int seed = 0; seed < 3; seed++) {
            // the chunk's middle, so the whole island is in the chunk the feature works on
            BlockPos centre = new BlockPos(9600 + 64 * seed + 8, level.getMaxY() - 60, 9608);
            diamond(level, centre, 3, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            BlockPos purpur = centre.offset(5, -1 - (5 - DOME_FLAT), 0);
            level.setBlock(purpur, net.minecraft.world.level.block.Blocks.PURPUR_BLOCK.defaultBlockState(), 2);
            edge.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(net.minecraft.util.Mth.murmurHash3Mixer(600 + seed)), centre);
            int rim = 0, middle = 0, stuck = 0;
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-8, -6, -8), centre.offset(8, 0, 8))) {
                if (!level.getBlockState(p).is(crumbling)) continue;
                if (Math.abs(p.getX() - centre.getX()) + Math.abs(p.getZ() - centre.getZ()) >= 3) rim++;
                else middle++;
                // a column's top: it and what is under it fall
                if (!level.getBlockState(p.above()).is(crumbling) && com.panda.promisedend.block.CrumblingVoidstoneBlock.column(level, p) == 0) stuck++;
            }
            if (stuck != 0) bad.add("seed " + seed + ": " + stuck + " crumbling tops that wouldn't fall");
            PromisedEnd.LOGGER.info("[crumbling_edges_line_islands] seed {}: {} crumbling on the rim (rings 3-6, 72 tops), {} in the middle", seed, rim, middle);
            if (rim < 10) bad.add("seed " + seed + ": only " + rim + " crumbling on the rim");
            if (middle != 0) bad.add("seed " + seed + ": " + middle + " crumbling in the middle");
            if (!level.getBlockState(purpur).is(net.minecraft.world.level.block.Blocks.PURPUR_BLOCK)) bad.add("seed " + seed + ": the purpur top became " + level.getBlockState(purpur));
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-8, -8, -8), centre.offset(8, 1, 8))) {
                if (!level.getBlockState(p).isAir()) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
        }
        // a thick edge: the dome on a floor 15 across and five deep under its rim
        BlockPos centre = new BlockPos(9600 + 64 * 3 + 8, level.getMaxY() - 60, 9608);
        diamond(level, centre, 3, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
        for (int d = 2; d <= 6; d++) floor(level, centre.below(d), 7, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
        edge.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(net.minecraft.util.Mth.murmurHash3Mixer(603)), centre);
        int thick = 0;
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-8, -8, -8), centre.offset(8, 0, 8))) if (level.getBlockState(p).is(crumbling)) thick++;
        PromisedEnd.LOGGER.info("[crumbling_edges_line_islands] a thick edge: {} crumbling", thick);
        if (thick != 0) bad.add(thick + " crumbling on a thick edge");
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-8, -8, -8), centre.offset(8, 1, 8))) {
            if (!level.getBlockState(p).isAir()) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        if (!bad.isEmpty()) helper.fail("crumbling edges: " + bad);
        helper.succeed();
    }

    /**
     * Crumbling Voidstone over open air: a survival player's step cracks it (a tick is scheduled) and DELAY ticks later
     * it has fallen, with the block of it under it (a column of two); a step by a sneaking player, one in creative, or a
     * pig cracks nothing and it stays. One on end stone cracks and stays (it can't fall). What falls leaves no item. A
     * pickaxe breaks it to Voidstone, Silk Touch keeps it.
     */
    private static void crumblingVoidstoneFalls(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = ModBlocks.CRUMBLING_VOIDSTONE.get();
        List<String> bad = new ArrayList<>();
        var walker = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        var sneaker = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        sneaker.setShiftKeyDown(true);
        var creative = helper.makeMockPlayer(net.minecraft.world.level.GameType.CREATIVE);
        var pig = net.minecraft.world.entity.EntityTypes.PIG.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        // the walker steps on a column of two over air, and on one on end stone (the last)
        List<net.minecraft.world.entity.Entity> steppers = List.of(walker, sneaker, creative, pig, walker);
        List<BlockPos> spots = new ArrayList<>();
        for (int i = 0; i < steppers.size(); i++) {
            BlockPos at = helper.absolutePos(new BlockPos(1 + 2 * i % 10, 5, 3 + 2 * (i / 5)));
            if (i == 4) at = helper.absolutePos(new BlockPos(1, 5, 6));
            for (int dy = 1; dy <= 3; dy++) level.setBlock(at.below(dy), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            if (i == 0) level.setBlock(at.below(), block.defaultBlockState(), 3);
            if (i == 4) level.setBlock(at.below(), net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 3);
            level.setBlock(at, block.defaultBlockState(), 3);
            block.stepOn(level, at, level.getBlockState(at), steppers.get(i));
            boolean cracked = level.getBlockTicks().hasScheduledTick(at, block);
            boolean cracks = i == 0 || i == 4;
            if (cracked != cracks) bad.add((cracks ? "a survival step didn't crack it" : List.of("", "a sneaking step", "a creative step", "a pig").get(i) + " cracked it"));
            spots.add(at);
        }
        var table = level.getServer().reloadableRegistries().getLootTable(block.getLootTable().orElseThrow());
        var silk = new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        for (var c : List.of(java.util.Map.entry(new ItemStack(net.minecraft.world.item.Items.WOODEN_PICKAXE), ModBlocks.VOIDSTONE_SET.base().get().asItem()),
                java.util.Map.entry(silk, block.asItem()))) {
            var drops = table.getRandomItems(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, c.getKey())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, block.defaultBlockState())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK));
            if (drops.size() != 1 || !drops.get(0).is(c.getValue()) || drops.get(0).getCount() != 1) bad.add("to " + c.getKey() + " it drops " + drops);
        }
        if (!new ItemStack(net.minecraft.world.item.Items.WOODEN_PICKAXE).isCorrectToolForDrops(block.defaultBlockState())) bad.add("a wooden pickaxe can't take it");
        helper.runAfterDelay(com.panda.promisedend.block.CrumblingVoidstoneBlock.DELAY + 2, () -> {
            for (int i = 0; i < spots.size(); i++) {
                boolean still = level.getBlockState(spots.get(i)).is(block);
                if (still == (i == 0)) bad.add(i == 0 ? "the cracked block hadn't fallen after " + (com.panda.promisedend.block.CrumblingVoidstoneBlock.DELAY + 2) + " ticks"
                        : i == 4 ? "the block on end stone went" : "the block stepped on by " + steppers.get(i).getName().getString() + " fell");
            }
            if (level.getBlockState(spots.get(0).below()).is(block)) bad.add("the block under the cracked one stayed");
            var box = new net.minecraft.world.phys.AABB(spots.get(0)).inflate(4, 8, 4);
            if (!level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box).isEmpty()) bad.add("what fell left an item");
            pig.discard();
            if (!bad.isEmpty()) helper.fail("Crumbling Voidstone: " + bad);
            helper.succeed();
        });
    }

    /**
     * Voidsteel Scrap Ore takes a diamond-tier pickaxe (diamond, Resonite, netherite) and not iron or Voidstone's; it
     * gives one Voidsteel Scrap, one still with Fortune III over 100 rolls, and itself to Silk Touch. It is in #c:ores,
     * glows (light 6) and doesn't blow up (blast resistance 1200, as ancient debris).
     */
    private static void voidsteelOreDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        var ore = ModBlocks.VOIDSTEEL_SCRAP_ORE.get();
        var state = ore.defaultBlockState();
        List<String> bad = new ArrayList<>();
        var tools = java.util.Map.of(net.minecraft.world.item.Items.IRON_PICKAXE, false, ModItems.VOIDSTONE_PICKAXE.get(), false,
                net.minecraft.world.item.Items.DIAMOND_PICKAXE, true, ModItems.RESONITE_PICKAXE.get(), true, net.minecraft.world.item.Items.NETHERITE_PICKAXE, true);
        tools.forEach((tool, takes) -> {
            if (new ItemStack(tool).isCorrectToolForDrops(state) != takes) bad.add(tool + (takes ? " can't take it" : " takes it"));
        });
        var table = level.getServer().reloadableRegistries().getLootTable(ore.getLootTable().orElseThrow());
        var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var silk = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        silk.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        var fortune = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
        fortune.enchant(enchantments.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.FORTUNE), 3);
        for (var c : List.of(java.util.Map.entry(new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE), ModItems.VOIDSTEEL_SCRAP.get()),
                java.util.Map.entry(fortune, ModItems.VOIDSTEEL_SCRAP.get()), java.util.Map.entry(silk, ore.asItem()))) {
            for (int i = 0; i < 100; i++) {
                var drops = table.getRandomItems(new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, c.getKey())
                        .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, state)
                        .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK), net.minecraft.util.Mth.murmurHash3Mixer(8000 + i));
                if (drops.size() != 1 || !drops.get(0).is(c.getValue()) || drops.get(0).getCount() != 1) {
                    bad.add("to " + c.getKey() + " roll " + i + " gave " + drops);
                    break;
                }
            }
        }
        if (!state.is(net.minecraft.tags.TagKey.create(Registries.BLOCK, Identifier.parse("c:ores")))) bad.add("not in #c:ores");
        if (state.getLightEmission() != 6) bad.add("light " + state.getLightEmission());
        if (ore.getExplosionResistance() < 1200) bad.add("blast resistance " + ore.getExplosionResistance());
        if (!bad.isEmpty()) helper.fail("Voidsteel Scrap Ore: " + bad);
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
        int fallenPieces = 0;
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
            // from the floor's own layer, where the columns' feet and the sunk pieces are
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-16, -1, -16), centre.offset(16, 40, 16))) {
                var state = level.getBlockState(p);
                // a fallen piece: crystal at the floor past the columns' feet (they lean out only higher up), sunk in the
                // floor or lying on it, never over air and never budding
                double off = Math.hypot(p.getX() - centre.getX(), p.getZ() - centre.getZ());
                if ((state.is(crystal) || state.is(budding)) && p.getY() <= y && off >= com.panda.promisedend.worldgen.ResoniteSpireFeature.FALLEN_FROM - 0.5) {
                    fallenPieces++;
                    if (p.getY() == y && level.getBlockState(p.below()).isAir()) bad.add("a fallen piece over air at " + p);
                    if (state.is(budding)) bad.add("a budding fallen piece at " + p);
                }
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
        PromisedEnd.LOGGER.info("[resonite_spires_grow] {} fallen blocks at the floor round ten formations", fallenPieces);
        if (fallenPieces < 10) bad.add("only " + fallenPieces + " fallen blocks at the floor round ten formations");
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
     * The Updraft Vent, in the arena, whose chunks already tick, with its barrier roof opened over each vent but the
     * roofed one (put back after): an armour stand on a crafted vent rises above its start within 40 ticks, its rise reaching the cap (0.7 a tick once its own
     * gravity is off the vent's 0.8) within 9 ticks, never faster than 0.8 a tick, and an item there rises too; a stand
     * on a platform 18 over a crafted vent's face (its column is 16) stays put; a roof 7 over a vent stops the
     * column at 6, and the stand under it never gets its head past the roof; a stand 20 over an ancient vent (32) rises.
     * (In the test level's far sky the forced chunks took from 3 to over 300 ticks to tick entities, and an entity added
     * before then doesn't move at all.)
     */
    private static void updraftLiftsEntity(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var crafted = ModBlocks.UPDRAFT_VENT.get().defaultBlockState();
        var ancient = crafted.setValue(com.panda.promisedend.block.UpdraftVentBlock.ANCIENT, true);
        // columns 4 apart (each reaches 1.5 out); the arena's roof is 7 over the floor, so it is the roofed vent's roof
        BlockPos onVent = helper.absolutePos(new BlockPos(2, 1, 2)), platformed = helper.absolutePos(new BlockPos(6, 1, 2)),
                roofed = helper.absolutePos(new BlockPos(10, 1, 2)), deep = helper.absolutePos(new BlockPos(2, 1, 8)), item = helper.absolutePos(new BlockPos(6, 1, 8));
        java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> roof = new java.util.HashMap<>();
        for (BlockPos vent : List.of(onVent, platformed, deep, item)) openSky(level, vent, 40, roof, bad);
        if (!level.getBlockState(roofed.above(7)).isCollisionShapeFullBlock(level, roofed.above(7))) bad.add("no roof 7 over the roofed vent: " + level.getBlockState(roofed.above(7)));
        for (BlockPos p : List.of(onVent, platformed, roofed, item)) level.setBlock(p, crafted, 3);
        level.setBlock(deep, ancient, 3);
        level.setBlock(platformed.above(18), net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 3);
        List<BlockPos> columns = List.of(onVent.above(), platformed.above(19), roofed.above(), deep.above(21), item.above());
        List<net.minecraft.world.entity.Entity> made = new ArrayList<>();
        double[] start = new double[5];
        double[] fastest = {0};
        double[] highestHead = {0};
        int[] began = {-1};
        int[] capped = {-1};
        for (int t = 1; t <= 340; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (began[0] < 0) {
                    if (!columns.stream().allMatch(level::isPositionEntityTicking)) {
                        if (tick == 300) helper.fail("the vents' chunks weren't entity-ticking by tick 300");
                        return;
                    }
                    began[0] = tick;
                    for (int i = 0; i < 4; i++) {
                        BlockPos at = columns.get(i);
                        made.add(new net.minecraft.world.entity.decoration.ArmorStand(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5));
                    }
                    BlockPos at = columns.get(4);
                    var stick = new net.minecraft.world.entity.item.ItemEntity(level, at.getX() + 0.5, at.getY(), at.getZ() + 0.5, new ItemStack(net.minecraft.world.item.Items.STICK));
                    stick.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                    stick.setNeverPickUp();
                    made.add(stick);
                    for (int i = 0; i < 5; i++) {
                        level.addFreshEntity(made.get(i));
                        start[i] = made.get(i).getY();
                    }
                    return;
                }
                var a = made.get(0);
                var b = made.get(1);
                var c = made.get(2);
                var d = made.get(3);
                var e = made.get(4);
                fastest[0] = Math.max(fastest[0], a.getDeltaMovement().y);
                if (capped[0] < 0 && a.getDeltaMovement().y >= 0.7) capped[0] = tick - began[0];
                highestHead[0] = Math.max(highestHead[0], c.getY() + c.getBbHeight());
                if (tick != began[0] + 40) return;
                PromisedEnd.LOGGER.info("[updraft_lifts_entity] from tick {}: rose {} (stand), {} (item), {} (20 over an ancient vent); at the cap after {} ticks, the fastest {}; on the platform {}; under the roof the head reached {} (roof at {})",
                        began[0], a.getY() - start[0], e.getY() - start[4], d.getY() - start[3], capped[0], fastest[0], b.getY() - start[1], highestHead[0], roofed.getY() + 7);
                if (a.getY() <= start[0] + 1) bad.add("a stand on a crafted vent rose only " + (a.getY() - start[0]) + " in 40 ticks");
                if (fastest[0] > com.panda.promisedend.logic.Updraft.CAP + 1e-6) bad.add("a stand rose at " + fastest[0] + " a tick, over 0.8");
                if (capped[0] < 0 || capped[0] > 9) bad.add("a stand on a crafted vent reached the cap after " + capped[0] + " ticks, not 9 or fewer");
                if (e.getY() <= start[4] + 1) bad.add("an item on a crafted vent rose only " + (e.getY() - start[4]));
                if (Math.abs(b.getY() - start[1]) > 0.01) bad.add("a stand 18 over a crafted vent moved " + (b.getY() - start[1]));
                if (highestHead[0] > roofed.getY() + 7 + 1e-6) bad.add("under a roof 7 over the vent the stand's head reached " + highestHead[0]);
                if (!(level.getBlockEntity(roofed) instanceof com.panda.promisedend.block.UpdraftVentBlockEntity vent) || vent.height() != 6)
                    bad.add("the roofed vent's column isn't 6: " + level.getBlockEntity(roofed));
                if (d.getY() <= start[3] + 1) bad.add("a stand 20 over an ancient vent rose only " + (d.getY() - start[3]));
                made.forEach(net.minecraft.world.entity.Entity::discard);
                for (BlockPos p : List.of(onVent, platformed, roofed, deep, item, platformed.above(18)))
                    level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                roof.forEach((pos, state) -> level.setBlock(pos, state, 3));
                if (!bad.isEmpty()) helper.fail("updraft: " + bad);
                helper.succeed();
            });
        }
    }

    /**
     * The Lumen Lamp gives light 15 (14 beside it, as the sea lantern's), drops itself, and five Lumen in a polished
     * voidstone cage make two; four Lumen don't.
     */
    private static void lumenLampLights(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        var lamp = ModBlocks.LUMEN_LAMP.get().defaultBlockState();
        level.setBlock(at, lamp, 3);
        var drops = net.minecraft.world.level.block.Block.getDrops(lamp, level, at, null);
        if (lamp.getLightEmission() != 15) bad.add("the lamp gives light " + lamp.getLightEmission());
        if (drops.size() != 1 || !drops.get(0).is(ModBlocks.LUMEN_LAMP.get().asItem())) bad.add("the lamp drops " + drops);
        var recipes = level.getServer().getRecipeManager();
        var p = new ItemStack(ModBlocks.POLISHED_VOIDSTONE_SET.base().get());
        var l = new ItemStack(ModItems.LUMEN.get());
        java.util.function.Function<List<ItemStack>, ItemStack> craft = cells -> {
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, cells);
            return recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
        };
        var made = craft.apply(List.of(p.copy(), l.copy(), p.copy(), l.copy(), l.copy(), l.copy(), p.copy(), l.copy(), p.copy()));
        if (!made.is(ModBlocks.LUMEN_LAMP.get().asItem()) || made.getCount() != 2) bad.add("five Lumen in a polished voidstone cage make " + made);
        var short1 = craft.apply(List.of(p.copy(), l.copy(), p.copy(), l.copy(), ItemStack.EMPTY, l.copy(), p.copy(), l.copy(), p.copy()));
        if (!short1.isEmpty()) bad.add("four Lumen make " + short1);
        // the light engine runs off the tick: read the block light beside it a little later
        helper.runAtTickTime(10, () -> {
            int beside = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, at.above());
            PromisedEnd.LOGGER.info("[lumen_lamp_lights] light {}, beside it {}; drops {}; the recipe makes {}", lamp.getLightEmission(), beside, drops, made);
            if (beside != 14) bad.add("the block light beside the lamp is " + beside);
            level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            if (!bad.isEmpty()) helper.fail("Lumen Lamp: " + bad);
            helper.succeed();
        });
    }

    /**
     * The column's edges, one vent tick each on still armour stands: over a crafted vent's face 15.5 up is lifted and
     * 16 isn't, 1.4 off the centre is and 1.6 isn't; over an ancient one 31.5 is and 32 isn't. A stand moving sideways
     * keeps its sideways speed. An end crystal (no gravity) and a shulker (`#promisedend:updraft_ignores`) aren't moved.
     */
    private static void updraftColumnBounds(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        // in the arena, whose chunks already tick: the two vents' columns 7 apart, a hole opened in the arena's barrier
        // ceiling over each (put back after) so the sky over them is open
        BlockPos crafted = helper.absolutePos(new BlockPos(2, 1, 2)), ancient = helper.absolutePos(new BlockPos(9, 1, 9));
        java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> roof = new java.util.HashMap<>();
        for (BlockPos vent : List.of(crafted, ancient)) openSky(level, vent, 40, roof, bad);
        var craftedState = ModBlocks.UPDRAFT_VENT.get().defaultBlockState();
        var ancientState = craftedState.setValue(com.panda.promisedend.block.UpdraftVentBlock.ANCIENT, true);
        level.setBlock(crafted, craftedState, 3);
        level.setBlock(ancient, ancientState, 3);
        // where each one stands, over the vent's face, and whether the column should take it
        record Spot(String name, BlockPos vent, double dx, double dy, boolean lifted) {}
        List<Spot> spots = List.of(new Spot("15.5 over a crafted vent", crafted, 0, 15.5, true), new Spot("16 over a crafted vent", crafted, 0, 16, false),
                new Spot("1.4 off a crafted vent", crafted, 1.4, 2, true), new Spot("1.6 off a crafted vent", crafted, 1.6, 2, false),
                new Spot("31.5 over an ancient vent", ancient, 0, 31.5, true), new Spot("32 over an ancient vent", ancient, 0, 32, false));
        List<net.minecraft.world.entity.Entity> made = new ArrayList<>();
        List<net.minecraft.world.entity.decoration.ArmorStand> stands = new ArrayList<>();
        for (Spot spot : spots) {
            var stand = new net.minecraft.world.entity.decoration.ArmorStand(level, spot.vent().getX() + 0.5 + spot.dx(), spot.vent().getY() + 1 + spot.dy(), spot.vent().getZ() + 0.5);
            level.addFreshEntity(stand);
            stands.add(stand);
            made.add(stand);
        }
        var sideways = new net.minecraft.world.entity.decoration.ArmorStand(level, crafted.getX() + 0.5, crafted.getY() + 5, crafted.getZ() + 0.5);
        level.addFreshEntity(sideways);
        made.add(sideways);
        var crystal = new net.minecraft.world.entity.boss.enderdragon.EndCrystal(level, crafted.getX() + 0.5, crafted.getY() + 8, crafted.getZ() + 0.5);
        level.addFreshEntity(crystal);
        made.add(crystal);
        var shulker = net.minecraft.world.entity.EntityTypes.SHULKER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        shulker.snapTo(crafted.getX() + 0.5, crafted.getY() + 11, crafted.getZ() + 0.5, 0, 0);
        level.addFreshEntity(shulker);
        made.add(shulker);
        // projectiles: one shot up a column would bob over it for ever (an arrow never lands, a pearl keeps its chunk loaded)
        List<net.minecraft.world.entity.Entity> shot = new ArrayList<>();
        for (var type : List.<net.minecraft.world.entity.EntityType<?>>of(net.minecraft.world.entity.EntityTypes.ARROW, net.minecraft.world.entity.EntityTypes.ENDER_PEARL)) {
            var p = type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            p.snapTo(crafted.getX() + 0.5, crafted.getY() + 3 + shot.size() * 10, crafted.getZ() + 0.5, 0, 0);
            level.addFreshEntity(p);
            shot.add(p);
            made.add(p);
        }
        for (var e : made) e.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
        sideways.setDeltaMovement(0.3, 0, 0.2);
        for (BlockPos vent : List.of(crafted, ancient)) {
            com.panda.promisedend.block.UpdraftVentBlockEntity.tick(level, vent, level.getBlockState(vent), (com.panda.promisedend.block.UpdraftVentBlockEntity) level.getBlockEntity(vent));
        }
        for (int i = 0; i < spots.size(); i++) {
            double vy = stands.get(i).getDeltaMovement().y;
            if ((vy > 0.1) != spots.get(i).lifted()) bad.add(spots.get(i).name() + ": " + (spots.get(i).lifted() ? "not lifted" : "lifted") + " (vy " + vy + ")");
        }
        var v = sideways.getDeltaMovement();
        if (v.y < 0.1 || Math.abs(v.x - 0.3) > 1e-9 || Math.abs(v.z - 0.2) > 1e-9) bad.add("a stand moving sideways came out at " + v);
        if (crystal.getDeltaMovement().lengthSqr() > 0) bad.add("an end crystal was moved: " + crystal.getDeltaMovement());
        if (shulker.getDeltaMovement().lengthSqr() > 0) bad.add("a shulker was moved: " + shulker.getDeltaMovement());
        for (var p : shot) if (p.getDeltaMovement().lengthSqr() > 0) bad.add(p.getType() + " was moved: " + p.getDeltaMovement());
        PromisedEnd.LOGGER.info("[updraft_column_bounds] columns {} and {}; stands {}; sideways {}; crystal {}; shulker {}",
                ((com.panda.promisedend.block.UpdraftVentBlockEntity) level.getBlockEntity(crafted)).height(), ((com.panda.promisedend.block.UpdraftVentBlockEntity) level.getBlockEntity(ancient)).height(),
                stands.stream().map(st -> String.format(java.util.Locale.ROOT, "%.3f", st.getDeltaMovement().y)).toList(), v, crystal.getDeltaMovement(), shulker.getDeltaMovement());
        made.forEach(net.minecraft.world.entity.Entity::discard);
        level.setBlock(crafted, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(ancient, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        roof.forEach((pos, state) -> level.setBlock(pos, state, 3));
        if (!bad.isEmpty()) helper.fail("updraft bounds: " + bad);
        helper.succeed();
    }

    /**
     * The server's part for players, whose own client lifts them: each tick a server player in a vent's column has their
     * fall distance and the count that kicks a player for floating cleared; one 3 blocks off the column keeps both. The
     * integrated server of single player never counts floating, so the `updraft` scenario can't show this.
     */
    private static void updraftKeepsPlayerAloft(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        var state = ModBlocks.UPDRAFT_VENT.get().defaultBlockState();
        level.setBlock(at, state, 3);
        var vent = (com.panda.promisedend.block.UpdraftVentBlockEntity) level.getBlockEntity(at);
        var inside = helper.makeMockServerPlayerInLevel();
        var beside = helper.makeMockServerPlayerInLevel();
        inside.snapTo(at.getX() + 0.5, at.getY() + 3, at.getZ() + 0.5);
        beside.snapTo(at.getX() + 3.5, at.getY() + 3, at.getZ() + 0.5);
        List<String> bad = new ArrayList<>();
        try {
            var floating = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount");
            floating.setAccessible(true);
            for (var p : List.of(inside, beside)) {
                p.fallDistance = 10;
                floating.setInt(p.connection, 50);
            }
            com.panda.promisedend.block.UpdraftVentBlockEntity.tick(level, at, state, vent);
            int in = floating.getInt(inside.connection), out = floating.getInt(beside.connection);
            PromisedEnd.LOGGER.info("[updraft_keeps_player_aloft] in the column: floating {}, fall distance {}; beside it: {}, {}", in, inside.fallDistance, out, beside.fallDistance);
            if (in != 0 || inside.fallDistance != 0) bad.add("a player in the column kept floating " + in + " and fall distance " + inside.fallDistance);
            if (out != 50 || beside.fallDistance != 10) bad.add("a player beside the column had floating " + out + " and fall distance " + beside.fallDistance);
        } catch (ReflectiveOperationException e) {
            bad.add(e.toString());
        }
        level.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        if (!bad.isEmpty()) helper.fail("updraft for players: " + bad);
        helper.succeed();
    }

    /**
     * The vent feature, high in the test level's sky: on an island 11 across it sinks one ancient vent into the top,
     * within 2 of the edge, with the air open over it for the whole column; on a domed one whose top falls a block
     * every ring out from its middle, so no spot near its edge has a side at its own height (sides within a block
     * count), one too; from the middle of one 29 across, with no
     * edge in reach, nothing; on a lone pillar (no ground beside it), on obsidian (not vent ground) and in a chunk an
     * End City has claimed, nothing (the same island unclaimed gets one). Six more runs on the island 11 across add
     * vents, none within UpdraftVentFeature.SPACING of another.
     */
    private static void updraftVentOnEdges(GameTestHelper helper) {
        var level = helper.getLevel();
        var feature = grove(level, "updraft_vent");
        var generator = level.getChunkSource().getGenerator();
        var endStone = net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState();
        var vent = ModBlocks.UPDRAFT_VENT.get();
        List<String> bad = new ArrayList<>();
        int y = level.getMaxY() - 140;
        java.util.function.Function<BlockPos, List<BlockPos>> vents = centre -> {
            List<BlockPos> found = new ArrayList<>();
            for (BlockPos p : BlockPos.betweenClosed(centre.offset(-16, -12, -16), centre.offset(16, 3, 16))) if (level.getBlockState(p).is(vent)) found.add(p.immutable());
            return found;
        };
        // r: the island's half width; ground: what it is made of; then what the feature should do
        record Island(String name, int r, net.minecraft.world.level.block.state.BlockState ground, boolean vent) {}
        List<Island> islands = List.of(new Island("an island 11 across", 5, endStone, true), new Island("a wide island", 14, endStone, false),
                new Island("a lone pillar", 0, endStone, false), new Island("an obsidian island", 5, net.minecraft.world.level.block.Blocks.OBSIDIAN.defaultBlockState(), false),
                new Island("a domed island", 6, endStone, true));
        for (int n = 0; n < islands.size(); n++) {
            Island island = islands.get(n);
            BlockPos centre = new BlockPos(5200 + n * 64, y, 3600);
            boolean dome = island.name().startsWith("a domed");
            if (dome) {
                diamond(level, centre, island.r(), island.ground());
            } else {
                floor(level, centre, island.r(), island.ground());
                floor(level, centre.below(), island.r(), island.ground());
            }
            boolean placed = feature.place(level, generator, net.minecraft.util.RandomSource.create(1300 + n), centre);
            var found = vents.apply(centre);
            PromisedEnd.LOGGER.info("[updraft_vent_on_edges] {}: placed {}, vents {}", island.name(), placed, found);
            if (placed != island.vent() || found.size() != (island.vent() ? 1 : 0)) bad.add(island.name() + ": placed " + placed + ", vents " + found);
            for (BlockPos p : found) {
                if (!level.getBlockState(p).getValue(com.panda.promisedend.block.UpdraftVentBlock.ANCIENT)) bad.add("a crafted vent placed at " + p);
                int ax = Math.abs(p.getX() - centre.getX()), az = Math.abs(p.getZ() - centre.getZ());
                // the top under the spot: the flat islands' y - 1; on the dome, a block lower each ring past the middle's
                int top = dome ? y - 1 - Math.max(0, ax + az - DOME_FLAT) : y - 1;
                if (p.getY() != top && p.getY() != top + 1) bad.add("the vent isn't sunk into the top at " + p + " (the top is " + top + ")");
                // in from the edge: a ring for the square islands; on the dome's diamond, a diagonal step crosses two rings
                int inward = dome ? island.r() * 2 - (ax + az) : island.r() - Math.max(ax, az);
                if (inward > (dome ? 3 : 1)) bad.add("the vent is more than 2 in from the edge at " + p);
                if (dome) {
                    int level0 = 0;
                    for (var d : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                        BlockPos side = p.relative(d);
                        if (level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, side.getX(), side.getZ()) - 1 == top) level0++;
                    }
                    if (level0 >= 2) bad.add("the dome's spot at " + p + " has " + level0 + " sides at its own height, so it doesn't test the rule");
                }
                for (int dy = 1; dy <= com.panda.promisedend.logic.Updraft.HEIGHT_ANCIENT; dy++) if (!level.getBlockState(p.above(dy)).isAir()) bad.add("the air over the vent is shut at " + p.above(dy));
                if (!(level.getBlockEntity(p) instanceof com.panda.promisedend.block.UpdraftVentBlockEntity)) bad.add("the placed vent has no block entity at " + p);
            }
            if (n == 0) {
                // more runs on the same island: more vents round its edge, but never two within the spacing
                for (int k = 0; k < 6; k++) feature.place(level, generator, net.minecraft.util.RandomSource.create(net.minecraft.util.Mth.murmurHash3Mixer(1400 + k)), centre);
                var all = vents.apply(centre);
                int closest = Integer.MAX_VALUE;
                for (int i = 0; i < all.size(); i++) for (int j = i + 1; j < all.size(); j++) {
                    BlockPos a = all.get(i), b = all.get(j);
                    closest = Math.min(closest, Math.max(Math.abs(a.getX() - b.getX()), Math.max(Math.abs(a.getY() - b.getY()), Math.abs(a.getZ() - b.getZ()))));
                }
                PromisedEnd.LOGGER.info("[updraft_vent_on_edges] seven runs on {}: {} vents, the closest two {} apart", island.name(), all.size(), closest);
                if (all.size() < 2) bad.add("seven runs on " + island.name() + " placed " + all.size() + " vents, so the spacing isn't tested");
                else if (closest <= com.panda.promisedend.worldgen.UpdraftVentFeature.SPACING) bad.add("two vents " + closest + " apart on " + island.name());
            }
            clear(level, centre);
            if (dome) for (BlockPos q : BlockPos.betweenClosed(centre.offset(-12, -12, -12), centre.offset(12, -3, 12))) level.setBlock(q, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        // an End City's claim on the chunks under the island (it straddles two): nothing (the same island unclaimed gets a vent)
        {
            BlockPos centre = new BlockPos(5200, y, 3800);
            floor(level, centre, 5, endStone);
            floor(level, centre.below(), 5, endStone);
            var city = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.END_CITY);
            var before = new java.util.HashMap<net.minecraft.world.level.chunk.ChunkAccess, java.util.Map<net.minecraft.world.level.levelgen.structure.Structure, it.unimi.dsi.fastutil.longs.LongSet>>();
            for (int cx = (centre.getX() - 8) >> 4; cx <= (centre.getX() + 8) >> 4; cx++) {
                for (int cz = (centre.getZ() - 8) >> 4; cz <= (centre.getZ() + 8) >> 4; cz++) {
                    var chunk = level.getChunk(cx, cz);
                    before.put(chunk, new java.util.HashMap<>(chunk.getAllReferences()));
                    chunk.addReferenceForStructure(city, net.minecraft.world.level.ChunkPos.pack(centre.getX() >> 4, centre.getZ() >> 4));
                }
            }
            boolean claimed = feature.place(level, generator, net.minecraft.util.RandomSource.create(1300), centre);
            before.forEach(net.minecraft.world.level.chunk.ChunkAccess::setAllReferences);
            if (claimed || !vents.apply(centre).isEmpty()) bad.add("a vent in an End City's chunk");
            boolean free = feature.place(level, generator, net.minecraft.util.RandomSource.create(1300), centre);
            if (!free) bad.add("no vent on the unclaimed control island, so the claim case can't tell");
            clear(level, centre);
        }
        if (!bad.isEmpty()) helper.fail("updraft vents: " + bad);
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
     * spear's stabs count the same: four on one golem, the 4th 2 more with Slowness II. The armour's tooltip, read as
     * this player, ends "4 of 4 pieces on" in dark aqua, and "3 of 4" in grey with the boots off.
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
        // the tooltip's set count, as the client reads it for its own player
        java.util.function.BiConsumer<Integer, net.minecraft.ChatFormatting> worn = (pieces, colour) -> {
            var viewer = com.panda.promisedend.item.ResoniteArmourItem.viewer;
            com.panda.promisedend.item.ResoniteArmourItem.viewer = () -> player;
            List<net.minecraft.network.chat.Component> lines = new ArrayList<>();
            ModItems.RESONITE_HELMET.get().appendHoverText(new ItemStack(ModItems.RESONITE_HELMET.get()), net.minecraft.world.item.Item.TooltipContext.EMPTY,
                    net.minecraft.world.item.component.TooltipDisplay.DEFAULT, lines::add, net.minecraft.world.item.TooltipFlag.NORMAL);
            com.panda.promisedend.item.ResoniteArmourItem.viewer = viewer;
            var last = lines.isEmpty() ? null : lines.get(lines.size() - 1);
            int n = com.panda.promisedend.item.ResonanceSet.piecesWorn(player);
            if (n != pieces) bad.add(n + " pieces counted, not " + pieces);
            if (last == null || !(last.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t)
                    || !t.getKey().equals("tooltip.promisedend.resonite_armour.worn") || !java.util.Arrays.asList(t.getArgs()).equals(List.of(pieces, 4))
                    || !net.minecraft.network.chat.TextColor.fromLegacyFormat(colour).equals(last.getStyle().getColor())) {
                bad.add("with " + pieces + " pieces on the tooltip ends " + (last == null ? "with nothing" : last.getString() + " " + last.getStyle().getColor()));
            }
        };
        worn.accept(4, net.minecraft.ChatFormatting.DARK_AQUA);
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
        worn.accept(3, net.minecraft.ChatFormatting.GRAY);
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

    /**
     * Landing on a vent is never a fall: its block takes the whole fall distance (a landing's last step comes in the
     * entity's move, before the column's tick clears it). A pig landing from 6.3 on end stone beside it is hurt.
     */
    private static void updraftLandingSafe(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        BlockPos vent = helper.absolutePos(new BlockPos(3, 1, 3)), stone = helper.absolutePos(new BlockPos(7, 1, 3));
        level.setBlock(vent, ModBlocks.UPDRAFT_VENT.get().defaultBlockState(), 3);
        level.setBlock(stone, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 3);
        for (BlockPos at : List.of(vent, stone)) {
            var player = net.minecraft.world.entity.EntityTypes.PIG.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            level.addFreshEntity(player);
            player.snapTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 0, 0);
            float before = player.getHealth();
            level.getBlockState(at).getBlock().fallOn(level, level.getBlockState(at), at, player, 6.3);
            float lost = before - player.getHealth();
            PromisedEnd.LOGGER.info("[updraft_landing_safe] a fall of 6.3 onto {}: lost {}", level.getBlockState(at).getBlock(), lost);
            if (at.equals(vent) && lost > 0) bad.add("landing on a vent hurt " + lost);
            if (at.equals(stone) && lost <= 0) bad.add("landing on end stone didn't hurt, so this can't tell");
            player.discard();
        }
        level.setBlock(vent, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(stone, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        if (!bad.isEmpty()) helper.fail("updraft landing: " + bad);
        helper.succeed();
    }

    /**
     * Opens the arena's barrier roof over {@code vent}, {@code up} blocks, keeping what it took in {@code roof} to be put
     * back; anything there but a barrier is a fault in the test.
     */
    private static void openSky(net.minecraft.server.level.ServerLevel level, BlockPos vent, int up, java.util.Map<BlockPos, net.minecraft.world.level.block.state.BlockState> roof, List<String> bad) {
        for (int dy = 1; dy <= up; dy++) {
            var over = level.getBlockState(vent.above(dy));
            if (over.isAir()) continue;
            if (!over.is(net.minecraft.world.level.block.Blocks.BARRIER)) bad.add("over a vent at " + dy + ": " + over);
            roof.put(vent.above(dy), over);
            level.setBlock(vent.above(dy), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
    }

    /** The dome's middle is flat out to this many rings. */
    private static final int DOME_FLAT = 4;

    /**
     * A domed island: a diamond {@code r * 2} rings out, two blocks thick, its top at y - 1 out to ring 4 and a block
     * lower each ring after, so near its edge every column has its neighbours a block over and under it.
     */
    private static void diamond(net.minecraft.server.level.ServerLevel level, BlockPos centre, int r, net.minecraft.world.level.block.state.BlockState state) {
        int reach = r * 2;
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                int ring = Math.abs(dx) + Math.abs(dz);
                if (ring > reach) continue;
                int top = centre.getY() - 1 - Math.max(0, ring - DOME_FLAT);
                level.setBlock(centre.offset(dx, top - centre.getY(), dz), state, 2);
                level.setBlock(centre.offset(dx, top - centre.getY() - 1, dz), state, 2);
            }
        }
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
     * (chorus wood above their foot, bark on every side), and chorus plants grown from the timber well above the ground
     * (the canopy); no lying log anywhere (its end grain would face out).
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
                } else if (axis != null && axis != net.minecraft.core.Direction.Axis.Y) {
                    bad.add("a lying log at " + p);
                } else if (state.is(ModBlocks.CHORUS_WOOD.get()) && p.getY() > y + 1) {
                    limbs++;
                } else if ((state.is(net.minecraft.world.level.block.Blocks.CHORUS_PLANT) || state.is(net.minecraft.world.level.block.Blocks.CHORUS_FLOWER)) && p.getY() >= y + 6) {
                    canopy++;
                }
            }
            // upright timber at the foot is a trunk (the roots there are wood)
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
     * (obsidian here; chorus timber too, so no tree stands on a root), under a roof 4 blocks up, the terrain-off copy
     * while our terrain is on, and where an End City has claimed a chunk next door the grove could reach (the same
     * floor and seed grow one unclaimed).
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
        // an End City's claim on the chunk east of the floor's, which the canopy could reach: no tree at all
        // 10 blocks from the chunk's east edge, inside the grove's reach of 12
        BlockPos city = new BlockPos(3270, y, 2000);
        floor(level, city, 12, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
        {
            var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.END_CITY);
            var chunk = level.getChunk(city.offset(16, 0, 0));
            var before = new java.util.HashMap<>(chunk.getAllReferences());
            chunk.addReferenceForStructure(structure, net.minecraft.world.level.ChunkPos.pack((city.getX() + 16) >> 4, city.getZ() >> 4));
            boolean claimed = feature.place(level, generator, net.minecraft.util.RandomSource.create(9), city);
            chunk.setAllReferences(before);
            int timberThere = 0;
            for (BlockPos p : BlockPos.betweenClosed(city.offset(-12, 0, -12), city.offset(12, 16, 12))) if (level.getBlockState(p).is(log)) timberThere++;
            if (claimed || timberThere > 0) bad.add(timberThere + " timber beside an End City's chunk");
            if (!feature.place(level, generator, net.minecraft.util.RandomSource.create(9), city)) bad.add("no grove on the unclaimed control floor, so the claim case can't tell");
        }
        for (BlockPos at : List.of(obsidian, timber, roofed, off, city)) clear(level, at);
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

    /**
     * Every block of ours drops something when mined with the right tool (its loot table rolls a stack): a netherite
     * pickaxe, shears for the grass, leaves and vine that shears keep, as vanilla's, Silk Touch for the huge mushroom's blocks,
     * and a Void Berry Vine while it bears.
     */
    private static void everyBlockDrops(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        for (var h : ModBlocks.BLOCKS.getEntries()) {
            var block = h.get();
            var key = block.getLootTable();
            if (key.isEmpty()) continue; // noLootTable blocks (boss arena pieces) drop nothing on purpose
            var table = level.getServer().reloadableRegistries().getLootTable(key.get());
            var tool = new net.minecraft.world.item.ItemStack(block == ModBlocks.VOID_GRASS.get() || block == ModBlocks.TALL_VOID_GRASS.get()
                    || block == ModBlocks.CHORUS_LEAVES.get() || block == ModBlocks.CHORUS_VINE.get() ? net.minecraft.world.item.Items.SHEARS : net.minecraft.world.item.Items.NETHERITE_PICKAXE);
            // the huge mushroom's blocks keep themselves only to Silk Touch, as vanilla's (a cap's mushrooms are 0-2)
            if (block == ModBlocks.VOID_MUSHROOM_BLOCK.get() || block == ModBlocks.VOID_MUSHROOM_STEM.get())
                tool.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, tool)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, block == ModBlocks.VOID_BERRY_VINE.get()
                            ? block.defaultBlockState().setValue(com.panda.promisedend.block.VoidBerryVineBlock.BERRIES, true) : block.defaultBlockState())
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

    /**
     * Each outer biome's visit advancement goes to a player standing in that biome, through the game's own location
     * check, and to no one standing elsewhere: a player on end highlands has none of them; in each biome in turn, that
     * one more, and only that one. Each has a display and sits under {@code outer/enter_outer}, itself under
     * {@code rim/root}; {@code OuterProgress.BIOMES} is exactly the biomes with one. A player in the End 1100 blocks out
     * gets {@code outer/enter_outer}, one 500 out doesn't, nor one 1100 out in another level. With our terrain off, what
     * each biome's terrain-off feature builds is in its marker tag, and a player out there 4 blocks from it gets that
     * visit and no other, 5 blocks off none; with terrain on the same spot gives none (the biome criterion is the way
     * then).
     */
    private static void biomeVisitsAward(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        var player = helper.makeMockServerPlayerInLevel();
        List<String> bad = new ArrayList<>();
        List<String> biomes = com.panda.promisedend.logic.OuterProgress.BIOMES;
        var withVisits = server.getAdvancements().getAllAdvancements().stream().map(h -> h.id())
                .filter(id -> id.getNamespace().equals(PromisedEnd.MODID) && id.getPath().startsWith("outer/visit_"))
                .map(id -> id.getPath().substring("outer/visit_".length())).sorted().toList();
        if (!withVisits.equals(biomes.stream().sorted().toList())) bad.add("OuterProgress.BIOMES " + biomes + " but visit advancements for " + withVisits);
        List<net.minecraft.advancements.AdvancementHolder> visits = new ArrayList<>();
        for (String biome : biomes) {
            var adv = server.getAdvancements().get(PromisedEnd.id("outer/visit_" + biome));
            if (adv == null) {
                bad.add("no advancement outer/visit_" + biome);
                continue;
            }
            visits.add(adv);
            if (adv.value().display().isEmpty()) bad.add(adv.id() + " has no display, so the client never sees it");
            if (!adv.value().parent().equals(java.util.Optional.of(com.panda.promisedend.logic.OuterProgress.ENTER_OUTER))) bad.add(adv.id() + " isn't under outer/enter_outer: " + adv.value().parent());
        }
        var enter = server.getAdvancements().get(com.panda.promisedend.logic.OuterProgress.ENTER_OUTER);
        if (enter == null) bad.add("no advancement outer/enter_outer");
        else if (!enter.value().parent().equals(java.util.Optional.of(PromisedEnd.id("rim/root")))) bad.add("outer/enter_outer isn't under rim/root: " + enter.value().parent());
        if (visits.size() == biomes.size()) {
            java.util.function.Function<BlockPos, List<String>> standAt = at -> {
                player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
                net.minecraft.advancements.triggers.CriteriaTriggers.LOCATION.trigger(player);
                List<String> done = new ArrayList<>();
                for (var adv : visits) if (player.getAdvancements().getOrStartProgress(adv).isDone()) done.add(adv.id().getPath());
                return done;
            };
            BlockPos highlands = new BlockPos(5000, 120, 5000);
            fillBiome(level, highlands, "minecraft:end_highlands");
            if (!level.getBiome(highlands).is(net.minecraft.world.level.biome.Biomes.END_HIGHLANDS)) bad.add("fillbiome didn't set end_highlands at " + highlands);
            List<String> none = standAt.apply(highlands);
            if (!none.isEmpty()) bad.add("on end highlands: " + none);
            for (int i = 0; i < biomes.size(); i++) {
                BlockPos at = new BlockPos(5064 + 64 * i, 120, 5000);
                fillBiome(level, at, "promisedend:" + biomes.get(i));
                if (!level.getBiome(at).is(ResourceKey.create(Registries.BIOME, PromisedEnd.id(biomes.get(i))))) {
                    bad.add("fillbiome didn't set " + biomes.get(i) + " at " + at);
                    continue;
                }
                List<String> done = standAt.apply(at);
                if (!done.equals(biomes.subList(0, i + 1).stream().map(b -> "outer/visit_" + b).toList())) bad.add("after standing in " + biomes.get(i) + ": " + done);
            }
        }
        var end = server.getLevel(net.minecraft.world.level.Level.END);
        if (end == null) bad.add("no End level on the test server");
        else if (enter != null && visits.size() == biomes.size()) {
            List<net.minecraft.server.level.ServerPlayer> made = new ArrayList<>();
            java.util.function.Supplier<net.minecraft.server.level.ServerPlayer> fresh = () -> {
                var p = helper.makeMockServerPlayerInLevel();
                made.add(p);
                return p;
            };
            // one tick of the check, at a spot in a level; what of ours the player then has
            java.util.function.BiFunction<net.minecraft.server.level.ServerPlayer, net.minecraft.world.level.portal.TeleportTransition, List<String>> tickAt = (p, to) -> {
                to.newLevel().getChunk(BlockPos.containing(to.position()));
                p.teleport(to);
                p.tickCount = com.panda.promisedend.logic.OuterProgress.MARKER_INTERVAL * 7;
                com.panda.promisedend.logic.OuterProgress.tick(p);
                List<String> done = new ArrayList<>();
                for (var adv : visits) if (p.getAdvancements().getOrStartProgress(adv).isDone()) done.add(adv.id().getPath());
                if (p.getAdvancements().getOrStartProgress(enter).isDone()) done.add(enter.id().getPath());
                return done;
            };
            java.util.function.BiFunction<net.minecraft.server.level.ServerLevel, BlockPos, net.minecraft.world.level.portal.TeleportTransition> to = (l, at) ->
                    new net.minecraft.world.level.portal.TeleportTransition(l, net.minecraft.world.phys.Vec3.atBottomCenterOf(at),
                            net.minecraft.world.phys.Vec3.ZERO, 0, 0, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING);
            // by where they stand: 500 out in the End isn't the outer End, 1100 out is, 1100 out in another level isn't
            var traveller = fresh.get();
            List<String> near = tickAt.apply(traveller, to.apply(end, new BlockPos(500, 200, 0)));
            if (!near.isEmpty()) bad.add("500 blocks out: " + near);
            List<String> far = tickAt.apply(traveller, to.apply(end, new BlockPos(1100, 200, 0)));
            if (!far.equals(List.of("outer/enter_outer"))) bad.add("1100 blocks out, with terrain on: " + far);
            List<String> elsewhere = tickAt.apply(fresh.get(), to.apply(level, new BlockPos(1100, 200, 0)));
            if (!elsewhere.isEmpty()) bad.add("1100 blocks out in the test level: " + elsewhere);
            // Seven Shores, through the outer End's own check: each of the seven marked off once its visit is done, done
            // with the seventh; the Wilds' four don't count
            var all = server.getAdvancements().get(com.panda.promisedend.logic.OuterProgress.ALL_SEVEN);
            if (all == null) bad.add("no advancement " + com.panda.promisedend.logic.OuterProgress.ALL_SEVEN);
            else {
                if (!all.value().parent().equals(java.util.Optional.of(com.panda.promisedend.logic.OuterProgress.ENTER_OUTER))) bad.add("all_seven isn't under outer/enter_outer");
                if (all.value().requirements().size() != com.panda.promisedend.logic.OuterProgress.SEVEN.size()) bad.add("all_seven needs " + all.value().requirements().size() + " things, not the seven");
                var seven = fresh.get();
                for (int i = 0; i < com.panda.promisedend.logic.OuterProgress.SEVEN.size(); i++) {
                    var visit = server.getAdvancements().get(com.panda.promisedend.logic.OuterProgress.visit(com.panda.promisedend.logic.OuterProgress.SEVEN.get(i)));
                    if (visit == null) continue;
                    seven.getAdvancements().award(visit, "visited");
                    tickAt.apply(seven, to.apply(end, new BlockPos(1100, 200, 0)));
                    boolean finished = seven.getAdvancements().getOrStartProgress(all).isDone(), last = i == com.panda.promisedend.logic.OuterProgress.SEVEN.size() - 1;
                    if (finished != last) bad.add("all_seven " + (finished ? "done" : "not done") + " after " + (i + 1) + " of the seven");
                }
                // the Wilds' four don't count towards it
                var wild = fresh.get();
                for (String biome : List.of("bloom_meadows", "chorus_canopy", "void_fungus_barrens", "hanging_gardens")) {
                    var visit = server.getAdvancements().get(com.panda.promisedend.logic.OuterProgress.visit(biome));
                    if (visit != null) wild.getAdvancements().award(visit, "visited");
                }
                tickAt.apply(wild, to.apply(end, new BlockPos(1100, 200, 0)));
                if (wild.getAdvancements().getOrStartProgress(all).getCompletedCriteria().iterator().hasNext()) bad.add("the Wilds' visits counted towards all_seven");
            }
            // with terrain off, each biome's marker is what its terrain-off feature builds: in its tag, and it earns that
            // visit 4 blocks off (the check's reach) and not 5; with terrain on the same spot earns none
            var random = net.minecraft.util.RandomSource.create(1);
            var grove = (com.panda.promisedend.worldgen.ChorusGroveFeature) grove(level, "chorus_grove_fallback");
            var pool = (com.panda.promisedend.worldgen.MirePoolFeature) grove(level, "mire_pool_fallback");
            var spire = (com.panda.promisedend.worldgen.ResoniteSpireFeature) grove(level, "resonite_spire_fallback");
            var blossom = (net.minecraft.world.level.levelgen.feature.SimpleBlockFeature) grove(level, "drift_blossom_fallback");
            var scar = (com.panda.promisedend.worldgen.RiftScarFeature) grove(level, "rift_scar_fallback");
            var reef = (com.panda.promisedend.worldgen.PurpurReefFeature) grove(level, "purpur_reef_fallback");
            var cavern = (com.panda.promisedend.worldgen.HollowCavernFeature) grove(level, "hollow_cavern_fallback");
            var bloom = (net.minecraft.world.level.levelgen.feature.SimpleBlockFeature) grove(level, "ender_bloom_fallback");
            List<net.minecraft.world.level.block.state.BlockState> built = List.of(grove.trunk().getState(level, random, BlockPos.ZERO),
                    pool.water().getState(level, random, BlockPos.ZERO), spire.budding().getState(level, random, BlockPos.ZERO),
                    blossom.toPlace().value().getState(level, random, BlockPos.ZERO), scar.ore().getState(level, random, BlockPos.ZERO),
                    reef.coral().getState(level, random, BlockPos.ZERO), cavern.cluster().getState(level, random, BlockPos.ZERO),
                    bloom.toPlace().value().getState(level, random, BlockPos.ZERO),
                    // the rest of the Wilds: the canopy's leaves (a tree feature's), the barrens' mycelium (a patch's ground), the gardens' vine
                    ModBlocks.CHORUS_LEAVES.get().defaultBlockState(), ModBlocks.VOID_MYCELIUM.get().defaultBlockState(),
                    ((com.panda.promisedend.worldgen.HangingVineFeature) grove(level, "void_berry_vines_fallback")).vine().defaultBlockState());
            var previous = com.panda.promisedend.EndConfig.current();
            for (int i = 0; i < biomes.size(); i++) {
                String biome = biomes.get(i);
                var state = built.get(i);
                if (!state.is(com.panda.promisedend.logic.OuterProgress.marker(biome))) bad.add(state.getBlock() + ", which the " + biome + "'s terrain-off feature builds, isn't in its visit marker tag");
                BlockPos spot = new BlockPos(1200 + 32 * i, 200, 0);
                // the chunks the check's cube touches (it reads none that isn't loaded)
                for (int cx = (spot.getX() - 8) >> 4; cx <= (spot.getX() + 8) >> 4; cx++) {
                    for (int cz = (spot.getZ() - 8) >> 4; cz <= (spot.getZ() + 8) >> 4; cz++) end.getChunk(cx, cz);
                }
                try {
                    end.setBlock(spot.east(5), state, 2);
                    com.panda.promisedend.EndConfig.use(new com.panda.promisedend.EndConfig.Source() {
                        @Override public boolean terrainEnabled() { return false; }
                        @Override public int edgeDistance() { return previous.edgeDistance(); }
                        @Override public int anchorTriggerDepth() { return previous.anchorTriggerDepth(); }
                        @Override public boolean dragonPhase3() { return previous.dragonPhase3(); }
                        @Override public boolean dragonWrath() { return previous.dragonWrath(); }
                        @Override public int maxWrath() { return previous.maxWrath(); }
                    });
                    var offPlayer = fresh.get();
                    List<String> five = tickAt.apply(offPlayer, to.apply(end, spot));
                    if (!five.equals(List.of("outer/enter_outer"))) bad.add("5 blocks from " + state.getBlock() + " with terrain off: " + five);
                    end.setBlock(spot.east(5), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                    end.setBlock(spot.east(4), state, 2);
                    List<String> four = tickAt.apply(offPlayer, to.apply(end, spot));
                    if (!four.equals(List.of("outer/visit_" + biome, "outer/enter_outer"))) bad.add("4 blocks from " + state.getBlock() + " with terrain off: " + four);
                    com.panda.promisedend.EndConfig.use(previous);
                    List<String> on = tickAt.apply(fresh.get(), to.apply(end, spot));
                    if (!on.equals(List.of("outer/enter_outer"))) bad.add("4 blocks from " + state.getBlock() + " with terrain on: " + on);
                } finally {
                    com.panda.promisedend.EndConfig.use(previous);
                    end.setBlock(spot.east(4), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                    end.setBlock(spot.east(5), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                }
            }
            made.forEach(p -> p.discard());
        }
        if (!bad.isEmpty()) helper.fail("biome visits: " + bad);
        helper.succeed();
    }

    /** Sets the biome of the 17-block cube round {@code at}, as {@code /fillbiome} does (which needs the chunks loaded). */
    /**
     * Where the constructs may spawn by themselves. With the terrain on, each keeps to its own land: on the vanilla
     * biome its land falls back to (the Weaver's highlands, the Stalker's barrens, the Wisp's midlands) it is turned
     * away, and in its own land it isn't. With the terrain off it is let in there. The Wisp's whole spawn rule is checked
     * too (the other two also need a dark, hard enough world, which the test server's difficulty decides). Each one's
     * local cap: with one fewer than the cap within 32 there is room, with the cap there isn't; the Wisp's whole rule
     * (on the arena's floor) turns one away at its cap.
     */
    private static void constructSpawnsKeepToTheirLands(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        record Gate(String name, String own, String fallback, net.minecraft.tags.TagKey<net.minecraft.world.level.biome.Biome> tag) {}
        List<Gate> gates = List.of(
                new Gate("chorus_weaver", "promisedend:chorus_thicket", "minecraft:end_highlands", com.panda.promisedend.registry.ModTags.GROVE_FALLBACK),
                new Gate("rift_stalker", "promisedend:voidscar_wastes", "minecraft:end_barrens", com.panda.promisedend.registry.ModTags.VOIDSCAR_FALLBACK),
                new Gate("lumen_wisp", "promisedend:lumen_mire", "minecraft:end_midlands", com.panda.promisedend.registry.ModTags.MIRE_FALLBACK));
        // each construct's fallback spawn is the same land its spawn rule lets in with the terrain off
        for (Gate gate : gates) {
            var spawn = com.panda.promisedend.entity.Constructs.FALLBACK_SPAWNS.stream()
                    .filter(f -> net.minecraft.world.entity.EntityType.getKey(f.type().get()).getPath().equals(gate.name())).findFirst();
            if (spawn.isEmpty()) bad.add("no terrain-off fallback spawn for " + gate.name());
            else if (!spawn.get().biomes().equals(gate.tag())) bad.add(gate.name() + "'s fallback spawn is in " + spawn.get().biomes().location() + ", not " + gate.tag().location());
        }
        var previous = com.panda.promisedend.EndConfig.current();
        try {
            for (int i = 0; i < gates.size(); i++) {
                Gate gate = gates.get(i);
                BlockPos own = new BlockPos(7000 + 64 * i, 120, 7000), fallback = new BlockPos(7000 + 64 * i, 120, 7064);
                fillBiome(level, own, gate.own());
                fillBiome(level, fallback, gate.fallback());
                for (BlockPos at : List.of(own, fallback)) level.setBlockAndUpdate(at.below(), net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
                if (!level.getBiome(fallback).is(gate.tag())) {
                    bad.add(gate.fallback() + " isn't in " + gate.tag().location() + " (or fillbiome didn't take)");
                    continue;
                }
                for (boolean terrain : new boolean[] {true, false}) {
                    com.panda.promisedend.EndConfig.use(terrain ? previous : terrainOff(previous));
                    boolean inOwn = com.panda.promisedend.entity.Constructs.terrainAllows(level, own, gate.tag());
                    boolean inFallback = com.panda.promisedend.entity.Constructs.terrainAllows(level, fallback, gate.tag());
                    PromisedEnd.LOGGER.info("[construct_spawns_keep_to_their_lands] {} with the terrain {}: own land {}, {} {}", gate.name(), terrain ? "on" : "off", inOwn, gate.fallback(), inFallback);
                    if (!inOwn) bad.add(gate.name() + " turned away from its own land with the terrain " + (terrain ? "on" : "off"));
                    if (inFallback == terrain) bad.add(gate.name() + (terrain ? " let in on " : " turned away from ") + gate.fallback() + " with the terrain " + (terrain ? "on" : "off"));
                    if (gate.name().equals("lumen_wisp")) {
                        var type = com.panda.promisedend.registry.ModEntities.LUMEN_WISP.get();
                        boolean wOwn = com.panda.promisedend.entity.LumenWisp.canSpawn(type, level, net.minecraft.world.entity.EntitySpawnReason.NATURAL, own, level.getRandom());
                        boolean wFallback = com.panda.promisedend.entity.LumenWisp.canSpawn(type, level, net.minecraft.world.entity.EntitySpawnReason.NATURAL, fallback, level.getRandom());
                        if (!wOwn) bad.add("a wisp may not spawn in the mire with the terrain " + (terrain ? "on" : "off"));
                        if (wFallback == terrain) bad.add("a wisp " + (terrain ? "may" : "may not") + " spawn on the midlands with the terrain " + (terrain ? "on" : "off"));
                    }
                }
            }
        } finally {
            com.panda.promisedend.EndConfig.use(previous);
        }
        // the local caps, in the arena once its chunk ticks entities (entities added before then aren't found)
        record Cap(net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.Mob> type, Class<? extends net.minecraft.world.entity.Entity> kind, int cap) {}
        List<Cap> caps = List.of(new Cap(com.panda.promisedend.registry.ModEntities.CHORUS_WEAVER.get(), com.panda.promisedend.entity.ChorusWeaver.class, com.panda.promisedend.entity.ChorusWeaver.LOCAL_CAP),
                new Cap(com.panda.promisedend.registry.ModEntities.RIFT_STALKER.get(), com.panda.promisedend.entity.RiftStalker.class, com.panda.promisedend.entity.RiftStalker.LOCAL_CAP),
                new Cap(com.panda.promisedend.registry.ModEntities.LUMEN_WISP.get(), com.panda.promisedend.entity.LumenWisp.class, com.panda.promisedend.entity.LumenWisp.LOCAL_CAP));
        BlockPos spot = helper.absolutePos(new BlockPos(2, 1, 2));
        boolean[] done = {false};
        for (int t = 1; t <= 80; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (done[0]) return;
                if (!level.isPositionEntityTicking(spot)) {
                    if (tick == 80) helper.fail("the arena's chunk wasn't entity-ticking by tick 80");
                    return;
                }
                done[0] = true;
                for (Cap c : caps) {
                    String name = net.minecraft.world.entity.EntityType.getKey(c.type()).getPath();
                    List<net.minecraft.world.entity.Entity> made = new ArrayList<>();
                    for (int k = 0; k < c.cap(); k++) {
                        if (k == c.cap() - 1 && !com.panda.promisedend.entity.Constructs.roomFor(level, spot, c.kind(), c.cap())) bad.add("no room for a " + name + " with " + k + " near");
                        var e = helper.spawn(c.type(), new BlockPos(1 + 2 * k, 1, 4));
                        e.setNoAi(true);
                        made.add(e);
                    }
                    if (com.panda.promisedend.entity.Constructs.roomFor(level, spot, c.kind(), c.cap())) bad.add("room for a " + name + " with " + c.cap() + " within 32");
                    if (c.kind() == com.panda.promisedend.entity.LumenWisp.class && com.panda.promisedend.entity.LumenWisp.canSpawn(com.panda.promisedend.registry.ModEntities.LUMEN_WISP.get(),
                            level, net.minecraft.world.entity.EntitySpawnReason.NATURAL, spot, level.getRandom())) bad.add("a wisp may spawn with " + c.cap() + " within 32");
                    made.forEach(net.minecraft.world.entity.Entity::discard);
                }
                if (!bad.isEmpty()) helper.fail("construct spawns: " + bad);
                else helper.succeed();
            });
        }
    }

    /** {@code previous} with the terrain off. */
    private static com.panda.promisedend.EndConfig.Source terrainOff(com.panda.promisedend.EndConfig.Source previous) {
        return new com.panda.promisedend.EndConfig.Source() {
            @Override public boolean terrainEnabled() { return false; }
            @Override public int edgeDistance() { return previous.edgeDistance(); }
            @Override public int anchorTriggerDepth() { return previous.anchorTriggerDepth(); }
            @Override public boolean dragonPhase3() { return previous.dragonPhase3(); }
            @Override public boolean dragonWrath() { return previous.dragonWrath(); }
            @Override public int maxWrath() { return previous.maxWrath(); }
        };
    }

    /**
     * An End portal in the Overworld at (1503, 64, -287) sets a player down in the End in the portal's direction, its
     * distance held in the band (1100-2500; the portal is 1530 out), within the search's reach of that point, on the
     * floor of a Waystation whose Return Gate is in its middle, facing the centre. The same portal again: the same
     * place, no second Waystation. The first arrival gives the Promise Codex, the second doesn't. (The test server's End
     * is a flat floor, so this is the no-ground path, an islet; real ground is the server check's.)
     */
    private static void arrivalByCoordinates(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        var end = server.getLevel(net.minecraft.world.level.Level.END);
        List<String> bad = new ArrayList<>();
        if (end == null) {
            helper.fail("no End level");
            return;
        }
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        com.panda.promisedend.logic.Arrivals.forget(server, player.getUUID());
        BlockPos portal = new BlockPos(1503, 64, -287);
        int before = com.panda.promisedend.logic.Arrivals.built(server).size();
        var first = com.panda.promisedend.logic.Arrivals.destination(level, player, portal);
        if (first == null) {
            helper.fail("no arrival for a portal in the Overworld");
            return;
        }
        if (first.newLevel() != end) bad.add("the arrival isn't in the End: " + first.newLevel().dimension());
        var at = first.position();
        double out = Math.hypot(at.x, at.z), angle = Math.atan2(at.z, at.x), want = Math.atan2(-287.5, 1503.5);
        PromisedEnd.LOGGER.info("[arrival_by_coordinates] set down at {} ({} out, {} rad; the portal's {} rad)", at, Math.round(out), String.format("%.3f", angle), String.format("%.3f", want));
        if (out < com.panda.promisedend.logic.Arrival.NEAREST - com.panda.promisedend.logic.Arrival.SEARCH || out > com.panda.promisedend.logic.Arrival.FARTHEST + com.panda.promisedend.logic.Arrival.SEARCH) bad.add("set down " + out + " out");
        double[] point = com.panda.promisedend.logic.Arrival.target(1503.5, -287.5);
        if (Math.hypot(at.x - point[0], at.z - point[1]) > com.panda.promisedend.logic.Arrival.SEARCH + 8) bad.add("set down " + Math.hypot(at.x - point[0], at.z - point[1]) + " from the portal's point");
        var built = com.panda.promisedend.logic.Arrivals.built(server);
        if (built.size() != before + 1) bad.add("the first arrival built " + (built.size() - before) + " Waystations");
        else {
            BlockPos gate = built.getLast();
            if (!end.getBlockState(gate).is(ModBlocks.RETURN_GATE.get())) bad.add("no Return Gate in the Waystation's middle: " + end.getBlockState(gate));
            if (!com.panda.promisedend.logic.Arrivals.stand(gate).equals(at)) bad.add("not set down on the Waystation's floor: " + at + ", the gate " + gate);
            if (!end.getBlockState(BlockPos.containing(at).below()).isFaceSturdy(end, BlockPos.containing(at).below(), net.minecraft.core.Direction.UP)) bad.add("nothing to stand on under " + at);
            if (!end.getBlockState(BlockPos.containing(at)).isAir() || !end.getBlockState(BlockPos.containing(at).above()).isAir()) bad.add("no room to stand at " + at);
            int chests = 0;
            for (BlockPos q : BlockPos.betweenClosed(gate.offset(-6, 0, -6), gate.offset(6, 3, 6))) if (end.getBlockState(q).is(net.minecraft.world.level.block.Blocks.CHEST)) chests++;
            if (chests != 1) bad.add(chests + " chests at the Waystation");
        }
        float yaw = net.minecraft.util.Mth.wrapDegrees(first.yRot()), face = com.panda.promisedend.logic.Arrival.facingCentre(at.x, at.z);
        if (Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - face)) > 1) bad.add("facing " + yaw + ", not the centre (" + face + ")");
        var again = com.panda.promisedend.logic.Arrivals.destination(level, player, portal);
        if (again == null || !again.position().equals(at)) bad.add("the same portal again set down at " + (again == null ? null : again.position()));
        if (com.panda.promisedend.logic.Arrivals.built(server).size() != before + 1) bad.add("the same portal again built another Waystation");
        // the greeting, as the teleport runs it after the move
        first.postTeleportTransition().onTransition(player);
        again.postTeleportTransition().onTransition(player);
        int codices = player.getInventory().countItem(ModItems.PROMISE_CODEX.get());
        if (codices != 1) bad.add(codices + " Promise Codices after two arrivals");
        // what it says, where it set them down: the Waystation, the way in with the Ward up, the pearl
        player.snapTo(at.x, at.y, at.z, 0, 0);
        var said = com.panda.promisedend.logic.Arrivals.greeting(player).stream()
                .map(c -> c.getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t ? t.getKey() : c.getString()).toList();
        PromisedEnd.LOGGER.info("[arrival_by_coordinates] the greeting: {}", said);
        // the pearl leads to the Foundry while its seal binds the Ward, to the heart after
        boolean stands = com.panda.promisedend.logic.AncientWard.stands(server);
        boolean foundry = stands && (com.panda.promisedend.logic.AncientWard.broken(server) & 1) == 0;
        String pearlLine = foundry ? "arrival.promisedend.pearl.foundry" : "arrival.promisedend.pearl";
        var lines = stands ? List.of("arrival.promisedend.first", "arrival.promisedend.ward", pearlLine)
                : List.of("arrival.promisedend.first", "arrival.promisedend.heart", pearlLine);
        if (!said.equals(lines)) bad.add("the greeting said " + said);
        player.discard();
        if (!bad.isEmpty()) helper.fail("arrival: " + bad);
        helper.succeed();
    }

    /**
     * A real player on the server's list, in {@code mode}: the test helper's mock server player always reports
     * creative, which the Ward lets by. Take it off the list ({@code getPlayerList().remove}) when the test ends.
     */
    /** Each real player's channel, whose outbound side holds the packets the server sent them. */
    private static final java.util.Map<java.util.UUID, io.netty.channel.embedded.EmbeddedChannel> CHANNELS = new java.util.concurrent.ConcurrentHashMap<>();

    /** The packets of {@code type} the server has sent a real player since the last call (the rest are dropped). */
    private static <T> List<T> sent(net.minecraft.server.level.ServerPlayer player, Class<T> type) {
        List<T> out = new ArrayList<>();
        var channel = CHANNELS.get(player.getUUID());
        if (channel == null) return out;
        for (Object m; (m = channel.readOutbound()) != null; ) {
            if (type.isInstance(m)) out.add(type.cast(m));
            else if (m instanceof net.minecraft.network.protocol.BundlePacket<?> bundle) {
                for (var inner : bundle.subPackets()) if (type.isInstance(inner)) out.add(type.cast(inner));
            }
        }
        return out;
    }

    static net.minecraft.server.level.ServerPlayer realPlayer(GameTestHelper helper, net.minecraft.world.level.GameType mode) {
        var level = helper.getLevel();
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-walker"), false);
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        CHANNELS.put(player.getUUID(), new io.netty.channel.embedded.EmbeddedChannel(connection));
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(mode);
        return player;
    }

    /**
     * The Ancients' Ward, with every seal mended: a survival player who steps from 3 outside it to 2 inside, on one
     * tick, is set back where they were; a creative or spectating player crossing isn't; a survival player found 100
     * inside is sent home (out of the End); a player riding a pig across is set back with the pig, still riding. With
     * every seal broken the same step is let through. The seals are left mended.
     */
    private static void wardSetsBack(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var end = server.getLevel(net.minecraft.world.level.Level.END);
        List<String> bad = new ArrayList<>();
        if (end == null) {
            helper.fail("no End level");
            return;
        }
        com.panda.promisedend.logic.AncientWard.reset(server);
        int r = com.panda.promisedend.EndConfig.wardRadius();
        List<net.minecraft.server.level.ServerPlayer> players = new ArrayList<>();
        java.util.function.Function<net.minecraft.world.level.GameType, net.minecraft.server.level.ServerPlayer> player = mode -> {
            var p = realPlayer(helper, mode);
            players.add(p);
            return p;
        };
        // a step across: placed outside, a tick of the Ward, placed inside, another tick; where it ends up
        java.util.function.BiFunction<net.minecraft.server.level.ServerPlayer, Double, Double> cross = (p, from) -> {
            wardAt(end, p, from);
            com.panda.promisedend.logic.AncientWard.tick(p);
            wardAt(end, p, r - 2.0);
            com.panda.promisedend.logic.AncientWard.tick(p);
            return p.getX();
        };
        if (!com.panda.promisedend.logic.AncientWard.stands(server)) bad.add("the Ward doesn't stand with every seal mended");
        var walker = player.apply(net.minecraft.world.level.GameType.SURVIVAL);
        double x = cross.apply(walker, r + 3.0);
        PromisedEnd.LOGGER.info("[ward_sets_back] stepped to {} inside: set back to {}", r - 2, walker.position());
        if (walker.level() != end || Math.abs(x - (r + 3.0)) > 0.01) bad.add("a survival player 2 inside was left at " + walker.position() + " in " + walker.level().dimension());
        for (var mode : List.of(net.minecraft.world.level.GameType.CREATIVE, net.minecraft.world.level.GameType.SPECTATOR)) {
            var exempt = player.apply(mode);
            if (Math.abs(cross.apply(exempt, r + 3.0) - (r - 2.0)) > 0.01) bad.add("a " + mode.getName() + " player was set back to " + exempt.position());
        }
        var deep = player.apply(net.minecraft.world.level.GameType.SURVIVAL);
        wardAt(end, deep, r - 100.0);
        com.panda.promisedend.logic.AncientWard.tick(deep);
        if (deep.level() == end) bad.add("a survival player 100 inside stayed in the End at " + deep.position());
        // riding: the pig is moved across with its rider on it, and both go back
        var rider = player.apply(net.minecraft.world.level.GameType.SURVIVAL);
        var pig = net.minecraft.world.entity.EntityTypes.PIG.create(end, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        end.getChunk(BlockPos.containing(r + 3.0, 80, 0));
        pig.snapTo(r + 3.0, 80, 0.5, 0, 0);
        pig.setNoAi(true);
        pig.setNoGravity(true);
        end.addFreshEntity(pig);
        wardAt(end, rider, r + 3.0);
        if (!rider.startRiding(pig, true, false)) bad.add("the rider couldn't mount the pig");
        com.panda.promisedend.logic.AncientWard.tick(rider);
        pig.teleportTo(end, r - 2.0, 80, 0.5, java.util.Set.of(), 0, 0, false);
        com.panda.promisedend.logic.AncientWard.tick(rider);
        PromisedEnd.LOGGER.info("[ward_sets_back] rode to {} inside: the pig at {}, the rider {} riding at {}", r - 2, pig.position(), rider.isPassenger() ? "still" : "not", rider.position());
        if (pig.getX() < r) bad.add("the mount was left inside at " + pig.position());
        if (rider.getVehicle() != pig) bad.add("the rider was taken off the mount");
        pig.discard();
        // every seal broken: let through
        for (int i = 0; i < com.panda.promisedend.logic.AncientWard.SEALS.size(); i++) com.panda.promisedend.logic.AncientWard.breakSeal(server, i);
        var free = player.apply(net.minecraft.world.level.GameType.SURVIVAL);
        if (Math.abs(cross.apply(free, r + 3.0) - (r - 2.0)) > 0.01) bad.add("with every seal broken a player was set back to " + free.position());
        com.panda.promisedend.logic.AncientWard.reset(server);
        for (var p : players) server.getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("ward: " + bad);
        helper.succeed();
    }

    /** Puts a player in the End at ({@code x}, 80, 0.5). */
    private static void wardAt(net.minecraft.server.level.ServerLevel end, net.minecraft.server.level.ServerPlayer p, double x) {
        end.getChunk(BlockPos.containing(x, 80, 0));
        p.teleport(new net.minecraft.world.level.portal.TeleportTransition(end, new net.minecraft.world.phys.Vec3(x, 80, 0.5), net.minecraft.world.phys.Vec3.ZERO, 0, 0,
                net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
    }

    /**
     * A pearl thrown inward at the Ward, by a survival player 20 outside it, bounces off: on the Ward's tick it is put
     * back outside and flies outward. With every seal broken it flies on. The seals are left mended.
     */
    private static void wardBouncesPearls(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var end = server.getLevel(net.minecraft.world.level.Level.END);
        List<String> bad = new ArrayList<>();
        com.panda.promisedend.logic.AncientWard.reset(server);
        int r = com.panda.promisedend.EndConfig.wardRadius();
        var thrower = realPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        wardAt(end, thrower, r + 20.0);
        // the pearl's chunk, 21 inward, holds entities once the player's own chunks load round them (a test before
        // this one may have loaded it, or not)
        helper.runAfterDelay(5, () -> {
            // mended again: the seal tests beside this one ran in those ticks
            com.panda.promisedend.logic.AncientWard.reset(server);
            for (boolean standing : new boolean[] {true, false}) {
                if (!standing) for (int i = 0; i < com.panda.promisedend.logic.AncientWard.SEALS.size(); i++) com.panda.promisedend.logic.AncientWard.breakSeal(server, i);
                var pearl = new net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl(net.minecraft.world.entity.EntityTypes.ENDER_PEARL, end);
                pearl.snapTo(r - 1.0, 82, 0.5, 0, 0);
                pearl.setDeltaMovement(-1.0, 0, 0);
                end.addFreshEntity(pearl);
                com.panda.promisedend.logic.AncientWard.tick(server);
                PromisedEnd.LOGGER.info("[ward_bounces_pearls] the Ward {}: the pearl at {} moving {}", standing ? "up" : "down", pearl.position(), pearl.getDeltaMovement());
                if (standing && (pearl.getX() < r || pearl.getDeltaMovement().x <= 0)) bad.add("a pearl went through the Ward: at " + pearl.position() + " moving " + pearl.getDeltaMovement());
                if (!standing && (pearl.getX() > r || pearl.getDeltaMovement().x >= 0)) bad.add("with the Ward down a pearl was turned: at " + pearl.position() + " moving " + pearl.getDeltaMovement());
                pearl.discard();
            }
            com.panda.promisedend.logic.AncientWard.reset(server);
            server.getPlayerList().remove(thrower);
            if (!bad.isEmpty()) helper.fail("pearls: " + bad);
            helper.succeed();
        });
    }

    /**
     * An Ender Pearl in hand shows the way in: a survival player 1000 out in the End is shown it with a pearl in either
     * hand and not without; 100 out (the main island in sight), a spectator, or in the Overworld, not. The way points
     * at the centre.
     */
    private static void pearlShowsTheWay(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var end = server.getLevel(net.minecraft.world.level.Level.END);
        List<String> bad = new ArrayList<>();
        var p = realPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var pearl = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ENDER_PEARL);
        wardAt(end, p, 1000.0);
        if (com.panda.promisedend.logic.PearlGuide.guides(p)) bad.add("shown the way with no pearl");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pearl.copy());
        if (!com.panda.promisedend.logic.PearlGuide.guides(p)) bad.add("not shown the way 1000 out with a pearl in hand");
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        p.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, pearl.copy());
        if (!com.panda.promisedend.logic.PearlGuide.guides(p)) bad.add("not shown the way with a pearl in the off hand");
        wardAt(end, p, 100.0);
        if (com.panda.promisedend.logic.PearlGuide.guides(p)) bad.add("shown the way 100 out");
        wardAt(end, p, 1000.0);
        p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        if (com.panda.promisedend.logic.PearlGuide.guides(p)) bad.add("a spectator shown the way");
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        p.teleport(new net.minecraft.world.level.portal.TeleportTransition(server.overworld(), new net.minecraft.world.phys.Vec3(1000, 100, 0.5), net.minecraft.world.phys.Vec3.ZERO, 0, 0,
                net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        if (com.panda.promisedend.logic.PearlGuide.guides(p)) bad.add("shown the way in the Overworld");
        double[] way = com.panda.promisedend.logic.PearlGuide.way(1000, -1000);
        if (Math.abs(way[0] + Math.sqrt(0.5)) > 1e-9 || Math.abs(way[1] - Math.sqrt(0.5)) > 1e-9) bad.add("the way from (1000, -1000) is " + java.util.Arrays.toString(way));
        // while the Foundry's seal binds the Ward the pearl leads to the nearest Foundry (logged: this level's End
        // may have none in reach); the motes are counted with it broken, when the way is inward
        wardAt(end, p, 1000.0);
        PromisedEnd.LOGGER.info("[pearl_shows_the_way] with the Foundry's seal whole the pearl leads to {}", com.panda.promisedend.logic.PearlGuide.lead(p));
        com.panda.promisedend.logic.AncientWard.breakSeal(server, 0);
        // the motes: 12 ticks, a mote each 3 to the holder alone, a trail to a point 6 inward; a player beside, without
        // a pearl, sent none
        wardAt(end, p, 1000.0);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pearl.copy());
        var other = realPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        wardAt(end, other, 1001.0);
        sent(p, Object.class);
        sent(other, Object.class);
        for (int t = 0; t < 12; t++) {
            p.tickCount = t;
            other.tickCount = t;
            com.panda.promisedend.logic.PearlGuide.tick(p);
            com.panda.promisedend.logic.PearlGuide.tick(other);
        }
        var motes = sent(p, net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class);
        int others = sent(other, net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.class).size();
        PromisedEnd.LOGGER.info("[pearl_shows_the_way] 12 ticks with a pearl: {} motes to the holder, {} to the player beside", motes.size(), others);
        if (motes.size() != 12 / com.panda.promisedend.logic.PearlGuide.EVERY) bad.add(motes.size() + " motes in 12 ticks");
        if (others != 0) bad.add(others + " motes to a player beside, with no pearl");
        for (var m : motes) {
            if (!(m.particle() instanceof net.minecraft.core.particles.TrailParticleOption trail)) {
                bad.add("a mote of " + m.particle().getType());
                continue;
            }
            // on the way the pearl leads: to the nearest Foundry while its seal binds the Ward, else inward
            double[] lead = com.panda.promisedend.logic.PearlGuide.way(p.getX(), p.getZ(), com.panda.promisedend.logic.PearlGuide.lead(p));
            double along = (trail.target().x - m.x()) * lead[0] + (trail.target().z - m.z()) * lead[1];
            if (along < com.panda.promisedend.logic.PearlGuide.REACH - 0.01) bad.add("a mote drifting to " + trail.target() + " from " + m.x() + ", " + m.z() + ", not on the way");
        }
        // with the Foundry's seal broken, the way is the heart of the End
        com.panda.promisedend.logic.AncientWard.breakSeal(server, 0);
        if (com.panda.promisedend.logic.PearlGuide.lead(p) != net.minecraft.core.BlockPos.ZERO) bad.add("led to " + com.panda.promisedend.logic.PearlGuide.lead(p) + " with the Foundry's seal broken");
        com.panda.promisedend.logic.AncientWard.reset(server);
        // off with arrivals by coordinates off
        var previous = com.panda.promisedend.EndConfig.current();
        com.panda.promisedend.EndConfig.use(new com.panda.promisedend.EndConfig.Source() {
            @Override public boolean terrainEnabled() { return previous.terrainEnabled(); }
            @Override public int edgeDistance() { return previous.edgeDistance(); }
            @Override public int anchorTriggerDepth() { return previous.anchorTriggerDepth(); }
            @Override public boolean dragonPhase3() { return previous.dragonPhase3(); }
            @Override public boolean dragonWrath() { return previous.dragonWrath(); }
            @Override public int maxWrath() { return previous.maxWrath(); }
            @Override public boolean arrivalByCoordinates() { return false; }
        });
        try {
            if (com.panda.promisedend.logic.PearlGuide.guides(p)) bad.add("shown the way with arrivals by coordinates off");
        } finally {
            com.panda.promisedend.EndConfig.use(previous);
        }
        server.getPlayerList().remove(other);
        server.getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("pearl guide: " + bad);
        helper.succeed();
    }

    /**
     * With {@code arrival.by_coordinates} off, an End portal is vanilla's (no arrival of ours) and the Ward never
     * stands, whatever the seals.
     */
    private static void arrivalOffIsVanilla(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        List<String> bad = new ArrayList<>();
        var previous = com.panda.promisedend.EndConfig.current();
        com.panda.promisedend.EndConfig.use(new com.panda.promisedend.EndConfig.Source() {
            @Override public boolean terrainEnabled() { return previous.terrainEnabled(); }
            @Override public int edgeDistance() { return previous.edgeDistance(); }
            @Override public int anchorTriggerDepth() { return previous.anchorTriggerDepth(); }
            @Override public boolean dragonPhase3() { return previous.dragonPhase3(); }
            @Override public boolean dragonWrath() { return previous.dragonWrath(); }
            @Override public int maxWrath() { return previous.maxWrath(); }
            @Override public boolean arrivalByCoordinates() { return false; }
        });
        try {
            com.panda.promisedend.logic.AncientWard.reset(server);
            int before = com.panda.promisedend.logic.Arrivals.built(server).size();
            if (com.panda.promisedend.logic.Arrivals.destination(level, null, new BlockPos(-900, 64, 1800)) != null) bad.add("an arrival of ours with it off");
            if (com.panda.promisedend.logic.Arrivals.built(server).size() != before) bad.add("a Waystation built with it off");
            if (com.panda.promisedend.logic.AncientWard.stands(server)) bad.add("the Ward stands with it off");
        } finally {
            com.panda.promisedend.EndConfig.use(previous);
        }
        if (!bad.isEmpty()) helper.fail("off: " + bad);
        helper.succeed();
    }

    /**
     * The seals: with none broken the Ward stands; it still stands with two; the third drops it. A seal broken twice
     * breaks once. Mended, it stands again.
     */
    private static void sealsDropTheWard(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        List<String> bad = new ArrayList<>();
        com.panda.promisedend.logic.AncientWard.reset(server);
        if (!com.panda.promisedend.logic.AncientWard.stands(server)) bad.add("down with no seal broken");
        if (!com.panda.promisedend.logic.AncientWard.breakSeal(server, 0)) bad.add("the first seal didn't break");
        if (com.panda.promisedend.logic.AncientWard.breakSeal(server, 0)) bad.add("the first seal broke twice");
        com.panda.promisedend.logic.AncientWard.breakSeal(server, 1);
        if (!com.panda.promisedend.logic.AncientWard.stands(server)) bad.add("down with two seals broken");
        com.panda.promisedend.logic.AncientWard.breakSeal(server, 2);
        if (com.panda.promisedend.logic.AncientWard.stands(server)) bad.add("still standing with all three broken");
        if (com.panda.promisedend.logic.AncientWard.broken(server) != com.panda.promisedend.logic.AncientWard.ALL) bad.add("broken " + com.panda.promisedend.logic.AncientWard.broken(server));
        com.panda.promisedend.logic.AncientWard.reset(server);
        if (!com.panda.promisedend.logic.AncientWard.stands(server)) bad.add("down after every seal was mended");
        if (!bad.isEmpty()) helper.fail("seals: " + bad);
        helper.succeed();
    }

    /**
     * The Return Gate sends a player to their respawn point, out of the End, as the exit portal does; a pig goes
     * nowhere. It can't be broken, has no item, and gives light.
     */
    private static void returnGateSendsHome(GameTestHelper helper) {
        var level = helper.getLevel();
        var end = level.getServer().getLevel(net.minecraft.world.level.Level.END);
        List<String> bad = new ArrayList<>();
        var gate = (com.panda.promisedend.block.ReturnGateBlock) ModBlocks.RETURN_GATE.get();
        var state = gate.defaultBlockState();
        var player = helper.makeMockServerPlayerInLevel();
        var home = gate.getPortalDestination(end, player, BlockPos.ZERO);
        if (home == null) bad.add("no way home for a player");
        else if (home.newLevel().dimension() == net.minecraft.world.level.Level.END) bad.add("the gate sends a player into the End");
        var pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(1, 1, 1));
        if (gate.getPortalDestination(end, pig, BlockPos.ZERO) != null) bad.add("the gate sends a pig somewhere");
        if (state.getDestroySpeed(level, BlockPos.ZERO) >= 0) bad.add("the gate can be broken");
        if (gate.asItem() != net.minecraft.world.item.Items.AIR) bad.add("the gate has an item");
        if (state.getLightEmission() < 10) bad.add("the gate gives light " + state.getLightEmission());
        player.discard();
        if (!bad.isEmpty()) helper.fail("return gate: " + bad);
        helper.succeed();
    }

    /** A Drift Islet in the arena on a 7-block route along x at y 3, leaving its first mooring at once. */
    private static com.panda.promisedend.entity.DriftIslet islet(GameTestHelper helper) {
        var level = helper.getLevel();
        var a = helper.absoluteVec(new net.minecraft.world.phys.Vec3(2.5, 3, 5.5));
        var b = helper.absoluteVec(new net.minecraft.world.phys.Vec3(9.5, 3, 5.5));
        var islet = com.panda.promisedend.registry.ModEntities.DRIFT_ISLET.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        islet.setRoute(new com.panda.promisedend.logic.DriftRoute(a.x, a.y, a.z, b.x, b.y, b.z), new com.panda.promisedend.logic.DriftRoute.State(0, 1, 0));
        level.addFreshEntity(islet);
        return islet;
    }

    /**
     * A pig set on a Drift Islet rides it across: after the 7-block crossing (47 ticks at 3 blocks a second) the islet
     * waits at its far mooring and the pig is still on it, carried the same 7 blocks. Something standing on it stands
     * on its top (it is a collision box).
     */
    private static void driftIsletCarries(GameTestHelper helper) {
        var islet = islet(helper);
        var pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(2, 4, 5));
        pig.setNoAi(true);
        // a pig without AI doesn't fall: set on the top
        pig.snapTo(islet.getX(), islet.getBoundingBox().maxY, islet.getZ(), 0, 0);
        double[] start = {0, 0};
        helper.runAtTickTime(5, () -> {
            start[0] = pig.getX() - islet.getX();
            start[1] = pig.getY() - islet.getBoundingBox().maxY;
            PromisedEnd.LOGGER.info("[drift_islet_carries] tick 5: the pig {} off the islet's middle, {} over its top", start[0], start[1]);
            if (Math.abs(start[1]) > 0.05) helper.fail("the pig isn't standing on the islet's top: " + start[1] + " over it");
        });
        helper.runAtTickTime(60, () -> {
            var to = islet.route();
            double off = pig.getX() - islet.getX();
            PromisedEnd.LOGGER.info("[drift_islet_carries] tick 60: the islet at {} ({} of the way, waiting {}), the pig at {}", islet.position(), islet.state().s, islet.state().wait, pig.position());
            if (islet.state().s != 1) helper.fail("the islet hasn't crossed: " + islet.state().s + " of the way");
            else if (Math.abs(islet.getX() - to.bx()) > 1e-6) helper.fail("the islet isn't at its far mooring: " + islet.position());
            else if (Math.abs(off - start[0]) > 0.3 || Math.abs(pig.getY() - islet.getBoundingBox().maxY) > 0.05) helper.fail("the pig wasn't carried: " + off + " off the middle (was " + start[0] + "), at " + pig.position());
            else {
                islet.discard();
                helper.succeed();
            }
        });
    }

    /**
     * A Drift Islet is saved with its route, its place on it, its way and its wait, and that its way was cleared: a copy
     * loaded from what it saved is at the same place and goes on the same way.
     */
    private static void driftIsletSaves(GameTestHelper helper) {
        var level = helper.getLevel();
        var islet = islet(helper);
        for (int t = 0; t < 20; t++) islet.tick();
        if (islet.clearWay() < 0) helper.fail("the arena's chunks aren't loaded");
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        islet.saveWithoutId(out);
        var copy = com.panda.promisedend.registry.ModEntities.DRIFT_ISLET.get().create(level, net.minecraft.world.entity.EntitySpawnReason.LOAD);
        copy.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
        List<String> bad = new ArrayList<>();
        if (!copy.route().equals(islet.route())) bad.add("route " + copy.route() + ", saved " + islet.route());
        if (Math.abs(copy.state().s - islet.state().s) > 1e-9 || copy.state().dir != islet.state().dir || copy.state().wait != islet.state().wait)
            bad.add("state " + copy.state().s + "/" + copy.state().dir + "/" + copy.state().wait + ", saved " + islet.state().s + "/" + islet.state().dir + "/" + islet.state().wait);
        if (copy.position().distanceTo(islet.position()) > 1e-6) bad.add("at " + copy.position() + ", saved at " + islet.position());
        if (!islet.cleared() || copy.cleared() != islet.cleared()) bad.add("cleared " + islet.cleared() + ", loaded " + copy.cleared());
        PromisedEnd.LOGGER.info("[drift_islet_saves] saved {} of the way going {}, loaded {} going {}", islet.state().s, islet.state().dir, copy.state().s, copy.state().dir);
        islet.discard();
        copy.discard();
        if (!bad.isEmpty()) helper.fail("drift islet save: " + bad);
        helper.succeed();
    }

    /**
     * A block in a Drift Islet's way turns it back: it never reaches the block, stands a second, and goes back the way it
     * came, all the way to its first mooring.
     */
    private static void driftIsletBlockedTurns(GameTestHelper helper) {
        blockedTurns(helper, new BlockPos(7, 3, 5), "drift_islet_blocked_turns");
    }

    /** A block at a rider's head height over its way turns it back as well, so it doesn't scrape a rider off. */
    private static void driftIsletHeadroomTurns(GameTestHelper helper) {
        blockedTurns(helper, new BlockPos(7, 5, 5), "drift_islet_headroom_turns");
    }

    private static void blockedTurns(GameTestHelper helper, BlockPos block, String name) {
        // a block a player put there after the islet cleared its way, which it does once, as it first ticks
        var islet = islet(helper);
        if (islet.clearWay() < 0) helper.fail("the arena's chunks aren't loaded");
        helper.setBlock(block, net.minecraft.world.level.block.Blocks.STONE);
        double wall = helper.absoluteVec(net.minecraft.world.phys.Vec3.atLowerCornerOf(block)).x;
        double start = islet.getX();
        double[] farthest = {Double.NEGATIVE_INFINITY};
        int[] turnedAt = {-1};
        helper.onEachTick(() -> {
            farthest[0] = Math.max(farthest[0], islet.getBoundingBox().maxX);
            if (turnedAt[0] < 0 && islet.turns() > 0) turnedAt[0] = (int) helper.getTick();
        });
        helper.runAtTickTime(120, () -> {
            PromisedEnd.LOGGER.info("[{}] turned at tick {}, the islet's front got to {} (the block at {}), now at {} (it started at {})", name, turnedAt[0], farthest[0], wall,
                    islet.getX(), start);
            if (islet.turns() < 1) helper.fail("never turned");
            else if (farthest[0] > wall + 1e-6) helper.fail("ran into the block: its front at " + farthest[0]);
            else if (Math.abs(islet.getX() - start) > 1e-6) helper.fail("not back at its first mooring: at " + islet.getX() + ", started at " + start);
            else {
                islet.discard();
                helper.succeed();
            }
        });
    }

    /**
     * A survival player standing on a Drift Islet on a dedicated server isn't kicked for flying: each tick it clears
     * the count the server kicks floating players at, and their fall distance; a player beside it keeps both. Single
     * player never counts floating, so no scenario can show this.
     */
    private static void driftIsletKeepsRiderAloft(GameTestHelper helper) {
        var islet = islet(helper);
        islet.setRoute(islet.route(), new com.panda.promisedend.logic.DriftRoute.State(0, 1, com.panda.promisedend.logic.DriftRoute.WAIT));
        var on = realPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var beside = realPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var level = helper.getLevel();
        on.teleportTo(level, islet.getX(), islet.getBoundingBox().maxY, islet.getZ(), java.util.Set.of(), 0, 0, true);
        beside.teleportTo(level, islet.getX() + 6, islet.getBoundingBox().maxY, islet.getZ(), java.util.Set.of(), 0, 0, true);
        List<String> bad = new ArrayList<>();
        try {
            var floating = net.minecraft.server.network.ServerGamePacketListenerImpl.class.getDeclaredField("aboveGroundTickCount");
            floating.setAccessible(true);
            for (var p : List.of(on, beside)) {
                p.fallDistance = 10;
                floating.setInt(p.connection, 50);
            }
            islet.tick();
            int a = floating.getInt(on.connection), b = floating.getInt(beside.connection);
            PromisedEnd.LOGGER.info("[drift_islet_keeps_rider_aloft] on it: floating {}, fall distance {}; beside it: {}, {}", a, on.fallDistance, b, beside.fallDistance);
            if (a != 0 || on.fallDistance != 0) bad.add("a rider kept floating " + a + " and fall distance " + on.fallDistance);
            if (b != 50 || beside.fallDistance != 10) bad.add("a player beside it had floating " + b + " and fall distance " + beside.fallDistance);
        } catch (ReflectiveOperationException e) {
            bad.add(e.toString());
        }
        level.getServer().getPlayerList().remove(on);
        level.getServer().getPlayerList().remove(beside);
        islet.discard();
        if (!bad.isEmpty()) helper.fail("drift islet riders: " + bad);
        helper.succeed();
    }

    /**
     * A player who rides a Drift Islet from one mooring to the other has crossed (the Codex's goal), and so has one whose
     * client places them 3 ticks late; one who boarded with them and stepped off on the way hasn't, nor has one who
     * boarded once it had left.
     */
    private static void driftIsletCrossing(GameTestHelper helper) {
        var islet = islet(helper);
        islet.setRoute(islet.route(), new com.panda.promisedend.logic.DriftRoute.State(0, 1, 3));
        var level = helper.getLevel();
        var crossed = level.getServer().getAdvancements().get(PromisedEnd.id("outer/ride_drift"));
        if (crossed == null) {
            helper.fail("no advancement outer/ride_drift");
            return;
        }
        var on = realPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var off = realPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var late = realPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        var lagging = realPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        lagging.teleportTo(level, islet.getX(), islet.getBoundingBox().maxY, islet.getZ() + 0.4, java.util.Set.of(), 0, 0, true);
        java.util.ArrayDeque<net.minecraft.world.phys.Vec3> behind = new java.util.ArrayDeque<>();
        for (var p : List.of(on, off)) p.teleportTo(level, islet.getX() + (p == on ? 0.4 : -0.4), islet.getBoundingBox().maxY, islet.getZ(), java.util.Set.of(), 0, 0, true);
        late.teleportTo(level, islet.getX() + 3, islet.getBoundingBox().maxY + 4, islet.getZ() + 6, java.util.Set.of(), 0, 0, true);
        int moved = 0, ticks = 0;
        List<net.minecraft.server.level.ServerPlayer> riding = new ArrayList<>(List.of(on, off));
        for (; ticks < 400; ticks++) {
            boolean moving = islet.state().wait == 0;
            var from = islet.position();
            islet.tick();
            // a player's client moves them with the islet, and the server takes the client's word for it
            var by = islet.position().subtract(from);
            for (var p : riding) p.teleportTo(level, p.getX() + by.x, p.getY() + by.y, p.getZ() + by.z, java.util.Set.of(), 0, 0, true);
            // a slow client: its player moves by what the islet moved 3 ticks ago
            behind.add(by);
            if (behind.size() > 3) {
                var then = behind.poll();
                lagging.teleportTo(level, lagging.getX() + then.x, lagging.getY() + then.y, lagging.getZ() + then.z, java.util.Set.of(), 0, 0, true);
            }
            if (!moving) continue;
            moved++;
            if (moved == 10) {
                off.teleportTo(level, islet.getX(), islet.getBoundingBox().maxY + 4, islet.getZ() + 6, java.util.Set.of(), 0, 0, true);
                late.teleportTo(level, islet.getX() - 0.4, islet.getBoundingBox().maxY, islet.getZ(), java.util.Set.of(), 0, 0, true);
                riding.remove(off);
                riding.add(late);
            }
            if (islet.state().wait > 0) break;
        }
        boolean a = on.getAdvancements().getOrStartProgress(crossed).isDone(), b = off.getAdvancements().getOrStartProgress(crossed).isDone(),
                c = late.getAdvancements().getOrStartProgress(crossed).isDone();
        boolean d = lagging.getAdvancements().getOrStartProgress(crossed).isDone();
        PromisedEnd.LOGGER.info("[drift_islet_crossing] arrived after {} ticks ({} moving) at s={}: rode it all {}, stepped off {}, boarded late {}, lagging {}", ticks, moved, islet.state().s, a, b, c, d);
        List<String> bad = new ArrayList<>();
        if (islet.state().s < 1) bad.add("the islet never reached its far mooring: s=" + islet.state().s);
        if (!a) bad.add("the player who rode it all the way hasn't crossed");
        if (b) bad.add("the player who stepped off has crossed");
        if (c) bad.add("the player who boarded on the way has crossed");
        if (!d) bad.add("the player whose client lags 3 ticks hasn't crossed");
        for (var p : List.of(on, off, late, lagging)) level.getServer().getPlayerList().remove(p);
        islet.discard();
        if (!bad.isEmpty()) helper.fail("drift islet crossing: " + bad);
        helper.succeed();
    }

    /**
     * A seal binds the Ancients' Ward only once its guardian is in the game: with none in, nothing binds it; with all
     * three, any one unbroken does; the GameTests run with every guardian in. With the Ward down, a player in the End
     * earns {@code rim/ward_down}, which opens the Codex's last act; with it up, a new player doesn't. The seals are left
     * mended.
     */
    private static void wardBindsByGuardian(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var end = server.getLevel(net.minecraft.world.level.Level.END);
        int all = com.panda.promisedend.logic.AncientWard.ALL;
        List<String> bad = new ArrayList<>();
        if (com.panda.promisedend.logic.AncientWard.binds(0, 0)) bad.add("binds with no guardian in");
        if (com.panda.promisedend.logic.AncientWard.binds(0b010, 0)) bad.add("binds with no guardian in and one seal broken");
        if (!com.panda.promisedend.logic.AncientWard.binds(0, all)) bad.add("doesn't bind with every guardian in and no seal broken");
        if (!com.panda.promisedend.logic.AncientWard.binds(0b011, all)) bad.add("doesn't bind with one seal unbroken");
        if (com.panda.promisedend.logic.AncientWard.binds(all, all)) bad.add("binds with every seal broken");
        if (com.panda.promisedend.logic.AncientWard.binds(0b001, 0b001)) bad.add("binds with its only guardian's seal broken");
        if (!com.panda.promisedend.logic.AncientWard.binds(0b110, 0b001)) bad.add("doesn't bind with its only guardian's seal unbroken");
        if (com.panda.promisedend.logic.AncientWard.guarded() != all) bad.add("the GameTests run without every guardian in: " + com.panda.promisedend.logic.AncientWard.guarded());
        var down = server.getAdvancements().get(com.panda.promisedend.logic.CodexBook.WARD_DOWN);
        if (end == null || down == null) {
            helper.fail("no End level, or no advancement rim/ward_down");
            return;
        }
        int r = com.panda.promisedend.EndConfig.wardRadius();
        com.panda.promisedend.logic.AncientWard.reset(server);
        var before = realPlayer(helper, net.minecraft.world.level.GameType.SURVIVAL);
        wardAt(end, before, r + 40.0);
        for (int i = 0; i < 20; i++) {
            before.tickCount++;
            com.panda.promisedend.logic.AncientWard.tick(before);
        }
        if (before.getAdvancements().getOrStartProgress(down).isDone()) bad.add("rim/ward_down earned with the Ward up");
        for (int i = 0; i < com.panda.promisedend.logic.AncientWard.SEALS.size(); i++) com.panda.promisedend.logic.AncientWard.breakSeal(server, i);
        for (int i = 0; i < 20; i++) {
            before.tickCount++;
            com.panda.promisedend.logic.AncientWard.tick(before);
        }
        if (!before.getAdvancements().getOrStartProgress(down).isDone()) bad.add("rim/ward_down not earned in a second with the Ward down");
        com.panda.promisedend.logic.AncientWard.reset(server);
        server.getPlayerList().remove(before);
        if (!bad.isEmpty()) helper.fail("ward by guardian: " + bad);
        helper.succeed();
    }

    /**
     * Something sunk into a Drift Islet's top from above (loaded before it, or a client that had its player before the
     * islet) is lifted onto it, not left to fall through; something under it is left alone.
     */
    private static void driftIsletLiftsTheSunk(GameTestHelper helper) {
        var islet = islet(helper);
        islet.setRoute(islet.route(), new com.panda.promisedend.logic.DriftRoute.State(0, 1, com.panda.promisedend.logic.DriftRoute.WAIT));
        var sunk = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(2, 4, 5));
        var under = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(2, 1, 5));
        for (var pig : List.of(sunk, under)) pig.setNoAi(true);
        double top = islet.getBoundingBox().maxY;
        sunk.snapTo(islet.getX(), top - 0.3, islet.getZ(), 0, 0);
        under.snapTo(islet.getX(), islet.getBoundingBox().minY - under.getBbHeight() + 0.2, islet.getZ(), 0, 0);
        double underY = under.getY();
        islet.tick();
        PromisedEnd.LOGGER.info("[drift_islet_lifts_the_sunk] the sunk pig now at {} (the top {}); the one under at {} (was {})", sunk.getY(), top, under.getY(), underY);
        List<String> bad = new ArrayList<>();
        if (Math.abs(sunk.getY() - top) > 1e-6) bad.add("the sunk pig left at " + sunk.getY() + ", the top at " + top);
        if (Math.abs(under.getY() - underY) > 1e-6) bad.add("the pig under it moved to " + under.getY());
        islet.discard();
        sunk.discard();
        under.discard();
        if (!bad.isEmpty()) helper.fail("drift islet lift: " + bad);
        helper.succeed();
    }

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
        // the other constructs: each one's clips, bones, textures, and its timed clips against the class's ticks
        record Rig(String id, List<String> clips, java.util.Map<String, Integer> ticks) {}
        for (var rig : List.of(new Rig("chorus_weaver", com.panda.promisedend.entity.ChorusWeaver.CLIPS, java.util.Map.of(
                "reveal", com.panda.promisedend.entity.ChorusWeaver.REVEAL_TICKS, "lash", com.panda.promisedend.entity.ChorusWeaver.LASH_TICKS)),
                new Rig("lumen_wisp", com.panda.promisedend.entity.LumenWisp.CLIPS, java.util.Map.of()),
                new Rig("rift_stalker", com.panda.promisedend.entity.RiftStalker.CLIPS, java.util.Map.of(
                        "gather", com.panda.promisedend.entity.RiftStalker.GATHER_TICKS, "strike", com.panda.promisedend.entity.RiftStalker.STRIKE_TICKS)),
                new Rig("purpur_sentry", com.panda.promisedend.entity.PurpurSentry.CLIPS, java.util.Map.of(
                        "lunge", com.panda.promisedend.entity.PurpurSentry.LUNGE_TICKS)))) {
            var a = readJson(base + "geckolib/animations/entity/" + rig.id() + ".animation.json");
            var g = readJson(base + "geckolib/models/entity/" + rig.id() + ".geo.json");
            if (a == null || g == null) {
                bad.add(rig.id() + ": no animation or model file");
                continue;
            }
            var its = new java.util.TreeSet<>(a.getAsJsonObject("animations").keySet());
            if (!its.equals(new java.util.TreeSet<>(rig.clips()))) bad.add(rig.id() + ": clips " + its + " but the class plays " + rig.clips());
            var has = new java.util.HashSet<String>();
            g.getAsJsonArray("minecraft:geometry").get(0).getAsJsonObject().getAsJsonArray("bones")
                    .forEach(b -> has.add(b.getAsJsonObject().get("name").getAsString()));
            a.getAsJsonObject("animations").entrySet().forEach(e -> e.getValue().getAsJsonObject().getAsJsonObject("bones").keySet().forEach(b -> {
                if (!has.contains(b)) bad.add(rig.id() + ": " + e.getKey() + " moves a bone the model lacks: " + b);
            }));
            rig.ticks().forEach((clip, ticks) -> {
                var c = a.getAsJsonObject("animations").getAsJsonObject(clip);
                if (c == null || Math.round(c.get("animation_length").getAsDouble() * 20) != ticks) bad.add(rig.id() + ": the " + clip + " clip isn't " + ticks + " ticks");
            });
            if (EndGameTests.class.getResource(base + "textures/entity/" + rig.id() + ".png") == null) bad.add("no texture for " + rig.id());
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
    private static final List<String> STRUCTURES = List.of("rim_outpost", "shulker_foundry", "void_shrine", "fallen_observatory");
    /** Our structures with no chest of their own. */
    private static final java.util.Set<String> CHESTLESS = java.util.Set.of("void_shrine");

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
            if (chests.isEmpty() && !CHESTLESS.contains(name)) bad.add(name + ": no chest");
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
        java.util.Map<String, String> home = java.util.Map.of("rim_outpost", "shattered_rim", "shulker_foundry", "shulker_reefs", "void_shrine", "chorus_thicket", "fallen_observatory", "hollow_highlands");
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
     * and a Lily on it; Resonite with budding Resonite in it), and neither outside the End; with terrain on nothing. The
     * Skyfall's blossoms and vents go on vanilla's small islands ({@code #promisedend:skyfall_fallback}) the same way.
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
            bad.addAll(fallbackSkyfall(helper));
            String scars = fallbackScars(helper);
            if (scars != null) bad.add(scars);
            String reefs = fallbackReefs(helper);
            if (reefs != null) bad.add(reefs);
            String hollows = fallbackHollows(helper);
            if (hollows != null) bad.add(hollows);
            String blooms = fallbackBlooms(helper);
            if (blooms != null) bad.add(blooms);
            String wilds = fallbackWilds(helper);
            if (wilds != null) bad.add(wilds);
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
        // the Skyfall's blossoms and vents on vanilla's small islands, through the same loader hook; with terrain on
        // neither builds (the terrain_off filter)
        var smallIslands = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.SMALL_END_ISLANDS);
        if (!smallIslands.is(net.minecraft.tags.TagKey.create(Registries.BIOME, PromisedEnd.id("skyfall_fallback")))) bad.add("small_end_islands isn't in #promisedend:skyfall_fallback");
        for (String name : List.of("drift_blossom_fallback", "updraft_vent_fallback")) {
            var feature = access.lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id(name)));
            if (smallIslands.value().getGenerationSettings().features().stream().noneMatch(step -> step.contains(feature)))
                bad.add(name + " isn't among small_end_islands' features (the loader's biome modifier didn't apply)");
        }
        if (com.panda.promisedend.EndConfig.terrainEnabled()) {
            var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
            int[] built = skyfallChunk(end, new BlockPos(9472, end.getMaxY() - 60, 9600));
            if (built[0] + built[1] != 0) bad.add("with terrain on the terrain-off copies built " + built[0] + " blossoms and " + built[1] + " vents");
        }
        // the Voidscar's scars and vents on vanilla's barrens, through the same loader hook; with terrain on the scar builds nothing
        var barrens = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.END_BARRENS);
        if (!barrens.is(net.minecraft.tags.TagKey.create(Registries.BIOME, PromisedEnd.id("voidscar_fallback")))) bad.add("end_barrens isn't in #promisedend:voidscar_fallback");
        for (String name : List.of("rift_scar_fallback", "updraft_vent_fallback")) {
            var feature = access.lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id(name)));
            if (barrens.value().getGenerationSettings().features().stream().noneMatch(step -> step.contains(feature)))
                bad.add(name + " isn't among end_barrens' features (the loader's biome modifier didn't apply)");
        }
        if (com.panda.promisedend.EndConfig.terrainEnabled()) {
            var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
            BlockPos at = new BlockPos(9800, end.getMaxY() - 60, 9000);
            for (int d = 0; d < 4; d++) floor(end, at.below(d), 16, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            if (grove(end, "rift_scar_fallback").place(end, end.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(501), at))
                bad.add("the terrain-off scar built with terrain on");
            clearDeep(end, at);
        }
        // the Hollow Highlands' caverns in vanilla's highlands, through the same loader hook
        var hollows = access.lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("hollow_cavern_fallback")));
        var hollowLands = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.END_HIGHLANDS);
        if (!hollowLands.is(net.minecraft.tags.TagKey.create(Registries.BIOME, PromisedEnd.id("hollow_fallback")))) bad.add("end_highlands isn't in #promisedend:hollow_fallback");
        if (hollowLands.value().getGenerationSettings().features().stream().noneMatch(step -> step.contains(hollows)))
            bad.add("hollow_cavern_fallback isn't among end_highlands' features (the loader's biome modifier didn't apply)");
        if (com.panda.promisedend.EndConfig.terrainEnabled()) {
            var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
            BlockPos top = new BlockPos(9900, end.getMaxY() - 50, 9600);
            for (BlockPos p : BlockPos.betweenClosed(top.offset(-16, -25, -16), top.offset(16, 0, 16))) end.setBlock(p, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 2);
            if (grove(end, "hollow_cavern_fallback").place(end, end.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(801), top.below(26)))
                bad.add("the terrain-off cavern carved with terrain on");
            for (BlockPos p : BlockPos.betweenClosed(top.offset(-16, -26, -16), top.offset(16, 1, 16))) end.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        // the Reefs' growths on vanilla's midlands, through the same loader hook
        var reefs = access.lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("purpur_reef_fallback")));
        var middle = biomes.getOrThrow(net.minecraft.world.level.biome.Biomes.END_MIDLANDS);
        if (!middle.is(net.minecraft.tags.TagKey.create(Registries.BIOME, PromisedEnd.id("reef_fallback")))) bad.add("end_midlands isn't in #promisedend:reef_fallback");
        if (middle.value().getGenerationSettings().features().stream().noneMatch(step -> step.contains(reefs)))
            bad.add("purpur_reef_fallback isn't among end_midlands' features (the loader's biome modifier didn't apply)");
        if (com.panda.promisedend.EndConfig.terrainEnabled()) {
            var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
            BlockPos at = new BlockPos(9900, end.getMaxY() - 60, 9000);
            floor(end, at, 14, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            if (grove(end, "purpur_reef_fallback").place(end, end.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(701), at))
                bad.add("the terrain-off reef built with terrain on");
            clearDeep(end, at);
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
     * With terrain off, the Skyfall's copies run as worldgen runs them, their biome check included (so the loader's biome
     * modifier): on a chunk of end stone high in the End's sky (one no structure has claimed), made small_end_islands, blossoms in 4 seeds and a vent
     * sunk into the chunk's edge in 8 (one chunk in two); the same chunk in the test level, outside the End, neither.
     */
    private static List<String> fallbackSkyfall(GameTestHelper helper) {
        List<String> bad = new ArrayList<>();
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        // a vent keeps out of a chunk a structure has claimed (out here, an End City's): a chunk none has, near by
        BlockPos origin = null;
        for (int i = 0; i < 16 && origin == null; i++) {
            BlockPos at = new BlockPos(9408 + 64 * i, end.getMaxY() - 60, 9600);
            if (!com.panda.promisedend.worldgen.StructureClaims.claimed(end, BlockPos.betweenClosed(at, at.offset(15, 0, 15)))) origin = at;
        }
        if (origin == null) {
            bad.add("no chunk free of structures along x 9408-10368 at z 9600 to test the Skyfall's copies on");
            return bad;
        }
        int[] inEnd = skyfallChunk(end, origin);
        PromisedEnd.LOGGER.info("[terrain_off_falls_back] the Skyfall's copies on a chunk of small islands: {} blossom spots in 4 seeds, {} vents in 8", inEnd[0], inEnd[1]);
        if (inEnd[0] == 0) bad.add("no fallback Drift Blossom on a chunk of small islands' end stone in 4 seeds");
        if (inEnd[1] == 0) bad.add("no fallback Updraft Vent on a chunk of small islands' end stone in 8 seeds");
        var test = helper.getLevel();
        int[] away = skyfallChunk(test, new BlockPos(9408, test.getMaxY() - 60, 9600));
        if (away[0] + away[1] != 0) bad.add("outside the End the Skyfall's copies built " + away[0] + " blossoms and " + away[1] + " vents");
        return bad;
    }

    /**
     * One chunk from {@code origin} (its corner, at the floor's height): made small_end_islands, floored with end stone
     * two deep with the void round it, then the blossom copy run in 4 seeds and the vent copy in 8, each vent taken off
     * again. Gives how many spots the blossoms built at and how many vents were built; the chunk is cleared after.
     */
    private static int[] skyfallChunk(net.minecraft.server.level.ServerLevel level, BlockPos origin) {
        var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        var blossoms = placed.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("drift_blossom_fallback")));
        var vents = placed.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("updraft_vent_fallback")));
        fillBiome(level, origin.offset(8, 0, 8), "minecraft:small_end_islands");
        var stone = net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(0, -2, 0), origin.offset(15, -1, 15))) level.setBlock(p, stone, 2);
        int flowers = 0, built = 0;
        for (int seed = 0; seed < 4; seed++) flowers += placeAsWorldgen(level, blossoms, 800 + seed, origin);
        for (int seed = 0; seed < 8; seed++) {
            built += placeAsWorldgen(level, vents, 900 + seed, origin);
            for (BlockPos p : BlockPos.betweenClosed(origin.offset(0, -1, 0), origin.offset(15, -1, 15))) {
                if (level.getBlockState(p).is(ModBlocks.UPDRAFT_VENT.get())) level.setBlock(p, stone, 2);
            }
        }
        for (BlockPos p : BlockPos.betweenClosed(origin.offset(0, -2, 0), origin.offset(15, 1, 15))) {
            if (!level.getBlockState(p).isAir()) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        return new int[] {flowers, built};
    }

    /**
     * A placed feature run as worldgen runs it: its modifiers in order with the feature as the top feature (vanilla's
     * {@code place} has none, so a biome check throws), then the feature at each spot left. Each seed stands for a world
     * of its own (ChunkChanceFilter.asIfSeed). How many spots it built at.
     */
    private static int placeAsWorldgen(net.minecraft.server.level.ServerLevel level, net.minecraft.core.Holder<net.minecraft.world.level.levelgen.placement.PlacedFeature> placed,
                                       long seed, BlockPos origin) {
        var generator = level.getChunkSource().getGenerator();
        // seeds mixed: consecutive seeds give correlated first rolls (a one-in-two rarity failed in all eight)
        var random = net.minecraft.util.RandomSource.create(net.minecraft.util.Mth.murmurHash3Mixer((int) seed));
        var context = new net.minecraft.world.level.levelgen.placement.PlacementContext(level, generator, java.util.Optional.of(placed.value()));
        // each test seed a world of its own: a chunk chance draws from the world's seed, not the random
        List<List<BlockPos>> out = new ArrayList<>();
        com.panda.promisedend.worldgen.ChunkChanceFilter.asIfSeed(net.minecraft.util.Mth.murmurHash3Mixer((int) seed) * 31L + seed, () -> {
            List<BlockPos> spots = List.of(origin);
            for (var modifier : placed.value().placement()) {
                List<BlockPos> next = new ArrayList<>();
                for (BlockPos p : spots) modifier.modify(context, random, p, next::add);
                spots = next;
            }
            out.add(spots);
        });
        int built = 0;
        for (BlockPos p : out.get(0)) if (placed.value().feature().value().place(level, generator, random, p)) built++;
        return built;
    }

    /**
     * With terrain off, the Voidscar's fallback scar in the End, run as worldgen runs it (its rarity and its biome check
     * included): on a chunk of end stone four deep high in the End's sky, made end_barrens, a scar with Voidsteel Scrap
     * Ore in its walls (the only scrap there is then) in 8 seeds; the same in the test level, outside the End, none.
     * Null when it holds.
     */
    private static String fallbackScars(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) return "no End level";
        String result = null;
        for (var level : List.of(end, helper.getLevel())) {
            var scar = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("rift_scar_fallback")));
            // a chunk's corner, at the floor's height: its middle is floored 16 round, so a scar has its whole reach
            BlockPos corner = new BlockPos(9792, level.getMaxY() - 60, 8992), at = corner.offset(8, 0, 8);
            fillBiome(level, at, "minecraft:end_barrens");
            int placed = 0, ore = 0;
            for (int seed = 0; seed < 8; seed++) {
                // a fresh floor for each seed, the ore counted before the next
                for (int d = 0; d < 4; d++) floor(level, at.below(d), 16, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
                placed += placeAsWorldgen(level, scar, 950 + seed, corner);
                for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, -4, -16), at.offset(16, -1, 16))) if (level.getBlockState(p).is(ModBlocks.VOIDSTEEL_SCRAP_ORE.get())) ore++;
            }
            clearDeep(level, at);
            PromisedEnd.LOGGER.info("[terrain_off_falls_back] the Voidscar's fallback scar in {}: {} scars in 8 seeds, {} Voidsteel Scrap Ore", level.dimension().identifier(), placed, ore);
            if (level == end && (placed == 0 || ore == 0)) result = "the Voidscar's fallback scar with terrain off: " + placed + " scars in 8 seeds, " + ore + " ore";
            if (level != end && placed != 0) result = "the Voidscar's fallback scar built outside the End";
        }
        return result;
    }

    /**
     * With terrain off, the Reefs' fallback in the End, run as worldgen runs it (its rarity, heightmap and biome check
     * included): on a chunk of end stone high in the End's sky, made end_midlands, growths of Ancient Purpur (the only
     * Ancient Purpur there is then) in 6 seeds; the same in the test level, outside the End, none. Null when it holds.
     */
    private static String fallbackReefs(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) return "no End level";
        String result = null;
        for (var level : List.of(end, helper.getLevel())) {
            var reef = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("purpur_reef_fallback")));
            BlockPos corner = new BlockPos(9920, level.getMaxY() - 60, 8992), at = corner.offset(8, 0, 8);
            fillBiome(level, at, "minecraft:end_midlands");
            int placed = 0, coral = 0;
            for (int seed = 0; seed < 6; seed++) {
                floor(level, at, 16, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
                placed += placeAsWorldgen(level, reef, 960 + seed, corner);
                for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, 0, -16), at.offset(16, 14, 16))) {
                    if (level.getBlockState(p).is(ModBlocks.ANCIENT_PURPUR_SET.base().get())) coral++;
                }
                clearDeep(level, at);
                for (var s : level.getEntitiesOfClass(net.minecraft.world.entity.monster.Shulker.class, new net.minecraft.world.phys.AABB(at).inflate(17))) s.discard();
            }
            PromisedEnd.LOGGER.info("[terrain_off_falls_back] the Reefs' fallback in {}: {} reefs in 6 seeds, {} Ancient Purpur", level.dimension().identifier(), placed, coral);
            if (level == end && (placed == 0 || coral == 0)) result = "the Reefs' fallback with terrain off: " + placed + " reefs in 6 seeds, " + coral + " Ancient Purpur";
            if (level != end && placed != 0) result = "the Reefs' fallback built outside the End";
        }
        return result;
    }

    /**
     * With terrain off, Bloom Meadows' flowers on vanilla's midlands ({@code #promisedend:bloom_fallback}, through each
     * loader's hook), run as worldgen runs them (their count, heightmap and biome check included): on a chunk of end
     * stone high in the End's sky, made end_midlands, Ender Blooms and Starflowers in 4 seeds; the same in the test
     * level, outside the End, none. Null when it holds.
     */
    private static String fallbackBlooms(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) return "no End level";
        var access = end.registryAccess();
        var midlands = access.lookupOrThrow(Registries.BIOME).getOrThrow(net.minecraft.world.level.biome.Biomes.END_MIDLANDS);
        if (!midlands.is(net.minecraft.tags.TagKey.create(Registries.BIOME, PromisedEnd.id("bloom_fallback")))) return "end_midlands isn't in #promisedend:bloom_fallback";
        String result = null;
        for (String name : List.of("ender_bloom_fallback", "starflower_fallback")) {
            var feature = access.lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id(name)));
            if (midlands.value().getGenerationSettings().features().stream().noneMatch(step -> step.contains(feature)))
                return name + " isn't among end_midlands' features (the loader's biome modifier didn't apply)";
        }
        for (var level : List.of(end, helper.getLevel())) {
            var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
            BlockPos corner = new BlockPos(10176, level.getMaxY() - 60, 8992), at = corner.offset(8, 0, 8);
            fillBiome(level, at, "minecraft:end_midlands");
            int spots = 0, blooms = 0, stars = 0;
            floor(level, at, 16, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            for (int seed = 0; seed < 4; seed++) {
                // each its own seed, as worldgen gives each feature its own
                for (String name : List.of("ender_bloom_fallback", "starflower_fallback")) {
                    spots += placeAsWorldgen(level, placed.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id(name))), 1010 + seed + 100 * name.length(), corner);
                }
            }
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, 0, -16), at.offset(16, 1, 16))) {
                if (level.getBlockState(p).is(ModBlocks.ENDER_BLOOM.get())) blooms++;
                if (level.getBlockState(p).is(ModBlocks.STARFLOWER.get())) stars++;
            }
            clear(level, at);
            PromisedEnd.LOGGER.info("[terrain_off_falls_back] the Meadows' fallback in {}: {} spots in 4 seeds, {} Ender Blooms, {} Starflowers", level.dimension().identifier(), spots, blooms, stars);
            if (level == end && (blooms == 0 || stars == 0)) result = "the Meadows' fallback with terrain off: " + blooms + " Ender Blooms, " + stars + " Starflowers in 4 seeds";
            if (level != end && blooms + stars != 0) result = "the Meadows' fallback built outside the End";
        }
        return result;
    }

    /**
     * The rest of the Wilds' plants. Chorus Leaves are leaves (#minecraft:leaves), shears keep them, 4000 breaks by hand
     * give Chorus Sprouts at 5 % and chorus fruit at 3 % (within about four spreads of each), and a leaf with no log
     * near decays while one by a log stays; a sprout stands on end stone and the mosses, not on stone, grows into a tree
     * of chorus log and leaves, and grows by random ticks in the dark (a sealed cell), where a vanilla sapling in a cell
     * beside it doesn't. A Chorus Vine hangs from the leaves and from itself, not from stone, is climbable, grows a block with bone
     * meal, and breaks with its anchor. Void Mycelium drops end stone, itself to Silk Touch. A Void Mushroom stands on
     * Void Mycelium, not on end stone, and grows huge (caps on a stem); a cap gives 0-2 mushrooms, itself to Silk Touch.
     * A Void Berry Vine hangs under end stone, gives light 10 while it bears and none once picked, a use picks a Void
     * Berry, bone meal brings the berries back, a picked vine drops nothing, and new vine bears one block in three (3000
     * grown, 850-1150 bearing); the berries plant the vine and are food. Two Void Mushrooms and a bowl make the stew (6 food, ten seconds of Slow
     * Falling, the bowl back), three Chorus Vines a string.
     */
    private static void wildsPlantsRules(GameTestHelper helper) {
        var level = helper.getLevel();
        var random = level.getRandom();
        List<String> bad = new ArrayList<>();
        var air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        var endStone = net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState();
        var stone = net.minecraft.world.level.block.Blocks.STONE.defaultBlockState();
        var silk = new ItemStack(net.minecraft.world.item.Items.NETHERITE_PICKAXE);
        silk.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH), 1);
        java.util.function.BiFunction<net.minecraft.world.level.block.state.BlockState, ItemStack, List<ItemStack>> drops = (state, tool) ->
                net.minecraft.world.level.block.Block.getDrops(state, level, BlockPos.ZERO, null, null, tool);
        // the leaves
        var leaves = ModBlocks.CHORUS_LEAVES.get().defaultBlockState();
        if (!leaves.is(net.minecraft.tags.BlockTags.LEAVES)) bad.add("Chorus Leaves aren't in #minecraft:leaves");
        var sheared = drops.apply(leaves, new ItemStack(net.minecraft.world.item.Items.SHEARS));
        if (sheared.size() != 1 || !sheared.get(0).is(ModBlocks.CHORUS_LEAVES.get().asItem())) bad.add("sheared leaves drop " + sheared);
        int sprouts = 0, fruit = 0;
        for (int i = 0; i < 4000; i++) {
            for (var stack : drops.apply(leaves, ItemStack.EMPTY)) {
                if (stack.is(ModBlocks.CHORUS_SPROUT.get().asItem())) sprouts += stack.getCount();
                if (stack.is(net.minecraft.world.item.Items.CHORUS_FRUIT)) fruit += stack.getCount();
            }
        }
        PromisedEnd.LOGGER.info("[wilds_plants_rules] 4000 leaves by hand: {} sprouts, {} chorus fruit", sprouts, fruit);
        // 200 and 120 expected, spreads of 14 and 11
        if (sprouts < 145 || sprouts > 255 || fruit < 75 || fruit > 165) bad.add("4000 leaves by hand gave " + sprouts + " sprouts (200 expected) and " + fruit + " chorus fruit (120)");
        {
            BlockPos lone = new BlockPos(2560, level.getMaxY() - 40, 2600), log = lone.offset(0, 0, 8);
            level.setBlock(lone, leaves, 3);
            level.setBlock(log, ModBlocks.CHORUS_LOG.get().defaultBlockState(), 3);
            level.setBlock(log.above(), leaves, 3);
            for (BlockPos p : List.of(lone, log.above())) level.getBlockState(p).tick(level, p, random);
            for (BlockPos p : List.of(lone, log.above())) level.getBlockState(p).randomTick(level, p, random);
            if (!level.getBlockState(lone).isAir()) bad.add("a leaf with no log near didn't decay");
            if (!level.getBlockState(log.above()).is(ModBlocks.CHORUS_LEAVES.get())) bad.add("a leaf on a log decayed");
            for (BlockPos p : List.of(lone, log, log.above())) level.setBlock(p, air, 3);
            level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(lone).inflate(12)).forEach(e -> e.discard());
        }
        // the sprout: where it stands, and a tree from it in the open sky
        var sprout = ModBlocks.CHORUS_SPROUT.get();
        int x = 1;
        for (var ground : List.of(endStone, ModBlocks.CHORUS_MOSS.get().defaultBlockState(), stone)) {
            BlockPos at = helper.absolutePos(new BlockPos(x++, 2, 1));
            level.setBlock(at.below(), ground, 3);
            boolean want = ground != stone;
            if (sprout.defaultBlockState().canSurvive(level, at) != want) bad.add("the sprout " + (want ? "can't stand on " : "stands on ") + ground.getBlock().getName().getString());
            level.setBlock(at.below(), air, 3);
        }
        BlockPos tree = new BlockPos(2600, level.getMaxY() - 60, 2600);
        floor(level, tree, 6, endStone);
        level.setBlock(tree, sprout.defaultBlockState(), 3);
        for (int i = 0; i < 12 && level.getBlockState(tree).is(sprout); i++) sprout.advanceTree(level, tree, level.getBlockState(tree), random);
        int logs = 0, leafBlocks = 0;
        for (BlockPos p : BlockPos.betweenClosed(tree.offset(-8, 0, -8), tree.offset(8, 20, 8))) {
            if (level.getBlockState(p).is(ModBlocks.CHORUS_LOG.get())) logs++;
            if (level.getBlockState(p).is(ModBlocks.CHORUS_LEAVES.get())) leafBlocks++;
        }
        if (logs == 0 || leafBlocks == 0) bad.add("the sprout grew " + logs + " logs and " + leafBlocks + " leaves");
        clear(level, tree);
        // in the dark (a sealed cell of stone: no sky light, no block light), by random ticks alone, as under the Canopy's
        // crowns or in a cave: a vanilla sapling in a cell beside it never grows. The open End has sky light, so a cell
        // with room for the tree: 13 across and 18 high inside
        BlockPos dark = new BlockPos(2600, level.getMaxY() - 100, 2700), control = dark.offset(20, 0, 0);
        for (BlockPos cell : List.of(dark, control)) {
            for (BlockPos p : BlockPos.betweenClosed(cell.offset(-7, -1, -7), cell.offset(7, 18, 7))) level.setBlock(p, stone, 2);
            for (BlockPos p : BlockPos.betweenClosed(cell.offset(-6, 0, -6), cell.offset(6, 17, 6))) level.setBlock(p, air, 2);
        }
        level.setBlock(dark.below(), endStone, 3);
        level.setBlock(control.below(), net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(), 3);
        level.setBlock(dark, sprout.defaultBlockState(), 3);
        level.setBlock(control, net.minecraft.world.level.block.Blocks.OAK_SAPLING.defaultBlockState(), 3);
        // the Chorus Vine
        var vine = ModBlocks.CHORUS_VINE.get();
        BlockPos anchor = new BlockPos(2640, level.getMaxY() - 40, 2600);
        level.setBlock(anchor, stone, 3);
        if (vine.defaultBlockState().canSurvive(level, anchor.below())) bad.add("a Chorus Vine hangs from stone");
        level.setBlock(anchor, leaves.setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 3);
        if (!vine.defaultBlockState().canSurvive(level, anchor.below())) bad.add("a Chorus Vine can't hang from the leaves");
        level.setBlock(anchor.below(), vine.defaultBlockState(), 3);
        if (!vine.defaultBlockState().canSurvive(level, anchor.below(2))) bad.add("a Chorus Vine can't hang from another");
        if (!vine.defaultBlockState().is(net.minecraft.tags.BlockTags.CLIMBABLE)) bad.add("a Chorus Vine isn't climbable");
        vine.performBonemeal(level, random, anchor.below(), vine.defaultBlockState(), net.minecraft.world.level.block.BonemealSource.INTERACTION);
        if (!level.getBlockState(anchor.below(2)).is(vine)) bad.add("bone meal didn't lengthen the vine");
        level.setBlock(anchor, air, 3);
        for (int dy = 1; dy <= 2; dy++) level.getBlockState(anchor.below(dy)).tick(level, anchor.below(dy), random);
        if (!level.getBlockState(anchor.below()).isAir() || !level.getBlockState(anchor.below(2)).isAir()) bad.add("the vine stayed when its anchor went");
        level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(anchor).inflate(4)).forEach(e -> e.discard());
        // the mycelium
        var mycelium = ModBlocks.VOID_MYCELIUM.get().defaultBlockState();
        var bare = drops.apply(mycelium, ItemStack.EMPTY);
        if (bare.size() != 1 || !bare.get(0).is(net.minecraft.world.item.Items.END_STONE)) bad.add("Void Mycelium by hand drops " + bare);
        var silked = drops.apply(mycelium, silk);
        if (silked.size() != 1 || !silked.get(0).is(mycelium.getBlock().asItem())) bad.add("Void Mycelium with Silk Touch drops " + silked);
        // the Void Mushroom
        var mushroom = ModBlocks.VOID_MUSHROOM.get();
        BlockPos shroom = new BlockPos(2680, level.getMaxY() - 60, 2600);
        floor(level, shroom, 4, endStone);
        if (mushroom.defaultBlockState().canSurvive(level, shroom)) bad.add("a Void Mushroom stands on end stone");
        floor(level, shroom, 4, ModBlocks.VOID_MYCELIUM.get().defaultBlockState());
        if (!mushroom.defaultBlockState().canSurvive(level, shroom)) bad.add("a Void Mushroom can't stand on Void Mycelium");
        level.setBlock(shroom, mushroom.defaultBlockState(), 3);
        boolean grown = false;
        for (int i = 0; i < 10 && !grown; i++) grown = mushroom.growMushroom(level, shroom, level.getBlockState(shroom), random);
        int caps = 0, stems = 0;
        for (BlockPos p : BlockPos.betweenClosed(shroom.offset(-4, 0, -4), shroom.offset(4, 12, 4))) {
            if (level.getBlockState(p).is(ModBlocks.VOID_MUSHROOM_BLOCK.get())) caps++;
            if (level.getBlockState(p).is(ModBlocks.VOID_MUSHROOM_STEM.get())) stems++;
        }
        if (!grown || caps == 0 || stems == 0) bad.add("the mushroom grew " + caps + " caps on " + stems + " stems");
        clear(level, shroom);
        var cap = ModBlocks.VOID_MUSHROOM_BLOCK.get().defaultBlockState();
        int most = 0, total = 0;
        for (int i = 0; i < 200; i++) {
            int n = drops.apply(cap, ItemStack.EMPTY).stream().filter(s -> s.is(mushroom.asItem())).mapToInt(ItemStack::getCount).sum();
            most = Math.max(most, n);
            total += n;
        }
        if (most > 2 || total == 0) bad.add("200 caps gave " + total + " mushrooms, at most " + most + " from one");
        var kept = drops.apply(cap, silk);
        if (kept.size() != 1 || !kept.get(0).is(cap.getBlock().asItem())) bad.add("a cap with Silk Touch drops " + kept);
        // the Void Berry Vine and its berries
        var berry = ModBlocks.VOID_BERRY_VINE.get();
        var bearing = berry.defaultBlockState().setValue(com.panda.promisedend.block.VoidBerryVineBlock.BERRIES, true);
        BlockPos islet = new BlockPos(2720, level.getMaxY() - 40, 2600);
        level.setBlock(islet, endStone, 3);
        if (!bearing.canSurvive(level, islet.below())) bad.add("a Void Berry Vine can't hang under end stone");
        level.setBlock(islet.below(), bearing, 3);
        if (bearing.getLightEmission() != com.panda.promisedend.block.VoidBerryVineBlock.LIT || berry.defaultBlockState().getLightEmission() != 0)
            bad.add("the berry vine's light: " + bearing.getLightEmission() + " bearing, " + berry.defaultBlockState().getLightEmission() + " picked");
        var picker = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        level.getBlockState(islet.below()).useWithoutItem(level, picker,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(islet.below()), net.minecraft.core.Direction.NORTH, islet.below(), false));
        var popped = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(islet).inflate(3));
        if (level.getBlockState(islet.below()).getValue(com.panda.promisedend.block.VoidBerryVineBlock.BERRIES)) bad.add("a use didn't pick the berries");
        if (popped.stream().noneMatch(e -> e.getItem().is(ModItems.VOID_BERRIES.get()))) bad.add("picking popped " + popped.stream().map(e -> e.getItem()).toList());
        popped.forEach(e -> e.discard());
        var picked = level.getBlockState(islet.below());
        if (!berry.isValidBonemealTarget(level, islet.below(), picked, net.minecraft.world.level.block.BonemealSource.INTERACTION)) bad.add("bone meal can't take a picked vine");
        berry.performBonemeal(level, random, islet.below(), picked, net.minecraft.world.level.block.BonemealSource.INTERACTION);
        if (!level.getBlockState(islet.below()).getValue(com.panda.promisedend.block.VoidBerryVineBlock.BERRIES)) bad.add("bone meal didn't bring the berries back");
        level.setBlock(islet.below(), air, 3);
        level.setBlock(islet, air, 3);
        var berryLoot = drops.apply(bearing, ItemStack.EMPTY);
        if (berryLoot.size() != 1 || !berryLoot.get(0).is(ModItems.VOID_BERRIES.get())) bad.add("a bearing vine drops " + berryLoot);
        var pickedLoot = drops.apply(berry.defaultBlockState(), ItemStack.EMPTY);
        if (!pickedLoot.isEmpty()) bad.add("a picked vine drops " + pickedLoot);
        int bore = 0;
        for (int i = 0; i < 3000; i++) if (berry.grown(random).getValue(com.panda.promisedend.block.VoidBerryVineBlock.BERRIES)) bore++;
        if (bore < 850 || bore > 1150) bad.add(bore + " of 3000 new vine blocks bear (1000 expected)");
        if (ModItems.VOID_BERRIES.get().getBlock() != berry) bad.add("Void Berries plant " + ModItems.VOID_BERRIES.get().getBlock());
        if (new ItemStack(ModItems.VOID_BERRIES.get()).get(net.minecraft.core.component.DataComponents.FOOD) == null) bad.add("Void Berries aren't food");
        // the recipes
        var recipes = level.getServer().getRecipeManager();
        java.util.function.Function<List<ItemStack>, ItemStack> craft = in -> {
            var input = net.minecraft.world.item.crafting.CraftingInput.of(in.size(), 1, in);
            return recipes.getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level).map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
        };
        var stew = craft.apply(List.of(new ItemStack(mushroom), new ItemStack(mushroom), new ItemStack(net.minecraft.world.item.Items.BOWL)));
        if (!stew.is(ModItems.VOID_MUSHROOM_STEW.get())) bad.add("two Void Mushrooms and a bowl make " + stew);
        else {
            var food = stew.get(net.minecraft.core.component.DataComponents.FOOD);
            var eat = stew.get(net.minecraft.core.component.DataComponents.CONSUMABLE);
            var rest = stew.get(net.minecraft.core.component.DataComponents.USE_REMAINDER);
            boolean falls = eat != null && eat.onConsumeEffects().stream().anyMatch(e -> e instanceof net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect a
                    && a.effects().stream().anyMatch(m -> m.is(net.minecraft.world.effect.MobEffects.SLOW_FALLING) && m.getDuration() == 200));
            if (food == null || food.nutrition() != 6 || !falls || rest == null || !rest.convertInto().is(net.minecraft.world.item.Items.BOWL))
                bad.add("the stew: food " + food + ", Slow Falling " + falls + ", leaves " + (rest == null ? null : rest.convertInto()));
        }
        var string = craft.apply(List.of(new ItemStack(vine), new ItemStack(vine), new ItemStack(vine)));
        if (!string.is(net.minecraft.world.item.Items.STRING)) bad.add("three Chorus Vines make " + string);
        if (!bad.isEmpty()) {
            helper.fail("wilds plants: " + bad);
            return;
        }
        // the light engine runs off the tick, slower under load: tick the cells once their dark is in, whenever that is
        boolean[] done = {false};
        helper.onEachTick(() -> {
            int light = Math.max(level.getMaxLocalRawBrightness(dark.above()), level.getMaxLocalRawBrightness(control.above()));
            if (done[0] || light >= 9) return;
            done[0] = true;
            int ticks = 0;
            for (; ticks < 400 && level.getBlockState(dark).is(sprout); ticks++) {
                level.getBlockState(dark).randomTick(level, dark, random);
                if (level.getBlockState(control).is(net.minecraft.world.level.block.Blocks.OAK_SAPLING)) level.getBlockState(control).randomTick(level, control, random);
            }
            boolean oak = level.getBlockState(control).is(net.minecraft.world.level.block.Blocks.OAK_SAPLING);
            PromisedEnd.LOGGER.info("[wilds_plants_rules] a sprout at light {} grew in {} random ticks; the oak sapling beside it {}", light, ticks, oak ? "didn't" : "did");
            List<String> dim = new ArrayList<>();
            if (!oak) dim.add("a vanilla sapling grew in the dark, so the case can't tell");
            if (level.getBlockState(dark).is(sprout)) dim.add("a sprout in the dark didn't grow in 400 random ticks");
            clear(level, dark);
            clear(level, control);
            if (!dim.isEmpty()) helper.fail("wilds plants: " + dim);
            else helper.succeed();
        });
    }

    /**
     * The rest of the Wilds' features, run as worldgen runs them (their counts, heightmaps and biome checks) in 4 seeds,
     * each in its biome: on a floor of end stone in the sky, the Chorus Canopy's trees (log and leaves) and vines under
     * them; the Void Fungus Barrens' mycelium, huge mushrooms (caps on stems) and small ones; and under a slab of end
     * stone at y 91, the Hanging Gardens' Void Berry Vines, some of them bearing.
     */
    private static void wildsFeaturesGrow(GameTestHelper helper) {
        var level = helper.getLevel();
        var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        List<String> bad = new ArrayList<>();
        record Case(String biome, List<String> features, List<net.minecraft.world.level.block.Block> counted, int y) {}
        var cases = List.of(
                new Case("chorus_canopy", List.of("chorus_canopy_tree", "chorus_vines"), List.of(ModBlocks.CHORUS_LOG.get(), ModBlocks.CHORUS_LEAVES.get(), ModBlocks.CHORUS_VINE.get()), 0),
                new Case("void_fungus_barrens", List.of("void_mycelium_patch", "huge_void_mushroom", "void_mushroom_scatter"),
                        List.of(ModBlocks.VOID_MYCELIUM.get(), ModBlocks.VOID_MUSHROOM_BLOCK.get(), ModBlocks.VOID_MUSHROOM_STEM.get(), ModBlocks.VOID_MUSHROOM.get()), 0),
                new Case("hanging_gardens", List.of("void_berry_vines"), List.of(ModBlocks.VOID_BERRY_VINE.get()), 92));
        for (int i = 0; i < cases.size(); i++) {
            var c = cases.get(i);
            BlockPos corner = new BlockPos(11200, c.y() == 0 ? level.getMaxY() - 60 : c.y(), 9056 + 64 * i), at = corner.offset(8, 0, 8);
            fillBiome(level, at, "promisedend:" + c.biome());
            fillBiome(level, at.above(12), "promisedend:" + c.biome());
            if (c.y() != 0) fillBiome(level, new BlockPos(at.getX(), 75, at.getZ()), "promisedend:" + c.biome());
            floor(level, at, 16, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            for (int seed = 0; seed < 4; seed++) {
                for (String name : c.features()) {
                    placeAsWorldgen(level, placed.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id(name))), 3030 + seed + 100 * name.length(), corner);
                }
            }
            java.util.Map<net.minecraft.world.level.block.Block, Integer> found = new java.util.LinkedHashMap<>();
            int bearing = 0, reachable = 0, tips = 0;
            for (var b : c.counted()) found.put(b, 0);
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, -16, -16), at.offset(16, 24, 16))) {
                var state = level.getBlockState(p);
                if (found.containsKey(state.getBlock())) found.merge(state.getBlock(), 1, Integer::sum);
                if (state.is(ModBlocks.VOID_BERRY_VINE.get()) && state.getValue(com.panda.promisedend.block.VoidBerryVineBlock.BERRIES)) bearing++;
                // a Chorus Vine's tip a player standing on the ground can take hold of: ground one or two blocks under it
                if (state.is(ModBlocks.CHORUS_VINE.get()) && !level.getBlockState(p.below()).is(ModBlocks.CHORUS_VINE.get())) {
                    tips++;
                    if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP)
                            || level.getBlockState(p.below()).isAir() && level.getBlockState(p.below(2)).isFaceSturdy(level, p.below(2), net.minecraft.core.Direction.UP))
                        reachable++;
                }
            }
            String extra = c.y() != 0 ? ", " + bearing + " bearing" : c.biome().equals("chorus_canopy") ? ", " + reachable + " of " + tips + " vines reach to the ground" : "";
            PromisedEnd.LOGGER.info("[wilds_features_grow] {} in 4 seeds: {}{}", c.biome(), found, extra);
            for (var e : found.entrySet()) if (e.getValue() == 0) bad.add(c.biome() + " built no " + e.getKey().getName().getString());
            if (c.y() != 0 && bearing == 0) bad.add(c.biome() + "'s vines bear no berries");
            if (c.biome().equals("chorus_canopy") && reachable < 4) bad.add("only " + reachable + " of " + tips + " Chorus Vines reach down to where a player can climb them");
            clear(level, at.below(16));
            clear(level, at.above(20));
        }
        if (!bad.isEmpty()) helper.fail("wilds features: " + bad);
        helper.succeed();
    }

    /**
     * The Chorus Canopy's trees and vines, run as worldgen runs them (their filters included) in 4 seeds on a floor of end
     * stone made the Canopy: with an End City's claim on the floor's chunk, none; on the same floor unclaimed (the
     * control), trees and vines. The Canopy is a highlands biome, where vanilla's End Cities stand.
     */
    private static void canopyKeepsOut(GameTestHelper helper) {
        var level = helper.getLevel();
        var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
        BlockPos corner = new BlockPos(11264, level.getMaxY() - 60, 9600), at = corner.offset(8, 0, 8);
        fillBiome(level, at, "promisedend:chorus_canopy");
        fillBiome(level, at.above(12), "promisedend:chorus_canopy");
        var structure = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).getValueOrThrow(net.minecraft.world.level.levelgen.structure.BuiltinStructures.END_CITY);
        int[] built = new int[2];
        for (int claimed = 1; claimed >= 0; claimed--) {
            floor(level, at, 16, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
            var chunk = level.getChunk(at);
            var before = new java.util.HashMap<>(chunk.getAllReferences());
            if (claimed == 1) chunk.addReferenceForStructure(structure, net.minecraft.world.level.ChunkPos.pack(at.getX() >> 4, at.getZ() >> 4));
            for (int seed = 0; seed < 4; seed++) {
                for (String name : List.of("chorus_canopy_tree", "chorus_vines")) {
                    placeAsWorldgen(level, placed.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id(name))), 3030 + seed + 100 * name.length(), corner);
                }
            }
            chunk.setAllReferences(before);
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-24, -2, -24), at.offset(24, 30, 24))) {
                var state = level.getBlockState(p);
                if (state.is(ModBlocks.CHORUS_LOG.get()) || state.is(ModBlocks.CHORUS_LEAVES.get()) || state.is(ModBlocks.CHORUS_VINE.get())) built[claimed]++;
            }
            clear(level, at.below(16));
            clear(level, at.above(20));
        }
        PromisedEnd.LOGGER.info("[canopy_keeps_out] canopy blocks in 4 seeds: {} on a claimed chunk, {} on the unclaimed control", built[1], built[0]);
        if (built[1] > 0) helper.fail(built[1] + " canopy blocks in an End City's chunk");
        if (built[0] == 0) helper.fail("no canopy on the unclaimed control, so the claim case can't tell");
        helper.succeed();
    }

    /**
     * With terrain off, the rest of the Wilds on vanilla's biomes (through each loader's hook), run as worldgen runs them
     * in 6 seeds: Chorus Canopy trees and their vines on a floor of end stone made end_highlands, Void Mycelium (and its
     * mushrooms) on one made end_barrens, and Void Berry Vines under a slab of end stone at y 91 made small_end_islands;
     * the same in the test level, outside the End, none. Null when it holds.
     */
    private static String fallbackWilds(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) return "no End level";
        var access = end.registryAccess();
        record Case(ResourceKey<net.minecraft.world.level.biome.Biome> biome, String tag, List<String> features, net.minecraft.world.level.block.Block counted, int y) {}
        var cases = List.of(
                new Case(net.minecraft.world.level.biome.Biomes.END_HIGHLANDS, "canopy_fallback", List.of("chorus_canopy_tree_fallback", "chorus_vines_fallback"), ModBlocks.CHORUS_LEAVES.get(), 0),
                new Case(net.minecraft.world.level.biome.Biomes.END_BARRENS, "fungus_fallback", List.of("void_mycelium_fallback", "void_mushroom_fallback"), ModBlocks.VOID_MYCELIUM.get(), 0),
                new Case(net.minecraft.world.level.biome.Biomes.END_BARRENS, "fungus_fallback", List.of("void_mycelium_fallback", "void_mushroom_fallback"), ModBlocks.VOID_MUSHROOM_BLOCK.get(), 0),
                new Case(net.minecraft.world.level.biome.Biomes.SMALL_END_ISLANDS, "garden_fallback", List.of("void_berry_vines_fallback"), ModBlocks.VOID_BERRY_VINE.get(), 92));
        String result = null;
        for (int i = 0; i < cases.size(); i++) {
            var c = cases.get(i);
            var biome = access.lookupOrThrow(Registries.BIOME).getOrThrow(c.biome());
            if (!biome.is(net.minecraft.tags.TagKey.create(Registries.BIOME, PromisedEnd.id(c.tag())))) return c.biome().identifier() + " isn't in #promisedend:" + c.tag();
            for (String name : c.features()) {
                var feature = access.lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id(name)));
                if (biome.value().getGenerationSettings().features().stream().noneMatch(step -> step.contains(feature)))
                    return name + " isn't among " + c.biome().identifier() + "'s features (the loader's biome modifier didn't apply)";
            }
            for (var level : List.of(end, helper.getLevel())) {
                var placed = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE);
                BlockPos corner = new BlockPos(10176, c.y() == 0 ? level.getMaxY() - 60 : c.y(), 9056 + 64 * i), at = corner.offset(8, 0, 8);
                String id = c.biome().identifier().toString();
                fillBiome(level, at, id);
                fillBiome(level, at.above(12), id);
                if (c.y() != 0) fillBiome(level, new BlockPos(at.getX(), 75, at.getZ()), id);
                floor(level, at, 16, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState());
                int spots = 0, built = 0;
                for (int seed = 0; seed < 6; seed++) {
                    for (String name : c.features()) {
                        spots += placeAsWorldgen(level, placed.getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id(name))), 2020 + seed + 100 * name.length(), corner);
                    }
                }
                for (BlockPos p : BlockPos.betweenClosed(at.offset(-16, -16, -16), at.offset(16, 24, 16))) if (level.getBlockState(p).is(c.counted())) built++;
                clear(level, at.below(16));
                clear(level, at.above(20));
                PromisedEnd.LOGGER.info("[terrain_off_falls_back] {}'s Wilds fallback in {}: {} spots in 6 seeds, {} {}", c.biome().identifier(),
                        level.dimension().identifier(), spots, built, c.counted().getName().getString());
                if (level == end && built == 0) result = c.biome().identifier() + "'s Wilds fallback with terrain off: no " + c.counted().getName().getString() + " in 6 seeds";
                if (level != end && built != 0) result = c.biome().identifier() + "'s Wilds fallback built outside the End";
            }
        }
        return result;
    }

    /**
     * With terrain off, the Hollow Highlands' fallback in the End, run as worldgen runs it (its rarity and biome check
     * included): in an island of end stone 49 across and 26 deep high in the End's sky, made end_highlands, caverns
     * with Echo Amber in 6 seeds; the same in the test level, outside the End, none. Null when it holds.
     */
    private static String fallbackHollows(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) return "no End level";
        String result = null;
        for (var level : List.of(end, helper.getLevel())) {
            var cavern = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).getOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("hollow_cavern_fallback")));
            BlockPos top = new BlockPos(10056, level.getMaxY() - 50, 9000), corner = new BlockPos(10048, level.getMinY(), 8992);
            fillBiome(level, top, "minecraft:end_highlands");
            fillBiome(level, top.below(20), "minecraft:end_highlands");
            // and where its placement's biome check reads it, the bottom of the world
            fillBiome(level, new BlockPos(top.getX(), level.getMinY() + 8, top.getZ()), "minecraft:end_highlands");
            int placed = 0, air = 0, amber = 0;
            for (int seed = 0; seed < 6; seed++) {
                for (BlockPos p : BlockPos.betweenClosed(top.offset(-24, -25, -24), top.offset(24, 0, 24))) level.setBlock(p, net.minecraft.world.level.block.Blocks.END_STONE.defaultBlockState(), 2);
                placed += placeAsWorldgen(level, cavern, 970 + seed, corner);
                for (BlockPos p : BlockPos.betweenClosed(top.offset(-24, -25, -24), top.offset(24, 0, 24))) {
                    var state = level.getBlockState(p);
                    if (state.isAir()) air++;
                    if (state.is(ModBlocks.ECHO_AMBER_CLUSTER.get())) amber++;
                }
            }
            for (BlockPos p : BlockPos.betweenClosed(top.offset(-24, -26, -24), top.offset(24, 1, 24))) {
                if (!level.getBlockState(p).isAir()) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
            }
            PromisedEnd.LOGGER.info("[terrain_off_falls_back] the Highlands' fallback in {}: {} caverns in 6 seeds, {} air, {} Echo Amber", level.dimension().identifier(), placed, air, amber);
            if (level == end && (placed == 0 || amber == 0)) result = "the Highlands' fallback with terrain off: " + placed + " caverns in 6 seeds, " + amber + " Echo Amber";
            if (level != end && placed != 0) result = "the Highlands' fallback carved outside the End";
        }
        return result;
    }

    /** Clears 16 round {@code centre} from 5 under it to 1 over: a scar's floors are four deep. */
    private static void clearDeep(net.minecraft.server.level.ServerLevel level, BlockPos centre) {
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-17, -5, -17), centre.offset(17, 1, 17))) {
            if (!level.getBlockState(p).isAir()) level.setBlock(p, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
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
     * the islands are built 300 blocks out, high in the test level's sky. Then each shape and each wreck from the first
     * seed that plans it: a tilted top spans 3 or more blocks of height, a split island's second piece is at least
     * 30 % of its first (a cap's loose shards are far smaller), shards stand 3 or more over a level top, and each
     * wreck leaves its own blocks (no wreck, none of them), each resting on something; three seeds for each wreck.
     * Every island holds together: no piece of it, joined face to face, under 8 blocks (so no crumb, and no rim block
     * held only by an edge); nothing beside an untilted island stands over its dome; each shape's islands are 62 % or
     * more Voidstone, and all of them 66-75 %. Over 3000 plans the shapes come 4:3:2:2 (4:3:0:2 under 13 across, which
     * aren't split) and a wreck on one island in six of 8 or more across, within 2.5 points.
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
        var rim = (com.panda.promisedend.worldgen.RimDebrisFeature) feature;
        // the twelve on seeds from 77-88, then the first seed from 1000 for each shape and each wreck; each mixed, since
        // consecutive seeds give correlated first draws (every island came out the same size)
        java.util.function.LongUnaryOperator mix = k -> net.minecraft.util.RandomSource.create(k).nextLong();
        List<Long> seeds = new ArrayList<>();
        for (long k = 77; k < 89; k++) seeds.add(mix.applyAsLong(k));
        java.util.Set<Integer> radii = new java.util.TreeSet<>();
        var wantShapes = java.util.EnumSet.allOf(com.panda.promisedend.worldgen.RimDebrisFeature.Shape.class);
        var wantWrecks = java.util.EnumSet.allOf(com.panda.promisedend.worldgen.RimDebrisFeature.Wreck.class);
        // three of each wreck: one seed alone can miss what goes wrong on another
        java.util.Map<com.panda.promisedend.worldgen.RimDebrisFeature.Wreck, Integer> wreckSeeds = new java.util.EnumMap<>(com.panda.promisedend.worldgen.RimDebrisFeature.Wreck.class);
        int[] shapeCount = new int[4];
        // what each shape's share should be: 4:3:2:2 of 11, or 4:3:0:2 of 9 on an island 8 across (none split)
        double[] expected = new double[4];
        int wrecked = 0, wreckable = 0;
        for (long k = 1000; k < 4000; k++) {
            var plan = rim.plan(net.minecraft.util.RandomSource.create(mix.applyAsLong(k)));
            radii.add(plan.radius());
            if (plan.radius() >= 4) {
                double[] w = plan.radius() < 6 ? new double[] {4 / 9.0, 3 / 9.0, 0, 2 / 9.0} : new double[] {4 / 11.0, 3 / 11.0, 2 / 11.0, 2 / 11.0};
                for (int i = 0; i < 4; i++) expected[i] += w[i];
                if (plan.radius() < 6 && plan.shape() == com.panda.promisedend.worldgen.RimDebrisFeature.Shape.SPLIT) bad.add("an island under 13 across was split");
                shapeCount[plan.shape().ordinal()]++;
                wreckable++;
                if (plan.wreck() != com.panda.promisedend.worldgen.RimDebrisFeature.Wreck.NONE) wrecked++;
            }
            boolean moreWreck = plan.wreck() != com.panda.promisedend.worldgen.RimDebrisFeature.Wreck.NONE && wreckSeeds.merge(plan.wreck(), 1, Integer::sum) <= 3;
            if (wantShapes.remove(plan.shape()) | wantWrecks.remove(plan.wreck()) | moreWreck) seeds.add(mix.applyAsLong(k));
        }
        int planned = java.util.Arrays.stream(shapeCount).sum();
        for (int i = 0; i < 4; i++) {
            if (Math.abs((shapeCount[i] - expected[i]) / planned) > 0.025) bad.add("shape " + i + " came " + shapeCount[i] + " of " + planned + ", against " + Math.round(expected[i]));
        }
        if (Math.abs(wrecked / (double) wreckable - 1 / 6.0) > 0.025) bad.add("a wreck on " + wrecked + " of " + wreckable + " islands");
        if (radii.size() < 4) bad.add("the plans' radii don't vary: " + radii);
        if (!wantShapes.isEmpty() || !wantWrecks.isEmpty()) bad.add("no seed planned " + wantShapes + " " + wantWrecks);
        List<String> traits = new ArrayList<>();
        java.util.Map<com.panda.promisedend.worldgen.RimDebrisFeature.Shape, int[]> shapeShare = new java.util.EnumMap<>(com.panda.promisedend.worldgen.RimDebrisFeature.Shape.class);
        for (int n = 0; n < seeds.size(); n++) {
            long seed = seeds.get(n);
            var plan = rim.plan(net.minecraft.util.RandomSource.create(seed));
            double a = Math.toRadians(n * 360.0 / seeds.size());
            int ring = n < 12 ? 300 : 340;
            BlockPos centre = new BlockPos((int) Math.round(ring * Math.cos(a)), y, (int) Math.round(ring * Math.sin(a)));
            if (!feature.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(seed), centre)) bad.add("nothing placed at " + centre);
            int high = 0, pillars = 0, bricks = 0, chiseled = 0;
            List<BlockPos> unsupported = new ArrayList<>(), raised = new ArrayList<>();
            java.util.Set<BlockPos> built = new java.util.HashSet<>();
            int islandVoidstone = 0, islandStone = 0;
            java.util.Map<Long, Integer> tops = new java.util.HashMap<>();
            java.util.Set<BlockPos> stone = new java.util.HashSet<>();
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-16, -16, -16), centre.offset(16, 10, 16))) {
                var state = level.getBlockState(pos);
                if (state.isAir()) continue;
                built.add(pos.immutable());
                boolean isVoidstone = state.is(ModBlocks.VOIDSTONE_SET.base().get()), isEndStone = state.is(net.minecraft.world.level.block.Blocks.END_STONE);
                if (isVoidstone) islandVoidstone++;
                if (isVoidstone || isEndStone) islandStone++;
                if (n < 12 && isVoidstone) voidstone++;
                else if (n < 12 && isEndStone) endStone++;
                if (state.is(net.minecraft.world.level.block.Blocks.PURPUR_PILLAR)) pillars++;
                if (state.is(ModBlocks.VOIDSTONE_BRICK_SET.base().get()) || state.is(ModBlocks.VOIDSTONE_BRICK_SET.stairs().get()) || state.is(ModBlocks.VOIDSTONE_BRICK_SET.slab().get())) bricks++;
                if (state.is(ModBlocks.CHISELED_VOIDSTONE.get())) chiseled++;
                // every wreck block rests on something
                if (!isVoidstone && !isEndStone && level.getBlockState(pos.below()).isAir()) unsupported.add(pos.immutable());
                if (n < 12 && pos.getY() < centre.getY() - 2) below++;
                if (!isVoidstone && !isEndStone) continue;
                // the pieces are counted within the island's own reach, so a cap's loose shards beside it don't count;
                // past it, on an island that isn't tilted, nothing stands over the top's dome (a loose shard sank)
                int ox = pos.getX() - centre.getX(), oz = pos.getZ() - centre.getZ();
                if (ox * ox + oz * oz > (plan.radius() + 0.5) * (plan.radius() + 0.5)) {
                    if (plan.shape() != com.panda.promisedend.worldgen.RimDebrisFeature.Shape.TILTED && pos.getY() > centre.getY() + 1) raised.add(pos.immutable());
                    continue;
                }
                stone.add(pos.immutable());
                tops.merge(net.minecraft.world.level.ChunkPos.pack(pos.getX(), pos.getZ()), pos.getY(), Math::max);
            }
            // the island's pieces: stone joined face to face, largest first; "high" is the largest's stone 3 or more over
            // the middle's level (a loose shard beside a cap may float higher, and isn't the island's)
            List<Integer> pieces = new ArrayList<>();
            int largest = 0;
            java.util.Set<BlockPos> seen = new java.util.HashSet<>();
            for (BlockPos start : stone) {
                if (!seen.add(start)) continue;
                int size = 0, up = 0;
                java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>(List.of(start));
                while (!queue.isEmpty()) {
                    BlockPos q = queue.poll();
                    size++;
                    if (q.getY() >= centre.getY() + 3) up++;
                    for (var d : net.minecraft.core.Direction.values()) {
                        BlockPos r = q.relative(d);
                        if (stone.contains(r) && seen.add(r)) queue.add(r);
                    }
                }
                pieces.add(size);
                if (size > largest) {
                    largest = size;
                    high = up;
                }
            }
            pieces.sort(java.util.Comparator.reverseOrder());
            double second = pieces.size() > 1 ? pieces.get(1) / (double) pieces.get(0) : 0;
            int topSpan = tops.isEmpty() ? 0 : java.util.Collections.max(tops.values()) - java.util.Collections.min(tops.values());
            // the lens across, on the level-topped ones: the unbroken run of blocks through the centre along x
            if (plan.shape() == com.panda.promisedend.worldgen.RimDebrisFeature.Shape.CAP || plan.shape() == com.panda.promisedend.worldgen.RimDebrisFeature.Shape.SHARDS) {
                int east = 0, west = 0;
                while (east < 16 && !level.getBlockState(centre.offset(east + 1, 0, 0)).isAir()) east++;
                while (west < 16 && !level.getBlockState(centre.offset(-west - 1, 0, 0)).isAir()) west++;
                widths.add(east + west + 1);
            }
            traits.add(seed + " " + plan.shape() + "/" + plan.wreck() + " r" + plan.radius() + ": span " + topSpan + ", second piece " + String.format(java.util.Locale.ROOT, "%.2f", second)
                    + ", high " + high + ", pillar " + pillars + ", brick " + bricks + ", chiseled " + chiseled);
            switch (plan.shape()) {
                case TILTED -> { if (topSpan < 3) bad.add(seed + " tilted: its top spans only " + topSpan); }
                case SPLIT -> { if (second < 0.3) bad.add(seed + " split: its second piece is " + second + " of the first"); }
                case SHARDS -> { if (high == 0) bad.add(seed + " shards: nothing 3 over the top"); }
                case CAP -> {
                    if (second >= 0.3) bad.add(seed + " cap: in two near-equal pieces (" + second + ")");
                    if (high > 0) bad.add(seed + " cap: " + high + " blocks 3 over the top");
                }
            }
            switch (plan.wreck()) {
                case PILLAR -> { if (pillars < 3) bad.add(seed + " pillar wreck: " + pillars + " pillar blocks"); }
                case CORNER -> { if (bricks < 5) bad.add(seed + " corner wreck: " + bricks + " brick blocks"); }
                case CHISELED -> { if (chiseled != 1) bad.add(seed + " chiseled wreck: " + chiseled + " chiseled blocks"); }
                case NONE -> { if (pillars + bricks + chiseled > 0) bad.add(seed + " no wreck, yet wreck blocks"); }
            }
            if (!unsupported.isEmpty()) bad.add(seed + " " + plan.wreck() + ": wreck blocks over air at " + unsupported);
            // everything built, joined face to face: no piece under 8 blocks
            List<Integer> crumbs = new ArrayList<>();
            java.util.Set<BlockPos> joined = new java.util.HashSet<>();
            for (BlockPos start : built) {
                if (!joined.add(start)) continue;
                int size = 0;
                java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>(List.of(start));
                while (!queue.isEmpty()) {
                    BlockPos q = queue.poll();
                    size++;
                    for (var d : net.minecraft.core.Direction.values()) {
                        BlockPos r = q.relative(d);
                        if (built.contains(r) && joined.add(r)) queue.add(r);
                    }
                }
                if (size < 8) crumbs.add(size);
            }
            if (!crumbs.isEmpty()) bad.add(seed + " " + plan.shape() + ": pieces under 8 blocks " + crumbs);
            shapeShare.computeIfAbsent(plan.shape(), s -> new int[2]);
            shapeShare.get(plan.shape())[0] += islandVoidstone;
            shapeShare.get(plan.shape())[1] += islandStone;
            if (!raised.isEmpty()) bad.add(seed + " " + plan.shape() + ": stone beside the island over its top at " + raised);
            for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-16, -16, -16), centre.offset(16, 10, 16))) level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
        }
        PromisedEnd.LOGGER.info("rim island shapes over 3000 plans {} (of {}), wrecks {} of {}; built: {}", java.util.Arrays.toString(shapeCount), planned, wrecked, wreckable, traits);
        if (voidstone == 0) bad.add("no Voidstone in the islands");
        double share = voidstone / (double) Math.max(1, voidstone + endStone);
        if (share < 0.66 || share > 0.75) bad.add("islands should be about 70 % Voidstone: " + voidstone + " voidstone, " + endStone + " end stone");
        List<String> byShape = new ArrayList<>();
        shapeShare.forEach((shape, c) -> {
            double s = c[0] / (double) Math.max(1, c[1]);
            byShape.add(shape + String.format(java.util.Locale.ROOT, " %.1f %%", 100 * s));
            if (s < 0.62) bad.add(shape + " islands are only " + Math.round(100 * s) + " % Voidstone");
        });
        PromisedEnd.LOGGER.info("rim islands by shape: {}", byShape);
        if (below == 0) bad.add("the islands have no tapering underside");
        if (widths.stream().anyMatch(w -> w < 6 || w > 18)) bad.add("islands outside 6–18 across: " + widths);
        PromisedEnd.LOGGER.info("rim islands: {} voidstone / {} end stone, widths {}", voidstone, endStone, widths);
        // outside the ring the feature builds nothing: on the main island, and out among the outer lands
        for (BlockPos outside : List.of(new BlockPos(100, y, 0), new BlockPos(1100, y, 0))) {
            if (feature.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(5), outside)) bad.add("placed outside the ring at " + outside);
        }
        BlockPos far = new BlockPos(1000, y, 0);
        if (!feature.place(level, level.getChunkSource().getGenerator(), net.minecraft.util.RandomSource.create(5), far)) bad.add("nothing placed at the ring's outer reach, " + far);
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

    /**
     * The Chorus Weaver. On the arena's floor it stands disguised for 20 ticks with a creative player 3 blocks
     * off (the in-level mock player is always creative, so a survival player's waking it is the harness's). Hurt, it
     * wakes at once; set on a pig 5.5 off, it throws its vine: when the vine lands the pig has Slowness IV for 2 s and
     * is pulled towards the weaver. With the pig gone, it roots again as a plant after its 200 ticks alone.
     */
    private static void chorusWeaverLashes(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        var weaver = helper.spawn(com.panda.promisedend.registry.ModEntities.CHORUS_WEAVER.get(), new BlockPos(1, 1, 1));
        var creative = helper.makeMockServerPlayerInLevel();
        net.minecraft.world.phys.Vec3 w = weaver.position();
        creative.snapTo(w.x + 3, w.y, w.z, 90, 0);
        net.minecraft.world.entity.Mob[] pig = {null};
        // the clock starts once the arena's chunk ticks its entities (a fresh one doesn't at once): {that tick, woke, landed}
        int[] began = {-1}, woke = {-1}, landed = {-1};
        for (int t = 1; t <= 370; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (began[0] < 0) {
                    if (level.isPositionEntityTicking(at)) began[0] = tick;
                    else if (tick == 60) helper.fail("the arena's chunk wasn't entity-ticking by tick 60");
                    return;
                }
                int in = tick - began[0];
                if (in <= 20) {
                    if (!weaver.disguised()) helper.fail("the weaver woke with only a creative player 3 off (" + in + " ticks in)");
                    if (in == 20) {
                        creative.discard();
                        pig[0] = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(1, 1, 1));
                        pig[0].snapTo(w.x, w.y, w.z + 5.5, 180, 0);
                        pig[0].setNoAi(true);
                        weaver.hurtServer(level, level.damageSources().magic(), 1.0F);
                    }
                    return;
                }
                if (woke[0] < 0) {
                    if (weaver.disguised()) {
                        helper.fail("hurt, the weaver didn't wake");
                        return;
                    }
                    woke[0] = in;
                }
                if (landed[0] < 0) weaver.setTarget(pig[0]);
                if (landed[0] < 0 && weaver.lashes() > 0) {
                    landed[0] = in;
                    var slow = pig[0].getEffect(net.minecraft.world.effect.MobEffects.SLOWNESS);
                    net.minecraft.world.phys.Vec3 v = pig[0].getDeltaMovement(), toward = weaver.position().subtract(pig[0].position());
                    double pull = (v.x * toward.x + v.z * toward.z) / Math.max(1e-6, Math.hypot(toward.x, toward.z));
                    PromisedEnd.LOGGER.info("[chorus_weaver_lashes] the vine landed {} ticks after waking: slowness {} for {}, pulled {} a tick towards it",
                            in - woke[0], slow == null ? "none" : slow.getAmplifier() + 1, slow == null ? 0 : slow.getDuration(), String.format("%.2f", pull));
                    if (slow == null || slow.getAmplifier() != com.panda.promisedend.entity.ChorusWeaver.SLOW_AMPLIFIER
                            || Math.abs(slow.getDuration() - com.panda.promisedend.entity.ChorusWeaver.SLOW_TICKS) > 1)
                        helper.fail("the vine's slowness: " + slow);
                    if (pull < 0.4) helper.fail("the vine pulled the pig " + pull + " a tick towards the weaver");
                    pig[0].discard();
                    weaver.setTarget(null);
                }
                if (landed[0] < 0 && in == 80) helper.fail("no vine 80 ticks in (woke at " + woke[0] + ", state " + weaver.state() + ")");
                if (landed[0] > 0 && weaver.disguised()) {
                    int after = in - landed[0];
                    PromisedEnd.LOGGER.info("[chorus_weaver_lashes] rooted again {} ticks after the pig went", after);
                    if (after < com.panda.promisedend.entity.ChorusWeaver.DISGUISE_AFTER) helper.fail("rooted again only " + after + " ticks after the pig went");
                    else helper.succeed();
                }
                if (tick == 370) helper.fail("the weaver never rooted again (the pig went " + landed[0] + " ticks in)");
            });
        }
    }

    /** Turns {@code player} to look at {@code at}, from where they stand. */
    private static void lookAt(net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.Vec3 at) {
        double dx = at.x - player.getX(), dy = at.y - player.getEyeY(), dz = at.z - player.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F, pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        player.snapTo(player.getX(), player.getY(), player.getZ(), yaw, pitch);
        player.setYHeadRot(yaw);
    }

    /**
     * The Purpur Sentry. In the arena, at an iron golem (without AI) 5 blocks off, it raises its shield; a blow from in
     * front then rings off it and one from behind lands. With the golem moved round to its side it turns no faster than
     * {@link com.panda.promisedend.entity.PurpurSentry#TURN_RATE} a tick, and has it in front again within 30 ticks. An
     * arrow shot at it from in front glances back off the shield, one from behind lands.
     * {@link com.panda.promisedend.entity.PurpurSentry#SHIELD_TICKS} after the shield came up it lunges, taking the
     * golem's 7, and while it recovers a blow from in front lands.
     */
    private static void purpurSentryShieldsAndLunges(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
        var sentry = helper.spawn(com.panda.promisedend.registry.ModEntities.PURPUR_SENTRY.get(), new BlockPos(2, 1, 2));
        net.minecraft.world.phys.Vec3 s0 = sentry.position();
        sentry.snapTo(s0.x, s0.y, s0.z, 0, 0);
        sentry.setYBodyRot(0);
        var golem = helper.spawn(net.minecraft.world.entity.EntityTypes.IRON_GOLEM, new BlockPos(2, 1, 7));
        golem.setNoAi(true);
        var blow = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK);
        int[] began = {-1}, shieldAt = {-1}, lungeAt = {-1}, recoverAt = {-1}, blockedBefore = {0};
        float[] health = {0}, golemHealth = {0}, yaw = {0};
        net.minecraft.world.entity.projectile.arrow.Arrow[] arrow = {null};
        // an arrow on {@code side} (east +1, 3 off; west -1, 1.8 off, inside the arena), flying at the sentry's middle
        java.util.function.IntFunction<net.minecraft.world.entity.projectile.arrow.Arrow> shoot = side -> {
            double y = sentry.getY() + sentry.getBbHeight() * 0.5;
            var a = new net.minecraft.world.entity.projectile.arrow.Arrow(level, sentry.getX() + (side > 0 ? 3 : -1.8), y, sentry.getZ(),
                    new ItemStack(net.minecraft.world.item.Items.ARROW), null);
            a.setDeltaMovement(-1.5 * side, 0, 0);
            level.addFreshEntity(a);
            return a;
        };
        for (int t = 1; t <= 250; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (began[0] < 0) {
                    if (level.isPositionEntityTicking(at)) began[0] = tick;
                    else if (tick == 60) helper.fail("the arena's chunk wasn't entity-ticking by tick 60");
                    return;
                }
                int in = tick - began[0];
                var state = sentry.state();
                if (shieldAt[0] < 0) {
                    sentry.setTarget(golem);
                    if (state == com.panda.promisedend.entity.PurpurSentry.State.SHIELD) shieldAt[0] = in;
                    else if (in == 40) helper.fail("the sentry didn't raise its shield at a golem 5 off: " + state);
                    return;
                }
                int up = in - shieldAt[0];
                if (up == 5) {
                    health[0] = sentry.getHealth();
                    sentry.hurtServer(level, new net.minecraft.world.damagesource.DamageSource(blow, golem.getEyePosition()), 4.0F);
                    if (sentry.getHealth() != health[0] || sentry.blocked() != 1)
                        helper.fail("a blow from in front got past the shield: health " + health[0] + " to " + sentry.getHealth() + ", blocked " + sentry.blocked());
                    sentry.hurtServer(level, new net.minecraft.world.damagesource.DamageSource(blow, sentry.position().add(0, 1, -3)), 4.0F);
                    PromisedEnd.LOGGER.info("[purpur_sentry_shields_and_lunges] shield up: a blow from in front stopped, one from behind took {}",
                            String.format("%.2f", health[0] - sentry.getHealth()));
                    if (sentry.getHealth() >= health[0]) helper.fail("a blow from behind didn't land on the shielded sentry");
                }
                if (up == 10) {
                    // round to its side (east of it)
                    golem.snapTo(sentry.getX() + 5, golem.getY(), sentry.getZ(), 0, 0);
                    yaw[0] = sentry.yBodyRot;
                }
                if (up == 11) {
                    float turned = Math.abs(net.minecraft.util.Mth.wrapDegrees(sentry.yBodyRot - yaw[0]));
                    if (turned > com.panda.promisedend.entity.PurpurSentry.TURN_RATE + 0.01F) helper.fail("the shielded sentry turned " + turned + " in a tick");
                }
                if (up == 40) {
                    PromisedEnd.LOGGER.info("[purpur_sentry_shields_and_lunges] the golem moved to its side; 30 ticks on it faces {} (wants -90)",
                            String.format("%.1f", net.minecraft.util.Mth.wrapDegrees(sentry.yBodyRot)));
                    if (!sentry.inFront(golem.position())) helper.fail("30 ticks after the golem went round to its side, the sentry didn't face it: " + sentry.yBodyRot);
                }
                if (up == 42) {
                    health[0] = sentry.getHealth();
                    blockedBefore[0] = sentry.blocked();
                    arrow[0] = shoot.apply(1);
                }
                if (up == 47) {
                    PromisedEnd.LOGGER.info("[purpur_sentry_shields_and_lunges] an arrow from in front: blocked {} to {}, health {} to {}, it flies {}",
                            blockedBefore[0], sentry.blocked(), health[0], sentry.getHealth(), arrow[0].getDeltaMovement());
                    if (sentry.blocked() != blockedBefore[0] + 1 || sentry.getHealth() != health[0])
                        helper.fail("an arrow from in front wasn't turned by the shield: blocked " + sentry.blocked() + ", health " + sentry.getHealth());
                    if (arrow[0].getDeltaMovement().x <= 0) helper.fail("the turned arrow didn't glance back east: " + arrow[0].getDeltaMovement());
                    arrow[0].discard();
                    health[0] = sentry.getHealth();
                    arrow[0] = shoot.apply(-1);
                }
                if (up == 52) {
                    PromisedEnd.LOGGER.info("[purpur_sentry_shields_and_lunges] an arrow from behind took {}", String.format("%.2f", health[0] - sentry.getHealth()));
                    if (sentry.getHealth() >= health[0]) helper.fail("an arrow from behind didn't land on the shielded sentry");
                    arrow[0].discard();
                }
                if (lungeAt[0] < 0 && state == com.panda.promisedend.entity.PurpurSentry.State.LUNGE) {
                    lungeAt[0] = in;
                    golemHealth[0] = golem.getHealth();
                    if (up != com.panda.promisedend.entity.PurpurSentry.SHIELD_TICKS) helper.fail("it lunged " + up + " ticks after the shield came up");
                }
                if (lungeAt[0] > 0 && recoverAt[0] < 0 && state == com.panda.promisedend.entity.PurpurSentry.State.RECOVER) {
                    recoverAt[0] = in;
                    float took = golemHealth[0] - golem.getHealth();
                    PromisedEnd.LOGGER.info("[purpur_sentry_shields_and_lunges] its lunge took {} from the golem", took);
                    if (sentry.lunges() != 1 || Math.abs(took - com.panda.promisedend.entity.PurpurSentry.DAMAGE) > 0.5)
                        helper.fail("the lunge landed " + sentry.lunges() + " times, taking " + took);
                }
                if (recoverAt[0] > 0 && in == recoverAt[0] + 2) {
                    health[0] = sentry.getHealth();
                    blockedBefore[0] = sentry.blocked();
                    sentry.hurtServer(level, new net.minecraft.world.damagesource.DamageSource(blow, golem.getEyePosition()), 4.0F);
                    PromisedEnd.LOGGER.info("[purpur_sentry_shields_and_lunges] recovering, a blow from in front took {}", String.format("%.2f", health[0] - sentry.getHealth()));
                    if (sentry.getHealth() >= health[0] || sentry.blocked() != blockedBefore[0]) helper.fail("recovering, the sentry still stopped a blow from in front");
                    else helper.succeed();
                }
                if (tick == 250) helper.fail("the test ran out (shield " + shieldAt[0] + ", lunge " + lungeAt[0] + ", recover " + recoverAt[0] + ")");
            });
        }
    }

    /**
     * The Rift Stalker. In the arena, a player looking straight at it freezes it within 3 ticks: for 37 more it doesn't
     * move and its blink doesn't count down, and looked away from it stays held a moment (no flicker). Then it hunts
     * again and the count runs a tick a tick at a target (an iron golem facing south, without AI). When the count runs
     * out on its own it gathers, comes out {@link com.panda.promisedend.entity.RiftStalker#BLINK_BEHIND} behind the
     * golem and its strike takes the golem's 6, its only blow. Burnt, it doesn't blink; hit by the golem, it gathers at
     * once, and a stare meanwhile doesn't hold it.
     */
    private static void riftStalkerBlinksAndFreezes(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(2, 1, 2));
        var stalker = helper.spawn(com.panda.promisedend.registry.ModEntities.RIFT_STALKER.get(), new BlockPos(2, 1, 2));
        var player = helper.makeMockServerPlayerInLevel();
        net.minecraft.world.phys.Vec3 s0 = stalker.position();
        player.snapTo(s0.x, s0.y, s0.z + 6, 0, 0);
        var golem = helper.spawn(net.minecraft.world.entity.EntityTypes.IRON_GOLEM, new BlockPos(9, 1, 9));
        golem.setNoAi(true);
        net.minecraft.world.phys.Vec3 g = golem.position();
        golem.snapTo(g.x, g.y, g.z, 0, 0);
        golem.setYHeadRot(0);
        golem.yBodyRot = 0;
        int hold = com.panda.promisedend.entity.RiftStalker.FREEZE_HOLD, hunt = 40 + hold + 3;
        // {the tick the clock started, the blink count when frozen, when it gathered and blinked on its own, burnt, hit}
        int[] began = {-1}, frozenBlinkIn = {-1}, gatheredAt = {-1}, blinkedAt = {-1}, burntAt = {-1}, hitAt = {-1};
        int[] countFrom = {0}, lastBlinkIn = {-1};
        float[] health = {0};
        net.minecraft.world.phys.Vec3[] held = {null};
        for (int t = 1; t <= 290; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (began[0] < 0) {
                    if (level.isPositionEntityTicking(at)) {
                        began[0] = tick;
                        lookAt(player, new net.minecraft.world.phys.Vec3(stalker.getX(), stalker.getY(0.6), stalker.getZ()));
                    } else if (tick == 60) helper.fail("the arena's chunk wasn't entity-ticking by tick 60");
                    return;
                }
                int in = tick - began[0];
                var state = stalker.state();
                if (in == 3) {
                    if (state != com.panda.promisedend.entity.RiftStalker.State.FROZEN) helper.fail("a player looking straight at the stalker didn't freeze it: " + state);
                    held[0] = stalker.position();
                    frozenBlinkIn[0] = stalker.blinkIn();
                }
                if (in > 3 && in <= 40) {
                    if (state != com.panda.promisedend.entity.RiftStalker.State.FROZEN) helper.fail("the stared-at stalker came out of its freeze at " + in + ": " + state);
                    if (stalker.position().distanceTo(held[0]) > 0.05) helper.fail("the frozen stalker moved " + stalker.position().distanceTo(held[0]));
                    if (stalker.blinkIn() != frozenBlinkIn[0]) helper.fail("the frozen stalker's blink counted down: " + frozenBlinkIn[0] + " to " + stalker.blinkIn());
                    if (in == 40) { // looking south, away from it (a living thing looks along its head's turn)
                        player.snapTo(player.getX(), player.getY(), player.getZ(), 0, 0);
                        player.setYHeadRot(0);
                    }
                }
                if (in == 42 && state != com.panda.promisedend.entity.RiftStalker.State.FROZEN)
                    helper.fail("2 ticks after the stare left it, the stalker wasn't still held: " + state);
                if (in == hunt) {
                    if (state != com.panda.promisedend.entity.RiftStalker.State.HUNT) helper.fail("looked away from, the stalker stayed " + state);
                    stalker.setTarget(golem);
                    countFrom[0] = stalker.blinkIn();
                }
                if (in == hunt + 10) {
                    int ran = countFrom[0] - stalker.blinkIn();
                    PromisedEnd.LOGGER.info("[rift_stalker_blinks_and_freezes] frozen 37 ticks without moving, held {} after; unwatched, its blink ran {} in 10 ticks", hold, ran);
                    if (ran != 10) helper.fail("unwatched with a target, the stalker's blink ran " + ran + " in 10 ticks");
                }
                // its count runs out on its own: it gathers on the tick it reaches 0
                if (in > hunt + 10 && gatheredAt[0] < 0) {
                    if (state == com.panda.promisedend.entity.RiftStalker.State.GATHER) {
                        gatheredAt[0] = in;
                        health[0] = golem.getHealth();
                        PromisedEnd.LOGGER.info("[rift_stalker_blinks_and_freezes] it gathered on its own {} ticks in, its count at {} the tick before", in, lastBlinkIn[0]);
                        if (lastBlinkIn[0] > 1) helper.fail("the stalker gathered with " + lastBlinkIn[0] + " still to count");
                        if (stalker.strikes() != 0) helper.fail("the stalker struck " + stalker.strikes() + " times before its first blink");
                    } else {
                        stalker.setTarget(golem);
                        lastBlinkIn[0] = stalker.blinkIn();
                    }
                }
                if (gatheredAt[0] > 0 && blinkedAt[0] < 0 && stalker.blinks() > 0) {
                    blinkedAt[0] = in;
                    net.minecraft.world.phys.Vec3 look = golem.getViewVector(1.0F), off = stalker.position().subtract(golem.position());
                    double behind = -(look.x * off.x + look.z * off.z), d = Math.hypot(off.x, off.z);
                    PromisedEnd.LOGGER.info("[rift_stalker_blinks_and_freezes] blinked {} ticks after gathering, {} behind the golem ({} off)",
                            in - gatheredAt[0], String.format("%.2f", behind), String.format("%.2f", d));
                    if (in - gatheredAt[0] != com.panda.promisedend.entity.RiftStalker.GATHER_TICKS) helper.fail("it blinked " + (in - gatheredAt[0]) + " ticks after gathering");
                    if (behind < 1.2 || d > 3.5) helper.fail("it came out " + behind + " behind the golem, " + d + " off");
                }
                if (blinkedAt[0] > 0 && in == blinkedAt[0] + com.panda.promisedend.entity.RiftStalker.STRIKE_HIT + 1) {
                    float took = health[0] - golem.getHealth();
                    PromisedEnd.LOGGER.info("[rift_stalker_blinks_and_freezes] its strike took {} from the golem", took);
                    if (stalker.strikes() != 1 || Math.abs(took - com.panda.promisedend.entity.RiftStalker.DAMAGE) > 0.5)
                        helper.fail("the strike landed " + stalker.strikes() + " times, taking " + took);
                }
                if (blinkedAt[0] > 0 && burntAt[0] < 0 && in >= blinkedAt[0] + com.panda.promisedend.entity.RiftStalker.STRIKE_TICKS + 2) {
                    if (state != com.panda.promisedend.entity.RiftStalker.State.HUNT) helper.fail("after its strike the stalker stayed " + state);
                    // burnt (no one's blow), it stays; looked at, it freezes
                    lookAt(player, new net.minecraft.world.phys.Vec3(stalker.getX(), stalker.getY(0.6), stalker.getZ()));
                    stalker.setTarget(golem);
                    stalker.hurtServer(level, level.damageSources().onFire(), 1.0F);
                    burntAt[0] = in;
                    if (stalker.state() == com.panda.promisedend.entity.RiftStalker.State.GATHER) helper.fail("burnt, the stalker gathered");
                }
                // past the burn's invulnerable half second, so the next blow counts in full
                if (burntAt[0] > 0 && in == burntAt[0] + 12) {
                    if (state == com.panda.promisedend.entity.RiftStalker.State.GATHER || stalker.blinks() != 1)
                        helper.fail("burnt, the stalker went into the rift: " + state + ", " + stalker.blinks() + " blinks");
                    // the golem's blow, while it is looked at: it gathers at once, and the stare doesn't hold it
                    stalker.setTarget(golem);
                    stalker.hurtServer(level, level.damageSources().mobAttack(golem), 1.0F);
                    hitAt[0] = in;
                    if (stalker.state() != com.panda.promisedend.entity.RiftStalker.State.GATHER) helper.fail("hit, the stalker didn't gather: " + stalker.state());
                }
                if (hitAt[0] > 0 && in > hitAt[0] && in < hitAt[0] + com.panda.promisedend.entity.RiftStalker.GATHER_TICKS
                        && state == com.panda.promisedend.entity.RiftStalker.State.FROZEN)
                    helper.fail("hit, the stalker was held by a stare " + (in - hitAt[0]) + " ticks into its gather");
                if (hitAt[0] > 0 && in == hitAt[0] + com.panda.promisedend.entity.RiftStalker.GATHER_TICKS + 2) {
                    PromisedEnd.LOGGER.info("[rift_stalker_blinks_and_freezes] burnt, it stayed; hit, it blinked again: {} blinks", stalker.blinks());
                    if (stalker.blinks() != 2) helper.fail("hit, the stalker didn't blink: " + stalker.blinks() + " blinks");
                    else helper.succeed();
                }
                if (tick == 290) helper.fail("the test ran out (gathered " + gatheredAt[0] + ", blinked " + blinkedAt[0] + ", burnt " + burntAt[0] + ", hit " + hitAt[0] + ")");
            });
        }
    }

    /**
     * The Lumen Wisp. In the arena a player 7 blocks off with empty hands draws nothing for 20 ticks; with Lumen in hand
     * the wisp comes within 4 blocks of them in 80 ticks, leaving Wisp Lights on its way, and a light it left is out
     * again by its 100 ticks unless the wisp is still beside it (hovering there, it lights it again); with the Lumen put away it stops following. Killed, it leaves a light where it was,
     * scheduled to go out (30 s), and no other block.
     */
    private static void lumenWispLightsTheWay(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 2, 1));
        var wisp = helper.spawn(com.panda.promisedend.registry.ModEntities.LUMEN_WISP.get(), new BlockPos(1, 2, 1));
        var player = helper.makeMockServerPlayerInLevel();
        net.minecraft.world.phys.Vec3 w = wisp.position();
        player.snapTo(w.x, w.y - 1, w.z + 7, 180, 0);
        var lumen = new ItemStack(com.panda.promisedend.registry.ModItems.LUMEN.get());
        // {the tick the clock started, the tick it came near, a light it left and when, the tick the Lumen went away}
        int[] began = {-1}, near = {-1}, litAt = {-1}, putAway = {-1};
        BlockPos[] light = {null};
        var wispLight = com.panda.promisedend.registry.ModBlocks.WISP_LIGHT.get();
        for (int t = 1; t <= 310; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (began[0] < 0) {
                    if (level.isPositionEntityTicking(at)) began[0] = tick;
                    else if (tick == 60) helper.fail("the arena's chunk wasn't entity-ticking by tick 60");
                    return;
                }
                int in = tick - began[0];
                if (in <= 20) {
                    if (wisp.following() != null) helper.fail("the wisp followed a player holding nothing");
                    if (in == 20) player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, lumen.copy());
                    return;
                }
                if (light[0] == null && wisp.lit() > 0) {
                    for (BlockPos q : BlockPos.betweenClosed(at.offset(-2, -2, -2), at.offset(2, 4, 10))) {
                        if (level.getBlockState(q).is(wispLight)) {
                            light[0] = q.immutable();
                            litAt[0] = in;
                            break;
                        }
                    }
                    if (light[0] == null) helper.fail("the wisp counts a light it left, but there is none in the arena");
                }
                if (near[0] < 0 && wisp.distanceTo(player) < 4.0) {
                    near[0] = in;
                    PromisedEnd.LOGGER.info("[lumen_wisp_lights_the_way] came within 4 of the player {} ticks after the Lumen came out, {} lights left",
                            in - 20, wisp.lit());
                }
                if (near[0] < 0 && in == 100) helper.fail("the wisp didn't come within 4 of a player holding Lumen in 80 ticks: " + wisp.distanceTo(player));
                if (light[0] != null && in == litAt[0] + com.panda.promisedend.entity.LumenWisp.TRAIL_TICKS + 5
                        && level.getBlockState(light[0]).is(wispLight) && light[0].distManhattan(wisp.blockPosition()) > 2)
                    helper.fail("a light the wisp left at " + light[0] + " was still lit " + (com.panda.promisedend.entity.LumenWisp.TRAIL_TICKS + 5) + " ticks on");
                if (near[0] > 0 && putAway[0] < 0 && in >= near[0] + 20) {
                    putAway[0] = in;
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                }
                if (putAway[0] > 0 && in == putAway[0] + 5 && wisp.following() != null) helper.fail("the wisp kept following after the Lumen was put away");
                if (putAway[0] > 0 && light[0] != null && in >= litAt[0] + com.panda.promisedend.entity.LumenWisp.TRAIL_TICKS + 10 && in >= putAway[0] + 10) {
                    BlockPos where = wisp.blockPosition();
                    wisp.kill(level);
                    helper.runAfterDelay(2, () -> {
                        boolean lit = level.getBlockState(where).is(wispLight), timed = level.getBlockTicks().hasScheduledTick(where, wispLight);
                        PromisedEnd.LOGGER.info("[lumen_wisp_lights_the_way] broken at {}: a light there {}, set to go out {}", where, lit, timed);
                        if (!lit || !timed) {
                            helper.fail("the broken wisp left no timed light where it was: lit " + lit + ", timed " + timed);
                            return;
                        }
                        // it outlasts a trail light: still lit when one left as it broke would be out
                        helper.runAfterDelay(com.panda.promisedend.entity.LumenWisp.TRAIL_TICKS + 20, () -> {
                            boolean still = level.getBlockState(where).is(wispLight);
                            PromisedEnd.LOGGER.info("[lumen_wisp_lights_the_way] {} ticks on, its light is {}", com.panda.promisedend.entity.LumenWisp.TRAIL_TICKS + 22, still ? "still lit" : "out");
                            if (!still) helper.fail("the broken wisp's light went out with a trail light's");
                            else helper.succeed();
                        });
                    });
                    putAway[0] = Integer.MAX_VALUE / 2;
                }
                if (tick == 310 && putAway[0] < Integer.MAX_VALUE / 2) helper.fail("the test ran out (near " + near[0] + ", lit at " + litAt[0] + ", put away " + putAway[0] + ")");
            });
        }
    }

    /**
     * The void currents. In the End, a column the field puts in a current, made the Skyfall Archipelago (where the
     * islands over it are, at Y 64: VoidCurrents.BIOME_Y), has a flow at
     * Y 10 and none at Y 25, over the band; made the end highlands, none; made small_end_islands, none while our terrain
     * is on and one while it is off. In the arena, whose chunks tick: an armour stand caught by a current running east,
     * held towards 3.5 over the floor, goes along it at about 0.4 a tick (6-9 blocks in 20 ticks) and stays within 1.5
     * of that height; another, held by a still current, is still up after 390 ticks, let go at 400 and on the floor by 460.
     */
    private static void currentCarriesEntity(GameTestHelper helper) {
        var level = helper.getLevel();
        var end = level.getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) {
            helper.fail("no End level");
            return;
        }
        List<String> bad = new ArrayList<>();
        long seed = com.panda.promisedend.logic.VoidCurrents.seed(end);
        BlockPos spot = null;
        for (int i = 0; i < 4096 && spot == null; i++) {
            int x = 4000 + (i % 64) * 5, z = 4000 + (i / 64) * 5;
            if (com.panda.promisedend.logic.VoidCurrent.inCurrent(seed, x + 0.5, z + 0.5)) spot = new BlockPos(x, com.panda.promisedend.logic.VoidCurrents.BIOME_Y, z);
        }
        if (spot == null) {
            helper.fail("no current in 4096 columns round (4000, 4000)");
            return;
        }
        BlockPos at = spot;
        java.util.function.DoubleFunction<double[]> flow = y -> com.panda.promisedend.logic.VoidCurrents.flowAt(end, at.getX() + 0.5, y, at.getZ() + 0.5);
        fillBiome(end, at, "promisedend:skyfall_archipelago");
        if (flow.apply(10.5) == null) bad.add("no flow at " + at + " in the Skyfall");
        if (flow.apply(25.5) != null) bad.add("a flow at Y 25, over the band");
        fillBiome(end, at, "minecraft:end_highlands");
        if (flow.apply(10.5) != null) bad.add("a flow under the end highlands");
        fillBiome(end, at, "minecraft:small_end_islands");
        if (flow.apply(10.5) != null) bad.add("a flow under small_end_islands with our terrain on");
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
            if (flow.apply(10.5) == null) bad.add("no flow under small_end_islands with our terrain off");
        } finally {
            com.panda.promisedend.EndConfig.use(previous);
        }
        // the server's own pass (VoidCurrents.tick), on the Skyfall column at Y 10: an armour stand is caught and carried
        // along the flow, and after its 400 ticks held the ride ends (washed ashore or let go); an arrow there and a
        // shulker (#void_current_ignores) are left alone. The far chunk doesn't tick its entities, so the pass is run
        // by hand on each, a tick at a time, and each one's speed read after it (the scenario sees the real hook)
        fillBiome(end, at, "promisedend:skyfall_archipelago");
        double cx = at.getX() + 0.5, cz = at.getZ() + 0.5;
        var stand = new net.minecraft.world.entity.decoration.ArmorStand(end, cx, 10, cz);
        var arrow = net.minecraft.world.entity.EntityTypes.ARROW.create(end, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        var shulker = net.minecraft.world.entity.EntityTypes.SHULKER.create(end, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (arrow == null || shulker == null) {
            helper.fail("couldn't make an arrow or a shulker");
            return;
        }
        arrow.setPos(cx, 10, cz);
        shulker.setPos(cx, 10, cz);
        List<net.minecraft.world.entity.Entity> placed = List.of(stand, arrow, shulker);
        placed.forEach(end::addFreshEntity);
        try {
            java.util.Map<java.util.UUID, com.panda.promisedend.logic.VoidCurrents.Ride> rides = new java.util.HashMap<>();
            placed.forEach(e -> e.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO));
            placed.forEach(e -> com.panda.promisedend.logic.VoidCurrents.tick(end, rides, e));
            double[] f = flow.apply(10.5);
            var v = stand.getDeltaMovement();
            double along = v.x * f[0] + v.z * f[1];
            if (!rides.containsKey(stand.getUUID()) || along < 0.2) bad.add("the server's pass didn't catch the stand: its speed along the flow " + along);
            if (!stand.needsSync) bad.add("the carried stand isn't marked to be sent");
            for (var e : List.of(arrow, shulker)) if (rides.containsKey(e.getUUID()) || e.getDeltaMovement().lengthSqr() > 0) bad.add("the pass moved " + e);
            for (int t = 1; t <= com.panda.promisedend.logic.VoidCurrent.HOLD_TICKS; t++) {
                stand.setPos(cx, 10, cz);
                com.panda.promisedend.logic.VoidCurrents.tick(end, rides, stand);
            }
            var ride = rides.get(stand.getUUID());
            PromisedEnd.LOGGER.info("[current_carries_entity] the server's pass: held {} ticks, then {}", ride == null ? -1 : ride.held,
                    ride == null ? "gone" : ride.shore != null ? "a shore at " + java.util.Arrays.toString(ride.shore) : "let go");
            if (ride == null || ride.held <= com.panda.promisedend.logic.VoidCurrent.HOLD_TICKS) bad.add("the ride didn't end after " + com.panda.promisedend.logic.VoidCurrent.HOLD_TICKS + " ticks: " + (ride == null ? "none" : ride.held));
            stand.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
            com.panda.promisedend.logic.VoidCurrents.tick(end, rides, stand);
            if (ride != null && ride.shore == null && stand.getDeltaMovement().lengthSqr() > 0) bad.add("a let-go stand was carried again");
        } finally {
            placed.forEach(net.minecraft.world.entity.Entity::discard);
        }
        if (!bad.isEmpty()) {
            helper.fail("void currents: " + bad);
            return;
        }
        // the arena: the floor's top at y 1, the roof 7 over it
        double floorY = helper.absolutePos(new BlockPos(0, 1, 0)).getY(), mid = floorY + 3.5;
        BlockPos carriedAt = helper.absolutePos(new BlockPos(1, 1, 3)), heldAt = helper.absolutePos(new BlockPos(1, 1, 8));
        double[] east = {1, 0}, still = {0, 0};
        int[] held = {0};
        net.minecraft.world.entity.Entity[] stands = new net.minecraft.world.entity.Entity[2];
        double[] start = new double[1];
        double[] drift = {0};
        int[] began = {-1};
        for (int t = 1; t <= 540; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (began[0] < 0) {
                    if (!level.isPositionEntityTicking(carriedAt) || !level.isPositionEntityTicking(heldAt)) {
                        if (tick == 60) helper.fail("the arena's chunks weren't entity-ticking by tick 60");
                        return;
                    }
                    began[0] = tick;
                    stands[0] = new net.minecraft.world.entity.decoration.ArmorStand(level, carriedAt.getX() + 0.5, mid, carriedAt.getZ() + 0.5);
                    stands[1] = new net.minecraft.world.entity.decoration.ArmorStand(level, heldAt.getX() + 0.5, mid, heldAt.getZ() + 0.5);
                    for (var s : stands) level.addFreshEntity(s);
                    start[0] = stands[0].getX();
                    return;
                }
                int in = tick - began[0];
                if (in <= 20) {
                    com.panda.promisedend.logic.VoidCurrents.carry(stands[0], east, mid, false);
                    drift[0] = Math.max(drift[0], Math.abs(stands[0].getY() - mid));
                }
                if (in == 21) {
                    double went = stands[0].getX() - start[0];
                    PromisedEnd.LOGGER.info("[current_carries_entity] carried {} blocks east in 20 ticks, at most {} off its height", String.format("%.2f", went), String.format("%.2f", drift[0]));
                    if (went < 6 || went > 9) helper.fail("a current carried the stand " + went + " blocks in 20 ticks (6-9)");
                    if (drift[0] > 1.5) helper.fail("a carried stand strayed " + drift[0] + " from its height");
                    stands[0].discard();
                }
                if (++held[0] <= com.panda.promisedend.logic.VoidCurrent.HOLD_TICKS) com.panda.promisedend.logic.VoidCurrents.carry(stands[1], still, mid, false);
                if (in == 390 && stands[1].getY() < floorY + 2.5) helper.fail("the held stand was down to " + (stands[1].getY() - floorY) + " over the floor after 390 ticks");
                if (in == 460) {
                    double over = stands[1].getY() - floorY;
                    PromisedEnd.LOGGER.info("[current_carries_entity] the held stand, let go at 400, is {} over the floor at 460", String.format("%.2f", over));
                    if (over > 0.5) helper.fail("the held stand wasn't let go: " + over + " over the floor at 460 ticks");
                    stands[1].discard();
                    helper.succeed();
                }
            });
        }
    }
}

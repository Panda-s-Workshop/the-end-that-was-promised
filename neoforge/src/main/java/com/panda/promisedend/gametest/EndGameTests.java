package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import com.panda.promisedend.worldgen.EndBiomes;
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
    }

    /** Tests that need longer than the default 100 ticks (a dragon's dying takes 200). */
    private static final java.util.Map<String, Integer> TIMEOUTS = java.util.Map.of("crystal_warden_beam_cap_and_rest", 220,
            "wrath_increments_on_kill", 260, "dragon_drops_scales", 240);
    /**
     * Tests in a batch of their own: the world has one Wrath, which these change, and a batch's tests run side by side
     * (batches run one after another).
     */
    private static final java.util.Map<String, String> OWN_BATCH = java.util.Map.of("wrath_increments_on_kill", "wrath_kill",
            "dragon_drops_scales", "wrath_hoard");

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
            var testEnv = OWN_BATCH.containsKey(path) ? own.get(OWN_BATCH.get(path)) : env;
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

    /** Every item, block and advancement text of ours has an English name. */
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
            if (!wrong.isEmpty()) helper.fail("End biomes: " + wrong);
        } finally {
            EndBiomes.setSeed(previous);
        }
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
        helper.runAtTickTime(5, () -> {
            if (com.panda.promisedend.logic.DragonPhase3.entered(dragon)) helper.fail("in phase 3 at full health");
            if (!wardensOf(helper, dragon).isEmpty()) helper.fail("wardens at full health");
            dragon.setHealth(dragon.getMaxHealth() * 0.2F);
        });
        helper.runAtTickTime(10, () -> {
            if (!com.panda.promisedend.logic.DragonPhase3.entered(dragon)) helper.fail("at a fifth of its health it isn't in phase 3");
            var wardens = wardensOf(helper, dragon);
            if (wardens.size() != com.panda.promisedend.logic.DragonPhase3.wardenCount(0)) helper.fail("phase 3 called " + wardens.size() + " wardens, wanted 3");
            for (var w : wardens) {
                if (!w.isPersistenceRequired()) helper.fail("a called warden can despawn");
                if (w.home() == null || Math.abs(Math.sqrt(w.home().distToCenterSqr(dragon.getX(), w.home().getY() + 0.5, dragon.getZ()))
                        - com.panda.promisedend.logic.DragonPhase3.RING_RADIUS) > 1.5) helper.fail("a warden's home is off the ring: " + w.home());
            }
        });
        helper.runAtTickTime(30, () -> {
            if (wardensOf(helper, dragon).size() != com.panda.promisedend.logic.DragonPhase3.wardenCount(0)) helper.fail("it called wardens again");
            dragon.kill(helper.getLevel());
        });
        helper.runAtTickTime(36, () -> {
            var left = wardensOf(helper, dragon);
            if (!left.isEmpty()) helper.fail(left.size() + " wardens outlived their dragon");
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
            dragon.discard();
            if (!warden.isBeaming(target)) helper.fail("it should still be beaming, is " + warden.phase());
            if (healed != 3.0F) helper.fail("after about 2 s of beam the dragon healed " + healed + ", wanted 3 (1 a second)");
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
        var victim = dummy(helper, 6, 1, 6);
        var construct = warden(helper, 7, 2, 6);
        construct.setNoAi(true);
        float full = victim.getHealth();
        net.minecraft.world.phys.Vec3 mark = victim.position();
        com.panda.promisedend.logic.DragonPhase3.startDive(helper.getLevel(), dragon, mark);
        java.util.function.Supplier<List<net.minecraft.world.entity.AreaEffectCloud>> clouds = () -> helper.getLevel().getEntitiesOfClass(
                net.minecraft.world.entity.AreaEffectCloud.class, new net.minecraft.world.phys.AABB(mark, mark).inflate(2),
                c -> c.entityTags().contains(com.panda.promisedend.logic.DragonPhase3.BREATH_TAG));
        helper.runAtTickTime(20, () -> {
            if (!clouds.get().isEmpty()) helper.fail("the breath landed during the telegraph");
            if (victim.getHealth() != full) helper.fail("damage during the telegraph");
        });
        helper.runAtTickTime(40, () -> {
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
        if (!bad.isEmpty()) helper.fail("terrain-off fallback: " + bad);
        helper.succeed();
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

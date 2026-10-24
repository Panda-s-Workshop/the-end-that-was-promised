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
    }

    private EndGameTests() {}

    public static void register(IEventBus modBus) {
        FUNCTIONS.register(modBus);
        modBus.addListener(EndGameTests::registerTests);
    }

    private static void registerTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(PromisedEnd.id("default"));
        for (var entry : FUNCTIONS.getEntries()) {
            ResourceKey<Consumer<GameTestHelper>> key = ResourceKey.create(Registries.TEST_FUNCTION, entry.getId());
            event.registerTest(key.identifier(), new FunctionGameTestInstance(key, new TestData<>(env, ARENA, 100, 0, true)));
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
                for (int dz = -10; dz <= 16; dz++) {
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
        if (maxX - minX + 1 != 11 || maxZ - minZ + 1 != 11) helper.fail("the outpost isn't 11×11: " + (maxX - minX + 1) + "×" + (maxZ - minZ + 1));
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

package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.block.PhantomLatticeBlock;
import com.panda.promisedend.entity.LatticeCrawler;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** The Unfinished Edge's lattice and its builder, the Lattice Crawler. */
final class LatticeGameTests {
    private LatticeGameTests() {}

    /**
     * A Lattice Crawler tears out only lattice. A survival player (healed every tick, so the test outlasts its bites)
     * stands on a Lattice Tile: within its first {@link LatticeCrawler#TEAR_EVERY} ticks and its
     * {@link LatticeCrawler#TEAR_TICKS} groan the tile becomes Phantom Lattice (no collision, no drop, the form it was);
     * moved onto oak planks, a block a player built, they are only bitten: through a whole cycle of its count and more the planks
     * stand and it never starts a tear. {@link PhantomLatticeBlock#REFORM} ticks after the tear the tile is back.
     */
    static void crawlerOnlyTearsLattice(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos tile = new BlockPos(3, 1, 3), planks = new BlockPos(7, 1, 3);
        for (int x = 0; x <= 9; x++) {
            for (int z = 0; z <= 6; z++) helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        }
        helper.setBlock(tile, ModBlocks.LATTICE_TILE.get());
        helper.setBlock(planks, Blocks.OAK_PLANKS);
        var crawler = helper.spawn(ModEntities.LATTICE_CRAWLER.get(), new BlockPos(3, 2, 0));
        var player = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        BlockPos onTile = helper.absolutePos(tile.above()), onPlanks = helper.absolutePos(planks.above());
        BlockPos[] stand = {onTile};
        int[] toreAt = {-1}, groanFrom = {-1}, moved = {-1};
        int lastTick = LatticeCrawler.TEAR_EVERY + LatticeCrawler.TEAR_TICKS + 40 + PhantomLatticeBlock.REFORM + 2;
        for (int t = 1; t <= lastTick; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                player.snapTo(stand[0].getX() + 0.5, stand[0].getY(), stand[0].getZ() + 0.5, 0, 0);
                player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                player.setOnGround(true);
                // its bites kept from killing them (an invulnerable player can't be a target)
                player.setHealth(player.getMaxHealth());
                crawler.setTarget(player);
                var at = helper.getBlockState(tile);
                if (toreAt[0] < 0) {
                    if (crawler.state() == LatticeCrawler.State.TEAR && groanFrom[0] < 0) {
                        groanFrom[0] = tick;
                        if (!helper.absolutePos(tile).equals(crawler.tearing())) helper.fail("it tore at " + crawler.tearing() + ", not under the player");
                    }
                    if (at.is(ModBlocks.PHANTOM_LATTICE.get())) {
                        toreAt[0] = tick;
                        int groan = tick - groanFrom[0];
                        PromisedEnd.LOGGER.info("[crawler_only_tears_lattice] the tile tore {} ticks in, after a {}-tick groan; {} tears", tick, groan, crawler.tears());
                        if (groanFrom[0] < 0 || Math.abs(groan - LatticeCrawler.TEAR_TICKS) > 1) helper.fail("the tile went after a groan of " + groan + " ticks");
                        if (at.getValue(PhantomLatticeBlock.FORM) != PhantomLatticeBlock.Form.LATTICE_TILE) helper.fail("the phantom would re-form as " + at.getValue(PhantomLatticeBlock.FORM));
                        if (!at.getCollisionShape(level, helper.absolutePos(tile)).isEmpty()) helper.fail("the phantom can be stood on");
                        // onto the planks, a block a player built
                        stand[0] = onPlanks;
                        moved[0] = tick;
                    } else if (tick == LatticeCrawler.TEAR_EVERY + LatticeCrawler.TEAR_TICKS + 40) {
                        helper.fail("no tear under a player on lattice in " + tick + " ticks (state " + crawler.state() + ", next in " + crawler.tearIn() + ")");
                    }
                    return;
                }
                if (tick > moved[0] + 2 && crawler.state() == LatticeCrawler.State.TEAR) helper.fail("it started a tear under a player on planks");
                if (!helper.getBlockState(planks).is(Blocks.OAK_PLANKS)) helper.fail("the planks went: " + helper.getBlockState(planks));
                int since = tick - toreAt[0];
                if (since < PhantomLatticeBlock.REFORM - 1 && !at.is(ModBlocks.PHANTOM_LATTICE.get())) helper.fail("the tile was back " + since + " ticks after the tear");
                if (since == PhantomLatticeBlock.REFORM + 2) {
                    if (!at.is(ModBlocks.LATTICE_TILE.get())) helper.fail("the tile wasn't back " + since + " ticks after the tear: " + at);
                    else if (since < LatticeCrawler.TEAR_EVERY + 10) helper.fail("the test ended before a whole cycle on the planks");
                    else {
                        PromisedEnd.LOGGER.info("[crawler_only_tears_lattice] on the planks {} ticks, nothing torn; the tile re-formed {} after", since, PhantomLatticeBlock.REFORM);
                        level.getServer().getPlayerList().remove(player);
                        helper.succeed();
                    }
                }
            });
        }
    }

    /**
     * With the terrain off there is no Unfinished Edge, so {@code void/reach_edge} is earned as far out as it would
     * begin: a survival player in the End 100 short of {@code terrain.edge_distance} hasn't it, one 100 past has. With
     * the terrain on, only the biome earns it.
     */
    static void reachEdgeWithTerrainOff(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var end = server.getLevel(net.minecraft.world.level.Level.END);
        var edge = server.getAdvancements().get(com.panda.promisedend.logic.OuterProgress.REACH_EDGE);
        var previous = com.panda.promisedend.EndConfig.current();
        java.util.List<String> bad = new java.util.ArrayList<>();
        var p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        int far = previous.edgeDistance();
        java.util.function.BiConsumer<Boolean, Integer> at = (terrain, x) -> {
            com.panda.promisedend.EndConfig.use(new com.panda.promisedend.EndConfig.Source() {
                @Override public boolean terrainEnabled() { return terrain; }
                @Override public int edgeDistance() { return previous.edgeDistance(); }
                @Override public int anchorTriggerDepth() { return previous.anchorTriggerDepth(); }
                @Override public boolean dragonPhase3() { return previous.dragonPhase3(); }
                @Override public boolean dragonWrath() { return previous.dragonWrath(); }
                @Override public int maxWrath() { return previous.maxWrath(); }
                @Override public boolean arrivalByCoordinates() { return previous.arrivalByCoordinates(); }
            });
            p.teleport(new net.minecraft.world.level.portal.TeleportTransition(end, new net.minecraft.world.phys.Vec3(x + 0.5, 300, 0.5),
                    net.minecraft.world.phys.Vec3.ZERO, 0, 0, net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
            p.tickCount = com.panda.promisedend.logic.RimProgress.CHECK_INTERVAL * 7;
            com.panda.promisedend.logic.OuterProgress.tick(p);
        };
        try {
            if (edge == null) bad.add("no void/reach_edge");
            else {
                at.accept(true, far + 100);
                if (p.getAdvancements().getOrStartProgress(edge).isDone()) bad.add("earned past the line with the terrain on, out of the biome");
                at.accept(false, far - 100);
                if (p.getAdvancements().getOrStartProgress(edge).isDone()) bad.add("earned 100 short of the line with the terrain off");
                at.accept(false, far + 100);
                if (!p.getAdvancements().getOrStartProgress(edge).isDone()) bad.add("not earned 100 past the line with the terrain off");
            }
        } finally {
            com.panda.promisedend.EndConfig.use(previous);
        }
        server.getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("reach_edge with the terrain off: " + bad);
        else helper.succeed();
    }

    /**
     * Phantom Lattice re-forms as each form it can take, and pushes up what stands in it: a tile torn under an iron
     * golem standing in its space is back {@link PhantomLatticeBlock#REFORM} ticks on, with the golem on top of it. A
     * block that isn't lattice doesn't tear.
     */
    static void phantomLatticeReforms(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos[] at = {new BlockPos(1, 2, 1), new BlockPos(3, 2, 1), new BlockPos(5, 2, 1)};
        net.minecraft.world.level.block.Block[] forms = {ModBlocks.RAW_LATTICE.get(), ModBlocks.LATTICE_SCAFFOLD.get(), ModBlocks.LATTICE_TILE.get()};
        for (int i = 0; i < 3; i++) helper.setBlock(at[i], forms[i]);
        helper.setBlock(new BlockPos(7, 2, 1), Blocks.OAK_PLANKS);
        if (PhantomLatticeBlock.tear(level, helper.absolutePos(new BlockPos(7, 2, 1)), PhantomLatticeBlock.REFORM)) helper.fail("planks tore");
        for (BlockPos p : at) {
            if (!PhantomLatticeBlock.tear(level, helper.absolutePos(p), PhantomLatticeBlock.REFORM)) helper.fail("lattice at " + p + " didn't tear");
        }
        // a golem dropped into the tile's space while it is torn
        var golem = helper.spawn(net.minecraft.world.entity.EntityTypes.IRON_GOLEM, at[2]);
        golem.setNoAi(true);
        helper.runAtTickTime(PhantomLatticeBlock.REFORM + 2, () -> {
            for (int i = 0; i < 3; i++) {
                if (!helper.getBlockState(at[i]).is(forms[i])) helper.fail(at[i] + " re-formed as " + helper.getBlockState(at[i]) + ", not " + forms[i]);
            }
            double top = helper.absolutePos(at[2]).getY() + 1.0;
            if (golem.getY() < top - 0.01) helper.fail("the golem was left in the re-formed tile, at y " + golem.getY() + " under its top " + top);
            else helper.succeed();
        });
    }

    /**
     * The Amber Gate: a 3 by 4 gate and a lone gate block apart from it. Without a key (a stick, or a bare hand) it stays
     * shut and solid; an Amber Key opens all twelve joined blocks (not the lone one), is spent, and leaves them walked
     * through; a second key on the open gate isn't spent. Closing, each block pushes up a pig standing in it.
     */
    /**
     * The Amber Key is crafted of two Echo Amber, two Leviathan Scales, two Ancient Purpur Scrap, an Ancient Core and two
     * of the reef's Ancient Purpur (ASA / PCP / BSB): one key; with stone where the purpur goes, nothing.
     */
    static void amberKeyRecipe(GameTestHelper helper) {
        var level = helper.getLevel();
        var items = new java.util.HashMap<Character, net.minecraft.world.item.Item>(java.util.Map.of(
                'A', com.panda.promisedend.registry.ModItems.ECHO_AMBER.get(), 'S', com.panda.promisedend.registry.ModItems.LEVIATHAN_SCALE.get(),
                'P', com.panda.promisedend.registry.ModItems.ANCIENT_PURPUR_SCRAP.get(), 'C', com.panda.promisedend.registry.ModItems.ANCIENT_CORE.get(),
                'B', ModBlocks.ANCIENT_PURPUR_SET.base().get().asItem()));
        java.util.function.Supplier<net.minecraft.world.item.ItemStack> craft = () -> {
            var grid = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
            for (char c : "ASAPCPBSB".toCharArray()) grid.add(new net.minecraft.world.item.ItemStack(items.get(c)));
            var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
            return level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
                    .map(r -> r.value().assemble(input)).orElse(net.minecraft.world.item.ItemStack.EMPTY);
        };
        java.util.List<String> bad = new java.util.ArrayList<>();
        var key = craft.get();
        if (!key.is(com.panda.promisedend.registry.ModItems.AMBER_KEY.get()) || key.getCount() != 1) bad.add("the full grid makes " + key);
        items.put('B', net.minecraft.world.item.Items.STONE);
        var none = craft.get();
        if (!none.isEmpty()) bad.add("stone in the purpur's place makes " + none);
        if (!bad.isEmpty()) helper.fail("the Amber Key's recipe: " + bad);
        helper.succeed();
    }

    static void amberGateNeedsKey(GameTestHelper helper) {
        var level = helper.getLevel();
        // no other test's fallen Architect or opened gate near (the record is the world's)
        com.panda.promisedend.logic.Citadels.clear(level.getServer());
        var gate = ModBlocks.AMBER_GATE.get();
        java.util.List<BlockPos> joined = new java.util.ArrayList<>();
        for (int x = 2; x <= 4; x++) for (int y = 2; y <= 5; y++) joined.add(helper.absolutePos(new BlockPos(x, y, 2)));
        for (BlockPos p : joined) level.setBlock(p, gate.defaultBlockState(), 3);
        BlockPos lone = helper.absolutePos(new BlockPos(7, 2, 2));
        level.setBlock(lone, gate.defaultBlockState(), 3);
        var player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        BlockPos used = joined.get(1);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(used), net.minecraft.core.Direction.SOUTH, used, false);
        java.util.List<String> bad = new java.util.ArrayList<>();
        java.util.function.Predicate<BlockPos> open = p -> level.getBlockState(p).getValue(com.panda.promisedend.block.AmberGateBlock.OPEN);
        var stick = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK);
        player.setItemInHand(hand, stick);
        level.getBlockState(used).useItemOn(stick, level, player, hand, hit);
        if (joined.stream().anyMatch(open)) bad.add("a stick opened it");
        if (level.getBlockState(used).getCollisionShape(level, used).isEmpty()) bad.add("the shut gate can be walked through");
        var key = new net.minecraft.world.item.ItemStack(com.panda.promisedend.registry.ModItems.AMBER_KEY.get(), 2);
        player.setItemInHand(hand, key);
        level.getBlockState(used).useItemOn(key, level, player, hand, hit);
        long opened = joined.stream().filter(open).count();
        if (opened != joined.size()) bad.add(opened + " of the " + joined.size() + " joined blocks opened");
        if (open.test(lone)) bad.add("the lone block apart from the gate opened");
        if (key.getCount() != 1) bad.add("opening it left " + key.getCount() + " keys of 2");
        if (!level.getBlockState(used).getCollisionShape(level, used).isEmpty()) bad.add("the open gate still blocks");
        level.getBlockState(used).useItemOn(key, level, player, hand, hit);
        if (key.getCount() != 1) bad.add("a key on the open gate was spent");
        // a pig in the gate's bottom middle as it closes
        BlockPos middle = joined.get(4);
        var pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, helper.relativePos(middle));
        pig.setNoAi(true);
        pig.snapTo(middle.getX() + 0.5, middle.getY() + 0.1, middle.getZ() + 0.5, 0, 0);
        for (BlockPos p : joined) level.getBlockState(p).tick(level, p, level.getRandom());
        if (joined.stream().anyMatch(open)) bad.add("its tick didn't close it");
        if (pig.getY() < middle.getY() + 1 - 0.01) bad.add("the closing gate left the pig in it, at y " + pig.getY());
        PromisedEnd.LOGGER.info("[amber_gate_needs_key] {} opened by the key, {} keys left; the pig at y {} (the block's top {})", opened, key.getCount(), pig.getY(), middle.getY() + 1);
        // shut again with its Architect unbound and alive, an empty hand opens it; once it has fallen, it needs a key
        var architect = helper.spawn(com.panda.promisedend.registry.ModEntities.THE_ARCHITECT.get(), new BlockPos(5, 2, 6));
        architect.setNoAi(true);
        architect.setBound(true);
        level.getBlockState(used).useWithoutItem(level, player, hit);
        if (open.test(used)) bad.add("an empty hand opened it with its Architect still bound");
        architect.unbind();
        level.getBlockState(used).useWithoutItem(level, player, hit);
        if (!open.test(used)) bad.add("an empty hand didn't open it with its Architect awake");
        for (BlockPos p : joined) level.getBlockState(p).tick(level, p, level.getRandom());
        architect.discard();
        level.getBlockState(used).useWithoutItem(level, player, hit);
        if (open.test(used)) bad.add("an empty hand opened it with no Architect left");
        // fallen, the gate opens to a key and keeps it, and to an empty hand
        com.panda.promisedend.logic.Citadels.fell(level.getServer(), architect.blockPosition());
        var kept = new net.minecraft.world.item.ItemStack(com.panda.promisedend.registry.ModItems.AMBER_KEY.get());
        player.setItemInHand(hand, kept);
        level.getBlockState(used).useItemOn(kept, level, player, hand, hit);
        if (!open.test(used) || kept.getCount() != 1) bad.add("its Architect fallen, a key opened it " + open.test(used) + " and was kept " + (kept.getCount() == 1));
        for (BlockPos p : joined) level.getBlockState(p).tick(level, p, level.getRandom());
        level.getBlockState(used).useWithoutItem(level, player, hit);
        if (!open.test(used)) bad.add("its Architect fallen, an empty hand didn't open it");
        // a bound Architect loaded only after its gate was opened unbinds itself within a second
        com.panda.promisedend.logic.Citadels.clear(level.getServer());
        com.panda.promisedend.logic.Citadels.opened(level.getServer(), used);
        var late = helper.spawn(com.panda.promisedend.registry.ModEntities.THE_ARCHITECT.get(), new BlockPos(5, 2, 6));
        late.setBound(true);
        late.setHome(late.blockPosition());
        for (int i = 0; i < 21; i++) late.tick();
        if (late.bound()) bad.add("an Architect loaded after its gate was opened stayed bound");
        late.discard();
        com.panda.promisedend.logic.Citadels.clear(level.getServer());
        player.discard();
        if (!bad.isEmpty()) helper.fail("amber gate: " + bad);
        else helper.succeed();
    }

    /**
     * The Unfinished Citadel's template: its Amber Gate is one gate of 3 by 4, shut, in the south wall; the arena over
     * the shaft is Lattice Tile with the open shaft under it between the spokes; eight Lumen Lamps, one chest; its
     * structure keeps monsters out of its box; the Leviathan's chart kind names it.
     */
    static void unfinishedCitadelPlaces(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var template = server.getStructureTemplateManager().get(PromisedEnd.id("unfinished_citadel"));
        if (template.isEmpty()) {
            helper.fail("no Citadel template");
            return;
        }
        var t = template.get();
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        java.util.List<String> bad = new java.util.ArrayList<>();
        var gate = t.filterBlocks(BlockPos.ZERO, settings, ModBlocks.AMBER_GATE.get());
        if (gate.size() != 12) bad.add(gate.size() + " gate blocks, not 12");
        int middle = t.getSize().getX() / 2;
        for (var g : gate) {
            if (g.state().getValue(com.panda.promisedend.block.AmberGateBlock.OPEN)) bad.add("a gate block placed open at " + g.pos());
            if (g.pos().getZ() <= middle || Math.abs(g.pos().getX() - middle) > 1) bad.add("a gate block off the south wall at " + g.pos());
        }
        var tiles = t.filterBlocks(BlockPos.ZERO, settings, ModBlocks.LATTICE_TILE.get());
        long arena = tiles.stream().filter(b -> Math.hypot(b.pos().getX() - middle, b.pos().getZ() - middle) <= 15).count();
        if (arena < 600) bad.add("only " + arena + " Lattice Tile in the arena over the shaft");
        int lamps = t.filterBlocks(BlockPos.ZERO, settings, ModBlocks.LUMEN_LAMP.get()).size();
        if (lamps != 8) bad.add(lamps + " Lumen Lamps");
        if (t.filterBlocks(BlockPos.ZERO, settings, Blocks.CHEST).size() != 1) bad.add("not one chest");
        var structure = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .get(PromisedEnd.id("unfinished_citadel"));
        if (structure.isEmpty()) bad.add("no unfinished_citadel structure");
        else if (structure.get().value().spawnOverrides().get(net.minecraft.world.entity.MobCategory.MONSTER) == null
                || !structure.get().value().spawnOverrides().get(net.minecraft.world.entity.MobCategory.MONSTER).spawns().isEmpty())
            bad.add("monsters may spawn in the Citadel's box");
        var charts = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE)
                .getTagOrEmpty(com.panda.promisedend.item.StarChartItem.tag(PromisedEnd.id("on_citadel_charts")));
        if (!charts.iterator().hasNext()) bad.add("no structure on the Citadel's charts");
        PromisedEnd.LOGGER.info("[unfinished_citadel_places] {} gate blocks, {} arena tiles, {} lamps; size {}", gate.size(), arena, lamps, t.getSize());
        if (!bad.isEmpty()) helper.fail("the Citadel: " + bad);
        else helper.succeed();
    }
}

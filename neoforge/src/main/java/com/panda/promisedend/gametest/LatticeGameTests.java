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
}

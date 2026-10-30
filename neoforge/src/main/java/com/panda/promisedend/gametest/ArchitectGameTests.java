package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.block.AmberGateBlock;
import com.panda.promisedend.block.PhantomLatticeBlock;
import com.panda.promisedend.entity.LatticeCrawler;
import com.panda.promisedend.entity.LatticeShard;
import com.panda.promisedend.entity.TheArchitect;
import com.panda.promisedend.item.Augments;
import com.panda.promisedend.logic.AncientWard;
import com.panda.promisedend.logic.ArchitectFight;
import com.panda.promisedend.logic.LatticeBoost;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModEntities;
import com.panda.promisedend.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The Unfinished Citadel's guardian, the Architect, and its augment. Each test lays a disc of Lattice Tile 9 out round
 * the Architect's rest (an arena of the Citadel's kind, smaller), 3 over the test's floor and past its bounds, so each
 * runs alone and takes its disc away at the end.
 */
final class ArchitectGameTests {
    /** The disc's middle, relative; the tiles are a block under the Architect's rest. */
    private static final BlockPos MIDDLE = new BlockPos(4, 3, 4);
    private static final int DISC = 9;

    private ArchitectGameTests() {}

    /** The disc of tiles round {@code MIDDLE}, absolute. */
    private static List<BlockPos> disc(GameTestHelper helper) {
        List<BlockPos> out = new ArrayList<>();
        for (int dx = -DISC; dx <= DISC; dx++) {
            for (int dz = -DISC; dz <= DISC; dz++) {
                if (dx * dx + dz * dz <= DISC * DISC) out.add(helper.absolutePos(MIDDLE.offset(dx, 0, dz)));
            }
        }
        return out;
    }

    private static void layDisc(GameTestHelper helper) {
        for (BlockPos at : disc(helper)) helper.getLevel().setBlock(at, ModBlocks.LATTICE_TILE.get().defaultBlockState(), 3);
    }

    /** The disc and everything over it (a wall's height and more) back to air. */
    private static void clearDisc(GameTestHelper helper) {
        for (BlockPos at : disc(helper)) {
            for (int y = 0; y <= TheArchitect.WALL_HEIGHT + 2; y++) helper.getLevel().setBlock(at.above(y), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static TheArchitect architect(GameTestHelper helper) {
        return helper.spawn(ModEntities.THE_ARCHITECT.get(), MIDDLE.above());
    }

    private static void hold(ServerPlayer p, Vec3 at) {
        p.snapTo(at.x, at.y, at.z, p.getYRot(), p.getXRot());
        p.setDeltaMovement(Vec3.ZERO);
        p.setOnGround(true);
        p.setHealth(p.getMaxHealth());
    }

    private static void lookAt(ServerPlayer player, Vec3 at) {
        double dx = at.x - player.getX(), dy = at.y - player.getEyeY(), dz = at.z - player.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F, pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        player.snapTo(player.getX(), player.getY(), player.getZ(), yaw, pitch);
        player.setYHeadRot(yaw);
    }

    /**
     * The Architect builds and unmakes only over its own lattice. Dormant, it wakes for a survival player 6 out (and
     * tells them of the keystone); its wall then rises square across their way to it, 3 from them: 7 columns 3 high of
     * raised Phantom Lattice, but none over a column whose floor is oak planks (a player's block) and none in place of a
     * planks block standing in the wall's line, so 17 of 21. Its unmaking marks the tiles within 2.5 of their feet, not
     * a planks block among them, and at the end of its 2 s tell they are torn to Phantom Lattice that re-forms as tile.
     * The planks all stand.
     */
    static void onlyEditsLattice(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        layDisc(helper);
        BlockPos floor = helper.absolutePos(MIDDLE);
        BlockPos plankFloor = floor.offset(1, 0, 3), plankInWall = floor.offset(0, 2, 3), plankUnderfoot = floor.offset(-1, 0, 6);
        for (BlockPos at : List.of(plankFloor, plankInWall, plankUnderfoot)) level.setBlock(at, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        TheArchitect a = architect(helper);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        Vec3 stand = new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 6.5);
        List<String> bad = new ArrayList<>();
        int[] marked = {-1};
        for (int t = 1; t <= 70; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                hold(p, stand);
                if (tick == 15) {
                    if (a.state() == TheArchitect.State.DORMANT) bad.add("it didn't wake for a survival player 6 out");
                    if (!p.entityTags().contains(TheArchitect.HINTED)) bad.add("the player wasn't told of the keystone");
                    int raised = a.raiseWall(level, p);
                    int found = 0;
                    for (int x = -3; x <= 3; x++) {
                        for (int y = 1; y <= TheArchitect.WALL_HEIGHT; y++) {
                            BlockState s = level.getBlockState(floor.offset(x, y, 3));
                            if (s.is(ModBlocks.PHANTOM_LATTICE.get()) && s.getValue(PhantomLatticeBlock.FORM) == PhantomLatticeBlock.Form.RAISED) {
                                found++;
                                if (s.getCollisionShape(level, floor.offset(x, y, 3)).isEmpty()) bad.add("a raised wall block can be walked through");
                            }
                        }
                    }
                    PromisedEnd.LOGGER.info("[architect_only_edits_lattice] its wall: {} raised, {} found in its line", raised, found);
                    if (raised != 17 || found != 17) bad.add("the wall raised " + raised + " (" + found + " in its line), not 17 of 21");
                    for (int y = 1; y <= TheArchitect.WALL_HEIGHT; y++) {
                        if (!level.getBlockState(plankFloor.above(y)).isAir()) bad.add("it raised a wall over a planks floor");
                    }
                    marked[0] = a.startUnmaking(level, p);
                    if (marked[0] != 20) bad.add(marked[0] + " tiles marked round the player's feet, not 20 (21 within 2.5, less the planks)");
                    if (a.marked().contains(plankUnderfoot)) bad.add("it marked the planks");
                }
                if (tick == 15 + TheArchitect.UNMAKE_TELL - 2) {
                    long gone = a.marked().stream().filter(m -> !level.getBlockState(m).is(ModBlocks.LATTICE_TILE.get())).count();
                    if (gone > 0) bad.add(gone + " marked tiles went before the end of the tell");
                }
                if (tick == 15 + TheArchitect.UNMAKE_TELL + 4) {
                    int torn = 0;
                    for (BlockPos at : disc(helper)) {
                        BlockState s = level.getBlockState(at);
                        if (s.is(ModBlocks.PHANTOM_LATTICE.get())) {
                            torn++;
                            if (s.getValue(PhantomLatticeBlock.FORM) != PhantomLatticeBlock.Form.LATTICE_TILE) bad.add("a torn tile would re-form as " + s.getValue(PhantomLatticeBlock.FORM));
                        }
                    }
                    PromisedEnd.LOGGER.info("[architect_only_edits_lattice] {} marked, {} torn after the tell, {} unmade", marked[0], torn, a.unmade());
                    if (torn != 20 || a.unmade() != 20) bad.add(torn + " tiles torn (" + a.unmade() + " unmade), not 20");
                    for (BlockPos at : List.of(plankFloor, plankInWall, plankUnderfoot)) {
                        if (!level.getBlockState(at).is(Blocks.OAK_PLANKS)) bad.add("the planks at " + at + " went: " + level.getBlockState(at));
                    }
                    a.resetArena(level);
                    a.discard();
                    clearDisc(helper);
                    level.getServer().getPlayerList().remove(p);
                    if (!bad.isEmpty()) helper.fail("the Architect's lattice: " + bad);
                    else helper.succeed();
                }
            });
        }
    }

    /**
     * The arena resets. Awake, with a wall up, tiles unmade and its two Crawlers called, the Architect puts it all back
     * once no player is near for {@link TheArchitect#RESET_TICKS} (the count run on from 10 short): no Phantom Lattice
     * left, every tile of the disc a tile, the Crawlers gone, and the Architect dormant, its bar hidden, with its wounds.
     */
    static void arenaResets(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        layDisc(helper);
        int tiles = disc(helper).size();
        TheArchitect a = architect(helper);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        BlockPos floor = helper.absolutePos(MIDDLE);
        List<String> bad = new ArrayList<>();
        helper.runAtTickTime(1, () -> hold(p, new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 6.5)));
        helper.runAtTickTime(3, () -> {
            a.wake();
            a.setHealth(a.getMaxHealth() * 0.5F);
            if (a.raiseWall(level, p) == 0) bad.add("no wall raised");
            a.startUnmaking(level, p);
            a.finishUnmaking(level);
            if (a.unmade() == 0) bad.add("no tile unmade");
            a.callCrawlers(level);
            long crawlers = a.called().stream().filter(id -> level.getEntity(id) instanceof LatticeCrawler).count();
            if (crawlers != TheArchitect.CRAWLERS) bad.add(crawlers + " Crawlers called, not " + TheArchitect.CRAWLERS);
            // the fighter leaves, and it has waited all but half a second
            level.getServer().getPlayerList().remove(p);
            a.setAlone(TheArchitect.RESET_TICKS - 10);
        });
        helper.runAtTickTime(20, () -> {
            int phantoms = 0, back = 0;
            for (BlockPos at : disc(helper)) {
                if (level.getBlockState(at).is(ModBlocks.LATTICE_TILE.get())) back++;
                for (int y = 0; y <= TheArchitect.WALL_HEIGHT + 1; y++) if (level.getBlockState(at.above(y)).is(ModBlocks.PHANTOM_LATTICE.get())) phantoms++;
            }
            long crawlers = a.called().stream().filter(id -> level.getEntity(id) != null).count()
                    + level.getEntitiesOfClass(LatticeCrawler.class, a.getBoundingBox().inflate(16)).size();
            PromisedEnd.LOGGER.info("[architect_arena_resets] after the reset: {} of {} tiles back, {} phantoms, {} crawlers, {}", back, tiles, phantoms, crawlers, a.state());
            if (phantoms != 0) bad.add(phantoms + " Phantom Lattice left");
            if (back != tiles) bad.add(back + " of " + tiles + " tiles back");
            if (crawlers != 0) bad.add(crawlers + " Crawlers left");
            if (a.state() != TheArchitect.State.DORMANT) bad.add("it didn't settle: " + a.state());
            if (a.bar().isVisible()) bad.add("its bar still shows");
            if (a.getHealth() != a.getMaxHealth() * 0.5F) bad.add("its wounds didn't keep: " + a.getHealth());
            a.discard();
            clearDisc(helper);
            if (!bad.isEmpty()) helper.fail("the Architect's arena reset: " + bad);
            else helper.succeed();
        });
    }

    /**
     * Its keystone takes a blow harder. Awake (its AI off, so it holds still and faces the player), with its cage shut
     * a blow does nothing; with it open, it is struck for 10 by a player looking at the keystone and by one looking at
     * its mast: the first lands on the keystone for {@link TheArchitect#KEYSTONE_DAMAGE} times, so after its armour 12
     * it takes 11.7 against 7.2. An arrow flown at the keystone counts too; one at the mast doesn't. Dormant, a blow
     * does nothing.
     */
    static void keystoneTakesDouble(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TheArchitect a = architect(helper);
        a.setNoAi(true);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        List<String> bad = new ArrayList<>();
        helper.runAtTickTime(2, () -> {
            a.setYRot(0.0F);
            a.yBodyRot = 0.0F;
            a.yHeadRot = 0.0F;
            Vec3 front = a.position().add(0, 0, 3.0);
            hold(p, front);
            float before = a.getHealth();
            lookAt(p, a.keystonePosition());
            if (a.hurtServer(level, level.damageSources().playerAttack(p), 10.0F) || a.getHealth() != before) bad.add("a dormant Architect was hurt");
            a.wake();
            lookAt(p, a.keystonePosition());
            if (a.hurtServer(level, level.damageSources().playerAttack(p), 10.0F) || a.getHealth() != before) bad.add("a blow landed with its cage shut");
            a.damageCooldownTime = 0;
            a.openCage(1000);
            a.hurtServer(level, level.damageSources().playerAttack(p), 10.0F);
            float onKey = before - a.getHealth();
            int hits = a.keystoneHits();
            a.damageCooldownTime = 0;
            lookAt(p, a.position().add(0, 1.0, 0));
            float mid = a.getHealth();
            a.hurtServer(level, level.damageSources().playerAttack(p), 10.0F);
            float onMast = mid - a.getHealth();
            PromisedEnd.LOGGER.info("[architect_keystone_takes_double] 10 on the keystone took {}, on the mast {}; keystone hits {} then {}", onKey, onMast, hits, a.keystoneHits());
            if (hits != 1 || a.keystoneHits() != 1) bad.add("keystone hits " + hits + " then " + a.keystoneHits() + ", not 1 and 1");
            float wantKey = com.panda.promisedend.logic.Balance.afterArmour(10.0F * TheArchitect.KEYSTONE_DAMAGE, (float) TheArchitect.ARMOUR, 0.0F);
            float wantMast = com.panda.promisedend.logic.Balance.afterArmour(10.0F, (float) TheArchitect.ARMOUR, 0.0F);
            if (Math.abs(onKey - wantKey) > 0.05F || Math.abs(onMast - wantMast) > 0.05F) bad.add("the keystone took " + onKey + " and the mast " + onMast + ", not " + wantKey + " and " + wantMast);
            a.damageCooldownTime = 0;
            // the arrows: the player stood aside, out of their flight
            hold(p, a.position().add(3.0, 0, 3.0));
            Vec3 key = a.keystonePosition();
            shoot(level, p, new Vec3(key.x, key.y + 0.15, key.z + 5.0));
        });
        helper.runAtTickTime(12, () -> {
            if (a.keystoneHits() != 2) bad.add("an arrow at the keystone made " + (a.keystoneHits() - 1) + " keystone hits");
            a.damageCooldownTime = 0;
            shoot(level, p, a.position().add(0, 0.8, 5.0));
        });
        helper.runAtTickTime(22, () -> {
            if (a.keystoneHits() != 2) bad.add("an arrow at the mast counted as the keystone");
            a.discard();
            level.getServer().getPlayerList().remove(p);
            if (!bad.isEmpty()) helper.fail("the keystone: " + bad);
            else helper.succeed();
        });
    }

    /** An arrow from {@code p}, at {@code from}, flying straight back toward the Architect (-z) at 2.5 a tick. */
    private static void shoot(ServerLevel level, ServerPlayer p, Vec3 from) {
        var arrow = net.minecraft.world.entity.EntityTypes.ARROW.create(level, EntitySpawnReason.TRIGGERED);
        if (arrow == null) return;
        arrow.setOwner(p);
        arrow.snapTo(from.x, from.y, from.z, 180.0F, 0.0F);
        arrow.setDeltaMovement(0, 0, -2.5);
        level.addFreshEntity(arrow);
    }

    /**
     * Its death breaks the Citadel's seal: each who fought it (two here) and a survival witness within
     * {@link TheArchitect#WITNESS} who never struck is given the Architect's Promise and the Architect's Lattice and
     * earns {@code void/defeat_architect}, and one further off is not; it drops 8-12 Raw Lattice; and its arena is put
     * back.
     */
    static void deathBreaksSeal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var server = level.getServer();
        AncientWard.reset(server);
        layDisc(helper);
        TheArchitect a = architect(helper);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL), second = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        ServerPlayer witness = EndGameTests.realPlayer(helper, GameType.SURVIVAL), far = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        BlockPos floor = helper.absolutePos(MIDDLE);
        List<String> bad = new ArrayList<>();
        helper.runAtTickTime(2, () -> {
            hold(p, new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 6.5));
            hold(second, new Vec3(floor.getX() - 5.5, floor.getY() + 1, floor.getZ() + 0.5));
            hold(witness, new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() - 25.5));
            hold(far, new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() - 40.5));
            a.wake();
            a.raiseWall(level, p);
            a.openCage(1000);
            a.hurtServer(level, level.damageSources().playerAttack(p), 5.0F);
            a.damageCooldownTime = 0;
            a.hurtServer(level, level.damageSources().playerAttack(second), 5.0F);
            // the second fighter has gone over to creative since: one who struck it is rewarded whatever their mode
            second.setGameMode(GameType.CREATIVE);
            a.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
            if ((AncientWard.broken(server) & 0b100) == 0) bad.add("the Citadel's seal didn't break");
            var defeated = server.getAdvancements().get(PromisedEnd.id("void/defeat_architect"));
            for (ServerPlayer who : List.of(p, second, witness, far)) {
                boolean want = who != far, promise = who.getInventory().countItem(ModBlocks.ARCHITECTS_PROMISE.get().asItem()) == 1;
                boolean lattice = who.getInventory().countItem(ModItems.ARCHITECTS_LATTICE.get()) == 1;
                boolean earned = defeated != null && who.getAdvancements().getOrStartProgress(defeated).isDone();
                String name = who == p ? "its fighter" : who == second ? "its second fighter" : who == witness ? "a witness 26 off" : "a player 41 off";
                if (promise != want || lattice != want || earned != want) bad.add(name + ": the Promise " + promise + ", the Lattice " + lattice + ", the advancement " + earned);
            }
            for (BlockPos at : disc(helper)) {
                for (int y = 1; y <= TheArchitect.WALL_HEIGHT; y++) if (level.getBlockState(at.above(y)).is(ModBlocks.PHANTOM_LATTICE.get())) bad.add("a wall stood after its death");
            }
        });
        helper.runAtTickTime(5, () -> {
            int lattice = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, a.getBoundingBox().inflate(6)).stream()
                    .filter(i -> i.getItem().is(ModBlocks.RAW_LATTICE.get().asItem())).mapToInt(i -> i.getItem().getCount()).sum();
            PromisedEnd.LOGGER.info("[architect_death_breaks_seal] it dropped {} Raw Lattice; seals broken {}", lattice, AncientWard.broken(server));
            if (lattice < 8 || lattice > 12) bad.add(lattice + " Raw Lattice dropped, not 8-12");
            AncientWard.reset(server);
            clearDisc(helper);
            for (ServerPlayer who : List.of(p, second, witness, far)) level.getServer().getPlayerList().remove(who);
            if (!bad.isEmpty()) helper.fail("the Architect's death: " + bad);
            else helper.succeed();
        });
    }

    /**
     * A throw shows where it will come down and comes down there: two shards from the Architect's keystone (its AI off),
     * one at a survival player held still 8 out, one at another 8 out the other way who steps 3 aside once it is
     * thrown. Each shard's aim is under its target's middle (so its ring is at their feet); the still one is hit for
     * {@link LatticeShard#DAMAGE} as the difficulty makes it, and the one who stepped aside isn't hit.
     */
    static void shardLandsWhereShown(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        layDisc(helper);
        TheArchitect a = architect(helper);
        a.setNoAi(true);
        ServerPlayer still = EndGameTests.realPlayer(helper, GameType.SURVIVAL), steps = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        BlockPos floor = helper.absolutePos(MIDDLE);
        Vec3 here = new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 8.5), there = new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() - 7.5);
        Vec3 aside = there.add(3, 0, 0);
        List<String> bad = new ArrayList<>();
        float[] taken = {0, 0};
        int[] thrown = {-1};
        // a new player can't be hurt until their client counts as loaded (60 ticks here): the throws wait for it
        for (int t = 1; t <= 170; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                // a blow taken last tick counted before the hold heals it
                for (int i = 0; i < 2; i++) {
                    ServerPlayer who = i == 0 ? still : steps;
                    var last = who.getLastDamageSource();
                    if (who.getHealth() < who.getMaxHealth() && last != null && last.getDirectEntity() instanceof LatticeShard) taken[i] += who.getMaxHealth() - who.getHealth();
                }
                hold(still, here);
                hold(steps, thrown[0] > 0 && tick > thrown[0] + 4 ? aside : there);
                if (thrown[0] < 0 && still.connection.hasClientLoaded() && steps.connection.hasClientLoaded()) {
                    thrown[0] = tick;
                    for (ServerPlayer target : List.of(still, steps)) {
                        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.4, 0);
                        LatticeShard shard = new LatticeShard(level, a, aim);
                        level.addFreshEntity(shard);
                        Vec3 at = shard.aim().orElse(Vec3.ZERO);
                        if (Math.hypot(at.x - target.getX(), at.z - target.getZ()) > 0.01 || Math.floor(at.y) != Math.floor(target.getY()))
                            bad.add("a shard's ring would be at " + at + ", not under " + target.position());
                    }
                }
            });
        }
        helper.runAtTickTime(172, () -> {
            PromisedEnd.LOGGER.info("[architect_shard_lands_where_shown] thrown on tick {}; the still player took {}, the one who stepped aside {}", thrown[0], taken[0], taken[1]);
            if (thrown[0] < 0 || thrown[0] > 90) bad.add("the throws went on tick " + thrown[0] + ", too late to land (the clients never loaded)");
            float want = onPlayer(level, LatticeShard.DAMAGE);
            if (want > 0 && Math.abs(taken[0] - want) > 0.01F) bad.add("the still player took " + taken[0] + ", not " + want);
            if (taken[1] > 0) bad.add("the player who stepped aside was hit for " + taken[1]);
            clearDisc(helper);
            for (ServerPlayer who : List.of(still, steps)) level.getServer().getPlayerList().remove(who);
            if (!bad.isEmpty()) helper.fail("a shard's landing: " + bad);
            else helper.succeed();
        });
    }

    /**
     * The Architect's Promise, set down and used by a survival player, writes the Codex's closing page in (its
     * advancement); the use succeeds and leaves the block.
     */
    static void promiseReadsClosingPage(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlock(at, ModBlocks.ARCHITECTS_PROMISE.get().defaultBlockState(), 3);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        hold(p, Vec3.atBottomCenterOf(at.south(2)));
        var kept = level.getServer().getAdvancements().get(com.panda.promisedend.logic.CodexBook.PROMISE_KEPT);
        List<String> bad = new ArrayList<>();
        if (kept == null) bad.add("there is no void/promise_kept advancement");
        else if (p.getAdvancements().getOrStartProgress(kept).isDone()) bad.add("the closing page was read before the Promise was used");
        var result = level.getBlockState(at).useWithoutItem(level, p, new net.minecraft.world.phys.BlockHitResult(Vec3.atCenterOf(at), net.minecraft.core.Direction.SOUTH, at, false));
        if (!result.consumesAction()) bad.add("using the Promise did nothing (" + result + ")");
        if (kept != null && !p.getAdvancements().getOrStartProgress(kept).isDone()) bad.add("using the Promise didn't write the closing page in");
        if (!level.getBlockState(at).is(ModBlocks.ARCHITECTS_PROMISE.get())) bad.add("the Promise went when used");
        level.getServer().getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("the Architect's Promise: " + bad);
        else helper.succeed();
    }

    /**
     * Saved and loaded mid-fight, in its second phase after a survival player's blow: it comes back awake and between
     * works, in the same phase with its bar magenta and shown, its health, rest and fighter kept (so its Promise still
     * goes to them).
     */
    static void savedMidFight(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        layDisc(helper);
        TheArchitect a = architect(helper);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        BlockPos floor = helper.absolutePos(MIDDLE);
        List<String> bad = new ArrayList<>();
        helper.runAtTickTime(2, () -> {
            hold(p, new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 8.5));
            a.wake();
            a.setHealth(a.getMaxHealth() * 0.61F);
            a.openCage(40);
            a.damageCooldownTime = 0;
            a.hurtServer(level, level.damageSources().playerAttack(p), 20.0F);
            if (a.phase() != 1) bad.add("the set-up left it in phase " + (a.phase() + 1) + ", not 2");
            TheArchitect b = EndGameTests.reloaded(level, a, ModEntities.THE_ARCHITECT.get());
            PromisedEnd.LOGGER.info("[architect_saved_mid_fight] before: {} phase {} health {}; after: {} phase {} health {} bar {} {}",
                    a.state(), a.phase(), a.getHealth(), b.state(), b.phase(), b.getHealth(), b.bar().getColor(), b.bar().isVisible());
            if (b.state() != TheArchitect.State.IDLE) bad.add("it came back " + b.state() + ", not awake between works");
            if (b.phase() != a.phase()) bad.add("it came back in phase " + (b.phase() + 1) + ", not " + (a.phase() + 1));
            if (b.bar().getColor() != TheArchitect.barColour(a.phase()) || !b.bar().isVisible()) bad.add("its bar came back " + b.bar().getColor() + ", shown " + b.bar().isVisible());
            if (Math.abs(b.getHealth() - a.getHealth()) > 0.01F) bad.add("its health came back " + b.getHealth() + ", not " + a.getHealth());
            if (!java.util.Objects.equals(b.home(), a.home())) bad.add("its rest came back " + b.home() + ", not " + a.home());
            if (!b.fought().contains(p.getUUID())) bad.add("its fighter was forgotten");
            clearDisc(helper);
            level.getServer().getPlayerList().remove(p);
            if (!bad.isEmpty()) helper.fail("the Architect saved mid-fight: " + bad);
            else helper.succeed();
        });
    }

    /** A mob's blow of {@code damage} on a player, as the level's difficulty makes it (Normal is as written). */
    private static float onPlayer(ServerLevel level, float damage) {
        return switch (level.getDifficulty()) {
            case PEACEFUL -> 0.0F;
            case EASY -> com.panda.promisedend.logic.Balance.onEasy(damage);
            case NORMAL -> damage;
            case HARD -> damage * com.panda.promisedend.logic.Balance.HARD;
        };
    }

    /**
     * The fight as it runs, its AI on, against a survival player held still on a tile 8 out. Phase 1's works come in
     * their turn (a wall, a throw, a throw, a wall), each {@link ArchitectFight#REST_BETWEEN} after the last one's end.
     * A throw comes down on the player for {@link LatticeShard#DAMAGE}. A blow lands only while its cage is open, from a
     * throw to its next work; one while it raises a wall does nothing. A wall's blocks are gone
     * {@link TheArchitect#WALL_TICKS} after it rose. A blow in an open window that takes it under 60 % starts phase 2,
     * whose first work is an unmaking: the tiles go at the end of its tell and are back {@link TheArchitect#UNMAKE_TICKS}
     * after.
     */
    static void runsItsTurns(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        layDisc(helper);
        TheArchitect a = architect(helper);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        BlockPos floor = helper.absolutePos(MIDDLE);
        Vec3 stand = new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 8.5);
        List<String> bad = new ArrayList<>(), notes = new ArrayList<>();
        int[] raisedAt = {-1}, tornAt = {-1}, struck = {0}, phased = {-1}, shut = {0};
        List<BlockPos> wall = new ArrayList<>(), torn = new ArrayList<>();
        List<Float> throwsTaken = new ArrayList<>();
        java.util.Map<java.util.UUID, Vec3> flying = new java.util.HashMap<>();
        int end = 740;
        for (int t = 1; t <= end; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                // held where it stands (its own client would keep it on its feet), its blows counted, then healed
                p.snapTo(stand.x, stand.y, stand.z, p.getYRot(), p.getXRot());
                p.setDeltaMovement(Vec3.ZERO);
                p.setOnGround(true);
                if (p.getHealth() < p.getMaxHealth()) {
                    var last = p.getLastDamageSource();
                    if (last != null && last.getDirectEntity() instanceof LatticeShard) throwsTaken.add(p.getMaxHealth() - p.getHealth());
                    p.setHealth(p.getMaxHealth());
                }
                // each throw in the air, and where it was last seen when it went
                java.util.Set<java.util.UUID> now = new java.util.HashSet<>();
                for (LatticeShard shard : level.getEntitiesOfClass(LatticeShard.class, a.getBoundingBox().inflate(24))) {
                    now.add(shard.getUUID());
                    flying.put(shard.getUUID(), shard.position());
                }
                for (var it = flying.entrySet().iterator(); it.hasNext(); ) {
                    var e = it.next();
                    if (now.contains(e.getKey())) continue;
                    Vec3 at = e.getValue();
                    notes.add("a throw went at " + String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f", at.x - stand.x, at.y - stand.y, at.z - stand.z)
                            + " from the player on tick " + tick + " (loaded " + p.connection.hasClientLoaded() + ")");
                    it.remove();
                }
                if (raisedAt[0] < 0 && a.walls() > 0) {
                    raisedAt[0] = tick;
                    for (BlockPos at : disc(helper)) {
                        for (int y = 1; y <= TheArchitect.WALL_HEIGHT; y++) if (level.getBlockState(at.above(y)).is(ModBlocks.PHANTOM_LATTICE.get())) wall.add(at.above(y));
                    }
                }
                if (raisedAt[0] > 0 && tick == raisedAt[0] + TheArchitect.WALL_TICKS + 4) {
                    long left = wall.stream().filter(w -> level.getBlockState(w).is(ModBlocks.PHANTOM_LATTICE.get())).count();
                    notes.add("wall of " + wall.size() + " at " + raisedAt[0] + ", " + left + " left at +" + (TheArchitect.WALL_TICKS + 4));
                    if (wall.isEmpty() || left > 0) bad.add("its wall of " + wall.size() + " left " + left + " standing " + (TheArchitect.WALL_TICKS + 4) + " ticks after it rose");
                }
                // a blow while it raises a wall does nothing
                if (a.state() == TheArchitect.State.RAISE && shut[0] == 0) {
                    a.damageCooldownTime = 0;
                    float before = a.getHealth();
                    if (a.hurtServer(level, level.damageSources().playerAttack(p), 4.0F) || a.getHealth() != before) bad.add("a blow landed while it raised a wall");
                    shut[0] = tick;
                }
                // in its first open window a blow lands; in its third, one takes it under 60 %
                if (a.isOpen() && a.state() == TheArchitect.State.IDLE && phased[0] < 0) {
                    if (struck[0] == 0) {
                        a.damageCooldownTime = 0;
                        float before = a.getHealth();
                        if (!a.hurtServer(level, level.damageSources().playerAttack(p), 4.0F) || a.getHealth() >= before) bad.add("a blow in its open window did nothing");
                        struck[0] = a.throwsMade();
                    } else if (a.throwsMade() >= 3 && a.throwsMade() > struck[0]) {
                        a.setHealth(a.getMaxHealth() * 0.61F);
                        a.damageCooldownTime = 0;
                        a.hurtServer(level, level.damageSources().playerAttack(p), 20.0F);
                        phased[0] = tick;
                        if (a.phase() != 1) bad.add("a blow under 60 % left it in phase " + (a.phase() + 1));
                        if (a.bar().getColor() != net.minecraft.world.BossEvent.BossBarColor.PINK) bad.add("its bar in phase 2 is " + a.bar().getColor() + ", not magenta");
                    }
                }
                if (tornAt[0] < 0 && a.unmade() > 0) {
                    tornAt[0] = tick;
                    for (BlockPos at : disc(helper)) if (level.getBlockState(at).is(ModBlocks.PHANTOM_LATTICE.get())) torn.add(at);
                }
                if (tornAt[0] > 0 && tick == tornAt[0] + TheArchitect.UNMAKE_TICKS + 4) {
                    long back = torn.stream().filter(at -> level.getBlockState(at).is(ModBlocks.LATTICE_TILE.get())).count();
                    notes.add(torn.size() + " tiles torn at " + tornAt[0] + ", " + back + " back at +" + (TheArchitect.UNMAKE_TICKS + 4));
                    if (torn.isEmpty() || back != torn.size()) bad.add(back + " of " + torn.size() + " unmade tiles back " + (TheArchitect.UNMAKE_TICKS + 4) + " ticks after they went");
                }
                if (tick == end) {
                    List<String> began = a.began();
                    PromisedEnd.LOGGER.info("[architect_runs_its_turns] its works {}; throws taken {}; phase 2 at {}; {}", began, throwsTaken, phased[0], notes);
                    // the turn, and the rests: each work begins its length and a rest after the last began
                    List<String> want = List.of("RAISE", "THROW", "THROW", "RAISE");
                    for (int i = 0; i < want.size(); i++) {
                        if (began.size() <= i || !began.get(i).endsWith(" " + want.get(i))) bad.add("work " + (i + 1) + " was " + (began.size() > i ? began.get(i) : "none") + ", not " + want.get(i));
                    }
                    for (int i = 0; i + 1 < Math.min(4, began.size()); i++) {
                        int from = Integer.parseInt(began.get(i).split(" ")[0]), to = Integer.parseInt(began.get(i + 1).split(" ")[0]);
                        ArchitectFight.Work work = ArchitectFight.Work.valueOf(began.get(i).split(" ")[1]);
                        int gap = ArchitectFight.length(work) + ArchitectFight.REST_BETWEEN;
                        if (Math.abs(to - from - gap) > 2) bad.add("work " + (i + 2) + " began " + (to - from) + " after work " + (i + 1) + ", not " + gap);
                    }
                    int unmaking = -1;
                    for (int i = 0; i < began.size(); i++) if (began.get(i).endsWith(" UNMAKE")) unmaking = i;
                    if (unmaking < 0) bad.add("phase 2 never unmade");
                    float want7 = onPlayer(level, LatticeShard.DAMAGE);
                    if (throwsTaken.isEmpty()) bad.add("no throw came down on the player in " + a.throwsMade());
                    for (float taken : throwsTaken) if (Math.abs(taken - want7) > 0.05F) bad.add("a throw took " + taken + ", not " + want7);
                    a.resetArena(level);
                    a.discard();
                    clearDisc(helper);
                    level.getServer().getPlayerList().remove(p);
                    if (!bad.isEmpty()) helper.fail("the Architect's turns: " + bad);
                    else helper.succeed();
                }
            });
        }
    }

    /**
     * A fighter under it on its hub, where its walls and unmaking can't reach, is swept out: held there, at its first
     * work's turn the Architect sweeps instead, dealing {@link TheArchitect#SWEEP_DAMAGE} and throwing them out off the
     * hub, onto the tiles.
     */
    static void sweepsTheHub(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        layDisc(helper);
        TheArchitect a = architect(helper);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        BlockPos floor = helper.absolutePos(MIDDLE);
        Vec3 stand = new Vec3(floor.getX() + 1.5, floor.getY() + 1, floor.getZ() + 0.5);
        // held bound (unwoken) until the player can be hurt
        a.setBound(true);
        List<String> bad = new ArrayList<>();
        float[] took = {0};
        Vec3[] flung = {Vec3.ZERO};
        int[] woke = {-1};
        for (int t = 1; t <= 200; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (a.sweeps() == 0) {
                    p.snapTo(stand.x, stand.y, stand.z, 0, 0);
                    p.setDeltaMovement(Vec3.ZERO);
                    p.setOnGround(true);
                    p.setHealth(p.getMaxHealth());
                    // a new player can't be hurt until their client counts as loaded (60 ticks here)
                    if (woke[0] < 0 && p.connection.hasClientLoaded()) {
                        woke[0] = tick;
                        a.unbind();
                        a.wake();
                    }
                } else if (flung[0] == Vec3.ZERO) {
                    took[0] = p.getMaxHealth() - p.getHealth();
                    flung[0] = p.getDeltaMovement();
                }
                if (tick == 200) {
                    PromisedEnd.LOGGER.info("[architect_sweeps_the_hub] woken on tick {};", woke[0]);
                    PromisedEnd.LOGGER.info("[architect_sweeps_the_hub] sweeps {}, works {}; it took {} and flung them {}", a.sweeps(), a.began(), took[0], flung[0]);
                    if (a.sweeps() < 1) bad.add("no sweep in the 200 ticks under it: " + a.began());
                    if (a.walls() > 0 || a.unmade() > 0) bad.add("it walled or unmade for one on its hub");
                    float want = onPlayer(level, TheArchitect.SWEEP_DAMAGE);
                    if (Math.abs(took[0] - want) > 0.05F) bad.add("the sweep took " + took[0] + ", not " + want);
                    if (flung[0].x < 1.0 || Math.abs(flung[0].z) > 0.2) bad.add("the sweep flung them " + flung[0] + ", not out (+x) off the hub");
                    a.discard();
                    clearDisc(helper);
                    level.getServer().getPlayerList().remove(p);
                    if (!bad.isEmpty()) helper.fail("the hub: " + bad);
                    else helper.succeed();
                }
            });
        }
    }

    /**
     * It wakes for a survival player within {@link TheArchitect#WAKE_RANGE} (16), not for one 17 out; and placed bound
     * by the Citadel, it wakes for no one, struck or near, until its Amber Gate is opened.
     */
    static void wakesAtSixteenOnceUnbound(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        TheArchitect a = architect(helper);
        a.setBound(true);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        BlockPos floor = helper.absolutePos(MIDDLE);
        BlockPos gate = floor.offset(0, 1, -3);
        level.setBlock(gate, ModBlocks.AMBER_GATE.get().defaultBlockState(), 3);
        level.setBlock(gate.above(), ModBlocks.AMBER_GATE.get().defaultBlockState(), 3);
        List<String> bad = new ArrayList<>();
        Vec3 near = new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 6.5);
        helper.runAtTickTime(1, () -> p.snapTo(near.x, near.y, near.z, 0, 0));
        helper.runAtTickTime(25, () -> {
            if (a.state() != TheArchitect.State.DORMANT) bad.add("bound, it woke for a player 6 out");
            if (a.hurtServer(level, level.damageSources().playerAttack(p), 10.0F) || a.state() != TheArchitect.State.DORMANT) bad.add("bound, a blow woke or hurt it");
            AmberGateBlock.open(level, gate);
            if (a.bound()) bad.add("its gate opened, it was still bound");
            Vec3 far = new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 17.5);
            p.snapTo(far.x, far.y, far.z, 0, 0);
        });
        helper.runAtTickTime(50, () -> {
            if (a.state() != TheArchitect.State.DORMANT) bad.add("it woke for a player 17 out");
            Vec3 in = new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 15.5);
            p.snapTo(in.x, in.y, in.z, 0, 0);
        });
        helper.runAtTickTime(75, () -> {
            if (a.state() == TheArchitect.State.DORMANT) bad.add("it didn't wake for a player 15 out");
            level.setBlock(gate, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(gate.above(), Blocks.AIR.defaultBlockState(), 3);
            a.discard();
            level.getServer().getPlayerList().remove(p);
            if (!bad.isEmpty()) helper.fail("its waking: " + bad);
            else helper.succeed();
        });
    }

    /**
     * The Architect's Lattice: a glider with it worn boosts on the jump key, as a flight-1 rocket fired from them; a
     * second press within {@link LatticeBoost#COOLDOWN} doesn't; wings without it, or a player not gliding, never.
     */
    static void latticeBoosts(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        Augments.forgetBoosts();
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        List<String> bad = new ArrayList<>();
        Vec3 high = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 8, 2)));
        p.snapTo(high.x, high.y, high.z, 0, 0);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        p.startFallFlying();
        if (!p.isFallFlying()) bad.add("the test player wouldn't glide");
        if (Augments.boost(p)) bad.add("bare wings boosted");
        p.setItemSlot(EquipmentSlot.CHEST, Augments.apply(new ItemStack(Items.ELYTRA), Augments.ARCHITECTS_LATTICE));
        p.stopFallFlying();
        if (Augments.boost(p)) bad.add("a player not gliding boosted");
        p.startFallFlying();
        boolean first = Augments.boost(p);
        int rockets = level.getEntitiesOfClass(FireworkRocketEntity.class, p.getBoundingBox().inflate(4)).size();
        boolean second = Augments.boost(p);
        PromisedEnd.LOGGER.info("[architect_lattice_boosts] first press {} ({} rocket), second {}", first, rockets, second);
        if (!first || rockets != 1) bad.add("the first press boosted " + first + " with " + rockets + " rockets");
        if (second) bad.add("a second press within the cooldown boosted");
        level.getEntitiesOfClass(FireworkRocketEntity.class, p.getBoundingBox().inflate(8)).forEach(r -> r.discard());
        Augments.forgetBoosts();
        level.getServer().getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("the Architect's Lattice: " + bad);
        else helper.succeed();
    }

    /** The Citadel's template places one Architect, bound, over the arena's hub, a block over its tiles. */
    static void citadelPlacesArchitect(GameTestHelper helper) {
        var template = helper.getLevel().getServer().getStructureTemplateManager().get(PromisedEnd.id("unfinished_citadel"));
        if (template.isEmpty()) {
            helper.fail("no Citadel template");
            return;
        }
        var saved = template.get().save(new net.minecraft.nbt.CompoundTag());
        var entities = saved.getListOrEmpty("entities");
        List<BlockPos> found = new ArrayList<>();
        boolean[] bound = {true};
        for (int i = 0; i < entities.size(); i++) {
            var e = entities.getCompoundOrEmpty(i);
            if (!e.getCompoundOrEmpty("nbt").getStringOr("id", "").equals("promisedend:the_architect")) continue;
            bound[0] &= e.getCompoundOrEmpty("nbt").getBooleanOr("bound", false);
            var pos = e.getListOrEmpty("blockPos");
            found.add(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)));
        }
        int middle = template.get().getSize().getX() / 2;
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        boolean onHub = !found.isEmpty() && template.get().filterBlocks(BlockPos.ZERO, settings, ModBlocks.RAW_LATTICE.get()).stream()
                .anyMatch(b -> b.pos().equals(found.getFirst().below()));
        PromisedEnd.LOGGER.info("[citadel_places_architect] Architects at {}; the template's middle {}", found, middle);
        if (found.size() != 1) helper.fail(found.size() + " Architects in the Citadel");
        else if (found.getFirst().getX() != middle || found.getFirst().getZ() != middle) helper.fail("the Architect at " + found.getFirst() + ", off the middle " + middle);
        else if (!onHub) helper.fail("the Architect isn't over the arena's hub of raw lattice");
        else if (!bound[0]) helper.fail("the Citadel's Architect isn't bound behind its gate");
        else helper.succeed();
    }
}

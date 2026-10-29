package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.block.PhantomLatticeBlock;
import com.panda.promisedend.entity.LatticeCrawler;
import com.panda.promisedend.entity.TheArchitect;
import com.panda.promisedend.item.Augments;
import com.panda.promisedend.logic.AncientWard;
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
     * left, every tile of the disc a tile, the Crawlers gone, and the Architect dormant, its bar hidden.
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
            a.discard();
            clearDisc(helper);
            if (!bad.isEmpty()) helper.fail("the Architect's arena reset: " + bad);
            else helper.succeed();
        });
    }

    /**
     * Its keystone takes a blow twice. Awake (its AI off, so it holds still and faces the player), it is struck for 10
     * by a player looking at the keystone and by one looking at its mast: the first lands on the keystone and takes off
     * more than half again as much (after its armour 12, 18.1 against 7.2). An arrow flown at the keystone counts too;
     * one at the mast doesn't. Dormant, a blow does nothing.
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
            if (!(onKey > onMast * 1.6F)) bad.add("the keystone took " + onKey + " and the mast " + onMast);
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
     * Its death breaks the Citadel's seal: everyone who fought it is given the Architect's Promise and the Architect's
     * Lattice and earns {@code void/defeat_architect}; it drops 8-12 Raw Lattice; and its arena is put back.
     */
    static void deathBreaksSeal(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        var server = level.getServer();
        AncientWard.reset(server);
        layDisc(helper);
        TheArchitect a = architect(helper);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        BlockPos floor = helper.absolutePos(MIDDLE);
        List<String> bad = new ArrayList<>();
        helper.runAtTickTime(2, () -> {
            hold(p, new Vec3(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() + 6.5));
            a.wake();
            a.raiseWall(level, p);
            a.hurtServer(level, level.damageSources().playerAttack(p), 5.0F);
            a.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
            if ((AncientWard.broken(server) & 0b100) == 0) bad.add("the Citadel's seal didn't break");
            if (p.getInventory().countItem(ModBlocks.ARCHITECTS_PROMISE.get().asItem()) != 1) bad.add("no Architect's Promise for its fighter");
            if (p.getInventory().countItem(ModItems.ARCHITECTS_LATTICE.get()) != 1) bad.add("no Architect's Lattice for its fighter");
            var defeated = server.getAdvancements().get(PromisedEnd.id("void/defeat_architect"));
            if (defeated == null || !p.getAdvancements().getOrStartProgress(defeated).isDone()) bad.add("no defeat_architect advancement");
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
            level.getServer().getPlayerList().remove(p);
            if (!bad.isEmpty()) helper.fail("the Architect's death: " + bad);
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

    /** The Citadel's template places one Architect, over the arena's hub, a block over its tiles. */
    static void citadelPlacesArchitect(GameTestHelper helper) {
        var template = helper.getLevel().getServer().getStructureTemplateManager().get(PromisedEnd.id("unfinished_citadel"));
        if (template.isEmpty()) {
            helper.fail("no Citadel template");
            return;
        }
        var saved = template.get().save(new net.minecraft.nbt.CompoundTag());
        var entities = saved.getListOrEmpty("entities");
        List<BlockPos> found = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            var e = entities.getCompoundOrEmpty(i);
            if (!e.getCompoundOrEmpty("nbt").getStringOr("id", "").equals("promisedend:the_architect")) continue;
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
        else helper.succeed();
    }
}

package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.entity.VoidLeviathan;
import com.panda.promisedend.logic.AncientWard;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModEntities;
import com.panda.promisedend.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

/** The Void Leviathan and its Rift Maw. */
final class LeviathanGameTests {
    private LeviathanGameTests() {}

    /**
     * Holds the chunks within {@code radius} of the test's corner entity-ticking: a breach at the pool's edge carries it
     * 13 blocks out, often into a chunk the test's own doesn't keep ticking. Returns what lets them go.
     */
    private static Runnable hold(GameTestHelper helper, int radius) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(2, 2, 2));
        List<long[]> held = new ArrayList<>();
        for (int cx = (at.getX() - radius) >> 4; cx <= (at.getX() + radius) >> 4; cx++) {
            for (int cz = (at.getZ() - radius) >> 4; cz <= (at.getZ() + radius) >> 4; cz++) {
                if (level.setChunkForced(cx, cz, true)) held.add(new long[] {cx, cz});
            }
        }
        return () -> held.forEach(c -> level.setChunkForced((int) c[0], (int) c[1], false));
    }

    private static VoidLeviathan spawn(GameTestHelper helper, boolean ai) {
        VoidLeviathan v = helper.spawn(ModEntities.VOID_LEVIATHAN.get(), new BlockPos(2, 2, 2));
        v.setNoAi(!ai);
        v.setHome(v.blockPosition());
        return v;
    }

    /**
     * Asleep or under the surface, a blow lands at a fifth (and a blow wakes it); reared, all of it: 20 becomes 18.72
     * through its armour of 8, and under the surface 4 becomes 3.04.
     */
    static void exposedWindowOnly(GameTestHelper helper) {
        var level = helper.getLevel();
        VoidLeviathan v = spawn(helper, false);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        List<String> bad = new ArrayList<>();
        if (v.state() != VoidLeviathan.State.DORMANT) bad.add("placed " + v.state() + ", not dormant");
        // each blow a second after the last, past its time unhurtable (in which a blow lands only past the last one)
        float[] took = new float[3];
        for (int i = 0; i < 3; i++) {
            int blow = i;
            helper.runAtTickTime(1 + 25 * i, () -> {
                if (blow == 2) v.rear(v.position().add(0, VoidLeviathan.SURFACE, 0));
                float before = v.getHealth();
                v.hurtServer(level, level.damageSources().playerAttack(p), 20.0F);
                took[blow] = before - v.getHealth();
                if (blow == 0 && v.state() != VoidLeviathan.State.SUBMERGED) bad.add("a blow didn't wake it: " + v.state());
            });
        }
        helper.runAtTickTime(60, () -> {
            PromisedEnd.LOGGER.info("[leviathan_exposed_window_only] a blow of 20 took {} asleep, {} under the surface, {} reared", took[0], took[1], took[2]);
            if (Math.abs(took[2] - 18.72F) > 0.05F) bad.add("reared, a blow of 20 took " + took[2] + ", not 18.72");
            if (Math.abs(took[0] - 3.04F) > 0.05F || Math.abs(took[1] - 3.04F) > 0.05F) bad.add("under the surface, a blow of 20 took " + took[1] + " (asleep " + took[0] + "), not 3.04");
            if (!v.fought().contains(p.getUUID())) bad.add("the player who struck it isn't counted as having fought it");
            level.getServer().getPlayerList().remove(p);
            if (!bad.isEmpty()) helper.fail("leviathan: " + bad);
            helper.succeed();
        });
    }

    /**
     * Woken by a player 6 from the pool's middle, it marks a ring under them, and only {@link VoidLeviathan#TELEGRAPH_TICKS}
     * and its rise later bursts up there: the player in the ring is struck, one 5 further out isn't. It rears there for
     * its four seconds, dives, and is back under the surface.
     */
    static void breachTelegraphs(GameTestHelper helper) {
        var level = helper.getLevel();
        VoidLeviathan v = spawn(helper, true);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        ServerPlayer far = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        Vec3 rest = v.position();
        double surface = rest.y + VoidLeviathan.SURFACE + 1;
        Vec3 stand = new Vec3(rest.x + 6, surface, rest.z), out = new Vec3(rest.x + 11, surface, rest.z);
        int[] began = {-1}, marked = {-1}, struck = {-1}, exposed = {-1}, dived = {-1};
        float[] health = {0};
        for (int t = 1; t <= 300; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                p.snapTo(stand.x, stand.y, stand.z, 90, 0);
                far.snapTo(out.x, out.y, out.z, 90, 0);
                if (began[0] < 0) {
                    if (level.isPositionEntityTicking(v.blockPosition())) {
                        began[0] = tick;
                        v.wake();
                    } else if (tick == 60) helper.fail("the arena's chunk wasn't entity-ticking by tick 60");
                    return;
                }
                if (marked[0] < 0 && v.state() == VoidLeviathan.State.TELEGRAPH) {
                    marked[0] = tick;
                    Vec3 at = v.breachPoint();
                    PromisedEnd.LOGGER.info("[leviathan_breach_telegraphs] marked {} ticks after it woke, at {}", tick - began[0], at);
                    if (at == null || Math.hypot(at.x - stand.x, at.z - stand.z) > 0.01 || Math.abs(at.y - surface) > 0.01) {
                        helper.fail("the ring isn't under the player at " + stand + ": " + at);
                    }
                }
                if (marked[0] > 0 && struck[0] < 0 && p.getHealth() < p.getMaxHealth()) {
                    struck[0] = tick;
                    health[0] = p.getHealth();
                    PromisedEnd.LOGGER.info("[leviathan_breach_telegraphs] the player was struck {} ticks after the ring, to {} health; it is at {}",
                            tick - marked[0], p.getHealth(), v.position());
                    if (tick - marked[0] < VoidLeviathan.TELEGRAPH_TICKS + VoidLeviathan.BREACH_TICKS - 1) {
                        helper.fail("struck " + (tick - marked[0]) + " ticks after the ring, before its telegraph and rise were done");
                    }
                    if (Math.abs(p.getMaxHealth() - p.getHealth() - VoidLeviathan.BREACH_DAMAGE) > 0.01F) helper.fail("the breach took " + (p.getMaxHealth() - p.getHealth()) + ", not " + VoidLeviathan.BREACH_DAMAGE);
                    if (far.getHealth() < far.getMaxHealth()) helper.fail("a player 5 outside the ring's middle was struck");
                    if (Math.hypot(v.getX() - stand.x, v.getZ() - stand.z) > 0.5) helper.fail("it breached at " + v.position() + ", not under the ring");
                }
                if (struck[0] > 0 && exposed[0] < 0 && v.state() == VoidLeviathan.State.EXPOSED) exposed[0] = tick;
                if (exposed[0] > 0 && dived[0] < 0 && v.state() == VoidLeviathan.State.DIVE) {
                    dived[0] = tick;
                    PromisedEnd.LOGGER.info("[leviathan_breach_telegraphs] reared {} ticks", tick - exposed[0]);
                    if (Math.abs(tick - exposed[0] - VoidLeviathan.EXPOSED_TICKS) > 2) helper.fail("reared " + (tick - exposed[0]) + " ticks, not " + VoidLeviathan.EXPOSED_TICKS);
                }
                if (dived[0] > 0 && v.state() == VoidLeviathan.State.SUBMERGED) {
                    PromisedEnd.LOGGER.info("[leviathan_breach_telegraphs] under again {} ticks after it dived, at {}, the next in {}", tick - dived[0], v.position(), v.timer());
                    if (v.getY() > surface - 4) helper.fail("under again, it is at " + v.position() + ", not below the surface");
                    if (v.timer() < VoidLeviathan.GAP_MIN - 2 || v.timer() > VoidLeviathan.GAP_MAX) helper.fail("at full health the next breach is " + v.timer() + " off, not 8-12 s");
                    if (v.breaches() != 1 || v.struck() != 1) helper.fail(v.breaches() + " breaches striking " + v.struck());
                    level.getServer().getPlayerList().remove(p);
                    level.getServer().getPlayerList().remove(far);
                    helper.succeed();
                }
                if (tick == 299) helper.fail("no full breach in 300 ticks: " + v.state() + ", marked " + marked[0] + ", struck " + struck[0] + ", reared " + exposed[0]);
            });
        }
    }

    /**
     * Below a fifth of its health, reared at the pool's edge, it draws a player on the ground 18 out toward it, a shove
     * of {@link VoidLeviathan#PULL_SHOVE} every {@link VoidLeviathan#PULL_EVERY} ticks, and not one in the air, nor one
     * already at the lip ({@link VoidLeviathan#HOLD_AT}); and below half its next breach follows straight after the dive.
     */
    static void pairsAndDraws(GameTestHelper helper) {
        var level = helper.getLevel();
        Runnable release = hold(helper, 20);
        VoidLeviathan v = spawn(helper, true);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        ServerPlayer flying = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        ServerPlayer lip = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        Vec3 rest = v.position();
        double surface = rest.y + VoidLeviathan.SURFACE + 1;
        Vec3 stand = new Vec3(rest.x + 18, surface, rest.z), air = new Vec3(rest.x + 18, surface + 1, rest.z + 3),
                edge = new Vec3(rest.x + 15, surface, rest.z - 3);
        int[] began = {-1}, shoves = {0}, lifted = {0}, held = {0}, ended = {0};
        double[] longest = {0};
        // where it will rear: in a chunk held ticking above, whose entities load in the background
        BlockPos rearsAt = BlockPos.containing(VoidLeviathan.breachPoint(rest, stand, surface));
        for (int t = 1; t <= 300; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (began[0] < 0) {
                    p.snapTo(stand.x, stand.y, stand.z, 90, 0);
                    flying.snapTo(air.x, air.y, air.z, 90, 0);
                    lip.snapTo(edge.x, edge.y, edge.z, 90, 0);
                    // the chunk map still has the players where they joined: told where they stand, it ticks the
                    // chunks round them as a real player's, the one where it rears among them
                    level.getChunkSource().move(p);
                    if (level.isPositionEntityTicking(v.blockPosition()) && level.isPositionEntityTicking(rearsAt)) {
                        began[0] = tick;
                        v.wake();
                        v.setHealth(v.getMaxHealth() * 0.15F);
                        v.rear(VoidLeviathan.breachPoint(rest, stand, surface));
                    } else if (tick == 150) {
                        release.run();
                        helper.fail("the arena's chunks weren't entity-ticking by tick 150: " + level.isPositionEntityTicking(v.blockPosition())
                                + ", where it rears " + level.isPositionEntityTicking(rearsAt));
                    }
                    return;
                }
                if (v.state() == VoidLeviathan.State.EXPOSED) {
                    // what the last tick gave each, then each set back where it stood
                    Vec3 got = p.getDeltaMovement(), up = flying.getDeltaMovement(), at = lip.getDeltaMovement();
                    if (Math.hypot(at.x, at.z) > 0.05) {
                        held[0]++;
                        Vec3 middle = Vec3.atBottomCenterOf(BlockPos.containing(rest));
                        var hurt = lip.getLastDamageSource();
                        PromisedEnd.LOGGER.info("[leviathan_pairs_and_draws] the lip player moved {} on tick {}: at {} ({} from the middle, held within {}), the Leviathan {} at {} ({} from them), its last hurt {}",
                                at, tick, lip.position(), String.format(java.util.Locale.ROOT, "%.2f", Math.hypot(lip.getX() - middle.x, lip.getZ() - middle.z)), VoidLeviathan.HOLD_AT,
                                v.state(), v.position(), String.format(java.util.Locale.ROOT, "%.2f", v.position().distanceTo(lip.position())), hurt == null ? "none" : hurt.getMsgId());
                    }
                    double flat = Math.hypot(got.x, got.z);
                    if (flat > 0.05) {
                        shoves[0]++;
                        longest[0] = Math.max(longest[0], flat);
                        if (got.x > -0.9 * flat) helper.fail("the draw shoved the player " + got + ", not toward it");
                    }
                    if (Math.hypot(up.x, up.z) > 0.05) lifted[0]++;
                    p.snapTo(stand.x, stand.y, stand.z, 90, 0);
                    p.setDeltaMovement(Vec3.ZERO);
                    p.setOnGround(true);
                    flying.snapTo(air.x, air.y, air.z, 90, 0);
                    flying.setDeltaMovement(Vec3.ZERO);
                    flying.setOnGround(false);
                    lip.snapTo(edge.x, edge.y, edge.z, 90, 0);
                    lip.setDeltaMovement(Vec3.ZERO);
                    lip.setOnGround(true);
                }
                if (v.state() == VoidLeviathan.State.SUBMERGED && ended[0]++ == 0) {
                    PromisedEnd.LOGGER.info("[leviathan_pairs_and_draws] reared, it drew {} times, the player read {} shoves (longest {}), the one in the air {}; the next breach in {}",
                            v.pulls(), shoves[0], String.format(java.util.Locale.ROOT, "%.3f", longest[0]), lifted[0], v.timer());
                    // the count is the Leviathan's own: the player's side misses the first and the last (read only while EXPOSED)
                    int want = VoidLeviathan.EXPOSED_TICKS / VoidLeviathan.PULL_EVERY;
                    if (Math.abs(v.pulls() - want) > 1) helper.fail(v.pulls() + " draws in its four seconds, not " + want);
                    if (shoves[0] < want - 3) helper.fail("the player felt " + shoves[0] + " of its " + v.pulls() + " draws");
                    if (Math.abs(longest[0] - VoidLeviathan.PULL_SHOVE) > 0.05) helper.fail("a shove of " + longest[0] + ", not " + VoidLeviathan.PULL_SHOVE);
                    if (lifted[0] > 0) helper.fail("a player in the air was drawn " + lifted[0] + " times");
                    if (held[0] > 0) helper.fail("a player already at the lip was drawn " + held[0] + " times, toward the pool");
                    if (v.timer() > VoidLeviathan.PAIR_GAP) helper.fail("below half, the next breach is " + v.timer() + " off, not a pair's " + VoidLeviathan.PAIR_GAP);
                    level.getServer().getPlayerList().remove(p);
                    level.getServer().getPlayerList().remove(flying);
                    level.getServer().getPlayerList().remove(lip);
                    release.run();
                    helper.succeed();
                }
                if (tick == began[0] + 199) {
                    release.run();
                    helper.fail("it never dived: " + v.state() + " at " + v.position() + ", " + v.timer() + " left, ticking "
                            + level.isPositionEntityTicking(v.blockPosition()));
                }
            });
        }
    }

    /**
     * Its death breaks the Maw's seal (and with every guardian in, the Ward still stands on the third), gives the
     * player beside it the advancement and a Star Chart to the Unfinished Citadel, and drops 6 to 10 Voidsteel Scrap
     * and 2 Leviathan Scales.
     */
    static void deathBreaksSeal(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        AncientWard.reset(server);
        Runnable release = hold(helper, 20);
        VoidLeviathan v = spawn(helper, false);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        p.snapTo(v.getX() + 4, v.getY(), v.getZ(), 0, 0);
        v.wake();
        v.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
        List<String> bad = new ArrayList<>();
        if ((AncientWard.broken(server) & 2) == 0) bad.add("the Maw's seal didn't break");
        if ((AncientWard.broken(server) & 1) != 0) bad.add("the Foundry's seal broke with it");
        var defeated = server.getAdvancements().get(PromisedEnd.id("void/defeat_leviathan"));
        if (defeated == null || !p.getAdvancements().getOrStartProgress(defeated).isDone()) bad.add("no defeat_leviathan advancement");
        boolean chart = false;
        for (var stack : p.getInventory().getNonEquipmentItems()) chart |= stack.is(ModItems.STAR_CHART.get())
                && PromisedEnd.id("on_citadel_charts").equals(stack.get(com.panda.promisedend.registry.ModComponents.STAR_CHART.get()));
        if (!chart) bad.add("no Star Chart to the Citadel given");
        helper.runAfterDelay(2, () -> {
            // anywhere about the pool: it was carried to the lip
            int scrap = level.getEntitiesOfClass(ItemEntity.class, v.getBoundingBox().inflate(VoidLeviathan.POOL + 6)).stream()
                    .filter(i -> i.getItem().is(ModItems.VOIDSTEEL_SCRAP.get())).mapToInt(i -> i.getItem().getCount()).sum();
            PromisedEnd.LOGGER.info("[leviathan_death_breaks_seal] it dropped {} Voidsteel Scrap", scrap);
            if (scrap < 6 || scrap > 10) bad.add(scrap + " scrap dropped, not 6-10");
            int templates = level.getEntitiesOfClass(ItemEntity.class, v.getBoundingBox().inflate(VoidLeviathan.POOL + 6)).stream()
                    .filter(i -> i.getItem().is(ModItems.VOIDSTEEL_UPGRADE_SMITHING_TEMPLATE.get())).mapToInt(i -> i.getItem().getCount()).sum();
            if (templates != 1) bad.add(templates + " Voidsteel templates dropped, not 1");
            int fins = level.getEntitiesOfClass(ItemEntity.class, v.getBoundingBox().inflate(VoidLeviathan.POOL + 6)).stream()
                    .filter(i -> i.getItem().is(ModItems.LEVIATHAN_FIN.get())).mapToInt(i -> i.getItem().getCount()).sum();
            if (fins != 1) bad.add(fins + " Leviathan Fins dropped, not 1");
            // one ingot's Starmetal each kill, with the Starfall on or off
            int starmetal = level.getEntitiesOfClass(ItemEntity.class, v.getBoundingBox().inflate(VoidLeviathan.POOL + 6)).stream()
                    .filter(i -> i.getItem().is(ModItems.STARMETAL.get())).mapToInt(i -> i.getItem().getCount()).sum();
            if (starmetal != 2) bad.add(starmetal + " Starmetal dropped, not 2");
            int scales = level.getEntitiesOfClass(ItemEntity.class, v.getBoundingBox().inflate(VoidLeviathan.POOL + 6)).stream()
                    .filter(i -> i.getItem().is(ModItems.LEVIATHAN_SCALE.get())).mapToInt(i -> i.getItem().getCount()).sum();
            if (scales != 2) bad.add(scales + " Leviathan Scales dropped, not 2");
            Vec3 lip = VoidLeviathan.lip(Vec3.atBottomCenterOf(v.home()), p.position(), v.surface());
            long there = level.getEntitiesOfClass(ItemEntity.class, new net.minecraft.world.phys.AABB(lip, lip).inflate(1.5, 3, 1.5)).size();
            if (there == 0) bad.add("nothing it dropped was carried to the lip at " + lip);
            AncientWard.reset(server);
            server.getPlayerList().remove(p);
            release.run();
            if (!bad.isEmpty()) helper.fail("leviathan death: " + bad);
            helper.succeed();
        });
    }

    /**
     * The Maw's template sets one Leviathan down at the pool's middle {@link VoidLeviathan#SURFACE} under the floor's
     * block, with the pool open above it to the sky ({@link VoidLeviathan#POOL} across from the middle at the floor),
     * eight ancient Updraft Vents on its lip and a chest.
     */
    static void riftMawPlaces(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var template = server.getStructureTemplateManager().get(PromisedEnd.id("rift_maw"));
        if (template.isEmpty()) {
            helper.fail("no Maw template");
            return;
        }
        var saved = template.get().save(new net.minecraft.nbt.CompoundTag());
        var entities = saved.getListOrEmpty("entities");
        List<BlockPos> leviathans = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            var e = entities.getCompoundOrEmpty(i);
            if (!e.getCompoundOrEmpty("nbt").getStringOr("id", "").equals("promisedend:void_leviathan")) continue;
            var pos = e.getListOrEmpty("blockPos");
            leviathans.add(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)));
        }
        if (leviathans.size() != 1) {
            helper.fail("the Maw places " + leviathans.size() + " Leviathans");
            return;
        }
        BlockPos rest = leviathans.getFirst();
        var settings = new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings();
        // the template keeps the air it clears as blocks too: what the island and its lip are made of
        java.util.Set<BlockPos> solid = new java.util.HashSet<>();
        for (var kind : List.of(ModBlocks.VOIDSTONE_SET.base().get(), ModBlocks.CHISELED_VOIDSTONE.get(), net.minecraft.world.level.block.Blocks.END_STONE,
                ModBlocks.UPDRAFT_VENT.get())) {
            for (var block : template.get().filterBlocks(BlockPos.ZERO, settings, kind)) solid.add(block.pos());
        }
        List<String> bad = new ArrayList<>();
        int floor = rest.getY() + VoidLeviathan.SURFACE;
        for (int y = rest.getY(); y < template.get().getSize().getY(); y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) if (solid.contains(new BlockPos(rest.getX() + dx, y, rest.getZ() + dz))) bad.add("the pool is shut at " + dx + ", " + y + ", " + dz);
            }
        }
        if (solid.contains(new BlockPos(rest.getX() + VoidLeviathan.POOL, floor, rest.getZ()))) bad.add("the floor reaches into the pool at " + VoidLeviathan.POOL);
        if (!solid.contains(new BlockPos(rest.getX() + VoidLeviathan.POOL + 1, floor, rest.getZ()))) bad.add("no lip at " + (VoidLeviathan.POOL + 1));
        var vents = template.get().filterBlocks(BlockPos.ZERO, settings, ModBlocks.UPDRAFT_VENT.get());
        if (vents.size() != 8) bad.add(vents.size() + " vents");
        for (var vent : vents) {
            double r = Math.hypot(vent.pos().getX() - rest.getX(), vent.pos().getZ() - rest.getZ());
            if (vent.pos().getY() != floor || r > VoidLeviathan.POOL + 3) bad.add("a vent at " + vent.pos() + ", not on the lip");
            if (!vent.state().getValue(com.panda.promisedend.block.UpdraftVentBlock.ANCIENT)) bad.add("a vent at " + vent.pos() + " isn't ancient");
        }
        if (template.get().filterBlocks(BlockPos.ZERO, settings, net.minecraft.world.level.block.Blocks.CHEST).size() != 1) bad.add("not one chest");
        if (!bad.isEmpty()) helper.fail("the Maw: " + bad.subList(0, Math.min(6, bad.size())));
        helper.succeed();
    }
}

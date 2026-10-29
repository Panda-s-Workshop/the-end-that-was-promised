package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.entity.HollowSentinel;
import com.panda.promisedend.entity.PurpurSentry;
import com.panda.promisedend.entity.ResonantBullet;
import com.panda.promisedend.logic.AncientWard;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModEntities;
import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.worldgen.EndBiomes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.phys.Vec3;

/** The Hollow Sentinel and its Shulker Foundry. */
final class SentinelGameTests {
    private SentinelGameTests() {}

    private static HollowSentinel still(GameTestHelper helper) {
        HollowSentinel s = helper.spawn(ModEntities.HOLLOW_SENTINEL.get(), new BlockPos(2, 2, 2));
        s.setNoAi(true);
        s.setHome(s.blockPosition());
        return s;
    }

    /**
     * Dormant or closed it takes nothing (a blow on a dormant one wakes it); open, a blow lands at half again: 10
     * becomes 15, 13.5 through its armour of 10, where 10 alone would have been 8.
     */
    static void closedIsInvulnerable(GameTestHelper helper) {
        var level = helper.getLevel();
        HollowSentinel s = still(helper);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        List<String> bad = new ArrayList<>();
        float full = s.getMaxHealth();
        if (s.state() != HollowSentinel.State.DORMANT) bad.add("placed " + s.state() + ", not dormant");
        if (s.hurtServer(level, level.damageSources().playerAttack(p), 10.0F) || s.getHealth() != full) bad.add("a dormant Sentinel was hurt");
        if (s.state() != HollowSentinel.State.CLOSED) bad.add("a blow didn't wake it: " + s.state());
        if (s.hurtServer(level, level.damageSources().playerAttack(p), 10.0F) || s.getHealth() != full) bad.add("a closed Sentinel was hurt");
        s.crackOpen();
        s.hurtServer(level, level.damageSources().playerAttack(p), 10.0F);
        float took = full - s.getHealth();
        PromisedEnd.LOGGER.info("[sentinel_closed_is_invulnerable] open, a blow of 10 took {}", took);
        if (Math.abs(took - 13.5F) > 0.01F) bad.add("open, a blow of 10 took " + took + ", not 13.5");
        helper.getLevel().getServer().getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("sentinel: " + bad);
        helper.succeed();
    }

    /**
     * Three of its bullets struck back by a player crack it open; a bullet out passes through it, and one that reaches
     * the player hurts and lifts them.
     */
    static void opensAfterThreeReturns(GameTestHelper helper) {
        var level = helper.getLevel();
        HollowSentinel s = still(helper);
        s.wake();
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        Vec3 stand = s.position().add(0, 0, 6);
        int[] began = {-1}, opened = {-1};
        ResonantBullet[] last = {null};
        for (int t = 1; t <= 190; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                p.snapTo(stand.x, stand.y, stand.z, 180, 0);
                if (began[0] < 0) {
                    if (level.isPositionEntityTicking(s.blockPosition())) began[0] = tick;
                    else if (tick == 60) helper.fail("the arena's chunk wasn't entity-ticking by tick 60");
                    return;
                }
                int in = tick - began[0];
                if (in < 60 && in % 20 == 1) {
                    // a bullet half way out, struck back by the player
                    var bullet = new ResonantBullet(level, s, p, ResonantBullet.SPEED, ResonantBullet.DAMAGE);
                    bullet.setPos(s.position().add(0, 1.5, 3));
                    level.addFreshEntity(bullet);
                    p.attack(bullet);
                    if (!bullet.returned()) helper.fail("a player's blow didn't send a bullet back");
                    last[0] = bullet;
                }
                if (opened[0] < 0 && s.state() == HollowSentinel.State.OPEN) {
                    opened[0] = in;
                    PromisedEnd.LOGGER.info("[sentinel_opens_after_three_returns] open {} ticks after the first was struck back", in);
                    if (in < 40) helper.fail("open after " + in + " ticks, before the third bullet was struck");
                }
                if (in == 100) {
                    if (opened[0] < 0) helper.fail("three bullets struck back and it never opened: " + s.state() + ", " + s.returns() + " returns");
                    // a bullet that isn't struck: through the Sentinel (it starts in its core) and on to the player
                    float health = p.getHealth();
                    var out = new ResonantBullet(level, s, p, ResonantBullet.SPEED, ResonantBullet.DAMAGE);
                    level.addFreshEntity(out);
                    last[0] = out;
                    p.setHealth(health);
                }
                if (in == 150) {
                    PromisedEnd.LOGGER.info("[sentinel_opens_after_three_returns] a bullet left alone: the player at {} health, levitating {}",
                            p.getHealth(), p.hasEffect(MobEffects.LEVITATION));
                    if (p.getHealth() >= p.getMaxHealth() || !p.hasEffect(MobEffects.LEVITATION)) {
                        helper.fail("a bullet left alone didn't hurt and lift the player: health " + p.getHealth() + ", " + last[0]);
                    }
                    if (s.hits() != 1) helper.fail(s.hits() + " hits counted");
                    // its six seconds bare aren't cut short
                    if (in - opened[0] < HollowSentinel.OPEN_TICKS && s.state() != HollowSentinel.State.OPEN) {
                        helper.fail("open at " + opened[0] + ", " + s.state() + " again by " + in);
                    }
                    level.getServer().getPlayerList().remove(p);
                    helper.succeed();
                }
            });
        }
    }

    /**
     * A live Sentinel's volleys at a player who never strikes back: its bullets fly, none turns back on it (a volley's
     * three once met and turned each other back, cracking its shell by themselves), and it stays closed.
     */
    static void volleysNeverCrackItself(GameTestHelper helper) {
        var level = helper.getLevel();
        HollowSentinel s = helper.spawn(ModEntities.HOLLOW_SENTINEL.get(), new BlockPos(2, 2, 2));
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        Vec3 stand = s.position().add(0, 0, 10);
        int[] fired = {0};
        Set<Integer> seen = new HashSet<>();
        for (int t = 1; t <= 230; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                p.snapTo(stand.x, stand.y, stand.z, 180, 0);
                p.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.RESISTANCE, 40, 4));
                p.setHealth(p.getMaxHealth());
                for (var b : level.getEntitiesOfClass(ResonantBullet.class, s.getBoundingBox().inflate(32))) {
                    if (seen.add(b.getId())) fired[0]++;
                    if (b.returned()) helper.fail("a bullet turned back on its own, " + tick + " ticks in");
                }
                if (s.state() == HollowSentinel.State.OPEN || s.returns() > 0) helper.fail("it cracked with no blow struck: " + s.state() + ", " + s.returns() + " returns");
                if (tick == 230) {
                    PromisedEnd.LOGGER.info("[sentinel_volleys_never_crack_itself] {} bullets fired in 230 ticks, {} hit, {}", fired[0], s.hits(), s.state());
                    if (fired[0] < 3) helper.fail("only " + fired[0] + " bullets fired in 230 ticks");
                    level.getServer().getPlayerList().remove(p);
                    helper.succeed();
                }
            });
        }
    }

    /**
     * Past two thirds of its health it calls two Purpur Sentries, closes and its bullets speed up a quarter; past a
     * third two more. Phases go forward only.
     */
    static void phasesCallSentries(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        if (HollowSentinel.phaseFor(0.2F, 0) != 2) bad.add("0.2 from the first phase isn't the third");
        if (HollowSentinel.phaseFor(0.5F, 0) != 1) bad.add("0.5 isn't the second phase");
        if (HollowSentinel.phaseFor(0.9F, 1) != 1) bad.add("a phase went back");
        HollowSentinel s = still(helper);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        s.wake();
        s.crackOpen();
        s.hurtServer(level, level.damageSources().playerAttack(p), 80.0F);
        int sentries = level.getEntitiesOfClass(PurpurSentry.class, s.getBoundingBox().inflate(16)).size();
        PromisedEnd.LOGGER.info("[sentinel_phases_call_sentries] at {} health: phase {}, {}, {} sentries, bullets {}",
                s.getHealth(), s.phase(), s.state(), sentries, s.bulletSpeed());
        if (s.phase() != 1 || s.state() != HollowSentinel.State.CLOSED || sentries != 2) bad.add("past two thirds: phase " + s.phase() + ", " + s.state() + ", " + sentries + " sentries");
        if (Math.abs(s.bulletSpeed() - ResonantBullet.SPEED * 1.25) > 1e-9) bad.add("phase 1 bullets at " + s.bulletSpeed());
        helper.runAfterDelay(15, () -> {
            s.crackOpen();
            s.hurtServer(level, level.damageSources().playerAttack(p), 80.0F);
            int more = level.getEntitiesOfClass(PurpurSentry.class, s.getBoundingBox().inflate(16)).size();
            if (s.phase() != 2 || more != 4) bad.add("past a third: phase " + s.phase() + ", " + more + " sentries");
            level.getServer().getPlayerList().remove(p);
            if (!bad.isEmpty()) helper.fail("sentinel phases: " + bad);
            helper.succeed();
        });
    }

    /**
     * Its death breaks the Foundry's seal; a player beside it (who never struck it) gets an Ancient Core and the
     * advancement; it drops the Resonite Upgrade. The seals are left mended.
     */
    static void deathBreaksSeal(GameTestHelper helper) {
        var level = helper.getLevel();
        var server = level.getServer();
        AncientWard.reset(server);
        HollowSentinel s = still(helper);
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        p.snapTo(s.getX() + 4, s.getY(), s.getZ(), 0, 0);
        s.wake();
        s.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
        List<String> bad = new ArrayList<>();
        if ((AncientWard.broken(server) & 1) == 0) bad.add("the Foundry's seal didn't break");
        if (p.getInventory().countItem(ModItems.ANCIENT_CORE.get()) != 1) bad.add("the player beside it has " + p.getInventory().countItem(ModItems.ANCIENT_CORE.get()) + " cores");
        var defeated = server.getAdvancements().get(PromisedEnd.id("outer/defeat_sentinel"));
        if (defeated == null || !p.getAdvancements().getOrStartProgress(defeated).isDone()) bad.add("no defeat_sentinel advancement");
        helper.runAfterDelay(2, () -> {
            boolean template = level.getEntitiesOfClass(ItemEntity.class, s.getBoundingBox().inflate(4))
                    .stream().anyMatch(i -> i.getItem().is(ModItems.RESONITE_UPGRADE_SMITHING_TEMPLATE.get()));
            if (!template) bad.add("no Resonite Upgrade dropped");
            int scrap = level.getEntitiesOfClass(ItemEntity.class, s.getBoundingBox().inflate(4)).stream()
                    .filter(i -> i.getItem().is(ModItems.ANCIENT_PURPUR_SCRAP.get())).mapToInt(i -> i.getItem().getCount()).sum();
            PromisedEnd.LOGGER.info("[sentinel_death_breaks_seal] it dropped {} Ancient Purpur Scrap", scrap);
            if (scrap < 6 || scrap > 10) bad.add(scrap + " scrap dropped, not 6-10");
            AncientWard.reset(server);
            server.getPlayerList().remove(p);
            if (!bad.isEmpty()) helper.fail("sentinel death: " + bad);
            helper.succeed();
        });
    }

    /**
     * Cracked open while it hovers over a player, it is down on its rest within 20 ticks (a flyer's drift took much of
     * its six seconds), and stays open its six seconds with the player gone from the arena.
     */
    static void opensOntoItsRest(GameTestHelper helper) {
        var level = helper.getLevel();
        HollowSentinel s = helper.spawn(ModEntities.HOLLOW_SENTINEL.get(), new BlockPos(2, 2, 2));
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        Vec3 at = s.position();
        int[] opened = {-1}, down = {-1};
        for (int t = 1; t <= 230; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (opened[0] < 0) {
                    p.snapTo(at.x + 8, at.y, at.z, 90, 0);
                    p.setHealth(p.getMaxHealth());
                    if (tick >= 60 && s.state() == HollowSentinel.State.CLOSED && s.getY() > at.y + 2.5) {
                        s.crackOpen();
                        opened[0] = tick;
                        // out of the arena at once: its window runs without a target
                        p.snapTo(at.x + 80, at.y, at.z, 90, 0);
                    } else if (tick == 120) helper.fail("it never rose to its hover: " + s.state() + " at " + s.position() + ", its rest " + at);
                    return;
                }
                int in = tick - opened[0];
                if (down[0] < 0 && s.position().distanceTo(Vec3.atBottomCenterOf(s.home())) < 0.6) {
                    down[0] = in;
                    PromisedEnd.LOGGER.info("[sentinel_opens_onto_its_rest] on its rest {} ticks after it opened", in);
                }
                if (in == 20 && down[0] < 0) helper.fail("open 20 ticks and still " + s.position().distanceTo(Vec3.atBottomCenterOf(s.home())) + " from its rest");
                if (in == HollowSentinel.OPEN_TICKS - 5 && s.state() != HollowSentinel.State.OPEN) helper.fail("closed again by " + in + " ticks with no one there");
                if (in == HollowSentinel.OPEN_TICKS + 5) {
                    if (s.state() != HollowSentinel.State.CLOSED) helper.fail("still " + s.state() + " " + in + " ticks after it opened, with no one there");
                    level.getServer().getPlayerList().remove(p);
                    helper.succeed();
                }
            });
        }
    }

    /**
     * The Waystation's chest in the test world, which makes no structures: its Star Chart, finding no Foundry, is left
     * out, never a blank map (a blank, unnamed map is what the chest gave while its functions went unread). The chart
     * itself is checked in a real world by the server check.
     */
    static void waystationChartNeverBlank(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(net.minecraft.world.level.Level.END);
        if (end == null) {
            helper.fail("no End");
            return;
        }
        var table = end.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, PromisedEnd.id("chests/waystation")));
        List<String> bad = new ArrayList<>();
        // the arrival's kit: one chart, sealed on the Foundry
        int charts = 0;
        for (var item : com.panda.promisedend.logic.Arrivals.kit()) {
            if (item.is(net.minecraft.world.item.Items.MAP) || item.is(net.minecraft.world.item.Items.FILLED_MAP)) bad.add("a vanilla map in the kit");
            if (item.is(com.panda.promisedend.registry.ModItems.STAR_CHART.get())) {
                charts++;
                var kind = item.get(com.panda.promisedend.registry.ModComponents.STAR_CHART.get());
                if (kind == null || !kind.equals(PromisedEnd.id("on_foundry_charts"))) bad.add("a kit chart of " + kind);
            }
        }
        if (charts != 1) bad.add(charts + " Star Charts in the kit");
        // the chest shares no map, no chart and no Voidstone (which would keep Act I's stone goal unearned)
        for (int i = 0; i < 20; i++) {
            var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(end)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, new Vec3(1500.5, 64, 0.5))
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
            for (var item : table.getRandomItems(params)) {
                if (item.is(net.minecraft.world.item.Items.MAP) || item.is(net.minecraft.world.item.Items.FILLED_MAP) || item.is(com.panda.promisedend.registry.ModItems.STAR_CHART.get())
                        || item.is(com.panda.promisedend.registry.ModBlocks.VOIDSTONE_SET.base().get().asItem())) bad.add("the chest rolled " + item);
            }
        }
        if (!bad.isEmpty()) helper.fail("the Waystation's kit and chest: " + bad);
        helper.succeed();
    }

    /** It wakes for a player in survival within 20 blocks, with its bar, and not for one in creative beside it. */
    static void wakesForSurvival(GameTestHelper helper) {
        var level = helper.getLevel();
        HollowSentinel s = helper.spawn(ModEntities.HOLLOW_SENTINEL.get(), new BlockPos(2, 2, 2));
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.CREATIVE);
        Vec3 at = s.position();
        helper.runAtTickTime(1, () -> p.snapTo(at.x + 5, at.y, at.z, 0, 0));
        helper.runAtTickTime(40, () -> {
            if (s.state() != HollowSentinel.State.DORMANT) helper.fail("it woke for a player in creative: " + s.state());
            p.setGameMode(GameType.SURVIVAL);
            p.snapTo(at.x + 15, at.y, at.z, 0, 0);
        });
        helper.runAtTickTime(70, () -> {
            PromisedEnd.LOGGER.info("[sentinel_wakes_for_survival] 30 ticks after a survival player came within 15: {}, bar {} with {}",
                    s.state(), s.bar().isVisible(), s.bar().getPlayers().size());
            if (s.state() == HollowSentinel.State.DORMANT) helper.fail("it didn't wake for a survival player 15 off");
            else if (!s.bar().isVisible() || !s.bar().getPlayers().contains(p)) helper.fail("woken, its bar isn't shown to the player");
            else if (!p.entityTags().contains(HollowSentinel.HINTED)) helper.fail("the player wasn't told how it is fought");
            level.getServer().getPlayerList().remove(p);
            helper.succeed();
        });
    }

    /**
     * The Foundry's template places one Sentinel, resting over the dais's chiseled heart, with the four pads'
     * chiseled hearts at {@link HollowSentinel#PAD} on the diagonals a step below it.
     */
    static void foundryPlacesSentinel(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var template = server.getStructureTemplateManager().get(PromisedEnd.id("shulker_foundry"));
        if (template.isEmpty()) {
            helper.fail("no Foundry template");
            return;
        }
        var saved = template.get().save(new net.minecraft.nbt.CompoundTag());
        var entities = saved.getListOrEmpty("entities");
        List<BlockPos> sentinels = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            var e = entities.getCompoundOrEmpty(i);
            if (!e.getCompoundOrEmpty("nbt").getStringOr("id", "").equals("promisedend:hollow_sentinel")) continue;
            var pos = e.getListOrEmpty("blockPos");
            sentinels.add(new BlockPos(pos.getIntOr(0, 0), pos.getIntOr(1, 0), pos.getIntOr(2, 0)));
        }
        if (sentinels.size() != 1) {
            helper.fail("the Foundry places " + sentinels.size() + " Sentinels");
            return;
        }
        BlockPos rest = sentinels.getFirst();
        Set<BlockPos> chiseled = new HashSet<>();
        template.get().filterBlocks(BlockPos.ZERO, new net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings(),
                ModBlocks.CHISELED_VOIDSTONE.get()).forEach(b -> chiseled.add(b.pos()));
        List<String> bad = new ArrayList<>();
        int pad = HollowSentinel.PAD;
        for (BlockPos want : List.of(rest.below(), rest.offset(pad, -1, pad), rest.offset(-pad, -1, pad), rest.offset(pad, -1, -pad), rest.offset(-pad, -1, -pad))) {
            if (!chiseled.contains(want)) bad.add("no chiseled heart at " + want + " (the Sentinel rests at " + rest + ")");
        }
        if (!bad.isEmpty()) helper.fail("the Foundry: " + bad);
        helper.succeed();
    }

    /**
     * A held pearl finds a Foundry within its search ({@code PearlGuide.SEARCH} chunks) from anywhere a portal sets a
     * player down. A survey that generates no chunks: over 16 seeds, from eight points 1500 out, every placement
     * cell's candidate chunk in reach whose biome the Foundry allows.
     */
    static void foundriesWithinReach(GameTestHelper helper) {
        var access = helper.getLevel().registryAccess();
        ChunkGenerator generator = access.lookupOrThrow(Registries.WORLD_PRESET).getOrThrow(WorldPresets.NORMAL).value()
                .createWorldDimensions().get(LevelStem.END).orElseThrow().generator();
        var noise = (NoiseBasedChunkGenerator) generator;
        var set = access.lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(ResourceKey.create(Registries.STRUCTURE_SET, PromisedEnd.id("shulker_foundry")));
        if (!(set.value().placement() instanceof RandomSpreadStructurePlacement placement)) {
            helper.fail("the Foundry's placement isn't a random spread");
            return;
        }
        var allowed = set.value().structures().getFirst().structure().value().biomes();
        int spacing = placement.spacing(), reach = com.panda.promisedend.logic.PearlGuide.SEARCH;
        long previous = EndBiomes.seed();
        List<String> bad = new ArrayList<>(), report = new ArrayList<>();
        try {
            for (long seed = 1; seed <= 16; seed++) {
                EndBiomes.setSeed(seed);
                RandomState random = RandomState.create(access.lookupOrThrow(Registries.NOISE), seed, noise.generatorSettings().value());
                BiomeResolver biomes = generator.getBiomeSource().createUncachedResolver(random);
                int worst = 0;
                for (int k = 0; k < 8; k++) {
                    double a = Math.PI / 4 * k;
                    int px = (int) (1500 * Math.cos(a)) >> 4, pz = (int) (1500 * Math.sin(a)) >> 4;
                    int nearest = Integer.MAX_VALUE;
                    for (int gx = Math.floorDiv(px - reach, spacing); gx <= Math.floorDiv(px + reach, spacing); gx++) {
                        for (int gz = Math.floorDiv(pz - reach, spacing); gz <= Math.floorDiv(pz + reach, spacing); gz++) {
                            var chunk = placement.getPotentialStructureChunk(seed, gx * spacing, gz * spacing);
                            int d = (int) Math.hypot(chunk.x() - px, chunk.z() - pz);
                            if (d > reach || d >= nearest) continue;
                            if (allowed.contains(biomes.getNoiseBiome((chunk.getMinBlockX() + 8) >> 2, 62 >> 2, (chunk.getMinBlockZ() + 8) >> 2))) nearest = d;
                        }
                    }
                    if (nearest == Integer.MAX_VALUE) bad.add("seed " + seed + ": none within reach of the point " + (px * 16) + ", " + (pz * 16));
                    else worst = Math.max(worst, nearest * 16);
                }
                report.add(seed + ":" + worst);
            }
        } finally {
            EndBiomes.setSeed(previous);
        }
        PromisedEnd.LOGGER.info("[foundries_within_reach] the farthest nearest Foundry from a point 1500 out, per seed (blocks): {}", report);
        if (!bad.isEmpty()) helper.fail("Foundries out of a pearl's reach: " + bad);
        helper.succeed();
    }
}

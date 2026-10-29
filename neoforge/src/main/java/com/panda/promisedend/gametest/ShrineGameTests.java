package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.block.ShrineAltarBlockEntity;
import com.panda.promisedend.logic.Offerings;
import com.panda.promisedend.logic.ShrineBoons;
import com.panda.promisedend.logic.VoidAnchor;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModEffects;
import com.panda.promisedend.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** The Void Shrine's altar and its six boons. */
final class ShrineGameTests {
    private ShrineGameTests() {}

    /**
     * Offerings at a Shrine Altar: two Chorus Resin are two favour and no offer; a third opens three distinct boons;
     * a boon not offered is refused; an offered one is given for its time, the favour spent; the altar then rests for
     * that player (an offering is refused and not taken) while it would still answer another; a non-offering does
     * nothing. A Dragon Scale is worth 3 by itself.
     */
    static void shrineGrantsBuff(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(at, ModBlocks.SHRINE_ALTAR.get().defaultBlockState());
        if (!(level.getBlockEntity(at) instanceof ShrineAltarBlockEntity altar)) {
            helper.fail("no altar block entity");
            return;
        }
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        ServerPlayer other = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        p.snapTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 2.5, 180, 0);
        other.snapTo(at.getX() + 2.5, at.getY() + 1, at.getZ() + 0.5, 90, 0);
        List<String> bad = new ArrayList<>();
        ItemStack resin = new ItemStack(ModItems.CHORUS_RESIN.get(), 5);
        if (ShrineAltarBlockEntity.worth(resin) != 1 || ShrineAltarBlockEntity.worth(new ItemStack(ModItems.ANCIENT_CORE.get())) != 5
                || ShrineAltarBlockEntity.worth(new ItemStack(Items.DIAMOND)) != 0) bad.add("favour values: resin " + ShrineAltarBlockEntity.worth(resin));
        altar.offer(p, resin);
        altar.offer(p, resin);
        if (altar.favour(p.getUUID()) != 2 || !altar.offered(p.getUUID()).isEmpty()) bad.add("two resin: favour " + altar.favour(p.getUUID()) + ", offered " + altar.offered(p.getUUID()));
        altar.offer(p, resin);
        var offered = altar.offered(p.getUUID());
        if (resin.getCount() != 2) bad.add("three offerings took " + (5 - resin.getCount()) + " resin");
        if (offered.size() != 3 || offered.stream().distinct().count() != 3) bad.add("offered " + offered);
        int notOffered = -1;
        for (int i = 0; i < Offerings.BOONS.length; i++) if (!offered.contains(i)) notOffered = i;
        if (altar.choose(p, notOffered)) bad.add("a boon not offered was given");
        int pick = offered.getFirst();
        if (!altar.choose(p, pick)) bad.add("the offered boon was refused");
        var effect = p.getEffect(ModEffects.boon(pick));
        int want = Offerings.BOONS[pick].equals("void_sight") ? ShrineBoons.VOID_SIGHT_DURATION : ShrineBoons.DURATION;
        if (effect == null || effect.getDuration() != want) bad.add(Offerings.BOONS[pick] + ": " + effect);
        if (altar.favour(p.getUUID()) != 0 || !altar.offered(p.getUUID()).isEmpty()) bad.add("after the boon: favour " + altar.favour(p.getUUID()));
        if (altar.choose(p, pick)) bad.add("the same boon given twice");
        // resting for p, not for another player
        int before = resin.getCount();
        if (altar.offer(p, resin) || resin.getCount() != before) bad.add("a resting altar took an offering");
        if (altar.waitLeft(p) < 23000) bad.add("the wait is " + altar.waitLeft(p));
        ItemStack scale = new ItemStack(ModItems.DRAGON_SCALE.get());
        altar.offer(other, scale);
        if (altar.offered(other.getUUID()).size() != 3 || !scale.isEmpty()) bad.add("another player's Dragon Scale: offered " + altar.offered(other.getUUID()));
        PromisedEnd.LOGGER.info("[shrine_grants_buff] offered {} for three resin, gave {} ({} ticks); another player's scale offered {}", offered,
                Offerings.BOONS[pick], effect == null ? 0 : effect.getDuration(), altar.offered(other.getUUID()));
        // through the block, as a click: an offering in hand is taken
        ItemStack lumen = new ItemStack(ModItems.LUMEN.get(), 2);
        altar.reset();
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, lumen);
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at).add(0, 0.5, 0), net.minecraft.core.Direction.UP, at, false);
        level.getBlockState(at).useItemOn(lumen, level, p, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        if (lumen.getCount() != 1 || altar.favour(p.getUUID()) != 1) bad.add("a click with Lumen: " + lumen.getCount() + " left, favour " + altar.favour(p.getUUID()));
        level.getServer().getPlayerList().remove(p);
        level.getServer().getPlayerList().remove(other);
        if (!bad.isEmpty()) helper.fail("shrine: " + bad);
        helper.succeed();
    }

    /**
     * What the boons do: Shulker Ward keeps Levitation off; Resonant Edge is a tenth more attack damage; Pearl Sense
     * ends a pearl's cooldown in half its time; Rift Step catches a fall into the void with no anchor carried, and is
     * spent (without it, and without an anchor, nothing catches it).
     */
    static void boonsDoTheirWork(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        p.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        List<String> bad = new ArrayList<>();
        // Shulker Ward
        ShrineBoons.grant(p, 5);
        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40));
        if (p.hasEffect(MobEffects.LEVITATION)) bad.add("Shulker Ward let Levitation on");
        p.removeEffect(ModEffects.SHULKER_WARD.holder());
        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 40));
        if (!p.hasEffect(MobEffects.LEVITATION)) bad.add("Levitation didn't take without the ward");
        p.removeEffect(MobEffects.LEVITATION);
        // Resonant Edge
        double base = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ShrineBoons.grant(p, 4);
        double edged = p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (Math.abs(edged - base * 1.1) > 1e-6) bad.add("Resonant Edge: attack damage " + base + " to " + edged);
        // Rift Step
        p.removeAllEffects();
        if (VoidAnchor.rescue(p)) bad.add("a fall was caught with no anchor and no Rift Step");
        ShrineBoons.grant(p, 3);
        boolean caught = VoidAnchor.rescue(p);
        if (!caught || p.hasEffect(ModEffects.RIFT_STEP.holder())) bad.add("Rift Step: caught " + caught + ", still on " + p.hasEffect(ModEffects.RIFT_STEP.holder()));
        if (p.getInventory().countItem(ModItems.CRACKED_VOID_ANCHOR.get()) != 0) bad.add("Rift Step cracked an anchor");
        // Pearl Sense: a 20-tick cooldown, halved as it starts; the cooldowns are ticked here (a test player isn't), the
        // same count without the boon as the control
        ItemStack pearl = new ItemStack(Items.ENDER_PEARL);
        int[] over = new int[2];
        for (int boon = 0; boon < 2; boon++) {
            p.removeAllEffects();
            if (boon == 1) ShrineBoons.grant(p, 1);
            ShrineBoons.tick(p);
            p.getCooldowns().addCooldown(pearl, 20);
            ShrineBoons.tick(p);
            int t = 0;
            while (p.getCooldowns().isOnCooldown(pearl) && t < 40) {
                p.getCooldowns().tick();
                ShrineBoons.tick(p);
                t++;
            }
            over[boon] = t;
        }
        if (over[0] != 20 || over[1] != 10) bad.add("Pearl Sense: a 20-tick cooldown lasted " + over[0] + " without it and " + over[1] + " with it");
        PromisedEnd.LOGGER.info("[boons_do_their_work] attack damage {} with Resonant Edge (from {}); a pearl's cooldown {} ticks, {} with Pearl Sense",
                edged, base, over[0], over[1]);
        // Shulker Ward taken while already lifted: the Levitation goes at the next tick
        p.removeAllEffects();
        p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 200));
        ShrineBoons.grant(p, 5);
        ShrineBoons.tick(p);
        if (p.hasEffect(MobEffects.LEVITATION)) bad.add("Shulker Ward kept a Levitation already on");
        level.getServer().getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("boons: " + bad);
        helper.succeed();
    }

    /**
     * Gliding Grace halves a worn glider's wear: twenty points of wear, a tick apart, leave ten with it and twenty
     * without. With Unbreaking III it never mends the glider (the wear Unbreaking spares isn't given back again), and a
     * damaged spare in hand is left alone.
     */
    static void glidingGraceHalvesWear(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        List<String> bad = new ArrayList<>();
        int[] after = new int[2];
        for (int boon = 0; boon < 2; boon++) {
            p.removeAllEffects();
            ShrineBoons.tick(p);
            if (boon == 1) ShrineBoons.grant(p, 0);
            ItemStack elytra = new ItemStack(Items.ELYTRA);
            p.setItemSlot(EquipmentSlot.CHEST, elytra);
            ShrineBoons.tick(p);
            for (int t = 0; t < 20; t++) {
                elytra.hurtAndBreak(1, p, EquipmentSlot.CHEST);
                ShrineBoons.tick(p);
            }
            after[boon] = elytra.getDamageValue();
        }
        if (after[0] != 20 || after[1] != 10) bad.add("twenty wear: " + after[0] + " without, " + after[1] + " with Gliding Grace");
        // Unbreaking III, with a damaged spare in hand
        ItemStack sturdy = new ItemStack(Items.ELYTRA);
        sturdy.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING), 3);
        ItemStack spare = new ItemStack(Items.ELYTRA);
        spare.setDamageValue(100);
        p.setItemSlot(EquipmentSlot.CHEST, sturdy);
        p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, spare);
        ShrineBoons.tick(p);
        int last = 0;
        for (int t = 0; t < 400; t++) {
            sturdy.hurtAndBreak(1, p, EquipmentSlot.CHEST);
            ShrineBoons.tick(p);
            if (sturdy.getDamageValue() < last) bad.add("Unbreaking III: mended from " + last + " to " + sturdy.getDamageValue());
            last = sturdy.getDamageValue();
        }
        if (spare.getDamageValue() != 100) bad.add("the spare in hand went from 100 to " + spare.getDamageValue());
        PromisedEnd.LOGGER.info("[gliding_grace_halves_wear] twenty wear left {} without and {} with; Unbreaking III over 400 wear left {}",
                after[0], after[1], last);
        level.getServer().getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("gliding grace: " + bad);
        helper.succeed();
    }

    /** Void Sight sees an Updraft Vent in reach of the player, and none when the player is past its reach. */
    static void voidSightShowsVents(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos vent = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(vent, com.panda.promisedend.registry.ModBlocks.UPDRAFT_VENT.get().defaultBlockState());
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        p.snapTo(vent.getX() + 6.5, vent.getY(), vent.getZ() + 0.5, 0, 0);
        List<String> bad = new ArrayList<>();
        var boxes = new ArrayList<net.minecraft.world.level.levelgen.structure.BoundingBox>();
        var near = ShrineBoons.seen(p, boxes).stream().map(v -> v.getBlockPos()).toList();
        if (!near.contains(vent)) bad.add("a vent 6 away unseen: " + near);
        p.snapTo(vent.getX() + ShrineBoons.VENT_REACH + 4.5, vent.getY(), vent.getZ() + 0.5, 0, 0);
        var far = ShrineBoons.seen(p, boxes).stream().map(v -> v.getBlockPos()).toList();
        if (far.contains(vent)) bad.add("a vent " + (ShrineBoons.VENT_REACH + 4) + " away seen");
        PromisedEnd.LOGGER.info("[void_sight_shows_vents] from 6 blocks {}, from {} blocks {}", near, ShrineBoons.VENT_REACH + 4, far);
        level.getServer().getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("void sight: " + bad);
        helper.succeed();
    }

    /**
     * An altar keeps each player's favour, offer and wait through a save and load. The choice packet's way in refuses
     * a player too far off (and the offer stands), a position that isn't an altar, and an offer already spent.
     */
    static void altarKeepsAndRefuses(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(at, ModBlocks.SHRINE_ALTAR.get().defaultBlockState());
        if (!(level.getBlockEntity(at) instanceof ShrineAltarBlockEntity altar)) {
            helper.fail("no altar block entity");
            return;
        }
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        ServerPlayer q = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        p.snapTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 2.5, 180, 0);
        q.snapTo(at.getX() + 2.5, at.getY() + 1, at.getZ() + 0.5, 90, 0);
        List<String> bad = new ArrayList<>();
        altar.offer(p, new ItemStack(ModItems.DRAGON_SCALE.get()));
        altar.offer(q, new ItemStack(ModItems.CHORUS_RESIN.get()));
        // a third has a boon and waits: taken through the packet's way in
        ServerPlayer r = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        r.snapTo(at.getX() - 1.5, at.getY() + 1, at.getZ() + 0.5, 270, 0);
        altar.offer(r, new ItemStack(ModItems.DRAGON_SCALE.get()));
        int rPick = altar.offered(r.getUUID()).getFirst();
        ShrineAltarBlockEntity.chosen(r, new com.panda.promisedend.network.ShrinePayloads.Choose(at, rPick));
        if (!r.hasEffect(ModEffects.boon(rPick))) bad.add("a choice through the packet's way in wasn't granted");
        // the same choice again: the offer is spent
        r.removeAllEffects();
        ShrineAltarBlockEntity.chosen(r, new com.panda.promisedend.network.ShrinePayloads.Choose(at, rPick));
        if (r.hasEffect(ModEffects.boon(rPick))) bad.add("a spent offer was granted again");
        // a position that isn't an altar
        int pPick = altar.offered(p.getUUID()).getFirst();
        ShrineAltarBlockEntity.chosen(p, new com.panda.promisedend.network.ShrinePayloads.Choose(at.above(), pPick));
        if (p.hasEffect(ModEffects.boon(pPick))) bad.add("a choice at a non-altar was granted");
        // too far: refused, and the offer stands
        p.snapTo(at.getX() + 10.5, at.getY() + 1, at.getZ() + 0.5, 0, 0);
        ShrineAltarBlockEntity.chosen(p, new com.panda.promisedend.network.ShrinePayloads.Choose(at, pPick));
        if (p.hasEffect(ModEffects.boon(pPick)) || altar.offered(p.getUUID()).size() != 3) bad.add("a choice from 10 away: granted "
                + p.hasEffect(ModEffects.boon(pPick)) + ", offer " + altar.offered(p.getUUID()));
        // save and load into a fresh altar
        var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess());
        altar.saveWithoutMetadata(out);
        var copy = new ShrineAltarBlockEntity(at, altar.getBlockState());
        copy.loadCustomOnly(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING, level.registryAccess(), out.buildResult()));
        copy.setLevel(level);
        if (!copy.offered(p.getUUID()).equals(altar.offered(p.getUUID()))) bad.add("offer " + copy.offered(p.getUUID()) + ", saved " + altar.offered(p.getUUID()));
        if (copy.favour(q.getUUID()) != 1) bad.add("favour " + copy.favour(q.getUUID()) + ", saved 1");
        if (copy.waitLeft(r) != altar.waitLeft(r) || copy.waitLeft(r) <= 0) bad.add("wait " + copy.waitLeft(r) + ", saved " + altar.waitLeft(r));
        PromisedEnd.LOGGER.info("[altar_keeps_and_refuses] loaded: offer {}, favour {}, wait {}", copy.offered(p.getUUID()), copy.favour(q.getUUID()), copy.waitLeft(r));
        for (ServerPlayer s : List.of(p, q, r)) level.getServer().getPlayerList().remove(s);
        if (!bad.isEmpty()) helper.fail("altar: " + bad);
        helper.succeed();
    }
}

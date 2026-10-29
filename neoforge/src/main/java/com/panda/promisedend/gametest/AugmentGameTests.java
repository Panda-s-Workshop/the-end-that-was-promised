package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.item.Augments;
import com.panda.promisedend.logic.ShrineBoons;
import com.panda.promisedend.registry.ModComponents;
import com.panda.promisedend.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;

/** Elytra augments. */
final class AugmentGameTests {
    private AugmentGameTests() {}

    private static ItemStack smith(GameTestHelper helper, ItemStack template, ItemStack base, ItemStack addition) {
        var level = helper.getLevel();
        var input = new SmithingRecipeInput(template, base, addition);
        return level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level)
                .map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    /**
     * A smithing table puts Drift Weave and the Leviathan Fin on an elytra, each once, keeping its enchantments, damage
     * and name; the result is still an elytra; it takes neither a second time, nor any glider outside
     * #promisedend:augmentable_elytra, nor without the Dragon Scale; the augments survive a save (the component's
     * codec) and show in the tooltip.
     */
    static void appliesInSmithing(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var weave = new ItemStack(ModItems.DRIFT_WEAVE.get());
        var fin = new ItemStack(ModItems.LEVIATHAN_FIN.get());
        var scale = new ItemStack(ModItems.DRAGON_SCALE.get());
        var elytra = new ItemStack(Items.ELYTRA);
        var unbreaking = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.UNBREAKING);
        elytra.enchant(unbreaking, 3);
        elytra.setDamageValue(100);
        var once = smith(helper, weave, elytra, scale);
        if (!once.is(Items.ELYTRA)) bad.add("Drift Weave on an elytra makes " + once);
        else {
            if (!Augments.of(once).has(Augments.DRIFT_WEAVE)) bad.add("the weave isn't on it: " + Augments.of(once));
            if (once.getEnchantments().getLevel(unbreaking) != 3 || once.getDamageValue() != 100) bad.add("it lost its enchantment or wear: " + once);
        }
        if (!smith(helper, weave, once, scale).isEmpty()) bad.add("a second Drift Weave went on");
        var twice = smith(helper, fin, once, scale);
        if (!Augments.of(twice).has(Augments.DRIFT_WEAVE) || !Augments.of(twice).has(Augments.LEVIATHAN_FIN) || Augments.of(twice).list().size() != 2)
            bad.add("weave then fin gives " + Augments.of(twice));
        if (!smith(helper, fin, new ItemStack(Items.DIAMOND_CHESTPLATE), scale).isEmpty()) bad.add("the fin went on a chestplate");
        if (!smith(helper, fin, new ItemStack(Items.ELYTRA), new ItemStack(Items.DIAMOND)).isEmpty()) bad.add("the fin went on without a Dragon Scale");
        // three at most, whatever they are
        var full = new ItemStack(Items.ELYTRA);
        full.set(ModComponents.AUGMENTS.get(), new Augments(List.of(PromisedEnd.id("a"), PromisedEnd.id("b"), PromisedEnd.id("c"))));
        if (!Augments.apply(full, Augments.LEVIATHAN_FIN).isEmpty()) bad.add("a fourth augment went on");
        // through the codec, as a save does
        var saved = ItemStack.CODEC.encodeStart(level.registryAccess().createSerializationContext(com.mojang.serialization.JavaOps.INSTANCE), twice).getOrThrow();
        var loaded = ItemStack.CODEC.parse(level.registryAccess().createSerializationContext(com.mojang.serialization.JavaOps.INSTANCE), saved).getOrThrow();
        if (!Augments.of(loaded).equals(Augments.of(twice))) bad.add("a save kept " + Augments.of(loaded) + " of " + Augments.of(twice));
        var lines = twice.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(level), null, TooltipFlag.NORMAL).stream().map(Component::getString).toList();
        if (lines.stream().noneMatch(l -> l.contains("Drift Weave")) || lines.stream().noneMatch(l -> l.contains("Leviathan Fin")))
            bad.add("the tooltip doesn't name its augments: " + lines);
        PromisedEnd.LOGGER.info("[augment_applies_in_smithing] tooltip {}", lines);
        if (!bad.isEmpty()) helper.fail("augments: " + bad);
        helper.succeed();
    }

    /**
     * Drift Weave: twenty points of glide wear leave 12 (40 % given back), 6 with Gliding Grace too (70 %), and an
     * elytra without it all 20. The Leviathan Fin: a gliding wearer's updraft lift and current push go up, a walking one's
     * not.
     */
    static void driftWeaveReducesWear(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        List<String> bad = new ArrayList<>();
        int[] after = new int[3];
        for (int run = 0; run < 3; run++) {
            p.removeAllEffects();
            ShrineBoons.tick(p);
            if (run == 2) ShrineBoons.grant(p, 0);
            ItemStack elytra = new ItemStack(Items.ELYTRA);
            if (run > 0) elytra = Augments.apply(elytra, Augments.DRIFT_WEAVE);
            p.setItemSlot(EquipmentSlot.CHEST, elytra);
            ShrineBoons.tick(p);
            for (int t = 0; t < 20; t++) {
                elytra.hurtAndBreak(1, p, EquipmentSlot.CHEST);
                ShrineBoons.tick(p);
            }
            after[run] = elytra.getDamageValue();
        }
        PromisedEnd.LOGGER.info("[augment_drift_weave_reduces_wear] twenty wear left {} bare, {} woven, {} woven with Gliding Grace", after[0], after[1], after[2]);
        if (after[0] != 20 || after[1] != 12 || after[2] != 6) bad.add("twenty wear left " + after[0] + ", " + after[1] + ", " + after[2] + ", not 20, 12, 6");
        // the Fin, worn and gliding
        p.removeAllEffects();
        p.setItemSlot(EquipmentSlot.CHEST, Augments.apply(new ItemStack(Items.ELYTRA), Augments.LEVIATHAN_FIN));
        if (Augments.lift(p) != 0) bad.add("the Fin lifts a player who isn't gliding");
        p.startFallFlying();
        if (!p.isFallFlying()) bad.add("the test player wouldn't glide");
        else if (Augments.lift(p) != Augments.FIN_LIFT) bad.add("the Fin adds " + Augments.lift(p) + " to a glider's lift, not " + Augments.FIN_LIFT);
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        if (Augments.lift(p) != 0) bad.add("a bare elytra lifts like the Fin");
        level.getServer().getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("augment effects: " + bad);
        helper.succeed();
    }
}

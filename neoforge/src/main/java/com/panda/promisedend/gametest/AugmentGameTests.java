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
     * #promisedend:augmentable_elytra, nor without the Echo Amber; the augments survive a save (the component's
     * codec) and show in the tooltip. Worn, the woven one looks woven and the one with both shows both (its equippable's
     * asset); a plain elytra keeps vanilla's.
     */
    static void appliesInSmithing(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var weave = new ItemStack(ModItems.DRIFT_WEAVE.get());
        var fin = new ItemStack(ModItems.LEVIATHAN_FIN.get());
        var scale = new ItemStack(ModItems.ECHO_AMBER.get());
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
        java.util.function.Function<ItemStack, String> looks = s -> s.get(net.minecraft.core.component.DataComponents.EQUIPPABLE).assetId()
                .map(k -> k.identifier().toString()).orElse("none");
        if (!looks.apply(once).equals("promisedend:elytra_weave")) bad.add("the woven elytra looks " + looks.apply(once));
        if (!looks.apply(new ItemStack(Items.ELYTRA)).equals("minecraft:elytra")) bad.add("a plain elytra looks " + looks.apply(new ItemStack(Items.ELYTRA)));
        var twice = smith(helper, fin, once, scale);
        if (!Augments.of(twice).has(Augments.DRIFT_WEAVE) || !Augments.of(twice).has(Augments.LEVIATHAN_FIN) || Augments.of(twice).list().size() != 2)
            bad.add("weave then fin gives " + Augments.of(twice));
        if (!looks.apply(twice).equals("promisedend:elytra_fin_weave")) bad.add("the elytra with both looks " + looks.apply(twice));
        if (!smith(helper, fin, new ItemStack(Items.DIAMOND_CHESTPLATE), scale).isEmpty()) bad.add("the fin went on a chestplate");
        if (!smith(helper, fin, new ItemStack(Items.ELYTRA), new ItemStack(Items.DIAMOND)).isEmpty()) bad.add("the fin went on without Echo Amber");
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
     * What an updraft's lift is multiplied by (the set's share plus the Fin's): a plain glider 1, the full Voidsteel set
     * 1.5, three pieces and a Fin elytra (which counts as the chestplate) 2 while gliding and 1.5 standing.
     */
    static void finAndSetStackLift(GameTestHelper helper) {
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        List<String> bad = new ArrayList<>();
        java.util.function.Function<String, Double> lift = what -> {
            double m = com.panda.promisedend.item.VoidTouched.lift(p) + Augments.lift(p);
            PromisedEnd.LOGGER.info("[augment_fin_and_set_stack_lift] {}: {}", what, m);
            return m;
        };
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.ELYTRA));
        p.startFallFlying();
        if (lift.apply("plain elytra, gliding") != 1.0) bad.add("a plain glider's lift isn't 1");
        p.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ModItems.VOIDSTEEL_HELMET.get()));
        p.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ModItems.VOIDSTEEL_CHESTPLATE.get()));
        p.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ModItems.VOIDSTEEL_LEGGINGS.get()));
        p.setItemSlot(EquipmentSlot.FEET, new ItemStack(ModItems.VOIDSTEEL_BOOTS.get()));
        p.stopFallFlying();
        if (lift.apply("full set") != 1.5) bad.add("the full set's lift isn't 1.5");
        p.setItemSlot(EquipmentSlot.CHEST, Augments.apply(new ItemStack(Items.ELYTRA), Augments.LEVIATHAN_FIN));
        if (lift.apply("three pieces and a Fin elytra, standing") != 1.5) bad.add("the set with a Fin elytra, standing, isn't 1.5");
        p.startFallFlying();
        if (!p.isFallFlying()) bad.add("the player didn't start gliding");
        if (lift.apply("three pieces and a Fin elytra, gliding") != 2.0) bad.add("the set and the Fin, gliding, don't stack to 2");
        p.stopFallFlying();
        // a mount the wearer drives rises as they would; one nobody rides doesn't
        var horse = net.minecraft.world.entity.EntityTypes.HORSE.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        horse.snapTo(p.getX(), p.getY(), p.getZ(), 0, 0);
        horse.setTamed(true);
        horse.setItemSlot(EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
        helper.getLevel().addFreshEntity(horse);
        double alone = com.panda.promisedend.block.UpdraftVentBlockEntity.liftFor(horse);
        p.startRiding(horse, true, false);
        double ridden = com.panda.promisedend.block.UpdraftVentBlockEntity.liftFor(horse);
        PromisedEnd.LOGGER.info("[augment_fin_and_set_stack_lift] horse alone {}, ridden by the wearer {} (driver {})", alone, ridden, horse.getControllingPassenger());
        if (alone != 1.0) bad.add("a horse alone lifts " + alone);
        if (ridden != 1.5) bad.add("a horse the wearer rides lifts " + ridden + ", not 1.5");
        p.stopRiding();
        horse.discard();
        helper.getLevel().getServer().getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("fin and set lift: " + bad);
        else helper.succeed();
    }

    /**
     * At a real smithing table, as a player uses it: the template slot takes Drift Weave, Echo Amber goes in the
     * addition's, the woven elytra shows in the result, and taking it uses the three up; the Leviathan Fin the same on
     * the woven one, and only the Fin earns Act IV's "Second Wings" (`void/augment_elytra`: Drift Weave is Act II's
     * materials). No dragon is needed.
     */
    static void smithingTableAwardsTheGoal(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer p = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        List<String> bad = new ArrayList<>();
        var goal = level.getServer().getAdvancements().get(PromisedEnd.id("void/augment_elytra"));
        if (goal == null) bad.add("no void/augment_elytra advancement");
        var table = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        level.setBlock(table, net.minecraft.world.level.block.Blocks.SMITHING_TABLE.defaultBlockState(), 3);
        var menu = new net.minecraft.world.inventory.SmithingMenu(1, p.getInventory(), net.minecraft.world.inventory.ContainerLevelAccess.create(level, table));
        ItemStack elytra = new ItemStack(Items.ELYTRA);
        for (var augment : List.of(ModItems.DRIFT_WEAVE, ModItems.LEVIATHAN_FIN)) {
            var template = new ItemStack(augment.get());
            if (!menu.getSlot(net.minecraft.world.inventory.SmithingMenu.TEMPLATE_SLOT).mayPlace(template)) bad.add("the template slot refuses " + augment.getId());
            if (!menu.getSlot(net.minecraft.world.inventory.SmithingMenu.ADDITIONAL_SLOT).mayPlace(new ItemStack(ModItems.ECHO_AMBER.get())))
                bad.add("the addition slot refuses Echo Amber");
            menu.getSlot(net.minecraft.world.inventory.SmithingMenu.TEMPLATE_SLOT).set(template);
            menu.getSlot(net.minecraft.world.inventory.SmithingMenu.BASE_SLOT).set(elytra);
            menu.getSlot(net.minecraft.world.inventory.SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(ModItems.ECHO_AMBER.get(), 2));
            menu.createResult();
            var result = menu.getSlot(net.minecraft.world.inventory.SmithingMenu.RESULT_SLOT).getItem();
            if (!result.is(Items.ELYTRA) || !Augments.of(result).has(PromisedEnd.id(augment.getId().getPath()))) {
                bad.add("the table shows " + result + " " + Augments.of(result) + " for " + augment.getId());
                break;
            }
            elytra = result.copy();
            menu.getSlot(net.minecraft.world.inventory.SmithingMenu.RESULT_SLOT).onTake(p, result);
            if (!menu.getSlot(net.minecraft.world.inventory.SmithingMenu.TEMPLATE_SLOT).getItem().isEmpty()) bad.add("taking it left the " + augment.getId());
            if (menu.getSlot(net.minecraft.world.inventory.SmithingMenu.ADDITIONAL_SLOT).getItem().getCount() != 1) bad.add("taking it didn't use one amber");
            boolean earned = goal != null && p.getAdvancements().getOrStartProgress(goal).isDone();
            if (earned != (augment == ModItems.LEVIATHAN_FIN)) bad.add("taking the " + augment.getId() + (earned ? " earned" : " didn't earn") + " Second Wings");
            menu.getSlot(net.minecraft.world.inventory.SmithingMenu.ADDITIONAL_SLOT).set(ItemStack.EMPTY);
        }
        if (Augments.of(elytra).list().size() != 2) bad.add("the table's elytra ends with " + Augments.of(elytra));
        level.getServer().getPlayerList().remove(p);
        if (!bad.isEmpty()) helper.fail("smithing table: " + bad);
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

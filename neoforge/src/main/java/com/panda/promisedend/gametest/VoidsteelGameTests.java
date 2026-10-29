package com.panda.promisedend.gametest;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.item.VoidTouched;
import com.panda.promisedend.logic.GearTiers;
import com.panda.promisedend.platform.registry.DeferredItem;
import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.registry.ModTags;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

/** The Voidsteel tier and its Void-touched set. */
final class VoidsteelGameTests {
    private VoidsteelGameTests() {}

    private static final List<DeferredItem<?>> TOOLS = List.of(ModItems.VOIDSTEEL_SWORD, ModItems.VOIDSTEEL_SHOVEL,
            ModItems.VOIDSTEEL_PICKAXE, ModItems.VOIDSTEEL_AXE, ModItems.VOIDSTEEL_HOE);
    private static final List<DeferredItem<?>> ARMOUR = List.of(ModItems.VOIDSTEEL_HELMET, ModItems.VOIDSTEEL_CHESTPLATE,
            ModItems.VOIDSTEEL_LEGGINGS, ModItems.VOIDSTEEL_BOOTS);
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static Item item(DeferredItem<?> h) {
        return (Item) h.get();
    }

    /**
     * The Blocks of Starmetal and Voidsteel: nine of each metal make one and one gives nine back; each drops itself to a
     * pickaxe of its tier and nothing to one below (Starmetal iron's, as raw gold's block; Voidsteel diamond's, as
     * netherite's); Voidsteel's is a beacon's base and its item won't burn; both are storage blocks to other mods.
     */
    static void storageBlocks(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        record Metal(net.minecraft.world.level.block.Block block, Item unit, Item enough, Item below, boolean beacon) {}
        for (var m : List.of(new Metal(com.panda.promisedend.registry.ModBlocks.STARMETAL_BLOCK.get(), ModItems.STARMETAL.get(), Items.IRON_PICKAXE, Items.STONE_PICKAXE, false),
                new Metal(com.panda.promisedend.registry.ModBlocks.VOIDSTEEL_BLOCK.get(), ModItems.VOIDSTEEL_INGOT.get(), Items.DIAMOND_PICKAXE, Items.IRON_PICKAXE, true))) {
            String name = m.block().builtInRegistryHolder().getRegisteredName();
            var units = new ArrayList<ItemStack>();
            for (int i = 0; i < 9; i++) units.add(new ItemStack(m.unit()));
            var nine = CraftingInput.of(3, 3, units);
            var made = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, nine, level).map(r -> r.value().assemble(nine)).orElse(ItemStack.EMPTY);
            if (!made.is(m.block().asItem()) || made.getCount() != 1) bad.add("nine " + m.unit() + " make " + made);
            var one = CraftingInput.of(1, 1, List.of(new ItemStack(m.block())));
            var back = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, one, level).map(r -> r.value().assemble(one)).orElse(ItemStack.EMPTY);
            if (!back.is(m.unit()) || back.getCount() != 9) bad.add(name + " gives back " + back);
            var state = m.block().defaultBlockState();
            if (!new ItemStack(m.enough()).isCorrectToolForDrops(state)) bad.add(m.enough() + " can't mine " + name);
            if (new ItemStack(m.below()).isCorrectToolForDrops(state)) bad.add(m.below() + " mines " + name);
            var drops = level.getServer().reloadableRegistries().getLootTable(m.block().getLootTable().orElseThrow()).getRandomItems(
                    new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, new ItemStack(m.enough()))
                            .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, state)
                            .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK));
            if (drops.size() != 1 || !drops.get(0).is(m.block().asItem())) bad.add(name + " drops " + drops);
            if (state.is(net.minecraft.tags.BlockTags.BEACON_BASE_BLOCKS) != m.beacon()) bad.add(name + (m.beacon() ? " isn't" : " is") + " a beacon's base");
            if (!state.is(net.minecraft.tags.TagKey.create(Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath("c", "storage_blocks")))) bad.add(name + " isn't in #c:storage_blocks");
        }
        if (!new ItemStack(com.panda.promisedend.registry.ModBlocks.VOIDSTEEL_BLOCK.get()).has(DataComponents.DAMAGE_RESISTANT)) bad.add("the Block of Voidsteel burns");
        if (!bad.isEmpty()) helper.fail("storage blocks: " + bad);
        helper.succeed();
    }

    /**
     * Voidsteel gear is built from GearTiers' numbers: each tool's durability, its sword's damage, its pickaxe's speed;
     * each armour piece's defence, toughness and knockback resistance, enchantability, the worn look, the Void-touched
     * set tag; every tool, piece and the ingot fire-resistant, as netherite; the
     * ingot repairs every piece and comes of 4 scrap, 2 Starmetal and 2 Echo Amber; the template smiths each Resonite
     * piece into its Voidsteel one with its enchantments, and not a diamond one; two templates from one.
     */
    static void voidsteelGear(GameTestHelper helper) {
        var level = helper.getLevel();
        List<String> bad = new ArrayList<>();
        var tier = GearTiers.VOIDSTEEL;
        var ingot = new ItemStack(ModItems.VOIDSTEEL_INGOT.get());
        for (var h : TOOLS) {
            var stack = new ItemStack(item(h));
            if (stack.getMaxDamage() != tier.durability()) bad.add(h.getId() + " lasts " + stack.getMaxDamage() + ", not " + tier.durability());
            if (!stack.isValidRepairItem(ingot)) bad.add("the ingot doesn't repair " + h.getId());
            var ench = stack.get(DataComponents.ENCHANTABLE);
            if (ench == null || ench.value() != tier.enchantability()) bad.add(h.getId() + " has enchantability " + ench);
            if (!stack.has(DataComponents.DAMAGE_RESISTANT)) bad.add(h.getId() + " burns");
        }
        if (!ingot.has(DataComponents.DAMAGE_RESISTANT)) bad.add("the ingot burns");
        var pick = new ItemStack(ModItems.VOIDSTEEL_PICKAXE.get());
        if (Math.abs(pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) - tier.speed()) > 1e-4) bad.add("the pickaxe mines stone at " + pick.getDestroySpeed(Blocks.STONE.defaultBlockState()));
        if (!pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState())) bad.add("the pickaxe doesn't mine obsidian");
        double[] damage = {1.0};
        new ItemStack(ModItems.VOIDSTEEL_SWORD.get()).forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) damage[0] += modifier.amount();
        });
        if (Math.abs(damage[0] - tier.swordDamage()) > 1e-4) bad.add("the sword deals " + damage[0] + ", not " + tier.swordDamage());
        var armour = GearTiers.VOIDSTEEL_ARMOUR;
        int[] defence = {armour.helmet(), armour.chestplate(), armour.leggings(), armour.boots()};
        String[] types = {"HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS"};
        for (int i = 0; i < 4; i++) {
            var h = ARMOUR.get(i);
            var stack = new ItemStack(item(h));
            double[] got = {0, 0, 0};
            stack.forEachModifier(SLOTS[i], (attribute, modifier) -> {
                if (attribute.is(Attributes.ARMOR)) got[0] += modifier.amount();
                if (attribute.is(Attributes.ARMOR_TOUGHNESS)) got[1] += modifier.amount();
                if (attribute.is(Attributes.KNOCKBACK_RESISTANCE)) got[2] += modifier.amount();
            });
            if (got[0] != defence[i] || Math.abs(got[1] - armour.toughness()) > 1e-4 || Math.abs(got[2] - GearTiers.VOIDSTEEL_KNOCKBACK_RESISTANCE) > 1e-4)
                bad.add(h.getId() + " gives " + got[0] + " armour, " + got[1] + " toughness, " + got[2] + " knockback resistance");
            var equippable = stack.get(DataComponents.EQUIPPABLE);
            if (equippable == null || equippable.slot() != SLOTS[i] || equippable.assetId().filter(ModItems.VOIDSTEEL_ASSET::equals).isEmpty())
                bad.add(h.getId() + " is worn as " + equippable);
            if (!stack.is(ModTags.VOID_TOUCHED_SET)) bad.add(h.getId() + " isn't in #promisedend:void_touched_set");
            var enchantable = stack.get(DataComponents.ENCHANTABLE);
            if (enchantable == null || enchantable.value() != armour.enchantability()) bad.add(h.getId() + " has enchantability " + enchantable);
            if (!stack.has(DataComponents.DAMAGE_RESISTANT)) bad.add(h.getId() + " burns");
            if (!stack.isValidRepairItem(ingot)) bad.add("the ingot doesn't repair " + h.getId());
            int durability = net.minecraft.world.item.equipment.ArmorType.valueOf(types[i]).getDurability(GearTiers.VOIDSTEEL_ARMOUR_DURABILITY);
            if (stack.getMaxDamage() != durability) bad.add(h.getId() + " lasts " + stack.getMaxDamage() + ", not " + durability);
        }
        // the ingot, shapeless, in any order; one Starmetal short makes nothing
        var recipes = level.getServer().getRecipeManager();
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 4; i++) grid.add(new ItemStack(ModItems.VOIDSTEEL_SCRAP.get()));
        for (int i = 0; i < 2; i++) grid.add(new ItemStack(ModItems.STARMETAL.get()));
        for (int i = 0; i < 2; i++) grid.add(new ItemStack(ModItems.ECHO_AMBER.get()));
        java.util.Collections.shuffle(grid, new java.util.Random(5));
        grid.add(ItemStack.EMPTY);
        var input = CraftingInput.of(3, 3, grid);
        var made = recipes.getRecipeFor(RecipeType.CRAFTING, input, level).map(r -> r.value().assemble(input)).orElse(ItemStack.EMPTY);
        if (!made.is(ModItems.VOIDSTEEL_INGOT.get()) || made.getCount() != 1) bad.add("4 scrap, 2 Starmetal and 2 amber make " + made);
        grid.set(grid.indexOf(grid.stream().filter(st -> st.is(ModItems.STARMETAL.get())).findFirst().orElseThrow()), ItemStack.EMPTY);
        var short1 = CraftingInput.of(3, 3, grid);
        if (recipes.getRecipeFor(RecipeType.CRAFTING, short1, level).isPresent()) bad.add("one Starmetal short still makes an ingot");
        // smithing: every Resonite piece to its Voidsteel one, enchantments kept
        var protection = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.PROTECTION);
        var template = new ItemStack(ModItems.VOIDSTEEL_UPGRADE_SMITHING_TEMPLATE.get());
        List<DeferredItem<?>> resonite = List.of(ModItems.RESONITE_SWORD, ModItems.RESONITE_SHOVEL, ModItems.RESONITE_PICKAXE,
                ModItems.RESONITE_AXE, ModItems.RESONITE_HOE, ModItems.RESONITE_HELMET, ModItems.RESONITE_CHESTPLATE,
                ModItems.RESONITE_LEGGINGS, ModItems.RESONITE_BOOTS);
        List<DeferredItem<?>> voidsteel = new ArrayList<>(TOOLS);
        voidsteel.addAll(ARMOUR);
        for (int i = 0; i < resonite.size(); i++) {
            var base = new ItemStack(item(resonite.get(i)));
            base.enchant(protection, 3);
            var smith = new SmithingRecipeInput(template, base, ingot);
            var out = recipes.getRecipeFor(RecipeType.SMITHING, smith, level).map(r -> r.value().assemble(smith)).orElse(ItemStack.EMPTY);
            if (!out.is(item(voidsteel.get(i)))) bad.add(resonite.get(i).getId() + " smiths into " + out + ", not " + voidsteel.get(i).getId());
            else if (out.getEnchantments().getLevel(protection) != 3) bad.add(voidsteel.get(i).getId() + " lost its enchantment: " + out.getEnchantments());
        }
        var diamond = new SmithingRecipeInput(template, new ItemStack(Items.DIAMOND_CHESTPLATE), ingot);
        if (recipes.getRecipeFor(RecipeType.SMITHING, diamond, level).isPresent()) bad.add("the template takes a diamond chestplate straight to Voidsteel");
        // the copy: the template, 7 diamonds and a Voidstone, as vanilla's netherite one
        List<ItemStack> copy = new ArrayList<>();
        for (char c : "DTDDVDDDD".toCharArray()) copy.add(c == 'D' ? new ItemStack(Items.DIAMOND) : c == 'T' ? template.copy() : new ItemStack(com.panda.promisedend.registry.ModBlocks.VOIDSTONE_SET.base().get()));
        var copyIn = CraftingInput.of(3, 3, copy);
        var copies = recipes.getRecipeFor(RecipeType.CRAFTING, copyIn, level).map(r -> r.value().assemble(copyIn)).orElse(ItemStack.EMPTY);
        if (!copies.is(ModItems.VOIDSTEEL_UPGRADE_SMITHING_TEMPLATE.get()) || copies.getCount() != 2) bad.add("the template's copy makes " + copies);
        if (!bad.isEmpty()) helper.fail("Voidsteel gear: " + bad);
        helper.succeed();
    }

    private static void wear(ServerPlayer p, int pieces) {
        for (int i = 0; i < 4; i++) p.setItemSlot(SLOTS[i], i < pieces ? new ItemStack(item(ARMOUR.get(i))) : ItemStack.EMPTY);
    }

    /**
     * Void-touched: below the world, a survival player in all four pieces takes no void damage for its first 200 ticks
     * and takes it after; back above the floor, the next fall is held off again (none in its first 60 ticks); in three
     * pieces it isn't held at all.
     * An updraft lifts the set's wearer half as much again (its lift runs on the client; the multiplier is checked
     * here, the lift itself by JUnit `UpdraftTest`).
     */
    static void setBonusVoidTouched(GameTestHelper helper) {
        var level = helper.getLevel();
        ServerPlayer full = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        ServerPlayer three = EndGameTests.realPlayer(helper, GameType.SURVIVAL);
        wear(full, 4);
        wear(three, 3);
        List<String> bad = new ArrayList<>();
        if (VoidTouched.lift(full) != VoidTouched.LIFT || VoidTouched.lift(three) != 1.0) bad.add("the lift is " + VoidTouched.lift(full) + " and " + VoidTouched.lift(three));
        // three pieces and an augmented elytra make the set, so a glider keeps the hold; a plain elytra doesn't
        var glider = helper.makeMockServerPlayerInLevel();
        wear(glider, 4);
        var chest = net.minecraft.world.entity.EquipmentSlot.CHEST;
        glider.setItemSlot(chest, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ELYTRA));
        if (VoidTouched.wearsSet(glider)) bad.add("three pieces and a plain elytra make the set");
        glider.setItemSlot(chest, com.panda.promisedend.item.Augments.apply(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.ELYTRA),
                com.panda.promisedend.item.Augments.LEVIATHAN_FIN));
        if (!VoidTouched.wearsSet(glider)) bad.add("three pieces and an augmented elytra don't make the set");
        glider.discard();
        var floor = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        double below = level.getMinY() - 70;
        int[] firstHurt = {-1, -1, -1};
        int[] start = {-1};
        // a new player can't be hurt until their client has loaded (a minute's grace on a real server, 60 ticks here):
        // the falls start once both have; the first from then, back up 230 ticks in, the second from 240
        for (int t = 1; t <= 380; t++) {
            int tick = t;
            helper.runAtTickTime(t, () -> {
                if (start[0] < 0) {
                    for (ServerPlayer p : List.of(full, three)) p.snapTo(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5, 0, 0);
                    if (full.connection.hasClientLoaded() && three.connection.hasClientLoaded()) start[0] = tick;
                    else if (tick == 120) helper.fail("the test players' clients never counted as loaded");
                    return;
                }
                int rel = tick - start[0];
                if (rel > 300) return;
                boolean falling = rel < 230 || rel >= 240;
                for (ServerPlayer p : List.of(full, three)) {
                    p.setHealth(p.getMaxHealth());
                    if (falling) p.snapTo(floor.getX() + 0.5, below, floor.getZ() + 0.5, 0, 0);
                    else p.snapTo(floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5, 0, 0);
                    p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                }
                // the game's own check, as its tick runs it: void damage under minY - 64
                float before = full.getHealth(), beforeThree = three.getHealth();
                full.checkBelowWorld();
                three.checkBelowWorld();
                if (falling && full.getHealth() < before) {
                    if (rel < 230 && firstHurt[0] < 0) firstHurt[0] = rel;
                    if (rel >= 240 && firstHurt[1] < 0) firstHurt[1] = rel - 240;
                }
                if (falling && rel < 230 && three.getHealth() < beforeThree && firstHurt[2] < 0) firstHurt[2] = rel;
                if (rel == 300) {
                    PromisedEnd.LOGGER.info("[set_bonus_void_touched] from tick {}: the full set first hurt {} ticks into its first fall, {} into its second; three pieces {}",
                            start[0], firstHurt[0], firstHurt[1], firstHurt[2]);
                    // the void's damage first lands on three pieces where it would on anyone (a tick or so after the
                    // first fall starts); on the full set exactly its hold later
                    if (firstHurt[2] < 0 || firstHurt[2] > 2) bad.add("three pieces were hurt first " + firstHurt[2] + " ticks in, not at once");
                    else if (firstHurt[0] - firstHurt[2] != VoidTouched.HOLD_TICKS) bad.add("the full set was hurt " + (firstHurt[0] - firstHurt[2]) + " ticks after three pieces were, not " + VoidTouched.HOLD_TICKS);
                    if (firstHurt[1] != -1) bad.add("the second fall hurt " + firstHurt[1] + " ticks in, inside its hold");
                    level.getServer().getPlayerList().remove(full);
                    level.getServer().getPlayerList().remove(three);
                    if (!bad.isEmpty()) helper.fail("Void-touched: " + bad);
                    helper.succeed();
                }
            });
        }
    }
}

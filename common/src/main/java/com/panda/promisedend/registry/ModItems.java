package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.item.HintItem;
import com.panda.promisedend.logic.GearTiers;
import com.panda.promisedend.platform.registry.DeferredItem;
import com.panda.promisedend.platform.registry.DeferredRegister;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Block;

/** The mod's items. On 26.3 tools are plain items: {@code Item.Properties.pickaxe(...)} adds the tool component. */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(PromisedEnd.MODID);

    // ---- Voidstone tools ----------------------------------------------------------------------------
    public static final ToolMaterial VOIDSTONE = material(GearTiers.VOIDSTONE, BlockTags.INCORRECT_FOR_IRON_TOOL, ModTags.VOIDSTONE_TOOL_MATERIALS);
    public static final DeferredItem<Item> VOIDSTONE_SWORD = ITEMS.registerSimpleItem("voidstone_sword",
            p -> p.sword(VOIDSTONE, GearTiers.SWORD_BASE, GearTiers.SWORD_SPEED));
    public static final DeferredItem<Item> VOIDSTONE_SHOVEL = ITEMS.registerSimpleItem("voidstone_shovel",
            p -> p.shovel(VOIDSTONE, GearTiers.SHOVEL_BASE, GearTiers.SHOVEL_SPEED));
    public static final DeferredItem<Item> VOIDSTONE_PICKAXE = ITEMS.registerSimpleItem("voidstone_pickaxe",
            p -> p.pickaxe(VOIDSTONE, GearTiers.PICKAXE_BASE, GearTiers.PICKAXE_SPEED));
    public static final DeferredItem<Item> VOIDSTONE_AXE = ITEMS.registerSimpleItem("voidstone_axe",
            p -> p.axe(VOIDSTONE, GearTiers.AXE_BASE, GearTiers.AXE_SPEED));
    public static final DeferredItem<Item> VOIDSTONE_HOE = ITEMS.registerSimpleItem("voidstone_hoe",
            p -> p.hoe(VOIDSTONE, GearTiers.HOE_BASE, GearTiers.HOE_SPEED));

    // ---- the Void Anchor: carried, not used; logic.VoidAnchor does the work -------------------------
    public static final DeferredItem<HintItem> VOID_ANCHOR = ITEMS.registerItem("void_anchor",
            p -> new HintItem(p, "void_anchor", 2), p -> p.stacksTo(1));
    public static final DeferredItem<HintItem> CRACKED_VOID_ANCHOR = ITEMS.registerItem("cracked_void_anchor",
            p -> new HintItem(p, "cracked_void_anchor", 2), p -> p.stacksTo(1));

    // ---- constructs -------------------------------------------------------------------------------
    public static final DeferredItem<HintItem> CRYSTAL_FRAGMENT = ITEMS.registerItem("crystal_fragment", p -> new HintItem(p, "crystal_fragment", 1));

    // ---- the dragon's hoard: a Dragon Scale repairs half an elytra in an anvil (mixin.AnvilMenuMixin) ----------------
    public static final DeferredItem<HintItem> DRAGON_SCALE = ITEMS.registerItem("dragon_scale", p -> new HintItem(p, "dragon_scale", 1));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> CRYSTAL_WARDEN_SPAWN_EGG = ITEMS.registerItem("crystal_warden_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.CRYSTAL_WARDEN.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> CHORUS_WEAVER_SPAWN_EGG = ITEMS.registerItem("chorus_weaver_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.CHORUS_WEAVER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> LUMEN_WISP_SPAWN_EGG = ITEMS.registerItem("lumen_wisp_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.LUMEN_WISP.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> RIFT_STALKER_SPAWN_EGG = ITEMS.registerItem("rift_stalker_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.RIFT_STALKER.get()));
    public static final DeferredItem<net.minecraft.world.item.SpawnEggItem> PURPUR_SENTRY_SPAWN_EGG = ITEMS.registerItem("purpur_sentry_spawn_egg",
            net.minecraft.world.item.SpawnEggItem::new, p -> p.spawnEgg(ModEntities.PURPUR_SENTRY.get()));
    /** The Purpur Sentry's: the Ancient Purpur set's and the Amber Key's. */
    public static final DeferredItem<HintItem> ANCIENT_PURPUR_SCRAP = ITEMS.registerItem("ancient_purpur_scrap", p -> new HintItem(p, "ancient_purpur_scrap", 3));
    /** The Hollow Sentinel's: its heart, one to each who fought it, and the template it guards. */
    public static final DeferredItem<HintItem> ANCIENT_CORE = ITEMS.registerItem("ancient_core", p -> new HintItem(p, "ancient_core", 2),
            p -> p.rarity(net.minecraft.world.item.Rarity.UNCOMMON));
    public static final DeferredItem<com.panda.promisedend.item.ResoniteTemplateItem> RESONITE_UPGRADE_SMITHING_TEMPLATE = ITEMS.registerItem(
            "resonite_upgrade_smithing_template", com.panda.promisedend.item.ResoniteTemplateItem::new, p -> p.rarity(net.minecraft.world.item.Rarity.UNCOMMON));

    // ---- the Lumen Mire ---------------------------------------------------------------------------
    public static final DeferredItem<HintItem> LUMEN = ITEMS.registerItem("lumen", p -> new HintItem(p, "lumen", 2));

    // ---- the Resonant Spires --------------------------------------------------------------------
    public static final DeferredItem<HintItem> RESONITE_SHARD = ITEMS.registerItem("resonite_shard", p -> new HintItem(p, "resonite_shard", 3));

    // ---- Resonite gear: the outer End's crystal tier; four armour pieces ring (item.ResonanceSet) -------------------
    public static final DeferredItem<HintItem> CHORUS_RESIN = ITEMS.registerItem("chorus_resin", p -> new HintItem(p, "chorus_resin", 1));
    /** The Skyfall Archipelago's: from a Drift Blossom broken by hand. */
    public static final DeferredItem<HintItem> DRIFT_SILK = ITEMS.registerItem("drift_silk", p -> new HintItem(p, "drift_silk", 2));
    /** The Voidscar Wastes': from the Voidsteel Scrap Ore in the rift walls. */
    public static final DeferredItem<HintItem> VOIDSTEEL_SCRAP = ITEMS.registerItem("voidsteel_scrap", p -> new HintItem(p, "voidsteel_scrap", 2));
    /** The Rift Stalker's: an offering, and a Cracked Void Anchor's charge in place of a pearl. */
    public static final DeferredItem<HintItem> RIFT_DUST = ITEMS.registerItem("rift_dust", p -> new HintItem(p, "rift_dust", 2));
    public static final DeferredItem<HintItem> ECHO_AMBER = ITEMS.registerItem("echo_amber", p -> new HintItem(p, "echo_amber", 2));
    public static final DeferredItem<Item> RESONITE_INGOT = ITEMS.registerSimpleItem("resonite_ingot");

    // ---- the Wilds' food: Void Berries (they plant the vine, as glow berries do) and the Void Mushroom's stew
    public static final DeferredItem<com.panda.promisedend.item.HintBlockItem> VOID_BERRIES = ITEMS.registerItem("void_berries",
            p -> new com.panda.promisedend.item.HintBlockItem(ModBlocks.VOID_BERRY_VINE.get(), p, "void_berries", 2),
            p -> p.food(new FoodProperties.Builder().nutrition(3).saturationModifier(0.3F).build()).useItemDescriptionPrefix());
    /** Mushroom stew's numbers, and the End's own lightness: ten seconds of Slow Falling. */
    public static final DeferredItem<HintItem> VOID_MUSHROOM_STEW = ITEMS.registerItem("void_mushroom_stew",
            p -> new HintItem(p, "void_mushroom_stew", 1), p -> p.stacksTo(1)
                    .food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.6F).build(), Consumables.defaultFood()
                            .onConsume(new ApplyStatusEffectsConsumeEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200))).build())
                    .usingConvertsTo(net.minecraft.world.item.Items.BOWL));
    public static final ToolMaterial RESONITE = material(GearTiers.RESONITE, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, ModTags.RESONITE_TOOL_MATERIALS);
    public static final DeferredItem<Item> RESONITE_SWORD = ITEMS.registerSimpleItem("resonite_sword",
            p -> p.sword(RESONITE, GearTiers.SWORD_BASE, GearTiers.SWORD_SPEED));
    public static final DeferredItem<Item> RESONITE_SHOVEL = ITEMS.registerSimpleItem("resonite_shovel",
            p -> p.shovel(RESONITE, GearTiers.SHOVEL_BASE, GearTiers.SHOVEL_SPEED));
    public static final DeferredItem<Item> RESONITE_PICKAXE = ITEMS.registerSimpleItem("resonite_pickaxe",
            p -> p.pickaxe(RESONITE, GearTiers.PICKAXE_BASE, GearTiers.PICKAXE_SPEED));
    public static final DeferredItem<Item> RESONITE_AXE = ITEMS.registerSimpleItem("resonite_axe",
            p -> p.axe(RESONITE, GearTiers.AXE_BASE, GearTiers.AXE_SPEED));
    public static final DeferredItem<Item> RESONITE_HOE = ITEMS.registerSimpleItem("resonite_hoe",
            p -> p.hoe(RESONITE, GearTiers.HOE_BASE, GearTiers.HOE_SPEED));
    /** The worn look: assets/promisedend/equipment/resonite.json. */
    public static final ResourceKey<EquipmentAsset> RESONITE_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, PromisedEnd.id("resonite"));
    public static final ArmorMaterial RESONITE_ARMOUR = new ArmorMaterial(GearTiers.RESONITE_ARMOUR_DURABILITY,
            defence(GearTiers.RESONITE_ARMOUR), GearTiers.RESONITE_ARMOUR.enchantability(), SoundEvents.ARMOR_EQUIP_DIAMOND,
            GearTiers.RESONITE_ARMOUR.toughness(), 0.0F, ModTags.REPAIRS_RESONITE_ARMOR, RESONITE_ASSET);
    public static final DeferredItem<com.panda.promisedend.item.ResoniteArmourItem> RESONITE_HELMET = armour("resonite_helmet", RESONITE_ARMOUR, ArmorType.HELMET);
    public static final DeferredItem<com.panda.promisedend.item.ResoniteArmourItem> RESONITE_CHESTPLATE = armour("resonite_chestplate", RESONITE_ARMOUR, ArmorType.CHESTPLATE);
    public static final DeferredItem<com.panda.promisedend.item.ResoniteArmourItem> RESONITE_LEGGINGS = armour("resonite_leggings", RESONITE_ARMOUR, ArmorType.LEGGINGS);
    public static final DeferredItem<com.panda.promisedend.item.ResoniteArmourItem> RESONITE_BOOTS = armour("resonite_boots", RESONITE_ARMOUR, ArmorType.BOOTS);

    // ---- the Promise Codex ------------------------------------------------------------------------
    public static final DeferredItem<com.panda.promisedend.item.PromiseCodexItem> PROMISE_CODEX = ITEMS.registerItem("promise_codex",
            com.panda.promisedend.item.PromiseCodexItem::new, p -> p.stacksTo(1));

    private static ToolMaterial material(GearTiers.Tier tier, TagKey<Block> incorrect, TagKey<Item> repair) {
        return new ToolMaterial(incorrect, tier.durability(), tier.speed(), tier.damageBonus(), tier.enchantability(), repair);
    }

    /** A Resonite armour piece: its tooltip tells the set bonus and how many pieces are on. */
    private static DeferredItem<com.panda.promisedend.item.ResoniteArmourItem> armour(String name, ArmorMaterial material, ArmorType type) {
        return ITEMS.registerItem(name, com.panda.promisedend.item.ResoniteArmourItem::new, p -> p.humanoidArmor(material, type));
    }

    /** Defence per slot; the body slot (a horse's or a wolf's) as diamond's 11, never worn by our armour. */
    private static java.util.Map<ArmorType, Integer> defence(GearTiers.Armour armour) {
        var map = new java.util.EnumMap<ArmorType, Integer>(ArmorType.class);
        map.put(ArmorType.HELMET, armour.helmet());
        map.put(ArmorType.CHESTPLATE, armour.chestplate());
        map.put(ArmorType.LEGGINGS, armour.leggings());
        map.put(ArmorType.BOOTS, armour.boots());
        map.put(ArmorType.BODY, 11);
        return map;
    }

    private ModItems() {}
}

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
import net.minecraft.world.item.Item;
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

    // ---- the Lumen Mire ---------------------------------------------------------------------------
    public static final DeferredItem<HintItem> LUMEN = ITEMS.registerItem("lumen", p -> new HintItem(p, "lumen", 1));

    // ---- the Resonant Spires --------------------------------------------------------------------
    public static final DeferredItem<HintItem> RESONITE_SHARD = ITEMS.registerItem("resonite_shard", p -> new HintItem(p, "resonite_shard", 3));

    // ---- Resonite gear: the outer End's crystal tier; four armour pieces ring (item.ResonanceSet) -------------------
    public static final DeferredItem<HintItem> CHORUS_RESIN = ITEMS.registerItem("chorus_resin", p -> new HintItem(p, "chorus_resin", 1));
    public static final DeferredItem<Item> RESONITE_INGOT = ITEMS.registerSimpleItem("resonite_ingot");
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

package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.item.HintItem;
import com.panda.promisedend.logic.GearTiers;
import com.panda.promisedend.platform.registry.DeferredItem;
import com.panda.promisedend.platform.registry.DeferredRegister;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
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

    // ---- the Promise Codex ------------------------------------------------------------------------
    public static final DeferredItem<com.panda.promisedend.item.PromiseCodexItem> PROMISE_CODEX = ITEMS.registerItem("promise_codex",
            com.panda.promisedend.item.PromiseCodexItem::new, p -> p.stacksTo(1));

    private static ToolMaterial material(GearTiers.Tier tier, TagKey<Block> incorrect, TagKey<Item> repair) {
        return new ToolMaterial(incorrect, tier.durability(), tier.speed(), tier.damageBonus(), tier.enchantability(), repair);
    }

    private ModItems() {}
}

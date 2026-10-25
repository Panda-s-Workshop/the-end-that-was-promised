package com.panda.promisedend.logic;

/**
 * The gear tiers' numbers, kept apart from the registry so a plain JUnit test can pin them. Sword damage is
 * the tooltip value: 1 (the player's fist) + 3 (the sword's base, as vanilla) + the material's damage bonus.
 */
public final class GearTiers {
    /** One tool material, as the tooltip shows it. {@code miningTier}: 0 wood/gold, 1 stone/copper, 2 iron, 3 diamond, 4 netherite. */
    public record Tier(String name, int durability, float speed, float swordDamage, int enchantability, int miningTier) {
        /** The material's damage bonus on top of each tool's own base (vanilla's {@code attackDamageBonus}). */
        public float damageBonus() {
            return swordDamage - 1 - SWORD_BASE;
        }
    }

    /** Vanilla's per-tool base damage and attack speed (iron's), which every one of our tool sets uses. */
    public static final float SWORD_BASE = 3.0F, SWORD_SPEED = -2.4F;
    public static final float SHOVEL_BASE = 1.5F, SHOVEL_SPEED = -3.0F;
    public static final float PICKAXE_BASE = 1.0F, PICKAXE_SPEED = -2.8F;
    public static final float AXE_BASE = 6.0F, AXE_SPEED = -3.1F;
    public static final float HOE_BASE = -2.0F, HOE_SPEED = -1.0F;

    // vanilla, read from the 26.3 ToolMaterial with javap, for comparison
    public static final Tier STONE = new Tier("stone", 131, 4.0F, 5.0F, 5, 1);
    public static final Tier COPPER = new Tier("copper", 190, 5.0F, 5.0F, 13, 1);
    public static final Tier IRON = new Tier("iron", 250, 6.0F, 6.0F, 14, 2);
    public static final Tier DIAMOND = new Tier("diamond", 1561, 8.0F, 7.0F, 10, 3);
    public static final Tier NETHERITE = new Tier("netherite", 2031, 9.0F, 8.0F, 15, 4);

    /** Cheap End gear before the dragon: copper's durability, a little faster, and it mines what iron mines. */
    public static final Tier VOIDSTONE = new Tier("voidstone", 190, 5.5F, 5.5F, 14, 2);
    /**
     * The outer End's crystal gear, the step after diamond: diamond's damage, a little faster and longer-lived, and it
     * mines what diamond mines; the Resonance set bonus is what sets it apart.
     */
    public static final Tier RESONITE = new Tier("resonite", 1700, 8.5F, 7.0F, 14, 3);

    /** One armour material: defence per piece (helmet, chestplate, leggings, boots), toughness. */
    public record Armour(String name, int helmet, int chestplate, int leggings, int boots, float toughness, int enchantability) {
        public int total() {
            return helmet + chestplate + leggings + boots;
        }
    }

    // vanilla, read from the 26.3 ArmorMaterials with javap, for comparison
    public static final Armour DIAMOND_ARMOUR = new Armour("diamond", 3, 8, 6, 3, 2.0F, 10);
    public static final Armour NETHERITE_ARMOUR = new Armour("netherite", 3, 8, 6, 3, 3.0F, 15);

    /** Diamond's defence and toughness; the set bonus is the step up. */
    public static final Armour RESONITE_ARMOUR = new Armour("resonite", 3, 8, 6, 3, 2.0F, 14);
    /** Armour durability: vanilla's multiplier per piece times this (diamond 33, netherite 37). */
    public static final int RESONITE_ARMOUR_DURABILITY = 35;

    private GearTiers() {}
}

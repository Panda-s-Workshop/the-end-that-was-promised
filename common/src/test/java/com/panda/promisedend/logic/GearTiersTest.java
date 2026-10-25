package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Each tier sits where the progression puts it. */
class GearTiersTest {
    @Test
    void vanillaNumbersMatchTheirTooltips() {
        // the 1 + base + bonus rule reproduces vanilla's sword tooltips
        assertEquals(2.0F, GearTiers.IRON.damageBonus(), 1e-6);
        assertEquals(3.0F, GearTiers.DIAMOND.damageBonus(), 1e-6);
        assertEquals(4.0F, GearTiers.NETHERITE.damageBonus(), 1e-6);
    }

    @Test
    void voidstoneSitsBetweenCopperAndIron() {
        GearTiers.Tier v = GearTiers.VOIDSTONE;
        assertTrue(v.durability() >= GearTiers.COPPER.durability() && v.durability() < GearTiers.IRON.durability(), "durability");
        assertTrue(v.speed() > GearTiers.COPPER.speed() && v.speed() < GearTiers.IRON.speed(), "speed");
        assertTrue(v.swordDamage() > GearTiers.COPPER.swordDamage() && v.swordDamage() < GearTiers.IRON.swordDamage(), "damage");
        assertEquals(GearTiers.IRON.miningTier(), v.miningTier(), "mines what iron mines");
        // the design numbers
        assertEquals(190, v.durability());
        assertEquals(5.5F, v.speed(), 1e-6);
        assertEquals(5.5F, v.swordDamage(), 1e-6);
    }

    @Test
    void resoniteSitsBetweenDiamondAndNetherite() {
        GearTiers.Tier r = GearTiers.RESONITE;
        // above the previous tier on every stat
        assertTrue(r.durability() > GearTiers.VOIDSTONE.durability() && r.speed() > GearTiers.VOIDSTONE.speed()
                && r.swordDamage() > GearTiers.VOIDSTONE.swordDamage() && r.miningTier() > GearTiers.VOIDSTONE.miningTier(), "above Voidstone");
        // the step after diamond (its template upgrades diamond gear) and short of netherite
        assertTrue(r.durability() > GearTiers.DIAMOND.durability() && r.durability() < GearTiers.NETHERITE.durability(), "durability");
        assertTrue(r.speed() > GearTiers.DIAMOND.speed() && r.speed() < GearTiers.NETHERITE.speed(), "speed");
        assertEquals(GearTiers.DIAMOND.swordDamage(), r.swordDamage(), 1e-6, "diamond's damage: the set bonus adds the rest");
        assertEquals(GearTiers.DIAMOND.miningTier(), r.miningTier(), "mines what diamond mines");
        assertEquals(1700, r.durability());
        assertEquals(8.5F, r.speed(), 1e-6);
        assertEquals(7.0F, r.swordDamage(), 1e-6);
        GearTiers.Armour a = GearTiers.RESONITE_ARMOUR;
        assertEquals(20, a.total(), "3/8/6/3");
        assertEquals(GearTiers.DIAMOND_ARMOUR.total(), a.total());
        assertTrue(a.toughness() >= GearTiers.DIAMOND_ARMOUR.toughness() && a.toughness() < GearTiers.NETHERITE_ARMOUR.toughness(), "toughness");
        assertTrue(GearTiers.RESONITE_ARMOUR_DURABILITY > 33 && GearTiers.RESONITE_ARMOUR_DURABILITY < 37, "armour durability between diamond and netherite");
    }
}

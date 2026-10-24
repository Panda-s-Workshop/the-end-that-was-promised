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
}

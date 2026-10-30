package com.panda.promisedend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * The progression's fight targets, each an inequality on the shipped numbers. A target is pinned on Normal, where a
 * mob's blow on a player is as written; Hard (half again) has a floor of its own, and Easy only gets kinder.
 */
class BalanceTest {
    private static final GearTiers.Armour RESONITE = GearTiers.RESONITE_ARMOUR;

    @Test
    void armourRuleMatchesVanilla() {
        // worked by hand from the 26.3 rule: 7 against 20 armour and 8 toughness keeps 27 %
        assertEquals(1.89F, Balance.afterArmour(7.0F, 20.0F, 8.0F), 1e-3);
        // armour alone never takes more than 80 % (the 20 cap): 1 against 30 armour keeps 20 %
        assertEquals(0.2F, Balance.afterArmour(1.0F, 30.0F, 0.0F), 1e-6);
        // and never less than a fifth of its value: a huge blow still loses armour / 5 / 25
        assertEquals(100.0F * (1 - 8.0F * 0.2F / 25.0F), Balance.afterArmour(100.0F, 8.0F, 0.0F), 1e-3);
    }

    @Test
    void resonitePlayerKillsASentryInFourToEightHits() {
        float sword = GearTiers.RESONITE.swordDamage();
        int plain = Balance.hitsToKill(Balance.SENTRY_HEALTH, Balance.SENTRY_ARMOUR, 0, sword, 1.0F, true);
        int critical = Balance.hitsToKill(Balance.SENTRY_HEALTH, Balance.SENTRY_ARMOUR, 0, sword, Balance.CRITICAL, true);
        System.out.printf("Sentry: %d plain hits, %d critical, with a Resonite sword in the full set%n", plain, critical);
        assertTrue(plain >= 4 && plain <= 8, "plain hits " + plain);
        assertTrue(critical >= 4 && critical <= 8, "critical hits " + critical);
    }

    @Test
    void resonitePlayerSurvivesSevenLunges() {
        float armour = RESONITE.total(), toughness = RESONITE.toughness() * 4;
        int normal = Balance.blowsSurvived(Balance.PLAYER_HEALTH, Balance.SENTRY_LUNGE, armour, toughness);
        int hard = Balance.blowsSurvived(Balance.PLAYER_HEALTH, Balance.SENTRY_LUNGE * Balance.HARD, armour, toughness);
        int easy = Balance.blowsSurvived(Balance.PLAYER_HEALTH, Balance.onEasy(Balance.SENTRY_LUNGE), armour, toughness);
        System.out.printf("Sentry: a player in full Resonite lives through %d lunges on Normal, %d on Hard, %d on Easy%n", normal, hard, easy);
        assertTrue(normal >= 7, "lunges survived on Normal " + normal);
        // Hard's floor: still more than a shield's turn of mistakes (the fight is 4-8 hits, about one lunge a hit)
        assertTrue(hard >= 5, "lunges survived on Hard " + hard);
        assertTrue(easy >= normal, "Easy is kinder: " + easy);
    }

    // each guardian's band holds for plain blows and for critical ones (every blow a falling one): ideal play either way

    @Test
    void diamondPlayerTakesOneToTwoAndAHalfMinutesOnTheSentinel() {
        float sword = GearTiers.DIAMOND.swordDamage(), swings = 4.0F + GearTiers.SWORD_SPEED;
        double plain = SentinelFight.seconds(sword, swings), critical = SentinelFight.seconds(sword * Balance.CRITICAL, swings);
        System.out.printf("Sentinel: %.0f s plain, %.0f s critical, a diamond sword at %.1f a second%n", plain, critical, swings);
        assertTrue(plain >= 60 && plain <= 150, "plain " + plain + " s");
        assertTrue(critical >= 60 && critical <= 150, "critical " + critical + " s");
    }

    @Test
    void resonitePlayerTakesThreeToSixMinutesOnTheLeviathan() {
        // the full set's Resonance: every fourth blow 2 more, a half a blow on average
        float sword = GearTiers.RESONITE.swordDamage(), ring = Resonance.BONUS_DAMAGE / Resonance.EVERY, swings = 4.0F + GearTiers.SWORD_SPEED;
        double plain = LeviathanFight.seconds(sword + ring, swings), critical = LeviathanFight.seconds(sword * Balance.CRITICAL + ring, swings);
        System.out.printf("Leviathan: %.1f min plain, %.1f min critical, a Resonite sword at %.1f a second%n", plain / 60, critical / 60, swings);
        assertTrue(plain >= 180 && plain <= 360, "plain " + plain + " s");
        assertTrue(critical >= 180 && critical <= 360, "critical " + critical + " s");
    }

    @Test
    void voidsteelPlayerTakesFourToEightMinutesOnTheArchitect() {
        float sword = GearTiers.VOIDSTEEL.swordDamage(), swings = 4.0F + GearTiers.SWORD_SPEED;
        double body = ArchitectFight.seconds(sword, swings), critical = ArchitectFight.seconds(sword * Balance.CRITICAL, swings);
        double keystone = ArchitectFight.seconds(sword * ArchitectFight.KEYSTONE_DAMAGE, swings);
        System.out.printf("Architect: %.0f s on its body (%.0f s critical), %.0f s on its keystone (%.2f times as fast), a Voidsteel sword at %.1f a second%n",
                body, critical, keystone, body / keystone, swings);
        assertTrue(body >= 240 && body <= 480, "on its body " + body + " s");
        assertTrue(critical >= 240 && critical <= 480, "on its body, critical " + critical + " s");
        assertTrue(body / keystone >= 1.5 && body / keystone <= 1.7, "the keystone " + body / keystone + " times as fast");
    }
}

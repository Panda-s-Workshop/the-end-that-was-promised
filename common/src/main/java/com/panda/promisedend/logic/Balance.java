package com.panda.promisedend.logic;

/**
 * The fight numbers the balance targets are pinned against, kept apart from the entities so a plain JUnit test can
 * reach them, and the damage arithmetic those targets need: vanilla's armour rule, the player's critical hit and the
 * Resonance ring.
 */
public final class Balance {
    /** The Purpur Sentry: its health, armour (no toughness) and lunge. */
    public static final float SENTRY_HEALTH = 40.0F, SENTRY_ARMOUR = 8.0F, SENTRY_LUNGE = 7.0F;
    /** A player's health, and what a critical hit (a falling blow) multiplies a sword's damage by. */
    public static final float PLAYER_HEALTH = 20.0F, CRITICAL = 1.5F;
    /** On Hard a mob's blow on a player is this much more (vanilla's own scaling; Normal is as written). */
    public static final float HARD = 1.5F;

    private Balance() {}

    /**
     * {@code damage} after {@code armour} and {@code toughness}: vanilla's {@code CombatRules.getDamageAfterAbsorb}
     * (26.3, read with javap), without enchantments.
     */
    public static float afterArmour(float damage, float armour, float toughness) {
        float f = 2.0F + toughness / 4.0F;
        float g = Math.clamp(armour - damage / f, armour * 0.2F, 20.0F);
        return damage * (1.0F - g / 25.0F);
    }

    /**
     * Sword blows to bring down {@code health} behind {@code armour}, each {@code damage} (times {@code critical}), and
     * every {@link Resonance#EVERY}th one {@link Resonance#BONUS_DAMAGE} more while {@code resonance} (the full set).
     */
    public static int hitsToKill(float health, float armour, float toughness, float damage, float critical, boolean resonance) {
        int hits = 0;
        while (health > 0) {
            hits++;
            float blow = damage * critical + (resonance && hits % Resonance.EVERY == 0 ? Resonance.BONUS_DAMAGE : 0);
            health -= afterArmour(blow, armour, toughness);
        }
        return hits;
    }

    /** A mob's blow of {@code damage} on a player on Easy: vanilla's half and one, never more than the blow. */
    public static float onEasy(float damage) {
        return Math.min(damage / 2.0F + 1.0F, damage);
    }

    /** How many blows of {@code damage} a player of {@code health} behind {@code armour} and {@code toughness} takes and lives. */
    public static int blowsSurvived(float health, float damage, float armour, float toughness) {
        float each = afterArmour(damage, armour, toughness);
        int n = 0;
        while (health - each * (n + 1) > 0) n++;
        return n;
    }
}

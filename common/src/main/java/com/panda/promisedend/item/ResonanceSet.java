package com.panda.promisedend.item;

import com.panda.promisedend.logic.Resonance;
import com.panda.promisedend.registry.ModSounds;
import com.panda.promisedend.registry.ModTags;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The Resonance set bonus: a player wearing four pieces from {@code #promisedend:resonance_set} (Resonite armour)
 * rings every fourth melee hit on one target within 3 s of the last ({@link Resonance}): +2 damage, Slowness II for
 * a second, a chime and a ring of glints at the target. The count lives on the server only and is lost on relog, as a
 * 3 s window may be.
 */
public final class ResonanceSet {
    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<Player, Resonance> COUNTS = new WeakHashMap<>();

    private ResonanceSet() {}

    /** The one target a hit counts on: a dragon's head, body and wings are one dragon. */
    private static Entity body(Entity target) {
        return target instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragonPart part ? part.parentMob : target;
    }

    public static boolean wearsSet(Player player) {
        for (EquipmentSlot slot : ARMOUR) {
            if (!player.getItemBySlot(slot).is(ModTags.RESONANCE_SET)) return false;
        }
        return true;
    }

    /** What the hit deals: the bonus on when this hit would ring. */
    public static float damage(Player player, Entity target, float amount) {
        if (!(player.level() instanceof ServerLevel level) || !wearsSet(player)) return amount;
        Resonance count = COUNTS.get(player);
        return count != null && count.wouldRing(body(target).getUUID(), level.getGameTime()) ? amount + Resonance.BONUS_DAMAGE : amount;
    }

    /** After the hit: counts it if it landed, and rings on the fourth. */
    public static void landed(Player player, Entity struck, boolean hurt) {
        if (!hurt || !(player.level() instanceof ServerLevel level) || !wearsSet(player)) return;
        Entity target = body(struck);
        if (!COUNTS.computeIfAbsent(player, p -> new Resonance()).hit(target.getUUID(), level.getGameTime())) return;
        if (target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, Resonance.SLOWNESS_TICKS, Resonance.SLOWNESS_AMPLIFIER), player);
        }
        level.playSound(null, target.getX(), target.getY(), target.getZ(), ModSounds.RESONITE_CHIME.get(), SoundSource.PLAYERS, 0.8F, 1.4F);
        double y = target.getY() + target.getBbHeight() * 0.6;
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6;
            level.sendParticles(ParticleTypes.GLOW, target.getX() + Math.cos(a) * 0.7, y, target.getZ() + Math.sin(a) * 0.7, 1, 0, 0, 0, 0.05);
        }
    }
}

package com.panda.promisedend.item;

import com.panda.promisedend.registry.ModTags;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * The Void-touched set bonus: a player wearing four pieces from {@code #promisedend:void_touched_set} (Voidsteel armour)
 * is held from the void's damage for {@link #HOLD_TICKS} once it first reaches them in a fall, with a cue as it takes
 * hold and as it lets go; a fall ends when they are back above the floor. An Updraft Vent lifts them half as much again
 * ({@link #LIFT}), on their own client, which moves them. The fall's start lives on the server only.
 */
public final class VoidTouched {
    private static final EquipmentSlot[] ARMOUR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** How many pieces make the set. */
    public static final int PIECES = ARMOUR.length;
    /** How long the void's damage is held off, each fall: 10 s. */
    public static final int HOLD_TICKS = 200;
    /** An updraft's lift, and the speed it lifts to, for a player in the set. */
    public static final double LIFT = 1.5;
    /** The game time each falling player first met the void's damage, this fall. */
    private static final Map<UUID, Long> FELL = new ConcurrentHashMap<>();

    private VoidTouched() {}

    public static boolean wearsSet(Player player) {
        return piecesWorn(player) == PIECES;
    }

    /** How many of the set's pieces the player has on. */
    public static int piecesWorn(Player player) {
        int worn = 0;
        for (EquipmentSlot slot : ARMOUR) {
            if (player.getItemBySlot(slot).is(ModTags.VOID_TOUCHED_SET)) worn++;
        }
        return worn;
    }

    /** What an updraft's lift is multiplied by for {@code e}: half as much again for a player in the set. */
    public static double lift(Entity e) {
        return e instanceof Player p && wearsSet(p) ? LIFT : 1.0;
    }

    /**
     * Whether the void's damage to {@code e} this tick is held off: a player in the set, within {@link #HOLD_TICKS} of
     * the first time it reached them this fall. Called each tick they are below the world, before the damage.
     */
    public static boolean holdsOff(LivingEntity e) {
        if (!(e instanceof ServerPlayer p) || !wearsSet(p)) return false;
        long now = p.level().getGameTime();
        Long since = FELL.putIfAbsent(p.getUUID(), now);
        long held = since == null ? 0 : now - since;
        if (held == 0) {
            p.sendOverlayMessage(Component.translatable("hint.promisedend.void_touched.held"));
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 1.0F, 0.6F);
        } else if (held == HOLD_TICKS) {
            p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.PLAYERS, 1.0F, 0.8F);
        }
        if (held < HOLD_TICKS && held % 5 == 0) {
            p.level().sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 6, 0.4, 0.6, 0.4, 0.02);
        }
        return held < HOLD_TICKS;
    }

    /** Ends the fall once the player is back above the floor. Called for each server player every tick. */
    public static void tick(ServerPlayer player) {
        if (player.getY() >= player.level().getMinY()) FELL.remove(player.getUUID());
    }

    /** How long the void's damage has been held off this fall, or -1 if it hasn't reached them: for the tests. */
    public static long held(ServerPlayer player) {
        Long since = FELL.get(player.getUUID());
        return since == null ? -1 : player.level().getGameTime() - since;
    }
}

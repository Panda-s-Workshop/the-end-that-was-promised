package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.logic.Offerings;
import com.panda.promisedend.platform.registry.DeferredHolder;
import com.panda.promisedend.platform.registry.DeferredRegister;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * The Void Shrines' boons, in {@link Offerings#BOONS}' order. What each does is in logic/ShrineBoons (and the Void
 * Anchor's rescue, for Rift Step); Resonant Edge is an attribute modifier.
 */
public final class ModEffects {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, PromisedEnd.MODID);

    /** An elytra wears half as fast. */
    public static final DeferredHolder<MobEffect, MobEffect> GLIDING_GRACE = EFFECTS.register("gliding_grace", () -> new Boon(0xC8B4FF));
    /** An Ender Pearl's cooldown is halved. */
    public static final DeferredHolder<MobEffect, MobEffect> PEARL_SENSE = EFFECTS.register("pearl_sense", () -> new Boon(0x2E8B7A));
    /** Updraft Vents and the Ancients' structures round the player are outlined for them. */
    public static final DeferredHolder<MobEffect, MobEffect> VOID_SIGHT = EFFECTS.register("void_sight", () -> new Boon(0x7B5CFF));
    /** The next fall into the void is caught, as by a Void Anchor, without cracking one. */
    public static final DeferredHolder<MobEffect, MobEffect> RIFT_STEP = EFFECTS.register("rift_step", () -> new Boon(0xB04AE0));
    /** A tenth more melee damage. */
    public static final DeferredHolder<MobEffect, MobEffect> RESONANT_EDGE = EFFECTS.register("resonant_edge", () -> new Boon(0x5FD6C8)
            .addAttributeModifier(Attributes.ATTACK_DAMAGE, PromisedEnd.id("effect.resonant_edge"), 0.1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    /** Levitation doesn't take hold. */
    public static final DeferredHolder<MobEffect, MobEffect> SHULKER_WARD = EFFECTS.register("shulker_ward", () -> new Boon(0xA97CA9));

    /** The boons by their index in {@link Offerings#BOONS}. */
    public static final List<DeferredHolder<MobEffect, MobEffect>> BOONS = List.of(GLIDING_GRACE, PEARL_SENSE, VOID_SIGHT, RIFT_STEP, RESONANT_EDGE, SHULKER_WARD);

    private ModEffects() {}

    public static Holder<MobEffect> boon(int index) {
        return BOONS.get(index).holder();
    }

    /** A boon: beneficial, its colour, no behaviour of its own (the game's systems read it). */
    private static final class Boon extends MobEffect {
        Boon(int colour) {
            super(MobEffectCategory.BENEFICIAL, colour);
        }
    }
}

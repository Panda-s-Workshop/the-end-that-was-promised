package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.platform.registry.DeferredRegister;
import java.util.function.Supplier;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;

/** The mod's own particles. Their providers are in client/ModParticleProviders. */
public final class ModParticles {
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, PromisedEnd.MODID);

    /** Chorus pollen: a pale lilac mote that drifts down through the Chorus Thicket, glowing faintly. */
    public static final Supplier<SimpleParticleType> CHORUS_POLLEN = PARTICLES.register("chorus_pollen", () -> new SimpleParticleType(false));

    /** A Drift Blossom's petal: white-lilac, carried sideways on the air as it falls, glowing faintly. */
    public static final Supplier<SimpleParticleType> DRIFT_PETAL = PARTICLES.register("drift_petal", () -> new SimpleParticleType(false));

    private ModParticles() {}
}

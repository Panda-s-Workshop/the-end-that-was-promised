package com.panda.promisedend.client;

import com.panda.promisedend.registry.ModParticles;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;

/** Every particle provider, for the loader's provider registration. */
public final class ModParticleProviders {
    public interface Sink {
        <T extends ParticleOptions> void sprites(ParticleType<T> type, SpriteProvider<T> provider);
    }

    /** A provider made from the type's sprite set (vanilla's own interface for this is package-private). */
    public interface SpriteProvider<T extends ParticleOptions> {
        ParticleProvider<T> create(SpriteSet sprites);
    }

    public static void register(Sink sink) {
        sink.sprites(ModParticles.CHORUS_POLLEN.get(), sprites -> (options, level, x, y, z, dx, dy, dz, random) ->
                new ChorusPollen(level, x, y, z, sprites));
    }

    private ModParticleProviders() {}

    /**
     * Chorus pollen: drifts down slowly, swaying, and fades in and out. It carries its own light: a leaf-like particle
     * takes the block light, and the End's is so low that lilac drew near-black.
     */
    static final class ChorusPollen extends SingleQuadParticle {
        private final SpriteSet sprites;
        private final float phase;

        ChorusPollen(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
            super(level, x, y, z, sprites.first());
            this.sprites = sprites;
            this.xd = 0;
            this.yd = -0.004;
            this.zd = 0;
            this.friction = 0.96F;
            this.gravity = 0.003F;
            this.hasPhysics = true;
            this.lifetime = 80 + this.random.nextInt(60);
            this.quadSize = 0.05F + this.random.nextFloat() * 0.04F;
            this.phase = this.random.nextFloat() * Mth.TWO_PI;
            float shade = 0.85F + this.random.nextFloat() * 0.15F;
            this.setColor(shade, shade, shade);
            this.setAlpha(0.0F);
            this.setSpriteFromAge(sprites);
        }

        @Override
        public void tick() {
            super.tick();
            if (this.removed) return;
            this.setSpriteFromAge(this.sprites);
            this.xd += Mth.sin(this.age * 0.12F + this.phase) * 0.0015;
            this.zd += Mth.cos(this.age * 0.10F + this.phase) * 0.0015;
            float t = (float) this.age / this.lifetime;
            this.setAlpha(0.85F * Math.min(1.0F, Math.min(t / 0.15F, (1.0F - t) / 0.3F)));
        }

        @Override
        protected Layer getLayer() {
            return Layer.TRANSLUCENT;
        }

        @Override
        public int getLightCoords(float partialTick) {
            return LightCoordsUtil.FULL_BRIGHT;
        }
    }
}

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
        // pollen sinks slowly and sways; a petal falls a little faster and is carried sideways on the air; a rift mote rises
        sink.sprites(ModParticles.CHORUS_POLLEN.get(), sprites -> (options, level, x, y, z, dx, dy, dz, random) ->
                new Mote(level, x, y, z, sprites, POLLEN));
        sink.sprites(ModParticles.DRIFT_PETAL.get(), sprites -> (options, level, x, y, z, dx, dy, dz, random) ->
                new Mote(level, x, y, z, sprites, PETAL));
        sink.sprites(ModParticles.RIFT_MOTE.get(), sprites -> (options, level, x, y, z, dx, dy, dz, random) ->
                new Mote(level, x, y, z, sprites, RIFT));
        sink.sprites(ModParticles.AMBER_MOTE.get(), sprites -> (options, level, x, y, z, dx, dy, dz, random) ->
                new Mote(level, x, y, z, sprites, AMBER));
        sink.sprites(ModParticles.UPDRAFT_WISP.get(), sprites -> (options, level, x, y, z, dx, dy, dz, random) ->
                new Mote(level, x, y, z, sprites, WISP));
        sink.sprites(ModParticles.UPDRAFT_PUFF.get(), sprites -> (options, level, x, y, z, dx, dy, dz, random) ->
                new Mote(level, x, y, z, sprites, PUFF));
        sink.sprites(ModParticles.CURRENT_MOTE.get(), sprites -> (options, level, x, y, z, dx, dy, dz, random) ->
                new Carried(level, x, y, z, sprites, CURRENT, dx, dy, dz));
    }

    private ModParticleProviders() {}

    /** How a mote moves: its start fall (up when positive), gravity, sway, a steady sideways drift, lifetime and size ranges. */
    record Motion(double fall, float gravity, double sway, double wind, int life, int lifeSpread, float size, float sizeSpread) {}

    static final Motion POLLEN = new Motion(-0.004, 0.003F, 0.0015, 0.0, 80, 60, 0.05F, 0.04F);
    static final Motion PETAL = new Motion(-0.01, 0.004F, 0.003, 0.012, 60, 50, 0.07F, 0.04F);
    /** Rising, not falling: a start upward and gravity the other way, slower than the others sink. */
    static final Motion RIFT = new Motion(0.012, -0.003F, 0.001, 0.0, 60, 50, 0.05F, 0.03F);
    /** Amber's motes hang in the cavern air, barely sinking: a slow start down, little gravity, a wide sway. */
    static final Motion AMBER = new Motion(-0.002, 0.0005F, 0.002, 0.0, 70, 50, 0.04F, 0.03F);
    /**
     * An updraft's wisp rises fast and steadily (gravity the other way holds it near 0.29 a tick against the
     * friction), some 6-10 blocks in its life; its puff rises at about 0.12, larger and softer, on vanilla's poof sprites.
     */
    static final Motion WISP = new Motion(0.3, -0.3F, 0.001, 0.0, 20, 15, 0.16F, 0.08F);
    static final Motion PUFF = new Motion(0.15, -0.12F, 0.002, 0.0, 20, 15, 0.18F, 0.12F);
    /** A void current's mote neither rises nor falls: it drifts along at the speed it was given, some 20-35 blocks. */
    static final Motion CURRENT = new Motion(0.0, 0.0F, 0.0008, 0.0, 50, 40, 0.22F, 0.1F);

    /**
     * A mote: drifts down slowly, swaying, and fades in and out. It carries its own light: a leaf-like particle takes
     * the block light, and the End's is so low that pale colours drew near-black.
     */
    static class Mote extends SingleQuadParticle {
        private final SpriteSet sprites;
        private final Motion motion;
        private final float phase;
        private final double windX, windZ;

        Mote(ClientLevel level, double x, double y, double z, SpriteSet sprites, Motion motion) {
            super(level, x, y, z, sprites.first());
            this.sprites = sprites;
            this.motion = motion;
            this.xd = 0;
            this.yd = motion.fall();
            this.zd = 0;
            this.friction = 0.96F;
            this.gravity = motion.gravity();
            this.hasPhysics = true;
            this.lifetime = motion.life() + this.random.nextInt(motion.lifeSpread());
            this.quadSize = motion.size() + this.random.nextFloat() * motion.sizeSpread();
            this.phase = this.random.nextFloat() * Mth.TWO_PI;
            // the wind blows one way through a whole area (the same way for every mote near by), a little each way of it
            int area = Math.floorMod(Math.floorDiv(Mth.floor(x), 64) * 31 + Math.floorDiv(Mth.floor(z), 64) * 17, 8);
            float heading = area * Mth.TWO_PI / 8 + (this.random.nextFloat() - 0.5F) * 0.6F;
            this.windX = Mth.cos(heading) * motion.wind();
            this.windZ = Mth.sin(heading) * motion.wind();
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
            this.xd += Mth.sin(this.age * 0.12F + this.phase) * motion.sway() + windX * 0.04;
            this.zd += Mth.cos(this.age * 0.10F + this.phase) * motion.sway() + windZ * 0.04;
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

    /**
     * A void current's mote, carried at a speed of its own across ({@code dx}, {@code dz}) that the air doesn't slow,
     * {@code dy} times larger than its own size again (far from the camera, so a river reads from an island). Every few
     * ticks it turns with the current where it is, and fades out soon after leaving it, so the drawn river keeps to the
     * real one's bends and ends.
     */
    static final class Carried extends Mote {
        /** How often it looks for the current again, and how long it takes to fade once out of it, in ticks. */
        static final int TURN_EVERY = 5, FADE = 8;

        Carried(ClientLevel level, double x, double y, double z, SpriteSet sprites, Motion motion, double dx, double dy, double dz) {
            super(level, x, y, z, sprites, motion);
            this.xd = dx;
            this.zd = dz;
            this.friction = 1.0F;
            this.quadSize *= (float) (1 + Math.max(0, dy));
        }

        @Override
        public void tick() {
            super.tick();
            if (this.removed || this.age % TURN_EVERY != 0) return;
            double[] f = com.panda.promisedend.logic.VoidCurrents.flowAt(this.level, this.x, this.y, this.z);
            if (f == null) {
                this.lifetime = Math.min(this.lifetime, this.age + FADE);
                return;
            }
            double speed = Math.hypot(this.xd, this.zd);
            this.xd = f[0] * speed;
            this.zd = f[1] * speed;
        }
    }
}

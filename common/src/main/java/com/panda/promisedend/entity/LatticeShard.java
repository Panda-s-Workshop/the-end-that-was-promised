package com.panda.promisedend.entity;

import com.panda.promisedend.logic.ArchitectFight;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModEntities;
import com.panda.promisedend.registry.ModSounds;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Architect's throw: a lump of Raw Lattice out of its keystone, lobbed high so it comes down over the walls it
 * raises. It deals {@link #DAMAGE} to what it lands on and breaks up; it places nothing, and passes through the
 * Ancients' own.
 */
public class LatticeShard extends ThrowableItemProjectile {
    public static final float DAMAGE = 7.0F;
    /**
     * Its fall, in blocks a tick a tick, and how fast it goes out across the ground: slow, and in the air a second at
     * least ({@link ArchitectFight#FLIGHT_LEAST}), so it goes up over the walls before it comes down.
     */
    public static final double GRAVITY = 0.04, SPEED = 0.4;
    /** It breaks up after this long, wherever it is. */
    public static final int LIFETIME = 120;
    /** The ring that shows where it will come down: this wide, on the ground under its aim. */
    public static final double RING = 1.2;

    /** Where it was thrown to come down, while it flies (not saved: one loaded mid-flight shows no ring). */
    private @org.jspecify.annotations.Nullable Vec3 aim;

    public LatticeShard(EntityType<? extends LatticeShard> type, Level level) {
        super(type, level);
    }

    /** A lob from {@code architect}'s keystone, to come down on {@code aim}. */
    public LatticeShard(Level level, TheArchitect architect, Vec3 aim) {
        super(ModEntities.LATTICE_SHARD.get(), architect, level, ModBlocks.RAW_LATTICE.get().asItem().getDefaultInstance());
        this.setPos(architect.keystonePosition());
        setDeltaMovement(lob(position(), aim, SPEED, GRAVITY));
        this.aim = aim;
    }

    /** Where it will come down, if it was thrown at somewhere. */
    public java.util.Optional<Vec3> aim() {
        return java.util.Optional.ofNullable(aim);
    }

    /** The throw from {@code from} that comes down on {@code to} ({@link ArchitectFight#lob}). */
    public static Vec3 lob(Vec3 from, Vec3 to, double speed, double gravity) {
        double[] v = ArchitectFight.lob(to.x - from.x, to.y - from.y, to.z - from.z, speed, gravity);
        return new Vec3(v[0], v[1], v[2]);
    }

    @Override
    protected Item getDefaultItem() {
        return ModBlocks.RAW_LATTICE.get().asItem();
    }

    @Override
    protected double getDefaultGravity() {
        return GRAVITY;
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        if (level().isClientSide()) {
            if (tickCount % 2 == 0) level().addParticle(ParticleTypes.END_ROD, getX(), getY(), getZ(), 0, 0, 0);
        } else if (tickCount > LIFETIME) {
            breakUp((ServerLevel) level());
        } else if (aim != null && tickCount % 2 == 0) {
            ring((ServerLevel) level());
        }
    }

    /**
     * A lilac ring on the ground under its aim (the aim is at a body's middle, its feet in that block), as the
     * Leviathan's breach shows: lit motes that hang still a moment, as dust draws near-black in the End's low light.
     */
    private void ring(ServerLevel level) {
        double y = Math.floor(aim.y) + 0.1;
        for (int i = 0; i < 14; i++) {
            double a = Math.PI * 2 * i / 14;
            level.sendParticles(com.panda.promisedend.registry.ModParticles.CHORUS_VINE.get(), aim.x + Math.cos(a) * RING, y, aim.z + Math.sin(a) * RING, 1, 0, 0, 0, 0);
        }
    }

    private void breakUp(ServerLevel level) {
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ModBlocks.RAW_LATTICE.get().defaultBlockState()),
                getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.1);
        discard();
    }

    /** It passes through the Ancients' own (the Architect, its Crawlers) and other shards. */
    @Override
    protected boolean canHitEntity(Entity target) {
        if (target instanceof LatticeShard) return false;
        if (target instanceof LivingEntity living && !Constructs.mayAttack(living)) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!(level() instanceof ServerLevel level)) return;
        Entity owner = getOwner();
        hit.getEntity().hurtServer(level, damageSources().mobProjectile(this, owner instanceof LivingEntity l ? l : null), DAMAGE);
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel level) {
            playSound(ModSounds.LATTICE_SHARD_HIT.get(), 1.0F, 0.9F + random.nextFloat() * 0.2F);
            breakUp(level);
        }
    }
}

package com.panda.promisedend.entity;

import com.panda.promisedend.registry.ModEntities;
import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.registry.ModSounds;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Hollow Sentinel's shot: a Resonite shard that drifts after its target, turning a little each tick, and on a hit
 * deals {@link #DAMAGE} and a second of Levitation. A player's blow turns it back, as it does a ghast's fireball
 * ({@code #minecraft:redirectable_projectile}, which also lets it be aimed at): a returned bullet seeks the Sentinel
 * that fired it, faster, and cracks its shell a step when it lands ({@link HollowSentinel#bulletReturned()}).
 */
public class ResonantBullet extends ThrowableItemProjectile {
    public static final float DAMAGE = 4.0F;
    /** A second of Levitation: a lift that spoils footing, not a fall that kills. */
    public static final int LEVITATION_TICKS = 20;
    /** How fast it flies out (blocks a tick, before the phase's speed-up), and back. */
    public static final double SPEED = com.panda.promisedend.logic.SentinelFight.BULLET_SPEED, RETURN_SPEED = com.panda.promisedend.logic.SentinelFight.RETURN_SPEED;
    /** How much of the way to its target it turns each tick. */
    public static final double TURN = 0.08;
    /** It breaks up after this long, out or back. */
    public static final int LIFETIME = 160;

    private @Nullable UUID sentinelId;
    private @Nullable UUID targetId;
    private double speed = SPEED;
    private float damage = DAMAGE;
    private boolean returned;

    public ResonantBullet(EntityType<? extends ResonantBullet> type, Level level) {
        super(type, level);
    }

    /** A shot from {@code sentinel}'s core at {@code target}, at {@code speed} for {@code damage}. */
    public ResonantBullet(Level level, HollowSentinel sentinel, LivingEntity target, double speed, float damage) {
        super(ModEntities.RESONANT_BULLET.get(), sentinel, level, ModItems.RESONITE_SHARD.get().getDefaultInstance());
        this.setPos(sentinel.corePosition());
        this.sentinelId = sentinel.getUUID();
        this.targetId = target.getUUID();
        this.speed = speed;
        this.damage = damage;
        Vec3 to = aim(target).subtract(position());
        // out of the core upward first, so a volley fans before it turns on its target
        setDeltaMovement(to.normalize().add(0, 0.6, 0).normalize().scale(speed));
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.RESONITE_SHARD.get();
    }

    public boolean returned() {
        return returned;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    private static Vec3 aim(Entity target) {
        return target.position().add(0, target.getBbHeight() * 0.5, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) return;
        Level level = level();
        if (level.isClientSide()) {
            level.addParticle(returned ? ParticleTypes.END_ROD : new DustParticleOptions(0x5FD6C8, 1.0F), getX(), getY(), getZ(), 0, 0, 0);
            return;
        }
        if (tickCount > LIFETIME) {
            fizzle((ServerLevel) level);
            return;
        }
        Entity seek = ((ServerLevel) level).getEntity(returned ? sentinelId : targetId);
        if (seek == null || !seek.isAlive()) {
            if (returned) fizzle((ServerLevel) level);
            return;
        }
        Vec3 want = aim(seek).subtract(position()).normalize();
        Vec3 now = getDeltaMovement().normalize();
        double turn = returned ? 0.35 : TURN;
        setDeltaMovement(now.add(want.subtract(now).scale(turn)).normalize().scale(returned ? RETURN_SPEED : speed));
    }

    private void fizzle(ServerLevel level) {
        level.sendParticles(ParticleTypes.END_ROD, getX(), getY(), getZ(), 4, 0.1, 0.1, 0.1, 0.02);
        discard();
    }

    /**
     * Struck by a player (vanilla's deflection by attack, which makes them its owner): it turns back on the Sentinel,
     * ringing. Only a player's: vanilla also counts one projectile hitting a redirectable one as a blow.
     */
    @Override
    protected void onDeflection(boolean byAttack) {
        super.onDeflection(byAttack);
        if (!byAttack || returned || !(getOwner() instanceof net.minecraft.world.entity.player.Player) || !(level() instanceof ServerLevel level)) return;
        returned = true;
        this.tickCount = 0;
        if (level.getEntity(sentinelId) instanceof HollowSentinel sentinel) {
            setDeltaMovement(aim(sentinel).subtract(position()).normalize().scale(RETURN_SPEED));
        }
        playSound(ModSounds.RESONANT_BULLET_RETURN.get(), 1.5F, 1.0F);
    }

    /**
     * Out, it passes through the Ancients' own (the Sentinel, its Sentries) and through other bullets (a volley's three
     * met and turned each other back); back, it hits only the Sentinel.
     */
    @Override
    protected boolean canHitEntity(Entity target) {
        if (target instanceof ResonantBullet) return false;
        if (returned) return target.getUUID().equals(sentinelId);
        if (target.getUUID().equals(sentinelId)) return false;
        if (target instanceof LivingEntity living && !Constructs.mayAttack(living)) return false;
        return super.canHitEntity(target);
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!(level() instanceof ServerLevel level)) return;
        Entity target = hit.getEntity();
        if (returned) {
            if (target instanceof HollowSentinel sentinel) sentinel.bulletReturned();
            return;
        }
        Entity sentinel = level.getEntity(sentinelId);
        if (target.hurtServer(level, damageSources().mobProjectile(this, sentinel instanceof LivingEntity l ? l : null), damage)
                && target instanceof LivingEntity living) {
            living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, LEVITATION_TICKS), sentinel);
            if (sentinel instanceof HollowSentinel s) s.bulletLanded(living);
        }
        playSound(ModSounds.RESONANT_BULLET_HIT.get(), 1.0F, 1.0F);
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level() instanceof ServerLevel level) fizzle(level);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("sentinel", UUIDUtil.CODEC, sentinelId);
        output.storeNullable("target", UUIDUtil.CODEC, targetId);
        output.putDouble("speed", speed);
        output.putFloat("damage", damage);
        output.putBoolean("returned", returned);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.sentinelId = input.read("sentinel", UUIDUtil.CODEC).orElse(null);
        this.targetId = input.read("target", UUIDUtil.CODEC).orElse(null);
        this.speed = input.getDoubleOr("speed", SPEED);
        this.damage = input.getFloatOr("damage", DAMAGE);
        this.returned = input.getBooleanOr("returned", false);
    }
}

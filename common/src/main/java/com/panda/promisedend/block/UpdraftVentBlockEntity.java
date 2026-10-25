package com.panda.promisedend.block;

import com.panda.promisedend.logic.Updraft;
import com.panda.promisedend.registry.ModBlockEntities;
import com.panda.promisedend.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.BlockAttachedEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Lifts what is in its vent's column. The server lifts everything but players and what players drive; a player moves
 * themselves (and their boat or mount), as under levitation, so the local player's client lifts them, and the server,
 * which works out the same column, keeps a player in it from being kicked for flying and from taking fall damage:
 * coming down onto a vent, or beside it in its column, is safe from any height. Sneaking in the column sinks slowly
 * instead, the way down. Left alone: spectators, players flying in creative, anything without gravity (fireballs,
 * markers, displays), projectiles (an arrow or a pearl shot up a column would bob over it for ever), passengers,
 * things hung on blocks, and {@code #promisedend:updraft_ignores} (the dragon, end crystals). The column stops at the
 * first block with a full collision box, looked for every second.
 */
public class UpdraftVentBlockEntity extends BlockEntity {
    /** How often the column's top is looked for again, in ticks. */
    private static final int RESCAN = 20;
    private int height = -1;
    /** Ticks to the next look; each vent starts at its own point in the second, so they don't all look on one tick. */
    private int rescanIn;
    /** The client's camera, and whether its particles are set to Minimal: set by the client's setup, null on a server. */
    public static Supplier<Vec3> camera = () -> null;
    public static BooleanSupplier minimalParticles = () -> false;

    public UpdraftVentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UPDRAFT_VENT.get(), pos, state);
        rescanIn = Math.floorMod(pos.hashCode(), RESCAN);
    }

    /** The column's height now: the vent's own, or up to the first full block in it. */
    public int height() {
        return Math.max(0, height);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, UpdraftVentBlockEntity vent) {
        if (vent.height < 0 || --vent.rescanIn < 0) {
            vent.height = open(level, pos, UpdraftVentBlock.height(state));
            vent.rescanIn = RESCAN - 1;
        }
        int h = vent.height;
        if (h <= 0) return;
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5, base = pos.getY() + 1;
        AABB box = new AABB(cx - Updraft.RADIUS, base, cz - Updraft.RADIUS, cx + Updraft.RADIUS, base + h, cz + Updraft.RADIUS);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box, UpdraftVentBlockEntity::lifts)) {
            if (!Updraft.inColumn(e.getX() - cx, e.getY() - base, e.getZ() - cz, h)) continue;
            Player player = e instanceof Player p ? p : e.getControllingPassenger() instanceof Player p ? p : null;
            if (player != null) {
                if (player.getAbilities().flying) continue;
                // a player, or what one drives: their client moves it
                if (level.isClientSide()) {
                    if (player.isLocalPlayer()) {
                        if (player.isShiftKeyDown()) sinkOrLift(e, true, false);
                        else sinkOrLift(e, false, e == player && player.isFallFlying());
                    }
                } else if (player instanceof ServerPlayer server) {
                    server.resetFallDistance();
                    e.resetFallDistance();
                    server.connection.resetFlyingTicks();
                }
            } else if (!level.isClientSide()) {
                if (sinkOrLift(e, false, false)) e.needsSync = true;
            }
        }
        if (level.isClientSide() && level.getGameTime() % Updraft.PARTICLES_EVERY == 0) particles(level, state, cx, base, cz, h);
    }

    /** Who the column moves: not spectators, the weightless, projectiles, passengers, things hung on blocks, or the ignored. */
    private static boolean lifts(Entity e) {
        return !e.isSpectator() && e.getGravity() > 0 && !e.isNoGravity() && !(e instanceof Projectile) && !e.isPassenger()
                && !(e instanceof BlockAttachedEntity) && !e.is(ModTags.UPDRAFT_IGNORES);
    }

    /** Sets the entity's speed up for the next tick; true if it changed. */
    private static boolean sinkOrLift(Entity e, boolean sink, boolean gliding) {
        Vec3 v = e.getDeltaMovement();
        boolean slowFalling = e instanceof LivingEntity living && living.hasEffect(MobEffects.SLOW_FALLING);
        double taken = Updraft.taken(e.getGravity(), v.y <= 0, slowFalling, e.isInWater() || e.isInLava(), gliding, e.getXRot());
        double vy = sink ? Updraft.SINK : Updraft.lift(v.y, gliding, taken);
        e.resetFallDistance();
        if (vy == v.y) return false;
        e.setDeltaMovement(v.x, vy, v.z);
        return true;
    }

    /**
     * Pale puffs rising over the whole column, more the taller it is, none close round the camera; an ancient column
     * also sparks, and is seen from 96 blocks, not the usual 32, so a glider can find it from across the void. Past 32
     * the game would drop them, so an ancient column's are forced, and then keep to the Minimal setting themselves.
     * (Not the game's cloud, which follows a player near it: a rider would carry the puffs up round their head.)
     */
    private static void particles(Level level, BlockState state, double cx, double base, double cz, int h) {
        Vec3 eye = camera.get();
        if (eye == null) return;
        boolean ancient = state.getValue(UpdraftVentBlock.ANCIENT);
        double ny = Math.max(base, Math.min(eye.y, base + h));
        double far = (cx - eye.x) * (cx - eye.x) + (ny - eye.y) * (ny - eye.y) + (cz - eye.z) * (cz - eye.z);
        if (ancient && (far > Updraft.SEEN_ANCIENT * Updraft.SEEN_ANCIENT || minimalParticles.getAsBoolean())) return;
        var random = level.getRandom();
        double clear = Updraft.CLEAR_OF_CAMERA * Updraft.CLEAR_OF_CAMERA;
        int puffs = far > Updraft.SEEN * Updraft.SEEN ? 1 : Updraft.puffs(h);
        for (int i = 0; i < puffs; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * Updraft.RADIUS * 0.8;
            double x = cx + Math.cos(a) * r, y = base + random.nextDouble() * h, z = cz + Math.sin(a) * r;
            if (eye.distanceToSqr(x, y, z) < clear) continue;
            level.addParticle(ParticleTypes.POOF, ancient, false, x, y, z, 0, 0.12 + random.nextDouble() * 0.1, 0);
        }
        if (ancient) {
            double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * Updraft.RADIUS * 0.6;
            level.addParticle(ParticleTypes.END_ROD, true, false, cx + Math.cos(a) * r, base + random.nextDouble() * h, cz + Math.sin(a) * r, 0, 0.2, 0);
        }
    }

    /** How many blocks over the vent are open, up to {@code max}: the column stops under the first full block. */
    private static int open(Level level, BlockPos pos, int max) {
        BlockPos.MutableBlockPos at = pos.mutable();
        for (int dy = 1; dy <= max; dy++) {
            at.setY(pos.getY() + dy);
            if (level.getBlockState(at).isCollisionShapeFullBlock(level, at)) return dy - 1;
        }
        return max;
    }
}

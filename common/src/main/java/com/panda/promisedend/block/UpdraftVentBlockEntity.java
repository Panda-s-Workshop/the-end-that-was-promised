package com.panda.promisedend.block;

import com.panda.promisedend.logic.Updraft;
import com.panda.promisedend.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Lifts what is in its vent's column. The server lifts everything but players; a player moves themselves, as under
 * levitation, so the local player's client lifts them, and the server, which works out the same column, keeps a
 * player in it from being kicked for flying and from taking fall damage. The column stops at the first block with a
 * full collision box, looked for every second.
 */
public class UpdraftVentBlockEntity extends BlockEntity {
    /** How often the column's top is looked for again, in ticks. */
    private static final int RESCAN = 20;
    private int height = -1;

    public UpdraftVentBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.UPDRAFT_VENT.get(), pos, state);
    }

    /** The column's height now: the vent's own, or up to the first full block in it. */
    public int height() {
        return Math.max(0, height);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, UpdraftVentBlockEntity vent) {
        if (vent.height < 0 || level.getGameTime() % RESCAN == 0) vent.height = open(level, pos, UpdraftVentBlock.height(state));
        int h = vent.height;
        if (h <= 0) return;
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5, base = pos.getY() + 1;
        AABB box = new AABB(cx - Updraft.RADIUS, base, cz - Updraft.RADIUS, cx + Updraft.RADIUS, base + h, cz + Updraft.RADIUS);
        for (Entity e : level.getEntitiesOfClass(Entity.class, box, e -> !e.isSpectator() && !e.isNoGravity() && !e.isPassenger() && !(e instanceof HangingEntity))) {
            if (!Updraft.inColumn(e.getX() - cx, e.getY() - base, e.getZ() - cz, h)) continue;
            if (e instanceof Player player) {
                if (level.isClientSide()) {
                    if (player.isLocalPlayer()) lift(e, player.isFallFlying());
                } else if (player instanceof ServerPlayer server) {
                    server.resetFallDistance();
                    server.connection.resetFlyingTicks();
                }
            } else if (!level.isClientSide()) {
                lift(e, false);
                e.needsSync = true;
            }
        }
        if (level.isClientSide() && level.getGameTime() % Updraft.PARTICLES_EVERY == 0) {
            var random = level.getRandom();
            for (int i = 0; i < 2; i++) {
                double a = random.nextDouble() * Math.PI * 2, r = Math.sqrt(random.nextDouble()) * Updraft.RADIUS * 0.8;
                double y = base + random.nextDouble() * Math.min(h, 6);
                level.addParticle(ParticleTypes.CLOUD, cx + Math.cos(a) * r, y, cz + Math.sin(a) * r, 0, 0.12 + random.nextDouble() * 0.1, 0);
            }
        }
    }

    private static void lift(Entity e, boolean gliding) {
        Vec3 v = e.getDeltaMovement();
        e.setDeltaMovement(v.x, Updraft.lift(v.y, gliding), v.z);
        e.resetFallDistance();
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

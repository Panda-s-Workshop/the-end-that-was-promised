package com.panda.promisedend.logic;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.registry.ModTags;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPlatformFeature;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The Void Anchor. Every 20 ticks a player standing on safe ground has that spot remembered; a player who
 * falls {@link EndConfig#anchorTriggerDepth()} blocks below the dimension's floor while carrying an anchor is pulled
 * back to it with Slow Falling, and the anchor cracks. The spot lives in memory only: after a logout
 * it falls back to the End's main island or the world spawn. The spot is checked again at the rescue, since the
 * player may have mined it away.
 */
public final class VoidAnchor {
    public static final int RECORD_INTERVAL = 20;
    public static final int SLOW_FALLING_TICKS = 100;
    /**
     * Safe footing has ground under every neighbouring column this far out: the middle of a 3-wide bridge counts, a
     * 1-wide bridge or an island's last block doesn't.
     */
    public static final int EDGE_MARGIN = 1;
    /** A glider is caught this far below the floor, still above vanilla's void damage at floor - 64. */
    public static final int GLIDER_DEPTH = 56;
    /** How far round a footing that no longer holds the rescue looks for one that does. */
    public static final int SEARCH_RADIUS = 4;

    private static final Map<UUID, GlobalPos> FOOTING = new ConcurrentHashMap<>();
    /** Players already told during this fall that their anchor is cracked, so the message isn't sent every tick. */
    private static final Map<UUID, Boolean> WARNED = new ConcurrentHashMap<>();
    /** Players already given the "the anchor has you" cue during this fall, so the long fall reads as caught. */
    private static final Map<UUID, Boolean> CUED = new ConcurrentHashMap<>();

    private VoidAnchor() {}

    /** Called for each server player at the end of every server tick (both loaders). */
    public static void tick(ServerPlayer player) {
        if (player.isSpectator()) return;
        ServerLevel level = player.level();
        // a flier isn't falling; a glider may still pull up, so it's caught lower down
        boolean gliding = player.isFallFlying();
        boolean falling = !gliding && !player.getAbilities().flying;
        boolean glidingDown = gliding && player.getDeltaMovement().y < 0 && player.getY() < level.getMinY() - GLIDER_DEPTH;
        if (falling && player.getY() < triggerY(level) || glidingDown) {
            rescue(player);
            return;
        }
        if (falling && player.getY() < level.getMinY() && player.getDeltaMovement().y < 0) {
            // past the floor with an intact anchor: a chime, so the long fall reads as caught before it is
            if (find(player, true) >= 0 && CUED.putIfAbsent(player.getUUID(), true) == null) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
            }
            return;
        }
        WARNED.remove(player.getUUID());
        CUED.remove(player.getUUID());
        if (player.tickCount % RECORD_INTERVAL == 0 && isSafeFooting(player)) {
            FOOTING.put(player.getUUID(), GlobalPos.of(level.dimension(), player.blockPosition()));
        }
    }

    public static int triggerY(Level level) {
        return level.getMinY() - EndConfig.anchorTriggerDepth();
    }

    /**
     * On ground, on a block with a sturdy top or in {@code #promisedend:void_safe_ground} (slabs, stairs, paths), not
     * in lava or riding, and {@link #EDGE_MARGIN} blocks from any void edge.
     */
    public static boolean isSafeFooting(ServerPlayer player) {
        if (!player.onGround() || player.isInLava() || player.isPassenger() || player.getAbilities().flying) return false;
        return isSafeGround(player.level(), player.blockPosition());
    }

    /** Whether feet at {@code feet} stand on safe ground, {@link #EDGE_MARGIN} blocks from any void column. */
    public static boolean isSafeGround(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        BlockState ground = level.getBlockState(below);
        // a bottom slab, a path or farmland holds the feet inside its own block; stairs hold them on top
        boolean standing = ground.isFaceSturdy(level, below, Direction.UP) || ground.is(ModTags.VOID_SAFE_GROUND)
                || level.getBlockState(feet).is(ModTags.VOID_SAFE_GROUND);
        if (!standing) return false;
        int floor = level.getMinY();
        for (int d = 1; d <= EDGE_MARGIN; d++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    if (!groundUnder(level, feet.getX() + dx * d, feet.getY(), feet.getZ() + dz * d)) return false;
                }
            }
        }
        return true;
    }

    /**
     * The height above a column's highest block, the floor itself when the column is empty: void. It loads the chunk
     * first ({@code Level.getHeight} reads an unloaded chunk as the floor) and counts any block, since other mods'
     * solid blocks are rarely in the motion-blocking tags.
     */
    private static int columnTop(ServerLevel level, int x, int z) {
        return level.getChunk(x >> 4, z >> 4).getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15) + 1;
    }

    /**
     * Whether column (x, z) holds a block at or below {@code y}: a roof or a bridge overhead isn't ground. Only a column
     * with something above {@code y} is scanned, down from {@code y}.
     */
    private static boolean groundUnder(ServerLevel level, int x, int y, int z) {
        int top = columnTop(level, x, z);
        if (top <= level.getMinY()) return false;
        if (top <= y + 1) return true;
        var pos = new BlockPos.MutableBlockPos(x, y, z);
        for (; pos.getY() >= level.getMinY(); pos.move(Direction.DOWN)) {
            if (!level.getBlockState(pos).isAir()) return true;
        }
        return false;
    }

    /** Room to stand at {@code feet}: the feet block is open or a safe partial block, and the head block is open. */
    private static boolean fits(ServerLevel level, BlockPos feet) {
        BlockState at = level.getBlockState(feet);
        boolean feetOpen = at.getCollisionShape(level, feet).isEmpty() || at.is(ModTags.VOID_SAFE_GROUND);
        return feetOpen && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    /**
     * Where a rescue lands near {@code footing}: the footing itself if it still holds (the block may have been mined
     * since it was recorded), else the nearest safe spot within {@link #SEARCH_RADIUS} blocks, else null.
     */
    public static BlockPos landingNear(ServerLevel level, BlockPos footing) {
        if (isSafeGround(level, footing) && fits(level, footing)) return footing;
        for (int r = 1; r <= SEARCH_RADIUS; r++) {
            for (int dy : new int[] {0, 1, -1, 2, -2, 3, -3}) {
                for (int dx = -r; dx <= r; dx++) {
                    for (int dz = -r; dz <= r; dz++) {
                        if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                        BlockPos p = footing.offset(dx, dy, dz);
                        if (isSafeGround(level, p) && fits(level, p)) return p;
                    }
                }
            }
        }
        return null;
    }

    /**
     * The End's last resort: the arrival platform if it still stands, then the main island's top walking in from
     * the platform toward the centre, and if even that fails, vanilla's own obsidian platform, rebuilt.
     */
    public static BlockPos endFallback(ServerLevel level) {
        BlockPos spawn = ServerLevel.END_SPAWN_POINT;
        // vanilla puts every arrival here, so its platform counts whatever the edge margin says
        if (level.getBlockState(spawn.below()).isFaceSturdy(level, spawn.below(), Direction.UP) && fits(level, spawn)) return spawn;
        for (int x = spawn.getX(); x >= 16; x -= 4) {
            int top = columnTop(level, x, spawn.getZ());
            BlockPos feet = new BlockPos(x, top, spawn.getZ());
            if (top > level.getMinY() && isSafeGround(level, feet) && fits(level, feet)) return feet;
        }
        EndPlatformFeature.createEndPlatform(level, spawn, true); // the feet position: obsidian goes one below it
        return spawn;
    }

    /** The exact landing point on a spot: on top of a bottom slab or path, not inside it. */
    public static Vec3 landing(ServerLevel level, BlockPos feet) {
        var shape = level.getBlockState(feet).getCollisionShape(level, feet);
        double y = feet.getY() + (shape.isEmpty() ? 0 : shape.max(Direction.Axis.Y));
        return new Vec3(feet.getX() + 0.5, y, feet.getZ() + 0.5);
    }

    /** Remembers a spot directly (the GameTests). */
    public static void setFooting(ServerPlayer player, GlobalPos pos) {
        FOOTING.put(player.getUUID(), pos);
    }

    /** Whether the falling cue has played during this fall (the GameTests). */
    public static boolean cued(ServerPlayer player) {
        return CUED.containsKey(player.getUUID());
    }

    public static GlobalPos footing(ServerPlayer player) {
        return FOOTING.get(player.getUUID());
    }

    public static void forget(ServerPlayer player) {
        FOOTING.remove(player.getUUID());
        WARNED.remove(player.getUUID());
        CUED.remove(player.getUUID());
    }

    /** Pulls the player back if they carry an intact anchor; returns whether they were. */
    public static boolean rescue(ServerPlayer player) {
        int slot = find(player, true);
        if (slot < 0) {
            if (find(player, false) >= 0 && WARNED.putIfAbsent(player.getUUID(), true) == null) {
                player.sendSystemMessage(Component.translatable("message.promisedend.anchor_cracked"), true);
            }
            return false;
        }
        var server = player.level().getServer();
        ServerLevel from = player.level();
        Vec3 fromPos = player.position();
        GlobalPos target = FOOTING.get(player.getUUID());
        // footing from another dimension doesn't count, and footing that no longer holds (mined out from under) is
        // replaced by the nearest spot that does. With neither, the End has its own fallbacks; anywhere else it's the
        // world spawn, as with a bed
        BlockPos near = target == null || target.dimension() != from.dimension() ? null : landingNear(from, target.pos());
        ServerLevel level;
        Vec3 to;
        if (near != null) {
            level = from;
            to = landing(from, near);
        } else if (from.dimension() == Level.END) {
            level = from;
            to = landing(from, endFallback(from));
        } else {
            var spawn = server.overworld().getRespawnData();
            level = server.getLevel(spawn.dimension());
            if (level == null) level = server.overworld();
            to = Vec3.atBottomCenterOf(level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn.pos()));
        }
        player.teleport(new TeleportTransition(level, to, Vec3.ZERO, player.getYRot(), player.getXRot(), TeleportTransition.DO_NOTHING));
        player.resetFallDistance();
        // the icon shows, but no swirl particles: in first person they pass the camera and cover the landing
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, SLOW_FALLING_TICKS, 0, false, false, true));
        player.getInventory().setItem(slot, new ItemStack(ModItems.CRACKED_VOID_ANCHOR.get()));
        level.playSound(null, to.x, to.y, to.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.7F);
        // and the crack: a low item break under a crystal shattering
        level.playSound(null, to.x, to.y, to.z, SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 0.8F, 0.8F);
        level.playSound(null, to.x, to.y, to.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 0.9F, 0.7F);
        // where they left the void and where they landed, so the pull is seen as well as heard
        from.sendParticles(ParticleTypes.REVERSE_PORTAL, fromPos.x, fromPos.y + 1, fromPos.z, 60, 0.4, 0.8, 0.4, 0.05);
        // a ring of rising motes around the landing spot, far enough out to be seen in first person (a particle that
        // passes the camera fills the screen). Forced: in the tick of a long teleport the client's camera is still at
        // the old spot and drops them as too far away
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24;
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, true, true, to.x + Math.cos(a) * 1.6, to.y + 0.3, to.z + Math.sin(a) * 1.6, 4, 0.08, 0.5, 0.08, 0.02);
        }
        CUED.remove(player.getUUID());
        player.sendSystemMessage(Component.translatable("message.promisedend.anchor_rescued"), true);
        var advancement = server.getAdvancements().get(CodexBook.SAVED_BY_ANCHOR);
        if (advancement != null) player.getAdvancements().award(advancement, "rescued");
        return true;
    }

    /** The inventory slot of an intact (or, with {@code intact} false, a cracked) anchor, or -1. */
    private static int find(ServerPlayer player, boolean intact) {
        var item = intact ? ModItems.VOID_ANCHOR.get() : ModItems.CRACKED_VOID_ANCHOR.get();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(item)) return i;
        }
        return -1;
    }
}

package com.panda.promisedend.entity;

import com.panda.promisedend.logic.DriftRoute;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * A Drift Islet: a 3x3 raft of end stone that shuttles between two moorings across the void, carrying whoever stands
 * on it. Players and mobs stand on it as on a block (it is a collision box, as a boat is). Its place on the route is
 * stepped once a tick the same way on the server and on each client, from a state the server sends when it turns and
 * once a second, so a rider's client moves its player by exactly what the islet moved. Something in its way (a block)
 * turns it back. It is saved with its route and never despawns.
 */
public class DriftIslet extends Entity {
    private static final EntityDataAccessor<Vector3fc> DATA_FROM = SynchedEntityData.defineId(DriftIslet.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3fc> DATA_TO = SynchedEntityData.defineId(DriftIslet.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Float> DATA_S = SynchedEntityData.defineId(DriftIslet.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> DATA_DIR = SynchedEntityData.defineId(DriftIslet.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> DATA_WAIT = SynchedEntityData.defineId(DriftIslet.class, EntityDataSerializers.INT);
    /** How often the server sends its state anyway, against drift. */
    private static final int RESYNC = 20;
    /**
     * A client that hasn't heard from the server this long stops stepping the islet: the server has stopped ticking it
     * (no player within simulation distance), and a client copy that went on would carry a rider somewhere it isn't.
     */
    private static final int SILENT = RESYNC * 2;
    /** How long it stands after something in its way turned it back, so a way blocked both sides doesn't flap. */
    public static final int TURN_PAUSE = 20;
    /** How many ticks before it leaves a mooring the first chime sounds. */
    private static final int DEPART_WARNING = 20;
    /** How high over its top what it carries may be: a jump's height, so a rider who jumps comes down on it. */
    private static final double CARRY_HEIGHT = 1.5;
    /** The room over its top a rider needs: something there turns it back, as a block in its own way does. */
    private static final double HEADROOM = 2.0;

    private DriftRoute route;
    private final DriftRoute.State state = DriftRoute.State.start();
    private int turns;
    /** On a client, the tick it last heard the server's state. */
    private int heard;
    /** Whether its way has been cleared, once, after worldgen was done with the chunks along it. */
    private boolean clear;
    /**
     * The players on it since it left its mooring, each with how many ticks running it has missed them: those still
     * aboard at the other have crossed. A player is dropped only after a second unseen (a client's place reaches the
     * server a tick or two late, more on a climbing route).
     */
    private final java.util.Map<java.util.UUID, Integer> aboard = new java.util.HashMap<>();
    private static final int MISSED = 20;
    /** A player told once what a mooring is for. */
    public static final String MOORING_HINTED = "promisedend.hinted_mooring";
    /** A lilac mote under its keel this often while it moves (client side): an islet mid-gap shows against the void. */
    private static final int TRAIL_EVERY = 3;
    private static final net.minecraft.core.particles.DustParticleOptions KEEL = new net.minecraft.core.particles.DustParticleOptions(0xE9DDF5, 1.2F);
    private static final net.minecraft.resources.Identifier CROSSED = com.panda.promisedend.PromisedEnd.id("outer/ride_drift");

    public DriftIslet(EntityType<? extends DriftIslet> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.blocksBuilding = true;
        setNoGravity(true);
    }

    /** Puts the islet on a route (its ends the islet's bottom middle at each mooring), where {@code at} says. */
    public void setRoute(DriftRoute route, DriftRoute.State at) {
        this.route = route;
        this.state.s = at.s;
        this.state.dir = at.dir;
        this.state.wait = at.wait;
        place();
        if (!level().isClientSide()) send(true);
    }

    public DriftRoute route() {
        return route;
    }

    public DriftRoute.State state() {
        return state;
    }

    /** Whether it has cleared its way (tests). */
    public boolean cleared() {
        return clear;
    }

    /** How many times something in its way turned it back (tests). */
    public int turns() {
        return turns;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_FROM, new Vector3f());
        builder.define(DATA_TO, new Vector3f());
        builder.define(DATA_S, 0.0F);
        builder.define(DATA_DIR, (byte) 1);
        builder.define(DATA_WAIT, DriftRoute.WAIT);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (!level().isClientSide()) return;
        this.heard = this.tickCount;
        // only what changed is sent, so each value is read on its own
        if (key == DATA_FROM || key == DATA_TO) {
            Vector3fc a = this.entityData.get(DATA_FROM), b = this.entityData.get(DATA_TO);
            this.route = new DriftRoute(a.x(), a.y(), a.z(), b.x(), b.y(), b.z());
        } else if (key == DATA_S) {
            this.state.s = this.entityData.get(DATA_S);
        } else if (key == DATA_DIR) {
            this.state.dir = this.entityData.get(DATA_DIR);
        } else if (key == DATA_WAIT) {
            this.state.wait = this.entityData.get(DATA_WAIT);
        }
    }

    private void send(boolean route) {
        if (route) {
            this.entityData.set(DATA_FROM, new Vector3f((float) this.route.ax(), (float) this.route.ay(), (float) this.route.az()));
            this.entityData.set(DATA_TO, new Vector3f((float) this.route.bx(), (float) this.route.by(), (float) this.route.bz()));
        }
        this.entityData.set(DATA_S, (float) state.s);
        this.entityData.set(DATA_DIR, (byte) state.dir);
        this.entityData.set(DATA_WAIT, state.wait);
    }

    private void place() {
        double[] p = route.at(state.s);
        setPos(p[0], p[1], p[2]);
    }

    @Override
    public void tick() {
        super.tick();
        if (route == null || route.length() < 1e-6) return;
        boolean server = !level().isClientSide();
        if (!clear && server && tickCount % 20 == 1) clearWay();
        if (!server && tickCount - heard > SILENT) return;
        if (!server && state.wait == 0 && tickCount % TRAIL_EVERY == 0) {
            level().addParticle(KEEL, getX() + (random.nextDouble() - 0.5) * 2.4, getY() - 0.2, getZ() + (random.nextDouble() - 0.5) * 2.4, 0, -0.02, 0);
        }
        AABB before = getBoundingBox();
        Vec3 from = position();
        List<Entity> riders = riders(before);
        boolean turned = false;
        int waited = state.wait;
        if (server && state.wait == 0 && blocked()) {
            DriftRoute.turn(state);
            state.wait = TURN_PAUSE;
            turns++;
            turned = true;
        }
        route.step(state);
        place();
        Vec3 delta = position().subtract(from);
        if (delta.lengthSqr() > 0) carry(riders, delta);
        lift();
        if (server) {
            for (Entity e : riders) {
                // a player stands on an entity, not a block: the server would count them floating and kick them
                if (e instanceof ServerPlayer player) {
                    player.connection.resetFlyingTicks();
                    player.resetFallDistance();
                }
            }
            // a chime a second before it leaves a mooring, and a higher one as it leaves; at the first, the mooring's
            // posts throw sparks, so the one about to go is seen as well as heard
            if (!turned && (waited == DEPART_WARNING + 1 && state.wait == DEPART_WARNING || waited == 1 && state.wait == 0)) {
                level().playSound(null, getX(), getY() + 1, getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 1.0F, state.wait == 0 ? 1.2F : 0.8F);
                if (state.wait == DEPART_WARNING) sparkPosts();
            }
            if (tickCount % 20 == 7) hintMoorings();
            if (turned || tickCount % RESYNC == 0) send(false);
            crossings(waited, turned);
        }
    }

    /** The two lit posts of the mooring it stands at throw a burst of sparks. */
    private void sparkPosts() {
        if (!(level() instanceof ServerLevel level) || (state.s > 0 && state.s < 1)) return;
        var gap = com.panda.promisedend.logic.DriftGap.Gap.of(route);
        boolean first = state.s <= 0;
        int ex = first ? gap.ex() : gap.fx(), ez = first ? gap.ez() : gap.fz(), top = (first ? gap.he() : gap.hf()) + 2;
        for (int l : new int[] {-com.panda.promisedend.logic.DriftGap.CORRIDOR, com.panda.promisedend.logic.DriftGap.CORRIDOR}) {
            double x = ex + 0.5 + (gap.dz() != 0 ? l : 0), z = ez + 0.5 + (gap.dx() != 0 ? l : 0);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, x, top + 0.7, z, 12, 0.1, 0.2, 0.1, 0.06);
        }
    }

    /**
     * The first time a player waits on one of its moorings while it is away, a line in the action bar says what the dock
     * is for, once.
     */
    private void hintMoorings() {
        var gap = com.panda.promisedend.logic.DriftGap.Gap.of(route);
        for (int[] end : new int[][] {{gap.ex(), gap.ez(), gap.he()}, {gap.fx(), gap.fz(), gap.hf()}}) {
            AABB dock = new AABB(end[0] - 2, end[2] - 0.5, end[1] - 2, end[0] + 3, end[2] + 2, end[1] + 3);
            if (getBoundingBox().inflate(1).intersects(dock)) continue;
            for (ServerPlayer p : level().getEntitiesOfClass(ServerPlayer.class, dock, p -> !p.isSpectator() && !p.entityTags().contains(MOORING_HINTED))) {
                p.addTag(MOORING_HINTED);
                p.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("hint.promisedend.drift_islet.mooring"));
            }
        }
    }

    /**
     * Who rode it from one mooring to the other, without stepping off or being turned back, has crossed. Counted from a
     * looser box than {@link #riders}: half a block round its top and from just under it, as a lagging client places
     * its player.
     */
    private void crossings(int waited, boolean turned) {
        if (turned) {
            aboard.clear();
            return;
        }
        AABB box = getBoundingBox();
        List<ServerPlayer> near = level().getEntitiesOfClass(ServerPlayer.class,
                new AABB(box.minX - 0.5, box.maxY - 0.75, box.minZ - 0.5, box.maxX + 0.5, box.maxY + CARRY_HEIGHT + 0.5, box.maxZ + 0.5),
                p -> !p.isSpectator() && !p.getAbilities().flying);
        if (waited == 1 && state.wait == 0 && (state.s <= 0 || state.s >= 1 || route.length() * Math.min(state.s, 1 - state.s) <= DriftRoute.SPEED)) {
            // leaving a mooring (not standing again after a turn mid-route)
            aboard.clear();
            for (ServerPlayer p : near) aboard.put(p.getUUID(), 0);
            return;
        }
        if (aboard.isEmpty()) return;
        aboard.replaceAll((id, missed) -> near.stream().anyMatch(p -> p.getUUID().equals(id)) ? 0 : missed + 1);
        aboard.values().removeIf(missed -> missed > MISSED);
        if (waited != 0 || state.wait == 0) return;
        var crossed = level().getServer().getAdvancements().get(CROSSED);
        for (ServerPlayer p : near) {
            if (crossed != null && aboard.containsKey(p.getUUID())) p.getAdvancements().award(crossed, "crossed");
        }
        aboard.clear();
    }

    /**
     * What stands on it, or is in the air just over it (a rider mid-jump): not passengers (they go with their mount),
     * not what passes through blocks, not spectators or flying players.
     */
    private List<Entity> riders(AABB box) {
        AABB top = new AABB(box.minX, box.maxY - 0.01, box.minZ, box.maxX, box.maxY + CARRY_HEIGHT, box.maxZ);
        return level().getEntities(this, top, e -> !e.isPassenger() && !e.noPhysics && !e.isSpectator()
                && !(e instanceof Player p && p.getAbilities().flying) && e.getBoundingBox().minY >= box.maxY - 0.05);
    }

    /**
     * Lifts onto its top what is sunk into it from above: a rider whose client had them before the islet (a relog, a
     * chunk loading in), or a mob loaded before it, would otherwise fall through it.
     */
    private void lift() {
        AABB box = getBoundingBox();
        for (Entity e : level().getEntities(this, box.deflate(0.01), e -> !e.isPassenger() && !e.noPhysics && !e.isSpectator())) {
            AABB at = e.getBoundingBox();
            if (at.minY < box.minY + 0.4 || at.minY >= box.maxY) continue;
            boolean mine = level().isClientSide() ? e.isLocalInstanceAuthoritative() : !(e instanceof Player);
            if (!mine) continue;
            e.setPos(e.getX(), box.maxY, e.getZ());
            if (e.getDeltaMovement().y < 0) e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0, 1));
        }
    }

    /**
     * Clears its way, once: what worldgen grew into the room it sweeps, its own block and the two over it a rider stands
     * in, slice by slice along its route, and mends its two moorings (a feature in a chunk next door can reach in after
     * the route's own pass, into its way or over a post). Takes only what the lands grow
     * ({@code #promisedend:drift_clearable}), never a block entity: a player may have built across the gap before the
     * far end loaded, and their bridge stays (the islet turns back at it, as at any block in its way). Leaves other
     * structures alone (an End City's ship). Waits until every chunk along the way and at its moorings is loaded.
     * Returns the blocks cleared, or -1 when it has to wait.
     */
    public int clearWay() {
        double length = route.length();
        for (double t = 0; t <= 1.0001; t += 8 / Math.max(8, length)) {
            double[] p = route.at(Math.min(1, t));
            if (!level().hasChunkAt(BlockPos.containing(p[0], p[1], p[2]))) return -1;
        }
        var gap = com.panda.promisedend.logic.DriftGap.Gap.of(route);
        for (int[] end : new int[][] {{gap.ex(), gap.ez()}, {gap.fx(), gap.fz()}}) {
            for (int l = -com.panda.promisedend.logic.DriftGap.CORRIDOR; l <= com.panda.promisedend.logic.DriftGap.CORRIDOR; l += 2 * com.panda.promisedend.logic.DriftGap.CORRIDOR) {
                if (!level().hasChunkAt(new BlockPos(end[0] + (gap.dz() != 0 ? l : 0), 0, end[1] + (gap.dx() != 0 ? l : 0)))) return -1;
            }
        }
        int cleared = 0;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        java.util.Map<Long, Boolean> claimed = new java.util.HashMap<>();
        for (double d = 0; d <= length + 0.01; d += 0.5) {
            double[] p = route.at(Math.min(1, d / Math.max(length, 1e-6)));
            AABB slice = new AABB(p[0] - 1.5, p[1], p[2] - 1.5, p[0] + 1.5, p[1] + 3, p[2] + 1.5).deflate(0.01);
            for (int x = (int) Math.floor(slice.minX); x <= (int) Math.floor(slice.maxX); x++) {
                for (int z = (int) Math.floor(slice.minZ); z <= (int) Math.floor(slice.maxZ); z++) {
                    int cx = x, cz = z, cy = (int) Math.floor(p[1]);
                    boolean other = level() instanceof ServerLevel server && claimed.computeIfAbsent(BlockPos.asLong(x, 0, z),
                            k -> server.structureManager().getStructureWithPieceAt(new BlockPos(cx, cy, cz),
                                    h -> !h.is(com.panda.promisedend.worldgen.DriftRouteStructure.KEY)).isValid());
                    if (other) continue;
                    for (int y = (int) Math.floor(slice.minY); y <= (int) Math.floor(slice.maxY); y++) {
                        var state = level().getBlockState(at.set(x, y, z));
                        if (!state.isAir() && state.is(com.panda.promisedend.registry.ModTags.DRIFT_CLEARABLE) && !state.hasBlockEntity()) {
                            level().setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 2);
                            cleared++;
                        }
                    }
                }
            }
        }
        com.panda.promisedend.worldgen.DriftRouteStructure.Piece.moor(level(), gap, p -> true);
        clear = true;
        return cleared;
    }

    /**
     * Whether the next step would run its box, or the room a rider stands in over it, into a block (a block at head
     * height would scrape a rider off). The step is cut at the route's end, where the islet lies against its mooring's
     * island (a full step's look there reached into the island, and it turned back short of it).
     */
    private boolean blocked() {
        double length = route.length();
        Vec3 way = new Vec3(route.bx() - route.ax(), route.by() - route.ay(), route.bz() - route.az()).scale(state.dir / length);
        double left = (state.dir > 0 ? 1 - state.s : state.s) * length;
        AABB box = getBoundingBox();
        AABB next = box.setMaxY(box.maxY + HEADROOM).deflate(0.05).move(way.scale(Math.min(DriftRoute.SPEED, left)));
        return !level().noBlockCollision(this, next);
    }

    /**
     * Moves its riders by what it moved: on the server, every mob and item; on a client, its own player (whose movement
     * the client owns, as with a shulker box's lid). Passengers go with their mount.
     */
    private void carry(List<Entity> riders, Vec3 delta) {
        for (Entity e : riders) {
            boolean mine = level().isClientSide() ? e.isLocalInstanceAuthoritative() : !(e instanceof Player);
            if (mine) e.move(MoverType.SHULKER_BOX, delta);
        }
    }

    @Override
    public boolean canBeCollidedWith(Entity other) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        DriftRoute r = new DriftRoute(input.getDoubleOr("from_x", 0), input.getDoubleOr("from_y", 0), input.getDoubleOr("from_z", 0),
                input.getDoubleOr("to_x", 0), input.getDoubleOr("to_y", 0), input.getDoubleOr("to_z", 0));
        setRoute(r, new DriftRoute.State(input.getDoubleOr("along", 0), input.getByteOr("dir", (byte) 1) < 0 ? -1 : 1, input.getIntOr("wait", DriftRoute.WAIT)));
        this.clear = input.getBooleanOr("cleared", false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (route == null) return;
        output.putDouble("from_x", route.ax());
        output.putDouble("from_y", route.ay());
        output.putDouble("from_z", route.az());
        output.putDouble("to_x", route.bx());
        output.putDouble("to_y", route.by());
        output.putDouble("to_z", route.bz());
        output.putDouble("along", state.s);
        output.putByte("dir", (byte) state.dir);
        output.putInt("wait", state.wait);
        output.putBoolean("cleared", clear);
    }
}

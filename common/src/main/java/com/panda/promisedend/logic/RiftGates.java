package com.panda.promisedend.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.block.RiftGateBlock;
import com.panda.promisedend.network.RiftPayloads;
import com.panda.promisedend.platform.Services;
import com.panda.promisedend.registry.ModBlocks;
import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;

/**
 * The Rift Gates on the server: each player's network ({@link RiftNetwork}, saved), attuning a gate to it, opening
 * the screen on it, and the journey, which the server checks end to end: the player stands at an attuned gate of
 * theirs, the other is in their network, is still a gate, isn't behind the Ward while it stands, has room on its
 * stepping side, and the wait is over.
 */
public final class RiftGates {
    /** How near the gate they leave from a player must stand. */
    public static final double REACH = 6;

    private RiftGates() {}

    private static String level(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    /** The player's network, live: changes to it are saved. */
    public static List<RiftNetwork.Gate> network(ServerPlayer player) {
        return Stored.of(player.level().getServer()).gates.computeIfAbsent(player.getUUID(), u -> new ArrayList<>());
    }

    public static boolean attuned(ServerPlayer player, BlockPos gate) {
        return RiftNetwork.find(network(player), level(player.level()), gate.asLong()) != null;
    }

    /** The two things that attune a gate, one of each from the player's inventory. */
    public static boolean canAttune(ServerPlayer player) {
        return player.isCreative() || (player.getInventory().countItem(ModBlocks.MIRE_LILY.get().asItem()) > 0
                && player.getInventory().countItem(ModItems.RESONITE_SHARD.get()) > 0);
    }

    /** Attunes the gate at {@code pos} to the player's network, for a Mire Lily and a Resonite Shard. */
    public static boolean attune(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.level();
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.RIFT_GATE.get())) return false;
        if (!canAttune(player)) {
            // a lit gate is someone else's: it asks the same, but doesn't say it sleeps
            player.sendOverlayMessage(Component.translatable(state.getValue(RiftGateBlock.LIT) ? "message.promisedend.rift_gate.other" : "message.promisedend.rift_gate.dormant"));
            return false;
        }
        String biome = level.getBiome(pos).unwrapKey().map(k -> k.identifier().toString()).orElse("minecraft:the_end");
        // no name of its own: the screen calls it by its land, in the reader's language
        var gate = new RiftNetwork.Gate(level(level), pos.asLong(), "", biome);
        var added = RiftNetwork.add(network(player), gate);
        if (added == RiftNetwork.Added.FULL) {
            player.sendOverlayMessage(Component.translatable("message.promisedend.rift_gate.full", RiftNetwork.MOST));
            return false;
        }
        if (added == RiftNetwork.Added.KNOWN) return true;
        if (!player.isCreative()) {
            takeOne(player, ModBlocks.MIRE_LILY.get().asItem());
            takeOne(player, ModItems.RESONITE_SHARD.get());
        }
        Stored.of(level.getServer()).setDirty();
        if (!state.getValue(RiftGateBlock.LIT)) level.setBlockAndUpdate(pos, state.setValue(RiftGateBlock.LIT, true));
        level.playSound(null, pos, ModSounds.RIFT_GATE_ATTUNE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 60, 0.6, 0.9, 0.6, 0.08);
        player.sendOverlayMessage(Component.translatable("message.promisedend.rift_gate.attuned", network(player).size(), RiftNetwork.MOST));
        return true;
    }

    private static void takeOne(ServerPlayer player, net.minecraft.world.item.Item item) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(item)) {
                inventory.getItem(i).shrink(1);
                inventory.setChanged();
                return;
            }
        }
    }

    /** Renames the player's entry for the gate at {@code pos}; the message gives the name as kept (trimmed, capped). */
    public static boolean rename(ServerPlayer player, BlockPos pos, String name) {
        String in = level(player.level());
        boolean done = RiftNetwork.rename(network(player), in, pos.asLong(), name);
        if (done) {
            Stored.of(player.level().getServer()).setDirty();
            player.sendOverlayMessage(Component.translatable("message.promisedend.rift_gate.renamed", RiftNetwork.find(network(player), in, pos.asLong()).name()));
        }
        return done;
    }

    /** Lets the gate at {@code pos} go from the player's network (a sneaking use): room for another. It stays lit. */
    public static boolean release(ServerPlayer player, BlockPos pos) {
        boolean done = RiftNetwork.remove(network(player), level(player.level()), pos.asLong());
        if (done) {
            Stored.of(player.level().getServer()).setDirty();
            player.sendOverlayMessage(Component.translatable("message.promisedend.rift_gate.released", network(player).size(), RiftNetwork.MOST));
        }
        return done;
    }

    /** Opens the screen at the gate at {@code here}: the player's gates in this level. */
    public static void open(ServerPlayer player, BlockPos here) {
        String in = level(player.level());
        List<RiftPayloads.Shown> shown = new ArrayList<>();
        for (var g : network(player)) if (g.level().equals(in)) shown.add(new RiftPayloads.Shown(BlockPos.of(g.pos()), g.name(), g.biome()));
        Services.PLATFORM.sendToPlayer(player, new RiftPayloads.Gates(here, shown));
    }

    public enum Journey { WENT, NOT_AT_A_GATE, NOT_IN_NETWORK, GONE, WARDED, WAITING, BLOCKED, SAME }

    /**
     * Where a traveller comes out of the gate at {@code gate}: on its floor row a step out on its facing side, or, if
     * that is taken, two out, or a step to either side of those; null when none has room for a player and a floor.
     */
    public static BlockPos standing(ServerLevel level, BlockPos gate, net.minecraft.core.Direction facing) {
        BlockPos near = gate.below().relative(facing);
        var side = facing.getClockWise();
        for (BlockPos p : List.of(near, near.relative(facing), near.relative(side), near.relative(side.getOpposite()),
                near.relative(facing).relative(side), near.relative(facing).relative(side.getOpposite()))) {
            if (roomAt(level, p)) return p;
        }
        return null;
    }

    private static boolean roomAt(ServerLevel level, BlockPos p) {
        for (BlockPos q : List.of(p, p.above())) {
            BlockState s = level.getBlockState(q);
            if (!s.getCollisionShape(level, q).isEmpty() || !s.getFluidState().isEmpty() || s.is(net.minecraft.tags.BlockTags.FIRE)) return false;
        }
        return level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP);
    }

    /** The payload's way in. */
    public static void travelled(ServerPlayer player, RiftPayloads.Travel travel) {
        travel(player, travel.from(), travel.to());
    }

    /** Sends the player from the gate at {@code from} to the one at {@code to}, if every check holds. */
    public static Journey travel(ServerPlayer player, BlockPos from, BlockPos to) {
        ServerLevel level = player.level();
        String in = level(level);
        var gates = network(player);
        if (from.equals(to)) return Journey.SAME;
        if (RiftNetwork.find(gates, in, from.asLong()) == null || !level.isLoaded(from) || !level.getBlockState(from).is(ModBlocks.RIFT_GATE.get())
                || player.distanceToSqr(Vec3.atCenterOf(from)) > REACH * REACH) {
            player.sendOverlayMessage(Component.translatable("message.promisedend.rift_gate.away"));
            return Journey.NOT_AT_A_GATE;
        }
        if (RiftNetwork.find(gates, in, to.asLong()) == null) return Journey.NOT_IN_NETWORK;
        long last = Stored.of(level.getServer()).last.getOrDefault(player.getUUID(), -1L);
        long wait = RiftNetwork.waitLeft(last, level.getGameTime());
        if (wait > 0 && !player.isCreative()) {
            player.sendOverlayMessage(Component.translatable("message.promisedend.rift_gate.waiting", (wait + 19) / 20));
            return Journey.WAITING;
        }
        if (AncientWard.stands(level.getServer()) && level.dimension() == net.minecraft.world.level.Level.END
                && AncientWard.inside(to.getX() + 0.5, to.getZ() + 0.5, EndConfig.wardRadius())) {
            player.sendOverlayMessage(Component.translatable("message.promisedend.rift_gate.warded"));
            return Journey.WARDED;
        }
        // the far gate's chunk is loaded to look at it: a gate broken (by a creative player) or gone leaves the network
        BlockState far = level.getChunk(to.getX() >> 4, to.getZ() >> 4).getBlockState(to);
        if (!far.is(ModBlocks.RIFT_GATE.get())) {
            RiftNetwork.remove(gates, in, to.asLong());
            Stored.of(level.getServer()).setDirty();
            player.sendOverlayMessage(Component.translatable("message.promisedend.rift_gate.gone"));
            return Journey.GONE;
        }
        var facing = far.getValue(RiftGateBlock.FACING);
        BlockPos stand = standing(level, to, facing);
        if (stand == null) {
            player.sendOverlayMessage(Component.translatable("message.promisedend.rift_gate.blocked"));
            return Journey.BLOCKED;
        }
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
        level.playSound(null, player.blockPosition(), ModSounds.RIFT_GATE_TRAVEL.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        player.teleportTo(level, stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, Set.of(), facing.toYRot(), 0, true);
        player.resetFallDistance();
        level.playSound(null, stand, ModSounds.RIFT_GATE_TRAVEL.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, stand.getX() + 0.5, stand.getY() + 1, stand.getZ() + 0.5, 40, 0.4, 0.8, 0.4, 0.1);
        Stored.of(level.getServer()).last.put(player.getUUID(), level.getGameTime());
        Stored.of(level.getServer()).setDirty();
        var travelled = level.getServer().getAdvancements().get(CodexBook.GATE_TRAVEL);
        if (travelled != null) player.getAdvancements().award(travelled, "travelled");
        return Journey.WENT;
    }

    /** Forgets the player's wait (tests). */
    public static void rest(ServerPlayer player) {
        Stored.of(player.level().getServer()).last.remove(player.getUUID());
    }

    /** Forgets the player's network and wait (tests). */
    public static void forget(ServerPlayer player) {
        Stored s = Stored.of(player.level().getServer());
        s.gates.remove(player.getUUID());
        s.last.remove(player.getUUID());
        s.setDirty();
    }

    /** Every player's network and when each last went through. */
    private static final class Stored extends SavedData {
        static final Codec<RiftNetwork.Gate> GATE = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("level").forGetter(RiftNetwork.Gate::level), Codec.LONG.fieldOf("pos").forGetter(RiftNetwork.Gate::pos),
                Codec.STRING.fieldOf("name").forGetter(RiftNetwork.Gate::name), Codec.STRING.fieldOf("biome").forGetter(RiftNetwork.Gate::biome)).apply(i, RiftNetwork.Gate::new));
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, GATE.listOf()).optionalFieldOf("gates", Map.of()).forGetter(s -> s.gates),
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.LONG).optionalFieldOf("last", Map.of()).forGetter(s -> s.last)).apply(i, Stored::new));
        static final SavedDataType<Stored> TYPE = new SavedDataType<>(PromisedEnd.id("rift_gates"), Stored::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

        final Map<UUID, List<RiftNetwork.Gate>> gates = new HashMap<>();
        final Map<UUID, Long> last = new HashMap<>();

        Stored() {}

        Stored(Map<UUID, List<RiftNetwork.Gate>> gates, Map<UUID, Long> last) {
            gates.forEach((k, v) -> this.gates.put(k, new ArrayList<>(v)));
            this.last.putAll(last);
        }

        static Stored of(MinecraftServer server) {
            return server.getDataStorage().computeIfAbsent(TYPE);
        }
    }

    /** Whether a held stack is a name tag with a name, as a gate is renamed. */
    public static boolean isNameTag(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.NAME_TAG) && stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
    }
}

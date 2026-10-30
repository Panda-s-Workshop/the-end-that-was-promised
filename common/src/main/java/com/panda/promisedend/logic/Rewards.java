package com.panda.promisedend.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.PromisedEnd;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * A guardian's rewards for each who fought it or watched it fall: given at once to one alive and online, else kept for
 * them by name and given the next time they are both (a fighter on the death screen at the kill, or logged out), so a
 * Fin or a Lattice, one a place, isn't lost to a respawn.
 */
public final class Rewards {
    /** The guardians whose rewards can be owed, saved by name, each with its entity's id and how it gives its reward. */
    public enum Guardian {
        SENTINEL("hollow_sentinel", com.panda.promisedend.entity.HollowSentinel::reward),
        LEVIATHAN("void_leviathan", com.panda.promisedend.entity.VoidLeviathan::reward),
        ARCHITECT("the_architect", com.panda.promisedend.entity.TheArchitect::reward);

        final String entity;
        final Consumer<ServerPlayer> give;

        Guardian(String entity, Consumer<ServerPlayer> give) {
            this.entity = entity;
            this.give = give;
        }
    }

    private static final int EVERY = 20;

    private Rewards() {}

    /** Rewards each of {@code who} for {@code guardian}: now, or when they are next alive and online. */
    public static void grant(MinecraftServer server, Collection<UUID> who, Guardian guardian) {
        for (UUID id : who) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null && p.isAlive()) give(p, guardian);
            else owe(server, id, guardian);
        }
    }

    /** Each second, for each player: what they are owed, given once they're alive. */
    public static void tick(ServerPlayer player) {
        if (player.tickCount % EVERY != 0 || !player.isAlive()) return;
        Stored s = Stored.of(player.level().getServer());
        List<String> owed = s.owed.remove(player.getUUID());
        if (owed == null) return;
        s.setDirty();
        for (String name : owed) {
            Guardian guardian = Guardian.valueOf(name);
            give(player, guardian);
            player.sendSystemMessage(Component.translatable("message.promisedend.reward_kept", Component.translatable("entity.promisedend." + guardian.entity)));
        }
    }

    /** What {@code id} is owed (GameTests). */
    public static List<String> owed(MinecraftServer server, UUID id) {
        return List.copyOf(Stored.of(server).owed.getOrDefault(id, List.of()));
    }

    private static void owe(MinecraftServer server, UUID id, Guardian guardian) {
        Stored s = Stored.of(server);
        s.owed.computeIfAbsent(id, k -> new ArrayList<>()).add(guardian.name());
        s.setDirty();
        PromisedEnd.LOGGER.info("[promisedend] {}'s reward kept for {}, not alive or online at its fall", guardian, id);
    }

    private static void give(ServerPlayer p, Guardian guardian) {
        guardian.give.accept(p);
    }

    private static final class Stored extends SavedData {
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.STRING.listOf()).optionalFieldOf("owed", Map.of()).forGetter(s -> s.owed)).apply(i, Stored::new));
        static final SavedDataType<Stored> TYPE = new SavedDataType<>(PromisedEnd.id("rewards"), Stored::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

        final Map<UUID, List<String>> owed = new HashMap<>();

        Stored() {}

        Stored(Map<UUID, List<String>> owed) {
            owed.forEach((k, v) -> this.owed.put(k, new ArrayList<>(v)));
        }

        static Stored of(MinecraftServer server) {
            return server.getDataStorage().computeIfAbsent(TYPE);
        }
    }
}

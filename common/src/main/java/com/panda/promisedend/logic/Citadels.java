package com.panda.promisedend.logic;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.PromisedEnd;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * What the world remembers of its Unfinished Citadels: each Amber Gate block a key opened, so an Architect not yet
 * loaded when its gate was opened unbinds once it is; and where each Architect fell, so its gate takes no key again.
 */
public final class Citadels {
    private Citadels() {}

    /** A gate at {@code gate} was opened by a key. */
    public static void opened(MinecraftServer server, BlockPos gate) {
        Stored s = Stored.of(server);
        if (!s.opened.contains(gate)) {
            s.opened.add(gate.immutable());
            s.setDirty();
        }
    }

    /** An Architect resting at {@code home} fell. */
    public static void fell(MinecraftServer server, BlockPos home) {
        Stored s = Stored.of(server);
        if (!s.fallen.contains(home)) {
            s.fallen.add(home.immutable());
            s.setDirty();
        }
    }

    /** Forgets every gate and fall (GameTests that need a world with neither). */
    public static void clear(MinecraftServer server) {
        Stored s = Stored.of(server);
        s.opened.clear();
        s.fallen.clear();
        s.setDirty();
    }

    /** Whether a key opened a gate within {@code reach} of {@code at}. */
    public static boolean openedNear(MinecraftServer server, BlockPos at, double reach) {
        return near(Stored.of(server).opened, at, reach);
    }

    /** Whether an Architect fell within {@code reach} of {@code at}. */
    public static boolean fallenNear(MinecraftServer server, BlockPos at, double reach) {
        return near(Stored.of(server).fallen, at, reach);
    }

    private static boolean near(List<BlockPos> list, BlockPos at, double reach) {
        for (BlockPos p : list) if (p.distSqr(at) <= reach * reach) return true;
        return false;
    }

    private static final class Stored extends SavedData {
        static final Codec<Stored> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.listOf().optionalFieldOf("opened", List.of()).forGetter(s -> s.opened),
                BlockPos.CODEC.listOf().optionalFieldOf("fallen", List.of()).forGetter(s -> s.fallen)).apply(i, Stored::new));
        static final SavedDataType<Stored> TYPE = new SavedDataType<>(PromisedEnd.id("citadels"), Stored::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

        final List<BlockPos> opened = new ArrayList<>(), fallen = new ArrayList<>();

        Stored() {}

        Stored(List<BlockPos> opened, List<BlockPos> fallen) {
            this.opened.addAll(opened);
            this.fallen.addAll(fallen);
        }

        static Stored of(MinecraftServer server) {
            return server.getDataStorage().computeIfAbsent(TYPE);
        }
    }
}

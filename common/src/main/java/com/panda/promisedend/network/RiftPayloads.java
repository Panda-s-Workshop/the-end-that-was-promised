package com.panda.promisedend.network;

import com.panda.promisedend.PromisedEnd;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The Rift Gates' two payloads: the network a player sees at a gate, and the journey they pick. */
public final class RiftPayloads {
    private RiftPayloads() {}

    /** One gate as the screen shows it: where it is, its name, the biome it stands in. */
    public record Shown(BlockPos pos, String name, String biome) {
        public static final StreamCodec<RegistryFriendlyByteBuf, Shown> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Shown::pos, ByteBufCodecs.stringUtf8(64), Shown::name, ByteBufCodecs.stringUtf8(128), Shown::biome, Shown::new);
    }

    /** Server → the player at an attuned gate: the gates of their network in this level; the screen opens. */
    public record Gates(BlockPos here, List<Shown> gates) implements CustomPacketPayload {
        public static final Type<Gates> TYPE = new Type<>(PromisedEnd.id("rift_gates"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Gates> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Gates::here, Shown.STREAM_CODEC.apply(ByteBufCodecs.list(64)), Gates::gates, Gates::new);

        @Override
        public Type<Gates> type() {
            return TYPE;
        }
    }

    /** Client → server: go from the gate at {@code from} to the one at {@code to}. The server checks both. */
    public record Travel(BlockPos from, BlockPos to) implements CustomPacketPayload {
        public static final Type<Travel> TYPE = new Type<>(PromisedEnd.id("rift_travel"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Travel> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Travel::from, BlockPos.STREAM_CODEC, Travel::to, Travel::new);

        @Override
        public Type<Travel> type() {
            return TYPE;
        }
    }
}

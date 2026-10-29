package com.panda.promisedend.network;

import com.panda.promisedend.PromisedEnd;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** The Void Shrine's two payloads: the altar's offer to a player, and the boon they pick. */
public final class ShrinePayloads {
    private ShrinePayloads() {}

    /** Server → the player at an altar: the boons it offers (their indices in Offerings.BOONS); the screen opens. */
    public record Offer(BlockPos altar, List<Integer> boons) implements CustomPacketPayload {
        public static final Type<Offer> TYPE = new Type<>(PromisedEnd.id("shrine_offer"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Offer> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Offer::altar, ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(6)), Offer::boons, Offer::new);

        @Override
        public Type<Offer> type() {
            return TYPE;
        }
    }

    /** Client → server: the boon picked at an altar. The server checks it was offered and the player is there. */
    public record Choose(BlockPos altar, int boon) implements CustomPacketPayload {
        public static final Type<Choose> TYPE = new Type<>(PromisedEnd.id("shrine_choose"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Choose> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Choose::altar, ByteBufCodecs.VAR_INT, Choose::boon, Choose::new);

        @Override
        public Type<Choose> type() {
            return TYPE;
        }
    }
}

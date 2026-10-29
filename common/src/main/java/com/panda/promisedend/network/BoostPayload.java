package com.panda.promisedend.network;

import com.panda.promisedend.PromisedEnd;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client → server: the jump key pressed while gliding on the Architect's Lattice. The server checks the rest. */
public record BoostPayload() implements CustomPacketPayload {
    public static final BoostPayload INSTANCE = new BoostPayload();
    public static final Type<BoostPayload> TYPE = new Type<>(PromisedEnd.id("lattice_boost"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BoostPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<BoostPayload> type() {
        return TYPE;
    }
}

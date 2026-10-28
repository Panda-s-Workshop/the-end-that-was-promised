package com.panda.promisedend.network;

import com.panda.promisedend.PromisedEnd;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server → client, at login: whether our terrain is on. A client works out the void currents itself, and with our
 * terrain off they run under vanilla's biomes, which only the server's setting can tell it (Fabric's config is read on
 * each side from its own file; NeoForge syncs its own, so there it says the same).
 */
public record TerrainPayload(boolean terrainEnabled) implements CustomPacketPayload {
    public static final Type<TerrainPayload> TYPE = new Type<>(PromisedEnd.id("terrain"));
    public static final StreamCodec<ByteBuf, TerrainPayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(TerrainPayload::new, TerrainPayload::terrainEnabled);

    @Override
    public Type<TerrainPayload> type() {
        return TYPE;
    }
}

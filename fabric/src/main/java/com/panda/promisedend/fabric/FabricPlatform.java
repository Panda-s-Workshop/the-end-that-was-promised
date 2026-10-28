package com.panda.promisedend.fabric;

import com.panda.promisedend.platform.EndPlatform;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.CreativeModeTab;

/** {@link EndPlatform} on Fabric. */
public final class FabricPlatform implements EndPlatform {
    @Override
    public String loaderName() {
        return "fabric";
    }

    @Override
    public String modName(String namespace) {
        return FabricLoader.getInstance().getModContainer(namespace).map(c -> c.getMetadata().getName()).orElse(namespace);
    }

    @Override
    public CreativeModeTab.Builder tabBuilder() {
        return FabricCreativeModeTab.builder();
    }

    @Override
    public boolean isFakePlayer(net.minecraft.world.entity.player.Player player) {
        return player instanceof net.fabricmc.fabric.api.entity.FakePlayer;
    }

    @Override
    public void sendToPlayer(net.minecraft.server.level.ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.canSend(player, payload.type())) net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player, payload);
    }
}

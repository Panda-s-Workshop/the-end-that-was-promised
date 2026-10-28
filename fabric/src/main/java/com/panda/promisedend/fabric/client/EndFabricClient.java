package com.panda.promisedend.fabric.client;

import com.panda.promisedend.client.DevExit;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/** The Fabric client entry point: hands the common client registrations to Fabric API's registries and events. */
public final class EndFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> DevExit.onClientStopping());
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(com.panda.promisedend.client.VoidCurrentView::tick);
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(com.panda.promisedend.network.TerrainPayload.TYPE,
                (payload, context) -> com.panda.promisedend.logic.VoidCurrents.heardTerrain(payload.terrainEnabled()));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> com.panda.promisedend.logic.VoidCurrents.heardTerrain(null));
        com.panda.promisedend.client.ModRenderers.register(new com.panda.promisedend.client.ModRenderers.Sink() {
            @Override
            public <T extends net.minecraft.world.entity.Entity> void register(net.minecraft.world.entity.EntityType<? extends T> type,
                    net.minecraft.client.renderer.entity.EntityRendererProvider<T> provider) {
                net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(type, provider);
            }
        });
        com.panda.promisedend.client.ModParticleProviders.register(new com.panda.promisedend.client.ModParticleProviders.Sink() {
            @Override
            public <T extends net.minecraft.core.particles.ParticleOptions> void sprites(net.minecraft.core.particles.ParticleType<T> type,
                    com.panda.promisedend.client.ModParticleProviders.SpriteProvider<T> provider) {
                net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry.getInstance().register(type, provider::create);
            }
        });
        com.panda.promisedend.PromisedEnd.openCodex = () -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(new com.panda.promisedend.client.CodexScreen());
        com.panda.promisedend.client.ClientView.install();
    }
}

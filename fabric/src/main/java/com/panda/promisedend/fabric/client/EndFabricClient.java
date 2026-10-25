package com.panda.promisedend.fabric.client;

import com.panda.promisedend.client.DevExit;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/** The Fabric client entry point: hands the common client registrations to Fabric API's registries and events. */
public final class EndFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> DevExit.onClientStopping());
        com.panda.promisedend.client.ModRenderers.register(new com.panda.promisedend.client.ModRenderers.Sink() {
            @Override
            public <T extends net.minecraft.world.entity.Entity> void register(net.minecraft.world.entity.EntityType<? extends T> type,
                    net.minecraft.client.renderer.entity.EntityRendererProvider<T> provider) {
                net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(type, provider);
            }
        });
        com.panda.promisedend.PromisedEnd.openCodex = () -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(new com.panda.promisedend.client.CodexScreen());
        com.panda.promisedend.client.UpdraftView.install();
    }
}

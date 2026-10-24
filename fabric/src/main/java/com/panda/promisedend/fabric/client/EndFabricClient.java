package com.panda.promisedend.fabric.client;

import com.panda.promisedend.client.DevExit;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

/** The Fabric client entry point: hands the common client registrations to Fabric API's registries and events. */
public final class EndFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> DevExit.onClientStopping());
        com.panda.promisedend.PromisedEnd.openCodex = () -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(new com.panda.promisedend.client.CodexScreen());
    }
}

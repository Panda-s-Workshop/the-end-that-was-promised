package com.panda.promisedend.neoforge.client;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.client.DevExit;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.GameShuttingDownEvent;

/** The NeoForge client entry point: hands the common client registrations NeoForge's events. */
@Mod(value = PromisedEnd.MODID, dist = Dist.CLIENT)
public final class EndNeoForgeClient {
    public EndNeoForgeClient(IEventBus modBus, ModContainer container) {
        NeoForge.EVENT_BUS.addListener((GameShuttingDownEvent e) -> DevExit.onClientStopping());
        PromisedEnd.openCodex = () -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(new com.panda.promisedend.client.CodexScreen());
    }
}

package com.panda.promisedend.neoforge;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.logic.VoidAnchor;
import com.panda.promisedend.platform.registry.DeferredRegister;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The NeoForge entry point: registers the common content and wires the common hooks to NeoForge's events. */
@Mod(PromisedEnd.MODID)
public final class EndNeoForge {
    public EndNeoForge(IEventBus modBus, ModContainer container) {
        PromisedEnd.init();
        modBus.addListener((RegisterEvent e) -> DeferredRegister.all().forEach(register -> registerInto(e, register)));
        container.registerConfig(ModConfig.Type.SYNCED, NeoForgeConfig.SPEC);
        EndConfig.use(new NeoForgeConfig());

        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> PromisedEnd.onServerStarting(e.getServer()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> com.panda.promisedend.ServerCheck.onServerStarted(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> e.getServer().getPlayerList().getPlayers().forEach(PromisedEnd::onPlayerTick));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> com.panda.promisedend.EndCommand.register(e.getDispatcher()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) VoidAnchor.forget(p);
        });

        com.panda.promisedend.gametest.EndGameTests.register(modBus);
    }

    private static <R> void registerInto(RegisterEvent event, DeferredRegister<R> register) {
        event.register(register.getRegistryKey(), helper -> register.registerAll(helper::register));
    }
}

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
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.ItemTooltipEvent e) ->
                com.panda.promisedend.client.ShrineScreen.offeringLine(e.getItemStack(), e.getToolTip()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) ->
                com.panda.promisedend.client.VoidCurrentView.tick(net.minecraft.client.Minecraft.getInstance()));
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e) ->
                com.panda.promisedend.client.ModRenderers.register(new com.panda.promisedend.client.ModRenderers.Sink() {
                    @Override
                    public <T extends net.minecraft.world.entity.Entity> void register(net.minecraft.world.entity.EntityType<? extends T> type,
                            net.minecraft.client.renderer.entity.EntityRendererProvider<T> provider) {
                        e.registerEntityRenderer(type, provider);
                    }
                }));
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent e) ->
                com.panda.promisedend.client.ModParticleProviders.register(new com.panda.promisedend.client.ModParticleProviders.Sink() {
                    @Override
                    public <T extends net.minecraft.core.particles.ParticleOptions> void sprites(net.minecraft.core.particles.ParticleType<T> type,
                            com.panda.promisedend.client.ModParticleProviders.SpriteProvider<T> provider) {
                        e.registerSpriteSet(type, provider::create);
                    }
                }));
        modBus.addListener((net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent e) -> {
            e.register(com.panda.promisedend.network.TerrainPayload.TYPE, (payload, context) -> com.panda.promisedend.logic.VoidCurrents.heardTerrain(payload.terrainEnabled()));
            e.register(com.panda.promisedend.network.ShrinePayloads.Offer.TYPE, (payload, context) -> com.panda.promisedend.client.ShrineScreen.open(payload));
            e.register(com.panda.promisedend.network.RiftPayloads.Gates.TYPE, (payload, context) -> com.panda.promisedend.client.RiftGateScreen.open(payload));
        });
        PromisedEnd.toServer = payload -> {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null && connection.hasChannel(payload.type())) net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload);
        };
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) ->
                com.panda.promisedend.logic.VoidCurrents.heardTerrain(null));
        PromisedEnd.openCodex = () -> net.minecraft.client.Minecraft.getInstance().gui.setScreen(new com.panda.promisedend.client.CodexScreen());
        com.panda.promisedend.client.ClientView.install();
    }
}

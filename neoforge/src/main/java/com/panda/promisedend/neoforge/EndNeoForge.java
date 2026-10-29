package com.panda.promisedend.neoforge;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.logic.VoidAnchor;
import com.panda.promisedend.platform.registry.DeferredRegister;
import com.panda.promisedend.registry.ModBlocks;
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
        modBus.addListener((RegisterEvent e) -> e.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
                helper -> helper.register(com.panda.promisedend.PromisedEnd.id("construct_fallback_spawns"), ConstructFallbackSpawns.CODEC)));
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e) ->
                com.panda.promisedend.registry.ModEntities.attributes((type, attributes) -> e.put(type, attributes.build())));
        modBus.addListener((net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent e) ->
                com.panda.promisedend.registry.ModEntities.spawnRules(new com.panda.promisedend.registry.ModEntities.SpawnRules() {
                    @Override
                    public <T extends net.minecraft.world.entity.Mob> void set(net.minecraft.world.entity.EntityType<T> type, net.minecraft.world.entity.SpawnPlacementType placement,
                                                                               net.minecraft.world.level.levelgen.Heightmap.Types heightmap, net.minecraft.world.entity.SpawnPlacements.SpawnPredicate<T> predicate) {
                        e.register(type, placement, heightmap, predicate, net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
                    }
                }));
        // the chorus signs and shelf are vanilla's block entities; stripping is data (neoforge/data_maps/block/transformables.json)
        modBus.addListener((net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent e) -> {
            e.modify(net.minecraft.world.level.block.entity.BlockEntityTypes.SIGN, ModBlocks.CHORUS_SIGN.get(), ModBlocks.CHORUS_WALL_SIGN.get());
            e.modify(net.minecraft.world.level.block.entity.BlockEntityTypes.HANGING_SIGN, ModBlocks.CHORUS_HANGING_SIGN.get(), ModBlocks.CHORUS_WALL_HANGING_SIGN.get());
            e.modify(net.minecraft.world.level.block.entity.BlockEntityTypes.SHELF, ModBlocks.CHORUS_SHELF.get());
        });
        container.registerConfig(ModConfig.Type.SYNCED, NeoForgeConfig.SPEC);
        EndConfig.use(new NeoForgeConfig());

        NeoForge.EVENT_BUS.addListener((ServerAboutToStartEvent e) -> PromisedEnd.onServerStarting(e.getServer()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStartedEvent e) -> com.panda.promisedend.ServerCheck.onServerStarted(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> PromisedEnd.onServerTick(e.getServer()));
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent e) -> com.panda.promisedend.EndCommand.register(e.getDispatcher()));
        // the client's handler is the client entry's
        modBus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) -> e.registrar("1")
                .playToClient(com.panda.promisedend.network.TerrainPayload.TYPE, com.panda.promisedend.network.TerrainPayload.STREAM_CODEC)
                .playToClient(com.panda.promisedend.network.ShrinePayloads.Offer.TYPE, com.panda.promisedend.network.ShrinePayloads.Offer.STREAM_CODEC)
                .playToClient(com.panda.promisedend.network.RiftPayloads.Gates.TYPE, com.panda.promisedend.network.RiftPayloads.Gates.STREAM_CODEC)
                .playToServer(com.panda.promisedend.network.RiftPayloads.Travel.TYPE, com.panda.promisedend.network.RiftPayloads.Travel.STREAM_CODEC, (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer p) com.panda.promisedend.logic.RiftGates.travelled(p, payload);
                })
                .playToServer(com.panda.promisedend.network.ShrinePayloads.Choose.TYPE, com.panda.promisedend.network.ShrinePayloads.Choose.STREAM_CODEC, (payload, context) -> {
                    if (context.player() instanceof net.minecraft.server.level.ServerPlayer p) com.panda.promisedend.block.ShrineAltarBlockEntity.chosen(p, payload);
                }));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) PromisedEnd.onPlayerJoin(p);
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> {
            if (e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p) VoidAnchor.forget(p);
        });

        com.panda.promisedend.gametest.EndGameTests.register(modBus);
    }

    private static <R> void registerInto(RegisterEvent event, DeferredRegister<R> register) {
        event.register(register.getRegistryKey(), helper -> register.registerAll(helper::register));
    }
}

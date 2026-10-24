package com.panda.promisedend.fabric;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.logic.VoidAnchor;
import com.panda.promisedend.platform.registry.DeferredRegister;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.GenerationStep;

/** The Fabric entry point: registers the common content and wires the common hooks to Fabric API's events. */
public final class EndFabric implements ModInitializer {
    /**
     * Registries whose entries others are made from, registered first in this order: items take blocks, components and
     * entity types (spawn eggs). The rest follow in the order the mod created them.
     */
    private static final List<ResourceKey<?>> FIRST = List.of(Registries.BLOCK, Registries.DATA_COMPONENT_TYPE, Registries.SOUND_EVENT,
            Registries.PARTICLE_TYPE, Registries.ENTITY_TYPE, Registries.BLOCK_ENTITY_TYPE, Registries.ITEM);

    @Override
    public void onInitialize() {
        EndConfig.use(FabricConfig.load());
        PromisedEnd.init();
        DeferredRegister.all().stream()
                .sorted(Comparator.comparingInt(r -> {
                    int i = FIRST.indexOf(r.getRegistryKey());
                    return i < 0 ? FIRST.size() : i;
                }))
                .forEach(EndFabric::registerAll);

        // terrain.enabled=false: a copy of the Rim's debris goes on every End biome in #promisedend:rim_fallback, where
        // the feature keeps to the ring; NeoForge reads the same from data/promisedend/neoforge/biome_modifier/
        BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME, PromisedEnd.id("rim_fallback"))),
                GenerationStep.Decoration.RAW_GENERATION, ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("rim_debris_fallback")));

        ServerLifecycleEvents.SERVER_STARTING.register(PromisedEnd::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(com.panda.promisedend.ServerCheck::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(server -> server.getPlayerList().getPlayers().forEach(PromisedEnd::onPlayerTick));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> VoidAnchor.forget(handler.getPlayer()));
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> com.panda.promisedend.EndCommand.register(dispatcher));
    }

    @SuppressWarnings("unchecked")
    private static <R> void registerAll(DeferredRegister<R> register) {
        Registry<R> registry = (Registry<R>) BuiltInRegistries.REGISTRY.getValue(register.getRegistryKey().identifier());
        if (registry == null) throw new IllegalStateException("No registry " + register.getRegistryKey().identifier());
        register.registerAll((key, value) -> Registry.register(registry, key, value));
    }
}

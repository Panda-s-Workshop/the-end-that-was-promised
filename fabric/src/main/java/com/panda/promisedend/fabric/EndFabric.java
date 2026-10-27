package com.panda.promisedend.fabric;

import com.panda.promisedend.EndConfig;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.logic.VoidAnchor;
import com.panda.promisedend.platform.registry.DeferredRegister;
import com.panda.promisedend.registry.ModBlocks;
import java.util.Comparator;
import java.util.List;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.item.v1.BlockTransformerHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
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
        // the chorus signs and shelf are vanilla's block entities, and the axe strips chorus timber (NeoForge: stripping is data)
        FabricBlockEntityType sign = (FabricBlockEntityType) BlockEntityTypes.SIGN, hanging = (FabricBlockEntityType) BlockEntityTypes.HANGING_SIGN;
        sign.addValidBlock(ModBlocks.CHORUS_SIGN.get());
        sign.addValidBlock(ModBlocks.CHORUS_WALL_SIGN.get());
        hanging.addValidBlock(ModBlocks.CHORUS_HANGING_SIGN.get());
        hanging.addValidBlock(ModBlocks.CHORUS_WALL_HANGING_SIGN.get());
        ((FabricBlockEntityType) BlockEntityTypes.SHELF).addValidBlock(ModBlocks.CHORUS_SHELF.get());
        BlockTransformerHelper.registerStripping(ModBlocks.CHORUS_LOG.get(), ModBlocks.STRIPPED_CHORUS_LOG.get());
        BlockTransformerHelper.registerStripping(ModBlocks.CHORUS_WOOD.get(), ModBlocks.STRIPPED_CHORUS_WOOD.get());
        com.panda.promisedend.registry.ModEntities.attributes(
                net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry::register);

        // terrain.enabled=false: a copy of the Rim's debris goes on every End biome in #promisedend:rim_fallback, where
        // the feature keeps to the ring; NeoForge reads the same from data/promisedend/neoforge/biome_modifier/
        BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME, PromisedEnd.id("rim_fallback"))),
                GenerationStep.Decoration.RAW_GENERATION, ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("rim_debris_fallback")));
        // and a sparser chorus grove on #promisedend:grove_fallback (vanilla's highlands), so the End still has its wood
        BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME, PromisedEnd.id("grove_fallback"))),
                GenerationStep.Decoration.VEGETAL_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("chorus_grove_fallback")));
        // and the Mire's pools on #promisedend:mire_fallback (vanilla's midlands), so void-water, Lumen and the Lily still exist
        BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME, PromisedEnd.id("mire_fallback"))),
                GenerationStep.Decoration.VEGETAL_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("mire_pool_fallback")));
        // and spire formations on #promisedend:spire_fallback (vanilla's highlands), so Resonite still exists
        BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME, PromisedEnd.id("spire_fallback"))),
                GenerationStep.Decoration.VEGETAL_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("resonite_spire_fallback")));
        // and Drift Blossoms and ancient vents on #promisedend:skyfall_fallback (vanilla's small islands), so Drift Silk
        // and the vents still exist; both placed only while our terrain is off (worldgen/TerrainOffFilter)
        for (String feature : new String[] {"drift_blossom_fallback", "updraft_vent_fallback"}) {
            BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME, PromisedEnd.id("skyfall_fallback"))),
                    GenerationStep.Decoration.VEGETAL_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id(feature)));
        }
        // and rift scars and ancient vents on #promisedend:voidscar_fallback (vanilla's barrens), so Voidsteel Scrap exists
        BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME, PromisedEnd.id("voidscar_fallback"))),
                GenerationStep.Decoration.LOCAL_MODIFICATIONS, ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("rift_scar_fallback")));
        BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME, PromisedEnd.id("voidscar_fallback"))),
                GenerationStep.Decoration.VEGETAL_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("updraft_vent_fallback")));
        // and the Reefs' growths and colonies on #promisedend:reef_fallback (vanilla's midlands), so Ancient Purpur exists
        BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME, PromisedEnd.id("reef_fallback"))),
                GenerationStep.Decoration.VEGETAL_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("purpur_reef_fallback")));
        // and the caverns and their Echo Amber in #promisedend:hollow_fallback (vanilla's highlands), before the chorus grows
        BiomeModifications.addFeature(BiomeSelectors.tag(TagKey.create(Registries.BIOME, PromisedEnd.id("hollow_fallback"))),
                GenerationStep.Decoration.LOCAL_MODIFICATIONS, ResourceKey.create(Registries.PLACED_FEATURE, PromisedEnd.id("hollow_cavern_fallback")));

        ServerLifecycleEvents.SERVER_STARTING.register(PromisedEnd::onServerStarting);
        ServerLifecycleEvents.SERVER_STARTED.register(com.panda.promisedend.ServerCheck::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(PromisedEnd::onServerTick);
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

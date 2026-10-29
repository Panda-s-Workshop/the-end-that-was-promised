package com.panda.promisedend.client;

import com.panda.promisedend.registry.ModEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Every entity renderer, for the loader's renderer registration. */
public final class ModRenderers {
    public interface Sink {
        <T extends Entity> void register(EntityType<? extends T> type, EntityRendererProvider<T> provider);
    }

    public static void register(Sink sink) {
        sink.register(ModEntities.CRYSTAL_WARDEN.get(), CrystalWardenRenderer::new);
        sink.register(ModEntities.DIVE_MARK.get(), DiveMarkRenderer::new);
        sink.register(ModEntities.DRIFT_ISLET.get(), DriftIsletRenderer::new);
        sink.register(ModEntities.CHORUS_WEAVER.get(), c -> new ConstructRenderer<>(c, "chorus_weaver", com.panda.promisedend.entity.ChorusWeaver.MODEL_SCALE, 0.5F));
        sink.register(ModEntities.LUMEN_WISP.get(), c -> new ConstructRenderer<>(c, "lumen_wisp", com.panda.promisedend.entity.LumenWisp.MODEL_SCALE, 0.2F));
        // the stalker's voidstone and the sentry's purpur hold a little of their rift's and crystal's light, so End light
        // doesn't draw them as silhouettes (the stalker read as an enderman, the sentry as grey)
        sink.register(ModEntities.RIFT_STALKER.get(), c -> new ConstructRenderer<>(c, "rift_stalker", com.panda.promisedend.entity.RiftStalker.MODEL_SCALE, 0.5F, 9));
        sink.register(ModEntities.PURPUR_SENTRY.get(), c -> new ConstructRenderer<>(c, "purpur_sentry", com.panda.promisedend.entity.PurpurSentry.MODEL_SCALE, 0.7F, 7));
        sink.register(ModEntities.HOLLOW_SENTINEL.get(), c -> new ConstructRenderer<>(c, "hollow_sentinel", com.panda.promisedend.entity.HollowSentinel.MODEL_SCALE, 1.2F, 7));
        sink.register(ModEntities.VOID_LEVIATHAN.get(), c -> new ConstructRenderer<>(c, "void_leviathan", com.panda.promisedend.entity.VoidLeviathan.MODEL_SCALE, 0.0F, 7));
        // the crawler's lattice holds a little of its seams' light, as the stalker's voidstone does
        sink.register(ModEntities.LATTICE_CRAWLER.get(), c -> new ConstructRenderer<>(c, "lattice_crawler", com.panda.promisedend.entity.LatticeCrawler.MODEL_SCALE, 0.6F, 7));
        // the Architect's lattice lit by its seams and keystone, as the crawler's
        sink.register(ModEntities.THE_ARCHITECT.get(), c -> new ConstructRenderer<>(c, "the_architect", com.panda.promisedend.entity.TheArchitect.MODEL_SCALE, 1.1F, 7));
        sink.register(ModEntities.LATTICE_SHARD.get(), c -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(c, 1.5F, true));
        // a Resonite shard, lit, so a shot reads against the void and the arena alike
        sink.register(ModEntities.RESONANT_BULLET.get(), c -> new net.minecraft.client.renderer.entity.ThrownItemRenderer<>(c, 1.25F, true));
    }

    private ModRenderers() {}
}

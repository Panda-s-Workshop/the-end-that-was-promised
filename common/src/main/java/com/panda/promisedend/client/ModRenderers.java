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
        sink.register(ModEntities.CHORUS_WEAVER.get(), c -> new ConstructRenderer<>(c, "chorus_weaver", com.panda.promisedend.entity.ChorusWeaver.MODEL_SCALE, 0.5F));
        sink.register(ModEntities.LUMEN_WISP.get(), c -> new ConstructRenderer<>(c, "lumen_wisp", com.panda.promisedend.entity.LumenWisp.MODEL_SCALE, 0.2F));
        sink.register(ModEntities.RIFT_STALKER.get(), c -> new ConstructRenderer<>(c, "rift_stalker", com.panda.promisedend.entity.RiftStalker.MODEL_SCALE, 0.5F));
    }

    private ModRenderers() {}
}

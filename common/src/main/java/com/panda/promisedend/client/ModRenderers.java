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
    }

    private ModRenderers() {}
}

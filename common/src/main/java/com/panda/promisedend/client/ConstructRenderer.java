package com.panda.promisedend.client;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.panda.promisedend.PromisedEnd;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;

/**
 * A construct drawn as its GeckoLib rig (assets/promisedend/geckolib/models/entity/{@code id}.geo.json) at a scale,
 * with what its glowmask marks drawn unlit. The Crystal Warden has its own renderer for its beam and tether.
 */
public class ConstructRenderer<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
    @SuppressWarnings("unchecked")
    public ConstructRenderer(EntityRendererProvider.Context context, String id, float scale, float shadow) {
        super(context, new DefaultedEntityGeoModel<>(PromisedEnd.id(id)));
        withScale(scale);
        withRenderLayer(glowLayer(this));
        this.shadowRadius = shadow;
    }

    /** As the Warden's: the glow layer's types erased, since common compiles against plain Minecraft's render states. */
    @SuppressWarnings("rawtypes")
    private static GeoRenderLayer glowLayer(GeoRenderer renderer) {
        return new AutoGlowingGeoLayer(renderer);
    }
}

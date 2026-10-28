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
 * with what its glowmask marks drawn unlit, and lit at least {@code minLight} (block light): End light leaves a mob
 * near black, and a stone construct that drew as a silhouette read as an enderman. The Crystal Warden has its own
 * renderer for its beam and tether.
 */
public class ConstructRenderer<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
    private final int minLight;

    public ConstructRenderer(EntityRendererProvider.Context context, String id, float scale, float shadow) {
        this(context, id, scale, shadow, 0);
    }

    @SuppressWarnings("unchecked")
    public ConstructRenderer(EntityRendererProvider.Context context, String id, float scale, float shadow, int minLight) {
        super(context, new DefaultedEntityGeoModel<>(PromisedEnd.id(id)));
        withScale(scale);
        withRenderLayer(glowLayer(this));
        this.shadowRadius = shadow;
        this.minLight = minLight;
    }

    @Override
    protected int getBlockLightLevel(T entity, net.minecraft.core.BlockPos pos) {
        return Math.max(minLight, super.getBlockLightLevel(entity, pos));
    }

    /** As the Warden's: the glow layer's types erased, since common compiles against plain Minecraft's render states. */
    @SuppressWarnings("rawtypes")
    private static GeoRenderLayer glowLayer(GeoRenderer renderer) {
        return new AutoGlowingGeoLayer(renderer);
    }
}

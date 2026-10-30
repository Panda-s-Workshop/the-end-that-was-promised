package com.panda.promisedend.client;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.panda.promisedend.PromisedEnd;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * A construct drawn as its GeckoLib rig (assets/promisedend/geckolib/models/entity/{@code id}.geo.json) at a scale,
 * with what its glowmask marks drawn unlit, and lit at least {@code minLight} (block light): End light leaves a mob
 * near black, and a stone construct that drew as a silhouette read as an enderman. A boss's glow has four stages
 * ({@code <id>_glowmask_<n>.png}), brighter and hotter as its health falls past three quarters, a half and a quarter.
 * The Crystal Warden has its own renderer for its beam and tether.
 */
public class ConstructRenderer<T extends LivingEntity & GeoEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
    private static final DataTicket<Integer> GLOW_STAGE = DataTicket.create("promisedend_glow_stage", Integer.class);
    private final int minLight;
    private final boolean staged;
    private int tint = 0xFFFFFFFF;

    public ConstructRenderer(EntityRendererProvider.Context context, String id, float scale, float shadow) {
        this(context, id, scale, shadow, 0);
    }

    public ConstructRenderer(EntityRendererProvider.Context context, String id, float scale, float shadow, int minLight) {
        this(context, id, scale, shadow, minLight, false);
    }

    @SuppressWarnings("unchecked")
    public ConstructRenderer(EntityRendererProvider.Context context, String id, float scale, float shadow, int minLight, boolean staged) {
        super(context, new DefaultedEntityGeoModel<>(PromisedEnd.id(id)));
        withScale(scale);
        withRenderLayer(staged ? stagedGlowLayer(this, id) : glowLayer(this));
        this.shadowRadius = shadow;
        this.minLight = minLight;
        this.staged = staged;
    }

    /** A boss's glow stage, 0 to 3, by its health: past three quarters, a half and a quarter. */
    public static int glowStage(float health, float max) {
        float share = max <= 0 ? 1 : health / max;
        return share > 0.75F ? 0 : share > 0.5F ? 1 : share > 0.25F ? 2 : 3;
    }

    @Override
    public void extractRenderState(T entity, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        if (staged) ((GeoRenderState) (Object) state).addGeckolibData(GLOW_STAGE, glowStage(entity.getHealth(), entity.getMaxHealth()));
    }

    /**
     * Draws it tinted {@code argb}: block light, the floor it is lit to, is warm, and turned a pale grey-violet shell
     * tan; a cool tint gives that warmth back.
     */
    public ConstructRenderer<T> withTint(int argb) {
        this.tint = argb;
        return this;
    }

    @Override
    public int getRenderColor(T animatable, Void relatedObject, float partialTick) {
        // multiplied, so GeckoLib's own colour (an invisible mob's faint drawing for a spectator) is kept
        return net.minecraft.util.ARGB.multiply(super.getRenderColor(animatable, relatedObject, partialTick), tint);
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

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static GeoRenderLayer stagedGlowLayer(GeoRenderer renderer, String id) {
        Identifier[] stages = new Identifier[4];
        for (int n = 0; n < 4; n++) stages[n] = PromisedEnd.id("textures/entity/" + id + "_glowmask_" + n + ".png");
        return new AutoGlowingGeoLayer(renderer) {
            @Override
            protected Identifier getTextureResource(GeoRenderState state) {
                Integer stage = state.getOrDefaultGeckolibData(GLOW_STAGE, -1);
                return stage < 0 ? super.getTextureResource(state) : stages[Math.min(3, stage)];
            }
        };
    }
}

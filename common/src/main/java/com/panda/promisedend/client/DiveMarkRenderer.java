package com.panda.promisedend.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.entity.DiveMark;
import com.panda.promisedend.logic.DragonPhase3;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;

/**
 * The dive's mark, drawn as full-bright geometry on the ground (End light greys lit particles). Through the telegraph a
 * fixed outer ring is the breath's edge, what is inside it when the breath lands is caught, and a second ring closes from
 * it to the centre as the 1.5 s run out, so the player reads where and when. Through the charge the edge pulses. Once the
 * breath is down the edge turns the void's dark violet and holds until the cloud's last quarter, which tells it from
 * vanilla's breath.
 */
public final class DiveMarkRenderer extends EntityRenderer<DiveMark, DiveMarkRenderer.State> {
    private static final Identifier TEXTURE = PromisedEnd.id("textures/entity/crystal_warden_beam.png");
    private static final RenderType RING = RenderTypes.beaconBeam(TEXTURE, true);
    private static final int SEGMENTS = 48;
    /** Just over the ground, so it doesn't flicker with the top face it lies on. */
    private static final float LIFT = 0.06F;

    public static final class State extends EntityRenderState {
        DiveMark.Stage stage = DiveMark.Stage.TELEGRAPH;
        float ticks;
    }

    public DiveMarkRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** It is a point, but what it draws is the breath's width. */
    @Override
    protected AABB getBoundingBoxForCulling(DiveMark mark, float partialTick) {
        return mark.getBoundingBox().inflate(DragonPhase3.BREATH_RADIUS + 0.5, 0.5, DragonPhase3.BREATH_RADIUS + 0.5);
    }

    @Override
    public void extractRenderState(DiveMark mark, State state, float partialTick) {
        super.extractRenderState(mark, state, partialTick);
        state.stage = mark.stage();
        state.ticks = mark.clientStageTicks() + partialTick;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        float edge = DragonPhase3.BREATH_RADIUS;
        float t = state.ticks;
        collector.submitCustomGeometry(pose, RING, (p, vc) -> {
            switch (state.stage) {
                case TELEGRAPH -> {
                    float left = 1.0F - Math.min(1.0F, t / DragonPhase3.TELEGRAPH_TICKS);
                    int a = (int) (200 + 55 * Math.sin(t * 0.6));
                    ring(vc, p, edge, 0.09F, 200, 120, 255, a);
                    if (left > 0.04F) ring(vc, p, edge * left, 0.06F, 255, 220, 255, 230);
                }
                // the charge is coming: the edge beats fast and bright
                case CHARGE -> ring(vc, p, edge, 0.11F, 220, 140, 255, (int) (190 + 65 * Math.sin(t * 1.4)));
                case BREATH -> {
                    // held for as long as the breath bites, then gone with it over its last quarter
                    float left = Math.max(0.0F, 1.0F - t / DragonPhase3.BREATH_TICKS);
                    ring(vc, p, edge, 0.16F, 70, 20, 110, (int) (230 * Math.min(1.0F, left * 4.0F)));
                }
            }
        });
    }

    /** A flat ring round the mark, drawn from above and below. */
    private static void ring(VertexConsumer vc, PoseStack.Pose p, float radius, float half, int r, int g, int b, int a) {
        if (a <= 0) return;
        float in = Math.max(0.0F, radius - half), out = radius + half;
        for (int i = 0; i < SEGMENTS; i++) {
            double a0 = Math.PI * 2 * i / SEGMENTS, a1 = Math.PI * 2 * (i + 1) / SEGMENTS;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            float v0 = (float) i / SEGMENTS, v1 = (float) (i + 1) / SEGMENTS;
            // up
            vertex(vc, p, c0 * in, s0 * in, 0, v0, r, g, b, a);
            vertex(vc, p, c1 * in, s1 * in, 0, v1, r, g, b, a);
            vertex(vc, p, c1 * out, s1 * out, 1, v1, r, g, b, a);
            vertex(vc, p, c0 * out, s0 * out, 1, v0, r, g, b, a);
            // down
            vertex(vc, p, c0 * out, s0 * out, 1, v0, r, g, b, a);
            vertex(vc, p, c1 * out, s1 * out, 1, v1, r, g, b, a);
            vertex(vc, p, c1 * in, s1 * in, 0, v1, r, g, b, a);
            vertex(vc, p, c0 * in, s0 * in, 0, v0, r, g, b, a);
        }
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float z, float u, float v, int r, int g, int b, int a) {
        vc.addVertex(p, x, LIFT, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(p, 0.0F, 1.0F, 0.0F);
    }
}

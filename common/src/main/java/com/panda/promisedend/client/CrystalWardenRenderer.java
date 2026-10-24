package com.panda.promisedend.client;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedEntityGeoModel;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.entity.CrystalWarden;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Crystal Warden: its GeckoLib rig (assets/promisedend/geckolib/models/entity/crystal_warden.geo.json) at 1.5 scale,
 * with the crystal and the lens drawn unlit from crystal_warden_glowmask.png. While it locks on, a thin thread of light
 * runs from the lens to the target and thickens over the second; while it beams, a full-bright beam, drawn as vanilla
 * draws a guardian's or a beacon's (particles are lit by the End's dim light and read grey). When the target is the
 * camera, the line stops short of it and fades out over its last stretch instead of ending in a cut. A warden its dragon
 * called draws a thin tether from its crystal to the dragon while it beams: the dragon heals through it, as through an
 * end crystal's beam.
 */
public final class CrystalWardenRenderer extends GeoEntityRenderer<CrystalWarden, LivingEntityRenderState> {
    private static final Identifier BEAM_TEXTURE = PromisedEnd.id("textures/entity/crystal_warden_beam.png");
    private static final RenderType BEAM = RenderTypes.beaconBeam(BEAM_TEXTURE, true);
    /** The lens relative to the warden's feet, and the line from it to the target. */
    private static final DataTicket<Vec3> LENS = DataTicket.create("promisedend_warden_lens", Vec3.class);
    private static final DataTicket<Vec3> SPAN = DataTicket.create("promisedend_warden_span", Vec3.class);
    /** How far through the lock-on it is (0 to 1), 2 while it beams, -1 when it does neither. */
    private static final DataTicket<Float> CHARGE = DataTicket.create("promisedend_warden_charge", Float.class);
    /** The tether, from the warden's feet to its crystal and from there to the dragon. */
    private static final DataTicket<Vec3> TETHER_FROM = DataTicket.create("promisedend_warden_tether_from", Vec3.class);
    private static final DataTicket<Vec3> TETHER = DataTicket.create("promisedend_warden_tether", Vec3.class);
    /** Where the crystal sits, in blocks above its feet (14 px up at the model's scale). */
    private static final double CORE_HEIGHT = 14.0 / 16.0 * CrystalWarden.MODEL_SCALE;
    /** The last stretch of the line that fades out (0 unless the target is the camera). */
    private static final DataTicket<Float> FADE = DataTicket.create("promisedend_warden_fade", Float.class);
    /** The target's own camera sits at the end of the line: stop this far short of it in first person, */
    private static final double CAMERA_GAP = 1.2;
    /** and fade its last this many blocks. */
    private static final float CAMERA_FADE = 1.5F;

    @SuppressWarnings("unchecked")
    public CrystalWardenRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(PromisedEnd.id("crystal_warden")));
        withScale(CrystalWarden.MODEL_SCALE);
        withRenderLayer(glowLayer(this));
        this.shadowRadius = 0.5F;
    }

    /**
     * GeckoLib's glow layer wants a render state that is a {@code GeoRenderState}. Vanilla's are at runtime (each loader's
     * GeckoLib injects the interface), but common code compiles against plain Minecraft, so the types are erased here.
     */
    @SuppressWarnings("rawtypes")
    private static GeoRenderLayer glowLayer(GeoRenderer renderer) {
        return new AutoGlowingGeoLayer(renderer);
    }

    /** The beam stays drawn while the warden itself is off screen, as long as the line from it to the target is not. */
    @Override
    public boolean shouldRender(CrystalWarden warden, Frustum frustum, double camX, double camY, double camZ, float partialTick) {
        if (super.shouldRender(warden, frustum, camX, camY, camZ, partialTick)) return true;
        if (warden.phase() == CrystalWarden.Phase.IDLE || !(warden.level().getEntity(warden.beamTargetId()) instanceof Entity target)) return false;
        if (frustum.isVisible(new AABB(warden.lensPosition(partialTick), CrystalWarden.aimPoint(target, partialTick)).inflate(0.2))) return true;
        Entity dragon = warden.phase() == CrystalWarden.Phase.BEAM ? warden.level().getEntity(warden.dragonEntityId()) : null;
        return dragon != null && frustum.isVisible(new AABB(warden.getPosition(partialTick), dragon.getPosition(partialTick)).inflate(0.5));
    }

    /** It shatters (CrystalWarden.tickDeath) rather than tipping over. */
    @Override
    protected float getDeathMaxRotation(GeoRenderState state) {
        return 0.0F;
    }

    @Override
    public void extractRenderState(CrystalWarden warden, LivingEntityRenderState state, float partialTick) {
        super.extractRenderState(warden, state, partialTick);
        GeoRenderState geo = (GeoRenderState) state;
        CrystalWarden.Phase phase = warden.phase();
        Entity target = phase == CrystalWarden.Phase.IDLE ? null : warden.level().getEntity(warden.beamTargetId());
        if (target == null) {
            geo.addGeckolibData(CHARGE, -1.0F);
            return;
        }
        Vec3 lens = warden.lensPosition(partialTick);
        Vec3 span = CrystalWarden.aimPoint(target, partialTick).subtract(lens);
        Minecraft mc = Minecraft.getInstance();
        float fade = 0.0F;
        if (target == mc.getCameraEntity() && mc.options.getCameraType().isFirstPerson()) {
            double length = span.length();
            span = span.scale(Math.max(0.0, length - CAMERA_GAP) / length);
            fade = CAMERA_FADE;
        }
        geo.addGeckolibData(FADE, fade);
        Entity dragon = phase == CrystalWarden.Phase.BEAM ? warden.level().getEntity(warden.dragonEntityId()) : null;
        if (dragon != null) {
            Vec3 core = warden.getPosition(partialTick).add(0, CORE_HEIGHT, 0);
            geo.addGeckolibData(TETHER_FROM, new Vec3(0, CORE_HEIGHT, 0));
            geo.addGeckolibData(TETHER, dragon.getPosition(partialTick).add(0, dragon.getBbHeight() * 0.5, 0).subtract(core));
        } else {
            // render states are reused from frame to frame: clear the last frame's tether
            geo.addGeckolibData(TETHER, Vec3.ZERO);
        }
        geo.addGeckolibData(LENS, lens.subtract(warden.getPosition(partialTick)));
        geo.addGeckolibData(SPAN, span);
        geo.addGeckolibData(CHARGE, phase == CrystalWarden.Phase.BEAM ? 2.0F
                : Math.min(1.0F, (warden.clientPhaseTicks() + partialTick) / CrystalWarden.LOCK_ON_TICKS));
    }

    @Override
    public void submit(LivingEntityRenderState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, pose, collector, camera);
        GeoRenderState geo = (GeoRenderState) state;
        float charge = geo.getOrDefaultGeckolibData(CHARGE, -1.0F);
        if (charge < 0) return;
        Vec3 lens = geo.getGeckolibData(LENS);
        Vec3 span = geo.getGeckolibData(SPAN);
        float length = (float) span.length();
        if (length < 0.05F) return;
        boolean beam = charge > 1;
        // the thread starts thick enough to see from across a pillar and thickens to a third of the beam
        float half = beam ? 0.14F : 0.04F + 0.03F * charge;
        // the beam in the lens's white, the thread in the crystal's pink, brightening as the lock-on completes
        int r = 255, g = beam ? 236 : 168, b = beam ? 252 : 238, a = beam ? 235 : (int) (150 + 90 * charge);
        float fade = Math.min(geo.getOrDefaultGeckolibData(FADE, 0.0F), length);
        float solid = length - fade;
        if (beam) tether(geo, pose, collector, state.ageInTicks);
        float scroll = state.ageInTicks * (beam ? 0.3F : 0.1F);
        Vec3 dir = span.normalize();
        pose.pushPose();
        pose.translate(lens.x, lens.y, lens.z);
        // as the guardian's beam: turn local +Y onto the line
        pose.rotateDegrees(Axis.YP, (float) Math.toDegrees(Math.PI / 2 - Math.atan2(dir.z, dir.x)));
        pose.rotateDegrees(Axis.XP, (float) Math.toDegrees(Math.acos(dir.y)));
        collector.submitCustomGeometry(pose, BEAM, (p, vc) -> {
            // two crossed planes, each drawn from both sides; the fading stretch goes from full alpha to none
            segment(vc, p, half, 0, solid, scroll, r, g, b, a, a);
            if (fade > 0) segment(vc, p, half, solid, length, scroll, r, g, b, a, 0);
        });
        pose.popPose();
    }

    /** The thin pink line to the dragon, its texture running towards the dragon. */
    private static void tether(GeoRenderState geo, PoseStack pose, SubmitNodeCollector collector, float age) {
        Vec3 from = geo.getGeckolibData(TETHER_FROM);
        Vec3 span = geo.getOrDefaultGeckolibData(TETHER, Vec3.ZERO);
        float length = (float) span.length();
        if (from == null || length < 0.5F) return;
        Vec3 dir = span.normalize();
        pose.pushPose();
        pose.translate(from.x, from.y, from.z);
        pose.rotateDegrees(Axis.YP, (float) Math.toDegrees(Math.PI / 2 - Math.atan2(dir.z, dir.x)));
        pose.rotateDegrees(Axis.XP, (float) Math.toDegrees(Math.acos(dir.y)));
        collector.submitCustomGeometry(pose, BEAM, (p, vc) -> segment(vc, p, 0.05F, 0, length, age * 0.4F, 255, 150, 235, 170, 170));
        pose.popPose();
    }

    private static void segment(VertexConsumer vc, PoseStack.Pose p, float half, float y0, float y1, float scroll, int r, int g, int b, int a0, int a1) {
        if (y1 - y0 < 0.01F) return;
        float v0 = y0 * 0.5F - scroll, v1 = y1 * 0.5F - scroll;
        quad(vc, p, -half, 0, half, 0, y0, y1, v0, v1, r, g, b, a0, a1);
        quad(vc, p, half, 0, -half, 0, y0, y1, v0, v1, r, g, b, a0, a1);
        quad(vc, p, 0, -half, 0, half, y0, y1, v0, v1, r, g, b, a0, a1);
        quad(vc, p, 0, half, 0, -half, y0, y1, v0, v1, r, g, b, a0, a1);
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, float x0, float z0, float x1, float z1, float y0, float y1, float v0, float v1,
            int r, int g, int b, int a0, int a1) {
        vertex(vc, p, x0, y0, z0, 0, v0, r, g, b, a0);
        vertex(vc, p, x1, y0, z1, 1, v0, r, g, b, a0);
        vertex(vc, p, x1, y1, z1, 1, v1, r, g, b, a1);
        vertex(vc, p, x0, y1, z0, 0, v1, r, g, b, a1);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int r, int g, int b, int a) {
        vc.addVertex(p, x, y, z).setColor(r, g, b, a).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(p, 0.0F, 1.0F, 0.0F);
    }
}

package com.panda.promisedend.client;

import com.panda.promisedend.block.UpdraftVentBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;

/** Gives the Updraft Vent's particles the camera and the particle setting, which common code can't read itself. */
public final class UpdraftView {
    private UpdraftView() {}

    public static void install() {
        UpdraftVentBlockEntity.camera = () -> Minecraft.getInstance().gameRenderer.mainCamera().position();
        UpdraftVentBlockEntity.minimalParticles = () -> Minecraft.getInstance().options.particles().get() == ParticleStatus.MINIMAL;
    }
}

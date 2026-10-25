package com.panda.promisedend.client;

import com.panda.promisedend.block.UpdraftVentBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ParticleStatus;

/**
 * Gives common code what only the client knows, through suppliers it holds: the Updraft Vent's particles the camera
 * and the particle setting, and the Resonite armour's tooltip the player reading it.
 */
public final class ClientView {
    private ClientView() {}

    public static void install() {
        UpdraftVentBlockEntity.camera = () -> Minecraft.getInstance().gameRenderer.mainCamera().position();
        UpdraftVentBlockEntity.minimalParticles = () -> Minecraft.getInstance().options.particles().get() == ParticleStatus.MINIMAL;
        com.panda.promisedend.item.ResoniteArmourItem.viewer = () -> Minecraft.getInstance().player;
    }
}

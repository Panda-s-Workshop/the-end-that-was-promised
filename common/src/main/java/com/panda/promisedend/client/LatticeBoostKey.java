package com.panda.promisedend.client;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.item.Augments;
import com.panda.promisedend.network.BoostPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * The Architect's Lattice's key: the jump key pressed while already gliding on wings that carry it asks the server for a
 * boost. Not the press that opens the wings (the glide has to have begun a tick before).
 */
public final class LatticeBoostKey {
    private static boolean wasDown, wasGliding;

    private LatticeBoostKey() {}

    public static void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        boolean down = mc.options.keyJump.isDown();
        boolean gliding = player != null && Augments.gliding(player, Augments.ARCHITECTS_LATTICE);
        if (down && !wasDown && gliding && wasGliding && mc.gui.screen() == null) PromisedEnd.toServer.accept(BoostPayload.INSTANCE);
        wasDown = down;
        wasGliding = gliding;
    }
}

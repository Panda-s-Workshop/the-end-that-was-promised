package com.panda.promisedend.mixin;

import com.panda.promisedend.logic.AncientWard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The dragon sleeps while the Ancients' Ward stands: the fight doesn't tick, so it never scans for players near the
 * centre and never spawns the dragon (a creative player may cross the Ward; the fight still waits for the seals).
 */
@Mixin(EnderDragonFight.class)
abstract class EnderDragonFightMixin {
    @Shadow
    private ServerLevel level;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void promisedend$heldByTheWard(CallbackInfo ci) {
        if (level != null && AncientWard.stands(level.getServer())) ci.cancel();
    }
}

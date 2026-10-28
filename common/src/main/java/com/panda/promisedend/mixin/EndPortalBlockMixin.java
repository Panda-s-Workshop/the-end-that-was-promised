package com.panda.promisedend.mixin;

import com.panda.promisedend.logic.Arrivals;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.portal.TeleportTransition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An End portal entered outside the End sets the entity down by the portal's direction at an Ancient Waystation in the
 * outer End (logic/Arrivals), not on vanilla's obsidian platform by the dragon. The way back from the End is vanilla's.
 */
@Mixin(EndPortalBlock.class)
abstract class EndPortalBlockMixin {
    @Inject(method = "getPortalDestination", at = @At("HEAD"), cancellable = true)
    private void promisedend$arrival(ServerLevel level, Entity entity, BlockPos pos, CallbackInfoReturnable<TeleportTransition> cir) {
        TeleportTransition arrival = Arrivals.destination(level, entity, pos);
        if (arrival != null) cir.setReturnValue(arrival);
    }
}

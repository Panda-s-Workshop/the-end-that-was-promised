package com.panda.promisedend.mixin;

import com.panda.promisedend.block.ResoniteClusterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A player's block break, from its start to its end: a Resonite Cluster that goes during it, the one broken or one
 * that stood on the broken block, knows whose break it was.
 */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void promisedend$breakStarts(BlockPos pos, CallbackInfoReturnable<Boolean> info) {
        ResoniteClusterBlock.breaking(player);
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void promisedend$breakEnds(BlockPos pos, CallbackInfoReturnable<Boolean> info) {
        ResoniteClusterBlock.breaking(null);
    }
}

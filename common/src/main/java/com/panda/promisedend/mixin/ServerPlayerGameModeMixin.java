package com.panda.promisedend.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.panda.promisedend.block.ResoniteClusterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * A player's block break, from its start to its end: a Resonite Cluster that goes during it, the one broken or one
 * that stood on the broken block, knows whose break it was. The whole method is wrapped, so a break another mod
 * cancels part way (Fabric's break event returns early) still ends the bracket, and a break inside a break (a vein
 * miner's) hands the outer one back when it ends.
 */
@Mixin(ServerPlayerGameMode.class)
abstract class ServerPlayerGameModeMixin {
    @Shadow @Final protected ServerPlayer player;

    @WrapMethod(method = "destroyBlock")
    private boolean promisedend$bracketBreak(BlockPos pos, Operation<Boolean> original) {
        Player outer = ResoniteClusterBlock.breaker();
        ResoniteClusterBlock.breaking(player);
        try {
            return original.call(pos);
        } finally {
            ResoniteClusterBlock.breaking(outer);
        }
    }
}

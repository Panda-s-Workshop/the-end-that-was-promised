package com.panda.promisedend.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.panda.promisedend.item.ResonanceSet;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Resonance set bonus on a player's melee hit: the wrapper adds the ringing hit's damage before the target is hurt
 * and counts the hit only if it landed. Both loaders keep vanilla's {@code hurtOrSimulate} call in {@code attack}
 * (NeoForge's events fire round it).
 */
@Mixin(Player.class)
abstract class PlayerMixin {
    @WrapOperation(method = "attack",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean promisedend$resonance(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
        Player self = (Player) (Object) this;
        boolean hurt = original.call(target, source, ResonanceSet.damage(self, target, amount));
        ResonanceSet.landed(self, target, hurt);
        return hurt;
    }
}

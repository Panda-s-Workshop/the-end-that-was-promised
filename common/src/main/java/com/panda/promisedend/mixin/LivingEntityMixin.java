package com.panda.promisedend.mixin;

import com.panda.promisedend.logic.ShrineBoons;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The Shulker Ward boon: Levitation can't be added to an entity that has it (a shulker's bullet, a Sentinel's). */
@Mixin(LivingEntity.class)
abstract class LivingEntityMixin {
    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void promisedend$shulkerWard(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if (ShrineBoons.wards((LivingEntity) (Object) this, effect)) cir.setReturnValue(false);
    }
}

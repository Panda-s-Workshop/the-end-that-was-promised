package com.panda.promisedend.mixin;

import com.panda.promisedend.registry.ModComponents;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** An elytra's augments in its tooltip, after vanilla's own lines (vanilla lists only its own components). */
@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    @Inject(method = "addDetailsToTooltip", at = @At("TAIL"))
    private void promisedend$augments(Item.TooltipContext context, TooltipDisplay display, Player player, TooltipFlag flag, Consumer<Component> out, CallbackInfo ci) {
        ((ItemStack) (Object) this).addToTooltip(ModComponents.AUGMENTS.get(), context, display, out, flag);
    }
}

package com.panda.promisedend.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.registry.ModTags;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A Dragon Scale repairs what {@code #promisedend:repaired_by_dragon_scale} lists (the elytra) in an anvil, half its
 * durability a scale where vanilla's repair materials give a quarter. Vanilla's repair branch asks
 * {@code isValidRepairItem} once and then divides the item's durability by 4 twice (the first unit and each further
 * one); the wrapper notes whether the material is a scale, and the divisor becomes 2 for one. A scale's repair keeps
 * the item's prior-work cost as it was, so an elytra without Mending can be mended after every rematch; the note is
 * cleared as each result begins, so whatever is combined after it raises the cost as vanilla's does. MixinExtras' wrappers
 * chain with other mods' hooks on the same call and constant, where a redirect would clash with them.
 *
 * <p>NeoForge moves the vanilla body of {@code createResult} into {@code createResultInternal} (its own anvil event runs
 * first), so both names are targeted; each loader has the body under exactly one of them.
 */
@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin {
    @Unique private boolean promisedend$scaleRepair;

    @Inject(method = {"createResult", "createResultInternal"}, at = @At("HEAD"))
    private void promisedend$forgetScale(CallbackInfo ci) {
        promisedend$scaleRepair = false;
    }

    @WrapOperation(method = {"createResult", "createResultInternal"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isValidRepairItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean promisedend$repairWithScale(ItemStack item, ItemStack material, Operation<Boolean> original) {
        promisedend$scaleRepair = material.is(ModItems.DRAGON_SCALE.get()) && item.is(ModTags.REPAIRED_BY_DRAGON_SCALE);
        return promisedend$scaleRepair || original.call(item, material);
    }

    @ModifyExpressionValue(method = {"createResult", "createResultInternal"}, at = @At(value = "CONSTANT", args = "intValue=4"), require = 2)
    private int promisedend$scaleRepairsHalf(int quarter) {
        return promisedend$scaleRepair ? 2 : quarter;
    }

    @WrapOperation(method = {"createResult", "createResultInternal"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/AnvilMenu;calculateIncreasedRepairCost(I)I"))
    private int promisedend$scaleKeepsPriorWork(int cost, Operation<Integer> original) {
        return promisedend$scaleRepair ? cost : original.call(cost);
    }
}

package com.panda.promisedend.mixin;

import com.panda.promisedend.registry.ModItems;
import com.panda.promisedend.registry.ModTags;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * A Dragon Scale repairs what {@code #promisedend:repaired_by_dragon_scale} lists (the elytra) in an anvil, half its
 * durability a scale where vanilla's repair materials give a quarter. Vanilla's repair branch asks
 * {@code isValidRepairItem} once and then divides the item's durability by 4 twice (the first unit and each further
 * one); the redirect notes whether the material is a scale, and the constant becomes 2 for one.
 *
 * <p>NeoForge moves the vanilla body of {@code createResult} into {@code createResultInternal} (its own anvil event runs
 * first), so both names are targeted; each loader has the body under exactly one of them.
 */
@Mixin(AnvilMenu.class)
abstract class AnvilMenuMixin {
    @Unique private boolean promisedend$scaleRepair;

    @Redirect(method = {"createResult", "createResultInternal"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isValidRepairItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean promisedend$repairWithScale(ItemStack item, ItemStack material) {
        promisedend$scaleRepair = material.is(ModItems.DRAGON_SCALE.get()) && item.is(ModTags.REPAIRED_BY_DRAGON_SCALE);
        return promisedend$scaleRepair || item.isValidRepairItem(material);
    }

    @ModifyConstant(method = {"createResult", "createResultInternal"}, constant = @Constant(intValue = 4), require = 2)
    private int promisedend$scaleRepairsHalf(int quarter) {
        return promisedend$scaleRepair ? 2 : quarter;
    }
}

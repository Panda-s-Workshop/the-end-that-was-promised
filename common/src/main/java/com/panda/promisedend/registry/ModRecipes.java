package com.panda.promisedend.registry;

import com.panda.promisedend.PromisedEnd;
import com.panda.promisedend.item.ElytraAugmentRecipe;
import com.panda.promisedend.platform.registry.DeferredHolder;
import com.panda.promisedend.platform.registry.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

/** The mod's recipe types: the smithing that adds an augment to an elytra. */
public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, PromisedEnd.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ElytraAugmentRecipe>> ELYTRA_AUGMENT = SERIALIZERS.register("elytra_augment",
            () -> new RecipeSerializer<>(ElytraAugmentRecipe.MAP_CODEC, ElytraAugmentRecipe.STREAM_CODEC));

    private ModRecipes() {}
}

package com.panda.promisedend.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.panda.promisedend.registry.ModRecipes;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleSmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

/**
 * Smithing an augment onto an elytra: the augment in the template slot, the elytra as the base, the addition on the
 * right; out comes the same elytra, enchantments and all, with {@code augment} added to its {@link Augments}. It
 * doesn't match an elytra that already has that augment or is full, so the table shows nothing to take.
 */
public class ElytraAugmentRecipe extends SimpleSmithingRecipe {
    public static final MapCodec<ElytraAugmentRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            Ingredient.CODEC.fieldOf("template").forGetter(r -> r.template),
            Ingredient.CODEC.fieldOf("base").forGetter(r -> r.base),
            Ingredient.CODEC.fieldOf("addition").forGetter(r -> r.addition),
            Identifier.CODEC.fieldOf("augment").forGetter(r -> r.augment)).apply(i, ElytraAugmentRecipe::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, ElytraAugmentRecipe> STREAM_CODEC = StreamCodec.composite(
            CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.template,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.base,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.addition,
            Identifier.STREAM_CODEC, r -> r.augment,
            ElytraAugmentRecipe::new);

    private final Ingredient template, base, addition;
    private final Identifier augment;

    public ElytraAugmentRecipe(CommonInfo info, Ingredient template, Ingredient base, Ingredient addition, Identifier augment) {
        super(info);
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.augment = augment;
    }

    public Identifier augment() {
        return augment;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return super.matches(input, level) && !Augments.apply(input.base(), augment).isEmpty();
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        return Augments.apply(input.base(), augment);
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return Optional.of(template);
    }

    @Override
    public Ingredient baseIngredient() {
        return base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return Optional.of(addition);
    }

    @Override
    public RecipeSerializer<ElytraAugmentRecipe> getSerializer() {
        return ModRecipes.ELYTRA_AUGMENT.get();
    }

    @Override
    protected PlacementInfo createPlacementInfo() {
        return PlacementInfo.createFromOptionals(List.of(Optional.of(template), Optional.of(base), Optional.of(addition)));
    }
}
